# Activity Diagram Specs Batch 01

Phạm vi: UC02 đến UC05 trong `docs/phan_3_2_dac_ta_usecase_activity.txt`, đối chiếu với code Java Swing, lớp SQL/DAO và schema Oracle hiện có. Luồng dưới đây đã được rút gọn để vẽ Activity Diagram 3 swimlane: Người dùng, Hệ thống, Cơ sở dữ liệu.

--------------------------------------------------

## UC02 / 3.2.2 - Đăng xuất hệ thống

### 1. Mục tiêu ngắn gọn
Cho phép người dùng kết thúc phiên làm việc hiện tại và quay về màn hình đăng nhập. Hệ thống cập nhật trạng thái tài khoản và ghi nhận lịch sử đăng xuất ở mức nghiệp vụ.

### 2. Tác nhân chính
- Admin
- Quản lý cửa hàng
- Nhân viên bán hàng / Thu ngân
- Nhân viên kho

### 3. Code liên quan đã tìm thấy
- Màn hình/View/Panel: `AdminDashboardView`, `DashboardView`, `WarehouseDashboardView`, `AdminSidebar`, `Sidebar`, `WarehouseSidebar`
- Controller/Service nếu có: `LoginService`, `AccountService`, `SessionManager`, `HeartbeatService`
- DAO/SQL class: `AccountSql`, `LoginHistorySql`, `TokenSql`
- File SQL/schema liên quan: `database/KhoiTaoCacBang.sql`

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS
- LOGIN_HISTORY

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- TOKENS: có thao tác thu hồi thông tin đăng nhập, nhưng đây là chi tiết kỹ thuật phiên đăng nhập, không nên đưa vào flow nghiệp vụ.
- AUDIT_LOG: có màn hình nhật ký hệ thống riêng, nhưng luồng đăng xuất chính trong code bám `LOGIN_HISTORY` rõ hơn.

### 6. Luồng Activity Diagram đã đơn giản hóa

Start

1. Hệ thống: Hiển thị dashboard theo vai trò người dùng.
2. Người dùng: Chọn chức năng đăng xuất trên sidebar.
3. Hệ thống: Hiển thị hộp thoại xác nhận đăng xuất.
4. Người dùng: Xác nhận hoặc hủy thao tác.

Decision 1:
- Điều kiện: Người dùng xác nhận đăng xuất?
- Nếu Không: Hệ thống đóng hộp thoại và giữ nguyên màn hình hiện tại.
- Nếu Có: Hệ thống tiếp tục xử lý đăng xuất.

5. Hệ thống: Kết thúc trạng thái làm việc của người dùng hiện tại.
6. Cơ sở dữ liệu: ACCOUNTS - cập nhật trạng thái tài khoản sang offline và thời điểm đăng xuất.
7. Cơ sở dữ liệu: LOGIN_HISTORY - ghi nhận sự kiện đăng xuất thành công.
8. Hệ thống: Xóa thông tin phiên làm việc khỏi ứng dụng.
9. Hệ thống: Đóng dashboard hiện tại.
10. Hệ thống: Mở lại màn hình đăng nhập.

End

### 7. Gợi ý vẽ theo swimlane

Người dùng:
- Chọn Đăng xuất.
- Xác nhận đăng xuất.

Hệ thống:
- Hiển thị xác nhận.
- Kết thúc trạng thái làm việc.
- Xóa thông tin phiên trong ứng dụng.
- Điều hướng về màn hình đăng nhập.

Cơ sở dữ liệu:
- ACCOUNTS: cập nhật trạng thái offline và thời điểm đăng xuất.
- LOGIN_HISTORY: lưu lịch sử đăng xuất.

### 8. Ghi chú bám code
- Có trong code:
  + `AdminDashboardView`, `DashboardView`, `WarehouseDashboardView` đều có xử lý menu "Đăng xuất" và mở lại `LoginView`.
  + `AccountService` gọi `AccountSql` để cập nhật trạng thái online/offline theo vai trò.
  + `LoginService` có ghi lịch sử đăng xuất qua `LoginHistorySql`.
- Suy luận nhẹ từ code:
  + Luồng diagram nên gom các biến thể dashboard thành một bước "dashboard theo vai trò" để tránh vẽ lặp.
  + Việc kiểm soát trạng thái phiên là chi tiết kỹ thuật, chỉ cần biểu diễn bằng "kết thúc trạng thái làm việc".
- Chưa tìm thấy / cần kiểm tra:
  + Cảnh báo dữ liệu đang nhập chưa lưu được nêu trong đặc tả nhưng chưa thấy xử lý nghiệp vụ thống nhất ở các dashboard.

### 9. Trạng thái
Tốt

--------------------------------------------------

## UC03 / 3.2.3 - Kích hoạt tài khoản nhân viên

### 1. Mục tiêu ngắn gọn
Cho phép nhân viên dùng mã kích hoạt hợp lệ để thiết lập tài khoản đăng nhập lần đầu. Sau khi thành công, hệ thống tạo tài khoản và đánh dấu mã kích hoạt đã dùng.

### 2. Tác nhân chính
- Nhân viên nhận mã kích hoạt
- Admin hoặc Quản lý là người cấp hồ sơ/mã trước đó

### 3. Code liên quan đã tìm thấy
- Màn hình/View/Panel: `LoginView`, `RegisterView`, `EmployeeView`, `ManagerManagementView`
- Controller/Service nếu có: `AccountActivationAPI`, `AccountActivationService`, `ActivationTokenService`, `EmailService`
- DAO/SQL class: `ActivationTokenSql`, `AccountActivationSql`, `AccountAssignRoleSql`, `AuditLogSql`, một số hàm cũ trong `AccountSql`
- File SQL/schema liên quan: `database/KhoiTaoCacBang.sql`

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACTIVATION_TOKENS
- ACCOUNTS

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- EMPLOYEES: dùng để lấy thông tin nhân viên và vai trò, nhưng nếu đưa vào diagram sẽ làm lane CSDL quá chi tiết.
- USERS: được tạo hoặc kiểm tra để liên kết tài khoản, nhưng không phải trọng tâm nghiệp vụ kích hoạt.
- ACCOUNT_ASSIGN_ROLE: có thao tác gán vai trò mặc định sau khi tạo tài khoản, nhưng nên gộp vào hành động "kích hoạt tài khoản".
- AUDIT_LOG: có ghi nhận sự kiện kích hoạt, nhưng là dữ liệu nhật ký phụ.

### 6. Luồng Activity Diagram đã đơn giản hóa

Start

1. Người dùng: Mở màn hình đăng ký/kích hoạt tài khoản.
2. Người dùng: Nhập mã kích hoạt được cấp.
3. Hệ thống: Kiểm tra mã kích hoạt và hồ sơ nhân viên tương ứng.
4. Cơ sở dữ liệu: ACTIVATION_TOKENS - kiểm tra mã còn hiệu lực và chưa sử dụng.

Decision 1:
- Điều kiện: Mã kích hoạt hợp lệ?
- Nếu Không: Hệ thống thông báo mã không hợp lệ hoặc đã hết hạn.
- Nếu Có: Hệ thống hiển thị thông tin nhân viên để tiếp tục.

5. Người dùng: Nhập tên đăng nhập và mật khẩu ban đầu.
6. Hệ thống: Kiểm tra thông tin tài khoản cần tạo.

Decision 2:
- Điều kiện: Tên đăng nhập hợp lệ và chưa tồn tại?
- Nếu Không: Hệ thống thông báo lỗi và yêu cầu nhập lại.
- Nếu Có: Hệ thống tạo tài khoản mới.

7. Cơ sở dữ liệu: ACCOUNTS - lưu tài khoản đã kích hoạt.
8. Cơ sở dữ liệu: ACTIVATION_TOKENS - đánh dấu mã kích hoạt đã sử dụng.
9. Hệ thống: Thông báo kích hoạt thành công.
10. Hệ thống: Chuyển người dùng về màn hình đăng nhập.

End

### 7. Gợi ý vẽ theo swimlane

Người dùng:
- Mở màn hình kích hoạt.
- Nhập mã kích hoạt.
- Nhập tên đăng nhập và mật khẩu ban đầu.

Hệ thống:
- Kiểm tra mã và hồ sơ nhân viên.
- Hiển thị thông tin nhân viên.
- Kiểm tra dữ liệu tài khoản.
- Thông báo kết quả và quay về đăng nhập.

Cơ sở dữ liệu:
- ACTIVATION_TOKENS: kiểm tra và đánh dấu mã.
- ACCOUNTS: tạo tài khoản đăng nhập.

### 8. Ghi chú bám code
- Có trong code:
  + `LoginView` và `RegisterView` có bước nhập mã kích hoạt, kiểm tra mã, sau đó cho nhập tên đăng nhập và mật khẩu.
  + `AccountActivationService` kiểm tra mã qua `ActivationTokenSql`, lấy thông tin nhân viên qua `AccountActivationSql`, tạo tài khoản, gán vai trò và đánh dấu mã đã dùng.
  + `EmployeeView` và `ManagerManagementView` có luồng phát hành/gửi mã kích hoạt qua email.
- Suy luận nhẹ từ code:
  + Actor trong đặc tả ghi Admin, nhưng màn hình kích hoạt thực tế là thao tác của nhân viên nhận mã; Admin/Quản lý tham gia ở bước cấp mã trước đó.
  + Diagram nên bỏ qua chi tiết tạo `USERS` và gán quyền để giữ luồng ở mức nghiệp vụ.
- Chưa tìm thấy / cần kiểm tra:
  + Có hai nhánh triển khai kích hoạt: `AccountActivationService` mới và một số hàm cũ trong `AccountSql`; nên ưu tiên luồng đang được `LoginView`/`RegisterView` gọi qua API/service.

### 9. Trạng thái
Tốt

--------------------------------------------------

## UC04 / 3.2.4 - Khôi phục mật khẩu

### 1. Mục tiêu ngắn gọn
Cho phép người dùng yêu cầu mã OTP qua email và đặt lại mật khẩu khi quên thông tin đăng nhập. Hệ thống chỉ cập nhật mật khẩu khi email tồn tại và OTP còn hợp lệ.

### 2. Tác nhân chính
- Admin
- Quản lý cửa hàng
- Nhân viên bán hàng / Thu ngân
- Nhân viên kho

### 3. Code liên quan đã tìm thấy
- Màn hình/View/Panel: `LoginView`, `ForgotPasswordView`
- Controller/Service nếu có: `EmailService`, `PasswordService` có helper liên quan nhưng màn hình hiện gọi trực tiếp lớp SQL
- DAO/SQL class: `AccountSql`
- File SQL/schema liên quan: `database/KhoiTaoCacBang.sql`

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS
- OTP_STORAGE

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- USERS: dùng để tra email của người dùng khi tìm tài khoản, nhưng có thể gộp vào bước kiểm tra tài khoản.
- AUDIT_LOG: chưa thấy luồng khôi phục mật khẩu ghi log nghiệp vụ rõ ràng trong màn hình hiện tại.

### 6. Luồng Activity Diagram đã đơn giản hóa

Start

1. Người dùng: Chọn chức năng quên mật khẩu từ màn hình đăng nhập.
2. Hệ thống: Mở màn hình khôi phục mật khẩu.
3. Người dùng: Nhập email đã đăng ký.
4. Hệ thống: Kiểm tra email có tài khoản hợp lệ.
5. Cơ sở dữ liệu: ACCOUNTS - tìm tài khoản theo email.

Decision 1:
- Điều kiện: Email có tài khoản hợp lệ?
- Nếu Không: Hệ thống thông báo email không tồn tại trong hệ thống.
- Nếu Có: Hệ thống tạo và gửi OTP qua email.

6. Cơ sở dữ liệu: OTP_STORAGE - lưu OTP và thời hạn sử dụng.
7. Người dùng: Nhập OTP, mật khẩu mới và xác nhận mật khẩu.
8. Hệ thống: Kiểm tra OTP và dữ liệu mật khẩu.
9. Cơ sở dữ liệu: OTP_STORAGE - xác thực OTP còn hiệu lực.

Decision 2:
- Điều kiện: OTP hợp lệ và mật khẩu xác nhận khớp?
- Nếu Không: Hệ thống thông báo lỗi và yêu cầu nhập lại.
- Nếu Có: Hệ thống cập nhật mật khẩu mới.

10. Cơ sở dữ liệu: ACCOUNTS - cập nhật mật khẩu mới cho tài khoản.
11. Hệ thống: Thông báo khôi phục thành công.
12. Hệ thống: Chuyển người dùng về màn hình đăng nhập.

End

### 7. Gợi ý vẽ theo swimlane

Người dùng:
- Chọn quên mật khẩu.
- Nhập email.
- Nhập OTP và mật khẩu mới.

Hệ thống:
- Mở màn hình khôi phục.
- Kiểm tra tài khoản theo email.
- Gửi OTP.
- Kiểm tra OTP và mật khẩu.
- Thông báo kết quả.

Cơ sở dữ liệu:
- ACCOUNTS: kiểm tra tài khoản và cập nhật mật khẩu.
- OTP_STORAGE: lưu và xác thực OTP.

### 8. Ghi chú bám code
- Có trong code:
  + `LoginView` có liên kết mở `ForgotPasswordView`.
  + `ForgotPasswordView` kiểm tra email, gửi OTP, xác thực OTP và cập nhật mật khẩu.
  + `AccountSql` có truy vấn tìm username theo email, lưu OTP, kiểm tra OTP và cập nhật mật khẩu theo email.
  + Schema có bảng `OTP_STORAGE` với email, mã OTP và thời hạn.
- Suy luận nhẹ từ code:
  + Đặc tả nói nhập email hoặc tên tài khoản; màn hình hiện tại tập trung vào email.
  + Điều kiện "kiểm tra độ mạnh mật khẩu" trong đặc tả chưa thể hiện rõ ở màn hình hiện tại, nên không đưa thành decision riêng.
- Chưa tìm thấy / cần kiểm tra:
  + Chưa thấy thao tác xóa OTP sau khi đổi mật khẩu thành công.
  + Chưa thấy log nghiệp vụ riêng cho khôi phục mật khẩu.

### 9. Trạng thái
Cần kiểm tra lại

--------------------------------------------------

## UC05 / 3.2.5 - Quản lý tài khoản và phân quyền

### 1. Mục tiêu ngắn gọn
Cho phép Admin xem danh sách tài khoản, khóa/mở khóa tài khoản và gán vai trò phù hợp cho người dùng. Use-case này cũng liên quan đến màn hình ma trận quyền, nhưng flow vẽ nên tập trung vào thao tác quản lý tài khoản và gán quyền ở mức tổng quát.

### 2. Tác nhân chính
- Admin

### 3. Code liên quan đã tìm thấy
- Màn hình/View/Panel: `AdminDashboardView`, `AccountRoleAssignmentPanel`, `RoleManagementPanel`, `LoginManagementPanel`, `AuditLogPanel`, `CreateAccountPanel`
- Controller/Service nếu có: `AuditLogService`, `AuthorizationService`, `UIPermissionGuard`
- DAO/SQL class: `AccountSql`, `RoleSql`, `AccountAssignRoleSql`, `RoleGroupSql`, `RoleGroupAssignRoleSql`, `AuditLogSql`
- File SQL/schema liên quan: `database/KhoiTaoCacBang.sql`

### 4. Bảng CSDL nên hiển thị trên Activity Diagram
- ACCOUNTS
- ACCOUNT_ASSIGN_ROLE

### 5. Bảng CSDL liên quan khác nhưng không nên hiển thị
- USERS: dùng để hiển thị họ tên/email của tài khoản, nhưng không phải bảng quyết định trong flow phân quyền.
- ROLES: là nguồn vai trò/quyền; chỉ nên hiển thị nếu vẽ riêng flow "chỉnh ma trận quyền".
- ROLE_GROUPS: có class/schema nhưng luồng hiện tại trong màn gán vai trò chủ yếu dùng vai trò trực tiếp.
- ROLE_GROUP_ASSIGN_ROLE: có schema/class nhưng class hiện tại chưa triển khai nghiệp vụ rõ.
- AUDIT_LOG: ghi vết thay đổi quyền/trạng thái, nhưng nên để ở ghi chú vì không phải kết quả nghiệp vụ chính.
- LOGIN_HISTORY: phục vụ màn hình lịch sử truy cập, không phải luồng chính của quản lý phân quyền.

### 6. Luồng Activity Diagram đã đơn giản hóa

Start

1. Người dùng: Mở menu quản lý tài khoản hoặc quản lý phân quyền.
2. Hệ thống: Kiểm tra quyền truy cập của Admin.

Decision 1:
- Điều kiện: Người dùng có quyền quản trị?
- Nếu Không: Hệ thống thông báo không có quyền truy cập.
- Nếu Có: Hệ thống tải dữ liệu quản lý tài khoản/phân quyền.

3. Cơ sở dữ liệu: ACCOUNTS - lấy danh sách tài khoản và trạng thái.
4. Cơ sở dữ liệu: ACCOUNT_ASSIGN_ROLE - lấy vai trò đang gán cho tài khoản.
5. Hệ thống: Hiển thị danh sách tài khoản, bộ lọc và vùng chọn vai trò.
6. Người dùng: Chọn một tài khoản cần xử lý.
7. Người dùng: Chọn thao tác khóa/mở khóa hoặc gán vai trò mới.
8. Hệ thống: Kiểm tra dữ liệu thao tác và yêu cầu xác nhận khi cần.

Decision 2:
- Điều kiện: Dữ liệu thao tác hợp lệ và được xác nhận?
- Nếu Không: Hệ thống giữ nguyên dữ liệu hiện tại và hiển thị cảnh báo phù hợp.
- Nếu Có: Hệ thống cập nhật tài khoản hoặc vai trò.

9. Cơ sở dữ liệu: ACCOUNTS - cập nhật trạng thái tài khoản nếu khóa/mở khóa.
10. Cơ sở dữ liệu: ACCOUNT_ASSIGN_ROLE - cập nhật vai trò được gán nếu đổi quyền.
11. Hệ thống: Ghi nhận thay đổi và đồng bộ giao diện.
12. Hệ thống: Làm mới danh sách và thông báo kết quả.

End

### 7. Gợi ý vẽ theo swimlane

Người dùng:
- Mở chức năng quản lý.
- Chọn tài khoản.
- Chọn khóa/mở khóa hoặc vai trò mới.
- Xác nhận thao tác.

Hệ thống:
- Kiểm tra quyền Admin.
- Tải và hiển thị danh sách.
- Kiểm tra thao tác.
- Cập nhật, đồng bộ và thông báo kết quả.

Cơ sở dữ liệu:
- ACCOUNTS: đọc danh sách và cập nhật trạng thái tài khoản.
- ACCOUNT_ASSIGN_ROLE: đọc/cập nhật vai trò gán cho tài khoản.

### 8. Ghi chú bám code
- Có trong code:
  + `AdminDashboardView` điều hướng đến `AccountRoleAssignmentPanel`, `RoleManagementPanel`, `LoginManagementPanel`, `AuditLogPanel`.
  + `AccountRoleAssignmentPanel` tải danh sách tài khoản, lọc theo vai trò, khóa/mở khóa tài khoản và gán vai trò mới.
  + `RoleManagementPanel` tải/cập nhật ma trận quyền trên bảng `ROLES`.
  + `AccountSql` có truy vấn lấy tài khoản kèm thông tin người dùng/vai trò và cập nhật quan hệ tài khoản-vai trò.
  + `AuditLogService`/`AuditLogSql` được dùng để ghi nhận thay đổi tài khoản hoặc quyền.
- Suy luận nhẹ từ code:
  + Vì use-case 3.2.5 bao quát nhiều màn hình, flow vẽ nên chọn một luồng đại diện: Admin mở quản lý, xem tài khoản, cập nhật trạng thái hoặc vai trò.
  + Nếu cần vẽ riêng ma trận quyền, nên tách một Activity Diagram khác với bảng chính là `ROLES`.
- Chưa tìm thấy / cần kiểm tra:
  + `RoleGroupSql` và `RoleGroupAssignRoleSql` tồn tại nhưng chưa thể hiện luồng quản lý nhóm quyền đầy đủ.
  + `CreateAccountPanel` có tạo tài khoản trực tiếp, nhưng đặc tả 3.2.5 ở mức bao quát; phần tạo tài khoản có thể đã được tách ở use-case sau.

### 9. Trạng thái
Tốt

--------------------------------------------------

## Bảng tổng hợp

| UC | Tên use-case | Bảng chính nên vẽ | Bảng phụ không vẽ | Màn hình liên quan | Trạng thái |
|----|--------------|-------------------|-------------------|--------------------|------------|
| UC02 | Đăng xuất hệ thống | ACCOUNTS; LOGIN_HISTORY | TOKENS; AUDIT_LOG | AdminDashboardView; DashboardView; WarehouseDashboardView; Sidebar/AdminSidebar/WarehouseSidebar | Tốt |
| UC03 | Kích hoạt tài khoản nhân viên | ACTIVATION_TOKENS; ACCOUNTS | EMPLOYEES; USERS; ACCOUNT_ASSIGN_ROLE; AUDIT_LOG | LoginView; RegisterView; EmployeeView; ManagerManagementView | Tốt |
| UC04 | Khôi phục mật khẩu | ACCOUNTS; OTP_STORAGE | USERS; AUDIT_LOG | LoginView; ForgotPasswordView | Cần kiểm tra lại |
| UC05 | Quản lý tài khoản và phân quyền | ACCOUNTS; ACCOUNT_ASSIGN_ROLE | USERS; ROLES; ROLE_GROUPS; ROLE_GROUP_ASSIGN_ROLE; AUDIT_LOG; LOGIN_HISTORY | AdminDashboardView; AccountRoleAssignmentPanel; RoleManagementPanel; LoginManagementPanel; AuditLogPanel; CreateAccountPanel | Tốt |
