# Hướng dẫn merge dữ liệu cá nhân của thành viên

Tài liệu này hướng dẫn cách mỗi thành viên xuất dữ liệu từ database cá nhân và nhập vào database chung để đồng bộ dữ liệu demo cho dự án **Smart Supermarket - Store Portal**.

## 1. Mục tiêu

Mỗi thành viên có thể có dữ liệu riêng trên máy của mình, ví dụ:

- Sản phẩm
- Nhân viên
- Khách hàng
- Hóa đơn
- Chi tiết hóa đơn
- Tồn kho
- Khuyến mãi

Để đồng bộ dữ liệu, mỗi người export dữ liệu của mình ra file `.dmp`, sau đó import vào database chung trên Docker hoặc Oracle local.

---

## 2. Cấu hình database chung

Database Docker hiện dùng cấu hình:

| Field | Value |
|---|---|
| Host | `localhost` |
| Port | `1522` |
| Service name | `FREEPDB1` |
| Username | `system` |
| Password | `Admin123` |
| JDBC URL | `jdbc:oracle:thin:@//localhost:1522/FREEPDB1` |

Nếu máy thành viên dùng Oracle local riêng thì có thể là:

```text
Host: localhost
Port: 1521
SID/Service: orcl
Username: system
Password: Admin123
```

---

## 3. Thành viên export dữ liệu cá nhân

Trên máy của từng thành viên, tạo thư mục backup, ví dụ:

```text
E:\JAVA\DoAnFinal\SieuThiThongMinh_Java\backup
```

Trong DataGrip, đăng nhập database cá nhân bằng user `system`, chạy:

```sql
CREATE OR REPLACE DIRECTORY DATA_PUMP_DIR_MEMBER AS 'E:\JAVA\DoAnFinal\SieuThiThongMinh_Java\backup';
```

Sau đó mở CMD hoặc PowerShell tại máy cá nhân và chạy:

```bash
expdp system/Admin123@orcl DIRECTORY=DATA_PUMP_DIR_MEMBER DUMPFILE=member_data.dmp LOGFILE=member_data_export.log SCHEMAS=SYSTEM
```

Nếu database cá nhân dùng service khác, thay `orcl` bằng service tương ứng, ví dụ:

```bash
expdp system/Admin123@localhost:1521/orcl DIRECTORY=DATA_PUMP_DIR_MEMBER DUMPFILE=member_data.dmp LOGFILE=member_data_export.log SCHEMAS=SYSTEM
```

Sau khi chạy xong, gửi cho người merge 2 file:

```text
member_data.dmp
member_data_export.log
```

---

## 4. Copy file dump vào Docker database chung

Trên máy dùng database Docker chung, đặt file `.dmp` vào thư mục:

```text
backup/member_data.dmp
```

Sau đó chạy:

```bash
docker exec -it supermarket-oracle bash -lc "mkdir -p /tmp/dpump && chmod 777 /tmp/dpump"
```

```bash
docker cp backup\member_data.dmp supermarket-oracle:/tmp/dpump/member_data.dmp
```

---

## 5. Tạo Oracle Directory trong Docker

Vào container:

```bash
docker exec -it supermarket-oracle bash
```

Đăng nhập SQLPlus bằng SYS:

```bash
sqlplus sys/Admin123@FREEPDB1 as sysdba
```

Chạy:

```sql
CREATE OR REPLACE DIRECTORY DATA_PUMP_DIR_SMART AS '/tmp/dpump';
GRANT READ, WRITE ON DIRECTORY DATA_PUMP_DIR_SMART TO SYSTEM;
EXIT;
```

Thoát container:

```bash
exit
```

---

## 6. Import dữ liệu thành viên vào database chung

Nếu file dump được export từ schema `SYSTEM`, chạy:

```bash
docker exec -it supermarket-oracle impdp system/Admin123@FREEPDB1 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=member_data.dmp LOGFILE=member_data_import.log TABLE_EXISTS_ACTION=APPEND
```

`TABLE_EXISTS_ACTION=APPEND` sẽ thêm dữ liệu vào bảng hiện có.

Nếu muốn ghi đè dữ liệu cũ trong bảng, chỉ dùng khi chắc chắn:

```bash
docker exec -it supermarket-oracle impdp system/Admin123@FREEPDB1 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=member_data.dmp LOGFILE=member_data_import.log TABLE_EXISTS_ACTION=REPLACE
```

Khuyến nghị khi merge dữ liệu nhiều thành viên:

```text
Dùng APPEND trước.
Chỉ dùng REPLACE khi muốn khôi phục toàn bộ database từ một bản backup chuẩn.
```

---

## 7. Kiểm tra dữ liệu sau khi import

Trong DataGrip, connect database Docker bằng:

```text
Host: localhost
Port: 1522
Service name: FREEPDB1
User: system
Password: Admin123
```

Chạy kiểm tra:

```sql
SELECT COUNT(*) FROM EMPLOYEES;
SELECT COUNT(*) FROM ACCOUNTS;
SELECT COUNT(*) FROM PRODUCTS;
SELECT COUNT(*) FROM INVENTORY;
SELECT COUNT(*) FROM CUSTOMERS;
SELECT COUNT(*) FROM ORDERS;
SELECT COUNT(*) FROM ORDER_DETAILS;
```

Nếu số lượng bản ghi tăng lên đúng như dữ liệu thành viên gửi thì import thành công.

---

## 8. Lưu ý khi merge dữ liệu nhiều người

### 8.1. Tránh trùng khóa chính

Nếu nhiều thành viên cùng tạo dữ liệu, cần tránh trùng ID, ví dụ:

```text
EMPLOYEE_ID
PRODUCT_ID
ORDER_ID
CUSTOMER_ID
ACCOUNT_ID
```

Nên quy ước prefix theo tên thành viên hoặc theo module, ví dụ:

```text
EMP_VI_001
EMP_QUYNH_001
PROD_TUNG_001
ORDER_STAFF_001
```

### 8.2. Không import bừa bảng hệ thống

Không nên merge các bảng hoặc object hệ thống không thuộc project. Chỉ kiểm tra các bảng nghiệp vụ của đồ án.

### 8.3. Reset trạng thái online sau khi import

Sau khi import dữ liệu, nên reset trạng thái đăng nhập:

```sql
UPDATE ACCOUNTS
SET ACTIVE_SESSIONS = 0,
    CURRENT_SESSION_ID = NULL,
    ONLINE_STATUS = 'OFFLINE',
    LAST_HEARTBEAT_AT = NULL,
    LAST_LOGIN_AT = NULL,
    LAST_LOGOUT_AT = NULL;

COMMIT;
```

---

## 9. Cấu hình Java sau khi merge

File:

```text
src/main/resources/database.properties
```

Máy chạy Docker port `1522`:

```properties
db.url=jdbc:oracle:thin:@//localhost:1522/FREEPDB1
db.username=system
db.password=Admin123
```

Máy dùng Docker port `1521`:

```properties
db.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
db.username=system
db.password=Admin123
```

Sau đó chạy:

```text
NetBeans → Clean and Build → Run Project
```

---

## 10. Quy trình chuẩn cho nhóm

```text
1. Mỗi thành viên export database cá nhân ra file .dmp.
2. Gửi file .dmp cho người phụ trách merge.
3. Người phụ trách copy file .dmp vào Docker container.
4. Import bằng impdp với TABLE_EXISTS_ACTION=APPEND.
5. Kiểm tra số lượng bản ghi trong DataGrip.
6. Reset trạng thái online trong ACCOUNTS.
7. Chạy app Java kiểm thử.
8. Nếu ổn, export lại một file demo chuẩn cho cả nhóm.
```
