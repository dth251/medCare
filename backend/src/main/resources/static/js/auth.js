/**
 * MEDCARE CLINIC - ĐĂNG NHẬP / ĐĂNG XUẤT & HIỂN THỊ THÔNG TIN NGƯỜI DÙNG
 * Vai trò lấy từ backend (/api/auth/me), không còn nút "đóng vai" phía client.
 */

const ROLE_LABEL = { NHAN_SU: 'Nhân sự y tế', QUAN_LY: 'Quản lý', KE_TOAN: 'Kế toán' };

/**
 * Thanh điều hướng chính theo vai trò.
 * `match`: các màn hình con được tính là thuộc mục này (để tô sáng tab đang mở).
 */
const ROLE_MENU_ITEMS = {
  NHAN_SU: [
    { id: 'screen-C02', label: 'Tổng quan', action: () => navigateTo('screen-C02') },
    { id: 'screen-DK01', label: 'Đăng ký ca', action: () => navigateTo('screen-DK01') },
    { id: 'screen-TT03', label: 'Chấm công', action: () => ttOpenMyTimesheet() },
    { id: 'screen-TT08', label: 'Phiếu lương', match: ['screen-TT07'], action: () => navigateTo('screen-TT08') },
    { id: 'screen-C05', label: 'Phân hệ', action: () => navigateTo('screen-C05') }
  ],
  QUAN_LY: [
    { id: 'screen-C03', label: 'Tổng quan', action: () => navigateTo('screen-C03') },
    { id: 'screen-PC01', label: 'Xếp lịch', match: ['screen-PC02'], action: () => navigateTo('screen-PC01') },
    { id: 'screen-C05', label: 'Phân hệ', action: () => navigateTo('screen-C05') }
  ],
  KE_TOAN: [
    { id: 'screen-C04', label: 'Tổng quan', action: () => navigateTo('screen-C04') },
    { id: 'screen-TT01', label: 'Quyết toán', match: ['screen-TT02', 'screen-TT03', 'screen-TT06'], action: () => navigateTo('screen-TT01') },
    { id: 'screen-TT05', label: 'Khiếu nại', action: () => navigateTo('screen-TT05') },
    { id: 'screen-TT09', label: 'Phiếu lương', action: () => navigateTo('screen-TT09') },
    { id: 'screen-C05', label: 'Phân hệ', action: () => navigateTo('screen-C05') }
  ]
};

function renderMainNav() {
  const nav = document.getElementById('mainNav');
  if (!nav) return;
  const items = STATE.user ? (ROLE_MENU_ITEMS[STATE.user.vaiTro] || []) : [];
  // Phiếu lương chi tiết (TT07) thuộc về mục đã mở nó (Bảng lương / Phiếu lương)
  const current = STATE.currentScreen === 'screen-TT07' && STATE.payment.slipBackScreen
    ? STATE.payment.slipBackScreen
    : STATE.currentScreen;
  nav.innerHTML = items.map((it, i) => {
    const active = it.id === current || (it.match || []).includes(current);
    return `<button type="button" class="nav-link${active ? ' active' : ''}" onclick="runNavItem(${i})">${escapeHtml(it.label)}</button>`;
  }).join('');
}

function runNavItem(index) {
  const it = (ROLE_MENU_ITEMS[STATE.user?.vaiTro] || [])[index];
  if (it) it.action();
}

async function navOpenScheduleDetail() {
  const sc = STATE.scheduling;
  sc.week = sc.week || pickSchedulingWeek();
  if (!sc.deptId && sc.week) {
    if (!sc.overview || !sc.overview.length) {
      sc.overview = await MedcareAPI.getScheduleOverview(sc.week.id).catch(() => []);
    }
    const firstActive = sc.overview.find(k => k.tongSoCa > 0) || sc.overview[0];
    if (firstActive) sc.deptId = firstActive.khoaId;
  }
  navigateTo(sc.deptId ? 'screen-PC02' : 'screen-PC01');
}

async function navOpenStaffPayroll() {
  const pay = STATE.payment;
  pay.week = pay.week || pickPaymentWeek();
  if (!pay.deptId && pay.week) {
    if (!pay.depts || !pay.depts.length) {
      pay.depts = await MedcareAPI.getPaymentDepartments(pay.week.id).catch(() => []);
    }
    const firstActive = pay.depts.find(d => d.soNhanSu > 0) || pay.depts[0];
    if (firstActive) pay.deptId = firstActive.khoaId;
  }
  navigateTo(pay.deptId ? 'screen-TT02' : 'screen-TT01');
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

function togglePasswordVisibility() {
  const input = document.getElementById('loginPassword');
  const icon = document.getElementById('loginPasswordEye');
  if (!input) return;
  const isPass = input.type === 'password';
  input.type = isPass ? 'text' : 'password';
  if (icon) {
    icon.setAttribute('data-icon', isPass ? 'eye-off' : 'eye');
  }
}

function logout() {
  MedcareAPI.clearSession();
  STATE.user = null;
  STATE.weeks = [];
  if (typeof NAV_STACK !== 'undefined') {
    NAV_STACK.back = [];
    NAV_STACK.forward = [];
  }
  const pInput = document.getElementById('loginPassword');
  if (pInput) pInput.value = '';
  applyUserToChrome();
  navigateTo('screen-C01', { replaceHistory: true });
  showToast('Đã đăng xuất');
}

function displayName(user) {
  if (!user) return '';
  const prefix = user.chucDanh === 'Bác sĩ' ? 'BS. ' : user.chucDanh === 'Y tá' ? 'YT. '
    : user.vaiTro === 'QUAN_LY' ? 'QL. ' : user.vaiTro === 'KE_TOAN' ? 'KT. ' : '';
  return prefix + user.fullName;
}

/** Cập nhật header (nút người dùng, đăng xuất, thanh điều hướng) theo người dùng hiện tại. */
function renderUserBadge() { applyUserToChrome(); }
window.renderUserBadge = renderUserBadge;
function applyUserToChrome() {
  const user = STATE.user;
  const chip = document.getElementById('roleBadge');
  const logoutBtn = document.getElementById('logoutBtn');
  renderMainNav();

  if (!user) {
    if (chip) chip.classList.add('hidden');
    if (logoutBtn) logoutBtn.classList.add('hidden');
    return;
  }
  if (chip) {
    chip.innerHTML = `<span class="avatar">${escapeHtml(initials(user.fullName))}</span>
      <span class="user-chip-text">
        <span class="user-chip-name block">${escapeHtml(user.fullName)}</span>
        <span class="user-chip-role block">${escapeHtml(user.chucDanh || ROLE_LABEL[user.vaiTro] || '')}</span>
      </span>`;
    chip.classList.remove('hidden');
  }
  if (logoutBtn) logoutBtn.classList.remove('hidden');
}

/** Hiển thị chi tiết hồ sơ người dùng trong modal. */
function openUserProfileModal() {
  const u = STATE.user;
  if (!u) return;

  // Luôn bắt đầu ở chế độ xem chi tiết
  toggleUserProfileEdit(false);

  setText('uModalName', u.fullName);
  setText('uModalMa', u.maNhanSu || '—');
  setText('uModalUsername', u.username || '—');
  setText('uModalRole', u.chucDanh || ROLE_LABEL[u.vaiTro] || '');

  const avatar = document.getElementById('uModalAvatar');
  if (avatar) avatar.textContent = initials(u.fullName);

  setText('uModalChucDanh', u.chucDanh || '—');
  setText('uModalHocVi', u.hocVi || '—');
  setText('uModalKhoa', u.tenKhoa || (u.vaiTro === 'QUAN_LY' ? 'Phòng Tổ chức cán bộ' : u.vaiTro === 'KE_TOAN' ? 'Phòng Tài chính kế toán' : '—'));
  setText('uModalDept', u.tenKhoa || '');

  setText('uModalPhone', u.soDienThoai || '—');
  setText('uModalEmail', u.email || '—');
  setText('uModalDob', u.ngaySinh ? fmtDate(u.ngaySinh) : '—');
  setText('uModalAddress', u.diaChi || '—');

  setText('uModalRate', u.donGiaCa ? `${fmtMoney(u.donGiaCa)} / ca` : '—');
  setText('uModalBankAcc', u.soTaiKhoan || '—');
  setText('uModalBankName', u.tenNganHang || '—');
  setText('uModalBankOwner', u.chuTaiKhoan || '—');

  openModal('modal-user-profile');
}

/** Chuyển đổi giữa chế độ Xem chi tiết và chế độ Chỉnh sửa thông tin cá nhân. */
function toggleUserProfileEdit(isEdit) {
  const u = STATE.user;
  const viewMode = document.getElementById('uModalViewMode');
  const editMode = document.getElementById('uModalEditMode');
  const footerView = document.getElementById('uModalFooterView');
  const footerEdit = document.getElementById('uModalFooterEdit');

  if (isEdit && u) {
    // Đổ dữ liệu hiện tại vào form chỉnh sửa
    const elFullName = document.getElementById('uEditFullName');
    const elPhone = document.getElementById('uEditPhone');
    const elEmail = document.getElementById('uEditEmail');
    const elDob = document.getElementById('uEditDob');
    const elAddress = document.getElementById('uEditAddress');
    const elBankAcc = document.getElementById('uEditBankAcc');
    const elBankName = document.getElementById('uEditBankName');
    const elBankOwner = document.getElementById('uEditBankOwner');

    if (elFullName) elFullName.value = u.fullName || '';
    if (elPhone) elPhone.value = u.soDienThoai || '';
    if (elEmail) elEmail.value = u.email || '';
    if (elDob) elDob.value = u.ngaySinh ? u.ngaySinh.substring(0, 10) : '';
    if (elAddress) elAddress.value = u.diaChi || '';
    if (elBankAcc) elBankAcc.value = u.soTaiKhoan || '';
    if (elBankName) elBankName.value = u.tenNganHang || '';
    if (elBankOwner) elBankOwner.value = u.chuTaiKhoan || u.fullName || '';

    if (viewMode) viewMode.classList.add('hidden');
    if (editMode) editMode.classList.remove('hidden');
    if (footerView) footerView.classList.add('hidden');
    if (footerEdit) {
      footerEdit.classList.remove('hidden');
      footerEdit.classList.add('flex');
    }

    if (elFullName) elFullName.focus();
  } else {
    if (viewMode) viewMode.classList.remove('hidden');
    if (editMode) editMode.classList.add('hidden');
    if (footerView) {
      footerView.classList.remove('hidden');
      footerView.classList.add('flex');
    }
    if (footerEdit) {
      footerEdit.classList.add('hidden');
      footerEdit.classList.remove('flex');
    }
  }
}

/** Gửi yêu cầu cập nhật thông tin cá nhân lên Backend và lưu vào Database. */
async function saveUserProfile() {
  const elFullName = document.getElementById('uEditFullName');
  const elPhone = document.getElementById('uEditPhone');
  const elEmail = document.getElementById('uEditEmail');
  const elDob = document.getElementById('uEditDob');
  const elAddress = document.getElementById('uEditAddress');
  const elBankAcc = document.getElementById('uEditBankAcc');
  const elBankName = document.getElementById('uEditBankName');
  const elBankOwner = document.getElementById('uEditBankOwner');
  const btnSave = document.getElementById('uModalBtnSave');

  const fullName = elFullName ? elFullName.value.trim() : '';
  if (!fullName) {
    showToast('Vui lòng nhập họ và tên', false);
    if (elFullName) elFullName.focus();
    return;
  }

  const payload = {
    fullName: fullName,
    soDienThoai: elPhone ? elPhone.value.trim() : '',
    email: elEmail ? elEmail.value.trim() : '',
    ngaySinh: elDob && elDob.value ? elDob.value : null,
    diaChi: elAddress ? elAddress.value.trim() : '',
    soTaiKhoan: elBankAcc ? elBankAcc.value.trim() : '',
    tenNganHang: elBankName ? elBankName.value.trim() : '',
    chuTaiKhoan: elBankOwner ? elBankOwner.value.trim() : ''
  };

  const oldHtml = btnSave ? btnSave.innerHTML : '';
  if (btnSave) {
    btnSave.disabled = true;
    btnSave.innerHTML = `${ic('loader', 'spin')} Đang lưu`;
  }

  try {
    const updated = await MedcareAPI.updateProfile(payload);
    STATE.user = updated;

    // Lưu lại session/local storage
    try {
      localStorage.setItem('medcare_user', JSON.stringify(updated));
    } catch (e) {}

    // Cập nhật các vị trí hiển thị trên Header và Trang chủ
    applyUserToChrome();

    // Cập nhật Trang chủ Bác sĩ & Y tá (C02) nếu đang mở
    if (typeof displayName === 'function') {
      setText('c02Name', updated.fullName);
      setText('c03Name', updated.fullName);
      setText('c04Name', updated.fullName);
    }
    setText('c02Info', [updated.chucDanh, updated.tenKhoa, updated.maNhanSu].filter(Boolean).join(' · '));

    // Làm mới dữ liệu trong Modal và chuyển về View Mode
    openUserProfileModal();
    showToast('Đã lưu thông tin cá nhân');

  } catch (err) {
    console.error('Lỗi cập nhật hồ sơ:', err);
    showToast(err.message || 'Không thể lưu thông tin cá nhân. Vui lòng thử lại.', false);
  } finally {
    if (btnSave) {
      btnSave.disabled = false;
      btnSave.innerHTML = oldHtml;
    }
  }
}



// ==========================================
// TRANG CHỦ THEO VAI TRÒ & CHỌN PHÂN HỆ
// ==========================================
/** Danh mục phân hệ; phân hệ thuộc quyền vai trò hiện tại được bật sáng. */
const MODULES = [
  {
    title: 'Đăng ký lịch làm việc',
    desc: () => 'Chọn ca và lưu nguyện vọng cho tuần kế tiếp.',
    actions: {
      NHAN_SU: [['Vào đăng ký ca', "navigateTo('screen-DK01')"]]
    }
  },
  {
    title: 'Lên lịch làm việc',
    desc: () => 'Phân công bác sĩ, y tá theo khoa và công bố lịch tuần.',
    actions: {
      QUAN_LY: [['Vào xếp lịch', "navigateTo('screen-PC01')"]]
    }
  },
  {
    title: 'Thanh toán tiền công',
    desc: role => role === 'KE_TOAN'
      ? 'Quyết toán thù lao, xử lý khiếu nại và phát hành phiếu lương.'
      : 'Đối chiếu công, gửi khiếu nại và xem phiếu lương cá nhân.',
    actions: {
      NHAN_SU: [['Xem bảng chấm công', 'ttOpenMyTimesheet()'], ['Phiếu lương của tôi', "navigateTo('screen-TT08')"]],
      KE_TOAN: [['Quyết toán', "navigateTo('screen-TT01')"], ['Khiếu nại', "navigateTo('screen-TT05')"], ['Phiếu lương', "navigateTo('screen-TT09')"]]
    }
  }
];

SCREEN_LOADERS['screen-C05'] = () => {
  const role = STATE.user?.vaiTro;
  setText('c05Role', `Vai trò hiện tại: ${STATE.user?.chucDanh || ROLE_LABEL[role] || ''} · Chọn một chức năng bên dưới để tiếp tục.`);
  setHtml('moduleList', MODULES.map((m, i) => {
    const actions = m.actions[role];
    const allowed = !!actions;
    return `<div class="card module-card ${allowed ? 'is-active' : 'is-locked'}">
      <div class="min-w-0">
        <div class="module-title"><span class="module-index">${String(i + 1).padStart(2, '0')}</span>${escapeHtml(m.title)}</div>
        <div class="module-desc">${escapeHtml(m.desc(role))}</div>
        <div class="module-status">${allowed ? 'Được truy cập' : 'Không thuộc quyền của vai trò hiện tại'}</div>
      </div>
      <div class="module-actions">
        ${allowed
          ? actions.map(([label, fn], k) => `<button type="button" onclick="${fn}" class="btn ${k === 0 ? 'btn-primary' : 'btn-secondary'}">${escapeHtml(label)}</button>`).join('')
          : '<button type="button" class="btn btn-secondary" disabled>Không có quyền</button>'}
      </div>
    </div>`;
  }).join(''));
};

function openModuleThanhToan() {
  navigateTo(STATE.user?.vaiTro === 'NHAN_SU' ? 'screen-TT08' : 'screen-TT01');
}
