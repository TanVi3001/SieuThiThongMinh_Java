# Hướng Dẫn Cấu Hình Kết Nối Oracle Database & Real-time (Mạng LAN)

**Dự án:** Smart Supermarket - Store Portal
**Công cụ:** Oracle Database, DataGrip, Java Swing, WebSocket

---

## GIỚI THIỆU

Tài liệu này hướng dẫn cách:

* Kết nối Oracle bằng DataGrip
* Setup database
* Cấu hình Java để chạy realtime
* Kết nối nhiều máy qua LAN

---

# PHẦN 1: CÀI ĐẶT DATABASE (DATAGRIP)

## 1. Tạo Connection

* Mở DataGrip
* Nhấn `+` → **Data Source → Oracle**

### Nhập thông tin:

* **Host:**

  * `localhost` (máy chủ)
  * hoặc `IP LAN` (VD: `10.0.23x.4x`)

* **Port:** `1521`

* **Service Name:** `orcl`

* **User/Password:** ví dụ `system / Admin123`

👉 Nhấn **Test Connection** → OK

---

## 2. Chạy Script

### Tạo bảng:

```
database/KhoiTaoCacBang.sql
```

### Seed data:

```
database/seed_data.sql
```

---

# PHẦN 2: CẤU HÌNH JAVA

## QUY TẮC

* 1 máy → **HOST**
* Máy còn lại → **CLIENT**

---

## 1. Database Connection (TẤT CẢ MÁY)

File:

```
src/main/java/common/db/DatabaseConnection.java
```

```java
// LOCAL
private static final String url = "jdbc:oracle:thin:@localhost:1521:orcl";

// LAN
private static final String url = "jdbc:oracle:thin:@10.0.23x.4x:1521:orcl";

private static final String USER = "your_oracle_user";
private static final String PASS = "your_oracle_password";
```

---

## 2. File Main

File:

```
src/main/java/business/main/SieuThiOnline.java
```

```java
String serverIp = "ws://10.0.23x.4x:9999";
```

---

## 3. WebSocket Server (MÁY CHỦ)

File:

```
src/main/java/common/realtime/RealtimeServer.java
```

```java
InetSocketAddress address = new InetSocketAddress("0.0.0.0", 9999);
RealtimeServer server = new RealtimeServer(address);
server.start();
```

👉 `0.0.0.0` = cho phép toàn LAN connect

---

## 4. WebSocket Client (MÁY TRẠM)

File:

```
src/main/java/common/realtime/RealtimeClient.java
```

```java
// LOCAL
private static final String WS_URL = "ws://localhost:9999";

// LAN
private static final String WS_URL = "ws://10.0.23x.4x:9999";
```

---

# PHẦN 3: FIX LỖI LAN

## 1. Tắt Firewall (MÁY CHỦ)

* Mở `Windows Defender Firewall`
* Chọn:

  * Turn off Private
  * Turn off Public

👉 Nhớ bật lại sau demo

---

## 2. Mở Oracle Listener

File:

```
C:\app\...\network\admin\listener.ora
```

Sửa:

```
HOST = localhost
```

→ thành:

```
HOST = 0.0.0.0
```

---

## 3. Restart Service

* Mở:

```
services.msc
```

* Tìm:

```
OracleXETNSListener
```

→ Restart

---

# TỔNG KẾT

| Thành phần       | Máy chủ | Máy trạm |
| ---------------- | ------- | -------- |
| Oracle DB        | ✅       | ❌        |
| WebSocket Server | ✅       | ❌        |
| WebSocket Client | ✅       | ✅        |
| DataGrip         | ✅       | ✅        |

---

# LƯU Ý

* Tất cả máy cùng Wi-Fi
* IP phải trỏ về máy chủ
* Port:

  * `1521` → Oracle
  * `9999` → Realtime

---

🔥 Copy file này vào `HuongDanKetNoi.md` là chạy được ngay.
