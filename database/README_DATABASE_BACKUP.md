# Hướng Dẫn Backup, Restore Và Khởi Tạo Database Demo

Tài liệu này hướng dẫn cách chuẩn bị, backup, khôi phục và chạy dữ liệu demo cho dự án **Smart Supermarket - Store Portal** bằng **Oracle Database** và **DataGrip**.

Mục tiêu là sau khi giảng viên hoặc thành viên nhóm **clone source code từ GitHub**, chỉ cần làm theo từng bước trong file này là có thể tạo lại database, import dữ liệu demo và chạy được ứng dụng Java Swing.

---

## 1. Mục đích của bộ backup database

Bộ backup/database demo dùng để:

- Tạo lại toàn bộ cấu trúc database từ đầu.
- Có sẵn dữ liệu mẫu để đăng nhập và demo app.
- Có dữ liệu sản phẩm, tồn kho, khách hàng, hóa đơn, KPI và báo cáo thống kê.
- Reset dữ liệu trước khi thuyết trình.
- Demo các tình huống đồng thời như:
  - Lost Update
  - Deadlock
  - Realtime Sync tồn kho
  - Thanh toán đồng thời nhiều máy POS

Nói ngắn gọn: **clone code thôi chưa đủ**, vì app cần database. Do đó project cần kèm theo các file SQL backup/demo.

---

## 2. Cấu trúc thư mục database đề xuất

Trong project nên có thư mục:

```text
SieuThiOnline/
│
├── database/
│   ├── 00_drop_all.sql
│   ├── 01_schema.sql
│   ├── 02_seed_demo.sql
│   ├── 03_demo_reset_data.sql
│   ├── demo_backup.dmp
│   └── README_DATABASE_BACKUP.md
│
├── src/
├── pom.xml
└── README.md
```

Ý nghĩa từng file:

| File | Vai trò |
|---|---|
| `00_drop_all.sql` | Xóa bảng/cấu trúc cũ nếu muốn tạo lại database từ đầu |
| `01_schema.sql` | Tạo bảng, khóa chính, khóa ngoại, sequence, trigger, view nếu có |
| `02_seed_demo.sql` | Thêm dữ liệu mẫu để chạy demo |
| `03_demo_reset_data.sql` | Reset dữ liệu demo về trạng thái chuẩn trước khi thuyết trình |
| `demo_backup.dmp` | File backup Oracle Data Pump, có thể bổ sung sau |
| `README_DATABASE_BACKUP.md` | File hướng dẫn chi tiết này |

Khuyến nghị: ưu tiên dùng file `.sql` vì dễ đọc, dễ sửa và dễ chạy trong DataGrip. File `.dmp` có thể tạo sau để backup nguyên database.

---

## 3. Yêu cầu trước khi thực hiện

Máy cần có:

- Oracle Database hoặc Oracle XE.
- DataGrip.
- JDK phù hợp với project Java.
- Source code đã clone từ GitHub.
- Oracle JDBC Driver nếu DataGrip chưa tự tải được.

Thông tin kết nối mẫu:

```text
Host: localhost
Port: 1521
SID/Service: xe hoặc XEPDB1
Username: SMART_SUPERMARKET
Password: 123456
```

Tùy máy, Oracle có thể dùng một trong hai dạng URL:

```text
jdbc:oracle:thin:@localhost:1521:xe
```

hoặc:

```text
jdbc:oracle:thin:@//localhost:1521/XEPDB1
```

---

## 4. Quy trình tổng quan

Thực hiện theo thứ tự:

```text
Bước 1: Clone source code từ GitHub
Bước 2: Tạo user Oracle riêng cho project
Bước 3: Kết nối user đó bằng DataGrip
Bước 4: Chạy 00_drop_all.sql nếu cần xóa dữ liệu cũ
Bước 5: Chạy 01_schema.sql để tạo cấu trúc bảng
Bước 6: Chạy 02_seed_demo.sql để thêm dữ liệu demo
Bước 7: Chạy 03_demo_reset_data.sql trước khi demo
Bước 8: Kiểm tra cấu hình DatabaseConnection.java
Bước 9: Run app Java Swing trong NetBeans
```

---

## 5. Bước 1 - Clone source code từ GitHub

Mở terminal hoặc Git Bash:

```bash
git clone <LINK_GITHUB_PROJECT>
```

Ví dụ:

```bash
git clone https://github.com/TanVi3001/SieuThiThongMinh_Java.git
```

Sau đó mở project bằng NetBeans hoặc IDE đang dùng.

---

## 6. Bước 2 - Tạo user Oracle cho project

Mở DataGrip và kết nối vào Oracle bằng tài khoản có quyền cao, ví dụ:

```text
Username: system
Password: mật khẩu Oracle của máy
```

Sau khi kết nối thành công, mở SQL Console và chạy:

```sql
CREATE USER SMART_SUPERMARKET IDENTIFIED BY 123456;

GRANT CONNECT, RESOURCE TO SMART_SUPERMARKET;

ALTER USER SMART_SUPERMARKET QUOTA UNLIMITED ON USERS;
```

Nếu user đã tồn tại và muốn tạo lại từ đầu, dùng:

```sql
DROP USER SMART_SUPERMARKET CASCADE;

CREATE USER SMART_SUPERMARKET IDENTIFIED BY 123456;

GRANT CONNECT, RESOURCE TO SMART_SUPERMARKET;

ALTER USER SMART_SUPERMARKET QUOTA UNLIMITED ON USERS;
```

Lưu ý: `DROP USER ... CASCADE` sẽ xóa toàn bộ bảng và dữ liệu thuộc user đó.

---

## 7. Bước 3 - Kết nối database bằng DataGrip

Trong DataGrip:

```text
Database panel → dấu + → Data Source → Oracle
```

Điền thông tin:

```text
Host: localhost
Port: 1521
User: SMART_SUPERMARKET
Password: 123456
SID/Service: xe hoặc XEPDB1
```

Nếu dùng SID `xe`, URL thường là:

```text
jdbc:oracle:thin:@localhost:1521:xe
```

Nếu dùng service `XEPDB1`, URL thường là:

```text
jdbc:oracle:thin:@//localhost:1521/XEPDB1
```

Bấm:

```text
Test Connection
```

Nếu thành công thì bấm:

```text
OK
```

---

## 8. Bước 4 - Chạy các file SQL trong DataGrip

Sau khi tạo connection `SMART_SUPERMARKET`, chạy các file trong thư mục `database/` theo đúng thứ tự.

Thứ tự chuẩn:

```text
00_drop_all.sql
01_schema.sql
02_seed_demo.sql
03_demo_reset_data.sql
```

Cách chạy trong DataGrip:

```text
Mở file .sql → chọn connection SMART_SUPERMARKET → Run File
```

Hoặc:

```text
Chuột phải vào file .sql → Run
```

Nếu chỉ bấm `Ctrl + Enter`, DataGrip có thể chỉ chạy câu lệnh đang đặt con trỏ. Với file dài, nên dùng **Run File**.

---

## 9. File 00_drop_all.sql dùng để làm gì?

File này dùng để xóa dữ liệu/cấu trúc cũ trước khi tạo lại database.

Nội dung thường có dạng:

```sql
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE ORDER_DETAILS CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE ORDERS CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE INVENTORY CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
```

Với Oracle, nên dùng block `BEGIN ... EXCEPTION ... END;` để tránh lỗi nếu bảng chưa tồn tại.

Chỉ chạy file này khi muốn reset toàn bộ database.

---

## 10. File 01_schema.sql dùng để làm gì?

File này tạo toàn bộ cấu trúc database.

Nên chứa:

- `CREATE TABLE`
- `PRIMARY KEY`
- `FOREIGN KEY`
- `CREATE SEQUENCE`
- `CREATE TRIGGER`
- `CREATE VIEW` nếu có

Ví dụ:

```sql
CREATE TABLE PRODUCTS (
    product_id VARCHAR2(50) PRIMARY KEY,
    product_name NVARCHAR2(255),
    base_price NUMBER(12,2),
    is_deleted NUMBER(1) DEFAULT 0
);

CREATE TABLE INVENTORY (
    inventory_id VARCHAR2(50) PRIMARY KEY,
    product_id VARCHAR2(50),
    quantity NUMBER(10),
    is_deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id)
        REFERENCES PRODUCTS(product_id)
);
```

Sau khi chạy file này, kiểm tra trong DataGrip xem các bảng đã xuất hiện chưa.

---

## 11. File 02_seed_demo.sql dùng để làm gì?

File này thêm dữ liệu mẫu để demo app.

Nên có dữ liệu cho:

- Roles/phân quyền.
- Accounts/tài khoản đăng nhập.
- Employees/nhân viên.
- Products/sản phẩm.
- Inventory/tồn kho.
- Customers/khách hàng.
- Payment methods/phương thức thanh toán.
- Orders/hóa đơn.
- Order details/chi tiết hóa đơn.
- KPI data nếu có.
- Dữ liệu doanh thu nhiều tháng cho dashboard.

Cuối file phải có:

```sql
COMMIT;
```

---

## 12. File 03_demo_reset_data.sql dùng để làm gì?

File này dùng để reset dữ liệu về trạng thái chuẩn trước khi thuyết trình.

Ví dụ:

```sql
UPDATE INVENTORY
SET quantity = 4
WHERE product_id = 'SP_DEMO_LOST';

DELETE FROM ORDER_DETAILS
WHERE order_id IN (
    SELECT order_id
    FROM ORDERS
    WHERE order_id LIKE 'DEMO_%'
);

DELETE FROM ORDERS
WHERE order_id LIKE 'DEMO_%';

COMMIT;
```

Mục đích:

- Đảm bảo sản phẩm demo Lost Update luôn có tồn kho ban đầu.
- Xóa hóa đơn test sinh ra trong quá trình demo.
- Đưa dữ liệu dashboard về trạng thái dễ trình bày.
- Tránh việc demo lần sau bị sai do dữ liệu lần trước còn sót lại.

---

## 13. Dữ liệu demo nên chuẩn bị

### 13.1. Tài khoản demo

Nên có ít nhất các tài khoản:

| Vai trò | Username | Password | Mục đích |
|---|---|---|---|
| Admin | admin | 123456 | Quản trị hệ thống |
| Manager | manager | 123456 | Xem báo cáo, quản lý cửa hàng |
| Staff 1 | staff_quynh | 123456 | Demo bán hàng |
| Staff 2 | staff_tuan | 123456 | Demo realtime/lost update |

---

### 13.2. Sản phẩm demo bán hàng bình thường

Cần có sản phẩm tồn kho nhiều để bán bình thường:

```text
Mì Hảo Hảo
Gạo ST25
Nước mắm
Dầu ăn
Trứng gà
```

---

### 13.3. Sản phẩm demo cảnh báo tồn kho thấp

Ví dụ:

```text
SP_LOW_STOCK
Tên: Sản phẩm sắp hết hàng
Tồn kho: 2
```

Dùng để demo trạng thái:

```text
Sắp hết
```

---

### 13.4. Sản phẩm demo Lost Update

Ví dụ:

```text
SP_DEMO_LOST
Tên: Dầu ăn demo Lost Update
Tồn kho ban đầu: 4
```

Kịch bản demo:

```text
Staff_Quynh thêm 4 sản phẩm vào giỏ.
Staff_Tuan thêm 1 sản phẩm cùng loại vào giỏ.
Staff_Quynh thanh toán trước.
Tồn kho giảm từ 4 xuống 0.
Staff_Tuan thanh toán sau.
Hệ thống kiểm tra tồn kho thực tế và báo xung đột tồn kho.
Giao dịch Staff_Tuan bị từ chối.
```

Ý nghĩa:

```text
Hệ thống không cho bán vượt tồn kho dù hai máy POS thao tác gần như cùng lúc.
Realtime giúp máy còn lại cập nhật tồn kho và cảnh báo ngay trên giỏ hàng.
```

---

### 13.5. Sản phẩm demo Deadlock

Ví dụ:

```text
SP_DEMO_A
SP_DEMO_B
```

Kịch bản lý thuyết:

```text
Máy POS 1 khóa SP_DEMO_A rồi chờ SP_DEMO_B.
Máy POS 2 khóa SP_DEMO_B rồi chờ SP_DEMO_A.
Hai giao dịch chờ nhau tạo thành deadlock.
Oracle phát hiện ORA-00060 và rollback một giao dịch.
```

Cách xử lý trong hệ thống:

```text
Trước khi update tồn kho, giỏ hàng được sắp xếp theo product_id tăng dần.
Mọi giao dịch đều khóa sản phẩm theo cùng một thứ tự.
Nhờ vậy tránh tình trạng khóa chéo.
```

---

### 13.6. Dữ liệu dashboard Power BI-style

Để biểu đồ doanh thu theo tháng hiển thị đẹp, cần có đơn hàng ở nhiều tháng:

```text
01/2026
02/2026
03/2026
04/2026
05/2026
```

Nếu chỉ có dữ liệu ở một tháng, chart chỉ hiện một điểm, không có đường line rõ ràng.

---

## 14. Kiểm tra dữ liệu sau khi import

Sau khi chạy xong `02_seed_demo.sql`, chạy các câu lệnh kiểm tra:

```sql
SELECT COUNT(*) FROM PRODUCTS;
SELECT COUNT(*) FROM INVENTORY;
SELECT COUNT(*) FROM EMPLOYEES;
SELECT COUNT(*) FROM ACCOUNTS;
SELECT COUNT(*) FROM ORDERS;
SELECT COUNT(*) FROM ORDER_DETAILS;
```

Kiểm tra tồn kho:

```sql
SELECT product_id, quantity
FROM INVENTORY
ORDER BY product_id;
```

Kiểm tra tài khoản:

```sql
SELECT account_id, username, role_id, status
FROM ACCOUNTS
WHERE NVL(is_deleted, 0) = 0;
```

Kiểm tra doanh thu theo tháng:

```sql
SELECT 
    TO_CHAR(order_date, 'MM/YYYY') AS month,
    COUNT(*) AS total_orders,
    SUM(total_amount) AS total_revenue
FROM ORDERS
WHERE NVL(is_deleted, 0) = 0
GROUP BY TO_CHAR(order_date, 'MM/YYYY')
ORDER BY month;
```

Kiểm tra KPI/doanh thu nhân viên:

```sql
SELECT 
    e.employee_id,
    e.employee_name,
    e.role_id,
    COUNT(o.order_id) AS total_orders,
    NVL(SUM(o.total_amount), 0) AS revenue
FROM EMPLOYEES e
LEFT JOIN ORDERS o 
    ON e.employee_id = o.employee_id
    AND NVL(o.is_deleted, 0) = 0
WHERE NVL(e.is_deleted, 0) = 0
GROUP BY e.employee_id, e.employee_name, e.role_id
ORDER BY revenue DESC;
```

---

## 15. Cấu hình kết nối trong Java

Sau khi database đã import xong, kiểm tra file kết nối trong source code.

Thường là:

```text
DatabaseConnection.java
```

Thông tin cần đúng với user Oracle vừa tạo:

```java
String url = "jdbc:oracle:thin:@localhost:1521:xe";
String username = "SMART_SUPERMARKET";
String password = "123456";
```

Nếu máy dùng `XEPDB1`, sửa URL thành:

```java
String url = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";
```

---

## 16. Quy trình demo đề xuất

### 16.1. Reset dữ liệu trước khi demo

Trong DataGrip, chạy:

```sql
@database/03_demo_reset_data.sql
```

Hoặc mở trực tiếp file `03_demo_reset_data.sql` và chọn **Run File**.

---

### 16.2. Chạy app Java

Mở project bằng NetBeans.

Chọn:

```text
Clean and Build
```

Sau đó:

```text
Run Project
```

---

### 16.3. Demo đăng nhập

Đăng nhập bằng các tài khoản demo:

```text
Admin: admin / 123456
Manager: manager / 123456
Staff 1: staff_quynh / 123456
Staff 2: staff_tuan / 123456
```

---

### 16.4. Demo bán hàng bình thường

Các bước:

```text
Staff đăng nhập.
Vào màn hình Bán hàng.
Chọn sản phẩm còn tồn kho.
Thêm vào giỏ.
Chọn phương thức thanh toán.
Bấm Thanh toán.
Hệ thống tạo hóa đơn, trừ tồn kho và cập nhật realtime.
```

---

### 16.5. Demo Lost Update + Realtime

Chuẩn bị:

```text
SP_DEMO_LOST có tồn kho = 4
```

Các bước:

```text
Mở 2 cửa sổ app hoặc 2 máy POS.
Staff_Quynh thêm 4 sản phẩm SP_DEMO_LOST vào giỏ.
Staff_Tuan thêm 1 sản phẩm SP_DEMO_LOST vào giỏ.
Staff_Quynh thanh toán trước.
Tồn kho thực tế về 0.
Realtime gửi sự kiện INVENTORY_CHANGED.
Máy Staff_Tuan cập nhật lại tồn kho trong giỏ.
Dòng sản phẩm chuyển trạng thái Vượt tồn hoặc Lỗi kho.
Nút Thanh toán bị vô hiệu hóa hoặc khi bấm sẽ báo xung đột.
```

Kết quả mong muốn:

```text
Không xảy ra bán âm kho.
Không xảy ra ghi đè tồn kho sai.
Giao dịch sau bị từ chối an toàn.
```

---

### 16.6. Demo Deadlock

Kịch bản giải thích:

```text
Nếu hai giao dịch khóa sản phẩm theo thứ tự khác nhau, deadlock có thể xảy ra.
Ví dụ POS 1 khóa A rồi B, POS 2 khóa B rồi A.
Oracle sẽ phát hiện deadlock và rollback một giao dịch.
```

Cách hệ thống xử lý:

```text
Trước khi cập nhật tồn kho, danh sách sản phẩm trong đơn hàng được sắp xếp theo product_id.
Mọi giao dịch đều khóa sản phẩm theo cùng thứ tự.
Điều này giảm nguy cơ deadlock trong quá trình thanh toán đồng thời.
```

Khi demo, có thể trình bày:

```text
Hệ thống đã áp dụng Locking Hierarchy.
Nếu Oracle trả ORA-00060, Java bắt lỗi DeadlockDetectedException.
Giao dịch bị rollback, không làm sai dữ liệu.
Người dùng được yêu cầu thử thanh toán lại.
```

---

### 16.7. Demo báo cáo thống kê

Đăng nhập Manager.

Vào:

```text
Báo cáo & Thống kê
```

Chọn khoảng ngày:

```text
Từ ngày: 01/01/2026
Đến ngày: ngày hiện tại hoặc 31/05/2026
```

Kiểm tra:

```text
Tổng doanh thu
Tổng đơn hàng
KPI trung bình
Nhân viên xuất sắc
Doanh thu theo tháng
Top nhân viên hiệu suất cao
Bảng KPI nhân viên
Xuất Excel/PDF
```

---

## 17. Tạo file backup .dmp sau này

File `.dmp` là backup Oracle Data Pump. Có thể tạo sau khi database đã ổn định.

### 17.1. Tạo thư mục dump trong Oracle

Đăng nhập bằng user có quyền DBA, chạy:

```sql
CREATE OR REPLACE DIRECTORY DATA_PUMP_DIR_SMART AS 'C:\oracle_backup';
GRANT READ, WRITE ON DIRECTORY DATA_PUMP_DIR_SMART TO SMART_SUPERMARKET;
```

Trên máy Windows, cần tạo sẵn thư mục:

```text
C:\oracle_backup
```

---

### 17.2. Export database ra file .dmp

Mở CMD/Terminal và chạy:

```bash
expdp SMART_SUPERMARKET/123456 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_export.log
```

Sau khi chạy xong, file sẽ nằm ở:

```text
C:\oracle_backup\smart_supermarket_demo.dmp
```

Có thể copy file này vào:

```text
database/demo_backup.dmp
```

Lưu ý: Không nên chỉ phụ thuộc vào `.dmp`, vì máy khác có thể khác version Oracle hoặc khác cấu hình directory.

---

### 17.3. Import file .dmp

Nếu muốn restore từ `.dmp`, tạo user Oracle trước, sau đó chạy:

```bash
impdp SMART_SUPERMARKET/123456 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_import.log
```

Nếu cần remap schema:

```bash
impdp system/your_password DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_import.log REMAP_SCHEMA=OLD_SCHEMA:SMART_SUPERMARKET
```

---

## 18. Lỗi thường gặp và cách xử lý

### 18.1. Không kết nối được Oracle

Kiểm tra:

```text
Oracle service đã chạy chưa.
Port có đúng 1521 không.
SID/service name có đúng không.
Username/password có đúng không.
```

Có thể thử URL:

```text
jdbc:oracle:thin:@localhost:1521:xe
```

hoặc:

```text
jdbc:oracle:thin:@//localhost:1521/XEPDB1
```

---

### 18.2. Lỗi table already exists

Nguyên nhân: database đã có bảng cũ.

Cách xử lý:

```sql
DROP USER SMART_SUPERMARKET CASCADE;
```

Sau đó tạo lại user và chạy lại các file SQL.

---

### 18.3. Chạy seed bị lỗi khóa ngoại

Nguyên nhân: insert sai thứ tự.

Cách xử lý: dữ liệu cha phải insert trước dữ liệu con.

Ví dụ:

```text
ROLES trước ACCOUNTS
PRODUCTS trước INVENTORY
ORDERS trước ORDER_DETAILS
CUSTOMERS trước ORDERS
EMPLOYEES trước ORDERS
```

---

### 18.4. App chạy nhưng không thấy dữ liệu

Kiểm tra database có dữ liệu chưa:

```sql
SELECT COUNT(*) FROM PRODUCTS;
SELECT COUNT(*) FROM INVENTORY;
SELECT COUNT(*) FROM ORDERS;
```

Nếu có dữ liệu nhưng app không hiện, kiểm tra lại file:

```text
DatabaseConnection.java
```

---

### 18.5. Biểu đồ doanh thu theo tháng chỉ có một chấm

Nguyên nhân: chỉ có doanh thu ở một tháng.

Cách xử lý: thêm dữ liệu hóa đơn ở nhiều tháng trong `02_seed_demo.sql`.

---

### 18.6. Lost Update demo không ra lỗi

Kiểm tra:

```text
Sản phẩm demo có tồn kho thấp chưa.
Hai máy POS có thêm cùng một sản phẩm chưa.
Máy thứ nhất đã thanh toán trước chưa.
Realtime có gửi INVENTORY_CHANGED chưa.
Máy thứ hai có refresh lại giỏ chưa.
```

---

## 19. Checklist trước khi nộp đồ án

Trước khi push GitHub, kiểm tra:

```text
[ ] Có thư mục database/
[ ] Có file 00_drop_all.sql
[ ] Có file 01_schema.sql
[ ] Có file 02_seed_demo.sql
[ ] Có file 03_demo_reset_data.sql
[ ] Có file README_DATABASE_BACKUP.md
[ ] Có tài khoản demo Admin/Manager/Staff
[ ] Có dữ liệu sản phẩm demo Lost Update
[ ] Có dữ liệu dashboard nhiều tháng
[ ] Có hướng dẫn cấu hình DatabaseConnection.java
[ ] App chạy được sau khi clone source
```

---

## 20. Cách push file hướng dẫn lên GitHub

Sau khi tạo hoặc sửa file này, chạy:

```bash
git add database/README_DATABASE_BACKUP.md

git commit -m "Add database backup and restore guide"

git push
```

Nếu thêm các file SQL:

```bash
git add database/00_drop_all.sql database/01_schema.sql database/02_seed_demo.sql database/03_demo_reset_data.sql

git commit -m "Add database schema and demo seed scripts"

git push
```

---

## 21. Ghi chú cuối

Khi giảng viên clone project, chỉ cần làm theo quy trình:

```text
1. Tạo user Oracle SMART_SUPERMARKET.
2. Kết nối DataGrip vào user đó.
3. Chạy 00_drop_all.sql nếu cần.
4. Chạy 01_schema.sql.
5. Chạy 02_seed_demo.sql.
6. Chạy 03_demo_reset_data.sql trước khi demo.
7. Kiểm tra DatabaseConnection.java.
8. Run app Java Swing.
```

Nhờ vậy, project không bị phụ thuộc vào dữ liệu cục bộ trên máy thành viên nhóm và có thể demo lại ổn định trên máy khác.
