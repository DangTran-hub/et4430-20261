# Hệ thống đăng ký học phần (ET4430)

Spring Boot + Thymeleaf + PostgreSQL. Đã làm: **UC1 – Thông báo kế hoạch đăng ký học phần**
và **sinh viên đăng ký học phần** có kiểm tra giới hạn tín chỉ theo mức cảnh báo học tập.

## Chạy

Cần JDK 21 và một database PostgreSQL **trống** (Flyway tự tạo bảng).

```bash
createdb -U postgres quanlyhocphan_web      # hoặc tạo trong pgAdmin

export DB_URL=jdbc:postgresql://localhost:5432/quanlyhocphan_web
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./mvnw spring-boot:run                       # Windows: .\mvnw.cmd spring-boot:run
```

Mở http://localhost:8080. Lần chạy đầu sẽ tự tạo dữ liệu demo.

## Tài khoản demo

| Tài khoản | Mật khẩu | Vai trò | Mức cảnh báo | Giới hạn |
|---|---|---|---|---|
| pdt | pdt123 | Phòng Đào tạo | | |
| sv01 | sv123 | Sinh viên | Bình thường | 12–24 TC |
| sv02 | sv123 | Sinh viên | Cảnh báo mức 1 | tối đa 14 TC |
| sv03 | sv123 | Sinh viên | Cảnh báo mức 2 | tối đa 10 TC |
| sv04 | sv123 | Sinh viên | Cảnh báo mức 3 | không được đăng ký |

## Kịch bản demo UC1

1. Đăng nhập `pdt`. Trang chủ liệt kê các học kỳ.
2. **2026-2027 Học kỳ 2 → "Mở đợt đăng ký"**: hệ thống nhận đây là kỳ chẵn, kế thừa học phần
   và thời gian từ đợt 2025-2026 Học kỳ 2, đồng thời bổ sung học phần mới theo quy tắc mở.
3. Sửa thời gian đăng ký thành **từ bây giờ đến vài ngày tới**, bỏ hoặc thêm học phần nếu muốn (bước 4, 5a).
4. **"Xác nhận mở đợt và công bố thông báo"** (bước 5).
5. Các nhánh phụ:
   - **2027-2028 Học kỳ 1** → 3a: chưa có đợt kỳ lẻ nào trước đó, hệ thống chọn học phần theo quy tắc mở.
   - **2026-2027 Học kỳ hè** → 3b: không có học phần mở kỳ hè.
   - Bấm **"Mở lại"** ở học kỳ đã có đợt → 4a: đợt đã tồn tại.
6. Đăng xuất, đăng nhập `sv01`…`sv04` (bước 6–8): thấy thông báo, thời gian đăng ký, trạng thái
   cảnh báo của chính mình. Vào "Xem học phần" để đăng ký:
   - `sv02` bị chặn khi vượt 14 TC, `sv03` bị chặn khi vượt 10 TC, `sv04` không đăng ký được.

## Cấu trúc

- `db/migration/V1` – 8 bảng theo từ điển dữ liệu trong báo cáo; `V2` – mức cảnh báo,
  chống trùng đợt, bảng `course_registration` (xem `docs/data/database-setup.md`).
- `service/CampaignService` – UC1 (chẵn/lẻ, kế thừa, 3a/3b/4a, công bố).
- `service/StudentRegistrationService` + `WarningPolicy` – đăng ký và giới hạn tín chỉ.
- `web/` – controller; `templates/` – giao diện Thymeleaf.
- Xuất ERD: pgAdmin → chuột phải database → **ERD For Database**.
