/**
 * MEDCARE CLINIC - MODULE 1: ĐĂNG KÝ LỊCH LÀM VIỆC THEO TUẦN (Bác sĩ / Y tá)
 * Ma trận ca được dựng từ ca trực thật của khoa trong tuần (/api/dang-ky-ca/ca-truc/{tuanId}).
 */

// ==========================================
// TRANG CHỦ NHÂN SỰ (C02)
// ==========================================
SCREEN_LOADERS['screen-C02'] = async () => {
  const u = STATE.user;
  setText('c02Name', displayName(u));
  setText('c02Ma', `Mã NS: ${u.maNhanSu}`);
  setText('c02Info', `Chức danh: ${u.chucDanh}${u.hocVi ? ' • Học vị: ' + u.hocVi : ''} • ${u.tenKhoa || 'Chưa gán khoa'}`);
  setHtml('c02Bank', u.soTaiKhoan
    ? `Số tài khoản ngân hàng: <strong>${escapeHtml(u.soTaiKhoan)} (${escapeHtml(u.tenNganHang || '')})</strong>`
    : 'Chưa khai báo tài khoản ngân hàng');

  const w = pickRegistrationWeek();
  const open = isRegistrationOpen(w);
  setText('c02DkWeek', w ? `${weekLabel(w)}. Hạn chót: ${w.hanDangKy ? fmtDateTime(w.hanDangKy) : 'chưa đặt'}.` : 'Chưa có tuần làm việc nào.');
  setHtml('c02DkBadge', open
    ? '<span class="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded-full">Cổng đang mở</span>'
    : '<span class="bg-slate-100 text-slate-600 text-[10px] font-bold px-2 py-0.5 rounded-full">Cổng đã đóng</span>');

  const pw = pickPaymentWeek();
  setText('c02CcWeek', pw ? `${weekLabel(pw)}. Đối chiếu giờ check-in, check-out và gửi khiếu nại sai sót ca trực.` : '');

  const [slips, complaints] = await Promise.all([
    MedcareAPI.getMySlips().catch(() => []),
    MedcareAPI.getMyComplaints().catch(() => [])
  ]);
  const pending = complaints.filter(k => k.trangThai === 'CHO_XU_LY').length;
  setHtml('c02CcBadge', pending
    ? `<span class="bg-amber-100 text-amber-800 text-[10px] font-bold px-2 py-0.5 rounded-full">${pending} khiếu nại chờ</span>`
    : '<span class="bg-slate-100 text-slate-600 text-[10px] font-bold px-2 py-0.5 rounded-full">Không có khiếu nại chờ</span>');
  setHtml('c02SlipBadge', `<span class="bg-sky-100 text-sky-800 text-[10px] font-bold px-2 py-0.5 rounded-full">${slips.length} phiếu lương</span>`);
};

function isRegistrationOpen(w) {
  if (!w || w.trangThai !== 'DANG_MO_DANG_KY') return false;
  const now = new Date();
  if (w.thoiDiemMoDangKy && now < new Date(w.thoiDiemMoDangKy)) return false;
  if (w.hanDangKy && now > new Date(w.hanDangKy)) return false;
  return true;
}

function timeLeftText(deadline) {
  if (!deadline) return '';
  const ms = new Date(deadline) - new Date();
  if (ms <= 0) return 'Đã hết hạn';
  const days = Math.floor(ms / 86400000);
  const hours = Math.floor((ms % 86400000) / 3600000);
  return `Còn ${days > 0 ? days + ' ngày ' : ''}${String(hours).padStart(2, '0')} giờ`;
}

// ==========================================
// MÀN HÌNH ĐĂNG KÝ (DK01)
// ==========================================
SCREEN_LOADERS['screen-DK01'] = loadRegistration;

async function loadRegistration() {
  const reg = STATE.registration;
  const u = STATE.user;
  reg.week = pickRegistrationWeek();
  const w = reg.week;

  setText('dkStaffName', displayName(u));
  setText('dkStaffMa', `Mã NS: ${u.maNhanSu}`);
  setText('dkKhoa', u.tenKhoa || '—');
  if (!w) {
    setTableMessage('dkMatrixBody', 8, 'Chưa có tuần làm việc nào được tạo.');
    return;
  }
  setText('dkWeekName', w.ten);
  setText('dkWeekRange', weekRange(w));
  setText('dkDeadline', w.hanDangKy ? fmtDateTime(w.hanDangKy) : 'Chưa đặt');
  setText('dkDeadlineLeft', timeLeftText(w.hanDangKy));
  setText('dkQuota', `${w.soCaToiThieu} – ${w.soCaToiDa} ca / tuần`);
  setText('dkQuotaDay', `Tối đa ${w.soCaToiDaMoiNgay} ca/ngày`);
  setText('dkModalWeek', w.ten);
  setText('dkModalDeadline', w.hanDangKy ? `Sau khi lưu, bạn vẫn có thể chỉnh sửa cho đến ${fmtDateTime(w.hanDangKy)}.` : 'Sau khi lưu, bạn vẫn có thể chỉnh sửa khi cổng còn mở.');

  const open = isRegistrationOpen(w);
  setHtml('dkGateStatus', open
    ? '<span class="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5 animate-pulse"></span> Đang mở'
    : `<span class="w-1.5 h-1.5 rounded-full bg-slate-400 mr-1.5"></span> ${escapeHtml(TRANG_THAI_TUAN[w.trangThai] || 'Đã đóng')}`);
  const gate = document.getElementById('dkGateStatus');
  if (gate) gate.className = `inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${open ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`;

  setTableMessage('dkMatrixBody', 8, 'Đang tải ca trực của khoa...');
  const [shifts, phieu] = await Promise.all([
    MedcareAPI.getMyShiftOptions(w.id),
    MedcareAPI.getMyRegistration(w.id)
  ]);
  reg.shifts = shifts;
  reg.phieu = phieu;
  reg.selected = new Set(phieu ? phieu.caTrucKhoaIds : []);
  renderShiftMatrix();
}

/** Trục ngày & trục ca của ma trận, dựng từ danh sách ca trực. */
function buildAxes(shifts) {
  const days = [];
  const caTypes = [];
  shifts.forEach(s => {
    if (!days.find(d => d.ngayId === s.ngayId)) days.push({ ngayId: s.ngayId, ngay: s.ngay, thu: s.thu });
    if (!caTypes.find(c => c.caLamViecId === s.caLamViecId)) {
      caTypes.push({ caLamViecId: s.caLamViecId, tenCa: s.tenCa, loaiCa: s.loaiCa, gioBatDau: s.gioBatDau, gioKetThuc: s.gioKetThuc, heSo: s.heSo });
    }
  });
  days.sort((a, b) => String(a.ngay).localeCompare(String(b.ngay)));
  caTypes.sort((a, b) => String(a.gioBatDau).localeCompare(String(b.gioBatDau)));
  return { days, caTypes };
}

function dayHeaderHtml(days, firstCellLabel) {
  return `<tr class="bg-slate-100 text-slate-700 border-b border-slate-200 text-center">
    <th class="py-2.5 px-3 font-semibold text-left w-40 border-r border-slate-200">${firstCellLabel}</th>
    ${days.map(d => `<th class="py-2.5 px-2 font-semibold ${d.thu === 'SUNDAY' ? 'text-rose-600' : ''}">${THU_SHORT[d.thu] || ''} (${fmtDateShort(d.ngay)})</th>`).join('')}
  </tr>`;
}

function caTypeCellHtml(c) {
  const ot = c.loaiCa === 'NGOAI_GIO';
  return `<td class="py-3 px-3 text-left font-medium border-r border-slate-200 bg-slate-50/50">
    <div class="font-bold text-slate-800">${escapeHtml(c.tenCa)}</div>
    <div class="text-[10px] text-slate-400">${fmtTime(c.gioBatDau)}–${fmtTime(c.gioKetThuc)}</div>
    <span class="text-[9px] font-semibold px-1.5 rounded ${ot ? 'bg-purple-100 text-purple-700' : 'bg-sky-100 text-sky-700'}">
      ${ot ? 'Ngoài giờ' : 'Chính'} (${Number(c.heSo || 1).toFixed(1)}x)
    </span>
  </td>`;
}

function shiftsOnDay(ngayId) {
  const reg = STATE.registration;
  return reg.shifts.filter(s => s.ngayId === ngayId && reg.selected.has(s.id)).length;
}

function renderShiftMatrix() {
  const reg = STATE.registration;
  const w = reg.week;
  const tbody = document.getElementById('dkMatrixBody');
  const thead = document.getElementById('dkMatrixHead');
  if (!tbody) return;

  if (!reg.shifts.length) {
    if (thead) thead.innerHTML = dayHeaderHtml([], 'Ca làm việc \\ Ngày');
    setTableMessage('dkMatrixBody', 8, 'Khoa của bạn chưa mở ca trực nào trong tuần này.');
    renderSummaryTable();
    return;
  }

  const { days, caTypes } = buildAxes(reg.shifts);
  if (thead) thead.innerHTML = dayHeaderHtml(days, 'Ca làm việc \\ Ngày');
  const editable = isRegistrationOpen(w);

  tbody.innerHTML = caTypes.map(c => {
    const cells = days.map(d => {
      const shift = reg.shifts.find(s => s.ngayId === d.ngayId && s.caLamViecId === c.caLamViecId);
      if (!shift) return '<td class="py-2 px-1 text-center text-slate-300">—</td>';
      const isSelected = reg.selected.has(shift.id);
      const full = shiftsOnDay(d.ngayId) >= w.soCaToiDaMoiNgay || reg.selected.size >= w.soCaToiDa;
      const disabled = !editable || (!isSelected && full);

      let cls = 'w-12 h-10 rounded-lg border text-xs font-semibold mx-auto flex items-center justify-center transition-all ';
      let content;
      if (isSelected) {
        cls += 'bg-sky-600 text-white border-sky-600 shadow-sm font-bold';
        content = '<i class="fa-solid fa-check"></i>';
      } else if (disabled) {
        cls += 'bg-slate-100 text-slate-300 border-slate-200 cursor-not-allowed opacity-60';
        content = `<span class="text-[9px] leading-tight">${editable ? 'Đủ ca' : '—'}</span>`;
      } else {
        cls += 'bg-white text-slate-400 border-slate-200 hover:border-sky-400 hover:text-sky-600 hover:bg-sky-50/40 cursor-pointer';
        content = '<span>+</span>';
      }
      return `<td class="py-2 px-1 text-center">
        <button type="button" onclick="toggleShiftSelection(${shift.id})" ${disabled ? 'disabled' : ''} class="${cls}"
          title="${escapeHtml(THU_LABEL[d.thu] || '')} - ${escapeHtml(c.tenCa)}">${content}</button>
      </td>`;
    }).join('');
    return `<tr class="hover:bg-slate-50/60 transition">${caTypeCellHtml(c)}${cells}</tr>`;
  }).join('');

  renderSummaryTable();
}

function toggleShiftSelection(shiftId) {
  const reg = STATE.registration;
  const w = reg.week;
  if (!isRegistrationOpen(w)) {
    showToast('Cổng đăng ký đã đóng, không thể thay đổi', false);
    return;
  }
  if (reg.selected.has(shiftId)) {
    reg.selected.delete(shiftId);
  } else {
    const shift = reg.shifts.find(s => s.id === shiftId);
    if (shiftsOnDay(shift.ngayId) >= w.soCaToiDaMoiNgay) {
      showToast(`Mỗi ngày chỉ được đăng ký tối đa ${w.soCaToiDaMoiNgay} ca!`, false);
      return;
    }
    if (reg.selected.size >= w.soCaToiDa) {
      showToast(`Đã đạt giới hạn tối đa ${w.soCaToiDa} ca/tuần!`, false);
      return;
    }
    reg.selected.add(shiftId);
  }
  renderShiftMatrix();
}

function removeShift(shiftId) {
  if (!isRegistrationOpen(STATE.registration.week)) return;
  STATE.registration.selected.delete(shiftId);
  renderShiftMatrix();
}

function renderSummaryTable() {
  const reg = STATE.registration;
  const w = reg.week;
  const tbody = document.getElementById('dkSummaryTableBody');
  const badge = document.getElementById('dkSummaryBadge');
  const submitBtn = document.getElementById('dkSubmitBtn');
  if (!tbody || !badge || !w) return;

  const selected = reg.shifts
    .filter(s => reg.selected.has(s.id))
    .sort((a, b) => String(a.ngay).localeCompare(String(b.ngay)) || String(a.gioBatDau).localeCompare(String(b.gioBatDau)));
  const total = selected.length;
  const otCount = selected.filter(s => s.loaiCa === 'NGOAI_GIO').length;
  const editable = isRegistrationOpen(w);

  setText('dkShiftCount', `${total} / ${w.soCaToiDa}`);
  setText('dkModalShiftCount', `${total} ca`);

  tbody.innerHTML = total === 0
    ? '<tr><td colspan="6" class="py-4 text-center text-slate-400 italic">Chưa có ca nào được chọn từ bảng lưới phía trên.</td></tr>'
    : selected.map((s, i) => `
      <tr class="hover:bg-slate-50/50">
        <td class="py-2.5 px-3 text-slate-400 font-mono text-[11px] text-center">${i + 1}</td>
        <td class="py-2.5 px-3 font-medium text-slate-800">${escapeHtml(THU_LABEL[s.thu] || '')} <span class="text-slate-400 text-[10px]">(${fmtDateShort(s.ngay)})</span></td>
        <td class="py-2.5 px-3 font-semibold text-slate-700">${escapeHtml(s.tenCa)}</td>
        <td class="py-2.5 px-3">
          <span class="text-[10px] font-semibold px-2 py-0.5 rounded ${s.loaiCa === 'NGOAI_GIO' ? 'bg-purple-100 text-purple-700' : 'bg-sky-100 text-sky-700'}">
            ${s.loaiCa === 'NGOAI_GIO' ? 'Ngoài giờ' : 'Chính'} (${Number(s.heSo || 1).toFixed(1)}x)
          </span>
        </td>
        <td class="py-2.5 px-3 text-slate-500">${fmtTime(s.gioBatDau)} – ${fmtTime(s.gioKetThuc)}</td>
        <td class="py-2.5 px-3 text-center">
          ${editable ? `<button onclick="removeShift(${s.id})" class="text-slate-400 hover:text-rose-600 transition" title="Bỏ ca này"><i class="fa-solid fa-trash-can"></i></button>` : ''}
        </td>
      </tr>`).join('');

  setText('dkSummaryFooter', `Tổng: ${total} ca (${total - otCount} ca chính, ${otCount} ca ngoài giờ)`
    + (reg.phieu ? ` • Phiếu ${reg.phieu.maPhieu} lưu lúc ${fmtDateTime(reg.phieu.ngayDangKy)}` : ''));

  const valid = total >= w.soCaToiThieu && total <= w.soCaToiDa;
  if (!editable) {
    badge.className = 'text-xs font-semibold px-2.5 py-0.5 rounded bg-slate-100 text-slate-600';
    badge.innerHTML = '<i class="fa-solid fa-lock mr-1"></i> Cổng đăng ký đã đóng';
  } else if (valid) {
    badge.className = 'text-xs font-semibold px-2.5 py-0.5 rounded bg-emerald-100 text-emerald-800';
    badge.innerHTML = `<i class="fa-solid fa-circle-check mr-1"></i> Hợp lệ (${total} ca, yêu cầu ${w.soCaToiThieu}–${w.soCaToiDa})`;
  } else {
    badge.className = 'text-xs font-semibold px-2.5 py-0.5 rounded bg-amber-100 text-amber-800';
    badge.innerHTML = `<i class="fa-solid fa-triangle-exclamation mr-1"></i> Chưa đủ số ca tối thiểu (${total} / ${w.soCaToiThieu})`;
  }

  if (submitBtn) {
    const enabled = editable && valid;
    submitBtn.disabled = !enabled;
    submitBtn.className = enabled
      ? 'px-5 py-2 bg-sky-600 hover:bg-sky-700 text-white cursor-pointer rounded-lg text-xs font-bold shadow transition flex items-center gap-1.5'
      : 'px-5 py-2 bg-slate-300 text-slate-500 cursor-not-allowed rounded-lg text-xs font-bold transition flex items-center gap-1.5';
  }
}

/** Tự chọn nhanh các ca hợp lệ (ưu tiên ca chính) đủ định mức tối thiểu. */
function dkAutoSelectDemo() {
  const reg = STATE.registration;
  const w = reg.week;
  if (!isRegistrationOpen(w)) {
    showToast('Cổng đăng ký đã đóng, không thể thay đổi', false);
    return;
  }
  const target = Math.min(w.soCaToiDa, Math.max(w.soCaToiThieu, 10));
  const ordered = [...reg.shifts].sort((a, b) =>
    (a.loaiCa === 'NGOAI_GIO') - (b.loaiCa === 'NGOAI_GIO') ||
    String(a.ngay).localeCompare(String(b.ngay)) ||
    String(a.gioBatDau).localeCompare(String(b.gioBatDau)));
  reg.selected.clear();
  for (const s of ordered) {
    if (reg.selected.size >= target) break;
    if (shiftsOnDay(s.ngayId) < w.soCaToiDaMoiNgay) reg.selected.add(s.id);
  }
  renderShiftMatrix();
  showToast(`Đã chọn nhanh ${reg.selected.size} ca hợp lệ – kiểm tra lại trước khi lưu`);
}

async function dkConfirmSave() {
  closeModal('modal-DK02');
  const reg = STATE.registration;
  try {
    const res = await MedcareAPI.saveRegistration(reg.week.id, Array.from(reg.selected));
    reg.phieu = res.data;
    reg.selected = new Set(res.data.caTrucKhoaIds);
    renderShiftMatrix();
    showToast(res.message || 'Đã lưu đăng ký ca trực');
  } catch (err) {
    handleError(err);
  }
}
