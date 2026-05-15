# Hướng dẫn Pull Code và Cập nhật Database Docker Oracle bằng DataGrip

Tài liệu này hướng dẫn quy trình làm việc chuẩn cho nhóm khi làm đồ án **Java Swing + Oracle Database**.

## 1. Bối cảnh cần hiểu trước

Nhóm đang làm việc theo mô hình:

```text
GitHub
├── Lưu source code Java
├── Lưu file SQL: schema.sql, seed.sql, patch.sql
└── Lưu tài liệu hướng dẫn

Docker Oracle trên máy từng thành viên
└── Chạy database local riêng của mỗi người
```

Điểm quan trọng nhất:

```text
GitHub chỉ đồng bộ code và file SQL.
GitHub KHÔNG tự cập nhật database đang nằm trong Docker.
```

Vì vậy, sau khi `git pull`, nếu có file SQL mới thì mỗi thành viên phải **tự chạy file SQL đó vào Oracle Docker local bằng DataGrip**.

---

## 2. Pull code từ GitHub về máy

### Cách 1: Pull bằng Terminal/CMD

Mở terminal tại thư mục project:

```bash
git checkout main
git pull origin main
```

Nếu nhóm đang làm trên branch khác, ví dụ `develop` hoặc `fix-employee-session-ui`, thì đổi lại đúng branch:

```bash
git checkout fix-employee-session-ui
git pull origin fix-employee-session-ui
```

### Cách 2: Pull bằng IDE

Nếu dùng NetBeans, IntelliJ hoặc VS Code:

```text
Git → Pull
```

hoặc dùng giao diện Version Control của IDE.

### Lưu ý quan trọng

Sau khi pull xong, máy bạn chỉ mới có:

```text
- Code Java mới
- File .sql mới
- File README/tài liệu mới
```

Database trong Docker **chưa tự thay đổi**.

Ví dụ GitHub có file mới:

```text
database/05_session_and_login_history_patch.sql
```

Sau khi pull, file này chỉ nằm trong project. Bạn vẫn phải mở DataGrip và chạy file này vào database local.

---

## 3. Kiểm tra Docker Oracle local có đang chạy không

Mở CMD/PowerShell/Terminal và chạy:

```bash
docker ps
```

Nếu Oracle Docker đang chạy, bạn sẽ thấy container kiểu:

```text
CONTAINER ID   IMAGE                       PORTS                    NAMES
abc123         gvenzl/oracle-free:23-slim  0.0.0.0:1521->1521/tcp   supermarket-oracle
```

hoặc nếu máy bạn bị trùng port 1521 và dùng port 1522:

```text
0.0.0.0:1522->1521/tcp
```

### Nếu container chưa chạy

Xem danh sách container:

```bash
docker ps -a
```

Start container Oracle:

```bash
docker start supermarket-oracle
```

Nếu tên container khác, thay `supermarket-oracle` bằng tên thật trong cột `NAMES`.

### Nếu chưa từng tạo container

Chạy Docker Compose tại thư mục project:

```bash
docker compose up -d
```

---

## 4. Hiểu port Docker Oracle

Thông thường Oracle Docker được map ra:

```text
localhost:1521
```

Nghĩa là app Java/DataGrip trên máy bạn sẽ connect vào:

```text
Host: localhost
Port: 1521
```

Nếu máy bạn đã có Oracle local chiếm port 1521, Docker có thể được map ra port 1522:

```text
Host: localhost
Port: 1522
```

Ví dụ trong `docker-compose.yml`:

```yaml
ports:
  - "1522:1521"
```

Ý nghĩa:

```text
Máy Windows dùng port 1522
Oracle bên trong Docker vẫn dùng port 1521
```

---

## 5. Mở DataGrip và tìm connection Oracle Docker local

Mở **DataGrip**.

Nhìn bên trái ở **Database Explorer**, tìm connection Oracle local.

Connection Docker local thường có dạng:

```text
Host: localhost
Port: 1521
```

hoặc:

```text
Host: localhost
Port: 1522
```

### Cách kiểm tra connection có đúng Docker local không

Click chuột phải vào connection:

```text
Properties
```

Kiểm tra các thông tin:

```text
Host
Port
User
Service name / SID
```

Nếu thấy:

```text
Host = localhost
Port = 1521 hoặc 1522
```

thì đó là database local trên máy bạn.

---

## 6. Tạo connection mới trong DataGrip nếu chưa có

Trong DataGrip:

```text
Database Explorer → dấu + → Data Source → Oracle
```

Nhập thông tin:

```text
Host: localhost
Port: 1521
User: system hoặc user project
Password: mật khẩu Oracle Docker
Service name: XEPDB1 hoặc XE hoặc FREEPDB1
```

Ví dụ nếu dùng Oracle Free Docker:

```text
Host: localhost
Port: 1521
Service name: FREEPDB1
User: system
Password: Admin123
```

Nếu máy bạn dùng port 1522:

```text
Host: localhost
Port: 1522
Service name: FREEPDB1
User: system
Password: Admin123
```

Bấm:

```text
Test Connection
```

Nếu báo thành công thì bấm **OK**.

### Nếu test connection lỗi service name

Thử lần lượt:

```text
FREEPDB1
XEPDB1
XE
```

Tùy image Oracle mà service name sẽ khác nhau.

---

## 7. Chạy file SQL patch/seed/schema trong DataGrip

Sau khi pull code, nếu có file SQL mới, ví dụ:

```text
database/patches/patch_002_add_customer_rank.sql
```

hoặc:

```text
database/05_session_and_login_history_patch.sql
```

thì làm như sau:

### Bước 1: Mở file SQL

Trong DataGrip hoặc IDE, mở file `.sql` cần chạy.

### Bước 2: Chọn đúng Data Source

Ở góc trên của editor SQL, DataGrip sẽ có chỗ chọn connection.

Nếu đang hiện:

```text
<no data source>
```

hoặc:

```text
<database>
```

thì bấm vào đó và chọn connection Oracle Docker local, ví dụ:

```text
@localhost
localhost:1521
localhost:1522
```

Cần đảm bảo chọn đúng database local của mình, không chạy nhầm vào database LAN hoặc database của người khác.

### Bước 3: Run file SQL

Có thể chạy bằng một trong các cách:

```text
Ctrl + A → Run
```

hoặc:

```text
Chuột phải vào file .sql → Run
```

hoặc bấm nút **Run/Execute** trên thanh công cụ.

### Lưu ý với PL/SQL block

Nếu file có dạng:

```sql
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD LAST_LOGIN_AT TIMESTAMP';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/
```

thì nên chạy **toàn bộ file**, không chỉ chạy từng dòng lẻ.

---

## 8. Luồng chuẩn cần nhớ

```text
git pull
    ↓
Có code mới và file SQL mới trong project
    ↓
Mở DataGrip
    ↓
Chọn đúng connection Oracle Docker local
    ↓
Run file SQL patch/seed/schema
    ↓
Database trong Docker local mới được cập nhật
    ↓
Clean and Build Java project
    ↓
Run app
```

---

## 9. Ví dụ cụ thể: thêm cột LAST_LOGIN_AT vào ACCOUNTS

Giả sử nhóm cần thêm cột mới để lưu thời gian đăng nhập cuối cùng.

Một bạn tạo file:

```text
database/patches/patch_005_add_last_login.sql
```

Nội dung:

```sql
ALTER TABLE ACCOUNTS ADD LAST_LOGIN_AT TIMESTAMP;
```

Bạn đó commit và push lên GitHub:

```bash
git add database/patches/patch_005_add_last_login.sql
git commit -m "add last login column patch"
git push origin main
```

Các thành viên khác làm:

```bash
git pull origin main
```

Sau khi pull, file patch đã nằm trong project, nhưng database Docker local **chưa có cột mới**.

Mỗi thành viên cần:

```text
1. Mở DataGrip
2. Chọn connection Oracle Docker local localhost:1521 hoặc localhost:1522
3. Mở file patch_005_add_last_login.sql
4. Run file SQL
5. Kiểm tra bảng ACCOUNTS đã có cột LAST_LOGIN_AT
```

Kiểm tra bằng SQL:

```sql
SELECT COLUMN_NAME, DATA_TYPE
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'ACCOUNTS'
  AND COLUMN_NAME = 'LAST_LOGIN_AT';
```

Nếu có kết quả là patch thành công.

---

## 10. Lỗi thường gặp và cách hiểu

### 10.1. Pull code xong nhưng database chưa đổi

Nguyên nhân:

```text
GitHub chỉ kéo file SQL về, chưa tự chạy SQL vào database.
```

Cách fix:

```text
Mở DataGrip và chạy file SQL patch mới.
```

---

### 10.2. Chạy nhầm connection

Ví dụ bạn chạy patch vào database LAN hoặc database cũ, còn app lại connect Docker local.

Hậu quả:

```text
Patch chạy thành công nhưng app vẫn lỗi.
```

Cách fix:

```text
Kiểm tra lại connection trong DataGrip.
Đảm bảo Host = localhost, Port = 1521 hoặc 1522 đúng với Docker local.
```

---

### 10.3. Java báo ORA-00904 invalid identifier

Ví dụ:

```text
ORA-00904: "LAST_LOGIN_AT": invalid identifier
ORA-00904: "ACTIVE_SESSIONS": invalid identifier
```

Nguyên nhân:

```text
Code Java đã dùng cột mới nhưng database local chưa được patch.
```

Cách fix:

```text
Chạy file patch SQL tương ứng trong DataGrip.
```

---

### 10.4. ALTER TABLE ADD bị lỗi vì cột đã tồn tại

Ví dụ:

```text
ORA-01430: column being added already exists in table
```

Nguyên nhân:

```text
Bạn đã chạy patch đó trước đó rồi.
```

Cách xử lý:

```text
Nếu chắc chắn cột đã tồn tại thì có thể bỏ qua.
```

Nên viết patch dạng an toàn:

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

Patch dạng này chạy nhiều lần vẫn không làm hỏng database.

---

### 10.5. DataGrip không connect được Oracle Docker

Kiểm tra Docker:

```bash
docker ps
```

Nếu container chưa chạy:

```bash
docker start supermarket-oracle
```

Nếu vẫn không được, kiểm tra port đang map:

```bash
docker ps
```

Xem cột `PORTS`, ví dụ:

```text
0.0.0.0:1521->1521/tcp
```

hoặc:

```text
0.0.0.0:1522->1521/tcp
```

Sau đó dùng đúng port trong DataGrip.

---

### 10.6. Sai Service name

Nếu DataGrip báo lỗi service, thử các service name phổ biến:

```text
FREEPDB1
XEPDB1
XE
```

Với Docker image `gvenzl/oracle-free`, service thường là:

```text
FREEPDB1
```

---

## 11. Quy tắc làm việc nhóm

Khi có thay đổi database, không sửa trực tiếp trên máy rồi im luôn. Cần tạo file SQL patch và push lên GitHub.

Quy trình đúng:

```text
1. Một bạn thay đổi schema/data mẫu.
2. Tạo file SQL patch/seed trong thư mục database/.
3. Commit và push file SQL lên GitHub.
4. Các thành viên git pull.
5. Mỗi người tự chạy file SQL đó vào Docker Oracle local bằng DataGrip.
```

Không nên chỉ nói miệng:

```text
Tôi thêm cột rồi, mọi người tự thêm nha.
```

Mà nên có file rõ ràng:

```text
database/patches/patch_005_add_last_login.sql
```

---

## 12. Tóm tắt ngắn gọn

```text
GitHub = nơi lưu code và file SQL.
Docker Oracle = nơi chạy database local của từng người.
DataGrip = công cụ để chạy SQL vào Docker Oracle.
```

Nhớ kỹ:

```text
git pull chỉ cập nhật project.
git pull không cập nhật database.
Muốn database cập nhật thì phải chạy SQL trong DataGrip.
```
