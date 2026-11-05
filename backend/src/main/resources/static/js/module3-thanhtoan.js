/**
 * MEDCARE CLINIC - MODULE 3: CHẤM CÔNG, KHIẾU NẠI & THANH TOÁN THÙ LAO
 * Kế toán: TT01 → TT02 → (TT03 / TT05 / TT06 → TT07)
 * Nhân sự: TT03 (chấm công của tôi, gửi khiếu nại TT04), TT08 → TT07
 */

// ==========================================
// TRANG CHỦ KẾ TOÁN (C04)
// ==========================================
SCREEN_LOADERS['screen-C04'] = async () => {
  const u = STATE.user;
  setText('c04Name', displayName(u));
  setText('c04Ma', `Mã NV: ${u.maNhanSu}`);
  const pay = STATE.payment;
  pay.week = pay.week || pickPaymentWeek();
  const w = pay.week;
  if (!w) return;
  setText('c04WeekBtnText', `Quyết toán thù lao ${w.ten}`);
  setText('c04ProgressLabel', `Tiến độ thanh toán ${w.ten}`);

  const [depts, pending] = await Promise.all([
    MedcareAPI.getPaymentDepartments(w.id),
    MedcareAPI.getComplaints('CHO_XU_LY')
  ]);
  const active = depts.filter(d => d.soNhanSu > 0);
  const done = active.filter(d => d.soDaThanhToan >= d.soNhanSu).length;
  setText('c04Progress', `${done} / ${active.length} Khoa`);
  const bar = document.getElementById('c04ProgressBar');
  if (bar) bar.style.width = active.length ? `${(done / active.length) * 100}%` : '0%';
  setText('c04Budget', fmtMoney(depts.reduce((s, d) => s + Number(d.tongTien || 0), 0)));
  setText('c04Complaints', `${pending.length} Đơn chờ`);
  setText('c04ComplaintsNote', pending[0] ? `Mới nhất: ${pending[0].hoTen} (${pending[0].maNhanSu})` : 'Không có khiếu nại mới');

  const banner = document.getElementById('c04ComplaintBanner');
  if (banner) banner.classList.toggle('hidden', pending.length === 0);
  if (pending[0]) {
    const k = pending[0];
    setText('c04ComplaintTitle', `Có ${pending.length} đơn khiếu nại chưa xử lý – mới nhất từ ${k.hoTen} (${k.maNhanSu})`);
    setText('c04ComplaintText', `Nội dung: "${k.noiDung || ''}"${k.tenCa ? ` • Ca ${k.tenCa} ${fmtDate(k.ngay)}` : ''}`);
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
    setTableMessage('ttDeptTableBody', 6, 'Chưa có tuần làm việc nào.');
    return;
  }
  setTableMessage('ttDeptTableBody', 6, 'Đang tính thù lao theo khoa...');
  pay.depts = await MedcareAPI.getPaymentDepartments(pay.week.id);

  const active = pay.depts.filter(d => d.soNhanSu > 0);
  const done = active.filter(d => d.soDaThanhToan >= d.soNhanSu).length;
  setText('ttOverallProgress', `${done} / ${active.length} đã hoàn tất`);

  setHtml('ttDeptTableBody', pay.depts.map(d => {
    const complete = d.soNhanSu > 0 && d.soDaThanhToan >= d.soNhanSu;
    const status = d.soNhanSu === 0 ? ['Không có nhân sự', 'bg-slate-100 text-slate-500']
      : complete ? ['Đã hoàn tất', 'bg-emerald-100 text-emerald-800']
      : [`Chờ quyết toán (${d.soDaThanhToan}/${d.soNhanSu})`, 'bg-amber-100 text-amber-800'];
    return `<tr class="hover:bg-slate-50/80 transition">
      <td class="py-3.5 px-4 font-mono font-bold text-sky-700">${escapeHtml(d.maKhoa)}</td>
      <td class="py-3.5 px-4 font-semibold text-slate-800">${escapeHtml(d.tenKhoa)}</td>
      <td class="py-3.5 px-4 text-center font-semibold">${d.soNhanSu} nhân sự</td>
      <td class="py-3.5 px-4 text-right font-bold text-slate-800">${fmtMoney(d.tongTien)}</td>
      <td class="py-3.5 px-4"><span class="text-[10px] font-bold px-2 py-0.5 rounded ${status[1]}">${status[0]}</span></td>
      <td class="py-3.5 px-4 text-center">
        ${d.soNhanSu > 0 ? `<button onclick="ttSelectDept(${d.khoaId})" class="px-3 py-1 bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200 rounded text-xs font-semibold">Chi tiết &rarr;</button>` : ''}
      </td>
    </tr>`;
  }).join('') || '<tr><td colspan="6" class="py-6 text-center text-slate-400 italic">Chưa có khoa nào.</td></tr>');
}

function ttChangeWeek(weekId) {
  STATE.payment.week = findWeek(weekId);
  loadPaymentDepts().catch(handleError);
}

function ttSelectDept(khoaId) {
  STATE.payment.deptId = khoaId;
  navigateTo('screen-TT02');
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
  const dept = pay.depts.find(d => d.khoaId === pay.deptId);
  setText('ttDeptTitle', dept ? `${dept.tenKhoa} (${dept.maKhoa})` : '');
  setText('ttDeptWeek', pay.week.ten);
  setTableMessage('ttStaffTableBody', 12, 'Đang tính thù lao nhân sự...');
  pay.staff = await MedcareAPI.getPaymentStaff(pay.week.id, pay.deptId);
  renderStaffPaymentList();
};

function renderStaffPaymentList() {
  const pay = STATE.payment;
  setHtml('ttStaffTableBody', pay.staff.map(ns => {
    const l = ns.luong;
    const paid = ns.trangThai === 'DA_THANH_TOAN';
    const hasShift = l.soCaChinh + l.soCaNgoaiGio > 0 || Number(l.tienDieuChinh) !== 0;
    let action;
    if (paid) {
      action = `<button onclick="ttOpenSlip(${ns.phieuId}, 'screen-TT02')" class="px-2.5 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200 rounded text-xs font-semibold">Phiếu lương</button>`;
    } else if (l.soKhieuNaiChoXuLy > 0) {
      action = `<button onclick="navigateTo('screen-TT05')" class="px-2.5 py-1 bg-amber-500 hover:bg-amber-600 text-white rounded text-xs font-semibold">Xử lý KN</button>`;
    } else if (hasShift) {
      action = `<button onclick="ttOpenPaymentScreen(${ns.nhanSuId})" class="px-2.5 py-1 bg-sky-600 hover:bg-sky-700 text-white rounded text-xs font-semibold shadow-sm">Chi trả</button>`;
    } else {
      action = '<span class="text-[11px] text-slate-400">Không có ca</span>';
    }
    return `<tr class="hover:bg-slate-50/80 transition whitespace-nowrap">
      <td class="py-3 px-3 font-mono font-bold text-sky-700">${escapeHtml(ns.maNhanSu)}</td>
      <td class="py-3 px-3 font-semibold text-slate-800">${escapeHtml(ns.hoTen)}</td>
      <td class="py-3 px-3">${escapeHtml(ns.chucDanh)}</td>
      <td class="py-3 px-3 text-slate-500">${escapeHtml(ns.hocVi || '—')}</td>
      <td class="py-3 px-3 text-center font-semibold">${l.soCaChinh}</td>
      <td class="py-3 px-3 text-center font-semibold">${l.soCaNgoaiGio}${l.soCaKhongTinh ? ` <span class="text-rose-500 text-[10px]" title="Ca không được tính công (vắng / thiếu dữ liệu quẹt thẻ)">(-${l.soCaKhongTinh})</span>` : ''}</td>
      <td class="py-3 px-3 text-right font-mono text-slate-600">${fmtMoney(l.donGiaCa)}</td>
      <td class="py-3 px-3 text-center">1.0</td>
      <td class="py-3 px-3 text-center">${Number(l.heSoNgoaiGio).toFixed(1)}</td>
      <td class="py-3 px-3 text-right font-bold text-slate-900">${fmtMoney(l.tongThucLinh)}
        ${Number(l.tienDieuChinh) !== 0 ? `<div class="text-[10px] font-medium text-emerald-700">gồm điều chỉnh ${fmtMoney(l.tienDieuChinh)}</div>` : ''}
      </td>
      <td class="py-3 px-3 text-center">
        <span class="text-[10px] font-bold px-2 py-0.5 rounded ${paid ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'}">${paid ? 'Đã TT' : 'Chưa TT'}</span>
        ${l.soKhieuNaiChoXuLy ? `<span class="ml-1 text-[10px] font-bold px-1.5 rounded bg-rose-100 text-rose-700">${l.soKhieuNaiChoXuLy} KN chờ</span>` : ''}
      </td>
      <td class="py-3 px-3 text-center">
        <div class="flex items-center justify-center gap-1.5">
          <button onclick="ttViewTimesheet(${ns.nhanSuId})" class="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-xs font-semibold" title="Xem bảng chấm công chi tiết">
            <i class="fa-solid fa-clipboard-user mr-1 text-indigo-600"></i>Chấm công
          </button>
          ${action}
        </div>
      </td>
    </tr>`;
  }).join('') || '<tr><td colspan="12" class="py-6 text-center text-slate-400 italic">Khoa chưa có nhân sự.</td></tr>');
}

// ==========================================
// TT03: BẢNG CHẤM CÔNG
// ==========================================
function ttViewTimesheet(nhanSuId) {
  const pay = STATE.payment;
  const ns = pay.staff.find(s => s.nhanSuId === nhanSuId);
  const dept = pay.depts.find(d => d.khoaId === pay.deptId);
  pay.timesheetOwner = { nhanSuId, hoTen: `${ns.chucDanh === 'Bác sĩ' ? 'BS. ' : 'YT. '}${ns.hoTen}`, maNhanSu: ns.maNhanSu, tenKhoa: dept?.tenKhoa };
  pay.timesheetWeek = pay.week;
  navigateTo('screen-TT03');
}

/** Nhân sự xem bảng chấm công của chính mình. */
function ttOpenMyTimesheet() {
  const u = STATE.user;
  STATE.payment.timesheetOwner = { nhanSuId: u.id, hoTen: displayName(u), maNhanSu: u.maNhanSu, tenKhoa: u.tenKhoa };
  STATE.payment.timesheetWeek = pickPaymentWeek();
  navigateTo('screen-TT03');
}

SCREEN_LOADERS['screen-TT03'] = async () => {
  const pay = STATE.payment;
  if (STATE.user.vaiTro === 'NHAN_SU' && (!pay.timesheetOwner || pay.timesheetOwner.nhanSuId !== STATE.user.id)) {
    const u = STATE.user;
    pay.timesheetOwner = { nhanSuId: u.id, hoTen: displayName(u), maNhanSu: u.maNhanSu, tenKhoa: u.tenKhoa };
  }
  if (!pay.timesheetOwner) {
    navigateTo('screen-TT01');
    return;
  }
  pay.timesheetWeek = pay.timesheetWeek || pickPaymentWeek();
  setHtml('ttTimesheetWeek', weekOptionsHtml(pay.timesheetWeek?.id));
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
  setText('ttTimesheetStaffHeader', `${o.hoTen} – Mã NS: ${o.maNhanSu}`);
  setText('ttTimesheetSub', `${o.tenKhoa || ''} • ${weekLabel(w)}`);
  if (!w) {
    setTableMessage('ttTimesheetRecordsBody', 8, 'Chưa có tuần làm việc nào.');
    return;
  }
  setTableMessage('ttTimesheetRecordsBody', 8, 'Đang tải bảng chấm công...');
  pay.timesheet = await MedcareAPI.getTimesheet(o.nhanSuId, w.id);
  renderTimesheetRecords();
}

function chamCongBadge(rec) {
  const label = TRANG_THAI_CHAM_CONG[rec.trangThai] || rec.trangThai;
  const cls = rec.thieuQuetThe ? 'bg-rose-100 text-rose-800'
    : rec.trangThai === 'DUNG_GIO' ? 'bg-emerald-100 text-emerald-800'
    : rec.trangThai === 'CHUA_CO_DU_LIEU' ? 'bg-slate-100 text-slate-600'
    : 'bg-amber-100 text-amber-800';
  return `<span class="text-[10px] font-bold px-2 py-0.5 rounded ${cls}">${escapeHtml(label)}</span>`;
}

function timesheetAction(rec) {
  const isStaff = STATE.user.vaiTro === 'NHAN_SU';
  const kn = rec.trangThaiKhieuNai;
  const knBadge = kn ? `<span class="text-[10px] font-bold px-2 py-0.5 rounded ${kn === 'DA_XU_LY' ? 'bg-emerald-100 text-emerald-800' : kn === 'TU_CHOI' ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-800'}" title="${escapeHtml(rec.maDonKhieuNai || '')}">KN: ${escapeHtml(TRANG_THAI_KHIEU_NAI[kn] || kn)}</span>` : '';

  if (isStaff) {
    if (kn === 'CHO_XU_LY') return knBadge;
    return `<div class="flex items-center justify-center gap-1">${knBadge}
      <button onclick="ttOpenComplaintForm(${rec.phanCongCaId})" class="px-2.5 py-1 bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 rounded text-[11px] font-semibold">
        <i class="fa-solid fa-paper-plane mr-1"></i>Khiếu nại
      </button></div>`;
  }
  if (kn === 'CHO_XU_LY') {
    return `<button onclick="ttOpenComplaint(${rec.khieuNaiId})" class="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded text-[11px] font-bold shadow-sm mx-auto flex items-center gap-1">
      <i class="fa-solid fa-code-compare"></i> Đối soát</button>`;
  }
  if (kn) return knBadge;
  return rec.duocTinhCong
    ? '<span class="text-emerald-600 font-semibold text-[11px]"><i class="fa-solid fa-circle-check"></i> Tính công</span>'
    : '<span class="text-rose-600 font-semibold text-[11px]"><i class="fa-solid fa-ban"></i> Không tính công</span>';
}

function renderTimesheetRecords() {
  const pay = STATE.payment;
  setHtml('ttTimesheetRecordsBody', pay.timesheet.map(rec => `
    <tr class="hover:bg-slate-50 transition border-b border-slate-100 text-xs">
      <td class="py-2.5 px-3 font-semibold text-slate-800">${escapeHtml(THU_LABEL[rec.thu] || '')} (${fmtDateShort(rec.ngay)})</td>
      <td class="py-2.5 px-3 font-medium text-slate-700">${escapeHtml(rec.tenCa)} <span class="text-slate-400 text-[10px]">(${escapeHtml(rec.thoiGian)})</span></td>
      <td class="py-2.5 px-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${rec.loaiCa === 'NGOAI_GIO' ? 'bg-purple-100 text-purple-800' : 'bg-slate-100 text-slate-700'}">${rec.loaiCa === 'NGOAI_GIO' ? 'Ngoài giờ' : 'Chính'}</span></td>
      <td class="py-2.5 px-3 text-center font-bold text-slate-700">${Number(rec.heSo || 1).toFixed(1)}</td>
      <td class="py-2.5 px-3 text-center font-mono ${rec.thieuQuetThe ? 'text-rose-600 font-bold' : ''}">${escapeHtml(rec.gioCheckIn)}</td>
      <td class="py-2.5 px-3 text-center font-mono ${rec.thieuQuetThe ? 'text-rose-600 font-bold' : ''}">${escapeHtml(rec.gioCheckOut)}</td>
      <td class="py-2.5 px-3">${chamCongBadge(rec)}${rec.ghiChu ? `<div class="text-[10px] text-slate-400 mt-0.5">${escapeHtml(rec.ghiChu)}</div>` : ''}</td>
      <td class="py-2.5 px-3 text-center">${timesheetAction(rec)}</td>
    </tr>`).join('') || '<tr><td colspan="8" class="py-6 text-center text-slate-400 italic">Không có ca nào được phân công trong tuần này.</td></tr>');
}

// ==========================================
// TT04: GỬI KHIẾU NẠI (Nhân sự)
// ==========================================
function ttOpenComplaintForm(phanCongCaId) {
  const rec = STATE.payment.timesheet.find(r => r.phanCongCaId === phanCongCaId);
  STATE.payment.complaintRow = rec;
  const shift = document.getElementById('ttComplaintShift');
  if (shift) shift.value = `${THU_LABEL[rec.thu] || ''} (${fmtDate(rec.ngay)}) – Ca ${rec.tenCa} (${rec.thoiGian}) – ${TRANG_THAI_CHAM_CONG[rec.trangThai] || rec.trangThai}`;
  const content = document.getElementById('ttComplaintContent');
  if (content) content.value = '';
  const proof = document.getElementById('ttComplaintProof');
  if (proof) proof.value = '';
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
  setTableMessage('ttComplaintListBody', 6, 'Đang tải danh sách khiếu nại...');
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
    <tr onclick="ttSelectComplaint(${k.id})" class="cursor-pointer transition ${k.id === pay.complaintId ? 'bg-indigo-50' : 'hover:bg-slate-50'}">
      <td class="py-2 px-3 font-mono font-bold text-sky-700">${escapeHtml(k.maYeuCau)}</td>
      <td class="py-2 px-3 font-semibold">${escapeHtml(k.hoTen || '—')} <span class="text-slate-400 font-normal">(${escapeHtml(k.maNhanSu || '')})</span></td>
      <td class="py-2 px-3">${k.tenCa ? `${escapeHtml(k.tenCa)} ${fmtDate(k.ngay)}` : '<span class="text-slate-400">Không rõ ca</span>'}</td>
      <td class="py-2 px-3">${escapeHtml(k.tenTuan || '—')}</td>
      <td class="py-2 px-3 text-slate-500">${fmtDateTime(k.ngayGui)}</td>
      <td class="py-2 px-3"><span class="text-[10px] font-bold px-2 py-0.5 rounded ${k.trangThai === 'CHO_XU_LY' ? 'bg-amber-100 text-amber-800' : k.trangThai === 'DA_XU_LY' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-700'}">${escapeHtml(TRANG_THAI_KHIEU_NAI[k.trangThai] || k.trangThai)}</span></td>
    </tr>`).join('') || '<tr><td colspan="6" class="py-6 text-center text-slate-400 italic">Không có khiếu nại nào.</td></tr>');
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

  setText('kn5Header', `${k.maYeuCau} – ${k.hoTen || ''} (${k.maNhanSu || ''}) • ${k.tenKhoa || ''}`);
  setText('kn5Time', k.tenCa ? `${THU_LABEL[k.thu] || ''} (${fmtDate(k.ngay)}) – Ca ${k.tenCa} (${k.thoiGian})` : 'Không gắn ca cụ thể');
  setText('kn5Assign', k.vaiTro ? `${VAI_TRO_LABEL[k.vaiTro] || k.vaiTro}${k.dieuDongNgoaiDangKy ? ' • điều động ngoài đăng ký' : ''}` : '—');
  setText('kn5Type', k.loaiCa ? `${k.loaiCa === 'NGOAI_GIO' ? 'Ngoài giờ' : 'Chính'} • Hệ số thù lao: ${Number(k.heSo || 1).toFixed(1)}` : '—');
  setText('kn5Week', k.tenTuan || '—');
  setText('kn5Check', k.trangThaiChamCong
    ? `${TRANG_THAI_CHAM_CONG[k.trangThaiChamCong] || k.trangThaiChamCong} (${k.gioCheckIn || '--:--'} → ${k.gioCheckOut || '--:--'})`
    : 'Không có bản ghi máy chấm công');
  setText('kn5Note', k.ghiChuChamCong || '—');
  setText('kn5Content', k.noiDung || '—');
  setText('kn5Proof', k.minhChung || 'Không đính kèm');

  const open = k.trangThai === 'CHO_XU_LY';
  document.querySelectorAll('#ttAdjustForm input, #ttAdjustForm select, #ttAdjustForm textarea, #ttAdjustForm button')
    .forEach(el => { el.disabled = !open; });
  const form = document.getElementById('ttAdjustForm');
  if (form) form.classList.toggle('opacity-60', !open);

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
      ? `Đã xử lý bởi ${k.nguoiXuLy || '—'} lúc ${fmtDateTime(k.ngayXuLy)}: +${dc?.soCaChinh || 0} ca chính, +${dc?.soCaNgoaiGio || 0} ca ngoài giờ, tổng điều chỉnh ${fmtMoney(dc?.soTien)}. Lý do: ${k.lyDoPhanHoi || ''}`
      : `Đã từ chối bởi ${k.nguoiXuLy || '—'} lúc ${fmtDateTime(k.ngayXuLy)}. Lý do: ${k.lyDoPhanHoi || ''}`);
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
    showToast(`${res.message}: ${fmtMoney(res.data.dieuChinh?.soTien)}`);
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
    showToast('Nhập lý do từ chối vào ô "Lý do" trước khi từ chối', false);
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
  setText('ttPayStaffName', `${ns.hoTen} (${ns.maNhanSu})`);
  setText('ttPayDept', `${dept?.tenKhoa || ''} (${pay.week.ten})`);
  setText('ttPayBreakdown', `${l.soCaChinh} ca chính × ${fmtMoney(l.donGiaCa)} + ${l.soCaNgoaiGio} ca ngoài giờ × ${fmtMoney(l.donGiaCa)} × ${Number(l.heSoNgoaiGio).toFixed(1)}`
    + (Number(l.tienDieuChinh) !== 0 ? ` + điều chỉnh ${fmtMoney(l.tienDieuChinh)}` : ''));
  setText('ttPayFinalAmount', fmtMoney(l.tongThucLinh).replace('đ', ' VNĐ'));

  // Thông tin tài khoản nhận được backend lấy từ hồ sơ nhân sự khi chi trả;
  // ở đây chỉ hiển thị trạng thái khai báo.
  setText('payBankNote', `PKMedCare TT Luong ${pay.week.ten} ${ns.maNhanSu}`);
  selectPaymentMethod('online');
};

function selectPaymentMethod(method) {
  const onlineCard = document.getElementById('payOptionOnline');
  const cashCard = document.getElementById('payOptionCash');
  const rOnline = document.getElementById('payRadioOnline');
  const rCash = document.getElementById('payRadioCash');
  const active = 'p-4 rounded-xl border-2 border-emerald-500 bg-emerald-50/50 cursor-pointer transition flex items-start gap-3';
  const idle = 'p-4 rounded-xl border-2 border-slate-200 hover:border-slate-300 bg-white cursor-pointer transition flex items-start gap-3';
  if (rOnline) rOnline.checked = method === 'online';
  if (rCash) rCash.checked = method !== 'online';
  if (onlineCard) onlineCard.className = method === 'online' ? active : idle;
  if (cashCard) cashCard.className = method === 'online' ? idle : active;
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
    showToast(res.message || 'Đã thanh toán');
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
    ? '<i class="fa-solid fa-circle-check mr-1"></i> Đã thanh toán'
    : '<i class="fa-solid fa-clock mr-1"></i> Chưa thanh toán');

  const hinhThuc = p.hinhThucThanhToan === 'TRUC_TUYEN'
    ? `Chuyển khoản trực tuyến${p.nganHangNhan ? ' (' + p.nganHangNhan + ')' : ''}`
    : 'Tiền mặt tại quầy';
  const dcRows = (p.dieuChinh || []).map((d, i) => `
    <tr class="bg-amber-50/60">
      <td class="py-2 px-3 border-r border-slate-200">${3 + i}. Điều chỉnh theo khiếu nại ${escapeHtml(d.maYeuCau)}
        <div class="text-[10px] text-slate-500 italic">${escapeHtml(d.lyDo || '')}</div></td>
      <td class="py-2 px-3 text-center border-r border-slate-200 font-semibold">${d.soCaChinh ? '+' + d.soCaChinh + ' chính ' : ''}${d.soCaNgoaiGio ? '+' + d.soCaNgoaiGio + ' ngoài giờ' : ''}${!d.soCaChinh && !d.soCaNgoaiGio ? '-' : ''}</td>
      <td class="py-2 px-3 text-right border-r border-slate-200">${fmtMoney(p.donGiaCa)}</td>
      <td class="py-2 px-3 text-center border-r border-slate-200">-</td>
      <td class="py-2 px-3 text-right font-medium ${Number(d.soTien) >= 0 ? 'text-emerald-700' : 'text-rose-700'}">${Number(d.soTien) >= 0 ? '+' : ''}${fmtMoney(d.soTien)}</td>
    </tr>`).join('');
  const n = 3 + (p.dieuChinh || []).length;
  const phuCap = Number(p.phuCapTrachNhiem || 0) + Number(p.thuong || 0);

  setHtml('printable-receipt', `
    <div class="flex items-start justify-between border-b-2 border-slate-800 pb-4">
      <div>
        <h2 class="text-xs font-semibold text-slate-600">HỆ THỐNG QUẢN LÝ PHÒNG KHÁM TƯ MEDCARE</h2>
        <p class="text-[10px] text-slate-500 mt-0.5">Phiếu chi trả thù lao nhân sự y tế theo tuần</p>
      </div>
      <div class="text-right">
        <span class="text-[10px] font-mono text-slate-400 block">Mã số phiếu: ${escapeHtml(p.maPhieu)}</span>
        <span class="text-[10px] text-slate-500 block">Ngày thanh toán: ${p.ngayThanhToan ? fmtDate(p.ngayThanhToan) : '—'}</span>
        ${p.maGiaoDich ? `<span class="text-[10px] text-slate-500 block">Mã giao dịch: ${escapeHtml(p.maGiaoDich)}</span>` : ''}
        <span class="text-xs font-bold ${paid ? 'text-emerald-700' : 'text-amber-700'} block mt-1 uppercase">${paid ? 'ĐÃ THANH TOÁN (PAID)' : 'CHƯA THANH TOÁN'}</span>
      </div>
    </div>
    <div class="text-center py-2">
      <h2 class="text-xl font-bold uppercase tracking-wide text-slate-900">PHIẾU CHI TRẢ TIỀN CÔNG NHÂN SỰ Y TẾ</h2>
      <p class="text-xs text-slate-500 mt-0.5">Kỳ quyết toán: ${escapeHtml(p.tenTuan)} (Từ ngày ${fmtDate(p.tuNgay)} đến hết ngày ${fmtDate(p.denNgay)})</p>
    </div>
    <div class="grid grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200">
      <div>
        <div>Họ và tên: <strong class="font-bold text-slate-900">${escapeHtml(p.hoTen)}</strong></div>
        <div>Mã nhân sự: <strong class="font-mono">${escapeHtml(p.maNhanSu)}</strong></div>
        <div>Chức danh: <strong>${escapeHtml(p.chucDanh)}</strong></div>
      </div>
      <div>
        <div>Khoa trực thuộc: <strong>${escapeHtml(p.tenKhoa || '—')}</strong></div>
        <div>Học vị / Trình độ: <strong>${escapeHtml(p.hocVi || '—')}</strong></div>
        <div>Hình thức chi trả: <strong class="text-emerald-700">${escapeHtml(hinhThuc)}</strong></div>
        ${p.hinhThucThanhToan === 'TRUC_TUYEN' && p.soTaiKhoanNhan ? `<div>Tài khoản nhận: <strong>${escapeHtml(p.soTaiKhoanNhan)} – ${escapeHtml(p.chuTaiKhoanNhan || '')}</strong></div>` : ''}
      </div>
    </div>
    <div>
      <h3 class="font-bold text-xs uppercase tracking-wider mb-2 text-slate-700">Chi tiết ca trực & tính toán thù lao:</h3>
      <table class="w-full text-xs text-left border border-slate-300">
        <thead class="bg-slate-100 text-slate-700 font-semibold border-b border-slate-300">
          <tr>
            <th class="py-2 px-3 border-r border-slate-300">Mục thù lao</th>
            <th class="py-2 px-3 text-center border-r border-slate-300">Số lượng ca</th>
            <th class="py-2 px-3 text-right border-r border-slate-300">Đơn giá cơ bản</th>
            <th class="py-2 px-3 text-center border-r border-slate-300">Hệ số</th>
            <th class="py-2 px-3 text-right">Thành tiền (VNĐ)</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          <tr>
            <td class="py-2 px-3 border-r border-slate-200">1. Ca trực chính</td>
            <td class="py-2 px-3 text-center border-r border-slate-200 font-semibold">${p.soCaChinh} ca</td>
            <td class="py-2 px-3 text-right border-r border-slate-200">${fmtMoney(p.donGiaCa)}</td>
            <td class="py-2 px-3 text-center border-r border-slate-200">1.0</td>
            <td class="py-2 px-3 text-right font-medium">${fmtMoney(p.tienCaChinh)}</td>
          </tr>
          <tr>
            <td class="py-2 px-3 border-r border-slate-200">2. Ca trực ngoài giờ</td>
            <td class="py-2 px-3 text-center border-r border-slate-200 font-semibold">${p.soCaNgoaiGio} ca</td>
            <td class="py-2 px-3 text-right border-r border-slate-200">${fmtMoney(p.donGiaCa)}</td>
            <td class="py-2 px-3 text-center border-r border-slate-200">${Number(p.heSoNgoaiGio).toFixed(1)}</td>
            <td class="py-2 px-3 text-right font-medium">${fmtMoney(p.tienCaNgoaiGio)}</td>
          </tr>
          ${dcRows}
          <tr>
            <td class="py-2 px-3 border-r border-slate-200">${n}. Phụ cấp trách nhiệm / Thưởng</td>
            <td class="py-2 px-3 text-center border-r border-slate-200">-</td>
            <td class="py-2 px-3 text-right border-r border-slate-200">-</td>
            <td class="py-2 px-3 text-center border-r border-slate-200">-</td>
            <td class="py-2 px-3 text-right font-medium">${fmtMoney(phuCap)}</td>
          </tr>
          <tr>
            <td class="py-2 px-3 border-r border-slate-200">${n + 1}. Khấu trừ vi phạm</td>
            <td class="py-2 px-3 text-center border-r border-slate-200">-</td>
            <td class="py-2 px-3 text-right border-r border-slate-200">-</td>
            <td class="py-2 px-3 text-center border-r border-slate-200">-</td>
            <td class="py-2 px-3 text-right font-medium">${Number(p.phat || 0) ? '-' : ''}${fmtMoney(p.phat)}</td>
          </tr>
        </tbody>
        <tfoot class="border-t-2 border-slate-800 bg-slate-50 text-slate-900 font-bold">
          <tr>
            <td colspan="4" class="py-3 px-4 text-right text-xs uppercase">TỔNG TIỀN THỰC LĨNH:</td>
            <td class="py-3 px-4 text-right text-base text-emerald-700 font-bold">${fmtMoney(p.tongThucLinh).replace('đ', ' VNĐ')}</td>
          </tr>
        </tfoot>
      </table>
    </div>
    <div class="italic text-[11px] text-slate-500">(Bằng chữ: ${escapeHtml(numberToVietnameseWords(p.tongThucLinh))} chẵn./.)</div>
    <div class="grid grid-cols-2 gap-4 pt-8 text-center text-xs">
      <div>
        <div class="font-bold">Người lập phiếu</div>
        <div class="text-[10px] text-slate-400 italic">(Ký, ghi rõ họ tên)</div>
        <div class="mt-14 font-semibold text-slate-800">${escapeHtml(p.tenKeToan || '')}</div>
      </div>
      <div>
        <div class="font-bold">Nhân sự nhận thù lao</div>
        <div class="text-[10px] text-slate-400 italic">(Xác nhận điện tử qua hệ thống)</div>
        <div class="mt-14 font-semibold text-emerald-700"><i class="fa-solid fa-signature"></i> ${escapeHtml(p.hoTen)}</div>
      </div>
    </div>`);
};

// ==========================================
// TT08: PHIẾU LƯƠNG CỦA TÔI (Nhân sự)
// ==========================================
SCREEN_LOADERS['screen-TT08'] = async () => {
  const u = STATE.user;
  setText('tt8Sub', `Nhân sự: ${displayName(u)} (Mã NS: ${u.maNhanSu}) • ${u.tenKhoa || ''}`);
  setTableMessage('tt8Body', 7, 'Đang tải phiếu lương...');
  const slips = await MedcareAPI.getMySlips();
  STATE.payment.mySlips = slips;
  setHtml('tt8Body', slips.map(p => `
    <tr class="hover:bg-slate-50/80">
      <td class="py-3 px-4 font-mono font-bold text-sky-700">${escapeHtml(p.maPhieu)}</td>
      <td class="py-3 px-4 font-medium">${escapeHtml(p.tenTuan)} (${fmtDateShort(p.tuNgay)} - ${fmtDate(p.denNgay)})</td>
      <td class="py-3 px-4 text-center font-semibold">${p.soCaChinh + p.soCaNgoaiGio} ca (${p.soCaChinh} chính, ${p.soCaNgoaiGio} ngoài giờ)</td>
      <td class="py-3 px-4 text-right font-bold text-emerald-600">${fmtMoney(p.tongThucLinh)}</td>
      <td class="py-3 px-4"><span class="${p.hinhThucThanhToan === 'TRUC_TUYEN' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'} text-[10px] font-semibold px-2 py-0.5 rounded">${p.hinhThucThanhToan === 'TRUC_TUYEN' ? 'Trực tuyến' : 'Tiền mặt tại quầy'}</span></td>
      <td class="py-3 px-4 text-slate-500">${p.ngayThanhToan ? fmtDate(p.ngayThanhToan) : '—'}</td>
      <td class="py-3 px-4 text-center">
        <button onclick="ttViewMySlip(${p.id})" class="px-2.5 py-1 bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200 rounded text-xs font-semibold">
          <i class="fa-solid fa-eye mr-1"></i> Xem phiếu
        </button>
      </td>
    </tr>`).join('') || '<tr><td colspan="7" class="py-6 text-center text-slate-400 italic">Bạn chưa có phiếu lương nào.</td></tr>');
};

function ttViewMySlip(id) {
  STATE.payment.slip = (STATE.payment.mySlips || []).find(p => p.id === id);
  STATE.payment.slipBackScreen = 'screen-TT08';
  navigateTo('screen-TT07');
}
