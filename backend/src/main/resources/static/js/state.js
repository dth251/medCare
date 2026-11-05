/**
 * MEDCARE CLINIC - TRẠNG THÁI ỨNG DỤNG & HÀM TIỆN ÍCH
 * Không còn dữ liệu mẫu: mọi dữ liệu được nạp từ Backend sau khi đăng nhập.
 */

const STATE = {
  user: null,                 // Hồ sơ từ /api/auth/me
  weeks: [],                  // Danh sách tuần (mới nhất trước)
  currentScreen: 'screen-C01',

  registration: {
    week: null,
    shifts: [],               // Ca trực của khoa trong tuần
    selected: new Set(),      // ID ca trực đã chọn
    phieu: null
  },

  scheduling: {
    week: null,
    overview: [],
    deptId: null,
    lich: null,
    picking: null,            // { ca, vaiTro }
    candidates: [],
    pendingCandidate: null
  },

  payment: {
    week: null,
    depts: [],
    deptId: null,
    staff: [],
    payTarget: null,          // Nhân sự đang chi trả (TT06)
    timesheetOwner: null,     // { nhanSuId, hoTen, maNhanSu, tenKhoa }
    timesheetWeek: null,
    timesheet: [],
    complaintRow: null,       // Dòng chấm công đang khiếu nại (TT04)
    complaintFilter: 'CHO_XU_LY',
    complaints: [],
    complaintId: null,
    slip: null,
    slipBackScreen: null
  }
};

// Màn hình được phép theo vai trò (đối chiếu với phân quyền phía backend)
const ROLE_SCREENS = {
  NHAN_SU: ['screen-C02', 'screen-C05', 'screen-DK01', 'screen-TT03', 'screen-TT07', 'screen-TT08'],
  QUAN_LY: ['screen-C03', 'screen-C05', 'screen-PC01', 'screen-PC02'],
  KE_TOAN: ['screen-C04', 'screen-C05', 'screen-TT01', 'screen-TT02', 'screen-TT03', 'screen-TT05', 'screen-TT06', 'screen-TT07', 'screen-TT09']
};

const ROLE_HOME = { NHAN_SU: 'screen-C02', QUAN_LY: 'screen-C03', KE_TOAN: 'screen-C04' };

const THU_LABEL = {
  MONDAY: 'Thứ 2', TUESDAY: 'Thứ 3', WEDNESDAY: 'Thứ 4', THURSDAY: 'Thứ 5',
  FRIDAY: 'Thứ 6', SATURDAY: 'Thứ 7', SUNDAY: 'Chủ Nhật'
};
const THU_SHORT = {
  MONDAY: 'T2', TUESDAY: 'T3', WEDNESDAY: 'T4', THURSDAY: 'T5',
  FRIDAY: 'T6', SATURDAY: 'T7', SUNDAY: 'CN'
};

const TRANG_THAI_TUAN = {
  DANG_MO_DANG_KY: 'Đang mở đăng ký',
  DA_DONG_DANG_KY: 'Đã đóng đăng ký',
  DANG_XEP_LICH: 'Đang xếp lịch',
  DA_CONG_BO: 'Đã công bố lịch',
  KET_THUC: 'Đã kết thúc'
};

const TRANG_THAI_CHAM_CONG = {
  DUNG_GIO: 'Đúng giờ',
  DI_MUON: 'Đi muộn',
  VE_SOM: 'Về sớm',
  VANG_MAT: 'Vắng mặt',
  LOI_THIET_BI: 'Thiếu dữ liệu quẹt thẻ',
  CHUA_CO_DU_LIEU: 'Chưa có dữ liệu'
};

const TRANG_THAI_KHIEU_NAI = { CHO_XU_LY: 'Chờ xử lý', DA_XU_LY: 'Đã xử lý', TU_CHOI: 'Bị từ chối' };

const VAI_TRO_LABEL = { BAC_SI_CHINH: 'Bác sĩ chính', Y_TA_1: 'Y tá 1', Y_TA_2: 'Y tá 2' };

// ==========================================
// HÀM TIỆN ÍCH
// ==========================================
function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function fmtMoney(v) {
  const n = Number(v || 0);
  return Math.round(n).toLocaleString('vi-VN') + 'đ';
}

/** '2026-09-07' -> '07/09/2026' */
function fmtDate(iso) {
  if (!iso) return '—';
  const [y, m, d] = String(iso).slice(0, 10).split('-');
  return `${d}/${m}/${y}`;
}

/** '2026-09-07' -> '07/09' */
function fmtDateShort(iso) {
  if (!iso) return '';
  const [, m, d] = String(iso).slice(0, 10).split('-');
  return `${d}/${m}`;
}

function fmtDateTime(iso) {
  if (!iso) return '—';
  const s = String(iso);
  return `${s.slice(11, 16)} ${fmtDate(s.slice(0, 10))}`;
}

function fmtTime(t) {
  return t ? String(t).slice(0, 5) : '--:--';
}

/** Ngày hiện tại định dạng YYYY-MM-DD theo giờ máy tính. */
function localDateIso(d = new Date()) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** Kiểm tra tuần w có phải Tuần làm việc hiện tại (chứa ngày hôm nay) không. */
function isCurrentWeek(w) {
  if (!w || !w.ngayBatDau || !w.ngayKetThuc) return false;
  const today = localDateIso();
  return w.ngayBatDau <= today && today <= w.ngayKetThuc;
}

/** Kiểm tra tuần w có phải Tuần làm việc kế tiếp (chứa ngày hôm nay + 7 ngày) không. */
function isNextWeek(w) {
  if (!w || !w.ngayBatDau || !w.ngayKetThuc) return false;
  const next = new Date();
  next.setDate(next.getDate() + 7);
  const nextIso = localDateIso(next);
  return w.ngayBatDau <= nextIso && nextIso <= w.ngayKetThuc;
}

/** Nhãn phân biệt tự động: Tuần hiện tại / Tuần kế tiếp / Tuần trước. */
function weekRelationLabel(w) {
  if (!w) return '';
  if (isCurrentWeek(w)) return 'Tuần hiện tại';
  if (isNextWeek(w)) return 'Tuần kế tiếp';
  const today = localDateIso();
  if (w.ngayKetThuc && w.ngayKetThuc < today) return 'Tuần trước';
  if (w.ngayBatDau && w.ngayBatDau > today) return 'Tuần sắp tới';
  return '';
}

function getCurrentWeek() {
  return STATE.weeks.find(isCurrentWeek) || null;
}

function getNextWeek() {
  return STATE.weeks.find(isNextWeek) || null;
}

function weekLabel(w) {
  if (!w) return '—';
  const rel = weekRelationLabel(w);
  return `${w.ten} (${fmtDate(w.ngayBatDau)} – ${fmtDate(w.ngayKetThuc)})${rel ? ' • ' + rel : ''}`;
}

function weekRange(w) {
  if (!w) return '';
  const rel = weekRelationLabel(w);
  return `${fmtDate(w.ngayBatDau)} – ${fmtDate(w.ngayKetThuc)}${rel ? ' (' + rel + ')' : ''}`;
}

/** Tự động bắt Tuần kế tiếp (đang mở đăng ký) cho chức năng Đăng ký ca. */
function pickRegistrationWeek() {
  return getNextWeek()
    || STATE.weeks.find(w => w.trangThai === 'DANG_MO_DANG_KY')
    || getCurrentWeek()
    || STATE.weeks[0]
    || null;
}

/** Tự động bắt Tuần kế tiếp cho chức năng Lên lịch & Phân công của Quản lý. */
function pickSchedulingWeek() {
  return getNextWeek()
    || STATE.weeks.find(w => w.trangThai !== 'KET_THUC')
    || getCurrentWeek()
    || STATE.weeks[0]
    || null;
}

/** Tự động bắt Tuần hiện tại (hoặc tuần vừa kết thúc) cho chức năng Chấm công & Quyết toán. */
function pickPaymentWeek() {
  return getCurrentWeek()
    || STATE.weeks.find(w => w.trangThai === 'KET_THUC')
    || STATE.weeks[0]
    || null;
}

function findWeek(id) {
  return STATE.weeks.find(w => String(w.id) === String(id)) || null;
}

function weekOptionsHtml(selectedId) {
  return STATE.weeks.map(w => {
    const rel = weekRelationLabel(w);
    const text = `${w.ten} · ${fmtDateShort(w.ngayBatDau)} – ${fmtDateShort(w.ngayKetThuc)}${rel ? ' · ' + rel : ''}`;
    return `<option value="${w.id}" ${String(w.id) === String(selectedId) ? 'selected' : ''}>${escapeHtml(text)}</option>`;
  }).join('');
}

// ==========================================
// THÀNH PHẦN GIAO DIỆN DÙNG CHUNG
// ==========================================
/** Badge trạng thái: tone = success | warning | danger | info | neutral */
function badge(text, tone = 'neutral') {
  return `<span class="badge badge-${tone}">${escapeHtml(text)}</span>`;
}

/** Thanh tiến độ mảnh. */
function progressBar(done, total) {
  const pct = total ? Math.round((done / total) * 100) : 0;
  return `<div class="progress ${total && done >= total ? 'is-done' : ''}"><span style="width:${pct}%"></span></div>`;
}

/** Bật trạng thái đang chọn cho một nhóm nút segmented theo id. */
function setSegmented(ids, activeId) {
  ids.forEach(id => {
    const btn = document.getElementById(id);
    if (btn) btn.classList.toggle('is-active', id === activeId);
  });
}

/** Chữ cái đầu cho avatar. */
function initials(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  return (parts.length === 1 ? parts[0][0] : parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/** Đọc số tiền bằng chữ (tiếng Việt). */
function numberToVietnameseWords(amount) {
  const n = Math.round(Number(amount || 0));
  if (n === 0) return 'Không đồng';
  const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];
  const units = ['', ' nghìn', ' triệu', ' tỷ'];

  function readTriple(num, full) {
    const h = Math.floor(num / 100), t = Math.floor((num % 100) / 10), u = num % 10;
    const parts = [];
    if (full || h > 0) parts.push(digits[h] + ' trăm');
    if (t > 1) {
      parts.push(digits[t] + ' mươi');
      if (u === 1) parts.push('mốt');
      else if (u === 5) parts.push('lăm');
      else if (u > 0) parts.push(digits[u]);
    } else if (t === 1) {
      parts.push('mười');
      if (u === 5) parts.push('lăm');
      else if (u > 0) parts.push(digits[u]);
    } else if (u > 0) {
      if (full || h > 0) parts.push('lẻ');
      parts.push(digits[u]);
    }
    return parts.join(' ');
  }

  const groups = [];
  let rest = Math.abs(n);
  while (rest > 0) { groups.push(rest % 1000); rest = Math.floor(rest / 1000); }
  const words = [];
  for (let i = groups.length - 1; i >= 0; i--) {
    if (groups[i] === 0) continue;
    words.push(readTriple(groups[i], i < groups.length - 1) + units[i % 4]);
  }
  const text = words.join(' ').trim();
  return (n < 0 ? 'Âm ' : '') + text.charAt(0).toUpperCase() + text.slice(1) + ' đồng';
}
