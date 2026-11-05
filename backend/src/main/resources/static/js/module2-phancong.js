/**
 * MEDCARE CLINIC - MODULE 2: LÊN LỊCH & PHÂN CÔNG THEO TUẦN (Quản lý)
 * Mỗi thao tác gán/hủy được lưu ngay xuống CSDL; lưới luôn hiển thị dữ liệu thật.
 */

function deptStatus(k) {
  if (k.trangThaiBang === 'DA_CONG_BO') return { text: 'Đã công bố', cls: 'bg-sky-100 text-sky-800', full: true };
  if (k.tongSoCa === 0) return { text: 'Không mở ca', cls: 'bg-slate-100 text-slate-500', full: true };
  if (k.soCaDaDu >= k.tongSoCa) return { text: `Đã đủ (${k.soCaDaDu}/${k.tongSoCa} ca)`, cls: 'bg-emerald-100 text-emerald-800', full: true };
  if (k.soCaDaDu === 0) return { text: `Chưa xếp (0/${k.tongSoCa} ca)`, cls: 'bg-rose-100 text-rose-800', full: false };
  return { text: `Đang xếp (${k.soCaDaDu}/${k.tongSoCa} ca)`, cls: 'bg-amber-100 text-amber-800', full: false };
}

// ==========================================
// TRANG CHỦ QUẢN LÝ (C03)
// ==========================================
SCREEN_LOADERS['screen-C03'] = async () => {
  const u = STATE.user;
  setText('c03Name', displayName(u));
  setText('c03Ma', `Mã NV: ${u.maNhanSu}`);
  const w = pickSchedulingWeek();
  STATE.scheduling.week = w;
  if (!w) return;
  setText('c03WeekBtnText', `Lên lịch làm việc ${w.ten}`);
  setText('c03ProgressLabel', `Tiến độ xếp lịch ${w.ten}`);

  const overview = await MedcareAPI.getScheduleOverview(w.id);
  STATE.scheduling.overview = overview;
  const active = overview.filter(k => k.tongSoCa > 0);
  const done = active.filter(k => deptStatus(k).full).length;
  const totalCa = active.reduce((s, k) => s + k.tongSoCa, 0);
  const fullCa = active.reduce((s, k) => s + k.soCaDaDu, 0);

  setText('c03Progress', `${done} / ${active.length} Khoa`);
  const bar = document.getElementById('c03ProgressBar');
  if (bar) bar.style.width = active.length ? `${(done / active.length) * 100}%` : '0%';
  setText('c03FullShifts', `${fullCa} / ${totalCa} ca`);
  setText('c03FullShiftsNote', totalCa ? `Đã đủ 1 BS + 2 YT: ${((fullCa / totalCa) * 100).toFixed(1)}%` : '');
  setText('c03TotalShifts', `${totalCa} Ca trực`);
  setText('c03TotalShiftsNote', `${active.length} chuyên khoa có mở ca`);

  const pending = active.filter(k => !deptStatus(k).full).slice(0, 4);
  setHtml('c03DeptList', pending.length === 0
    ? '<div class="text-xs text-emerald-700 font-semibold p-3 bg-emerald-50 rounded-lg border border-emerald-200">Tất cả các khoa đã xếp đủ ca – có thể công bố lịch.</div>'
    : pending.map(k => `
      <div class="flex items-center justify-between p-3 rounded-lg ${k.soCaDaDu ? 'bg-amber-50/60 border border-amber-200/60' : 'bg-rose-50/60 border border-rose-200/60'}">
        <div class="flex items-center gap-3">
          <span class="font-bold text-xs px-2 py-0.5 rounded ${k.soCaDaDu ? 'text-amber-900 bg-amber-100' : 'text-rose-900 bg-rose-100'}">${escapeHtml(k.maKhoa)}</span>
          <div>
            <div class="text-xs font-semibold text-slate-800">${escapeHtml(k.tenKhoa)} (${k.soBacSi} BS, ${k.soYTa} YT)</div>
            <div class="text-[11px] text-slate-500">${k.soCaDaDu}/${k.tongSoCa} ca đã đủ • Còn thiếu ${k.tongSoCa - k.soCaDaDu} ca</div>
          </div>
        </div>
        <button onclick="pcSelectDept(${k.khoaId})" class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded text-xs font-semibold">${k.soCaDaDu ? 'Tiếp tục phân ca' : 'Bắt đầu phân ca'}</button>
      </div>`).join(''));
};

// ==========================================
// DANH SÁCH KHOA (PC01)
// ==========================================
SCREEN_LOADERS['screen-PC01'] = loadSchedulingOverview;

async function loadSchedulingOverview() {
  const sc = STATE.scheduling;
  sc.week = sc.week || pickSchedulingWeek();
  const w = sc.week;
  if (!w) {
    setTableMessage('pcDeptTableBody', 7, 'Chưa có tuần làm việc nào.');
    return;
  }
  setText('pcWeekName', w.ten);
  setText('pcWeekRange', `${weekRange(w)} • ${TRANG_THAI_TUAN[w.trangThai] || w.trangThai}`);
  setTableMessage('pcDeptTableBody', 7, 'Đang tải tiến độ xếp lịch...');

  sc.overview = await MedcareAPI.getScheduleOverview(w.id);
  renderDeptListTable();
}

function renderDeptListTable() {
  const sc = STATE.scheduling;
  const tbody = document.getElementById('pcDeptTableBody');
  if (!tbody) return;

  const active = sc.overview.filter(k => k.tongSoCa > 0);
  const published = active.length > 0 && active.every(k => k.trangThaiBang === 'DA_CONG_BO');
  const allFull = active.length > 0 && active.every(k => deptStatus(k).full);

  tbody.innerHTML = sc.overview.map(k => {
    const st = deptStatus(k);
    return `<tr class="hover:bg-slate-50/80 transition">
      <td class="py-3.5 px-4 font-mono font-bold text-sky-700">${escapeHtml(k.maKhoa)}</td>
      <td class="py-3.5 px-4 font-semibold text-slate-800">${escapeHtml(k.tenKhoa)}</td>
      <td class="py-3.5 px-4 text-center">${k.soBacSi}</td>
      <td class="py-3.5 px-4 text-center">${k.soYTa}</td>
      <td class="py-3.5 px-4 text-center font-bold">${k.tongSoCa}</td>
      <td class="py-3.5 px-4">
        <div class="flex items-center gap-2">
          <div class="w-20 bg-slate-100 rounded-full h-1.5 overflow-hidden">
            <div class="h-1.5 rounded-full ${st.full ? 'bg-emerald-500' : 'bg-amber-500'}" style="width:${k.tongSoCa ? (k.soCaDaDu / k.tongSoCa) * 100 : 0}%"></div>
          </div>
          <span class="text-[10px] font-bold px-2 py-0.5 rounded ${st.cls}">${st.text}</span>
        </div>
      </td>
      <td class="py-3.5 px-4 text-center">
        ${k.tongSoCa > 0
          ? `<button onclick="pcSelectDept(${k.khoaId})" class="px-3 py-1 bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200 rounded text-xs font-semibold">${k.trangThaiBang === 'DA_CONG_BO' ? 'Xem lịch' : 'Xếp ca'} &rarr;</button>`
          : '<span class="text-slate-300 text-xs">—</span>'}
      </td>
    </tr>`;
  }).join('') || '<tr><td colspan="7" class="py-6 text-center text-slate-400 italic">Chưa có khoa nào.</td></tr>';

  const doneCount = active.filter(k => deptStatus(k).full).length;
  setText('pcOverallProgress', published ? 'Đã công bố toàn viện' : `${doneCount} / ${active.length} khoa đã đủ`);

  const publishBtn = document.getElementById('pcPublishBtn');
  if (publishBtn) {
    const enabled = allFull && !published;
    publishBtn.disabled = !enabled;
    publishBtn.className = enabled
      ? 'px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold shadow-md flex items-center gap-1.5 cursor-pointer transition'
      : 'px-5 py-2 bg-slate-300 text-slate-500 cursor-not-allowed rounded-lg text-xs font-bold transition flex items-center gap-1.5';
  }
  const totalCa = active.reduce((s, k) => s + k.tongSoCa, 0);
  setHtml('pcPublishSummary', `Toàn bộ <strong>${active.length} chuyên khoa</strong> với tổng số <strong>${totalCa} ca trực</strong> của <strong>${escapeHtml(sc.week.ten)}</strong> đã được phân bổ đủ nhân sự theo định mức (1 BS chính + 2 Y tá).`);
}

async function pcConfirmPublish() {
  closeModal('modal-PC05');
  const sc = STATE.scheduling;
  try {
    const res = await MedcareAPI.publishWeek(sc.week.id);
    showToast(res.message || 'Đã công bố lịch làm việc');
    STATE.weeks = await MedcareAPI.getWeeks();
    sc.week = findWeek(sc.week.id);
    await loadSchedulingOverview();
  } catch (err) {
    handleError(err);
  }
}

// ==========================================
// LƯỚI PHÂN CA CỦA KHOA (PC02)
// ==========================================
function pcSelectDept(khoaId) {
  STATE.scheduling.deptId = khoaId;
  STATE.scheduling.week = STATE.scheduling.week || pickSchedulingWeek();
  navigateTo('screen-PC02');
}

SCREEN_LOADERS['screen-PC02'] = async () => {
  const sc = STATE.scheduling;
  if (!sc.deptId || !sc.week) {
    navigateTo('screen-PC01');
    return;
  }
  setTableMessage('pcGridBody', 8, 'Đang tải lưới phân ca...');
  await reloadDeptGrid();
};

async function reloadDeptGrid() {
  const sc = STATE.scheduling;
  sc.lich = await MedcareAPI.getDepartmentSchedule(sc.deptId, sc.week.id);
  renderDeptGridSlots();
}

function slotHtml(ca, slot, vaiTro, label, locked) {
  const color = vaiTro === 'BAC_SI_CHINH' ? 'text-blue-700 bg-blue-100/70' : 'text-teal-700 bg-teal-100/70';
  let body;
  if (slot) {
    body = `<div class="truncate font-semibold text-slate-800 flex-1 text-[10px] ml-1" title="${escapeHtml(slot.hoTen)} (${escapeHtml(slot.maNhanSu)})${slot.dieuDongNgoaiDangKy ? ' – điều động ngoài đăng ký' : ''}">
        ${escapeHtml(slot.hoTen)}${slot.dieuDongNgoaiDangKy ? ' <i class="fa-solid fa-bolt text-amber-500" title="Điều động ngoài đăng ký"></i>' : ''}
      </div>
      ${locked ? '' : `<button onclick="pcClearSlot(${slot.phanCongId})" class="text-slate-300 hover:text-rose-600 text-[10px]" title="Hủy phân công">&times;</button>`}`;
  } else if (locked) {
    body = '<span class="text-[10px] text-rose-400 italic ml-1">Trống</span>';
  } else {
    body = `<button onclick="pcOpenSlotPicker(${ca.id}, '${vaiTro}')" class="w-full text-left py-0.5 px-1 bg-slate-50 hover:bg-sky-50 text-slate-400 hover:text-sky-600 border border-dashed rounded text-[10px]">+ Chọn</button>`;
  }
  return `<div class="flex items-center justify-between gap-1">
    <span class="text-[9px] font-bold ${color} px-1 rounded shrink-0">${label}</span>${body}
  </div>`;
}

function renderDeptGridSlots() {
  const sc = STATE.scheduling;
  const lich = sc.lich;
  const tbody = document.getElementById('pcGridBody');
  if (!tbody || !lich) return;

  const locked = lich.trangThaiBang === 'DA_CONG_BO';
  setText('pcCurrentDeptName', `${lich.tenKhoa} (${lich.maKhoa})`);
  setText('pcDeptWeek', lich.tenTuan);
  setText('pcDeptSlotProgress', `Tiến độ: ${lich.soCaDaDu} / ${lich.tongSoCa} ca đã đủ${locked ? ' • ĐÃ CÔNG BỐ (khóa chỉnh sửa)' : ''}`);

  const shifts = lich.danhSachCa.map(x => x.ca);
  const { days, caTypes } = buildAxes(shifts);
  setHtml('pcGridHead', dayHeaderHtml(days, 'Ca trực \\ Ngày'));

  if (!shifts.length) {
    setTableMessage('pcGridBody', 8, 'Khoa chưa mở ca trực nào trong tuần.');
    return;
  }

  tbody.innerHTML = caTypes.map(c => {
    const cells = days.map(d => {
      const item = lich.danhSachCa.find(x => x.ca.ngayId === d.ngayId && x.ca.caLamViecId === c.caLamViecId);
      if (!item) return '<td class="py-2 px-1.5 text-center text-slate-300">—</td>';
      const full = item.bacSiChinh && item.yTa1 && item.yTa2;
      return `<td class="py-2 px-1.5 align-top border-r border-slate-100 text-left">
        <div class="p-1.5 rounded-lg border text-[11px] space-y-1.5 ${full ? 'bg-emerald-50/40 border-emerald-200' : 'bg-white border-slate-200'}">
          ${slotHtml(item.ca, item.bacSiChinh, 'BAC_SI_CHINH', 'BS chính', locked)}
          ${slotHtml(item.ca, item.yTa1, 'Y_TA_1', 'Y tá 1', locked)}
          ${slotHtml(item.ca, item.yTa2, 'Y_TA_2', 'Y tá 2', locked)}
        </div>
      </td>`;
    }).join('');
    return `<tr class="hover:bg-slate-50/40 transition">${caTypeCellHtml(c)}${cells}</tr>`;
  }).join('');
}

async function pcClearSlot(phanCongId) {
  try {
    const res = await MedcareAPI.unassign(phanCongId);
    showToast(res.message || 'Đã hủy phân công');
    await reloadDeptGrid();
  } catch (err) {
    handleError(err);
  }
}

// ==========================================
// CHỌN NHÂN SỰ (PC03) & ĐIỀU ĐỘNG (PC04)
// ==========================================
async function pcOpenSlotPicker(caId, vaiTro) {
  const sc = STATE.scheduling;
  const item = sc.lich.danhSachCa.find(x => x.ca.id === caId);
  sc.picking = { ca: item.ca, vaiTro };
  const ca = item.ca;
  setText('pcModalTitle', `Chọn ${VAI_TRO_LABEL[vaiTro]} cho ca ${ca.tenCa} (${THU_LABEL[ca.thu] || ''})`);
  setText('pcModalSubtitle', `${THU_LABEL[ca.thu] || ''} (${fmtDate(ca.ngay)}) – Ca ${ca.tenCa} (${fmtTime(ca.gioBatDau)}–${fmtTime(ca.gioKetThuc)}) • Vị trí: ${VAI_TRO_LABEL[vaiTro]}`);
  const search = document.getElementById('pcStaffSearch');
  if (search) search.value = '';
  setTableMessage('pcStaffListBody', 6, 'Đang tải danh sách nhân sự...');
  openModal('modal-PC03');

  try {
    sc.candidates = await MedcareAPI.getCandidates(caId, vaiTro);
    pcRenderStaffCandidates('');
  } catch (err) {
    setTableMessage('pcStaffListBody', 6, err.message, true);
  }
}

function pcFilterStaffList(query) {
  pcRenderStaffCandidates((query || '').toLowerCase().trim());
}

function pcRenderStaffCandidates(query) {
  const sc = STATE.scheduling;
  const tbody = document.getElementById('pcStaffListBody');
  if (!tbody) return;
  const list = sc.candidates.filter(c => !query
    || c.hoTen.toLowerCase().includes(query)
    || c.maNhanSu.toLowerCase().includes(query)
    || (c.tenKhoa || '').toLowerCase().includes(query));

  if (!list.length) {
    setTableMessage('pcStaffListBody', 6, 'Không tìm thấy nhân sự phù hợp');
    return;
  }

  tbody.innerHTML = list.map((c, i) => {
    const maxed = c.soCaTrongNgay >= c.soCaToiDaMoiNgay;
    const blocked = maxed || c.daCoTrongCa;
    const reason = c.daCoTrongCa ? 'Đã có trong ca' : maxed ? `Đủ ${c.soCaToiDaMoiNgay} ca/ngày` : '';
    const wish = c.daDangKy
      ? '<span class="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 border border-emerald-200"><i class="fa-solid fa-check mr-1"></i>Đã đăng ký ca này</span>'
      : `<span class="text-[10px] font-bold px-2 py-0.5 rounded bg-amber-100 text-amber-800 border border-amber-200"><i class="fa-solid fa-triangle-exclamation mr-1"></i>${c.cungKhoa ? 'Chưa đăng ký' : 'Khác khoa'} (cần điều động)</span>`;
    return `<tr class="hover:bg-slate-50 transition border-b border-slate-100 text-xs">
      <td class="py-2.5 px-3 text-slate-500 font-medium">${i + 1}</td>
      <td class="py-2.5 px-3 font-mono font-bold text-sky-700">${escapeHtml(c.maNhanSu)}</td>
      <td class="py-2.5 px-3">
        <div class="font-semibold text-slate-800">${escapeHtml(c.hoTen)}</div>
        <div class="text-[10px] text-slate-400">${escapeHtml(c.hocVi || '')} • ${escapeHtml(c.tenKhoa || '')}</div>
      </td>
      <td class="py-2.5 px-3 text-center">
        <span class="text-xs font-bold ${maxed ? 'text-rose-600 bg-rose-50 border border-rose-200' : 'text-slate-600 bg-slate-100'} px-2 py-0.5 rounded-full">${c.soCaTrongNgay} / ${c.soCaToiDaMoiNgay} ca</span>
      </td>
      <td class="py-2.5 px-3">${wish}</td>
      <td class="py-2.5 px-3 text-center">
        <button onclick="pcAssignCandidate(${c.nhanSuId})" ${blocked ? 'disabled' : ''}
          class="px-3 py-1 rounded text-xs font-bold transition ${blocked ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-sky-600 hover:bg-sky-700 text-white shadow-sm'}">
          ${blocked ? reason : 'Chọn gán'}
        </button>
      </td>
    </tr>`;
  }).join('');
}

async function pcAssignCandidate(nhanSuId) {
  const sc = STATE.scheduling;
  const cand = sc.candidates.find(c => c.nhanSuId === nhanSuId);
  if (!cand || !sc.picking) return;

  if (!cand.daDangKy) {
    sc.pendingCandidate = cand;
    setText('pcDispatchStaffName', `${cand.hoTen} (${cand.maNhanSu})`);
    setText('pcDispatchReason', cand.cungKhoa ? 'KHÔNG ĐĂNG KÝ' : `thuộc ${cand.tenKhoa} và KHÔNG ĐĂNG KÝ`);
    openModal('modal-PC04');
    return;
  }
  await doAssign(cand, false);
}

async function pcConfirmDispatch() {
  const cand = STATE.scheduling.pendingCandidate;
  closeModal('modal-PC04');
  if (cand) await doAssign(cand, true);
}

async function doAssign(cand, dieuDong) {
  const sc = STATE.scheduling;
  try {
    const res = await MedcareAPI.assignStaff({
      caTrucKhoaId: sc.picking.ca.id,
      nhanSuId: cand.nhanSuId,
      vaiTro: sc.picking.vaiTro,
      dieuDongNgoaiDangKy: dieuDong,
      ghiChu: dieuDong ? 'Điều động ngoài đăng ký có xác nhận quản lý' : null
    });
    closeModal('modal-PC03');
    showToast(res.message || `Đã xếp ${cand.hoTen} vào ca trực`);
    await reloadDeptGrid();
  } catch (err) {
    handleError(err);
  }
}
