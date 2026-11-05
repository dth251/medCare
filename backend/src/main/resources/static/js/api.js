/**
 * MEDCARE CLINIC - CLIENT REST API
 * Giao tiếp với Backend Spring Boot qua Fetch API (JWT Bearer).
 * Mọi lỗi từ máy chủ được ném ra dưới dạng ApiError để giao diện hiển thị đúng thông báo.
 */

const API_CONFIG = {
  // Khi trang được phục vụ bởi chính Spring Boot (cổng 8080) dùng cùng origin,
  // ngược lại (mở file tĩnh bằng Live Server...) gọi về localhost:8080.
  baseUrl: (window.location.port === '8080')
    ? `${window.location.origin}/api`
    : 'http://localhost:8080/api',
  tokenKey: 'medcare_jwt_token'
};

class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

const MedcareAPI = {
  // ================================================================
  // Phiên đăng nhập
  // ================================================================
  getToken() {
    try { return localStorage.getItem(API_CONFIG.tokenKey); } catch (e) { return null; }
  },

  setToken(token) {
    try { localStorage.setItem(API_CONFIG.tokenKey, token); } catch (e) { /* trình duyệt chặn storage */ }
  },

  clearSession() {
    try { localStorage.removeItem(API_CONFIG.tokenKey); } catch (e) { /* ignore */ }
  },

  async request(endpoint, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = this.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    let response;
    try {
      response = await fetch(`${API_CONFIG.baseUrl}${endpoint}`, { ...options, headers });
    } catch (networkErr) {
      throw new ApiError('Không kết nối được máy chủ Backend (http://localhost:8080). Hãy kiểm tra Spring Boot đã chạy chưa.', 0);
    }

    const data = await response.json().catch(() => null);

    if (!response.ok || (data && data.success === false)) {
      let message = data?.message || `Lỗi máy chủ (${response.status})`;
      // Gộp chi tiết lỗi validation (nếu có)
      if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) {
        const details = Object.values(data.data).filter(v => typeof v === 'string');
        if (details.length) message += ': ' + details.join('; ');
      }
      const err = new ApiError(message, response.status, data);
      if (response.status === 401 && endpoint !== '/auth/login') {
        err.sessionExpired = true;
        this.clearSession();
        window.dispatchEvent(new CustomEvent('medcare:unauthorized'));
      }
      throw err;
    }
    return data;
  },

  get(endpoint) { return this.request(endpoint, { method: 'GET' }); },
  post(endpoint, body) { return this.request(endpoint, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }); },
  put(endpoint, body) { return this.request(endpoint, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }); },
  del(endpoint) { return this.request(endpoint, { method: 'DELETE' }); },

  /** Máy chủ đang chạy nếu trả về bất kỳ HTTP status nào (kể cả 401). */
  async ping() {
    try {
      const res = await fetch(`${API_CONFIG.baseUrl}/tuan`, { method: 'GET' });
      return res.status > 0;
    } catch (e) {
      return false;
    }
  },

  // ================================================================
  // Xác thực
  // ================================================================
  async login(username, password) {
    const res = await this.post('/auth/login', { username, password });
    this.setToken(res.data.token);
    return res.data;
  },

  async me() { return (await this.get('/auth/me')).data; },
  async updateProfile(profileData) { return (await this.put('/auth/profile', profileData)).data; },

  async getWeeks() { return (await this.get('/tuan')).data || []; },

  // ================================================================
  // MODULE 1: ĐĂNG KÝ CA (Bác sĩ / Y tá)
  // ================================================================
  async getMyShiftOptions(tuanId) { return (await this.get(`/dang-ky-ca/ca-truc/${tuanId}`)).data || []; },
  async getMyRegistration(tuanId) { return (await this.get(`/dang-ky-ca/lich-tuan/${tuanId}`)).data; },
  saveRegistration(tuanId, caTrucKhoaIds) {
    return this.post('/dang-ky-ca/luu', { tuanLamViecId: tuanId, caTrucKhoaIds });
  },

  // ================================================================
  // MODULE 2: LÊN LỊCH & PHÂN CÔNG (Quản lý)
  // ================================================================
  async getScheduleOverview(tuanId) { return (await this.get(`/phan-cong/tuan/${tuanId}`)).data || []; },
  async getDepartmentSchedule(khoaId, tuanId) { return (await this.get(`/phan-cong/khoa/${khoaId}/tuan/${tuanId}`)).data; },
  async getCandidates(caTrucKhoaId, vaiTro) {
    return (await this.get(`/phan-cong/ung-vien?caTrucKhoaId=${encodeURIComponent(caTrucKhoaId)}&vaiTro=${encodeURIComponent(vaiTro)}`)).data || [];
  },
  assignStaff(payload) { return this.post('/phan-cong/ca', payload); },
  unassign(phanCongId) { return this.del(`/phan-cong/ca/${phanCongId}`); },
  publishWeek(tuanId) { return this.post(`/phan-cong/cong-bo/${tuanId}`); },

  // ================================================================
  // MODULE 3: CHẤM CÔNG, KHIẾU NẠI & THANH TOÁN
  // ================================================================
  async getPaymentDepartments(tuanId) { return (await this.get(`/thanh-toan/tuan/${tuanId}/khoa`)).data || []; },
  async getPaymentStaff(tuanId, khoaId) { return (await this.get(`/thanh-toan/tuan/${tuanId}/khoa/${khoaId}/nhan-su`)).data || []; },
  async getTimesheet(nhanSuId, tuanId) { return (await this.get(`/thanh-toan/cham-cong/${nhanSuId}/tuan/${tuanId}`)).data || []; },
  async getSlip(phieuId) { return (await this.get(`/thanh-toan/phieu/${phieuId}`)).data; },
  async getMySlips() { return (await this.get('/thanh-toan/phieu-cua-toi/tat-ca')).data || []; },
  async getWeeklySlips(tuanId) { return (await this.get(`/thanh-toan/tuan/${tuanId}`)).data || []; },
  executePayment(payload) { return this.post('/thanh-toan/chi-tra', payload); },

  submitComplaint(payload) { return this.post('/khieu-nai/gui', payload); },
  async getMyComplaints() { return (await this.get('/khieu-nai/cua-toi')).data || []; },
  async getComplaints(trangThai) {
    return (await this.get('/khieu-nai' + (trangThai ? `?trangThai=${encodeURIComponent(trangThai)}` : ''))).data || [];
  },
  async getComplaint(id) { return (await this.get(`/khieu-nai/${id}`)).data; },
  saveAdjustment(payload) { return this.post('/khieu-nai/dieu-chinh', payload); },
  rejectComplaint(id, lyDo) { return this.post(`/khieu-nai/${id}/tu-choi`, { lyDo }); }
};
