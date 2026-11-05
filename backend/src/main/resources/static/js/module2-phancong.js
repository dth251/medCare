/**
 * MEDCARE CLINIC - MODULE 2: LÊN LỊCH & PHÂN CÔNG THEO TUẦN (Quản lý)
 * Mỗi thao tác gán/hủy được lưu ngay xuống CSDL; lưới luôn hiển thị dữ liệu thật.
 */

function deptStatus(k) {
  if (k.trangThaiBang === 'DA_CONG_BO') return { text: 'Đã công bố', tone: 'info', full: true };
  if (k.tongSoCa === 0) return { text: 'Không mở ca', tone: 'neutral', full: true };
  if (k.soCaDaDu >= k.tongSoCa) return { text: 'Đã đủ', tone: 'success', full: true };
  if (k.soCaDaDu === 0) return { text: 'Chưa xếp', tone: 'danger', full: false };
  return { text: 'Đang xếp', tone: 'warning', full: false };
}

// ==========================================
// TRANG CHỦ QUẢN LÝ (C03)
// ==========================================
SCREEN_LOADERS['screen-C03'] = async () => {
  const u = STATE.user;
  setText('c03Name', u.fullName);
  setText('c03Ma', [u.chucDanh, u.maNhanSu].filter(Boolean).join(' · '));
  const w = pickSchedulingWeek();
  STATE.scheduling.week = w;
  if (!w) return;
  setText('c03WeekBtnText', `Xếp lịch ${w.ten}`);
  setText('c03ProgressLabel', `Khoa đã xếp đủ · ${w.ten}`);

  const overview = await MedcareAPI.getScheduleOverview(w.id);
  STATE.scheduling.overview = overview;
  const active = overview.filter(k => k.tongSoCa > 0);
  const done = active.filter(k => deptStatus(k).full).length;
  const totalCa = active.reduce((s, k) => s + k.tongSoCa, 0);
  const fullCa = active.reduce((s, k) => s + k.soCaDaDu, 0);

  setText('c03Progress', `${done} / ${active.length}`);
  const bar = document.getElementById('c03ProgressBar');
  if (bar) bar.style.width = active.length ? `${(done / active.length) * 100}%` : '0%';
  setText('c03FullShifts', `${fullCa} / ${totalCa}`);
  setText('c03FullShiftsNote', totalCa ? `${Math.round((fullCa / totalCa) * 100)}% hoàn thành` : '');
  setText('c03TotalShifts', `${totalCa}`);
  setText('c03TotalShiftsNote', `${active.length} khoa`);

  const pending = active.filter(k => !deptStatus(k).full).slice(0, 6);
  setHtml('c03DeptList', pending.length === 0
    ? '<div class="empty">Tất cả các khoa đã xếp đủ.</div>'
    : `<table class="table"><tbody>${pending.map(k => `
        <tr>
          <td><div class="cell-strong">${escapeHtml(k.tenKhoa)}</div><div class="cell-sub">${escapeHtml(k.maKhoa)}</div></td>
          <td style="width:40%">${progressBar(k.soCaDaDu, k.tongSoCa)}</td>
          <td class="num text-right cell-sub">${k.soCaDaDu}/${k.tongSoCa}</td>
          <td class="text-right"><button onclick="pcSelectDept(${k.khoaId})" class="btn btn-secondary btn-sm">Xếp ca</button></td>
        </tr>`).join('')}</tbody></table>`);
};

// ==========================================
// DANH SÁCH KHOA (PC01)
// ==========================================
SCREEN_LOADERS['screen-PC01'] = loadSchedulingOverview;

async function loadSchedulingOverview() {
  const sc = STATE.scheduling;
  sc.week = sc.week || pickSchedulingWeek();
  const w = sc.week;
  setHtml('pcWeekSelect', weekOptionsHtml(w?.id));
  setSegmented(['pcFilterAll', 'pcFilterIncomplete', 'pcFilterComplete'],
    { ALL: 'pcFilterAll', INCOMPLETE: 'pcFilterIncomplete', COMPLETE: 'pcFilterComplete' }[sc.deptFilter || 'ALL']);
  if (!w) {
    setTableMessage('pcDeptTableBody', 6, 'Chưa có tuần làm việc nào.');
    return;
  }
  setText('pcWeekName', w.ten);
  setText('pcWeekRange', weekRange(w));
  setTableMessage('pcDeptTableBody', 6, 'Đang tải…');

  sc.overview = await MedcareAPI.getScheduleOverview(w.id);
  renderDeptListTable();
}

function pcChangeWeek(weekId) {
  const found = findWeek(weekId);
  if (!found) return;
  STATE.scheduling.week = found;
  if (STATE.currentScreen === 'screen-PC02') {
    setHtml('pc2WeekSelect', weekOptionsHtml(found.id));
    reloadDeptGrid().catch(handleError);
  } else {
    loadSchedulingOverview().catch(handleError);
  }
}

function pcQuickSwitchWeek(mode) {
  const target = mode === 'next' ? getNextWeek() : getCurrentWeek();
  if (!target) {
    showToast(mode === 'next' ? 'Không tìm thấy tuần kế tiếp' : 'Không tìm thấy tuần hiện tại', false);
    return;
  }
  pcChangeWeek(target.id);
}

function pcSetDeptFilter(mode) {
  STATE.scheduling.deptFilter = mode || 'ALL';
  setSegmented(['pcFilterAll', 'pcFilterIncomplete', 'pcFilterComplete'],
    { ALL: 'pcFilterAll', INCOMPLETE: 'pcFilterIncomplete', COMPLETE: 'pcFilterComplete' }[STATE.scheduling.deptFilter]);
  renderDeptListTable();
}

function pcJumpToFirstIncompleteDept() {
  const sc = STATE.scheduling;
  const target = (sc.overview || []).find(k => k.tongSoCa > 0 && k.soCaDaDu < k.tongSoCa);
  if (!target) {
    showToast('Tất cả các khoa đã xếp đủ');
    return;
  }
  pcSelectDept(target.khoaId);
}

/** Chuyển sang khoa kế tiếp ngay trên màn hình PC02. */
function pcNextDepartment() {
  const sc = STATE.scheduling;
  const list = (sc.overview || []).filter(k => k.tongSoCa > 0);
  if (!list.length) return;
  const idx = list.findIndex(k => k.khoaId === sc.deptId);
  const next = list[(idx + 1) % list.length];
  if (next) pcSelectDept(next.khoaId);
}

function renderDeptListTable() {
  const sc = STATE.scheduling;
  const tbody = document.getElementById('pcDeptTableBody');
  if (!tbody) return;

  const active = sc.overview.filter(k => k.tongSoCa > 0);
  const published = active.length > 0 && active.every(k => k.trangThaiBang === 'DA_CONG_BO');
  const allFull = active.length > 0 && active.every(k => deptStatus(k).full);

  const filterMode = sc.deptFilter || 'ALL';
  const filteredList = sc.overview.filter(k => {
    const st = deptStatus(k);
    if (filterMode === 'INCOMPLETE') return k.tongSoCa > 0 && !st.full;
    if (filterMode === 'COMPLETE') return k.tongSoCa > 0 && st.full;
    return true;
  });

  tbody.innerHTML = filteredList.map(k => {
    const st = deptStatus(k);
    return `<tr>
      <td><div class="cell-strong">${escapeHtml(k.tenKhoa)}</div><div class="cell-sub">${escapeHtml(k.maKhoa)}</div></td>
      <td class="text-center num">${k.soBacSi}</td>
      <td class="text-center num">${k.soYTa}</td>
      <td>
        <div class="flex items-center gap-3">
          <div class="flex-1">${progressBar(k.soCaDaDu, k.tongSoCa)}</div>
          <span class="num cell-sub" style="min-width:44px">${k.soCaDaDu}/${k.tongSoCa}</span>
        </div>
      </td>
      <td>${badge(st.text, st.tone)}</td>
      <td class="text-right">
        ${k.tongSoCa > 0 ? `<button onclick="pcSelectDept(${k.khoaId})" class="btn btn-secondary btn-sm">${k.trangThaiBang === 'DA_CONG_BO' ? 'Xem' : 'Xếp ca'}</button>` : ''}
      </td>
    </tr>`;
  }).join('') || '<tr><td colspan="6" class="empty">Không có khoa phù hợp.</td></tr>';

  const doneCount = active.filter(k => deptStatus(k).full).length;
  setText('pcOverallProgress', published
    ? `${sc.week.ten} · Đã công bố`
    : `${sc.week.ten} · ${doneCount}/${active.length} khoa đã xếp đủ`);

  const publishBtn = document.getElementById('pcPublishBtn');
  if (publishBtn) publishBtn.disabled = !(allFull && !published);
  const totalCa = active.reduce((s, k) => s + k.tongSoCa, 0);
  setHtml('pcPublishSummary', `${active.length} khoa, ${totalCa} ca trực của <strong class="text-ink">${escapeHtml(sc.week.ten)}</strong>. Sau khi công bố, lịch sẽ bị khóa chỉnh sửa.`);
}

async function pcConfirmPublish() {
  closeModal('modal-PC05');
  const sc = STATE.scheduling;
  try {
    const res = await MedcareAPI.publishWeek(sc.week.id);
    showToast(res.message || 'Đã công bố lịch');
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
  STATE.scheduling.deptId = Number(khoaId);
  STATE.scheduling.week = STATE.scheduling.week || pickSchedulingWeek();
  if (STATE.currentScreen === 'screen-PC02') {
    reloadDeptGrid().catch(handleError);
  } else {
    navigateTo('screen-PC02');
  }
}

SCREEN_LOADERS['screen-PC02'] = async () => {
  const sc = STATE.scheduling;
  if (!sc.deptId || !sc.week) {
    navigateTo('screen-PC01');
    return;
  }
  setHtml('pc2WeekSelect', weekOptionsHtml(sc.week?.id));
  setTableMessage('pcGridBody', 8, 'Đang tải…');
  await reloadDeptGrid();
};

async function reloadDeptGrid() {
  const sc = STATE.scheduling;
  const [lich, overview] = await Promise.all([
    MedcareAPI.getDepartmentSchedule(sc.deptId, sc.week.id),
    MedcareAPI.getScheduleOverview(sc.week.id).catch(() => sc.overview || [])
  ]);
  sc.lich = lich;
  sc.overview = overview;
  renderPcDeptQuickTabs();
  renderDeptGridSlots();
}

/** Ô chọn khoa (thay cho dải tab) để chuyển nhanh giữa các khoa. */
function renderPcDeptQuickTabs() {
  const sc = STATE.scheduling;
  const el = document.getElementById('pcDeptQuickTabs');
  if (!el) return;
  const list = (sc.overview || []).filter(k => k.tongSoCa > 0);
  el.innerHTML = list.length
    ? `<select class="select select-sm" onchange="pcSelectDept(this.value)">${list.map(k =>
        `<option value="${k.khoaId}" ${k.khoaId === sc.deptId ? 'selected' : ''}>${escapeHtml(k.tenKhoa)} · ${k.soCaDaDu}/${k.tongSoCa}</option>`).join('')}</select>`
    : '';
}

function slotHtml(ca, slot, vaiTro, label, locked) {
  let body;
  if (slot) {
    body = `<span class="slot-name" title="${escapeHtml(slot.hoTen)} · ${escapeHtml(slot.maNhanSu)}${slot.dieuDongNgoaiDangKy ? ' · điều động ngoài đăng ký' : ''}">${escapeHtml(slot.hoTen)}</span>
      ${slot.dieuDongNgoaiDangKy ? `<span title="Điều động ngoài đăng ký" style="color:var(--warning)">${ic('zap', 'text-[12px]')}</span>` : ''}
      ${locked ? '' : `<button onclick="pcClearSlot(${slot.phanCongId})" class="slot-remove" title="Hủy phân công">${ic('x', 'text-[13px]')}</button>`}`;
  } else if (locked) {
    body = '<span class="slot-name" style="color:var(--muted-soft);font-weight:400">Trống</span>';
  } else {
    body = `<button onclick="pcOpenSlotPicker(${ca.id}, '${vaiTro}')" class="slot-add">Chọn</button>`;
  }
  return `<div class="slot-row"><span class="slot-role">${label}</span>${body}</div>`;
}

function renderDeptGridSlots() {
  const sc = STATE.scheduling;
  const lich = sc.lich;
  const tbody = document.getElementById('pcGridBody');
  if (!tbody || !lich) return;

  const locked = lich.trangThaiBang === 'DA_CONG_BO';
  const hasIncomplete = lich.soCaDaDu < lich.tongSoCa;
  setText('pcCurrentDeptName', lich.tenKhoa);
  setText('pcDeptWeek', lich.tenTuan);
  setText('pcDeptSlotProgress', locked ? 'Đã công bố' : `${lich.soCaDaDu}/${lich.tongSoCa} ca đã đủ`);

  const autoBtn = document.getElementById('pcAutoFillDeptBtn');
  if (autoBtn) autoBtn.classList.toggle('hidden', locked || !hasIncomplete);

  const shifts = lich.danhSachCa.map(x => x.ca);
  const { days, caTypes } = buildAxes(shifts);
  setHtml('pcGridHead', shifts.length ? dayHeaderHtml(days, 'Ca') : '');

  if (!shifts.length) {
    setTableMessage('pcGridBody', 8, 'Khoa chưa mở ca trực trong tuần.');
    return;
  }

  tbody.innerHTML = caTypes.map(c => {
    const cells = days.map(d => {
      const item = lich.danhSachCa.find(x => x.ca.ngayId === d.ngayId && x.ca.caLamViecId === c.caLamViecId);
      if (!item) return '<td style="color:var(--muted-soft)">—</td>';
      const full = item.bacSiChinh && item.yTa1 && item.yTa2;
      return `<td style="vertical-align:top;padding:8px 4px">
        <div class="slot-card ${full ? 'is-full' : locked ? '' : 'is-missing'}">
          ${slotHtml(item.ca, item.bacSiChinh, 'BAC_SI_CHINH', 'BS', locked)}
          ${slotHtml(item.ca, item.yTa1, 'Y_TA_1', 'YT1', locked)}
          ${slotHtml(item.ca, item.yTa2, 'Y_TA_2', 'YT2', locked)}
        </div>
      </td>`;
    }).join('');
    return `<tr>${caTypeCellHtml(c)}${cells}</tr>`;
  }).join('');
}

/** Tự động điền toàn bộ vị trí còn trống trong khoa hiện tại bằng ứng viên hợp lệ. */
async function pcAutoFillEmptySlots() {
  const sc = STATE.scheduling;
  const lich = sc.lich;
  if (!lich || lich.trangThaiBang === 'DA_CONG_BO') return;

  const missingSlots = [];
  for (const item of lich.danhSachCa) {
    if (!item.bacSiChinh) missingSlots.push({ caId: item.ca.id, vaiTro: 'BAC_SI_CHINH' });
    if (!item.yTa1) missingSlots.push({ caId: item.ca.id, vaiTro: 'Y_TA_1' });
    if (!item.yTa2) missingSlots.push({ caId: item.ca.id, vaiTro: 'Y_TA_2' });
  }
  if (!missingSlots.length) {
    showToast('Khoa đã xếp đủ');
    return;
  }

  const autoBtn = document.getElementById('pcAutoFillDeptBtn');
  if (autoBtn) autoBtn.disabled = true;

  let assignedCount = 0;
  try {
    for (const slot of missingSlots) {
      const candidates = await MedcareAPI.getCandidates(slot.caId, slot.vaiTro);
      const valid = candidates.filter(c => !c.daCoTrongCa && c.soCaTrongNgay < c.soCaToiDaMoiNgay);
      valid.sort((a, b) =>
        (b.daDangKy ? 1 : 0) - (a.daDangKy ? 1 : 0) ||
        (b.cungKhoa ? 1 : 0) - (a.cungKhoa ? 1 : 0) ||
        a.soCaTrongNgay - b.soCaTrongNgay);
      const chosen = valid[0];
      if (chosen) {
        const dieuDong = !chosen.daDangKy;
        await MedcareAPI.assignStaff({
          caTrucKhoaId: slot.caId,
          nhanSuId: chosen.nhanSuId,
          vaiTro: slot.vaiTro,
          dieuDongNgoaiDangKy: dieuDong,
          ghiChu: dieuDong ? 'Tự động phân bổ bởi Quản lý' : null
        });
        assignedCount++;
      }
    }
    await reloadDeptGrid();
    showToast(`Đã điền ${assignedCount}/${missingSlots.length} vị trí`);
  } catch (err) {
    handleError(err);
  } finally {
    if (autoBtn) autoBtn.disabled = false;
  }
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
  sc.candidateFilter = 'ALL';
  const ca = item.ca;
  setText('pcModalTitle', `Chọn ${VAI_TRO_LABEL[vaiTro].toLowerCase()}`);
  setText('pcModalSubtitle', `${THU_LABEL[ca.thu] || ''} ${fmtDate(ca.ngay)} · Ca ${ca.tenCa} ${fmtTime(ca.gioBatDau)}–${fmtTime(ca.gioKetThuc)}`);
  const search = document.getElementById('pcStaffSearch');
  if (search) search.value = '';
  pcSetCandidateFilter('ALL', true);
  setTableMessage('pcStaffListBody', 4, 'Đang tải…');
  openModal('modal-PC03');

  try {
    sc.candidates = await MedcareAPI.getCandidates(caId, vaiTro);
    pcRenderStaffCandidates('');
  } catch (err) {
    setTableMessage('pcStaffListBody', 4, err.message, true);
  }
}

function pcSetCandidateFilter(mode, skipRender) {
  STATE.scheduling.candidateFilter = mode || 'ALL';
  setSegmented(['pcCandFilterAll', 'pcCandFilterReg', 'pcCandFilterDept', 'pcCandFilterAvail'],
    { ALL: 'pcCandFilterAll', REGISTERED: 'pcCandFilterReg', SAME_DEPT: 'pcCandFilterDept', AVAILABLE: 'pcCandFilterAvail' }[STATE.scheduling.candidateFilter]);
  if (!skipRender) {
    const q = document.getElementById('pcStaffSearch')?.value || '';
    pcFilterStaffList(q);
  }
}

function pcFilterStaffList(query) {
  pcRenderStaffCandidates((query || '').toLowerCase().trim());
}

function pcRenderStaffCandidates(query) {
  const sc = STATE.scheduling;
  const tbody = document.getElementById('pcStaffListBody');
  if (!tbody) return;
  const mode = sc.candidateFilter || 'ALL';
  const list = (sc.candidates || []).filter(c => {
    if (mode === 'REGISTERED' && !c.daDangKy) return false;
    if (mode === 'SAME_DEPT' && !c.cungKhoa) return false;
    if (mode === 'AVAILABLE' && (c.daCoTrongCa || c.soCaTrongNgay >= c.soCaToiDaMoiNgay)) return false;
    return !query
      || c.hoTen.toLowerCase().includes(query)
      || c.maNhanSu.toLowerCase().includes(query)
      || (c.tenKhoa || '').toLowerCase().includes(query);
  });

  if (!list.length) {
    setTableMessage('pcStaffListBody', 4, 'Không có nhân sự phù hợp.');
    return;
  }

  tbody.innerHTML = list.map(c => {
    const maxed = c.soCaTrongNgay >= c.soCaToiDaMoiNgay;
    const blocked = maxed || c.daCoTrongCa;
    const reason = c.daCoTrongCa ? 'Đã trong ca' : maxed ? 'Đủ ca' : '';
    const wish = c.daDangKy
      ? badge('Đã đăng ký', 'success')
      : badge(c.cungKhoa ? 'Chưa đăng ký' : 'Khác khoa', 'warning');
    return `<tr>
      <td>
        <div class="cell-strong">${escapeHtml(c.hoTen)}</div>
        <div class="cell-sub">${escapeHtml(c.maNhanSu)}${c.tenKhoa ? ' · ' + escapeHtml(c.tenKhoa) : ''}</div>
      </td>
      <td class="text-center num ${maxed ? '' : 'cell-sub'}" ${maxed ? 'style="color:var(--danger)"' : ''}>${c.soCaTrongNgay}/${c.soCaToiDaMoiNgay}</td>
      <td>${wish}</td>
      <td class="text-right">
        <button onclick="pcAssignCandidate(${c.nhanSuId})" ${blocked ? 'disabled' : ''} class="btn ${blocked ? 'btn-ghost' : 'btn-primary'} btn-sm">${blocked ? reason : 'Chọn'}</button>
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
    setText('pcDispatchStaffName', cand.hoTen);
    setText('pcDispatchReason', cand.cungKhoa ? 'chưa đăng ký' : `thuộc ${cand.tenKhoa} và chưa đăng ký`);
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
      ghiChu: dieuDong ? 'Điều động ngoài đăng ký' : null
    });
    closeModal('modal-PC03');
    showToast(res.message || `Đã xếp ${cand.hoTen}`);
    await reloadDeptGrid();
  } catch (err) {
    handleError(err);
  }
}
