# Backup Database Demo

Thư mục này dùng để lưu file backup database và hướng dẫn restore nhanh cho dự án **Smart Supermarket - Store Portal**.

## 1. Thành phần trong thư mục

| File | Mục đích |
|---|---|
| `README_BACKUP.md` | Hướng dẫn backup/restore ngắn gọn |
| `export_dmp.bat` | Export database Oracle ra file `.dmp` |
| `import_dmp.bat` | Import file `.dmp` vào Oracle |
| `SMART_SUPERMARKET_DEMO.DMP` | File dump database demo |

---

## 2. Thông tin database đang dùng

```text
Oracle User: system
Password: mật khẩu Oracle của máy demo
Host: localhost hoặc IP máy chạy Oracle
Port: 1521
Service/SID: orcl
```

URL JDBC trong Java:

```text
jdbc:oracle:thin:@localhost:1521:orcl
```

Nếu chạy realtime qua LAN, thay `localhost` bằng IP máy chủ Oracle, ví dụ:

```text
jdbc:oracle:thin:@10.0.232.16:1521:orcl
```

---

## 3. Tạo Oracle Directory trong DataGrip

Đăng nhập DataGrip bằng user `system`, mở SQL Console và chạy:

```sql
CREATE OR REPLACE DIRECTORY DATA_PUMP_DIR_SMART
AS 'E:\JAVA\DoAnFinal\SieuThiThongMinh_Java\backup';
```

Kiểm tra lại:

```sql
SELECT directory_name, directory_path
FROM all_directories
WHERE directory_name = 'DATA_PUMP_DIR_SMART';
```

> Lưu ý: đường dẫn trên phải tồn tại trên máy đang chạy Oracle Database.

---

## 4. Export database ra file DMP

Chạy file:

```text
backup/export_dmp.bat
```

Hoặc chạy trực tiếp bằng CMD:

```bat
expdp system/<DB_PASSWORD>@//localhost:1521/orcl DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=SMART_SUPERMARKET_DEMO.DMP LOGFILE=smart_supermarket_export.log REUSE_DUMPFILES=Y
```

Nếu Oracle nằm trên máy khác trong LAN, đổi `localhost` thành IP máy đó:

```bat
expdp system/<DB_PASSWORD>@//10.0.232.16:1521/orcl DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=SMART_SUPERMARKET_DEMO.DMP LOGFILE=smart_supermarket_export.log REUSE_DUMPFILES=Y
```

Sau khi export thành công, trong thư mục `backup/` sẽ có:

```text
SMART_SUPERMARKET_DEMO.DMP
smart_supermarket_export.log
```

---

## 5. Import database từ file DMP

Đảm bảo file này đã nằm trong thư mục `backup/`:

```text
SMART_SUPERMARKET_DEMO.DMP
```

Sau đó chạy file:

```text
backup/import_dmp.bat
```

Hoặc chạy trực tiếp bằng CMD:

```bat
impdp system/<DB_PASSWORD>@//localhost:1521/orcl DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=SMART_SUPERMARKET_DEMO.DMP LOGFILE=smart_supermarket_import.log TABLE_EXISTS_ACTION=REPLACE
```

Nếu dùng IP máy chủ Oracle:

```bat
impdp system/<DB_PASSWORD>@//10.0.232.16:1521/orcl DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=SMART_SUPERMARKET_DEMO.DMP LOGFILE=smart_supermarket_import.log TABLE_EXISTS_ACTION=REPLACE
```

---

## 6. Kiểm tra nhanh sau khi import

Mở DataGrip bằng user `system` và chạy:

```sql
SELECT COUNT(*) FROM PRODUCTS;
SELECT COUNT(*) FROM INVENTORY;
SELECT COUNT(*) FROM EMPLOYEES;
SELECT COUNT(*) FROM ACCOUNTS;
SELECT COUNT(*) FROM ORDERS;
SELECT COUNT(*) FROM ORDER_DETAILS;
```

Nếu các bảng đều có dữ liệu thì có thể chạy app Java.

---

## 7. Chạy project Java

Kiểm tra file kết nối database trong project, ví dụ `DatabaseConnection.java`:

```text
jdbc:oracle:thin:@localhost:1521:orcl
system
mật khẩu Oracle của máy demo
```

Sau đó mở NetBeans:

```text
Clean and Build → Run Project
```

---

## 8. Ghi chú

- Project hiện đang backup theo user `system` và service `orcl`.
- File `.dmp` không chỉnh sửa bằng tay.
- Nếu import/export lỗi `ORA-12514`, kiểm tra lại service name `orcl`.
- Nếu lỗi `DATA_PUMP_DIR_SMART is invalid`, chạy lại bước tạo Oracle Directory trong DataGrip.
- Nếu chạy realtime qua nhiều máy, các máy client cần trỏ JDBC URL về IP máy chủ Oracle.
