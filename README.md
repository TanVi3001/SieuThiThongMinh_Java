<p align="center"><img width="454" height="126" alt="image" src="https://github.com/user-attachments/assets/2036c003-62d1-42f1-9817-6cca86de0fc8" /> </p>

## [GIỚI THIỆU ĐỒ ÁN](#)

* **Đề tài:** Xây dựng phần mềm quản lý Siêu thị Thông Minh(Smart Supermarket - Store Portal)
* **Repository:** [LẬP TRÌNH JAVA - SIÊU THỊ THÔNG MINH](https://github.com/TanVi3001/SieuThiOnline_Java)
* **Mô tả tổng quan:** Đề tài "Xây dựng Hệ thống Quản trị Siêu thị Thông Minh" là một dự án phần mềm Desktop toàn diện, được thiết kế chuyên biệt để giải quyết bài toán vận hành của mô hình bán lẻ trực tuyến hiện đại. Khác với các hệ thống POS quét mã vạch truyền thống tại quầy, hệ thống này đóng vai trò là một **Cổng thông tin điều phối (Store Portal)** tập trung. Phần mềm hỗ trợ đắc lực cho bộ phận quản lý, nhân viên Telesale và bộ phận kho xử lý đồng bộ các khâu: Tiếp nhận đơn hàng trực tuyến, Quản lý tồn kho đa đơn vị tính, Chăm sóc khách hàng và Quản trị nhân sự.
Điểm sáng giá nhất của dự án là việc áp dụng kiến trúc **N-Tier** chuẩn mực kết hợp với hệ quản trị cơ sở dữ liệu **Oracle**. Đặc biệt, hệ thống tích hợp công nghệ **đồng bộ dữ liệu thời gian thực (Real-time)** qua mạng LAN bằng WebSocket, giúp mọi thay đổi về số lượng tồn kho hay doanh thu đều được cập nhật chớp nhoáng trên toàn bộ các máy trạm tham gia hệ thống mà không cần người dùng thao tác tải lại trang.

## [CÁC TÍNH NĂNG VÀ NGHIỆP VỤ NỔI BẬT](#)
Hệ thống được chia thành các phân hệ lõi với hàm lượng kỹ thuật cao:
* **[Bán hàng & Xử lý Đơn hàng trực tuyến (Telesale & Order Management):](#)** Xây dựng luồng tạo đơn hàng nội bộ tốc độ cao với thanh tìm kiếm thông minh thay vì quét mã. Tích hợp giỏ hàng, tính toán giảm giá (Promotion), đa dạng hình thức thanh toán và tối ưu cho luồng Online với các tùy chọn giao nhận: Giao tận nơi (Home Delivery) hoặc Khách lấy tại quầy (Store Pickup). Toàn bộ giao dịch (Transaction) được đảm bảo tính toàn vẹn dữ liệu nghiêm ngặt.
* **[Quản lý Kho & Sản phẩm (Inventory & Product):](#)** Hỗ trợ cấu hình tỷ lệ quy đổi Đơn vị tính (Unit of Measure) linh hoạt cho từng mặt hàng. Cung cấp công cụ Import/Export dữ liệu hàng loạt thông qua file CSV/Excel và tự động cảnh báo tồn kho.
* **[Bảo mật & Phân quyền chuẩn Doanh nghiệp (RBAC Security):](#)** Triển khai cơ chế phân quyền RBAC (Role-Based Access Control) chặt chẽ, ẩn/hiện giao diện và tính năng dựa trên cấp bậc của nhân viên. Tích hợp hệ thống mã hóa mật khẩu bảo mật, kiểm soát phiên đăng nhập bằng Timer. Đặc biệt, tự động hóa việc cấp phát tài khoản bằng cách gửi Mã kích hoạt qua Email sử dụng luồng chạy ngầm (Background Thread) với JavaMail API.
* **[Quản trị Nhân sự & Khách hàng (HR & Customer Management):](#)** Quản lý hồ sơ nhân viên, phân quyền phòng ban. Theo dõi thông tin liên hệ và lịch sử của khách hàng, tích hợp tính năng tự động điền (Auto-complete) để tăng tốc độ tạo đơn cho nhân viên Telesale.
* **[Dashboard & Thống kê Thời gian thực:](#)** Giao diện tổng quan trực quan với các chỉ số hoạt động kinh doanh (Doanh thu, Đơn hàng, Tồn kho). Ứng dụng mô hình hướng sự kiện (Event-Driven) để Dashboard tự động lắng nghe và nhảy số liệu ngay lập tức khi có bất kỳ giao dịch nào phát sinh từ các máy trạm khác.

## [CÔNG NGHỆ VÀ CÔNG CỤ SỬ DỤNG](#)

**Application Development**
* [Java](https://www.java.com/) (JDK 11+) - Ngôn ngữ lập trình chính 
* [Java Swing](https://docs.oracle.com/javase/tutorial/uiswing/) - Thư viện xây dựng giao diện người dùng (GUI)
* [N-Tier Architecture](https://en.wikipedia.org/wiki/Multitier_architecture) - Kiến trúc phân tầng dự án (common, business, model, view, controller)

**Database & Tools**
* [Oracle](https://www.oracle.com/) - Hệ quản trị cơ sở dữ liệu 
* [JDBC](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) - Công cụ kết nối và truy cập dữ liệu
* [NetBeans](https://netbeans.apache.org/) - IDE phát triển chính
* [GitHub](https://github.com/) - Quản lý mã nguồn và làm việc nhóm

**Third-party Services**
* [Apache POI](https://poi.apache.org/) - Xử lý và xuất báo cáo file Excel (.xlsx)
* [iText 7](https://itextpdf.com/products/itext-7) - Công cụ xuất báo cáo/hóa đơn định dạng PDF (Unicode)

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
