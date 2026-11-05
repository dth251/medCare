/**
 * MEDCARE ENTERPRISE - ĐIỀU HƯỚNG CHUẨN DESIGN.MD, ĐỒNG BỘ NÚT BACK TRÌNH DUYỆT (POPSTATE),
 * PHÂN QUYỀN MÀN HÌNH, MODAL & THÔNG BÁO
 */

// Đảm bảo tương thích ngược mọi nơi gọi renderUserBadge
window.renderUserBadge = function() {
  if (typeof applyUserToChrome === 'function') {
    applyUserToChrome();
  }
};

// Hàm nạp dữ liệu cho từng màn hình (được đăng ký bởi các module)
const SCREEN_LOADERS = {};

// Lịch sử điều hướng nội bộ để hỗ trợ nút Back / Forward mượt mà
const NAV_STACK = {
  back: [],
  forward: []
};

const SCREEN_TITLES = {
  'screen-C01': 'Đăng nhập hệ thống',
  'screen-C02': 'Trang chủ Bác sĩ & Y tá',
  'screen-C03': 'Tổng quan Điều phối',
  'screen-C04': 'Tổng quan Tài chính',
  'screen-C05': 'Trung tâm chọn phân hệ',
  'screen-DK01': 'Đăng ký ca trực theo tuần',
  'screen-PC01': 'Tiến độ xếp lịch 6 chuyên khoa',
  'screen-PC02': 'Lưới phân ca chi tiết khoa',
  'screen-TT01': 'Quyết toán thù lao theo khoa',
  'screen-TT02': 'Bảng lương nhân sự chuyên khoa',
  'screen-TT03': 'Bảng chấm công chi tiết',
  'screen-TT05': 'Đối soát & Xử lý khiếu nại',
  'screen-TT06': 'Xác nhận chi trả thù lao',
  'screen-TT07': 'Phiếu thanh toán thù lao (A4)',
  'screen-TT08': 'Lịch sử phiếu lương cá nhân',
  'screen-TT09': 'Phiếu lương'
};

const SCREEN_SHORT_LABELS = {
  'screen-C02': 'Trang chủ',
  'screen-C03': 'Tổng quan',
  'screen-C04': 'Tổng quan',
  'screen-C05': 'Phân hệ',
  'screen-DK01': 'Đăng ký ca trực',
  'screen-PC01': 'Danh sách 6 khoa',
  'screen-PC02': 'Lưới xếp ca khoa',
  'screen-TT01': 'Quyết toán 6 khoa',
  'screen-TT02': 'Bảng lương khoa',
  'screen-TT03': 'Bảng chấm công',
  'screen-TT05': 'Đối soát khiếu nại',
  'screen-TT06': 'Chi trả thù lao',
  'screen-TT07': 'Phiếu lương A4',
  'screen-TT08': 'Phiếu lương cá nhân'
};

function canAccess(screenId) {
  if (screenId === 'screen-C01') return true;
  if (!STATE.user) return false;
  return (ROLE_SCREENS[STATE.user.vaiTro] || []).includes(screenId);
}

function homeScreen() {
  return STATE.user ? (ROLE_HOME[STATE.user.vaiTro] || 'screen-C05') : 'screen-C01';
}

/** Xác định màn hình quay lại hợp lý nhất cho màn hiện tại. */
function getLogicalBackScreen(currentScreen) {
  const current = currentScreen || STATE.currentScreen;
  if (!STATE.user || current === 'screen-C01') return null;

  const backMap = {
    'screen-DK01': 'screen-C02',
    'screen-TT08': 'screen-C02',
    'screen-PC01': 'screen-C03',
    'screen-PC02': 'screen-PC01',
    'screen-TT01': 'screen-C04',
    'screen-TT02': 'screen-TT01',
    'screen-TT05': STATE.payment?.deptId ? 'screen-TT02' : 'screen-C04',
    'screen-TT06': 'screen-TT02',
    'screen-TT09': 'screen-C04',
    'screen-C05': homeScreen()
  };

  if (current === 'screen-TT03') {
    return STATE.user.vaiTro === 'NHAN_SU' ? 'screen-C02' : 'screen-TT02';
  }
  if (current === 'screen-TT07') {
    return STATE.payment?.slipBackScreen || (STATE.user.vaiTro === 'NHAN_SU' ? 'screen-TT08' : 'screen-TT02');
  }
  if (backMap[current]) return backMap[current];

  // Nếu đang ở Trang chủ (C02/C03/C04) nhưng có lịch sử trước đó thì cho phép quay lại màn vừa xem
  for (let i = NAV_STACK.back.length - 1; i >= 0; i--) {
    const prev = NAV_STACK.back[i];
    if (prev && prev !== current && prev !== 'screen-C01' && canAccess(prev)) {
      return prev;
    }
  }
  return null;
}

/** Xác định bước tiếp theo hợp lý trong quy trình làm việc của từng vai trò. */
function getLogicalNextStep(currentScreen) {
  const current = currentScreen || STATE.currentScreen;
  const role = STATE.user?.vaiTro;
  if (!role || current === 'screen-C01') return null;

  if (role === 'NHAN_SU') {
    const map = {
      'screen-C02': { label: 'Đăng ký ca trực', action: () => navigateTo('screen-DK01') },
      'screen-DK01': { label: 'Chấm công của tôi', action: () => ttOpenMyTimesheet() },
      'screen-TT03': { label: 'Phiếu lương cá nhân', action: () => navigateTo('screen-TT08') },
      'screen-TT08': { label: 'Đăng ký ca trực', action: () => navigateTo('screen-DK01') },
      'screen-TT07': { label: 'Danh sách phiếu lương', action: () => navigateTo('screen-TT08') }
    };
    return map[current] || null;
  }

  if (role === 'QUAN_LY') {
    const map = {
      'screen-C03': { label: 'Tiến độ 6 chuyên khoa', action: () => navigateTo('screen-PC01') },
      'screen-PC01': { label: 'Xếp ca chi tiết khoa', action: () => navOpenScheduleDetail() },
      'screen-PC02': { label: 'Khoa tiếp theo', action: () => (typeof pcNextDepartment === 'function' ? pcNextDepartment() : navigateTo('screen-PC01')) }
    };
    return map[current] || null;
  }

  if (role === 'KE_TOAN') {
    const map = {
      'screen-C04': { label: 'Quyết toán 6 chuyên khoa', action: () => navigateTo('screen-TT01') },
      'screen-TT01': { label: 'Bảng lương nhân sự khoa', action: () => navOpenStaffPayroll() },
      'screen-TT02': { label: 'Đối soát khiếu nại', action: () => navigateTo('screen-TT05') },
      'screen-TT03': { label: 'Đối soát khiếu nại', action: () => navigateTo('screen-TT05') },
      'screen-TT05': { label: 'Bảng lương nhân sự', action: () => navOpenStaffPayroll() },
      'screen-TT06': { label: 'Bảng lương nhân sự', action: () => navigateTo('screen-TT02') },
      'screen-TT07': { label: 'Bảng lương nhân sự', action: () => navigateTo('screen-TT02') }
    };
    return map[current] || null;
  }

  return null;
}

// ==========================================
// ĐIỀU HƯỚNG & ĐỒNG BỘ LỊCH SỬ TRÌNH DUYỆT
// ==========================================
function navigateTo(targetId, options = {}) {
  if (!targetId) return;
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

  const prevScreen = STATE.currentScreen;
  if (!options.fromHistory && prevScreen && prevScreen !== targetId && prevScreen !== 'screen-C01') {
    NAV_STACK.back.push(prevScreen);
    if (NAV_STACK.back.length > 30) NAV_STACK.back.shift();
    if (!options.keepForward) NAV_STACK.forward = [];
  }

  document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active', 'animate-fadeIn'));
  target.classList.add('active', 'animate-fadeIn');
  STATE.currentScreen = targetId;
  window.scrollTo({ top: 0, behavior: 'smooth' });

  // Đồng bộ URL & History API của trình duyệt để nút Back/Forward của trình duyệt hoạt động thật
  if (!options.fromPopState) {
    const stateObj = {
      screenId: targetId,
      deptId: STATE.scheduling?.deptId || STATE.payment?.deptId || null
    };
    if (options.replaceHistory || prevScreen === targetId) {
      history.replaceState(stateObj, '', '#' + targetId);
    } else {
      history.pushState(stateObj, '', '#' + targetId);
    }
  }

  updateBreadcrumb(targetId);
  if (typeof renderMainNav === 'function') renderMainNav();

  const loader = SCREEN_LOADERS[targetId];
  if (loader) {
    Promise.resolve()
      .then(() => loader())
      .catch(err => handleError(err));
  }
}

/** Xử lý khi người dùng bấm nút Quay lại (trên giao diện hoặc phím tắt). */
function goBackFromCurrentScreen() {
  if (!STATE.user || STATE.currentScreen === 'screen-C01') return;

  const targetBack = getLogicalBackScreen(STATE.currentScreen);
  if (targetBack) {
    NAV_STACK.forward.push(STATE.currentScreen);
    navigateTo(targetBack, { fromHistory: true, keepForward: true });
  } else {
    const home = homeScreen();
    if (STATE.currentScreen !== home) {
      NAV_STACK.forward.push(STATE.currentScreen);
      navigateTo(home, { fromHistory: true, keepForward: true });
    } else {
      showToast('Bạn đang ở màn hình Trang chủ');
    }
  }
}

/** Xử lý khi người dùng bấm nút Tiến tới (Forward). */
function goForwardFromCurrentScreen() {
  if (!STATE.user) return;
  while (NAV_STACK.forward.length > 0) {
    const next = NAV_STACK.forward.pop();
    if (next && next !== STATE.currentScreen && canAccess(next)) {
      NAV_STACK.back.push(STATE.currentScreen);
      navigateTo(next, { fromHistory: true, keepForward: true });
      return;
    }
  }
  goNextStepFromCurrentScreen();
}

/** Chuyển nhanh sang bước tiếp theo trong quy trình nghiệp vụ. */
function goNextStepFromCurrentScreen() {
  const nextStep = getLogicalNextStep(STATE.currentScreen);
  if (nextStep && typeof nextStep.action === 'function') {
    nextStep.action();
  }
}

function updateBreadcrumb(screenId) {
  // Breadcrumb không cần thiết trên giao diện tối giản
}

/** Cập nhật trạng thái điều hướng khi chuyển màn hình. */
function updateErgonomicNavControls(screenId) {
  // Không cần xử lý các nút rườm rà trên header nữa vì mỗi màn hình con đã có nút Quay lại riêng tinh tế.
}

// Lắng nghe sự kiện nhấn nút Back / Forward của chính trình duyệt web
window.addEventListener('popstate', (event) => {
  if (!STATE.user) {
    navigateTo('screen-C01', { fromPopState: true, fromHistory: true });
    return;
  }
  const hashScreen = window.location.hash ? window.location.hash.replace('#', '') : '';
  const targetScreen = event.state?.screenId || hashScreen || homeScreen();
  if (targetScreen && targetScreen !== STATE.currentScreen) {
    if (canAccess(targetScreen)) {
      navigateTo(targetScreen, { fromPopState: true, fromHistory: true });
    } else {
      navigateTo(homeScreen(), { fromPopState: true, fromHistory: true });
    }
  }
});

// Hỗ trợ phím tắt Alt + Mũi tên Trái / Phải như ứng dụng Desktop chuẩn Linear
window.addEventListener('keydown', (e) => {
  if (!STATE.user || STATE.currentScreen === 'screen-C01') return;
  if (e.altKey && e.key === 'ArrowLeft') {
    e.preventDefault();
    goBackFromCurrentScreen();
  } else if (e.altKey && e.key === 'ArrowRight') {
    e.preventDefault();
    goForwardFromCurrentScreen();
  }
});

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
    icon.className = `toast-icon ${isSuccess ? 'is-success' : 'is-error'}`;
    icon.innerHTML = ic(isSuccess ? 'circle-check' : 'alert');
  }
  toast.classList.remove('is-hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.add('is-hidden'), isSuccess ? 3000 : 5500);
}

/** Hiển thị lỗi API thật sự. */
function handleError(err) {
  console.warn('[MedCare]', err);
  if (err && err.sessionExpired) return;
  showToast(err?.message || 'Đã có lỗi xảy ra', false);
}

/** Đặt nội dung "đang tải" / "trống" cho tbody. */
function setTableMessage(tbodyId, colspan, message, isError = false) {
  const tbody = document.getElementById(tbodyId);
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="${colspan}" class="empty"${isError ? ' style="color:var(--danger)"' : ''}>${escapeHtml(message)}</td></tr>`;
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
    NAV_STACK.back = [];
    NAV_STACK.forward = [];
    applyUserToChrome();
    showToast('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại', false);
  }
  navigateTo('screen-C01', { replaceHistory: true });
});

window.addEventListener('DOMContentLoaded', async () => {
  const liveDateEl = document.getElementById('loginLiveDateText');
  if (liveDateEl) {
    const now = new Date();
    const dStr = now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
    liveDateEl.textContent = `Đồng bộ lịch tự động • ${dStr}`;
  }

  const isOnline = await MedcareAPI.ping();


  const requested = window.location.hash.replace('#', '');
  if (isOnline && MedcareAPI.getToken()) {
    try {
      await startSession();
      const initialScreen = requested && canAccess(requested) && requested !== 'screen-C01' ? requested : homeScreen();
      if (initialScreen !== homeScreen()) {
        // Đảm bảo luôn có màn hình Trang chủ trong lịch sử trình duyệt khi F5 ở màn hình con
        history.replaceState({ screenId: homeScreen() }, '', '#' + homeScreen());
        NAV_STACK.back.push(homeScreen());
        navigateTo(initialScreen);
      } else {
        navigateTo(initialScreen, { replaceHistory: true });
      }
      return;
    } catch (err) {
      MedcareAPI.clearSession();
    }
  }
  applyUserToChrome();
  navigateTo('screen-C01', { replaceHistory: true });
  if (!isOnline) showToast('Không kết nối được máy chủ. Vui lòng kiểm tra dịch vụ hệ thống.', false);
});
