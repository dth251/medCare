/**
 * MEDCARE CLINIC - MODULE 1: ĐĂNG KÝ LỊCH LÀM VIỆC THEO TUẦN (Bác sĩ / Y tá)
 * Ma trận ca được dựng từ ca trực thật của khoa trong tuần (/api/dang-ky-ca/ca-truc/{tuanId}).
 */

// ==========================================
// TRANG CHỦ NHÂN SỰ (C02)
// ==========================================
SCREEN_LOADERS['screen-C02'] = async () => {
  const u = STATE.user;
  setText('c02Name', u.fullName);
  setText('c02Info', [u.chucDanh, u.tenKhoa].filter(Boolean).join(' · '));

  const w = pickRegistrationWeek();
  const open = isRegistrationOpen(w);
  const notice = document.getElementById('c02Notice');
  if (notice) {
    notice.classList.toggle('hidden', !(w && open));
    if (w && open) {
      notice.textContent = `${w.ten} đang mở đăng ký` + (w.hanDangKy ? ` · Hạn chót ${fmtTime(String(w.hanDangKy).slice(11))} ngày ${fmtDate(w.hanDangKy)}` : '');
    }
  }
  setText('c02DkWeek', w ? w.ten : '—');
  setText('c02DkDesc', w ? `Định mức ${w.soCaToiThieu}–${w.soCaToiDa} ca/tuần.` : 'Chưa có tuần đăng ký.');
  setHtml('c02DkBadge', w ? (open ? badge('Đang mở', 'success') : badge('Đã đóng', 'neutral')) : '');

  const pw = pickPaymentWeek();
  setText('c02CcWeek', pw ? pw.ten : '—');

  const [slips, complaints] = await Promise.all([
    MedcareAPI.getMySlips().catch(() => []),
    MedcareAPI.getMyComplaints().catch(() => [])
  ]);
  const pending = complaints.filter(k => k.trangThai === 'CHO_XU_LY').length;
  setHtml('c02CcBadge', pending ? badge(`${pending} khiếu nại chờ`, 'warning') : '');
  setHtml('c02SlipBadge', slips.length ? badge(`${slips.length} phiếu`, 'neutral') : '');
  setText('c02SlipLatest', slips[0] ? fmtMoney(slips[0].tongThucLinh) : 'Theo kỳ');

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
  if (ms <= 0) return '(đã hết hạn)';
  const days = Math.floor(ms / 86400000);
  const hours = Math.floor((ms % 86400000) / 3600000);
  return `(còn ${days > 0 ? days + ' ngày ' : ''}${hours} giờ)`;
}

// ==========================================
// MÀN HÌNH ĐĂNG KÝ (DK01)
// ==========================================
SCREEN_LOADERS['screen-DK01'] = loadRegistration;

async function loadRegistration() {
  const reg = STATE.registration;
  const u = STATE.user;
  reg.week = reg.week || pickRegistrationWeek();
  const w = reg.week;
  setHtml('dkWeekSelect', weekOptionsHtml(w?.id));

  setText('dkStaffName', displayName(u));
  setText('dkStaffMa', u.maNhanSu);
  setText('dkKhoa', u.tenKhoa || '—');
  if (!w) {
    setTableMessage('dkMatrixBody', 8, 'Chưa có tuần làm việc nào.');
    return;
  }
  setText('dkWeekName', w.ten);
  setText('dkWeekRange', `${fmtDate(w.ngayBatDau)} – ${fmtDate(w.ngayKetThuc)}`);
  setText('dkDeadline', w.hanDangKy ? fmtDateTime(w.hanDangKy) : 'chưa đặt');
  setText('dkDeadlineLeft', timeLeftText(w.hanDangKy));
  setText('dkQuota', `${w.soCaToiThieu}–${w.soCaToiDa} ca/tuần`);
  setText('dkQuotaDay', `Tối đa ${w.soCaToiDaMoiNgay} ca/ngày`);
  setText('dkModalWeek', `${w.ten} (${fmtDate(w.ngayBatDau)} – ${fmtDate(w.ngayKetThuc)})`);
  setText('dkModalDeadline', w.hanDangKy ? `Có thể chỉnh sửa đến ${fmtDateTime(w.hanDangKy)}.` : '');

  const open = isRegistrationOpen(w);
  const gate = document.getElementById('dkGateStatus');
  if (gate) {
    gate.className = `badge ${open ? 'badge-success' : 'badge-neutral'}`;
    gate.textContent = open ? 'Đang mở' : (TRANG_THAI_TUAN[w.trangThai] || 'Đã đóng');
  }

  setTableMessage('dkMatrixBody', 8, 'Đang tải…');
  const [shifts, phieu] = await Promise.all([
    MedcareAPI.getMyShiftOptions(w.id),
    MedcareAPI.getMyRegistration(w.id)
  ]);
  reg.shifts = shifts;
  reg.phieu = phieu;
  reg.selected = new Set(phieu ? phieu.caTrucKhoaIds : []);
  renderShiftMatrix();
}

function dkChangeWeek(weekId) {
  const found = findWeek(weekId);
  if (found) {
    STATE.registration.week = found;
    loadRegistration().catch(handleError);
  }
}

function dkQuickSwitchWeek(mode) {
  const target = mode === 'next' ? getNextWeek() : getCurrentWeek();
  if (!target) {
    showToast(mode === 'next' ? 'Không tìm thấy tuần kế tiếp' : 'Không tìm thấy tuần hiện tại', false);
    return;
  }
  STATE.registration.week = target;
  loadRegistration().catch(handleError);
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
  return `<tr>
    <th style="width:112px">${firstCellLabel}</th>
    ${days.map(d => `<th>${THU_SHORT[d.thu] || ''} <span class="num" style="color:var(--muted-soft)">${fmtDateShort(d.ngay)}</span></th>`).join('')}
  </tr>`;
}

function caTypeCellHtml(c) {
  const ot = c.loaiCa === 'NGOAI_GIO';
  return `<td>
    <div class="cell-strong">${escapeHtml(c.tenCa)}</div>
    <div class="cell-sub num">${fmtTime(c.gioBatDau)}–${fmtTime(c.gioKetThuc)}${ot ? ` · ×${Number(c.heSo || 1).toFixed(1)}` : ''}</div>
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
    if (thead) thead.innerHTML = '';
    setTableMessage('dkMatrixBody', 8, 'Khoa chưa mở ca trực trong tuần này.');
    renderSummaryTable();
    return;
  }

  const { days, caTypes } = buildAxes(reg.shifts);
  if (thead) thead.innerHTML = dayHeaderHtml(days, 'Ca');
  const editable = isRegistrationOpen(w);

  tbody.innerHTML = caTypes.map(c => {
    const cells = days.map(d => {
      const shift = reg.shifts.find(s => s.ngayId === d.ngayId && s.caLamViecId === c.caLamViecId);
      if (!shift) return '<td style="color:var(--muted-soft)">—</td>';
      const isSelected = reg.selected.has(shift.id);
      const full = shiftsOnDay(d.ngayId) >= w.soCaToiDaMoiNgay || reg.selected.size >= w.soCaToiDa;
      const disabled = !editable || (!isSelected && full);
      return `<td>
        <button type="button" onclick="toggleShiftSelection(${shift.id})" ${disabled ? 'disabled' : ''}
          class="shift-cell${isSelected ? ' is-selected' : ''}"
          title="${escapeHtml(THU_LABEL[d.thu] || '')} · ${escapeHtml(c.tenCa)}">${isSelected ? ic('check') : ''}</button>
      </td>`;
    }).join('');
    return `<tr>${caTypeCellHtml(c)}${cells}</tr>`;
  }).join('');

  renderSummaryTable();
}

function toggleShiftSelection(shiftId) {
  const reg = STATE.registration;
  const w = reg.week;
  if (!isRegistrationOpen(w)) {
    showToast('Cổng đăng ký đã đóng', false);
    return;
  }
  if (reg.selected.has(shiftId)) {
    reg.selected.delete(shiftId);
  } else {
    const shift = reg.shifts.find(s => s.id === shiftId);
    if (shiftsOnDay(shift.ngayId) >= w.soCaToiDaMoiNgay) {
      showToast(`Tối đa ${w.soCaToiDaMoiNgay} ca mỗi ngày`, false);
      return;
    }
    if (reg.selected.size >= w.soCaToiDa) {
      showToast(`Tối đa ${w.soCaToiDa} ca mỗi tuần`, false);
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
  const badgeEl = document.getElementById('dkSummaryBadge');
  const submitBtn = document.getElementById('dkSubmitBtn');
  if (!tbody || !badgeEl || !w) return;

  const selected = reg.shifts
    .filter(s => reg.selected.has(s.id))
    .sort((a, b) => String(a.ngay).localeCompare(String(b.ngay)) || String(a.gioBatDau).localeCompare(String(b.gioBatDau)));
  const total = selected.length;
  const otCount = selected.filter(s => s.loaiCa === 'NGOAI_GIO').length;
  const editable = isRegistrationOpen(w);

  setText('dkShiftCount', `${total} / ${w.soCaToiDa}`);
  setText('dkModalShiftCount', `${total} ca`);

  const baseRate = Number(STATE.user?.donGiaCa) || 0;
  const estimatedPay = selected.reduce((sum, s) => sum + baseRate * Number(s.heSo || 1), 0);
  setText('dkEstimatedPay', fmtMoney(estimatedPay));

  tbody.innerHTML = total === 0
    ? '<tr><td colspan="5" class="empty">Chưa chọn ca nào.</td></tr>'
    : selected.map(s => `
      <tr>
        <td class="cell-strong">${escapeHtml(THU_LABEL[s.thu] || '')} <span class="cell-sub num">${fmtDateShort(s.ngay)}</span></td>
        <td>${escapeHtml(s.tenCa)}</td>
        <td class="num">${fmtTime(s.gioBatDau)} – ${fmtTime(s.gioKetThuc)}</td>
        <td>${s.loaiCa === 'NGOAI_GIO' ? `Ngoài giờ · ×${Number(s.heSo || 1).toFixed(1)}` : 'Chính'}</td>
        <td class="text-right">
          ${editable ? `<button onclick="removeShift(${s.id})" class="btn btn-ghost btn-icon btn-sm" title="Bỏ ca">${ic('x', 'text-[15px]')}</button>` : ''}
        </td>
      </tr>`).join('');

  setText('dkSummaryFooter', `${total - otCount} ca chính · ${otCount} ca ngoài giờ`
    + (reg.phieu ? ` · Đã lưu ${fmtDateTime(reg.phieu.ngayDangKy)}` : ''));

  const valid = total >= w.soCaToiThieu && total <= w.soCaToiDa;
  if (!editable) {
    badgeEl.className = 'badge badge-neutral';
    badgeEl.textContent = 'Đã khóa';
  } else if (valid) {
    badgeEl.className = 'badge badge-success';
    badgeEl.textContent = 'Hợp lệ';
  } else {
    badgeEl.className = 'badge badge-warning';
    badgeEl.textContent = `Cần tối thiểu ${w.soCaToiThieu} ca`;
  }

  if (submitBtn) submitBtn.disabled = !(editable && valid);
}

/** Xóa toàn bộ các ca đang chọn trên lưới. */
function dkClearAll() {
  const reg = STATE.registration;
  if (!isRegistrationOpen(reg.week)) {
    showToast('Cổng đăng ký đã đóng', false);
    return;
  }
  reg.selected.clear();
  renderShiftMatrix();
}

/** Tự chọn nhanh các ca hợp lệ (ưu tiên ca chính) đủ định mức tối thiểu. */
function dkAutoSelectDemo() {
  const reg = STATE.registration;
  const w = reg.week;
  if (!isRegistrationOpen(w)) {
    showToast('Cổng đăng ký đã đóng', false);
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
}

async function dkConfirmSave() {
  closeModal('modal-DK02');
  const reg = STATE.registration;
  try {
    const res = await MedcareAPI.saveRegistration(reg.week.id, Array.from(reg.selected));
    reg.phieu = res.data;
    reg.selected = new Set(res.data.caTrucKhoaIds);
    renderShiftMatrix();
    showToast(res.message || 'Đã lưu đăng ký');
  } catch (err) {
    handleError(err);
  }
}
