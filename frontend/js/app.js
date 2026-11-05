/**
 * MEDCARE CLINIC - ĐIỀU HƯỚNG, PHÂN QUYỀN MÀN HÌNH, MODAL & THÔNG BÁO
 */

// Hàm nạp dữ liệu cho từng màn hình (được đăng ký bởi các module)
const SCREEN_LOADERS = {};

const SCREEN_TITLES = {
  'screen-C01': 'Đăng nhập hệ thống',
  'screen-C02': 'Trang chủ Bác sĩ & Y tá',
  'screen-C03': 'Trang chủ Quản lý',
  'screen-C04': 'Trang chủ Kế toán',
  'screen-C05': 'Trung tâm chọn phân hệ',
  'screen-DK01': 'Đăng ký ca trực theo tuần',
  'screen-PC01': 'Danh sách khoa & Tiến độ xếp lịch',
  'screen-PC02': 'Phân ca chi tiết khoa',
  'screen-TT01': 'Danh sách khoa thanh toán thù lao',
  'screen-TT02': 'Danh sách nhân sự của khoa',
  'screen-TT03': 'Bảng chấm công chi tiết',
  'screen-TT05': 'Đối soát & Xử lý khiếu nại',
  'screen-TT06': 'Thanh toán chi trả thù lao',
  'screen-TT07': 'Phiếu thanh toán thù lao',
  'screen-TT08': 'Lịch sử phiếu thù lao cá nhân'
};

function canAccess(screenId) {
  if (screenId === 'screen-C01') return true;
  if (!STATE.user) return false;
  return (ROLE_SCREENS[STATE.user.vaiTro] || []).includes(screenId);
}

function homeScreen() {
  return STATE.user ? (ROLE_HOME[STATE.user.vaiTro] || 'screen-C05') : 'screen-C01';
}

// ==========================================
// ĐIỀU HƯỚNG
// ==========================================
function navigateTo(targetId) {
  if (targetId.startsWith('modal-')) {
    openModal(targetId);
    return;
  }

  if (!canAccess(targetId)) {
    if (STATE.user) showToast('Bạn không có quyền truy cập chức năng này', false);
    targetId = homeScreen();
  }

  const target = document.getElementById(targetId);
  if (!target) return;

  document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active', 'animate-fadeIn'));
  target.classList.add('active', 'animate-fadeIn');
  STATE.currentScreen = targetId;
  window.scrollTo({ top: 0, behavior: 'smooth' });
  updateBreadcrumb(targetId);
  history.replaceState(null, '', '#' + targetId);

  const loader = SCREEN_LOADERS[targetId];
  if (loader) {
    Promise.resolve()
      .then(() => loader())
      .catch(err => handleError(err));
  }
}

function goBackFromCurrentScreen() {
  const back = {
    'screen-DK01': 'screen-C02',
    'screen-TT08': 'screen-C02',
    'screen-PC01': 'screen-C03',
    'screen-PC02': 'screen-PC01',
    'screen-TT01': 'screen-C04',
    'screen-TT02': 'screen-TT01',
    'screen-TT05': 'screen-C04',
    'screen-TT06': 'screen-TT02'
  };
  const current = STATE.currentScreen;
  if (current === 'screen-TT03') {
    navigateTo(STATE.user?.vaiTro === 'NHAN_SU' ? 'screen-C02' : 'screen-TT02');
  } else if (current === 'screen-TT07') {
    navigateTo(STATE.payment.slipBackScreen || homeScreen());
  } else {
    navigateTo(back[current] || homeScreen());
  }
}

function updateBreadcrumb(screenId) {
  const titleElem = document.getElementById('currentScreenTitle');
  if (titleElem) titleElem.textContent = SCREEN_TITLES[screenId] || screenId;
}

// ==========================================
// MODAL & TOAST
// ==========================================
function openModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) { m.classList.remove('hidden'); m.classList.add('flex'); }
}

function closeModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) { m.classList.add('hidden'); m.classList.remove('flex'); }
}

let toastTimer = null;
function showToast(message, isSuccess = true) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  const msg = document.getElementById('toastMessage');
  const icon = document.getElementById('toastIcon');
  if (msg) msg.textContent = message;
  if (icon) {
    icon.className = `w-6 h-6 rounded-full flex items-center justify-center text-xs text-white shrink-0 ${isSuccess ? 'bg-emerald-500' : 'bg-rose-500'}`;
    icon.innerHTML = isSuccess ? '<i class="fa-solid fa-check"></i>' : '<i class="fa-solid fa-triangle-exclamation"></i>';
  }
  toast.classList.remove('translate-y-20', 'opacity-0');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.add('translate-y-20', 'opacity-0'), isSuccess ? 3200 : 5500);
}

/** Hiển thị lỗi API thật sự (không còn báo "thành công" khi lỗi). */
function handleError(err) {
  console.warn('[MedCare]', err);
  if (err && err.sessionExpired) return; // đã chuyển về màn đăng nhập (sai mật khẩu vẫn được báo)
  showToast(err?.message || 'Đã có lỗi xảy ra', false);
}

/** Đặt nội dung "đang tải" / "trống" cho tbody. */
function setTableMessage(tbodyId, colspan, message, isError = false) {
  const tbody = document.getElementById(tbodyId);
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="${colspan}" class="py-6 text-center ${isError ? 'text-rose-500' : 'text-slate-400'} italic">${escapeHtml(message)}</td></tr>`;
  }
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value ?? '';
}

function setHtml(id, html) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = html;
}

// ==========================================
// KHỞI TẠO
// ==========================================
window.addEventListener('medcare:unauthorized', () => {
  if (STATE.user) {
    STATE.user = null;
    applyUserToChrome();
    showToast('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại', false);
  }
  navigateTo('screen-C01');
});

window.addEventListener('DOMContentLoaded', async () => {
  const isOnline = await MedcareAPI.ping();
  const badge = document.getElementById('backendStatusBadge');
  if (badge) {
    badge.className = isOnline
      ? 'flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-emerald-950 text-emerald-300 border border-emerald-500/40'
      : 'flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-rose-950 text-rose-300 border border-rose-500/40';
    badge.innerHTML = isOnline
      ? '<span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>Spring Boot API: Online'
      : '<span class="w-2 h-2 rounded-full bg-rose-400"></span>Spring Boot API: Offline';
  }

  const requested = window.location.hash.replace('#', '');
  if (isOnline && MedcareAPI.getToken()) {
    try {
      await startSession();
      navigateTo(requested && canAccess(requested) && requested !== 'screen-C01' ? requested : homeScreen());
      return;
    } catch (err) {
      MedcareAPI.clearSession();
    }
  }
  applyUserToChrome();
  navigateTo('screen-C01');
  if (!isOnline) showToast('Không kết nối được Backend. Hãy chạy Spring Boot ở cổng 8080.', false);
});
