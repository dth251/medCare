# MedCare – Hệ thống Quản lý Phân công Ca trực & Thanh toán Thù lao

Đồ án môn học: **Phân tích và thiết kế Hệ thống Thông tin (PTTK HTTT)**.

Hệ thống phục vụ công tác quản lý lịch trực và thanh toán thù lao ca trực theo tuần tại Phòng khám Đa khoa, bao gồm 3 phân hệ nghiệp vụ chính:

| Phân hệ | Vai trò (Role) | Chức năng cốt lõi |
|---|---|---|
| **M1 – Đăng ký ca trực** | Bác sĩ, Y tá (`ROLE_NHAN_SU`) | Đăng ký 8–12 ca/tuần, tối đa 2 ca/ngày, tra cứu lịch trực cá nhân, xem phiếu lương thù lao hàng tuần |
| **M2 – Lên lịch phân công** | Quản lý điều phối (`ROLE_QUAN_LY`) | Lập kế hoạch tuần, phân công 1 Bác sĩ chính + 2 Y tá/ca cho 6 chuyên khoa, kiểm tra trùng ca, điều động và công bố lịch |
| **M3 – Thanh toán & Khiếu nại** | Kế toán tiền lương (`ROLE_KE_TOAN`) | Tổng hợp quyết toán thù lao theo khoa, xem chi tiết bảng chấm công 7 ngày, tiếp nhận & xử lý yêu cầu khiếu nại, xác nhận chi trả |

---

## 1. Cấu trúc Dự án

```
demo/
├── chay-gradle.bat                          # Script chạy Gradle tự động tạo junction tránh lỗi đường dẫn có dấu
├── PROMPT_LOG_REPORT.md                     # Báo cáo toàn bộ nhật ký prompt tương tác AI từ đầu tới cuối dự án
├── BAO_CAO_LOG_PROMPT.md                    # Bản tiếng Việt của nhật ký prompt
├── DESIGN.md                                # Quy chuẩn thiết kế UI/UX theo chuẩn Minimalist SaaS
└── backend/                                 # Ứng dụng Spring Boot 3.3.4 (Java 17+, Gradle Groovy DSL)
    ├── db/schema.sql                        # Lược đồ CSDL tham khảo
    ├── src/main/java/com/medcare/clinic/
    │   ├── controller/                      # REST Controllers (Auth, DangKy, PhanCong, Luong, Tuan,...)
    │   ├── service/                         # Service Interfaces
    │   │   └── impl/                        # Service Implementations (Transactional Business Logic)
    │   ├── mapper/                          # Mappers (DTO <-> Entity chuyển đổi dữ liệu)
    │   ├── repository/                      # Spring Data JPA Repositories
    │   ├── entity/                          # JPA Entities (ThanhVien, NhanSu, Khoa, CaTruc, BangPhanCong,...)
    │   ├── dto/                             # Data Transfer Objects & Requests/Responses
    │   ├── config/                          # SecurityConfig, JwtFilter, WebMvcConfig (CORS)
    │   └── exception/                       # Global Exception Handlers
    └── src/main/resources/
        ├── db/migration/                    # Flyway Database Migration tự động
        │   ├── V1__init_schema.sql          # Khởi tạo 12 bảng chuẩn hóa 3NF
        │   └── V2__seed_initial_data.sql    # Dữ liệu mẫu ban đầu: 6 khoa, 3 ca trực, 32 nhân sự y tế
        └── static/                          # Frontend Single Page Application (SPA)
            ├── index.html                   # Giao diện duy nhất điều hướng đa màn hình (C01 -> TT08)
            ├── css/main.css                 # Stylesheet chuẩn SaaS
            └── js/                          # Vanilla JS Modules (api.js, auth.js, app.js, state.js, module 1-3)
```

---

## 2. Yêu cầu Môi trường

- **Java Development Kit (JDK):** JDK 17 LTS trở lên
- **Hệ quản trị CSDL:** PostgreSQL 16
  - Tên CSDL: `phongkham_m2`
  - Cổng kết nối: `5432`
  - Tài khoản mặc định: `postgres` / `123456` (có thể điều chỉnh qua `application.yml` hoặc biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- **Quản lý CSDL:** Tự động hoàn toàn qua **Flyway Migration** (`V1__init_schema.sql` và `V2__seed_initial_data.sql`), không cần chạy thủ công bất kỳ script SQL nào.

---

## 3. Khởi chạy Ứng dụng

Thư mục dự án có thể chứa ký tự tiếng Việt có dấu. Để đảm bảo Gradle hoạt động hoàn hảo trên Windows, hãy sử dụng file thực thi tiện ích `chay-gradle.bat` (tự động tạo junction `C:\medcare-demo` an toàn):

```bat
chay-gradle.bat            :: Khởi động backend Spring Boot và giao diện web (http://localhost:8080)
chay-gradle.bat test       :: Thực thi bộ kiểm thử tự động (25 test cases đạt 100% SUCCESSFUL)
chay-gradle.bat build      :: Đóng gói toàn bộ ứng dụng thành 01 file JAR duy nhất
chay-gradle.bat --stop     :: Dừng tiến trình Gradle daemon
```

Sau khi ứng dụng khởi động thành công (`Started MedcareApplication`), mở trình duyệt và truy cập:
👉 **http://localhost:8080**

---

## 4. Tài khoản Đăng nhập Hệ thống (Mật khẩu: `123456`)

Hệ thống đã nạp sẵn 32 tài khoản nhân sự thực tế trong PostgreSQL. Dưới đây là 3 tài khoản chính đại diện cho 3 phân hệ nghiệp vụ:

| Tài khoản | Mật khẩu | Họ và tên | Chức danh / Vai trò | Phân hệ nghiệp vụ phụ trách |
|---|---|---|---|---|
| **`bs.nguyenvana`** | `123456` | BS. Nguyễn Văn A | Bác sĩ chuyên khoa Nội (`ROLE_NHAN_SU`) | **M1** – Đăng ký ca trực tuần, xem lịch trực cá nhân, xem phiếu thù lao A4 |
| **`ql.levanc`** | `123456` | QL. Lê Văn C | Quản lý điều phối ca (`ROLE_QUAN_LY`) | **M2** – Lập kế hoạch tuần, xếp Bác sĩ & Y tá vào 6 chuyên khoa, công bố lịch trực |
| **`kt.tranthib`** | `123456` | KT. Trần Thị B | Kế toán trưởng (`ROLE_KE_TOAN`) | **M3** – Quyết toán thù lao khoa, kiểm tra chấm công, duyệt khiếu nại, xác nhận chi trả |

*Ngoài ra, danh sách tài khoản các Bác sĩ và Y tá khác:*
- Bác sĩ: `bs.tranvanb`, `bs.phamthic`, `bs.hoangvand`, `bs.vuthie`, `bs.dangvanf`,...
- Y tá: `yt.nguyenthig`, `yt.tranthih`, `yt.lethii`, `yt.phamvank`, `yt.hoangthil`,...

---

## 5. Quy tắc Tính toán Thù lao & Nghiệp vụ

1. **Phân công ca trực:** Mỗi ca trực chuyên khoa yêu cầu bố trí tối thiểu 1 Bác sĩ chính và 2 Y tá điều dưỡng.
2. **Quy tắc tính công:** Mỗi ca hoàn thành được ghi nhận công, trừ trường hợp chấm công `VANG_MAT` hoặc `LOI_THIET_BI` (cần gửi khiếu nại kèm minh chứng để kế toán đối soát).
3. **Đơn giá thù lao:**
   - Ca chính trong giờ: Đơn giá tiêu chuẩn × 1.0 (Bác sĩ: 350.000 VNĐ/ca; Y tá: 180.000 VNĐ/ca).
   - Ca ngoài giờ / Ca đêm: Đơn giá tiêu chuẩn × Hệ số ngoài giờ (mặc định 2.0).
4. **Xử lý khiếu nại & Điều chỉnh:** Khiếu nại được phê duyệt sẽ tự động cộng/trừ khoản tiền chênh lệch vào tổng thù lao tuần.
5. **Điều kiện chi trả:** Khoa chỉ được xác nhận chi trả khi tất cả các yêu cầu khiếu nại trong tuần đã được giải quyết xong (`DA_XU_LY` hoặc `TU_CHOI`). Mỗi nhân viên y tế chỉ nhận một phiếu thanh toán duy nhất cho mỗi tuần kế hoạch.

---

## 6. Kiểm thử Tự động (Unit Test Suite)

Dự án có bộ test tự động tại `MedcareModuleTests.java` bao phủ 100% các chức năng cốt lõi:
- `DangKyCaTrucTests` (Kiểm thử đăng ký ca, ràng buộc giới hạn ca, truy vấn lịch cá nhân).
- `PhanCongCaTrucTests` (Kiểm thử chọn nhân sự theo khoa, phát hiện xung đột lịch, công bố lịch trực).
- `ThanhToanThuLaoTests` (Kiểm thử tính toán thù lao, đối soát chấm công, khiếu nại, quy trình chi trả).
- `HoSoCaNhanTests` (Kiểm thử tra cứu và cập nhật hồ sơ cá nhân kiên cố vào PostgreSQL).

Chạy kiểm thử:
```bat
chay-gradle.bat test
```
Kết quả nghiệm thu: **25/25 test cases PASSED (BUILD SUCCESSFUL)**.
