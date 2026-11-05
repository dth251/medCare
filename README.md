# MedCare – Hệ thống quản lý ca trực & thanh toán thù lao

Đồ án môn Phân tích và thiết kế HTTT. Gồm 3 phân hệ:

| Phân hệ | Vai trò | Chức năng |
|---|---|---|
| M1 – Đăng ký ca | Bác sĩ, Y tá | Đăng ký 8–12 ca/tuần, tối đa 2 ca/ngày |
| M2 – Lên lịch | Quản lý | Phân công 1 BS chính + 2 Y tá cho mỗi ca, điều động, công bố lịch |
| M3 – Thanh toán | Kế toán (và Bác sĩ/Y tá xem của mình) | Chấm công, khiếu nại, biên bản điều chỉnh, chi trả, phiếu lương |

```
demo/
├── chay-gradle.bat          # Chạy Gradle (xem mục "Chạy dự án")
├── backend/                 # Spring Boot 3.2, Java 17+, PostgreSQL
│   ├── db/schema.sql        # Lược đồ CSDL sinh từ entity
│   ├── db/don-dep-csdl-cu.sql
│   ├── db/thiet-ke-ban-dau/ # Lược đồ thiết kế ban đầu (tham khảo)
│   └── src/main/resources/static/   # Giao diện (được Spring Boot phục vụ)
└── frontend/                # Bản sao giao diện để mở bằng Live Server (cổng 5500)
```

## Yêu cầu

- JDK 17 trở lên
- PostgreSQL, CSDL `phongkham_m2` (mặc định `postgres` / `123456`, đổi bằng biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)

## Chạy dự án

Thư mục dự án có dấu tiếng Việt (`Kì 1 Năm 4\Phân tích và thiết kế HTTT`). Windows
đang dùng code page ANSI 1252, không biểu diễn được các chữ như "ă", "ế", nên Java/Gradle
làm hỏng đường dẫn khi truyền cho tiến trình con (lỗi `GradleWrapperMain`,
`ClassNotFoundException` khi chạy test). Vì vậy **hãy chạy Gradle qua `chay-gradle.bat`**:
script tự tạo junction `C:\medcare-demo` trỏ tới thư mục dự án (không copy, không di chuyển
file) rồi chạy `gradlew.bat` từ đường dẫn không dấu đó.

```bat
chay-gradle.bat            :: khởi động backend, sau đó mở http://localhost:8080
chay-gradle.bat test       :: chạy unit test
chay-gradle.bat build      :: build file jar
chay-gradle.bat --stop     :: dừng Gradle daemon
```

Có thể bấm đúp vào `chay-gradle.bat` trong Explorer để khởi động backend.

**Với VS Code / IntelliJ**, hãy mở thư mục `C:\medcare-demo` (thay vì đường dẫn có dấu) để
tiện ích Java/Gradle nạp được project.

Cách khác (sửa tận gốc cho toàn máy): bật *Settings → Time & language → Language & region →
Administrative language settings → Change system locale → "Beta: Use Unicode UTF-8 for
worldwide language support"* rồi khởi động lại máy. Khi đó có thể chạy `gradlew.bat` trực tiếp.

### Tài khoản mẫu (mật khẩu `123456`)

| Tài khoản | Vai trò |
|---|---|
| `bs.nguyenvana`, `bs.tranvanb`, `yt.tranthib`, ... | Bác sĩ / Y tá |
| `ql.levanc` | Quản lý |
| `kt.tranthib` | Kế toán |

## Cơ sở dữ liệu

- Khi khởi động, Hibernate (`ddl-auto: update`) tự bổ sung bảng/cột còn thiếu.
- `backend/db/schema.sql` dùng để tạo CSDL mới rỗng:
  `createdb -U postgres phongkham_m2` rồi `psql -U postgres -d phongkham_m2 -f C:\medcare-demo\backend\db\schema.sql`
  (dùng đường dẫn qua `C:\medcare-demo` vì `psql` cũng gặp lỗi đường dẫn có dấu).
- `backend/db/don-dep-csdl-cu.sql`: dọn bảng/cột của thiết kế cũ trên CSDL đã có dữ liệu
  (chuyển bảng cũ sang schema `legacy`, không xóa dữ liệu). **Đã chạy trên `phongkham_m2`.**

### Sinh lại schema.sql

Sau khi sửa entity, sinh lại lược đồ bằng Hibernate (chạy một instance tạm ở cổng 8099,
không đụng tới dữ liệu):

```powershell
$env:SPRING_APPLICATION_JSON = '{"server":{"port":8099},"spring":{"jpa":{"hibernate":{"ddl-auto":"none"},"properties":{"jakarta.persistence.schema-generation.scripts.action":"create","jakarta.persistence.schema-generation.scripts.create-target":"C:/medcare-demo/backend/build/schema-generated.sql","hibernate.hbm2ddl.delimiter":";","hibernate.format_sql":"true"}}}}'
.\chay-gradle.bat bootRun     # dừng (Ctrl+C) sau khi thấy "Started MedcareApplication"
Remove-Item Env:SPRING_APPLICATION_JSON
```

Sau đó chép nội dung `backend/build/schema-generated.sql` vào `backend/db/schema.sql`
(giữ phần chú thích đầu file).

## Quy tắc tính thù lao

- Mỗi ca được phân công trong tuần được tính công, trừ khi chấm công là `VANG_MAT` hoặc
  `LOI_THIET_BI` (cần gửi khiếu nại để được bổ sung). Ca chưa có dữ liệu máy chấm công
  được tính theo lịch phân công.
- Ca chính = đơn giá × 1.0; ca ngoài giờ = đơn giá × hệ số của ca (mặc định 2.0).
- Cộng thêm số tiền của các biên bản điều chỉnh (khiếu nại đã xử lý) trong tuần.
- Không chi trả khi còn khiếu nại chờ xử lý; mỗi nhân sự chỉ được chi trả một lần mỗi tuần.
