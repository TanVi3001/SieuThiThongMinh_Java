# Hướng dẫn Pull Code và Cập nhật Database Docker Oracle

Tài liệu này hướng dẫn quy trình làm việc chuẩn cho nhóm khi làm đồ án **Java Swing + Oracle Database**.

Mục tiêu mới của nhóm:

```text
Không cần mở DataGrip để chạy patch thủ công mỗi lần.
Sau khi pull code, chỉ cần chạy 1 file .bat để cập nhật database Docker local.
```

File hỗ trợ chính:

```text
database/run_patches.bat
```

---

## 1. Bối cảnh cần hiểu trước

Nhóm đang làm việc theo mô hình:

```text
GitHub
├── Lưu source code Java
├── Lưu file SQL: schema.sql, seed.sql, patch.sql
├── Lưu file script chạy patch: database/run_patches.bat
└── Lưu tài liệu hướng dẫn

Docker Oracle trên máy từng thành viên
└── Chạy database local riêng của mỗi người
```

Điểm quan trọng nhất:

```text
GitHub chỉ đồng bộ code và file SQL.
GitHub KHÔNG tự cập nhật database đang nằm trong Docker.
```

Vì vậy, sau khi `git pull`, nếu có file SQL patch mới thì mỗi thành viên cần chạy:

```text
database/run_patches.bat
```

Script này sẽ tự đưa các file patch SQL vào Oracle Docker local.

---

## 2. Pull code từ GitHub về máy

Mở terminal tại thư mục project:

```bash
git checkout main
git pull origin main
```

Nếu nhóm đang làm trên branch khác, ví dụ `fix-employee-session-ui`, thì dùng:

```bash
git checkout fix-employee-session-ui
git pull origin fix-employee-session-ui
```

Hoặc pull bằng IDE:

```text
Git → Pull
```

Sau khi pull xong, máy bạn chỉ mới có:

```text
- Code Java mới
- File .sql mới
- File .bat mới
- File README/tài liệu mới
```

Database trong Docker **chưa tự thay đổi** cho tới khi chạy script patch.

---

## 3. Kiểm tra Docker Oracle local

Mở CMD/PowerShell/Terminal và chạy:

```bash
docker ps
```

Nếu Oracle Docker đang chạy, bạn sẽ thấy container kiểu:

```text
CONTAINER ID   IMAGE                       PORTS                    NAMES
abc123         gvenzl/oracle-free:23-slim  0.0.0.0:1521->1521/tcp   supermarket-oracle
```

hoặc nếu máy dùng port 1522:

```text
0.0.0.0:1522->1521/tcp
```

Nếu container chưa chạy:

```bash
docker ps -a
docker start supermarket-oracle
```

Nếu chưa từng tạo container:

```bash
docker compose up -d
```

---

## 4. Cách chạy patch nhanh bằng file .bat

Sau khi pull code xong, chạy file:

```text
database/run_patches.bat
```

Có 2 cách chạy.

### Cách 1: Double click

Mở thư mục project:

```text
SieuThiThongMinh_Java/database/
```

Double click:

```text
run_patches.bat
```

### Cách 2: Chạy bằng terminal

Mở CMD/PowerShell tại thư mục project:

```bash
database/run_patches.bat
```

Script sẽ tự làm các việc sau:

```text
1. Kiểm tra Docker CLI có dùng được không.
2. Kiểm tra container supermarket-oracle có tồn tại không.
3. Nếu container chưa chạy thì tự start.
4. Kiểm tra kết nối Oracle bằng system/Admin123@FREEPDB1.
5. Chạy file database/05_session_and_login_history_patch.sql nếu có.
6. Chạy tất cả file .sql trong database/patches nếu folder này tồn tại.
```

---

## 5. Cấu hình mặc định của run_patches.bat

Trong file:

```text
database/run_patches.bat
```

Có cấu hình mặc định:

```bat
set CONTAINER_NAME=supermarket-oracle
set ORACLE_USER=system
set ORACLE_PASSWORD=Admin123
set ORACLE_SERVICE=FREEPDB1
```

Nghĩa là script sẽ connect vào Oracle Docker bằng:

```text
system/Admin123@FREEPDB1
```

Nếu máy bạn dùng tên container khác hoặc service khác, sửa lại các dòng trên.

Ví dụ nếu service là `XEPDB1`:

```bat
set ORACLE_SERVICE=XEPDB1
```

Nếu container tên khác:

```bat
set CONTAINER_NAME=ten_container_cua_ban
```

---

## 6. Luồng chuẩn sau khi có run_patches.bat

```text
git pull
    ↓
Có code mới và file SQL mới trong project
    ↓
Docker Oracle local đang chạy
    ↓
Chạy database/run_patches.bat
    ↓
Script tự chạy SQL patch vào Oracle Docker
    ↓
Database trong Docker local được cập nhật
    ↓
Clean and Build Java project
    ↓
Run app
```

Đây là luồng khuyên dùng cho nhóm.

---

## 7. Khi nào vẫn cần DataGrip?

Dù có `run_patches.bat`, DataGrip vẫn cần cho các việc:

```text
- Xem bảng và dữ liệu
- Kiểm tra patch đã chạy chưa
- Test câu SQL
- Debug lỗi database
- Import/export dữ liệu demo
```

Nhưng với patch thông thường, không cần mở DataGrip nữa. Chỉ chạy `.bat` là đủ.

---

## 8. Kiểm tra patch đã chạy thành công

Mở DataGrip, connect Oracle Docker local.

Ví dụ Docker port 1521:

```text
Host: localhost
Port: 1521
Service name: FREEPDB1
User: system
Password: Admin123
```

Nếu máy dùng port 1522:

```text
Host: localhost
Port: 1522
Service name: FREEPDB1
User: system
Password: Admin123
```

Chạy kiểm tra:

```sql
SELECT COLUMN_NAME, DATA_TYPE
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'ACCOUNTS'
  AND COLUMN_NAME IN (
      'ACTIVE_SESSIONS',
      'CURRENT_SESSION_ID',
      'ONLINE_STATUS',
      'LAST_HEARTBEAT_AT',
      'LAST_LOGIN_AT',
      'LAST_LOGOUT_AT'
  )
ORDER BY COLUMN_NAME;
```

Nếu có đủ các cột trên là patch đã chạy đúng.

---

## 9. Ví dụ cụ thể: thêm cột LAST_LOGIN_AT vào ACCOUNTS

Một bạn tạo file patch:

```text
database/patches/patch_005_add_last_login.sql
```

Nội dung nên viết dạng an toàn:

```sql
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD LAST_LOGIN_AT TIMESTAMP';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/
```

Bạn đó push lên GitHub.

Các thành viên khác làm:

```bash
git pull origin main
```

Sau đó chạy:

```bash
database/run_patches.bat
```

Script sẽ tự tìm file `.sql` trong `database/patches` và chạy vào Docker Oracle local.

Kiểm tra bằng DataGrip:

```sql
SELECT COLUMN_NAME, DATA_TYPE
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'ACCOUNTS'
  AND COLUMN_NAME = 'LAST_LOGIN_AT';
```

Nếu có kết quả là thành công.

---

## 10. Lỗi thường gặp

### 10.1. Pull code xong nhưng database chưa đổi

Nguyên nhân:

```text
Bạn mới pull file SQL về, nhưng chưa chạy run_patches.bat.
```

Cách fix:

```text
Chạy database/run_patches.bat
```

---

### 10.2. Script báo không tìm thấy container

Ví dụ:

```text
Container "supermarket-oracle" was not found.
```

Cách fix:

```bash
docker ps -a
```

Xem tên container thật, rồi sửa trong `run_patches.bat`:

```bat
set CONTAINER_NAME=ten_container_that
```

Hoặc tạo container bằng:

```bash
docker compose up -d
```

---

### 10.3. Script không connect được Oracle

Có thể Oracle chưa ready.

Xem log:

```bash
docker logs -f supermarket-oracle
```

Chờ database ready rồi chạy lại:

```bash
database/run_patches.bat
```

---

### 10.4. Sai service name

Nếu script connect lỗi, có thể service không phải `FREEPDB1`.

Thử sửa:

```bat
set ORACLE_SERVICE=XEPDB1
```

hoặc:

```bat
set ORACLE_SERVICE=XE
```

Tùy image Oracle mà service khác nhau.

---

### 10.5. Java báo ORA-00904 invalid identifier

Ví dụ:

```text
ORA-00904: "ACTIVE_SESSIONS": invalid identifier
ORA-00904: "LAST_HEARTBEAT_AT": invalid identifier
```

Nguyên nhân:

```text
Code Java đã dùng cột mới nhưng database local chưa được patch.
```

Cách fix:

```text
Chạy database/run_patches.bat
```

---

### 10.6. ALTER TABLE ADD báo cột đã tồn tại

Ví dụ:

```text
ORA-01430: column being added already exists in table
```

Nguyên nhân:

```text
Patch đã từng chạy trước đó.
```

Cách xử lý:

```text
Nếu patch viết dạng an toàn BEGIN...EXCEPTION thì có thể chạy nhiều lần.
Nếu patch chỉ viết ALTER TABLE trực tiếp thì lần 2 sẽ lỗi.
```

Vì vậy, khi tạo patch mới, nên viết dạng an toàn.

---

## 11. Quy tắc tạo file patch mới cho nhóm

Khi có thay đổi database, không sửa trực tiếp trên máy rồi im luôn. Cần tạo file SQL patch và push lên GitHub.

Nên đặt file trong:

```text
database/patches/
```

Ví dụ:

```text
database/patches/patch_006_add_customer_rank.sql
database/patches/patch_007_add_invoice_status.sql
```

Quy trình đúng:

```text
1. Một bạn thay đổi schema/data mẫu.
2. Tạo file SQL patch trong database/patches/.
3. Viết patch dạng an toàn nếu có thể.
4. Commit và push file SQL lên GitHub.
5. Thành viên khác git pull.
6. Thành viên khác chạy database/run_patches.bat.
```

---

## 12. Tóm tắt ngắn gọn

```text
GitHub = nơi lưu code và file SQL.
Docker Oracle = nơi chạy database local của từng người.
run_patches.bat = công cụ tự chạy SQL patch vào Docker Oracle.
DataGrip = công cụ kiểm tra/debug database khi cần.
```

Nhớ kỹ:

```text
git pull chỉ cập nhật project.
git pull không cập nhật database.
Muốn database cập nhật thì chạy database/run_patches.bat.
```
