# Backup Database Demo

Thư mục này dùng để lưu file backup database và hướng dẫn restore nhanh cho dự án **Smart Supermarket - Store Portal**.

## 1. Thành phần trong thư mục

| File | Mục đích |
|---|---|
| `README_BACKUP.md` | Hướng dẫn restore/backup ngắn gọn |
| `export_dmp.bat` | Lệnh export database Oracle ra file `.dmp` |
| `import_dmp.bat` | Lệnh import file `.dmp` vào Oracle |
| `smart_supermarket_demo.dmp` | File dump database demo, tạo sau khi chạy `export_dmp.bat` |

> Lưu ý: file `.dmp` thật cần được export từ máy có Oracle Database đang chứa dữ liệu demo.

---

## 2. Thông tin database demo

```text
Oracle User: system
Password: Admin123
Host: localhost(hoặc địa chỉ IP máy của mình để chạy real-time)
Port: 1521
SID/Service: xe hoặc XEPDB1
```

URL Java thường dùng:

```text
jdbc:oracle:thin:@localhost:1521:orcl
```

hoặc:

```text
jdbc:oracle:thin:@//localhost:1521/XEPDB1
```

---

## 3. Restore database bằng DataGrip

### Bước 1: Tạo user Oracle

Đăng nhập Oracle bằng user `system`, mở SQL Console trong DataGrip và chạy:

```sql
DROP USER SMART_SUPERMARKET CASCADE;

CREATE USER system IDENTIFIED BY Admin123;
GRANT CONNECT, RESOURCE TO SMART_SUPERMARKET;
ALTER USER system QUOTA UNLIMITED ON USERS;
```

Nếu chưa từng tạo user thì có thể bỏ dòng `DROP USER`.

---

### Bước 2: Tạo thư mục dump cho Oracle

Trên Windows tạo thư mục:

```text
C:\oracle_backup
```

Trong DataGrip, đăng nhập bằng `system` và chạy:

```sql
CREATE OR REPLACE DIRECTORY DATA_PUMP_DIR_SMART AS 'C:\oracle_backup';
GRANT READ, WRITE ON DIRECTORY DATA_PUMP_DIR_SMART TO SMART_SUPERMARKET;
```

---

### Bước 3: Copy file dump

Copy file:

```text
backup/smart_supermarket_demo.dmp
```

vào thư mục:

```text
C:\oracle_backup\smart_supermarket_demo.dmp
```

---

### Bước 4: Import database

Mở CMD tại Windows và chạy:

```bat
impdp SMART_SUPERMARKET/123456 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_import.log TABLE_EXISTS_ACTION=REPLACE
```

Hoặc chạy trực tiếp file:

```text
backup/import_dmp.bat
```

---

## 4. Export database ra file dmp

Sau khi dữ liệu demo đã ổn định, chạy file:

```text
backup/export_dmp.bat
```

File `.dmp` sẽ được tạo tại:

```text
C:\oracle_backup\smart_supermarket_demo.dmp
```

Sau đó copy file này vào thư mục `backup/` của project nếu muốn nộp kèm.

---

## 5. Kiểm tra nhanh sau khi restore

Sau khi import xong, mở DataGrip bằng user `SMART_SUPERMARKET` và chạy:

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

## 6. Chạy project Java

Kiểm tra file kết nối database trong project, ví dụ `DatabaseConnection.java`:

```java
jdbc:oracle:thin:@localhost:1521:orcl
system
Admin123
```

Sau đó mở NetBeans:

```text
Clean and Build → Run Project
```

---

## 7. Ghi chú

- File `.dmp` không nên chỉnh sửa bằng tay.
- Nếu import lỗi do khác SID/Service, kiểm tra lại Oracle đang dùng `xe` hay `XEPDB1`.
- Nếu muốn tạo database từ SQL thay vì `.dmp`, có thể dùng thêm các file schema/seed SQL nếu nhóm có chuẩn bị riêng.
