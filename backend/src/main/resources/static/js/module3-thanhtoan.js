/**
 * MEDCARE CLINIC - MODULE 3: CHẤM CÔNG, KHIẾU NẠI & THANH TOÁN THÙ LAO
 * Kế toán: TT01 → TT02 → (TT03 / TT05 / TT06 → TT07)
 * Nhân sự: TT03 (chấm công của tôi, gửi khiếu nại TT04), TT08 → TT07
 */

const KN_TONE = { CHO_XU_LY: 'warning', DA_XU_LY: 'success', TU_CHOI: 'danger' };

// ==========================================
// TRANG CHỦ KẾ TOÁN (C04)
// ==========================================
SCREEN_LOADERS['screen-C04'] = async () => {
  const u = STATE.user;
  setText('c04Name', u.fullName);
  setText('c04Ma', [u.chucDanh, u.maNhanSu].filter(Boolean).join(' · '));
  const pay = STATE.payment;
  pay.week = pay.week || pickPaymentWeek();
  const w = pay.week;
  if (!w) return;
  setText('c04WeekBtnText', `Quyết toán ${w.ten}`);
  setText('c04ProgressLabel', `Khoa đã chi trả · ${w.ten}`);

  const [depts, pending] = await Promise.all([
    MedcareAPI.getPaymentDepartments(w.id),
    MedcareAPI.getComplaints('CHO_XU_LY')
  ]);
  const active = depts.filter(d => d.soNhanSu > 0);
  const done = active.filter(d => d.soDaThanhToan >= d.soNhanSu).length;
  setText('c04Progress', `${done} / ${active.length}`);
  const bar = document.getElementById('c04ProgressBar');
  if (bar) bar.style.width = active.length ? `${(done / active.length) * 100}%` : '0%';
  setText('c04Budget', fmtMoney(depts.reduce((s, d) => s + Number(d.tongTien || 0), 0)));
  setText('c04Complaints', `${pending.length}`);
  setText('c04ComplaintsNote', pending[0] ? `Mới nhất: ${pending[0].hoTen}` : '');

  const banner = document.getElementById('c04ComplaintBanner');
  if (banner) banner.classList.toggle('hidden', pending.length === 0);
  if (pending[0]) {
    const k = pending[0];
    setText('c04ComplaintTitle', `${pending.length} khiếu nại đang chờ xử lý`);
    setText('c04ComplaintText', `${k.hoTen}${k.tenCa ? ` · Ca ${k.tenCa} ${fmtDate(k.ngay)}` : ''} · ${k.noiDung || ''}`);
  }
};

// ==========================================
// TT01: DANH SÁCH KHOA
// ==========================================
SCREEN_LOADERS['screen-TT01'] = loadPaymentDepts;

async function loadPaymentDepts() {
  const pay = STATE.payment;
  pay.week = pay.week || pickPaymentWeek();
  setHtml('ttWeekSelect', weekOptionsHtml(pay.week?.id));
  if (!pay.week) {
    setTableMessage('ttDeptTableBody', 5, 'Chưa có tuần làm việc nào.');
    return;
  }
  setTableMessage('ttDeptTableBody', 5, 'Đang tải…');
  pay.depts = await MedcareAPI.getPaymentDepartments(pay.week.id);

  const active = pay.depts.filter(d => d.soNhanSu > 0);
  const done = active.filter(d => d.soDaThanhToan >= d.soNhanSu).length;
  setText('ttOverallProgress', `${pay.week.ten} · ${done}/${active.length} khoa đã chi trả`);

  setHtml('ttDeptTableBody', pay.depts.map(d => {
    const complete = d.soNhanSu > 0 && d.soDaThanhToan >= d.soNhanSu;
    const status = d.soNhanSu === 0 ? badge('Không có nhân sự', 'neutral')
      : complete ? badge('Hoàn tất', 'success')
      : badge(`Đã chi ${d.soDaThanhToan}/${d.soNhanSu}`, 'warning');
    return `<tr>
      <td><div class="cell-strong">${escapeHtml(d.tenKhoa)}</div><div class="cell-sub">${escapeHtml(d.maKhoa)}</div></td>
      <td class="text-center num">${d.soNhanSu}</td>
      <td class="text-right num cell-strong">${fmtMoney(d.tongTien)}</td>
      <td>${status}</td>
      <td class="text-right">
        ${d.soNhanSu > 0 ? `<button onclick="ttSelectDept(${d.khoaId})" class="btn btn-secondary btn-sm">Chi tiết</button>` : ''}
      </td>
    </tr>`;
  }).join('') || '<tr><td colspan="5" class="empty">Chưa có khoa nào.</td></tr>');
}

function ttChangeWeek(weekId) {
  STATE.payment.week = findWeek(weekId);
  loadPaymentDepts().catch(handleError);
}

function ttQuickSwitchWeek(mode) {
  let target = null;
  if (mode === 'current') {
    target = getCurrentWeek();
  } else {
    const cur = getCurrentWeek();
    const sorted = [...STATE.weeks].sort((a, b) => String(b.ngayBatDau).localeCompare(String(a.ngayBatDau)));
    target = cur ? sorted.find(w => String(w.ngayKetThuc) < String(cur.ngayBatDau)) : sorted[1] || sorted[0];
  }
  if (!target) {
    showToast('Không tìm thấy tuần phù hợp', false);
    return;
  }
  STATE.payment.week = target;
  loadPaymentDepts().catch(handleError);
}

function ttSelectDept(khoaId) {
  STATE.payment.deptId = Number(khoaId);
  if (STATE.currentScreen === 'screen-TT02') {
    SCREEN_LOADERS['screen-TT02']().catch(handleError);
  } else {
    navigateTo('screen-TT02');
  }
}

// ==========================================
// TT02: NHÂN SỰ CỦA KHOA
// ==========================================
SCREEN_LOADERS['screen-TT02'] = async () => {
  const pay = STATE.payment;
  if (!pay.deptId || !pay.week) {
    navigateTo('screen-TT01');
    return;
  }
  if (!pay.depts || !pay.depts.length) {
    pay.depts = await MedcareAPI.getPaymentDepartments(pay.week.id).catch(() => []);
  }
  const dept = pay.depts.find(d => d.khoaId === pay.deptId);
  setText('ttDeptTitle', dept ? dept.tenKhoa : '—');
  setText('ttDeptWeek', `Bảng lương · ${pay.week.ten}`);
  pay.staffFilter = pay.staffFilter || 'ALL';
  const searchInput = document.getElementById('ttStaffSearchInput');
  if (searchInput) searchInput.value = '';
  ttSetPayrollFilter(pay.staffFilter, true);
  renderTtDeptQuickTabs();
  setTableMessage('ttStaffTableBody', 7, 'Đang tải…');
  pay.staff = await MedcareAPI.getPaymentStaff(pay.week.id, pay.deptId);
  renderStaffPaymentList();
};

function ttFilterStaffPayroll() {
  renderStaffPaymentList();
}

function ttSetPayrollFilter(mode, skipRender) {
  STATE.payment.staffFilter = mode || 'ALL';
  setSegmented(['ttFilterAll', 'ttFilterUnpaid', 'ttFilterPaid', 'ttFilterComplaint'],
    { ALL: 'ttFilterAll', UNPAID: 'ttFilterUnpaid', PAID: 'ttFilterPaid', COMPLAINT: 'ttFilterComplaint' }[STATE.payment.staffFilter]);
  if (!skipRender) renderStaffPaymentList();
}

/** Chuyển sang bảng lương của khoa tiếp theo. */
function ttNextDepartment() {
  const pay = STATE.payment;
  const list = (pay.depts || []).filter(d => d.soNhanSu > 0);
  if (!list.length) return;
  const idx = list.findIndex(d => d.khoaId === pay.deptId);
  const next = list[(idx + 1) % list.length];
  if (next) ttSelectDept(next.khoaId);
}

/** Ô chọn khoa để chuyển nhanh giữa các bảng lương. */
function renderTtDeptQuickTabs() {
  const pay = STATE.payment;
  const el = document.getElementById('ttDeptQuickTabs');
  if (!el) return;
  const list = (pay.depts || []).filter(d => d.soNhanSu > 0);
  el.innerHTML = list.length
    ? `<select class="select select-sm" onchange="ttSelectDept(this.value)">${list.map(d =>
        `<option value="${d.khoaId}" ${d.khoaId === pay.deptId ? 'selected' : ''}>${escapeHtml(d.tenKhoa)}</option>`).join('')}</select>`
    : '';
}

function renderStaffPaymentList() {
  const pay = STATE.payment;
  const q = (document.getElementById('ttStaffSearchInput')?.value || '').toLowerCase().trim();
  const mode = pay.staffFilter || 'ALL';
  const filtered = (pay.staff || []).filter(ns => {
    const l = ns.luong || {};
    const paid = ns.trangThai === 'DA_THANH_TOAN';
    if (mode === 'UNPAID' && paid) return false;
    if (mode === 'PAID' && !paid) return false;
    if (mode === 'COMPLAINT' && !(l.soKhieuNaiChoXuLy > 0)) return false;
    if (q) {
      return (ns.hoTen || '').toLowerCase().includes(q)
        || (ns.maNhanSu || '').toLowerCase().includes(q)
        || (ns.chucDanh || '').toLowerCase().includes(q);
    }
    return true;
  });

  setHtml('ttStaffTableBody', filtered.map(ns => {
    const l = ns.luong;
    const paid = ns.trangThai === 'DA_THANH_TOAN';
    const hasShift = l.soCaChinh + l.soCaNgoaiGio > 0 || Number(l.tienDieuChinh) !== 0;
    let status;
    let action;
    if (paid) {
      status = badge('Đã chi', 'success');
      action = `<button onclick="ttOpenSlip(${ns.phieuId}, 'screen-TT02')" class="btn btn-secondary btn-sm">Phiếu lương</button>`;
    } else if (l.soKhieuNaiChoXuLy > 0) {
      status = badge(`${l.soKhieuNaiChoXuLy} khiếu nại`, 'warning');
      action = `<button onclick="navigateTo('screen-TT05')" class="btn btn-secondary btn-sm">Xử lý</button>`;
    } else if (hasShift) {
      status = badge('Chưa chi', 'neutral');
      action = `<button onclick="ttOpenPaymentScreen(${ns.nhanSuId})" class="btn btn-primary btn-sm">Chi trả</button>`;
    } else {
      status = badge('Không có ca', 'neutral');
      action = '';
    }
    return `<tr>
      <td>
        <div class="cell-strong">${escapeHtml(ns.hoTen)}</div>
        <div class="cell-sub">${escapeHtml(ns.maNhanSu)} · ${escapeHtml(ns.chucDanh)}${ns.hocVi ? ' · ' + escapeHtml(ns.hocVi) : ''}</div>
      </td>
      <td class="text-center num">${l.soCaChinh}</td>
      <td class="text-center num">${l.soCaNgoaiGio}${l.soCaKhongTinh ? ` <span class="cell-sub" title="Ca không được tính công">(−${l.soCaKhongTinh})</span>` : ''}</td>
      <td class="text-right num">${fmtMoney(l.donGiaCa)}</td>
      <td class="text-right num">
        <div class="cell-strong">${fmtMoney(l.tongThucLinh)}</div>
        ${Number(l.tienDieuChinh) !== 0 ? `<div class="cell-sub">gồm điều chỉnh ${fmtMoney(l.tienDieuChinh)}</div>` : ''}
      </td>
      <td>${status}</td>
      <td class="text-right">
        <div class="row-actions">
          <button onclick="ttViewTimesheet(${ns.nhanSuId})" class="btn btn-ghost btn-sm">Chấm công</button>
          ${action || '<span></span>'}
        </div>
      </td>
    </tr>`;
  }).join('') || '<tr><td colspan="7" class="empty">Không có nhân sự phù hợp.</td></tr>');
}

// ==========================================
// TT03: BẢNG CHẤM CÔNG
// ==========================================
function ttViewTimesheet(nhanSuId) {
  const pay = STATE.payment;
  const ns = pay.staff.find(s => s.nhanSuId === nhanSuId);
  const dept = pay.depts.find(d => d.khoaId === pay.deptId);
  pay.timesheetOwner = { nhanSuId, hoTen: ns.hoTen, maNhanSu: ns.maNhanSu, tenKhoa: dept?.tenKhoa };
  pay.timesheetWeek = pay.week;
  navigateTo('screen-TT03');
}

/** Nhân sự xem bảng chấm công của chính mình. */
function ttOpenMyTimesheet() {
  const u = STATE.user;
  STATE.payment.timesheetOwner = { nhanSuId: u.id, hoTen: u.fullName, maNhanSu: u.maNhanSu, tenKhoa: u.tenKhoa };
  STATE.payment.timesheetWeek = pickPaymentWeek();
  navigateTo('screen-TT03');
}

SCREEN_LOADERS['screen-TT03'] = async () => {
  const pay = STATE.payment;
  const isStaff = STATE.user.vaiTro === 'NHAN_SU';
  if (isStaff && (!pay.timesheetOwner || pay.timesheetOwner.nhanSuId !== STATE.user.id)) {
    const u = STATE.user;
    pay.timesheetOwner = { nhanSuId: u.id, hoTen: u.fullName, maNhanSu: u.maNhanSu, tenKhoa: u.tenKhoa };
  }
  if (!pay.timesheetOwner) {
    navigateTo('screen-TT01');
    return;
  }
  const back = document.getElementById('ttTsBack');
  if (back) back.classList.toggle('hidden', isStaff);
  pay.timesheetWeek = pay.timesheetWeek || pickPaymentWeek();
  setHtml('ttTimesheetWeek', weekOptionsHtml(pay.timesheetWeek?.id));
  setSegmented(['ttTsFilterAll', 'ttTsFilterIssue'], pay.timesheetFilter === 'ISSUE' ? 'ttTsFilterIssue' : 'ttTsFilterAll');
  await loadTimesheet();
};

function ttChangeTimesheetWeek(weekId) {
  STATE.payment.timesheetWeek = findWeek(weekId);
  loadTimesheet().catch(handleError);
}

async function loadTimesheet() {
  const pay = STATE.payment;
  const o = pay.timesheetOwner;
  const w = pay.timesheetWeek;
  const isStaff = STATE.user.vaiTro === 'NHAN_SU';
  setText('ttTimesheetStaffHeader', isStaff ? 'Chấm công' : o.hoTen);
  setText('ttTimesheetSub', [isStaff ? null : o.maNhanSu, o.tenKhoa, w ? `${w.ten} · ${fmtDateShort(w.ngayBatDau)} – ${fmtDateShort(w.ngayKetThuc)}` : null].filter(Boolean).join(' · '));
  if (!w) {
    setTableMessage('ttTimesheetRecordsBody', 7, 'Chưa có tuần làm việc nào.');
    return;
  }
  setTableMessage('ttTimesheetRecordsBody', 7, 'Đang tải…');
  pay.timesheet = await MedcareAPI.getTimesheet(o.nhanSuId, w.id);
  renderTimesheetRecords();
}

function chamCongBadge(rec) {
  const label = TRANG_THAI_CHAM_CONG[rec.trangThai] || rec.trangThai;
  const tone = rec.thieuQuetThe ? 'danger'
    : rec.trangThai === 'DUNG_GIO' ? 'success'
    : rec.trangThai === 'CHUA_CO_DU_LIEU' ? 'neutral'
    : 'warning';
  return badge(label, tone);
}

function timesheetAction(rec) {
  const isStaff = STATE.user.vaiTro === 'NHAN_SU';
  const kn = rec.trangThaiKhieuNai;
  const knBadge = kn ? badge(`Khiếu nại: ${(TRANG_THAI_KHIEU_NAI[kn] || kn).toLowerCase()}`, KN_TONE[kn] || 'neutral') : '';

  if (isStaff) {
    if (kn === 'CHO_XU_LY') return knBadge;
    const issue = rec.thieuQuetThe || !rec.duocTinhCong || (rec.trangThai !== 'DUNG_GIO' && rec.trangThai !== 'CHUA_CO_DU_LIEU');
    return `<div class="flex items-center justify-end gap-2">${knBadge}
      <button onclick="ttOpenComplaintForm(${rec.phanCongCaId})" class="btn ${issue ? 'btn-secondary' : 'btn-ghost row-action'} btn-sm">Khiếu nại</button></div>`;
  }
  if (kn === 'CHO_XU_LY') {
    return `<button onclick="ttOpenComplaint(${rec.khieuNaiId})" class="btn btn-secondary btn-sm">Đối soát</button>`;
  }
  if (kn) return knBadge;
  return rec.duocTinhCong ? '' : '<span class="cell-sub">Không tính công</span>';
}

function ttSetTimesheetFilter(mode) {
  STATE.payment.timesheetFilter = mode || 'ALL';
  setSegmented(['ttTsFilterAll', 'ttTsFilterIssue'], STATE.payment.timesheetFilter === 'ISSUE' ? 'ttTsFilterIssue' : 'ttTsFilterAll');
  renderTimesheetRecords();
}

function renderTimesheetRecords() {
  const pay = STATE.payment;
  const all = pay.timesheet || [];
  const validCount = all.filter(r => r.duocTinhCong).length;
  const issueCount = all.filter(r => !r.duocTinhCong || r.thieuQuetThe || (r.trangThai !== 'DUNG_GIO' && r.trangThai !== 'CHUA_CO_DU_LIEU')).length;
  const knCount = all.filter(r => r.trangThaiKhieuNai).length;

  const stat = (label, value) => `<div class="card stat"><div class="stat-label">${label}</div><div class="stat-value">${value}</div></div>`;
  setHtml('ttTimesheetSummaryStrip',
    stat('Ca phân công', all.length) + stat('Ca tính công', validCount) + stat('Ca có vấn đề', issueCount) + stat('Khiếu nại', knCount));

  const filterMode = pay.timesheetFilter || 'ALL';
  const rows = filterMode === 'ISSUE'
    ? all.filter(r => !r.duocTinhCong || r.thieuQuetThe || (r.trangThai !== 'DUNG_GIO' && r.trangThai !== 'CHUA_CO_DU_LIEU') || r.trangThaiKhieuNai)
    : all;

  const isIssue = rec => rec.thieuQuetThe || !rec.duocTinhCong || (rec.trangThai !== 'DUNG_GIO' && rec.trangThai !== 'CHUA_CO_DU_LIEU');
  setHtml('ttTimesheetRecordsBody', rows.map(rec => `
    <tr>
      <td class="cell-strong">${escapeHtml(THU_LABEL[rec.thu] || '')} <span class="cell-sub num">${fmtDateShort(rec.ngay)}</span></td>
      <td>${escapeHtml(rec.tenCa)} <span class="cell-sub num">${escapeHtml(rec.thoiGian)}</span>${rec.loaiCa === 'NGOAI_GIO' ? ' <span class="cell-sub">· ngoài giờ</span>' : ''}</td>
      <td class="text-center num">${Number(rec.heSo || 1).toFixed(1)}</td>
      <td class="text-center num">${escapeHtml(rec.gioCheckIn)}</td>
      <td class="text-center num">${escapeHtml(rec.gioCheckOut)}</td>
      <td>${chamCongBadge(rec)}${rec.ghiChu && isIssue(rec) ? `<div class="cell-sub mt-1">${escapeHtml(rec.ghiChu)}</div>` : ''}</td>
      <td class="text-right">${timesheetAction(rec)}</td>
    </tr>`).join('') || '<tr><td colspan="7" class="empty">Không có ca trực nào.</td></tr>');
}

// ==========================================
// TT04: GỬI KHIẾU NẠI (Nhân sự)
// ==========================================
function ttOpenComplaintForm(phanCongCaId) {
  const rec = STATE.payment.timesheet.find(r => r.phanCongCaId === phanCongCaId);
  STATE.payment.complaintRow = rec;
  const shift = document.getElementById('ttComplaintShift');
  if (shift) shift.value = `${THU_LABEL[rec.thu] || ''} ${fmtDate(rec.ngay)} · Ca ${rec.tenCa} (${rec.thoiGian})`;
  setInput('ttComplaintContent', '');
  setInput('ttComplaintProof', '');
  openModal('modal-TT04');
}

async function ttSubmitComplaint(e) {
  if (e) e.preventDefault();
  const rec = STATE.payment.complaintRow;
  if (!rec) return;
  try {
    const res = await MedcareAPI.submitComplaint({
      phanCongCaId: rec.phanCongCaId,
      noiDung: document.getElementById('ttComplaintContent')?.value.trim(),
      duongDanMinhChung: document.getElementById('ttComplaintProof')?.value.trim() || null
    });
    closeModal('modal-TT04');
    showToast(res.message || 'Đã gửi khiếu nại');
    await loadTimesheet();
  } catch (err) {
    handleError(err);
  }
}

// ==========================================
// TT05: ĐỐI SOÁT & XỬ LÝ KHIẾU NẠI (Kế toán)
// ==========================================
function ttOpenComplaint(khieuNaiId) {
  STATE.payment.complaintId = khieuNaiId;
  navigateTo('screen-TT05');
}

SCREEN_LOADERS['screen-TT05'] = loadComplaintQueue;

function ttChangeComplaintFilter(value) {
  STATE.payment.complaintFilter = value;
  loadComplaintQueue().catch(handleError);
}

async function loadComplaintQueue() {
  const pay = STATE.payment;
  const filter = document.getElementById('ttComplaintFilter');
  if (filter) filter.value = pay.complaintFilter;
  setTableMessage('ttComplaintListBody', 5, 'Đang tải…');
  pay.complaints = await MedcareAPI.getComplaints(pay.complaintFilter || null);

  if (pay.complaintId && !pay.complaints.find(k => k.id === pay.complaintId)) {
    try { pay.complaints.unshift(await MedcareAPI.getComplaint(pay.complaintId)); } catch (e) { pay.complaintId = null; }
  }
  if (!pay.complaintId && pay.complaints.length) pay.complaintId = pay.complaints[0].id;
  renderComplaintQueue();
  renderComplaintDetail();
}

function renderComplaintQueue() {
  const pay = STATE.payment;
  setHtml('ttComplaintListBody', pay.complaints.map(k => `
    <tr onclick="ttSelectComplaint(${k.id})" class="cursor-pointer ${k.id === pay.complaintId ? 'is-selected' : ''}">
      <td class="mono">${escapeHtml(k.maYeuCau)}</td>
      <td><div class="cell-strong">${escapeHtml(k.hoTen || '—')}</div><div class="cell-sub">${escapeHtml(k.maNhanSu || '')}</div></td>
      <td>${k.tenCa ? `${escapeHtml(k.tenCa)} <span class="cell-sub num">${fmtDate(k.ngay)}</span>` : '<span class="cell-sub">—</span>'}</td>
      <td class="num cell-sub">${fmtDateTime(k.ngayGui)}</td>
      <td>${badge(TRANG_THAI_KHIEU_NAI[k.trangThai] || k.trangThai, KN_TONE[k.trangThai] || 'neutral')}</td>
    </tr>`).join('') || '<tr><td colspan="5" class="empty">Không có khiếu nại nào.</td></tr>');
}

function ttSelectComplaint(id) {
  STATE.payment.complaintId = id;
  renderComplaintQueue();
  renderComplaintDetail();
}

function renderComplaintDetail() {
  const pay = STATE.payment;
  const k = pay.complaints.find(x => x.id === pay.complaintId);
  const panel = document.getElementById('ttComplaintDetail');
  if (panel) panel.classList.toggle('hidden', !k);
  if (!k) return;

  setText('kn5Header', `${k.hoTen || ''} · ${k.maYeuCau}`);
  setText('kn5Time', k.tenCa ? `${THU_LABEL[k.thu] || ''} ${fmtDate(k.ngay)} · Ca ${k.tenCa} (${k.thoiGian})` : '—');
  setText('kn5Assign', k.vaiTro ? `${VAI_TRO_LABEL[k.vaiTro] || k.vaiTro}${k.dieuDongNgoaiDangKy ? ' (điều động)' : ''}` : '—');
  setText('kn5Type', k.loaiCa ? `${k.loaiCa === 'NGOAI_GIO' ? 'Ngoài giờ' : 'Chính'} · ×${Number(k.heSo || 1).toFixed(1)}` : '—');
  setText('kn5Week', k.tenTuan || '—');
  setText('kn5Check', k.trangThaiChamCong
    ? `${TRANG_THAI_CHAM_CONG[k.trangThaiChamCong] || k.trangThaiChamCong} (${k.gioCheckIn || '--:--'} – ${k.gioCheckOut || '--:--'})`
    : 'Không có dữ liệu');
  setText('kn5Note', k.ghiChuChamCong || '—');
  setText('kn5Content', k.noiDung || '—');
  setText('kn5Proof', k.minhChung || '—');

  const open = k.trangThai === 'CHO_XU_LY';
  document.querySelectorAll('#ttAdjustForm input, #ttAdjustForm select, #ttAdjustForm textarea, #ttAdjustForm button')
    .forEach(el => { el.disabled = !open; });

  if (open) {
    const typeSel = document.getElementById('ttAdjType');
    if (typeSel) typeSel.value = k.loaiCa === 'CHINH' ? 'chinh' : 'ngoai-gio';
    setInput('ttAdjShiftCount', 1);
    setInput('ttAdjBonus', 0);
    setInput('ttAdjPenalty', 0);
    setInput('ttAdjReason', '');
    setText('ttAdjResult', '');
  } else {
    const dc = k.dieuChinh;
    setText('ttAdjResult', k.trangThai === 'DA_XU_LY'
      ? `Đã duyệt ${fmtDateTime(k.ngayXuLy)} bởi ${k.nguoiXuLy || '—'}: +${dc?.soCaChinh || 0} ca chính, +${dc?.soCaNgoaiGio || 0} ca ngoài giờ, điều chỉnh ${fmtMoney(dc?.soTien)}. ${k.lyDoPhanHoi || ''}`
      : `Đã từ chối ${fmtDateTime(k.ngayXuLy)} bởi ${k.nguoiXuLy || '—'}. ${k.lyDoPhanHoi || ''}`);
  }
}

function setInput(id, value) {
  const el = document.getElementById(id);
  if (el) el.value = value;
}

async function ttSaveAdjustment() {
  const pay = STATE.payment;
  const k = pay.complaints.find(x => x.id === pay.complaintId);
  if (!k) return;
  const type = document.getElementById('ttAdjType')?.value;
  const count = parseInt(document.getElementById('ttAdjShiftCount')?.value || '0', 10) || 0;
  try {
    const res = await MedcareAPI.saveAdjustment({
      donKhieuNaiId: k.id,
      soCaChinhDieuChinh: type === 'chinh' ? count : 0,
      soCaNgoaiGioDieuChinh: type === 'ngoai-gio' ? count : 0,
      phuCapBoSung: Number(document.getElementById('ttAdjBonus')?.value || 0),
      khauTru: Number(document.getElementById('ttAdjPenalty')?.value || 0),
      lyDo: document.getElementById('ttAdjReason')?.value.trim()
    });
    showToast(`Đã duyệt điều chỉnh ${fmtMoney(res.data.dieuChinh?.soTien)}`);
    await loadComplaintQueue();
  } catch (err) {
    handleError(err);
  }
}

async function ttRejectComplaint() {
  const pay = STATE.payment;
  const k = pay.complaints.find(x => x.id === pay.complaintId);
  if (!k) return;
  const lyDo = document.getElementById('ttAdjReason')?.value.trim();
  if (!lyDo) {
    showToast('Nhập kết luận trước khi từ chối', false);
    document.getElementById('ttAdjReason')?.focus();
    return;
  }
  try {
    const res = await MedcareAPI.rejectComplaint(k.id, lyDo);
    showToast(res.message || 'Đã từ chối khiếu nại');
    await loadComplaintQueue();
  } catch (err) {
    handleError(err);
  }
}

// ==========================================
// TT06: CHI TRẢ
// ==========================================
function ttOpenPaymentScreen(nhanSuId) {
  STATE.payment.payTarget = STATE.payment.staff.find(s => s.nhanSuId === nhanSuId);
  navigateTo('screen-TT06');
}

SCREEN_LOADERS['screen-TT06'] = () => {
  const pay = STATE.payment;
  const ns = pay.payTarget;
  if (!ns) {
    navigateTo('screen-TT01');
    return;
  }
  const dept = pay.depts.find(d => d.khoaId === pay.deptId);
  const l = ns.luong;
  setText('ttPayStaffName', `${ns.hoTen} · ${ns.maNhanSu}`);
  setText('ttPayDept', `${dept?.tenKhoa || ''} · ${pay.week.ten}`);
  setText('ttPayBreakdown', `${l.soCaChinh} × ${fmtMoney(l.donGiaCa)} + ${l.soCaNgoaiGio} × ${fmtMoney(l.donGiaCa)} × ${Number(l.heSoNgoaiGio).toFixed(1)}`
    + (Number(l.tienDieuChinh) !== 0 ? ` + ${fmtMoney(l.tienDieuChinh)} điều chỉnh` : ''));
  setText('ttPayFinalAmount', fmtMoney(l.tongThucLinh));
  setText('payBankNote', `CLINIC ${pay.week.ten} ${ns.maNhanSu}`);
  selectPaymentMethod('online');
};

function ttGenerateTxnCode(silent) {
  const ns = STATE.payment.payTarget;
  const isOnline = document.getElementById('payRadioOnline')?.checked !== false;
  const prefix = isOnline ? 'CK' : 'TM';
  const rand = Math.floor(100000 + Math.random() * 900000);
  const code = `${prefix}-${ns ? ns.maNhanSu : 'NS'}-${rand}`;
  setInput('payTxnCode', code);
  if (!silent) showToast(`Mã giao dịch: ${code}`);
}

function selectPaymentMethod(method) {
  const online = method === 'online';
  const rOnline = document.getElementById('payRadioOnline');
  const rCash = document.getElementById('payRadioCash');
  if (rOnline) rOnline.checked = online;
  if (rCash) rCash.checked = !online;
  document.getElementById('payOptionOnline')?.classList.toggle('is-active', online);
  document.getElementById('payOptionCash')?.classList.toggle('is-active', !online);
  ttGenerateTxnCode(true);
}

async function executePayment() {
  const pay = STATE.payment;
  const ns = pay.payTarget;
  if (!ns) return;
  const isOnline = document.getElementById('payRadioOnline')?.checked;
  try {
    const res = await MedcareAPI.executePayment({
      nhanSuId: ns.nhanSuId,
      tuanId: pay.week.id,
      hinhThucThanhToan: isOnline ? 'TRUC_TUYEN' : 'TRUC_TIEP',
      maGiaoDich: document.getElementById('payTxnCode')?.value.trim() || null
    });
    showToast(res.message || 'Đã chi trả');
    pay.slip = res.data;
    pay.slipBackScreen = 'screen-TT02';
    navigateTo('screen-TT07');
  } catch (err) {
    handleError(err);
  }
}

// ==========================================
// TT07: PHIẾU LƯƠNG
// ==========================================
async function ttOpenSlip(phieuId, backScreen) {
  try {
    STATE.payment.slip = await MedcareAPI.getSlip(phieuId);
    STATE.payment.slipBackScreen = backScreen;
    navigateTo('screen-TT07');
  } catch (err) {
    handleError(err);
  }
}

SCREEN_LOADERS['screen-TT07'] = () => {
  const p = STATE.payment.slip;
  if (!p) {
    navigateTo(homeScreen());
    return;
  }
  const paid = p.trangThai === 'DA_THANH_TOAN';
  setText('tt7Ma', p.maPhieu);
  setHtml('tt7Status', paid
    ? badge(`Đã thanh toán${p.ngayThanhToan ? ' · ' + fmtDate(p.ngayThanhToan) : ''}`, 'success')
    : badge('Chưa thanh toán', 'warning'));

  const hinhThuc = p.hinhThucThanhToan === 'TRUC_TUYEN'
    ? `Chuyển khoản${p.nganHangNhan ? ' · ' + p.nganHangNhan : ''}`
    : 'Tiền mặt';
  const row = (label, qty, price, coef, amount, sub) => `
    <tr>
      <td>${label}${sub ? `<div class="cell-sub">${sub}</div>` : ''}</td>
      <td class="text-center num">${qty}</td>
      <td class="text-right num">${price}</td>
      <td class="text-center num">${coef}</td>
      <td class="text-right num cell-strong">${amount}</td>
    </tr>`;
  const dcRows = (p.dieuChinh || []).map(d => row(
    `Điều chỉnh theo khiếu nại ${escapeHtml(d.maYeuCau)}`,
    [d.soCaChinh ? `+${d.soCaChinh} chính` : '', d.soCaNgoaiGio ? `+${d.soCaNgoaiGio} ngoài giờ` : ''].filter(Boolean).join(', ') || '—',
    '—', '—', `${Number(d.soTien) >= 0 ? '+' : ''}${fmtMoney(d.soTien)}`, escapeHtml(d.lyDo || ''))).join('');
  const phuCap = Number(p.phuCapTrachNhiem || 0) + Number(p.thuong || 0);

  setHtml('printable-receipt', `
    <div class="flex items-start justify-between gap-6 pb-6" style="border-bottom:1px solid var(--hairline)">
      <div>
        <div class="flex items-center gap-2 text-ink"><img src="img/logo-mark.svg" alt="" width="24" height="24"><span class="brand-name">Clinic</span></div>
        <h2 class="text-xl font-semibold mt-4">Phiếu chi trả thù lao</h2>
        <div class="text-muted mt-1">${escapeHtml(p.tenTuan)} · ${fmtDate(p.tuNgay)} – ${fmtDate(p.denNgay)}</div>
      </div>
      <dl class="text-right text-[13px] space-y-1">
        <div><span class="text-muted">Số phiếu </span><span class="mono text-ink">${escapeHtml(p.maPhieu)}</span></div>
        <div><span class="text-muted">Ngày chi </span><span class="num text-ink">${p.ngayThanhToan ? fmtDate(p.ngayThanhToan) : '—'}</span></div>
        ${p.maGiaoDich ? `<div><span class="text-muted">Mã giao dịch </span><span class="mono text-ink">${escapeHtml(p.maGiaoDich)}</span></div>` : ''}
      </dl>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 py-6">
      <dl class="kv" style="grid-template-columns:110px 1fr">
        <dt>Họ và tên</dt><dd class="font-medium">${escapeHtml(p.hoTen)}</dd>
        <dt>Mã nhân sự</dt><dd class="mono">${escapeHtml(p.maNhanSu)}</dd>
        <dt>Chức danh</dt><dd>${escapeHtml(p.chucDanh)}${p.hocVi ? ' · ' + escapeHtml(p.hocVi) : ''}</dd>
      </dl>
      <dl class="kv" style="grid-template-columns:110px 1fr">
        <dt>Khoa</dt><dd>${escapeHtml(p.tenKhoa || '—')}</dd>
        <dt>Hình thức</dt><dd>${escapeHtml(hinhThuc)}</dd>
        ${p.hinhThucThanhToan === 'TRUC_TUYEN' && p.soTaiKhoanNhan ? `<dt>Tài khoản</dt><dd class="mono">${escapeHtml(p.soTaiKhoanNhan)}${p.chuTaiKhoanNhan ? ' · ' + escapeHtml(p.chuTaiKhoanNhan) : ''}</dd>` : ''}
      </dl>
    </div>

    <table class="table" style="border:1px solid var(--hairline)">
      <thead>
        <tr>
          <th>Khoản mục</th>
          <th class="text-center">Số ca</th>
          <th class="text-right">Đơn giá</th>
          <th class="text-center">Hệ số</th>
          <th class="text-right">Thành tiền</th>
        </tr>
      </thead>
      <tbody>
        ${row('Ca chính', p.soCaChinh, fmtMoney(p.donGiaCa), '1.0', fmtMoney(p.tienCaChinh))}
        ${row('Ca ngoài giờ', p.soCaNgoaiGio, fmtMoney(p.donGiaCa), Number(p.heSoNgoaiGio).toFixed(1), fmtMoney(p.tienCaNgoaiGio))}
        ${dcRows}
        ${phuCap ? row('Phụ cấp / thưởng', '—', '—', '—', fmtMoney(phuCap)) : ''}
        ${Number(p.phat || 0) ? row('Khấu trừ', '—', '—', '—', `−${fmtMoney(p.phat)}`) : ''}
      </tbody>
    </table>

    <div class="flex items-baseline justify-between pt-5">
      <span class="text-muted">Tổng thực lĩnh</span>
      <span class="text-2xl font-semibold text-ink num">${fmtMoney(p.tongThucLinh)}</span>
    </div>
    <div class="text-right text-[13px] text-muted mt-1">${escapeHtml(numberToVietnameseWords(p.tongThucLinh))} chẵn</div>

    <div class="grid grid-cols-2 gap-6 pt-12 text-center text-[13px]">
      <div>
        <div class="font-medium text-ink">Người lập phiếu</div>
        <div class="mt-14 text-ink">${escapeHtml(p.tenKeToan || '')}</div>
      </div>
      <div>
        <div class="font-medium text-ink">Người nhận</div>
        <div class="mt-14 text-ink">${escapeHtml(p.hoTen)}</div>
      </div>
    </div>`);
};

// ==========================================
// TT08: PHIẾU LƯƠNG CỦA TÔI (Nhân sự)
// ==========================================
SCREEN_LOADERS['screen-TT08'] = async () => {
  const u = STATE.user;
  setText('tt8Sub', [u.maNhanSu, u.tenKhoa].filter(Boolean).join(' · '));
  setTableMessage('tt8Body', 7, 'Đang tải…');
  const slips = await MedcareAPI.getMySlips();
  STATE.payment.mySlips = slips;
  setHtml('tt8Body', slips.map(p => `
    <tr>
      <td class="mono">${escapeHtml(p.maPhieu)}</td>
      <td><div class="cell-strong">${escapeHtml(p.tenTuan)}</div><div class="cell-sub num">${fmtDateShort(p.tuNgay)} – ${fmtDate(p.denNgay)}</div></td>
      <td class="text-center num">${p.soCaChinh + p.soCaNgoaiGio}</td>
      <td class="text-right num cell-strong">${fmtMoney(p.tongThucLinh)}</td>
      <td>${p.hinhThucThanhToan === 'TRUC_TUYEN' ? 'Chuyển khoản' : 'Tiền mặt'}</td>
      <td class="num cell-sub">${p.ngayThanhToan ? fmtDate(p.ngayThanhToan) : '—'}</td>
      <td class="text-right"><button onclick="ttViewMySlip(${p.id})" class="btn btn-secondary btn-sm">Xem</button></td>
    </tr>`).join('') || '<tr><td colspan="7" class="empty">Chưa có phiếu lương.</td></tr>');
};

function ttViewMySlip(id) {
  STATE.payment.slip = (STATE.payment.mySlips || []).find(p => p.id === id);
  STATE.payment.slipBackScreen = 'screen-TT08';
  navigateTo('screen-TT07');
}

// ==========================================
// TT09: PHIẾU LƯƠNG – DANH SÁCH TOÀN VIỆN (Kế toán)
// ==========================================
SCREEN_LOADERS['screen-TT09'] = loadWeeklySlips;

async function loadWeeklySlips() {
  const pay = STATE.payment;
  pay.slipWeek = pay.slipWeek || pay.week || pickPaymentWeek();
  pay.slipFilter = pay.slipFilter || 'ALL';
  setHtml('tt9WeekSelect', weekOptionsHtml(pay.slipWeek?.id));
  tt9SetFilter(pay.slipFilter, true);
  if (!pay.slipWeek) {
    setTableMessage('tt9Body', 8, 'Chưa có tuần làm việc nào.');
    return;
  }
  setText('tt9Meta', `${pay.slipWeek.ten} · ${fmtDate(pay.slipWeek.ngayBatDau)} – ${fmtDate(pay.slipWeek.ngayKetThuc)}`);
  setTableMessage('tt9Body', 8, 'Đang tải…');
  pay.weeklySlips = await MedcareAPI.getWeeklySlips(pay.slipWeek.id);

  const paid = pay.weeklySlips.filter(p => p.trangThai === 'DA_THANH_TOAN');
  const online = paid.filter(p => p.hinhThucThanhToan === 'TRUC_TUYEN').length;
  setText('tt9Count', `${pay.weeklySlips.length}`);
  setText('tt9Total', fmtMoney(paid.reduce((sum, p) => sum + Number(p.tongThucLinh || 0), 0)));
  setText('tt9Methods', `${online} / ${paid.length - online}`);
  renderTt9List();
}

function tt9ChangeWeek(weekId) {
  STATE.payment.slipWeek = findWeek(weekId);
  loadWeeklySlips().catch(handleError);
}

function tt9SetFilter(mode, skipRender) {
  STATE.payment.slipFilter = mode || 'ALL';
  setSegmented(['tt9FilterAll', 'tt9FilterOnline', 'tt9FilterCash'],
    { ALL: 'tt9FilterAll', TRUC_TUYEN: 'tt9FilterOnline', TRUC_TIEP: 'tt9FilterCash' }[STATE.payment.slipFilter]);
  if (!skipRender) renderTt9List();
}

function renderTt9List() {
  const pay = STATE.payment;
  const q = (document.getElementById('tt9Search')?.value || '').toLowerCase().trim();
  const mode = pay.slipFilter || 'ALL';
  const rows = (pay.weeklySlips || []).filter(p => {
    if (mode !== 'ALL' && p.hinhThucThanhToan !== mode) return false;
    if (!q) return true;
    return [p.maPhieu, p.hoTen, p.maNhanSu, p.tenKhoa].some(v => String(v || '').toLowerCase().includes(q));
  });
  setHtml('tt9Body', rows.map(p => `
    <tr>
      <td class="mono">${escapeHtml(p.maPhieu)}</td>
      <td><div class="cell-strong">${escapeHtml(p.hoTen)}</div><div class="cell-sub">${escapeHtml(p.maNhanSu)} · ${escapeHtml(p.chucDanh || '')}</div></td>
      <td>${escapeHtml(p.tenKhoa || '—')}</td>
      <td class="text-center num">${p.soCaChinh + p.soCaNgoaiGio}</td>
      <td class="text-right num cell-strong">${fmtMoney(p.tongThucLinh)}</td>
      <td>${p.hinhThucThanhToan === 'TRUC_TUYEN' ? 'Chuyển khoản' : 'Tiền mặt'}</td>
      <td class="num cell-sub">${p.ngayThanhToan ? fmtDate(p.ngayThanhToan) : '—'}</td>
      <td class="text-right"><button onclick="tt9ViewSlip(${p.id})" class="btn btn-secondary btn-sm">Xem</button></td>
    </tr>`).join('') || '<tr><td colspan="8" class="empty">Không có phiếu lương nào.</td></tr>');
}

function tt9ViewSlip(id) {
  const p = (STATE.payment.weeklySlips || []).find(x => x.id === id);
  if (!p) return;
  STATE.payment.slip = p;
  STATE.payment.slipBackScreen = 'screen-TT09';
  navigateTo('screen-TT07');
}
