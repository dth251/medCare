/**
 * MEDCARE CLINIC - ĐĂNG NHẬP / ĐĂNG XUẤT & HIỂN THỊ THÔNG TIN NGƯỜI DÙNG
 * Vai trò lấy từ backend (/api/auth/me), không còn nút "đóng vai" phía client.
 */

const ROLE_STYLE = {
  NHAN_SU: { icon: 'fa-user-doctor', cls: 'bg-blue-50 text-blue-700 border border-blue-200' },
  QUAN_LY: { icon: 'fa-clipboard-user', cls: 'bg-purple-50 text-purple-700 border border-purple-200' },
  KE_TOAN: { icon: 'fa-calculator', cls: 'bg-emerald-50 text-emerald-700 border border-emerald-200' }
};

function quickFillLogin(username) {
  const uInput = document.getElementById('loginUsername');
  const pInput = document.getElementById('loginPassword');
  if (uInput) uInput.value = username;
  if (pInput) pInput.value = '123456';
}

/** Nạp hồ sơ + danh sách tuần sau khi có token. */
async function startSession() {
  const [me, weeks] = await Promise.all([MedcareAPI.me(), MedcareAPI.getWeeks()]);
  STATE.user = me;
  STATE.weeks = weeks;
  applyUserToChrome();
}

async function handleLogin(e) {
  if (e) e.preventDefault();
  const u = document.getElementById('loginUsername')?.value.trim();
  const p = document.getElementById('loginPassword')?.value;
  const btn = document.getElementById('loginSubmitBtn');
  if (btn) btn.disabled = true;

  try {
    await MedcareAPI.login(u, p);
    await startSession();
    showToast(`Đăng nhập thành công! Chào mừng ${STATE.user.fullName}`);
    navigateTo(homeScreen());
  } catch (err) {
    MedcareAPI.clearSession();
    STATE.user = null;
    applyUserToChrome();
    handleError(err);
  } finally {
    if (btn) btn.disabled = false;
  }
}

function logout() {
  MedcareAPI.clearSession();
  STATE.user = null;
  STATE.weeks = [];
  applyUserToChrome();
  navigateTo('screen-C01');
  showToast('Đã đăng xuất');
}

function displayName(user) {
  if (!user) return '';
  const prefix = user.chucDanh === 'Bác sĩ' ? 'BS. ' : user.chucDanh === 'Y tá' ? 'YT. '
    : user.vaiTro === 'QUAN_LY' ? 'QL. ' : user.vaiTro === 'KE_TOAN' ? 'KT. ' : '';
  return prefix + user.fullName;
}

/** Cập nhật thanh trên cùng & breadcrumb theo người dùng hiện tại. */
function applyUserToChrome() {
  const user = STATE.user;
  const badge = document.getElementById('roleBadge');
  const topUser = document.getElementById('topUserInfo');
  const logoutBtn = document.getElementById('logoutBtn');

  if (!user) {
    if (badge) badge.classList.add('hidden');
    if (topUser) topUser.classList.add('hidden');
    if (logoutBtn) logoutBtn.classList.add('hidden');
    return;
  }

  const style = ROLE_STYLE[user.vaiTro] || ROLE_STYLE.NHAN_SU;
  if (badge) {
    badge.className = `${style.cls} px-2.5 py-0.5 rounded-full font-medium text-[11px] flex items-center gap-1.5`;
    badge.innerHTML = `<i class="fa-solid ${style.icon}"></i> <span>${escapeHtml(displayName(user))} (${escapeHtml(user.maNhanSu)})</span>`;
  }
  if (topUser) {
    topUser.classList.remove('hidden');
    topUser.innerHTML = `<i class="fa-solid ${style.icon} mr-1"></i>${escapeHtml(user.chucDanh)} • ${escapeHtml(user.username)}`;
  }
  if (logoutBtn) logoutBtn.classList.remove('hidden');
}

// ==========================================
// TRANG CHỦ THEO VAI TRÒ & CHỌN PHÂN HỆ
// ==========================================
SCREEN_LOADERS['screen-C05'] = () => {
  const role = STATE.user?.vaiTro;
  const map = { NHAN_SU: ['moduleCard-dangky', 'moduleCard-thanhtoan'], QUAN_LY: ['moduleCard-phancong'], KE_TOAN: ['moduleCard-thanhtoan'] };
  ['moduleCard-dangky', 'moduleCard-phancong', 'moduleCard-thanhtoan'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('hidden', !(map[role] || []).includes(id));
  });
};

function openModuleThanhToan() {
  navigateTo(STATE.user?.vaiTro === 'NHAN_SU' ? 'screen-TT08' : 'screen-TT01');
}
