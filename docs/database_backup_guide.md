# Hướng Dẫn Backup / Restore Database Oracle

Tài liệu này hướng dẫn cách sao lưu và khôi phục database Oracle cho đồ án **Smart Supermarket - Store Portal**.

Mục tiêu là giúp thành viên trong nhóm có thể backup dữ liệu demo đầy đủ, restore lại khi database bị lỗi, hoặc chuyển dữ liệu sang máy khác.

---

## 1. Khi nào cần backup database?

Nên backup trước khi thực hiện các thao tác có thể làm thay đổi dữ liệu hoặc cấu trúc database:

- Trước khi chạy file SQL lớn.
- Trước khi chạy các file trong `database/patches`.
- Trước khi import CSV số lượng lớn.
- Trước khi sửa bảng, thêm cột, thêm khóa ngoại.
- Trước khi demo cho giảng viên.
- Trước khi pull code mới có thay đổi database.
- Trước khi xóa dữ liệu hoặc reset database.

---

## 2. Các file backup trong project

Thư mục backup nằm tại:

```txt
SieuThiThongMinh_Java/backup
```

Cấu trúc đề xuất:

```txt
backup/
├── export_dmp.bat
├── import_dmp.bat
├── restore_dmp.bat
├── SMART_SUPERMARKET_*.DMP
└── *.log
```

Ý nghĩa từng file:

| File | Chức năng |
|---|---|
| `export_dmp.bat` | Sao lưu database hiện tại ra file `.DMP` |
| `import_dmp.bat` | Import một file `.DMP` cụ thể vào database |
| `restore_dmp.bat` | Tự động lấy file `.DMP` mới nhất rồi restore |
| `*.DMP` | File backup dữ liệu Oracle |
| `*.log` | Log quá trình export/import |

---

## 3. Kiểm tra cấu hình trước khi backup

Mở các file `.bat` và kiểm tra các biến cấu hình sau:

```bat
set DB_HOST=10.0.216.238
set DB_PORT=1521
set DB_SERVICE=orcl

set DB_ADMIN_USER=system
set DB_ADMIN_PASSWORD=Admin123

set APP_SCHEMA=SYSTEM
```

Ý nghĩa:

| Biến | Ý nghĩa |
|---|---|
| `DB_HOST` | IP máy đang chạy Oracle |
| `DB_PORT` | Port Oracle, thường là `1521` |
| `DB_SERVICE` | Service name, ví dụ `orcl` hoặc `FREEPDB1` |
| `DB_ADMIN_USER` | Tài khoản đăng nhập Oracle |
| `DB_ADMIN_PASSWORD` | Mật khẩu Oracle |
| `APP_SCHEMA` | Schema chứa bảng của đồ án |

Ví dụ nếu Oracle chạy local:

```bat
set DB_HOST=localhost
set DB_PORT=1521
set DB_SERVICE=orcl
```

Ví dụ nếu Oracle chạy Docker:

```bat
set DB_HOST=localhost
set DB_PORT=1522
set DB_SERVICE=FREEPDB1
```

Ví dụ nếu Oracle chạy trên máy nhóm trong LAN:

```bat
set DB_HOST=10.0.216.238
set DB_PORT=1521
set DB_SERVICE=orcl
```

---

## 4. Yêu cầu trước khi chạy backup

Máy chạy file `.bat` cần có Oracle Client hoặc Oracle Database để dùng được các lệnh:

```bash
sqlplus
expdp
impdp
```

Kiểm tra bằng CMD:

```bash
sqlplus -v
expdp help=y
impdp help=y
```

Nếu CMD báo không nhận lệnh, cần thêm thư mục `bin` của Oracle vào biến môi trường `PATH`.

Ví dụ đường dẫn thường gặp:

```txt
C:\app\<username>\product\21c\dbhomeXE\bin
```

hoặc:

```txt
C:\oracle\product\21c\dbhome_1\bin
```

---

## 5. Cách backup database bằng `export_dmp.bat`

### Bước 1: Tắt app Java

Trước khi backup, nên tắt app Java để tránh dữ liệu đang được ghi dở.

### Bước 2: Mở thư mục backup

Vào thư mục:

```txt
E:\JAVA\DoAnFinal\SieuThiThongMinh_Java\backup
```

### Bước 3: Chạy file export

Double click:

```txt
export_dmp.bat
```

Hoặc mở CMD tại thư mục `backup`, rồi chạy:

```bash
export_dmp.bat
```

### Bước 4: Kiểm tra kết quả

Nếu thành công, thư mục `backup` sẽ xuất hiện file dạng:

```txt
SMART_SUPERMARKET_SYSTEM_20260518_150000.DMP
SMART_SUPERMARKET_SYSTEM_20260518_150000_EXPORT.log
```

Trong đó:

| File | Ý nghĩa |
|---|---|
| `.DMP` | File backup dữ liệu database |
| `.log` | File log quá trình export |

Nếu thấy dòng này là backup thành công:

```txt
[DONE] Backup completed successfully.
```

---

## 6. Cách restore database bằng `restore_dmp.bat`

Dùng file này khi muốn khôi phục database về bản backup mới nhất.

### Bước 1: Tắt app Java

Đóng app để tránh database đang bị truy cập.

### Bước 2: Chạy file restore

Double click:

```txt
restore_dmp.bat
```

Hoặc chạy bằng CMD:

```bash
restore_dmp.bat
```

### Bước 3: Xác nhận restore

Script sẽ tự tìm file `.DMP` mới nhất trong thư mục `backup`.

Khi thấy dòng:

```txt
Type YES to restore latest backup:
```

gõ:

```txt
YES
```

rồi nhấn Enter.

Nếu thành công sẽ thấy:

```txt
[DONE] Restore completed successfully.
```

---

## 7. Cách import một file backup cụ thể bằng `import_dmp.bat`

Dùng khi muốn chọn đúng một file `.DMP` cụ thể.

### Bước 1: Chạy file import

```bash
import_dmp.bat
```

### Bước 2: Nhập tên file `.DMP`

Ví dụ:

```txt
SMART_SUPERMARKET_SYSTEM_20260518_150000.DMP
```

### Bước 3: Xác nhận import

Khi chương trình hỏi:

```txt
Type YES to continue import:
```

gõ:

```txt
YES
```

Nếu thành công sẽ thấy:

```txt
[DONE] Import completed successfully.
```

---

## 8. Quy trình backup an toàn trước khi demo

Nên làm theo thứ tự:

```txt
1. Mở app và kiểm tra dữ liệu hiện tại ổn.
2. Tắt app Java.
3. Chạy export_dmp.bat.
4. Kiểm tra có file .DMP mới trong thư mục backup.
5. Kiểm tra file .DMP có dung lượng > 0 KB.
6. Mở DataGrip, refresh database.
7. Chạy lại app để test nhanh đăng nhập, sản phẩm, kho, hóa đơn.
```

---

## 9. Quy trình restore khi database bị lỗi

```txt
1. Tắt app Java.
2. Mở thư mục backup.
3. Chạy restore_dmp.bat.
4. Gõ YES để xác nhận.
5. Chờ import hoàn tất.
6. Mở DataGrip, refresh schema.
7. Chạy lại app Java.
8. Test nhanh đăng nhập và các chức năng chính.
```

---

## 10. Lưu ý quan trọng về Oracle DIRECTORY

Oracle Data Pump không đọc/ghi file trực tiếp như Java đọc file Windows thông thường.

Script sẽ tạo Oracle Directory:

```sql
CREATE OR REPLACE DIRECTORY DATA_PUMP_DIR_SMART AS '<đường dẫn thư mục backup>';
```

Nếu Oracle chạy trên chính máy của bạn, file `.DMP` sẽ nằm trong:

```txt
SieuThiThongMinh_Java\backup
```

Nếu Oracle chạy trên máy khác trong LAN, file `.DMP` sẽ được tạo trên máy đang chạy Oracle, không nhất thiết nằm trên máy client.

Ví dụ:

```txt
Bạn chạy file .bat trên máy A
Oracle chạy trên máy B
=> File .DMP thực tế được tạo trên máy B
```

Vì vậy khi demo nhóm, nên thống nhất một máy làm máy Oracle chính.

---

## 11. Các lỗi thường gặp

### Lỗi `sqlplus is not recognized`

Nguyên nhân:

```txt
Máy chưa cấu hình Oracle vào PATH.
```

Cách xử lý:

```txt
Thêm thư mục Oracle bin vào Environment Variables -> Path.
```

Ví dụ:

```txt
C:\app\<username>\product\21c\dbhomeXE\bin
```

---

### Lỗi `expdp is not recognized`

Nguyên nhân:

```txt
Chưa cài Oracle Client đầy đủ hoặc PATH chưa có Oracle bin.
```

Cách xử lý:

```txt
Cài Oracle Client hoặc dùng máy có Oracle Database.
```

---

### Lỗi `ORA-01017: invalid username/password`

Nguyên nhân:

```txt
Sai user hoặc password Oracle.
```

Cách xử lý:

Mở file `.bat` và sửa lại:

```bat
set DB_ADMIN_USER=system
set DB_ADMIN_PASSWORD=Admin123
```

---

### Lỗi `ORA-12154` hoặc không kết nối được Oracle

Nguyên nhân có thể là:

```txt
Sai host
Sai port
Sai service name
Oracle chưa chạy
Firewall chặn kết nối
```

Cách xử lý:

Kiểm tra lại:

```bat
set DB_HOST=...
set DB_PORT=...
set DB_SERVICE=...
```

Nên test connection bằng DataGrip trước.

---

### Import xong nhưng không thấy bảng

Nguyên nhân có thể là import vào sai schema.

Kiểm tra biến:

```bat
set APP_SCHEMA=SYSTEM
```

Nếu database dùng schema khác, đổi `APP_SCHEMA` đúng với user chứa bảng.

---

## 12. Có nên push file `.DMP` lên GitHub không?

Không nên push file `.DMP` và file `.log` lên GitHub vì:

- File backup có thể lớn.
- Có thể chứa dữ liệu demo hoặc dữ liệu nhạy cảm.
- Làm repository nặng.

Nên push:

```txt
export_dmp.bat
import_dmp.bat
restore_dmp.bat
docs/database_backup_guide.md
```

Không nên push:

```txt
*.DMP
*.log
```

Có thể thêm vào `.gitignore`:

```gitignore
backup/*.DMP
backup/*.dmp
backup/*.log
backup/*.LOG
```

Nếu cần chia sẻ dữ liệu demo, gửi file `.DMP` riêng qua Google Drive, OneDrive hoặc Zalo.

---

## 13. Checklist backup thành công

Trước khi báo backup thành công, kiểm tra:

```txt
[ ] Chạy export_dmp.bat không lỗi
[ ] Có file .DMP mới
[ ] Có file EXPORT.log mới
[ ] File .DMP dung lượng > 0 KB
[ ] DataGrip kết nối được database
[ ] App Java chạy lại bình thường
[ ] Đăng nhập được
[ ] Xem được sản phẩm, khách hàng, hóa đơn, tồn kho
```

---

## 14. Lệnh Git sau khi chỉnh file backup

Sau khi chỉnh các file backup hoặc tài liệu này, chạy:

```bash
git add backup/export_dmp.bat backup/import_dmp.bat backup/restore_dmp.bat docs/database_backup_guide.md
git commit -m "Add Oracle database backup guide"
git push
```

Nếu có chỉnh `.gitignore`:

```bash
git add .gitignore
git commit -m "Ignore Oracle backup dump files"
git push
```
