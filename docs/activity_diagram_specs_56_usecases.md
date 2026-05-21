# TÀI LIỆU ĐẶC TẢ ACTIVITY DIAGRAM CHO 56 USE-CASES

Tài liệu này cung cấp thông tin đầu vào chi tiết, được chuẩn hóa theo đúng cấu trúc nghiệp vụ tiếng Việt và ánh xạ chính xác với mã nguồn Java Swing và Cơ sở dữ liệu Oracle thực tế của dự án Smart Supermarket - Store Portal.

## Nguyên tắc thiết kế sơ đồ hoạt động (Activity Diagram):
1. **Phân chia 3 Swimlanes**: Người dùng | Hệ thống | Cơ sở dữ liệu.
2. **Số lượng Action Nodes**: Giới hạn từ 8 đến 14 action nodes cho mỗi sơ đồ để đảm bảo tính trực quan và đơn giản, tập trung vào nghiệp vụ chính.
3. **Giới hạn Decision Nodes**: Tối đa 2 decision nodes chính để kiểm tra dữ liệu và quyền truy cập.
4. **Lane Cơ sở dữ liệu**: Chỉ hiển thị từ 1 đến 2 bảng CSDL chính thực hiện lưu trữ hoặc cập nhật dữ liệu. Các bảng đọc tham chiếu/join được phân loại là bảng thứ cấp (Secondary) và ẩn đi.
5. **Ngôn ngữ nghiệp vụ**: Hoàn toàn bằng tiếng Việt nghiệp vụ, không đưa các thuật ngữ kỹ thuật sâu như (ResultSet, BCrypt, token, session, Java method) vào sơ đồ.

---

--------------------------------------------------
## UC02 / 3.2.2 - Đăng xuất tài khoản

### 1. Mục tiêu ngắn gọn
Cho phép người dùng kết thúc phiên làm việc và thoát khỏi hệ thống.

### 2. Tác nhân chính
- Admin; Quản lý cửa hàng; Nhân viên bán hàng / Thu ngân; Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [Sidebar / AdminSidebar (Nút Đăng xuất)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/Sidebar / AdminSidebar.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- LOGIN_HISTORY
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- TOKENS: thu hồi token hiện tại

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn nút Đăng xuất trên thanh Sidebar
- Xác nhận đăng xuất tại hộp thoại cảnh báo

Swimlane: Hệ thống
- Hiển thị hộp thoại xác nhận đăng xuất
- Thực hiện hủy thông tin phiên làm việc hiện tại
- Quay về màn hình Đăng nhập và hiển thị thông báo kết thúc phiên

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Cập nhật trạng thái online_status thành ngoại tuyến
- LOGIN_HISTORY: Ghi nhận thời điểm đăng xuất vào nhật ký hệ thống
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện Sidebar / AdminSidebar.

2. Hệ thống: Hiển thị yêu cầu xác nhận kết thúc phiên.

3. Người dùng: Xác nhận đăng xuất.

4. Hệ thống: Gọi AccountSql để hủy thông tin phiên.

5. Cơ sở dữ liệu: Cập nhật trạng thái trong ACCOUNTS và ghi log vào LOGIN_HISTORY.

6. Hệ thống: Điều hướng người dùng về giao diện đăng nhập và thông báo đăng xuất thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + LOGIN_HISTORY, ACCOUNTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: Sidebar và AdminSidebar chứa action listener xử lý đăng xuất, gọi AccountSql để cập nhật online_status và LoginHistorySql để ghi log.

--------------------------------------------------
## UC03 / 3.2.3 - Kích hoạt tài khoản nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép nhân viên dùng mã kích hoạt để thiết lập tài khoản đăng nhập lần đầu.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [RegisterView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/RegisterView.java)
- DAO: [ActivationTokenSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/ActivationTokenSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACTIVATION_TOKENS
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- EMPLOYEES: kiểm tra email/id nhân viên
- USERS: kiểm tra liên kết

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở màn hình Kích hoạt tài khoản nhân viên
- Nhập Email, số điện thoại và Mã kích hoạt
- Thiết lập mật khẩu đăng nhập ban đầu
- Nhấn Xác nhận kích hoạt tài khoản

Swimlane: Hệ thống
- Hiển thị màn hình kích hoạt tài khoản
- Kiểm tra tính hợp lệ và thời hạn của Mã kích hoạt
- Kiểm tra mật khẩu có đáp ứng độ dài tối thiểu
- Lưu thông tin và hiển thị thông báo kích hoạt thành công

Swimlane: Cơ sở dữ liệu
- ACTIVATION_TOKENS: Cập nhật cột USED_AT và trạng thái của mã kích hoạt
- ACCOUNTS: Cập nhật mật khẩu mới, kích hoạt trạng thái tài khoản hoạt động
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện RegisterView.

2. Người dùng: Nhập mã kích hoạt và mật khẩu đăng nhập ban đầu.

3. Hệ thống: Kiểm tra tính hợp lệ của mã kích hoạt.

Decision 1:
- Điều kiện: Mã kích hoạt không hợp lệ hoặc hết hạn?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và dừng xử lý.
- Nếu Không: Đi tiếp bước tiếp theo.

4. Hệ thống: Xác thực mật khẩu mới đáp ứng tiêu chuẩn an toàn.

5. Cơ sở dữ liệu: Cập nhật thông tin tài khoản trong ACCOUNTS và đánh dấu mã đã dùng trong ACTIVATION_TOKENS.

6. Hệ thống: Thông báo kích hoạt tài khoản thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACTIVATION_TOKENS, ACCOUNTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: RegisterView xử lý đăng ký tài khoản mới bằng mã kích hoạt (activation code) lấy từ ACTIVATION_TOKENS.

--------------------------------------------------
## UC04 / 3.2.4 - Khôi phục mật khẩu

### 1. Mục tiêu ngắn gọn
Cho phép người dùng nhận OTP qua email và đặt lại mật khẩu khi quên thông tin đăng nhập.

### 2. Tác nhân chính
- Admin; Quản lý cửa hàng; Nhân viên bán hàng / Thu ngân; Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ForgotPasswordView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ForgotPasswordView.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- OTP_STORAGE
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở màn hình Khôi phục mật khẩu
- Nhập Email tài khoản cần khôi phục và gửi yêu cầu OTP
- Nhập mã OTP nhận được qua Email và mật khẩu mới
- Xác nhận thay đổi mật khẩu

Swimlane: Hệ thống
- Hiển thị giao diện khôi phục mật khẩu
- Kiểm tra thông tin tài khoản liên kết với Email
- Gửi OTP qua email và lưu vào bộ nhớ tạm
- Kiểm tra tính hợp lệ của OTP và độ mạnh mật khẩu mới
- Thông báo cập nhật mật khẩu thành công

Swimlane: Cơ sở dữ liệu
- OTP_STORAGE: Lưu thông tin mã OTP, thời gian hết hạn
- ACCOUNTS: Cập nhật mật khẩu mới và làm sạch bộ nhớ tạm OTP
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ForgotPasswordView.

2. Người dùng: Nhập Email khôi phục mật khẩu.

3. Hệ thống: Gửi mã OTP về email của người dùng.

4. Cơ sở dữ liệu: Lưu mã OTP và thời gian hết hạn vào OTP_STORAGE.

5. Người dùng: Nhập mã OTP nhận được và đặt mật khẩu mới.

Decision 1:
- Điều kiện: OTP sai hoặc hết hạn?
- Nếu Có: Hệ thống: Thông báo OTP không hợp lệ.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Cập nhật mật khẩu mới trong ACCOUNTS.

7. Hệ thống: Thông báo khôi phục mật khẩu thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + OTP_STORAGE, ACCOUNTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ForgotPasswordView gửi OTP qua email, lưu vào OTP_STORAGE, kiểm tra và cập nhật mật khẩu mới trong ACCOUNTS.

--------------------------------------------------
## UC05 / 3.2.5 - Quản lý tài khoản và phân quyền

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng quản lý tài khoản, vai trò và quyền truy cập trong hệ thống.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [LoginManagementPanel, AccountRoleAssignmentPanel, RoleManagementPanel, CreateAccountPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/LoginManagementPanel, AccountRoleAssignmentPanel, RoleManagementPanel, CreateAccountPanel.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- ROLES: hiển thị danh sách quyền
- ROLE_GROUPS: hiển thị danh sách nhóm quyền

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện LoginManagementPanel, AccountRoleAssignmentPanel, RoleManagementPanel, CreateAccountPanel.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACCOUNTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: LoginManagementPanel và các panel liên quan hiển thị danh sách tài khoản, hỗ trợ mở các dialog tạo, sửa, phân quyền.

--------------------------------------------------
## UC06 / 3.2.6 - Thêm tài khoản nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Admin tạo tài khoản mới cho nhân viên nội bộ hoặc cấp tài khoản từ hồ sơ nhân viên.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CreateAccountPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CreateAccountPanel.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS
- USERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- EMPLOYEES: liên kết thông tin nhân viên

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Nhấn nút chức năng Thêm mới
- Nhập đầy đủ thông tin vào biểu mẫu hiển thị
- Nhấn Xác nhận lưu

Swimlane: Hệ thống
- Hiển thị biểu mẫu nhập thông tin
- Kiểm tra dữ liệu bắt buộc và các ràng buộc nghiệp vụ (trùng lặp, định dạng)
- Tạo mới bản ghi nghiệp vụ
- Làm mới danh sách hiển thị và thông báo thành công

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Thêm mới bản ghi vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CreateAccountPanel.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng ACCOUNTS, USERS.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACCOUNTS, USERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CreateAccountPanel cho phép chọn hồ sơ nhân viên chưa có tài khoản, thêm record mới vào USERS và ACCOUNTS.

--------------------------------------------------
## UC07 / 3.2.7 - Cập nhật tài khoản nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Admin chỉnh sửa thông tin tài khoản, trạng thái hoặc thông tin liên kết.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [LoginManagementPanel (Dialog chỉnh sửa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/LoginManagementPanel.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS
- USERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn một dòng dữ liệu cần chỉnh sửa trong danh sách
- Nhấn nút chức năng Chỉnh sửa
- Thay đổi thông tin trên biểu mẫu chỉnh sửa
- Nhấn Xác nhận cập nhật

Swimlane: Hệ thống
- Tải chi tiết dữ liệu của bản ghi được chọn
- Hiển thị biểu mẫu sửa với dữ liệu cũ
- Kiểm tra tính hợp lệ của dữ liệu chỉnh sửa mới
- Cập nhật thông tin bản ghi
- Làm mới danh sách và thông báo thành công

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Cập nhật thông tin mới vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện LoginManagementPanel.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng ACCOUNTS, USERS.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACCOUNTS, USERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: LoginManagementPanel thực hiện cập nhật thông tin email, số điện thoại trong USERS và trạng thái trong ACCOUNTS.

--------------------------------------------------
## UC08 / 3.2.8 - Khóa/Mở khóa tài khoản

### 1. Mục tiêu ngắn gọn
Cho phép Admin khóa hoặc mở lại tài khoản của nhân viên.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [LoginManagementPanel (Nút Khóa/Mở khóa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/LoginManagementPanel.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS
- AUDIT_LOG

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn bản ghi tài khoản cần thay đổi trạng thái
- Nhấn nút Khóa hoặc Mở khóa tài khoản
- Xác nhận thao tác

Swimlane: Hệ thống
- Kiểm tra trạng thái hiện tại của tài khoản
- Cập nhật trạng thái tài khoản trong hệ thống
- Ghi nhận nhật ký thay đổi thông tin hệ thống
- Thông báo kết quả thao tác thành công

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Cập nhật cột status tương ứng
- AUDIT_LOG: Ghi log thao tác thay đổi trạng thái của tài khoản
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện LoginManagementPanel.

2. Người dùng: Chọn tài khoản và chọn thao tác thay đổi trạng thái.

3. Hệ thống: Hiển thị hộp thoại yêu cầu xác nhận hành động.

4. Người dùng: Xác nhận đổi trạng thái.

5. Cơ sở dữ liệu: Cập nhật status trong ACCOUNTS và ghi log vào AUDIT_LOG.

6. Hệ thống: Cập nhật lại giao diện và thông báo kết quả thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACCOUNTS, AUDIT_LOG
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: LoginManagementPanel có sự kiện Toggle Status tài khoản, cập nhật trường status trong ACCOUNTS và ghi vào AUDIT_LOG.

--------------------------------------------------
## UC09 / 3.2.9 - Gán vai trò và quyền hạn

### 1. Mục tiêu ngắn gọn
Cho phép Admin gán vai trò như Admin, Quản lý cửa hàng, Nhân viên bán hàng/Thu ngân, Nhân viên kho và các nhóm quyền tương ứng.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [AccountRoleAssignmentPanel, RoleManagementPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/AccountRoleAssignmentPanel, RoleManagementPanel.java)
- DAO: [AccountAssignRoleSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountAssignRoleSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNT_ASSIGN_ROLE
- ACCOUNT_ASSIGN_ROLE_GROUP

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- ROLES: lấy chi tiết quyền để kiểm tra
- ROLE_GROUPS: liên kết nhóm quyền

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn tài khoản cần phân quyền trên danh sách
- Chọn vai trò/nhóm vai trò gán thêm hoặc loại bỏ
- Nhấn Xác nhận cập nhật quyền hạn

Swimlane: Hệ thống
- Hiển thị biểu đồ phân quyền và các hộp chọn vai trò
- Kiểm tra quyền quản trị của người dùng hiện hành
- Lưu cấu hình phân quyền mới
- Thông báo cập nhật phân quyền thành công

Swimlane: Cơ sở dữ liệu
- ACCOUNT_ASSIGN_ROLE: Lưu/Xóa các liên kết vai trò của tài khoản
- ACCOUNT_ASSIGN_ROLE_GROUP: Lưu/Xóa các liên kết nhóm quyền tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện AccountRoleAssignmentPanel, RoleManagementPanel.

2. Người dùng: Chọn tài khoản cần gán quyền và chọn các nhóm quyền phù hợp.

3. Người dùng: Chọn nút Lưu phân quyền.

4. Hệ thống: Kiểm tra quyền quản trị của tài khoản đang thao tác.

5. Cơ sở dữ liệu: Ghi các quyền/nhóm quyền mới vào bảng ACCOUNT_ASSIGN_ROLE và ACCOUNT_ASSIGN_ROLE_GROUP.

6. Hệ thống: Thông báo phân quyền thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACCOUNT_ASSIGN_ROLE, ACCOUNT_ASSIGN_ROLE_GROUP
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: AccountRoleAssignmentPanel hỗ trợ thêm/xóa liên kết vai trò của tài khoản trong ACCOUNT_ASSIGN_ROLE và ACCOUNT_ASSIGN_ROLE_GROUP.

--------------------------------------------------
## UC10 / 3.2.10 - Tra cứu tài khoản

### 1. Mục tiêu ngắn gọn
Cho phép Admin tìm kiếm và xem thông tin tài khoản theo tên, email, vai trò hoặc trạng thái.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [LoginManagementPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/LoginManagementPanel.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- USERS: join lấy thông tin họ tên, email

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện LoginManagementPanel.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng ACCOUNTS.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ACCOUNTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: LoginManagementPanel thực hiện tìm kiếm tài khoản theo username hoặc họ tên nhân viên.

--------------------------------------------------
## UC11 / 3.2.11 - Theo dõi lịch sử truy cập và nhật ký hệ thống

### 1. Mục tiêu ngắn gọn
Cho phép Admin xem lịch sử đăng nhập/đăng xuất và các thao tác quan trọng trong hệ thống.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [AuditLogPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/AuditLogPanel.java)
- DAO: [LoginHistorySql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/LoginHistorySql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- LOGIN_HISTORY
- AUDIT_LOG

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- LOGIN_HISTORY: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện AuditLogPanel.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng LOGIN_HISTORY, AUDIT_LOG.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + LOGIN_HISTORY, AUDIT_LOG
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: AuditLogPanel hiển thị danh sách nhật ký tác động hệ thống (AUDIT_LOG) và lịch sử truy cập (LOGIN_HISTORY).

--------------------------------------------------
## UC12 / 3.2.12 - Quản lý chi nhánh

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng quản lý thông tin cửa hàng/chi nhánh.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StoreManagementPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StoreManagementPanel.java)
- DAO: [StoresSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/StoresSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- STORES: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StoreManagementPanel.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: StoreManagementPanel hiển thị danh sách chi nhánh và tích hợp các nút Thêm, Sửa, Xóa.

--------------------------------------------------
## UC13 / 3.2.13 - Thêm chi nhánh

### 1. Mục tiêu ngắn gọn
Cho phép Admin thêm mới thông tin chi nhánh.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StoreManagementPanel (Dialog Thêm)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StoreManagementPanel.java)
- DAO: [StoresSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/StoresSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Nhấn nút chức năng Thêm mới
- Nhập đầy đủ thông tin vào biểu mẫu hiển thị
- Nhấn Xác nhận lưu

Swimlane: Hệ thống
- Hiển thị biểu mẫu nhập thông tin
- Kiểm tra dữ liệu bắt buộc và các ràng buộc nghiệp vụ (trùng lặp, định dạng)
- Tạo mới bản ghi nghiệp vụ
- Làm mới danh sách hiển thị và thông báo thành công

Swimlane: Cơ sở dữ liệu
- STORES: Thêm mới bản ghi vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StoreManagementPanel.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng STORES.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: StoreManagementPanel lưu chi nhánh mới vào bảng STORES qua StoresSql.insertStore.

--------------------------------------------------
## UC14 / 3.2.14 - Cập nhật chi nhánh

### 1. Mục tiêu ngắn gọn
Cho phép Admin chỉnh sửa thông tin chi nhánh như địa chỉ, email, số điện thoại và trạng thái.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StoreManagementPanel (Dialog Sửa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StoreManagementPanel.java)
- DAO: [StoresSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/StoresSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn một dòng dữ liệu cần chỉnh sửa trong danh sách
- Nhấn nút chức năng Chỉnh sửa
- Thay đổi thông tin trên biểu mẫu chỉnh sửa
- Nhấn Xác nhận cập nhật

Swimlane: Hệ thống
- Tải chi tiết dữ liệu của bản ghi được chọn
- Hiển thị biểu mẫu sửa với dữ liệu cũ
- Kiểm tra tính hợp lệ của dữ liệu chỉnh sửa mới
- Cập nhật thông tin bản ghi
- Làm mới danh sách và thông báo thành công

Swimlane: Cơ sở dữ liệu
- STORES: Cập nhật thông tin mới vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StoreManagementPanel.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng STORES.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: StoreManagementPanel cập nhật thông tin chi nhánh trong bảng STORES qua StoresSql.updateStore.

--------------------------------------------------
## UC15 / 3.2.15 - Xóa mềm chi nhánh

### 1. Mục tiêu ngắn gọn
Cho phép Admin ngừng hoạt động chi nhánh nhưng vẫn giữ lại dữ liệu trong hệ thống.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StoreManagementPanel (Nút Xóa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StoreManagementPanel.java)
- DAO: [StoresSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/StoresSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn bản ghi cần xóa trong danh sách hiển thị
- Nhấn nút chức năng Xóa bản ghi
- Xác nhận xóa tại hộp thoại cảnh báo của hệ thống

Swimlane: Hệ thống
- Hiển thị hộp thoại cảnh báo và yêu cầu xác nhận xóa
- Kiểm tra ràng buộc dữ liệu liên quan đến bản ghi cần xóa
- Đánh dấu ẩn bản ghi trong hệ thống
- Làm mới danh sách và thông báo xóa thành công

Swimlane: Cơ sở dữ liệu
- STORES: Cập nhật trường is_deleted = 1 (hoặc thay đổi trạng thái hoạt động)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StoreManagementPanel.

2. Người dùng: Chọn bản ghi trong danh sách và chọn Xóa.

3. Hệ thống: Hiển thị cảnh báo ảnh hưởng nghiệp vụ và yêu cầu xác nhận.

4. Người dùng: Xác nhận xóa bản ghi.

5. Hệ thống: Kiểm tra các ràng buộc liên quan (khóa ngoại).

Decision 1:
- Điều kiện: Có ràng buộc nghiệp vụ ngăn cản xóa?
- Nếu Có: Hệ thống: Hiển thị cảnh báo và dừng xóa.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Cập nhật flag is_deleted = 1 trong bảng STORES.

7. Hệ thống: Làm mới danh sách và thông báo xóa thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: StoreManagementPanel gọi StoresSql.deleteStore thực chất cập nhật flag is_deleted = 1 trong bảng STORES.

--------------------------------------------------
## UC16 / 3.2.16 - Tra cứu chi nhánh

### 1. Mục tiêu ngắn gọn
Cho phép Admin tìm kiếm và xem thông tin các chi nhánh.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StoreManagementPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StoreManagementPanel.java)
- DAO: [StoresSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/StoresSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- STORES: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StoreManagementPanel.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng STORES.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: StoreManagementPanel lọc danh sách chi nhánh theo tên hoặc địa chỉ từ bảng STORES.

--------------------------------------------------
## UC17 / 3.2.17 - Quản lý cửa hàng trưởng

### 1. Mục tiêu ngắn gọn
Cho phép Admin quản lý thông tin cửa hàng trưởng hoặc quản lý cửa hàng.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ManagerManagementView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ManagerManagementView.java)
- DAO: [StoreManagerSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/StoreManagerSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ManagerManagementView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES, STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ManagerManagementView quản lý việc gán nhân viên làm Quản lý (Manager) cho các chi nhánh.

--------------------------------------------------
## UC18 / 3.2.18 - Phân công cửa hàng trưởng

### 1. Mục tiêu ngắn gọn
Cho phép Admin liên kết cửa hàng trưởng với chi nhánh phụ trách.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ManagerManagementView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ManagerManagementView.java)
- DAO: [StoreManagerSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/rbac/StoreManagerSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES
- STORES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng phân công Quản lý cửa hàng
- Chọn chi nhánh cần phân công và chọn Nhân viên phù hợp
- Xác nhận phân công làm Cửa hàng trưởng

Swimlane: Hệ thống
- Hiển thị danh sách chi nhánh và nhân viên đủ điều kiện làm quản lý
- Kiểm tra xem nhân viên đã được phân công chi nhánh khác chưa
- Cập nhật thông tin chi nhánh quản lý và vai trò mới của nhân viên
- Thông báo phân công thành công

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Cập nhật store_id và role_id của nhân viên được chọn
- STORES: Cập nhật liên kết quản lý chi nhánh nếu có
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ManagerManagementView.

2. Người dùng: Chọn chi nhánh và nhân viên để phân công làm cửa hàng trưởng.

3. Người dùng: Nhấn nút Xác nhận phân công.

4. Hệ thống: Kiểm tra điều kiện nhân viên có đang quản lý chi nhánh khác.

Decision 1:
- Điều kiện: Nhân viên đang quản lý chi nhánh khác?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Cập nhật store_id và role_id quản lý của nhân viên trong EMPLOYEES.

6. Hệ thống: Thông báo phân công thành công và cập nhật giao diện.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES, STORES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ManagerManagementView cập nhật thông tin store_id và role_id (gán role quản lý) của nhân viên trong EMPLOYEES.

--------------------------------------------------
## UC19 / 3.2.19 - Quản lý nhân viên

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng quản lý hồ sơ nhân viên trong cửa hàng.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView hiển thị danh sách hồ sơ nhân viên trong chi nhánh.

--------------------------------------------------
## UC20 / 3.2.20 - Thêm nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng thêm mới thông tin nhân viên vào hệ thống.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Dialog Thêm)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- STORES: liên kết chi nhánh
- SHIFTS: liên kết ca mặc định

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Nhấn nút chức năng Thêm mới
- Nhập đầy đủ thông tin vào biểu mẫu hiển thị
- Nhấn Xác nhận lưu

Swimlane: Hệ thống
- Hiển thị biểu mẫu nhập thông tin
- Kiểm tra dữ liệu bắt buộc và các ràng buộc nghiệp vụ (trùng lặp, định dạng)
- Tạo mới bản ghi nghiệp vụ
- Làm mới danh sách hiển thị và thông báo thành công

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Thêm mới bản ghi vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng EMPLOYEES.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView lưu thông tin nhân viên mới vào bảng EMPLOYEES qua EmployeeSql.insertEmployee.

--------------------------------------------------
## UC21 / 3.2.21 - Cập nhật nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng chỉnh sửa thông tin nhân viên hiện có.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Dialog Sửa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn một dòng dữ liệu cần chỉnh sửa trong danh sách
- Nhấn nút chức năng Chỉnh sửa
- Thay đổi thông tin trên biểu mẫu chỉnh sửa
- Nhấn Xác nhận cập nhật

Swimlane: Hệ thống
- Tải chi tiết dữ liệu của bản ghi được chọn
- Hiển thị biểu mẫu sửa với dữ liệu cũ
- Kiểm tra tính hợp lệ của dữ liệu chỉnh sửa mới
- Cập nhật thông tin bản ghi
- Làm mới danh sách và thông báo thành công

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Cập nhật thông tin mới vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng EMPLOYEES.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView cập nhật hồ sơ nhân viên trong bảng EMPLOYEES qua EmployeeSql.updateEmployee.

--------------------------------------------------
## UC22 / 3.2.22 - Xóa mềm nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng ngừng hoạt động hồ sơ nhân viên nhưng vẫn giữ dữ liệu lịch sử.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Nút Xóa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn bản ghi cần xóa trong danh sách hiển thị
- Nhấn nút chức năng Xóa bản ghi
- Xác nhận xóa tại hộp thoại cảnh báo của hệ thống

Swimlane: Hệ thống
- Hiển thị hộp thoại cảnh báo và yêu cầu xác nhận xóa
- Kiểm tra ràng buộc dữ liệu liên quan đến bản ghi cần xóa
- Đánh dấu ẩn bản ghi trong hệ thống
- Làm mới danh sách và thông báo xóa thành công

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Cập nhật trường is_deleted = 1 (hoặc thay đổi trạng thái hoạt động)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Người dùng: Chọn bản ghi trong danh sách và chọn Xóa.

3. Hệ thống: Hiển thị cảnh báo ảnh hưởng nghiệp vụ và yêu cầu xác nhận.

4. Người dùng: Xác nhận xóa bản ghi.

5. Hệ thống: Kiểm tra các ràng buộc liên quan (khóa ngoại).

Decision 1:
- Điều kiện: Có ràng buộc nghiệp vụ ngăn cản xóa?
- Nếu Có: Hệ thống: Hiển thị cảnh báo và dừng xóa.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Cập nhật flag is_deleted = 1 trong bảng EMPLOYEES.

7. Hệ thống: Làm mới danh sách và thông báo xóa thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView gọi EmployeeSql.deleteEmployee thực chất cập nhật flag is_deleted = 1 trong bảng EMPLOYEES.

--------------------------------------------------
## UC23 / 3.2.23 - Tra cứu nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng tìm kiếm và xem thông tin nhân viên theo tên, mã, loại nhân viên hoặc trạng thái.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- EMPLOYEES: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng EMPLOYEES.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView lọc danh sách nhân viên theo tên, email, sđt từ bảng EMPLOYEES.

--------------------------------------------------
## UC24 / 3.2.24 - Phân ca nhân viên

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng lập, cập nhật và theo dõi ca làm việc của nhân viên.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Tab Phân ca)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeShiftSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeShiftSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEE_SHIFTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- SHIFTS: hiển thị danh sách ca
- EMPLOYEES: hiển thị danh sách nhân viên

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- EMPLOYEE_SHIFTS: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEE_SHIFTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView chứa tab phân ca, hiển thị lịch phân ca của nhân viên từ EMPLOYEE_SHIFTS.

--------------------------------------------------
## UC25 / 3.2.25 - Lập ca làm việc

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng phân công ca làm cho nhân viên bán hàng, thu ngân hoặc nhân viên kho.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Dialog Lập ca)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeShiftSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeShiftSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEE_SHIFTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- SHIFTS: liên kết ca
- EMPLOYEES: liên kết nhân viên

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn chức năng Lập ca làm việc
- Chọn nhân viên, chọn ca làm việc (Sáng/Chiều/Tối) và ngày làm việc
- Xác nhận lập lịch làm việc

Swimlane: Hệ thống
- Hiển thị giao diện lập lịch và ca làm việc
- Kiểm tra trùng lịch làm việc của nhân viên trong ngày đã chọn
- Lưu thông tin lịch làm việc mới và thông báo kết quả

Swimlane: Cơ sở dữ liệu
- EMPLOYEE_SHIFTS: Thêm mới bản ghi ca làm việc được phân công cho nhân viên
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng EMPLOYEE_SHIFTS.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEE_SHIFTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView cho phép lập lịch phân ca mới, lưu vào EMPLOYEE_SHIFTS.

--------------------------------------------------
## UC26 / 3.2.26 - Cập nhật ca làm việc

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng thay đổi thông tin ca đã phân công.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Dialog Sửa ca)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeShiftSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeShiftSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEE_SHIFTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn một ca làm việc của nhân viên trên lịch
- Thay đổi ca làm hoặc chọn ngày làm việc mới
- Xác nhận cập nhật ca làm việc

Swimlane: Hệ thống
- Tải thông tin chi tiết ca làm việc cũ
- Kiểm tra tính hợp lệ của ca làm việc mới (tránh trùng lặp ca)
- Lưu thông tin thay đổi và thông báo thành công

Swimlane: Cơ sở dữ liệu
- EMPLOYEE_SHIFTS: Cập nhật thông tin ca làm việc hoặc ngày làm việc mới
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng EMPLOYEE_SHIFTS.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEE_SHIFTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView cập nhật thông tin ca làm việc được phân công trong EMPLOYEE_SHIFTS.

--------------------------------------------------
## UC27 / 3.2.27 - Hủy ca làm việc

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng hủy ca làm việc khi không còn áp dụng.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Nút Hủy phân ca)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeShiftSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeShiftSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEE_SHIFTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn ca làm việc cần hủy trên lịch phân ca
- Nhấn nút Hủy ca làm việc và xác nhận thao tác

Swimlane: Hệ thống
- Hiển thị hộp thoại xác nhận hủy lịch ca làm việc
- Cập nhật trạng thái ca làm việc được chọn thành CANCELED
- Thông báo hủy ca làm việc thành công

Swimlane: Cơ sở dữ liệu
- EMPLOYEE_SHIFTS: Cập nhật cột status thành 'CANCELED' hoặc is_deleted = 1
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEE_SHIFTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView cho phép cập nhật trạng thái phân ca thành 'CANCELED' hoặc is_deleted = 1 trong EMPLOYEE_SHIFTS.

--------------------------------------------------
## UC28 / 3.2.28 - Tra cứu ca làm việc

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng lọc và xem lịch làm việc theo nhân viên, ngày, ca hoặc trạng thái.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeeView (Tab Phân ca)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeeView.java)
- DAO: [EmployeeShiftSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/EmployeeShiftSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- EMPLOYEE_SHIFTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- SHIFTS: lọc theo ca
- EMPLOYEES: lọc theo tên nhân viên

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- EMPLOYEE_SHIFTS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeeView.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng EMPLOYEE_SHIFTS.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + EMPLOYEE_SHIFTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeeView hỗ trợ lọc lịch phân ca theo ngày, nhân viên hoặc ca làm việc.

--------------------------------------------------
## UC29 / 3.2.29 - Quản lý KPI nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng nhập, xem, phân tích và xuất dữ liệu KPI của nhân viên.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [EmployeePerformancePanel, ImportKpiDialog](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/EmployeePerformancePanel, ImportKpiDialog.java)
- DAO: [KpiEvaluationSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/hr_kpi/KpiEvaluationSql.java)
- File SQL/schema: [create_kpi_history_table.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/create_kpi_history_table.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- KPI_EVALUATION
- EMPLOYEE_KPI_HISTORY

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- KPI_CRITERIA: tham chiếu tiêu chí KPI
- EMPLOYEES: thông tin nhân viên

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn kỳ đánh giá và chọn Nhân viên cần cập nhật KPI
- Nhập kết quả thực tế KPI hoặc chọn file Excel chứa kết quả KPI để import
- Nhập nhận xét của quản lý và xác nhận lưu kết quả

Swimlane: Hệ thống
- Đọc dữ liệu nhập trực tiếp hoặc phân tích file Excel tải lên
- Tính toán điểm KPI tự động dựa trên trọng số tiêu chí
- Lưu kết quả đánh giá KPI và hiển thị thông báo thành công

Swimlane: Cơ sở dữ liệu
- KPI_EVALUATION: Lưu kết quả thực tế, điểm đạt được của nhân viên theo tiêu chí
- EMPLOYEE_KPI_HISTORY: Ghi nhận lịch sử tổng hợp hiệu suất làm việc của nhân viên
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện EmployeePerformancePanel, ImportKpiDialog.

2. Người dùng: Chọn nhân viên, kỳ đánh giá hoặc thực hiện Import từ Excel.

3. Hệ thống: Tiếp nhận kết quả nhập hoặc phân tích file Excel tải lên.

4. Hệ thống: Tính toán điểm hiệu suất đạt được dựa trên trọng số KPI.

5. Cơ sở dữ liệu: Lưu điểm chi tiết vào KPI_EVALUATION và tổng hợp vào EMPLOYEE_KPI_HISTORY.

6. Hệ thống: Hiển thị kết quả đánh giá KPI và thông báo lưu thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + KPI_EVALUATION, EMPLOYEE_KPI_HISTORY
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: EmployeePerformancePanel xử lý nhập KPI trực tiếp hoặc import từ excel vào EMPLOYEE_KPI_HISTORY, đánh giá KPI nhân viên.

--------------------------------------------------
## UC30 / 3.2.30 - Quản lý sản phẩm

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng quản lý danh sách sản phẩm trong hệ thống.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ProductView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ProductView.java)
- DAO: [ProductsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/ProductsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- PRODUCTS: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ProductView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ProductView hiển thị danh sách sản phẩm hệ thống.

--------------------------------------------------
## UC31 / 3.2.31 - Thêm sản phẩm

### 1. Mục tiêu ngắn gọn
Cho phép người dùng có quyền thêm mới sản phẩm vào hệ thống.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ProductView (Dialog Thêm) / ImportProductDialog](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ProductView.java)
- DAO: [ProductsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/ProductsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCTS
- STORE_PRODUCTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- CATEGORIES: chọn danh mục
- SUPPLIERS: chọn nhà cung cấp
- UNITS: chọn đơn vị tính gốc

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Nhấn chức năng Thêm sản phẩm
- Nhập Tên sản phẩm, chọn Danh mục, Nhà cung cấp, Đơn vị tính gốc, Giá vốn
- Nhập giá bán và tồn kho tối thiểu tại chi nhánh
- Xác nhận lưu sản phẩm

Swimlane: Hệ thống
- Hiển thị biểu mẫu thêm sản phẩm mới
- Kiểm tra trùng mã vạch/tên sản phẩm, tính hợp lý của giá vốn và giá bán
- Thêm mới sản phẩm vào hệ thống
- Khởi tạo giá bán và chính sách tồn kho cho sản phẩm tại chi nhánh đang quản lý
- Thông báo thành công

Swimlane: Cơ sở dữ liệu
- PRODUCTS: Lưu thông tin chung của sản phẩm (tên, danh mục, nhà cung cấp, đơn vị tính)
- STORE_PRODUCTS: Lưu thông tin cấu hình giá bán, min/max stock của sản phẩm tại chi nhánh
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ProductView.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng PRODUCTS, STORE_PRODUCTS.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCTS, STORE_PRODUCTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ProductView thực hiện lưu sản phẩm mới vào PRODUCTS và đồng thời khởi tạo thông tin bán hàng trong STORE_PRODUCTS.

--------------------------------------------------
## UC32 / 3.2.32 - Cập nhật sản phẩm

### 1. Mục tiêu ngắn gọn
Cho phép người dùng có quyền chỉnh sửa thông tin sản phẩm.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ProductView (Dialog Sửa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ProductView.java)
- DAO: [ProductsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/ProductsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCTS
- STORE_PRODUCTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn sản phẩm cần chỉnh sửa và nhấn nút Chỉnh sửa
- Thay đổi các thông tin được phép (tên, giá vốn, giá bán, min/max stock)
- Xác nhận cập nhật thông tin

Swimlane: Hệ thống
- Tải thông tin chi tiết sản phẩm cũ
- Kiểm tra tính hợp lệ của dữ liệu chỉnh sửa mới
- Cập nhật thông tin sản phẩm trên toàn hệ thống và tại chi nhánh đang quản lý
- Thông báo cập nhật thành công

Swimlane: Cơ sở dữ liệu
- PRODUCTS: Cập nhật thông tin chung của sản phẩm
- STORE_PRODUCTS: Cập nhật thông tin giá bán và mức tồn kho tối thiểu tại chi nhánh
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ProductView.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng PRODUCTS, STORE_PRODUCTS.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCTS, STORE_PRODUCTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ProductView cập nhật thông tin tên, giá vốn trong PRODUCTS và cập nhật giá bán, min/max stock trong STORE_PRODUCTS.

--------------------------------------------------
## UC33 / 3.2.33 - Xóa mềm sản phẩm

### 1. Mục tiêu ngắn gọn
Cho phép người dùng có quyền ngừng kinh doanh hoặc ẩn sản phẩm khỏi danh sách.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ProductView (Nút Xóa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ProductView.java)
- DAO: [ProductsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/ProductsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- STORE_PRODUCTS: cascade/ngừng hoạt động sản phẩm tại cửa hàng

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn bản ghi cần xóa trong danh sách hiển thị
- Nhấn nút chức năng Xóa bản ghi
- Xác nhận xóa tại hộp thoại cảnh báo của hệ thống

Swimlane: Hệ thống
- Hiển thị hộp thoại cảnh báo và yêu cầu xác nhận xóa
- Kiểm tra ràng buộc dữ liệu liên quan đến bản ghi cần xóa
- Đánh dấu ẩn bản ghi trong hệ thống
- Làm mới danh sách và thông báo xóa thành công

Swimlane: Cơ sở dữ liệu
- PRODUCTS: Cập nhật trường is_deleted = 1 (hoặc thay đổi trạng thái hoạt động)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ProductView.

2. Người dùng: Chọn bản ghi trong danh sách và chọn Xóa.

3. Hệ thống: Hiển thị cảnh báo ảnh hưởng nghiệp vụ và yêu cầu xác nhận.

4. Người dùng: Xác nhận xóa bản ghi.

5. Hệ thống: Kiểm tra các ràng buộc liên quan (khóa ngoại).

Decision 1:
- Điều kiện: Có ràng buộc nghiệp vụ ngăn cản xóa?
- Nếu Có: Hệ thống: Hiển thị cảnh báo và dừng xóa.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Cập nhật flag is_deleted = 1 trong bảng PRODUCTS.

7. Hệ thống: Làm mới danh sách và thông báo xóa thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ProductView gọi ProductsSql.deleteProduct cập nhật is_deleted = 1 trong PRODUCTS.

--------------------------------------------------
## UC34 / 3.2.34 - Tra cứu sản phẩm

### 1. Mục tiêu ngắn gọn
Cho phép người dùng tìm kiếm và xem thông tin sản phẩm.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ProductView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ProductView.java)
- DAO: [ProductsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/ProductsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCTS
- INVENTORY

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- PRODUCTS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ProductView.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng PRODUCTS, INVENTORY.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCTS, INVENTORY
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ProductView lọc sản phẩm theo từ khóa, danh mục hoặc nhà cung cấp.

--------------------------------------------------
## UC35 / 3.2.35 - Cấu hình đơn vị tính sản phẩm

### 1. Mục tiêu ngắn gọn
Cho phép cấu hình đơn vị gốc, đơn vị quy đổi và tỷ lệ quy đổi cho sản phẩm.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [ProductView (Tab Đơn vị tính)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/ProductView.java)
- DAO: [ProductUnitsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/ProductUnitsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCT_UNITS
- UNITS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- PRODUCTS: tham chiếu sản phẩm

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn sản phẩm cần cấu hình đơn vị tính quy đổi
- Chọn thêm đơn vị quy đổi, nhập tỷ lệ quy đổi so với đơn vị gốc
- Xác nhận lưu cấu hình đơn vị tính

Swimlane: Hệ thống
- Hiển thị giao diện danh sách đơn vị tính của sản phẩm
- Kiểm tra tỷ lệ quy đổi phải lớn hơn 0 và không trùng đơn vị gốc
- Lưu thiết lập quy đổi đơn vị tính và thông báo thành công

Swimlane: Cơ sở dữ liệu
- PRODUCT_UNITS: Lưu thông tin liên kết giữa sản phẩm, đơn vị quy đổi và tỷ lệ tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện ProductView.

2. Người dùng: Chọn sản phẩm và chọn Thêm đơn vị quy đổi mới.

3. Người dùng: Chọn đơn vị quy đổi, nhập tỷ lệ quy đổi và nhấn Xác nhận.

4. Hệ thống: Kiểm tra tính hợp lệ của tỷ lệ quy đổi.

Decision 1:
- Điều kiện: Đơn vị tính bị trùng lặp?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Ghi nhận đơn vị tính mới cho sản phẩm vào PRODUCT_UNITS.

6. Hệ thống: Làm mới danh sách đơn vị quy đổi và thông báo thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCT_UNITS, UNITS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: ProductView có tab đơn vị tính để gán và cấu hình tỷ lệ quy đổi trong PRODUCT_UNITS.

--------------------------------------------------
## UC36 / 3.2.36 - Quản lý danh mục và thuế VAT

### 1. Mục tiêu ngắn gọn
Cho phép quản lý danh mục sản phẩm và mức thuế VAT áp dụng; chức năng đã có một phần trong code.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CategoryTaxView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CategoryTaxView.java)
- DAO: [CategoriesSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/CategoriesSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- CATEGORIES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở giao diện Quản lý danh mục và thuế VAT
- Nhập tên danh mục mới và thiết lập thuế VAT (%) mặc định
- Xác nhận lưu danh mục sản phẩm

Swimlane: Hệ thống
- Hiển thị danh mục và mức thuế áp dụng hiện hành
- Kiểm tra trùng tên danh mục, mức thuế VAT hợp lệ (từ 0% đến 100%)
- Lưu thông tin danh mục mới và thông báo thành công

Swimlane: Cơ sở dữ liệu
- CATEGORIES: Thêm mới/cập nhật thông tin danh mục và mức thuế VAT mặc định
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CategoryTaxView.

2. Người dùng: Chọn Thêm mới danh mục hoặc điều chỉnh thuế VAT.

3. Người dùng: Nhập tên danh mục và mức thuế VAT (%), chọn Lưu.

4. Hệ thống: Kiểm tra trùng lặp tên danh mục và phạm vi thuế VAT hợp lệ (0-100%).

Decision 1:
- Điều kiện: Tên danh mục trùng lặp hoặc thuế VAT ngoài khoảng 0-100%?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Lưu thông tin danh mục và thuế VAT mặc định vào CATEGORIES.

6. Hệ thống: Thông báo lưu thông tin danh mục thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + CATEGORIES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CategoryTaxView hiển thị danh sách danh mục sản phẩm, mức thuế VAT mặc định của danh mục.

--------------------------------------------------
## UC37 / 3.2.37 - Quản lý nhà cung cấp

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng quản lý thông tin nhà cung cấp.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SupplierManagementView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SupplierManagementView.java)
- DAO: [SuppliersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/SuppliersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- SUPPLIERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- SUPPLIERS: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SupplierManagementView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + SUPPLIERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SupplierManagementView hiển thị danh sách nhà cung cấp của hệ thống.

--------------------------------------------------
## UC38 / 3.2.38 - Thêm nhà cung cấp

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng hoặc Nhân viên kho thêm mới thông tin nhà cung cấp.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SupplierManagementView (Dialog Thêm)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SupplierManagementView.java)
- DAO: [SuppliersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/SuppliersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- SUPPLIERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Nhấn nút chức năng Thêm mới
- Nhập đầy đủ thông tin vào biểu mẫu hiển thị
- Nhấn Xác nhận lưu

Swimlane: Hệ thống
- Hiển thị biểu mẫu nhập thông tin
- Kiểm tra dữ liệu bắt buộc và các ràng buộc nghiệp vụ (trùng lặp, định dạng)
- Tạo mới bản ghi nghiệp vụ
- Làm mới danh sách hiển thị và thông báo thành công

Swimlane: Cơ sở dữ liệu
- SUPPLIERS: Thêm mới bản ghi vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SupplierManagementView.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng SUPPLIERS.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + SUPPLIERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SupplierManagementView thêm nhà cung cấp mới vào bảng SUPPLIERS qua SuppliersSql.insertSupplier.

--------------------------------------------------
## UC39 / 3.2.39 - Cập nhật nhà cung cấp

### 1. Mục tiêu ngắn gọn
Cho phép chỉnh sửa thông tin nhà cung cấp hiện có.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SupplierManagementView (Dialog Sửa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SupplierManagementView.java)
- DAO: [SuppliersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/SuppliersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- SUPPLIERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn một dòng dữ liệu cần chỉnh sửa trong danh sách
- Nhấn nút chức năng Chỉnh sửa
- Thay đổi thông tin trên biểu mẫu chỉnh sửa
- Nhấn Xác nhận cập nhật

Swimlane: Hệ thống
- Tải chi tiết dữ liệu của bản ghi được chọn
- Hiển thị biểu mẫu sửa với dữ liệu cũ
- Kiểm tra tính hợp lệ của dữ liệu chỉnh sửa mới
- Cập nhật thông tin bản ghi
- Làm mới danh sách và thông báo thành công

Swimlane: Cơ sở dữ liệu
- SUPPLIERS: Cập nhật thông tin mới vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SupplierManagementView.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng SUPPLIERS.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + SUPPLIERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SupplierManagementView cập nhật thông tin nhà cung cấp trong bảng SUPPLIERS qua SuppliersSql.updateSupplier.

--------------------------------------------------
## UC40 / 3.2.40 - Xóa mềm nhà cung cấp

### 1. Mục tiêu ngắn gọn
Cho phép ngừng sử dụng nhà cung cấp nhưng vẫn giữ dữ liệu liên quan.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SupplierManagementView (Nút Xóa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SupplierManagementView.java)
- DAO: [SuppliersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/SuppliersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- SUPPLIERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn bản ghi cần xóa trong danh sách hiển thị
- Nhấn nút chức năng Xóa bản ghi
- Xác nhận xóa tại hộp thoại cảnh báo của hệ thống

Swimlane: Hệ thống
- Hiển thị hộp thoại cảnh báo và yêu cầu xác nhận xóa
- Kiểm tra ràng buộc dữ liệu liên quan đến bản ghi cần xóa
- Đánh dấu ẩn bản ghi trong hệ thống
- Làm mới danh sách và thông báo xóa thành công

Swimlane: Cơ sở dữ liệu
- SUPPLIERS: Cập nhật trường is_deleted = 1 (hoặc thay đổi trạng thái hoạt động)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SupplierManagementView.

2. Người dùng: Chọn bản ghi trong danh sách và chọn Xóa.

3. Hệ thống: Hiển thị cảnh báo ảnh hưởng nghiệp vụ và yêu cầu xác nhận.

4. Người dùng: Xác nhận xóa bản ghi.

5. Hệ thống: Kiểm tra các ràng buộc liên quan (khóa ngoại).

Decision 1:
- Điều kiện: Có ràng buộc nghiệp vụ ngăn cản xóa?
- Nếu Có: Hệ thống: Hiển thị cảnh báo và dừng xóa.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Cập nhật flag is_deleted = 1 trong bảng SUPPLIERS.

7. Hệ thống: Làm mới danh sách và thông báo xóa thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + SUPPLIERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SupplierManagementView gọi SuppliersSql.deleteSupplier cập nhật is_deleted = 1 trong SUPPLIERS.

--------------------------------------------------
## UC41 / 3.2.41 - Tra cứu nhà cung cấp

### 1. Mục tiêu ngắn gọn
Cho phép tìm kiếm và xem thông tin nhà cung cấp.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SupplierManagementView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SupplierManagementView.java)
- DAO: [SuppliersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/SuppliersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- SUPPLIERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- SUPPLIERS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SupplierManagementView.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng SUPPLIERS.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + SUPPLIERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SupplierManagementView lọc nhà cung cấp theo tên, số điện thoại hoặc email.

--------------------------------------------------
## UC42 / 3.2.42 - Quản lý tồn kho

### 1. Mục tiêu ngắn gọn
Cho phép Nhân viên kho xem, tìm kiếm, lọc và theo dõi số lượng hàng tồn kho.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [InventoryView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/InventoryView.java)
- DAO: [InventorySql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/InventorySql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- INVENTORY
- STORE_PRODUCTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- PRODUCTS: join lấy tên sản phẩm, đơn vị

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- INVENTORY: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện InventoryView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + INVENTORY, STORE_PRODUCTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: InventoryView hiển thị số lượng tồn kho thực tế của các sản phẩm tại chi nhánh đang đăng nhập.

--------------------------------------------------
## UC43 / 3.2.43 - Nhập hàng và lập phiếu nhập

### 1. Mục tiêu ngắn gọn
Cho phép Nhân viên kho nhập hàng thủ công hoặc từ CSV, tạo phiếu nhập và cập nhật tồn kho.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StockImportReceiptDialog / PurchaseReceiptInvoiceDialog](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StockImportReceiptDialog / PurchaseReceiptInvoiceDialog.java)
- DAO: [PurchaseReceiptSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/PurchaseReceiptSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PURCHASE_RECEIPTS
- PURCHASE_RECEIPT_DETAILS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- INVENTORY: cộng số lượng tồn kho
- INVENTORY_TRANSACTIONS: ghi nhận giao dịch nhập WAREHOUSE_INBOUND

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở màn hình lập phiếu nhập kho
- Chọn nhà cung cấp giao hàng
- Quét/Thêm sản phẩm nhập, nhập số lượng và giá nhập kho thực tế
- Xác nhận hoàn tất lập phiếu nhập và nhập hàng vào kho

Swimlane: Hệ thống
- Hiển thị giao diện nhập hàng
- Kiểm tra thông tin nhà cung cấp và danh sách sản phẩm nhập
- Tính tổng tiền trước thuế, thuế VAT và tổng thanh toán của phiếu nhập
- Lưu phiếu nhập và chi tiết phiếu nhập
- Cộng số lượng tồn kho của sản phẩm tại chi nhánh tương ứng
- Ghi nhận lịch sử giao dịch biến động kho
- Thông báo nhập hàng thành công

Swimlane: Cơ sở dữ liệu
- PURCHASE_RECEIPTS: Tạo mới thông tin phiếu nhập hàng (nhà cung cấp, tổng tiền)
- PURCHASE_RECEIPT_DETAILS: Lưu chi tiết danh sách sản phẩm, số lượng, giá nhập, thuế
- INVENTORY: Cập nhật (cộng thêm) số lượng sản phẩm nhập vào kho chi nhánh
- INVENTORY_TRANSACTIONS: Lưu lịch sử giao dịch biến động kho loại 'INBOUND'
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StockImportReceiptDialog / PurchaseReceiptInvoiceDialog.

2. Người dùng: Chọn nhà cung cấp và thêm các sản phẩm cần nhập kho.

3. Người dùng: Nhập số lượng nhập, đơn giá nhập thực tế và chọn Xác nhận.

4. Hệ thống: Kiểm tra tính hợp lệ của số lượng và tính toán tổng số tiền thanh toán.

5. Cơ sở dữ liệu: Lưu phiếu nhập vào PURCHASE_RECEIPTS và chi tiết vào PURCHASE_RECEIPT_DETAILS.

6. Cơ sở dữ liệu: Cập nhật cộng tồn kho trong INVENTORY và ghi lịch sử vào INVENTORY_TRANSACTIONS.

7. Hệ thống: Xuất phiếu nhập hàng hóa và thông báo hoàn thành nhập kho.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PURCHASE_RECEIPTS, PURCHASE_RECEIPT_DETAILS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: Giao diện lập phiếu nhập hàng, lưu dữ liệu vào PURCHASE_RECEIPTS và PURCHASE_RECEIPT_DETAILS, cập nhật INVENTORY và ghi lịch sử giao dịch kho.

--------------------------------------------------
## UC44 / 3.2.44 - Xem lịch sử biến động kho

### 1. Mục tiêu ngắn gọn
Cho phép Nhân viên kho xem các giao dịch nhập, xuất, hoàn kho hoặc điều chỉnh tồn kho.

### 2. Tác nhân chính
- Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [InventoryHistoryDialog](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/InventoryHistoryDialog.java)
- DAO: [InventoryTransactionSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/InventoryTransactionSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- INVENTORY_TRANSACTIONS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- PRODUCTS: tham chiếu tên
- PURCHASE_RECEIPTS: liên kết phiếu nhập

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- INVENTORY_TRANSACTIONS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện InventoryHistoryDialog.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng INVENTORY_TRANSACTIONS.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + INVENTORY_TRANSACTIONS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: InventoryHistoryDialog hiển thị chi tiết lịch sử các giao dịch nhập, xuất kho từ bảng INVENTORY_TRANSACTIONS.

--------------------------------------------------
## UC45 / 3.2.45 - Gửi và xử lý cảnh báo tồn kho

### 1. Mục tiêu ngắn gọn
Cho phép Nhân viên bán hàng/Quản lý tạo cảnh báo hàng thấp và Nhân viên kho tiếp nhận, xử lý cảnh báo.

### 2. Tác nhân chính
- Nhân viên kho; Quản lý cửa hàng; Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [WarehouseDashboardView / AdminDashboardView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/WarehouseDashboardView / AdminDashboardView.java)
- DAO: [InventoryNotificationSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/prod_inventory/InventoryNotificationSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- INVENTORY_NOTIFICATIONS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- PRODUCTS: thông tin sản phẩm
- INVENTORY: kiểm tra số lượng tồn kho

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Người dùng xem thông báo trên thanh tiêu đề hệ thống hoặc màn hình Warehouse

Swimlane: Hệ thống
- Tự động chạy tiến trình quét tồn kho của sản phẩm tại chi nhánh định kỳ
- Kiểm tra số lượng tồn kho hiện tại (INVENTORY) so với mức tồn kho tối thiểu (STORE_PRODUCTS)
- Tạo cảnh báo tồn kho thấp cho sản phẩm vi phạm ràng buộc
- Gửi thông báo đến tài khoản thuộc nhóm nhân viên kho (Warehouse)

Swimlane: Cơ sở dữ liệu
- INVENTORY_NOTIFICATIONS: Thêm mới bản ghi cảnh báo tồn kho thấp của sản phẩm
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện WarehouseDashboardView / AdminDashboardView.

2. Hệ thống: Tự động chạy tiến trình quét kiểm tra mức tồn kho sản phẩm định kỳ.

3. Cơ sở dữ liệu: Truy vấn dữ liệu tồn kho hiện tại trong INVENTORY và min_stock trong STORE_PRODUCTS.

4. Hệ thống: Phát hiện các sản phẩm có số lượng tồn thực tế thấp hơn mức tối thiểu.

Decision 1:
- Điều kiện: Phát hiện sản phẩm có tồn kho thấp?
- Nếu Có: Đi tiếp bước tiếp theo.
- Nếu Không: Kết thúc tiến trình quét.

5. Cơ sở dữ liệu: Ghi nhận bản ghi cảnh báo mới vào bảng INVENTORY_NOTIFICATIONS.

6. Hệ thống: Hiển thị chuông thông báo màu đỏ trên giao diện làm việc của Nhân viên kho.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + INVENTORY_NOTIFICATIONS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: Hệ thống quét sản phẩm dưới mức min_stock và tự động tạo thông báo trong INVENTORY_NOTIFICATIONS, Warehouse UI hiển thị chuông cảnh báo.

--------------------------------------------------
## UC46 / 3.2.46 - Quản lý khách hàng/hội viên

### 1. Mục tiêu ngắn gọn
Bao quát các chức năng quản lý thông tin khách hàng và hội viên phục vụ POS.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CustomerView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CustomerView.java)
- DAO: [CustomersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/CustomersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- CUSTOMERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn mở màn hình quản lý nghiệp vụ tương ứng
- Xem danh sách dữ liệu hiện tại
- Chọn các nút chức năng (Thêm, Sửa, Xóa, Tra cứu)

Swimlane: Hệ thống
- Hiển thị màn hình quản lý nghiệp vụ và tải danh sách dữ liệu
- Kiểm tra quyền hạn của người dùng đăng nhập đối với chức năng

Swimlane: Cơ sở dữ liệu
- CUSTOMERS: Truy vấn danh sách bản ghi hoạt động (chưa xóa)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CustomerView.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + CUSTOMERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CustomerView hiển thị danh sách thông tin khách hàng, số điểm tích lũy, hạng thành viên.

--------------------------------------------------
## UC47 / 3.2.47 - Thêm khách hàng/hội viên

### 1. Mục tiêu ngắn gọn
Cho phép nhân viên thêm mới thông tin khách hàng phục vụ bán hàng POS.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CustomerView (Dialog Thêm)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CustomerView.java)
- DAO: [CustomersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/CustomersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- CUSTOMERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Nhấn nút chức năng Thêm mới
- Nhập đầy đủ thông tin vào biểu mẫu hiển thị
- Nhấn Xác nhận lưu

Swimlane: Hệ thống
- Hiển thị biểu mẫu nhập thông tin
- Kiểm tra dữ liệu bắt buộc và các ràng buộc nghiệp vụ (trùng lặp, định dạng)
- Tạo mới bản ghi nghiệp vụ
- Làm mới danh sách hiển thị và thông báo thành công

Swimlane: Cơ sở dữ liệu
- CUSTOMERS: Thêm mới bản ghi vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CustomerView.

2. Hệ thống: Hiển thị biểu mẫu nhập thông tin.

3. Người dùng: Điền đầy đủ thông tin bắt buộc và chọn Xác nhận lưu.

4. Hệ thống: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp.

Decision 1:
- Điều kiện: Dữ liệu không hợp lệ hoặc bị trùng?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi và yêu cầu nhập lại.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Thực hiện lưu mới bản ghi vào bảng CUSTOMERS.

6. Hệ thống: Làm mới danh sách hiển thị và thông báo tạo mới thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + CUSTOMERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CustomerView thêm khách hàng mới vào bảng CUSTOMERS qua CustomersSql.insertCustomer.

--------------------------------------------------
## UC48 / 3.2.48 - Cập nhật khách hàng/hội viên

### 1. Mục tiêu ngắn gọn
Cho phép nhân viên chỉnh sửa thông tin khách hàng hiện có.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CustomerView (Dialog Sửa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CustomerView.java)
- DAO: [CustomersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/CustomersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- CUSTOMERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn một dòng dữ liệu cần chỉnh sửa trong danh sách
- Nhấn nút chức năng Chỉnh sửa
- Thay đổi thông tin trên biểu mẫu chỉnh sửa
- Nhấn Xác nhận cập nhật

Swimlane: Hệ thống
- Tải chi tiết dữ liệu của bản ghi được chọn
- Hiển thị biểu mẫu sửa với dữ liệu cũ
- Kiểm tra tính hợp lệ của dữ liệu chỉnh sửa mới
- Cập nhật thông tin bản ghi
- Làm mới danh sách và thông báo thành công

Swimlane: Cơ sở dữ liệu
- CUSTOMERS: Cập nhật thông tin mới vào bảng dữ liệu tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CustomerView.

2. Người dùng: Chọn bản ghi cần chỉnh sửa trên bảng và nhấn Sửa.

3. Hệ thống: Tải dữ liệu bản ghi và hiển thị biểu mẫu chỉnh sửa.

4. Người dùng: Thay đổi các thông tin cần thiết và nhấn Xác nhận cập nhật.

5. Hệ thống: Kiểm tra tính hợp lệ của dữ liệu mới.

Decision 1:
- Điều kiện: Dữ liệu sửa không hợp lệ?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Lưu các thay đổi vào bảng CUSTOMERS.

7. Hệ thống: Làm mới danh sách và thông báo cập nhật thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + CUSTOMERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CustomerView cập nhật thông tin khách hàng trong bảng CUSTOMERS qua CustomersSql.updateCustomer.

--------------------------------------------------
## UC49 / 3.2.49 - Xóa mềm khách hàng/hội viên

### 1. Mục tiêu ngắn gọn
Cho phép ngừng hoạt động hồ sơ khách hàng nhưng vẫn giữ lịch sử mua hàng.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CustomerView (Nút Xóa)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CustomerView.java)
- DAO: [CustomersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/CustomersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- CUSTOMERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn bản ghi cần xóa trong danh sách hiển thị
- Nhấn nút chức năng Xóa bản ghi
- Xác nhận xóa tại hộp thoại cảnh báo của hệ thống

Swimlane: Hệ thống
- Hiển thị hộp thoại cảnh báo và yêu cầu xác nhận xóa
- Kiểm tra ràng buộc dữ liệu liên quan đến bản ghi cần xóa
- Đánh dấu ẩn bản ghi trong hệ thống
- Làm mới danh sách và thông báo xóa thành công

Swimlane: Cơ sở dữ liệu
- CUSTOMERS: Cập nhật trường is_deleted = 1 (hoặc thay đổi trạng thái hoạt động)
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CustomerView.

2. Người dùng: Chọn bản ghi trong danh sách và chọn Xóa.

3. Hệ thống: Hiển thị cảnh báo ảnh hưởng nghiệp vụ và yêu cầu xác nhận.

4. Người dùng: Xác nhận xóa bản ghi.

5. Hệ thống: Kiểm tra các ràng buộc liên quan (khóa ngoại).

Decision 1:
- Điều kiện: Có ràng buộc nghiệp vụ ngăn cản xóa?
- Nếu Có: Hệ thống: Hiển thị cảnh báo và dừng xóa.
- Nếu Không: Đi tiếp bước tiếp theo.

6. Cơ sở dữ liệu: Cập nhật flag is_deleted = 1 trong bảng CUSTOMERS.

7. Hệ thống: Làm mới danh sách và thông báo xóa thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + CUSTOMERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CustomerView gọi CustomersSql.deleteCustomer cập nhật is_deleted = 1 trong CUSTOMERS.

--------------------------------------------------
## UC50 / 3.2.50 - Tra cứu khách hàng/hội viên

### 1. Mục tiêu ngắn gọn
Cho phép tìm kiếm khách hàng theo tên, số điện thoại, email hoặc hạng thành viên.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [CustomerView / CustomerAnalyticsPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/CustomerView / CustomerAnalyticsPanel.java)
- DAO: [CustomersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/CustomersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- CUSTOMERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- CUSTOMERS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện CustomerView / CustomerAnalyticsPanel.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng CUSTOMERS.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + CUSTOMERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: CustomerView lọc danh sách khách hàng theo tên, số điện thoại hoặc hạng thành viên.

--------------------------------------------------
## UC51 / 3.2.51 - Bán hàng POS

### 1. Mục tiêu ngắn gọn
Cho phép Thu ngân tìm sản phẩm, thêm vào giỏ hàng, chọn khách hàng, chọn phương thức thanh toán và hoàn tất giao dịch.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SellPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SellPanel.java)
- DAO: [ProductsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/ProductsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PRODUCTS
- CUSTOMERS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- INVENTORY: kiểm tra số lượng có sẵn
- PROMOTIONS: lấy thông tin khuyến mãi

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở màn hình Bán hàng POS
- Quét mã vạch sản phẩm hoặc chọn sản phẩm từ danh sách hiển thị
- Điều chỉnh số lượng mua hoặc xóa sản phẩm khỏi giỏ hàng nếu cần
- Nhập thông tin số điện thoại khách hàng thành viên (nếu có)
- Chọn áp dụng chương trình khuyến mãi và nhấn nút Thanh toán

Swimlane: Hệ thống
- Hiển thị giao diện POS bán hàng
- Lấy thông tin sản phẩm, đơn giá bán và kiểm tra số lượng tồn kho có sẵn
- Tính toán tổng tiền tạm tính, chiết khấu khuyến mãi tự động dựa trên giỏ hàng
- Hiển thị thông tin tích điểm, hạng thành viên của khách hàng được chọn
- Chuyển thông tin giỏ hàng sang màn hình thanh toán hóa đơn

Swimlane: Cơ sở dữ liệu
- PRODUCTS: Truy vấn thông tin sản phẩm và giá bán cấu hình
- CUSTOMERS: Truy vấn thông tin số điện thoại, điểm tích lũy và hạng thành viên
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SellPanel.

2. Người dùng: Quét mã vạch sản phẩm hoặc nhập tên sản phẩm tìm kiếm.

3. Hệ thống: Tìm kiếm thông tin sản phẩm và hiển thị lên giỏ hàng POS.

4. Người dùng: Nhập số điện thoại khách hàng để tra cứu hội viên tích điểm.

5. Hệ thống: Tra cứu thông tin khách hàng, tính toán chiết khấu khuyến mãi tự động.

Decision 1:
- Điều kiện: Sản phẩm trong giỏ hàng vượt quá số lượng tồn kho?
- Nếu Có: Hệ thống: Hiển thị cảnh báo số lượng không đủ.
- Nếu Không: Người dùng nhấn nút Thanh toán.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PRODUCTS, CUSTOMERS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SellPanel hỗ trợ nhân viên bán hàng thêm sản phẩm vào giỏ hàng, chọn khách hàng, chọn chương trình khuyến mãi tự động.

--------------------------------------------------
## UC52 / 3.2.52 - Thanh toán hóa đơn

### 1. Mục tiêu ngắn gọn
Cho phép Thu ngân xác nhận thanh toán; hệ thống kiểm tra tồn kho, tạo hóa đơn, tạo chi tiết hóa đơn, trừ kho và cập nhật thông tin khách hàng.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [SellPanel (Nút Thanh toán)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/SellPanel.java)
- DAO: [OrdersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/OrdersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ORDERS
- ORDER_DETAILS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- INVENTORY: trừ số lượng tồn kho
- CUSTOMERS: cập nhật tích điểm và tổng chi tiêu
- CASH_PAYMENT / BANK_TRANSFER_PAYMENT: lưu thông tin thanh toán tương ứng

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Kiểm tra lại tổng tiền hóa đơn trên màn hình thanh toán
- Chọn Phương thức thanh toán (Tiền mặt hoặc Chuyển khoản ngân hàng)
- Nhập số tiền khách đưa (nếu dùng tiền mặt) hoặc quét mã QR chuyển khoản
- Xác nhận hoàn tất thanh toán hóa đơn
- Nhận hóa đơn bán hàng in ra từ hệ thống

Swimlane: Hệ thống
- Kiểm tra lại tồn kho thực tế của các sản phẩm trong giỏ hàng một lần nữa
- Thực hiện lưu thông tin hóa đơn và lưu chi tiết danh sách sản phẩm mua
- Cập nhật trừ số lượng tồn kho của sản phẩm tại chi nhánh tương ứng
- Tính tích điểm mới cho khách hàng thành viên và cập nhật tổng chi tiêu
- Tạo file PDF hóa đơn và gửi lệnh in hóa đơn bán hàng
- Làm trống giỏ hàng hiện hành và sẵn sàng cho giao dịch tiếp theo

Swimlane: Cơ sở dữ liệu
- ORDERS: Lưu thông tin hóa đơn (khách hàng, tổng tiền, phương thức thanh toán)
- ORDER_DETAILS: Lưu chi tiết danh sách sản phẩm, số lượng, giá bán của hóa đơn
- INVENTORY: Cập nhật (trừ bớt) số lượng tồn kho của các sản phẩm trong hóa đơn
- CUSTOMERS: Cập nhật cộng điểm tích lũy và tăng tổng tiền chi tiêu của khách hàng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện SellPanel.

2. Người dùng: Chọn phương thức thanh toán (Tiền mặt / Chuyển khoản) và xác nhận thanh toán.

3. Hệ thống: Kiểm tra tồn kho của sản phẩm trước khi tạo hóa đơn.

Decision 1:
- Điều kiện: Có sản phẩm bị hết hàng trong lúc chờ thanh toán?
- Nếu Có: Hệ thống: Hiển thị thông báo và quay lại giỏ hàng.
- Nếu Không: Đi tiếp bước tiếp theo.

4. Cơ sở dữ liệu: Lưu hóa đơn vào ORDERS, chi tiết hóa đơn vào ORDER_DETAILS.

5. Cơ sở dữ liệu: Cập nhật trừ tồn kho trong INVENTORY và cộng điểm tích lũy trong CUSTOMERS.

6. Hệ thống: In hóa đơn bán hàng cho khách hàng và hoàn tất giao dịch bán lẻ.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ORDERS, ORDER_DETAILS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: SellPanel thực hiện thanh toán, tạo record trong ORDERS và ORDER_DETAILS, cập nhật điểm CUSTOMERS, trừ tồn kho trong INVENTORY.

--------------------------------------------------
## UC53 / 3.2.53 - Quản lý hóa đơn

### 1. Mục tiêu ngắn gọn
Cho phép người dùng có quyền xem danh sách hóa đơn, lọc theo ngày, xem chi tiết, cập nhật trạng thái và xuất hóa đơn.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [OrderView / OrderDetailDialog](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/OrderView / OrderDetailDialog.java)
- DAO: [OrdersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/OrdersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ORDERS
- ORDER_DETAILS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- CUSTOMERS: thông tin người mua

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở chức năng tra cứu hoặc lọc dữ liệu
- Nhập từ khóa tìm kiếm hoặc chọn các tiêu chí bộ lọc
- Nhấn nút Tìm kiếm

Swimlane: Hệ thống
- Tiếp nhận từ khóa và bộ lọc dữ liệu
- Kiểm tra định dạng điều kiện lọc và thực hiện truy vấn
- Hiển thị danh sách kết quả phù hợp lên bảng dữ liệu

Swimlane: Cơ sở dữ liệu
- ORDERS: Thực hiện câu lệnh SELECT với điều kiện WHERE tương ứng bộ lọc
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện OrderView / OrderDetailDialog.

2. Người dùng: Nhập từ khóa tìm kiếm hoặc chọn điều kiện bộ lọc.

3. Người dùng: Nhấn nút chức năng Tìm kiếm/Tra cứu.

4. Hệ thống: Tiếp nhận các điều kiện lọc và thực hiện truy vấn.

5. Cơ sở dữ liệu: Truy vấn dữ liệu phù hợp từ bảng ORDERS, ORDER_DETAILS.

6. Hệ thống: Hiển thị danh sách kết quả lên bảng biểu đồ giao diện.

Decision 1:
- Điều kiện: Không tìm thấy kết quả phù hợp?
- Nếu Có: Hệ thống: Thông báo không tìm thấy dữ liệu.
- Nếu Không: Người dùng xem chi tiết bản ghi.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ORDERS, ORDER_DETAILS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: OrderView hiển thị danh sách hóa đơn bán lẻ, khi nhấn xem chi tiết sẽ mở OrderDetailDialog hiển thị chi tiết hóa đơn.

--------------------------------------------------
## UC54 / 3.2.54 - Hủy hóa đơn và hoàn kho

### 1. Mục tiêu ngắn gọn
Cho phép hủy hóa đơn, ghi lý do hủy, hoàn lại tồn kho và ghi nhận thao tác hủy vào nhật ký hệ thống.

### 2. Tác nhân chính
- Nhân viên bán hàng / Thu ngân

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [OrderView (Nút Hủy hóa đơn)](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/OrderView.java)
- DAO: [OrdersSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/OrdersSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ORDERS
- INVENTORY

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- INVENTORY_TRANSACTIONS: ghi nhận giao dịch hoàn kho CANCEL

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Tra cứu và chọn hóa đơn cần hủy trong danh sách hóa đơn
- Nhấn nút Yêu cầu hủy hóa đơn
- Nhập lý do hủy hóa đơn và xác nhận thao tác

Swimlane: Hệ thống
- Hiển thị thông tin chi tiết hóa đơn được chọn
- Kiểm tra quyền hạn của người dùng (chỉ cho phép Thu ngân/Quản lý) và trạng thái hóa đơn
- Cập nhật trạng thái hóa đơn thành Hủy (CANCELLED) trong hệ thống
- Thực hiện hoàn lại số lượng sản phẩm của hóa đơn bị hủy vào kho chi nhánh
- Ghi nhận lịch sử giao dịch biến động kho loại hoàn trả hàng
- Thông báo hủy hóa đơn và hoàn kho thành công

Swimlane: Cơ sở dữ liệu
- ORDERS: Cập nhật trạng thái hóa đơn (status) thành 'CANCELLED'
- INVENTORY: Cập nhật (cộng trả lại) số lượng các sản phẩm của hóa đơn vào kho chi nhánh
- INVENTORY_TRANSACTIONS: Ghi nhận lịch sử giao dịch hoàn kho loại 'CANCEL'
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện OrderView.

2. Người dùng: Tra cứu danh sách hóa đơn, chọn hóa đơn cần hủy và nhấn Hủy hóa đơn.

3. Người dùng: Nhập lý do hủy hóa đơn nghiệp vụ và xác nhận.

4. Hệ thống: Kiểm tra trạng thái hóa đơn có được phép hủy.

Decision 1:
- Điều kiện: Hóa đơn đã được thanh toán quá thời gian quy định?
- Nếu Có: Hệ thống: Thông báo không được phép hủy.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Cập nhật status thành 'CANCELLED' trong ORDERS.

6. Cơ sở dữ liệu: Cộng trả lại số lượng sản phẩm vào INVENTORY và ghi giao dịch hoàn kho vào INVENTORY_TRANSACTIONS.

7. Hệ thống: Làm mới danh sách hóa đơn và thông báo hủy thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ORDERS, INVENTORY
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: OrderView hỗ trợ hủy hóa đơn, cập nhật status = 'CANCELLED' trong ORDERS, cộng trả tồn kho trong INVENTORY.

--------------------------------------------------
## UC55 / 3.2.55 - Báo cáo và thống kê

### 1. Mục tiêu ngắn gọn
Cho phép Quản lý cửa hàng/Admin xem doanh thu, đơn hàng, sản phẩm bán chạy, tồn kho thấp và hiệu suất nhân viên.

### 2. Tác nhân chính
- Quản lý cửa hàng

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [StatisticView](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/StatisticView.java)
- DAO: [StatisticSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/StatisticSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ORDERS
- INVENTORY
- EMPLOYEES

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- ORDER_DETAILS: tính doanh số chi tiết

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở màn hình Thống kê báo cáo
- Chọn loại báo cáo cần xem (Doanh thu, Tồn kho, Hiệu suất nhân viên)
- Chọn khoảng thời gian thống kê (ngày, tuần, tháng, năm)
- Nhấn Thống kê dữ liệu hoặc chọn nút Xuất báo cáo (Excel/PDF)

Swimlane: Hệ thống
- Hiển thị giao diện bộ lọc thống kê và báo cáo
- Truy vấn dữ liệu tổng hợp theo khoảng thời gian đã chọn
- Tính toán doanh số, lợi nhuận, thống kê sản phẩm bán chạy nhất
- Hiển thị biểu đồ tăng trưởng và bảng tổng hợp số liệu tương ứng
- Tạo file Excel/PDF báo cáo khi người dùng nhấn nút Xuất dữ liệu
- Thông báo xuất báo cáo thành công

Swimlane: Cơ sở dữ liệu
- ORDERS: Truy vấn tổng hợp doanh thu và số lượng đơn hàng theo thời gian
- INVENTORY: Truy vấn số lượng tồn kho hiện hành để lập báo cáo tồn kho
- EMPLOYEES: Truy vấn doanh số bán hàng của từng nhân viên để báo cáo hiệu suất
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện StatisticView.

2. Người dùng: Chọn kỳ báo cáo, khoảng thời gian lọc và nhấn nút Thống kê.

3. Hệ thống: Nhận điều kiện lọc và thực hiện tính toán số liệu thống kê.

4. Cơ sở dữ liệu: Truy vấn tổng hợp dữ liệu từ ORDERS, INVENTORY và EMPLOYEES.

5. Hệ thống: Hiển thị các chỉ số doanh thu, biểu đồ cột, và danh sách sản phẩm bán chạy.

Decision 1:
- Điều kiện: Người dùng nhấn nút Xuất báo cáo?
- Nếu Có: Hệ thống: Xuất dữ liệu ra Excel/PDF và lưu xuống thiết bị.
- Nếu Không: Người dùng tiếp tục xem số liệu trên màn hình.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + ORDERS, INVENTORY, EMPLOYEES
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: StatisticView tải doanh thu, biểu đồ tăng trưởng, danh sách bán chạy thông qua các hàm tổng hợp SQL (SUM, GROUP BY).

--------------------------------------------------
## UC56 / 3.2.56 - Quản lý khuyến mãi

### 1. Mục tiêu ngắn gọn
Cho phép Admin thêm, sửa, ngừng áp dụng và tra cứu các chương trình khuyến mãi; chức năng đã có một phần trong code.

### 2. Tác nhân chính
- Admin

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [PromotionManagementPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/PromotionManagementPanel.java)
- DAO: [PromotionsSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/promotion/PromotionsSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- PROMOTIONS
- PROMOTION_CAMPAIGNS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Chọn chức năng Quản lý chương trình khuyến mãi
- Chọn Thêm chương trình khuyến mãi mới
- Nhập Tên chương trình, chọn chiến dịch, thiết lập điều kiện áp dụng, tỷ lệ giảm giá
- Chọn ngày bắt đầu và ngày kết thúc chương trình
- Xác nhận lưu chương trình khuyến mãi

Swimlane: Hệ thống
- Hiển thị biểu mẫu cấu hình chương trình khuyến mãi
- Kiểm tra điều kiện ngày kết thúc phải sau ngày bắt đầu, mức giảm giá hợp lệ
- Tạo mới chương trình khuyến mãi trong hệ thống và thông báo kết quả

Swimlane: Cơ sở dữ liệu
- PROMOTION_CAMPAIGNS: Lưu thông tin chiến dịch khuyến mãi tổng quát
- PROMOTIONS: Lưu chi tiết chương trình khuyến mãi và điều kiện áp dụng cụ thể
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện PromotionManagementPanel.

2. Người dùng: Chọn Thêm khuyến mãi mới và nhập thông tin chương trình.

3. Người dùng: Chọn chiến dịch khuyến mãi liên kết, thiết lập điều kiện giảm giá, nhấn Lưu.

4. Hệ thống: Kiểm tra tính hợp lệ của thời gian áp dụng và mức chiết khấu.

Decision 1:
- Điều kiện: Ngày kết thúc sớm hơn ngày bắt đầu?
- Nếu Có: Hệ thống: Hiển thị thông báo lỗi.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Lưu chương trình khuyến mãi vào PROMOTIONS và PROMOTION_CAMPAIGNS.

6. Hệ thống: Làm mới danh sách khuyến mãi và thông báo lưu thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + PROMOTIONS, PROMOTION_CAMPAIGNS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: PromotionManagementPanel quản lý các chiến dịch khuyến mãi và mã giảm giá áp dụng khi bán hàng.

--------------------------------------------------
## UC57 / 3.2.57 - Cấu hình cá nhân/hệ thống

### 1. Mục tiêu ngắn gọn
Cho phép người dùng xem hoặc cập nhật một số thiết lập cá nhân và cấu hình chung của hệ thống.

### 2. Tác nhân chính
- Admin; Quản lý cửa hàng; Nhân viên bán hàng / Thu ngân; Nhân viên kho

### 3. Màn hình / class liên quan trong code
- View/Form/Panel: [UnifiedSettingsPanel](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/view/UnifiedSettingsPanel.java)
- DAO: [AccountSql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/src/main/java/business/sql/sales_order/AccountSql.java)
- File SQL/schema: [KhoiTaoCacBang.sql](file:///d:/UIT/HocTrenTruong/HK4/Lap_Trinh_Java_IS216/DoAn/SieuThiOnline_Java/database/KhoiTaoCacBang.sql)

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- SYSTEM_CONFIG
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- Không có

### 6. Luồng Activity Diagram đã đơn giản hóa
```markdown
Swimlane: Người dùng
- Mở màn hình cấu hình cá nhân hoặc hệ thống
- Thực hiện cập nhật mật khẩu mới hoặc thay đổi các cấu hình hệ thống chung
- Xác nhận lưu cấu hình thiết lập

Swimlane: Hệ thống
- Hiển thị giao diện cấu hình hệ thống và thông tin cá nhân
- Kiểm tra tính hợp lệ của mật khẩu mới hoặc định dạng giá trị cấu hình thay đổi
- Thực hiện cập nhật thông tin trong hệ thống
- Thông báo lưu cấu hình thành công

Swimlane: Cơ sở dữ liệu
- ACCOUNTS: Cập nhật mật khẩu mới của tài khoản cá nhân nếu có đổi mật khẩu
- SYSTEM_CONFIG: Cập nhật các giá trị cấu hình hệ thống chung tương ứng
```

### 7. Luồng vẽ đề xuất
1. Người dùng: Chọn chức năng thực hiện trên giao diện UnifiedSettingsPanel.

2. Người dùng: Chọn thay đổi mật khẩu tài khoản hoặc điều chỉnh các thiết lập hệ thống.

3. Người dùng: Nhập mật khẩu cũ, mật khẩu mới hoặc điền giá trị cấu hình mới và chọn Lưu.

4. Hệ thống: Kiểm tra tính hợp lệ của thông tin thay đổi.

Decision 1:
- Điều kiện: Mật khẩu cũ không chính xác?
- Nếu Có: Hệ thống: Thông báo lỗi mật khẩu cũ.
- Nếu Không: Đi tiếp bước tiếp theo.

5. Cơ sở dữ liệu: Cập nhật mật khẩu mới vào ACCOUNTS hoặc lưu giá trị cấu hình vào SYSTEM_CONFIG.

6. Hệ thống: Thông báo lưu thiết lập thành công.

End

### 8. Ghi chú vẽ
- Số swimlane: Người dùng | Hệ thống | Cơ sở dữ liệu
- Bảng nên đặt trong lane Cơ sở dữ liệu:
  + SYSTEM_CONFIG, ACCOUNTS
- Các nhánh lỗi nên quay lại bước: Chọn lại chức năng hoặc nhập lại thông tin biểu mẫu ở các bước đầu.
- Các bước có thể gộp: Các bước hiển thị biểu mẫu và tải dữ liệu cũ có thể hiển thị song song.

### 9. Mức độ bám code
- Có trong code: UnifiedSettingsPanel cho phép người dùng đổi mật khẩu tài khoản trong ACCOUNTS và cấu hình chung hệ thống trong SYSTEM_CONFIG.


---

## BẢNG TỔNG HỢP MAPPING BẢNG CSDL VÀ CLASS CODE

| Mã UC | Tên Use-case | Màn hình (Swing View) | Class SQL/DAO | Bảng CSDL hiển thị trên AD | Bảng CSDL liên quan khác |
|-------|--------------|-----------------------|---------------|----------------------------|--------------------------|
| UC02 | Đăng xuất tài khoản | `Sidebar / AdminSidebar` | `AccountSql` | **LOGIN_HISTORY, ACCOUNTS** | TOKENS |
| UC03 | Kích hoạt tài khoản nhân viên | `RegisterView` | `ActivationTokenSql` | **ACTIVATION_TOKENS, ACCOUNTS** | EMPLOYEES, USERS |
| UC04 | Khôi phục mật khẩu | `ForgotPasswordView` | `AccountSql` | **OTP_STORAGE, ACCOUNTS** | Không có |
| UC05 | Quản lý tài khoản và phân quyền | `LoginManagementPanel, AccountRoleAssignmentPanel, RoleManagementPanel, CreateAccountPanel` | `AccountSql` | **ACCOUNTS** | ROLES, ROLE_GROUPS |
| UC06 | Thêm tài khoản nhân viên | `CreateAccountPanel` | `AccountSql` | **ACCOUNTS, USERS** | EMPLOYEES |
| UC07 | Cập nhật tài khoản nhân viên | `LoginManagementPanel` | `AccountSql` | **ACCOUNTS, USERS** | Không có |
| UC08 | Khóa/Mở khóa tài khoản | `LoginManagementPanel` | `AccountSql` | **ACCOUNTS, AUDIT_LOG** | Không có |
| UC09 | Gán vai trò và quyền hạn | `AccountRoleAssignmentPanel, RoleManagementPanel` | `AccountAssignRoleSql` | **ACCOUNT_ASSIGN_ROLE, ACCOUNT_ASSIGN_ROLE_GROUP** | ROLES, ROLE_GROUPS |
| UC10 | Tra cứu tài khoản | `LoginManagementPanel` | `AccountSql` | **ACCOUNTS** | USERS |
| UC11 | Theo dõi lịch sử truy cập và nhật ký hệ thống | `AuditLogPanel` | `LoginHistorySql` | **LOGIN_HISTORY, AUDIT_LOG** | Không có |
| UC12 | Quản lý chi nhánh | `StoreManagementPanel` | `StoresSql` | **STORES** | Không có |
| UC13 | Thêm chi nhánh | `StoreManagementPanel` | `StoresSql` | **STORES** | Không có |
| UC14 | Cập nhật chi nhánh | `StoreManagementPanel` | `StoresSql` | **STORES** | Không có |
| UC15 | Xóa mềm chi nhánh | `StoreManagementPanel` | `StoresSql` | **STORES** | Không có |
| UC16 | Tra cứu chi nhánh | `StoreManagementPanel` | `StoresSql` | **STORES** | Không có |
| UC17 | Quản lý cửa hàng trưởng | `ManagerManagementView` | `StoreManagerSql` | **EMPLOYEES, STORES** | Không có |
| UC18 | Phân công cửa hàng trưởng | `ManagerManagementView` | `StoreManagerSql` | **EMPLOYEES, STORES** | Không có |
| UC19 | Quản lý nhân viên | `EmployeeView` | `EmployeeSql` | **EMPLOYEES** | Không có |
| UC20 | Thêm nhân viên | `EmployeeView` | `EmployeeSql` | **EMPLOYEES** | STORES, SHIFTS |
| UC21 | Cập nhật nhân viên | `EmployeeView` | `EmployeeSql` | **EMPLOYEES** | Không có |
| UC22 | Xóa mềm nhân viên | `EmployeeView` | `EmployeeSql` | **EMPLOYEES** | Không có |
| UC23 | Tra cứu nhân viên | `EmployeeView` | `EmployeeSql` | **EMPLOYEES** | Không có |
| UC24 | Phân ca nhân viên | `EmployeeView` | `EmployeeShiftSql` | **EMPLOYEE_SHIFTS** | SHIFTS, EMPLOYEES |
| UC25 | Lập ca làm việc | `EmployeeView` | `EmployeeShiftSql` | **EMPLOYEE_SHIFTS** | SHIFTS, EMPLOYEES |
| UC26 | Cập nhật ca làm việc | `EmployeeView` | `EmployeeShiftSql` | **EMPLOYEE_SHIFTS** | Không có |
| UC27 | Hủy ca làm việc | `EmployeeView` | `EmployeeShiftSql` | **EMPLOYEE_SHIFTS** | Không có |
| UC28 | Tra cứu ca làm việc | `EmployeeView` | `EmployeeShiftSql` | **EMPLOYEE_SHIFTS** | SHIFTS, EMPLOYEES |
| UC29 | Quản lý KPI nhân viên | `EmployeePerformancePanel, ImportKpiDialog` | `KpiEvaluationSql` | **KPI_EVALUATION, EMPLOYEE_KPI_HISTORY** | KPI_CRITERIA, EMPLOYEES |
| UC30 | Quản lý sản phẩm | `ProductView` | `ProductsSql` | **PRODUCTS** | Không có |
| UC31 | Thêm sản phẩm | `ProductView` | `ProductsSql` | **PRODUCTS, STORE_PRODUCTS** | CATEGORIES, SUPPLIERS, UNITS |
| UC32 | Cập nhật sản phẩm | `ProductView` | `ProductsSql` | **PRODUCTS, STORE_PRODUCTS** | Không có |
| UC33 | Xóa mềm sản phẩm | `ProductView` | `ProductsSql` | **PRODUCTS** | STORE_PRODUCTS |
| UC34 | Tra cứu sản phẩm | `ProductView` | `ProductsSql` | **PRODUCTS, INVENTORY** | Không có |
| UC35 | Cấu hình đơn vị tính sản phẩm | `ProductView` | `ProductUnitsSql` | **PRODUCT_UNITS, UNITS** | PRODUCTS |
| UC36 | Quản lý danh mục và thuế VAT | `CategoryTaxView` | `CategoriesSql` | **CATEGORIES** | Không có |
| UC37 | Quản lý nhà cung cấp | `SupplierManagementView` | `SuppliersSql` | **SUPPLIERS** | Không có |
| UC38 | Thêm nhà cung cấp | `SupplierManagementView` | `SuppliersSql` | **SUPPLIERS** | Không có |
| UC39 | Cập nhật nhà cung cấp | `SupplierManagementView` | `SuppliersSql` | **SUPPLIERS** | Không có |
| UC40 | Xóa mềm nhà cung cấp | `SupplierManagementView` | `SuppliersSql` | **SUPPLIERS** | Không có |
| UC41 | Tra cứu nhà cung cấp | `SupplierManagementView` | `SuppliersSql` | **SUPPLIERS** | Không có |
| UC42 | Quản lý tồn kho | `InventoryView` | `InventorySql` | **INVENTORY, STORE_PRODUCTS** | PRODUCTS |
| UC43 | Nhập hàng và lập phiếu nhập | `StockImportReceiptDialog / PurchaseReceiptInvoiceDialog` | `PurchaseReceiptSql` | **PURCHASE_RECEIPTS, PURCHASE_RECEIPT_DETAILS** | INVENTORY, INVENTORY_TRANSACTIONS |
| UC44 | Xem lịch sử biến động kho | `InventoryHistoryDialog` | `InventoryTransactionSql` | **INVENTORY_TRANSACTIONS** | PRODUCTS, PURCHASE_RECEIPTS |
| UC45 | Gửi và xử lý cảnh báo tồn kho | `WarehouseDashboardView / AdminDashboardView` | `InventoryNotificationSql` | **INVENTORY_NOTIFICATIONS** | PRODUCTS, INVENTORY |
| UC46 | Quản lý khách hàng/hội viên | `CustomerView` | `CustomersSql` | **CUSTOMERS** | Không có |
| UC47 | Thêm khách hàng/hội viên | `CustomerView` | `CustomersSql` | **CUSTOMERS** | Không có |
| UC48 | Cập nhật khách hàng/hội viên | `CustomerView` | `CustomersSql` | **CUSTOMERS** | Không có |
| UC49 | Xóa mềm khách hàng/hội viên | `CustomerView` | `CustomersSql` | **CUSTOMERS** | Không có |
| UC50 | Tra cứu khách hàng/hội viên | `CustomerView / CustomerAnalyticsPanel` | `CustomersSql` | **CUSTOMERS** | Không có |
| UC51 | Bán hàng POS | `SellPanel` | `ProductsSql` | **PRODUCTS, CUSTOMERS** | INVENTORY, PROMOTIONS |
| UC52 | Thanh toán hóa đơn | `SellPanel` | `OrdersSql` | **ORDERS, ORDER_DETAILS** | INVENTORY, CUSTOMERS, CASH_PAYMENT / BANK_TRANSFER_PAYMENT |
| UC53 | Quản lý hóa đơn | `OrderView / OrderDetailDialog` | `OrdersSql` | **ORDERS, ORDER_DETAILS** | CUSTOMERS |
| UC54 | Hủy hóa đơn và hoàn kho | `OrderView` | `OrdersSql` | **ORDERS, INVENTORY** | INVENTORY_TRANSACTIONS |
| UC55 | Báo cáo và thống kê | `StatisticView` | `StatisticSql` | **ORDERS, INVENTORY, EMPLOYEES** | ORDER_DETAILS |
| UC56 | Quản lý khuyến mãi | `PromotionManagementPanel` | `PromotionsSql` | **PROMOTIONS, PROMOTION_CAMPAIGNS** | Không có |
| UC57 | Cấu hình cá nhân/hệ thống | `UnifiedSettingsPanel` | `AccountSql` | **SYSTEM_CONFIG, ACCOUNTS** | Không có |

---

## BÁO CÁO TRẠNG THÁI VÀ KẾT QUẢ THỰC HIỆN

1. **Tổng số Use-cases được đặc tả**: 56 Use-cases (UC02 đến UC57).
2. **Số lượng Use-cases khớp hoàn toàn với code**: 56/56.
3. **Danh sách các bảng CSDL Oracle đã xác minh và ánh xạ**:
   - `USERS`: Thông tin cá nhân của người dùng hệ thống.
   - `ACCOUNTS`: Tài khoản đăng nhập, trạng thái khóa/mở và mật khẩu băm.
   - `LOGIN_HISTORY`: Nhật ký thời điểm đăng nhập, đăng xuất và địa chỉ IP.
   - `AUDIT_LOG`: Nhật ký các thao tác tạo mới, cập nhật, xóa trên toàn hệ thống.
   - `STORES`: Quản lý các chi nhánh/cửa hàng siêu thị.
   - `EMPLOYEES`: Thông tin hồ sơ nhân sự, phân bổ cửa hàng và vai trò.
   - `SHIFTS`: Định nghĩa các ca làm việc của siêu thị.
   - `EMPLOYEE_SHIFTS`: Lịch phân ca chi tiết cho từng nhân viên theo ngày.
   - `ACTIVATION_TOKENS`: Lưu mã kích hoạt cấp cho nhân viên để kích hoạt tài khoản.
   - `KPI_EVALUATION`: Đánh giá kết quả các tiêu chí KPI của nhân viên.
   - `EMPLOYEE_KPI_HISTORY`: Lịch sử tổng hợp điểm hiệu suất làm việc.
   - `PRODUCTS`: Thông tin sản phẩm toàn hệ thống.
   - `STORE_PRODUCTS`: Thiết lập giá bán, tồn kho tối thiểu của sản phẩm tại chi nhánh.
   - `INVENTORY`: Số lượng hàng tồn kho thực tế ở mỗi chi nhánh.
   - `PRODUCT_UNITS`: Cấu hình đơn vị quy đổi và tỷ lệ quy đổi của sản phẩm.
   - `UNITS`: Danh mục các đơn vị tính.
   - `CATEGORIES`: Danh mục phân loại sản phẩm và thiết lập VAT mặc định.
   - `SUPPLIERS`: Thông tin các nhà cung cấp hàng hóa.
   - `PURCHASE_RECEIPTS`: Thông tin chung của phiếu nhập kho hàng hóa.
   - `PURCHASE_RECEIPT_DETAILS`: Chi tiết danh sách sản phẩm nhập kho, giá nhập và VAT.
   - `INVENTORY_TRANSACTIONS`: Nhật ký biến động kho (Nhập, xuất, điều chỉnh, hoàn trả).
   - `INVENTORY_NOTIFICATIONS`: Thông báo cảnh báo tồn kho thấp cho nhân viên.
   - `CUSTOMERS`: Thông tin hội viên, hạng thành viên, tích điểm chi tiêu.
   - `ORDERS`: Hóa đơn bán lẻ phát sinh tại quầy POS.
   - `ORDER_DETAILS`: Chi tiết giỏ hàng của từng hóa đơn.
   - `PROMOTIONS`: Chương trình khuyến mãi, giảm giá được áp dụng.
   - `PROMOTION_CAMPAIGNS`: Chiến dịch khuyến mãi lớn của siêu thị.
   - `OTP_STORAGE`: Lưu mã OTP gửi qua email phục vụ lấy lại mật khẩu.
   - `SYSTEM_CONFIG`: Cấu hình tham số vận hành chung của ứng dụng.
