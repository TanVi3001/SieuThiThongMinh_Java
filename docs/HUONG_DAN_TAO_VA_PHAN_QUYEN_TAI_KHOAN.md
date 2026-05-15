# HƯỚNG DẪN CẤP PHÁT & KÍCH HOẠT TÀI KHOẢN NHÂN VIÊN
**Hệ thống Quản lý Siêu thị thông minh (Smart Supermarket)**

Quy trình quản lý nhân sự được thiết kế bảo mật chặt chẽ qua 2 giai đoạn: **Quản lý tạo hồ sơ cấp mã** và **Nhân viên tự thiết lập tài khoản**. Để hệ thống gửi mail tự động hoạt động, cần cấu hình Email Service trước khi sử dụng.

---

## Phần 1: Cấu hình hệ thống Gửi Mail tự động (Dành cho Developer/Admin)
Để hệ thống có thể tự động gửi Mã kích hoạt qua email của nhân viên mới, bạn cần cấu hình Mật khẩu ứng dụng (App Password) từ Google.

### Bước 1: Lấy Mã Mật khẩu ứng dụng Google
1. Truy cập vào tài khoản Google (Gmail) mà bạn muốn dùng làm email gửi tự động của siêu thị.
2. Đi tới **Quản lý tài khoản Google** -> Chọn tab **Bảo mật (Security)**.
3. Đảm bảo **Xác minh 2 bước (2-Step Verification)** đã được BẬT.
4. Tìm kiếm từ khóa `Mật khẩu ứng dụng` (App passwords) trên thanh tìm kiếm của Google Account.
5. Tại mục Ứng dụng, chọn "Khác (Tên tùy chỉnh)" và nhập tên (VD: `SmartSupermarket`).
6. Nhấn **Tạo**. Google sẽ cấp cho bạn một đoạn mã gồm 16 chữ cái (VD: `abcd efgh ijkl mnop`). Hãy copy đoạn mã này (bỏ các dấu cách).

### Bước 2: Dán mã vào Hệ thống
1. Mở Project trong NetBeans, tìm đến file `business.service.EmailService.java`.
2. Tìm đến phần cấu hình tài khoản gửi (thường nằm ở đầu class hoặc trong hàm `sendEmail`).
3. Thay thế email và mật khẩu bằng thông tin của bạn:
```java
// Ví dụ cấu hình trong EmailService.java
final String username = "emailcuaban@gmail.com"; // Điền email của bạn
final String password = "abcdefghijklmnop";    // Dán 16 ký tự mã ứng dụng vào đây (không có khoảng trắng)
```

## Phần 2: Tạo Hồ sơ Nhân viên & Phân Quyền (Dành cho Quản lý)
*Lưu ý: Quản lý cửa hàng (Manager) chỉ được phép tạo và phân quyền cho nhân viên cấp dưới.*

1. **Đăng nhập** vào hệ thống bằng tài khoản Quản lý (Ví dụ: `Le_Tan_Vi_Store_Manager`).
2. Điều hướng đến menu **Quản lý nhân viên** -> Chọn tab **Hồ sơ nhân viên**.
3. Điền đầy đủ thông tin cá nhân của nhân viên mới:
   * **Họ và tên:** Tên đầy đủ của nhân viên.
   * **Số điện thoại:** Dùng để liên lạc nội bộ.
   * **Email:** Bắt buộc sử dụng định dạng `@gmail.com` hoặc `@gm.uit.edu.vn` *(Hệ thống sẽ gửi mã kích hoạt về email này)*.
   * **Phân quyền (Role ID):** Mở dropdown và chọn 1 trong 2 quyền cho phép:
     * `R_STAFF_SALE`: Quyền Nhân viên bán hàng (Được thao tác thanh toán, tạo hóa đơn).
     * `R_STAFF_VIEW_PROD`: Quyền Xem sản phẩm (Chỉ tra cứu thông tin kho, không được bán).
4. Bấm nút **[ + Thêm hồ sơ ]**.
5. Hệ thống sẽ lưu dữ liệu vào CSDL, đồng thời tự động gửi một Email chứa **Mã kích hoạt** (Mã Nhân viên - `EMP...`) đến hòm thư của nhân viên đó.

---

## Phần 3: Kích hoạt Tài khoản (Dành cho Nhân viên mới)
Sau khi Quản lý tạo hồ sơ thành công, nhân viên mới tự thực hiện các bước sau để lấy quyền truy cập:

1. **Kiểm tra Email:** Mở hộp thư cá nhân đã đăng ký, tìm email từ "Smart Supermarket" và copy **Mã kích hoạt** (VD: `EMP1777747188184`).
2. Mở ứng dụng Smart Supermarket. Tại màn hình Đăng nhập, nhấp vào dòng chữ: **"Chưa có tài khoản? Đăng ký"** ở góc dưới cùng.
3. **Giai đoạn 1 - Xác thực:**
   * Dán mã kích hoạt vào ô **MÃ KÍCH HOẠT (*)**.
   * Nhấn nút **[ KIỂM TRA MÃ ]**.
   * Hệ thống sẽ đối chiếu mã với Database. Nếu hợp lệ, chuyển sang Giai đoạn 2.
4. **Giai đoạn 2 - Thiết lập thông tin bảo mật:**
   * Màn hình sẽ mở khóa và tự động điền (pre-fill) các thông tin cá nhân (Tên, Email, SĐT) mà Quản lý đã khai báo.
   * Nhân viên tiến hành nhập **Tên đăng nhập** mong muốn và tạo **Mật khẩu mới**.
5. Nhấn nút **[ HOÀN TẤT KÍCH HOẠT ]**.
6. Hệ thống sẽ băm (hash) mật khẩu bằng thuật toán `BCrypt` và lưu vào Database. Tài khoản kích hoạt thành công, nhân viên có thể sử dụng Username/Password vừa tạo để Đăng nhập và làm việc ngay lập tức.
