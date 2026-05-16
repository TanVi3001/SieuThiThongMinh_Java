<p align="center"><img width="454" height="126" alt="image" src="https://github.com/user-attachments/assets/2036c003-62d1-42f1-9817-6cca86de0fc8" /> </p>

## [GIỚI THIỆU ĐỒ ÁN](#)

* **Đề tài:** Xây dựng phần mềm quản lý Siêu thị Thông Minh(Smart Supermarket - Store Portal)
* **Repository:** [LẬP TRÌNH JAVA - SIÊU THỊ THÔNG MINH](https://github.com/TanVi3001/SieuThiThongMinh_Java)
* **Mô tả tổng quan:** Đề tài "Xây dựng Hệ thống Quản trị Siêu thị Thông Minh" là một dự án phần mềm Desktop toàn diện, được thiết kế chuyên biệt để giải quyết bài toán vận hành của mô hình bán lẻ trực tuyến hiện đại. Khác với các hệ thống POS quét mã vạch truyền thống tại quầy, hệ thống này đóng vai trò là một **Cổng thông tin điều phối (Store Portal)** tập trung, hỗ trợ bộ phận quản lý, nhân viên bán hàng, nhân viên kho và quản trị viên xử lý đồng bộ các nghiệp vụ: tiếp nhận đơn hàng, quản lý sản phẩm, kiểm soát tồn kho, chăm sóc khách hàng, phân quyền tài khoản, theo dõi hiệu suất nhân viên và lập báo cáo doanh thu.

* Điểm nổi bật của dự án là việc áp dụng kiến trúc **N-Tier** kết hợp với hệ quản trị cơ sở dữ liệu **Oracle Database**. Hệ thống được tổ chức theo các tầng rõ ràng gồm giao diện Swing, tầng xử lý nghiệp vụ, tầng truy vấn dữ liệu và tầng mô hình dữ liệu, giúp mã nguồn dễ bảo trì và mở rộng. Ngoài ra, dự án tích hợp **đồng bộ dữ liệu thời gian thực (Real-time)** qua WebSocket trong mạng LAN, giúp các thay đổi về tồn kho, đơn hàng, hóa đơn, tài khoản và doanh thu được cập nhật nhanh trên nhiều máy trạm mà không cần thao tác tải lại thủ công.

* Bên cạnh chức năng nghiệp vụ chính, hệ thống còn được bổ sung các công cụ hỗ trợ triển khai và làm việc nhóm như **Docker Oracle** để chuẩn hóa môi trường database, **DataGrip** để quản trị và chạy script SQL, **GitHub** để quản lý source code, file patch database và tài liệu, cùng cơ chế backup/restore bằng file `.dmp` phục vụ demo và nộp đồ án. Nhờ đó, dự án không chỉ tập trung vào giao diện quản lý mà còn mô phỏng gần hơn quy trình phát triển phần mềm thực tế trong môi trường nhóm.

## [CÁC TÍNH NĂNG VÀ NGHIỆP VỤ NỔI BẬT](#)
Hệ thống được chia thành các phân hệ lõi với hàm lượng kỹ thuật cao:
* **[Bán hàng & Xử lý Đơn hàng trực tuyến (Telesale & Order Management):](#)** Xây dựng luồng tạo đơn hàng nội bộ tốc độ cao với thanh tìm kiếm thông minh thay vì quét mã. Tích hợp giỏ hàng, tính toán khuyến mãi (Promotion), nhiều hình thức thanh toán và tối ưu cho luồng bán hàng online với các tùy chọn giao nhận như giao tận nơi (Home Delivery) hoặc khách lấy tại quầy (Store Pickup). Toàn bộ quy trình thanh toán và cập nhật tồn kho được xử lý theo hướng đảm bảo tính nhất quán dữ liệu.
* **[Quản lý Kho & Sản phẩm (Inventory & Product):](#)** Hỗ trợ quản lý danh mục, sản phẩm, nhà cung cấp, tồn kho và trạng thái hàng hóa. Hệ thống có khả năng theo dõi số lượng tồn, cảnh báo tồn kho, hỗ trợ import/export dữ liệu bằng Excel/CSV và phục vụ các nghiệp vụ kiểm soát kho trong môi trường bán lẻ.
* **[Bảo mật & Phân quyền chuẩn Doanh nghiệp (RBAC Security):](#)** Triển khai cơ chế phân quyền RBAC (Role-Based Access Control) theo vai trò như Admin, Manager, Staff và Warehouse Staff. Giao diện và chức năng được ẩn/hiện theo quyền hạn người dùng. Hệ thống sử dụng mã hóa mật khẩu bằng BCrypt, kiểm soát phiên đăng nhập, trạng thái online/offline, hỗ trợ nhiều phiên cho Admin/Manager và ghi nhận lịch sử đăng nhập để tăng tính an toàn khi vận hành.
* **[Quản trị Nhân sự & Khách hàng (HR & Customer Management):](#)** Quản lý hồ sơ nhân viên, phân quyền phòng ban, cấp phát và kích hoạt tài khoản nhân viên qua email. Đồng thời theo dõi thông tin khách hàng, điểm thưởng, hạng thành viên, lịch sử mua hàng và hỗ trợ tự động điền thông tin để tăng tốc độ tạo đơn cho nhân viên.
* **[Dashboard & Thống kê Thời gian thực:](#)** Giao diện báo cáo được thiết kế theo phong cách dashboard hiện đại, hiển thị các chỉ số như doanh thu, đơn hàng, hiệu suất nhân viên, KPI và xu hướng kinh doanh. Ứng dụng mô hình hướng sự kiện (Event-Driven) để Dashboard tự động lắng nghe thay đổi dữ liệu và cập nhật số liệu theo thời gian thực.
* **[Backup, Restore & Đồng bộ Database Demo:](#)** Dự án hỗ trợ quy trình backup/restore Oracle bằng Data Pump (`expdp/impdp`), giúp nhóm tạo bản dữ liệu demo hoàn chỉnh để phục vụ báo cáo, nộp module hoặc khôi phục nhanh database khi cần. Các file SQL patch và script cập nhật database được quản lý trong repository để các thành viên dễ đồng bộ sau khi pull code.

## [CÔNG NGHỆ VÀ CÔNG CỤ SỬ DỤNG](#)

**Application Development**
* [Java](https://www.java.com/) (JDK 21) - Ngôn ngữ lập trình chính cho ứng dụng Desktop
* [Java Swing](https://docs.oracle.com/javase/tutorial/uiswing/) - Thư viện xây dựng giao diện người dùng (GUI)
* [FlatLaf](https://www.formdev.com/flatlaf/) - Giao diện hiện đại cho ứng dụng Java Swing
* [N-Tier Architecture](https://en.wikipedia.org/wiki/Multitier_architecture) - Kiến trúc phân tầng dự án (common, business, model, view, controller)
* [Maven](https://maven.apache.org/) - Quản lý dependencies, build và chạy project

**Database & Tools**
* [Oracle Database](https://www.oracle.com/database/) - Hệ quản trị cơ sở dữ liệu chính
* [JDBC](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) - Công cụ kết nối và truy cập dữ liệu Oracle từ Java
* [Docker](https://www.docker.com/) - Đóng gói Oracle Database thành môi trường local thống nhất cho các thành viên
* [Docker Compose](https://docs.docker.com/compose/) - Khởi tạo và quản lý Oracle container bằng file cấu hình `docker-compose.yml`
* [DataGrip](https://www.jetbrains.com/datagrip/) - Công cụ quản trị database, chạy schema/seed/patch SQL và kiểm tra dữ liệu
* [Oracle Data Pump](https://docs.oracle.com/en/database/) - Export/Import database demo thông qua `expdp` và `impdp`
* [NetBeans](https://netbeans.apache.org/) - IDE phát triển chính, hỗ trợ Java Swing Form Designer
* [GitHub](https://github.com/) - Quản lý mã nguồn, pull request, tài liệu và các file SQL patch dùng chung cho nhóm

**Third-party Services**
* [Apache POI](https://poi.apache.org/) - Xử lý và xuất báo cáo file Excel (.xlsx)
* [iText 7](https://itextpdf.com/products/itext-7) - Công cụ xuất báo cáo/hóa đơn định dạng PDF (Unicode)
* [JavaMail API](https://javaee.github.io/javamail/) - Gửi email kích hoạt tài khoản nhân viên
* [jBCrypt](https://www.mindrot.org/projects/jBCrypt/) - Mã hóa và kiểm tra mật khẩu người dùng
* [WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API) - Đồng bộ dữ liệu thời gian thực giữa các máy trong mạng LAN

## [CẤU TRÚC DỰ ÁN](#)

```text
SieuThiOnline [Project Root]
├── src/main/java
│   ├── business.main
│   │   └── SieuThiOnline.java       <-- File chạy thử nghiệm & Main App
│   ├── business.service
│   │   └── PaymentService.java      <-- Xử lý Logic (Transaction, tính toán)
│   ├── business.sql
│   │   ├── SqlInterface.java        <-- "BẢN HIẾN PHÁP" CHUNG (Duy nhất ở đây)
│   │   ├── prod_inventory           <-- Phân hệ Kho & Sản phẩm
│   │   │   ├── CategoriesSql.java
│   │   │   ├── InventorySql.java
│   │   │   ├── ProductsSql.java
│   │   │   └── SuppliersSql.java
│   │   ├── rbac                     <-- Phân hệ Phân quyền & Tài khoản
│   │   │   ├── AccountSql.java
│   │   │   ├── FunctionsSql.java
│   │   │   └── ...
│   │   └── sales_order              <-- Phân hệ Đơn hàng & Khách hàng
│   │       ├── CustomersSql.java
│   │       ├── OrdersSql.java
│   │       └── OrderDetailsSql.java
│   ├── common.db
│   │   └── DatabaseConnection.java  <-- Kết nối Oracle (Singleton)
│   ├── common.report
│   │   └── ExcelExporter.java       <-- Module xuất Excel
│   └── model                        <-- Chứa các thực thể (POJO)
│       ├── Category.java
│       ├── Product.java
│       ├── Supplier.java
│       └── ...
├── database                         <-- Schema, seed, patch SQL và script cập nhật database
├── backup                           <-- File dump database demo dùng cho restore khi báo cáo/nộp bài
├── docs                             <-- Tài liệu hướng dẫn Git, Docker, DataGrip, backup và workflow nhóm
├── docker-compose.yml               <-- Cấu hình Oracle Docker local
├── .gitignore                       <-- Chặn file rác 
└── README.md                        <-- Hướng dẫn dự án
```
## [THÀNH VIÊN NHÓM](#)

| STT | MSSV | Họ và Tên | GitHub | Email | 
| :--- | :--- | :--- | :--- | :--- | 
| 1 | 24521985 | Lê Tấn Vĩ | https://github.com/TanVi3001 | 24521985@gm.uit.edu.vn |
| 2 | 24521949 | Nguyễn Đinh Tùng | https://github.com/DeeTung | 24521949@gm.uit.edu.vn |
| 3 | 24521176 | Hoàng Khôi Nguyên | https://github.com/Paulhoang8326 | 24521176@gm.uit.edu.vn | 
| 4 | 24521507 | Dương Thúy Quỳnh | https://github.com/duongthuyquynh | 24521507@gm.uit.edu.vn | 


