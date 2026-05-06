package business.api;

import business.service.AccountActivationService;
import model.account.ActivationEmployeeInfo;

/**
 * API phục vụ luồng Tự kích hoạt tài khoản cho nhân viên.
 * Đảm bảo: Xác thực mã -> Tạo Account -> Gán quyền -> Cập nhật trạng thái "Đã cấp" -> Thu hồi mã.
 */
public class AccountActivationAPI {
    private final AccountActivationService service = new AccountActivationService();

    /**
     * Kiểm tra mã kích hoạt từ Email.
     * Trả về thông tin Họ tên, Email, SĐT để điền tự động (Auto-fill) vào Form.
     */
    public ActivationEmployeeInfo check(String code) throws Exception {
        // Trim mã code để tránh lỗi do copy-paste dính khoảng trắng
        ActivationEmployeeInfo info = service.checkAndFetchActivation(code != null ? code.trim() : "");
        
        if (info == null) {
            throw new Exception("Mã kích hoạt không hợp lệ, đã được sử dụng hoặc đã hết hạn.");
        }
        return info;
    }

    /**
     * Thực hiện kích hoạt tài khoản "Chốt sổ".
     * Quy trình này bao gồm việc đổi trạng thái nhân viên sang "Đã cấp" và thu hồi mã.
     */
    public void activate(String code, String username, String passwordPlain) throws Exception {
        // Thực thi nghiệp vụ qua Service (đã bọc trong Transaction)
        boolean isSuccess = service.activateAccount(code, username, passwordPlain);
        
        if (!isSuccess) {
            // Nếu Service trả về false, nghĩa là có lỗi trong quá trình Transaction (trùng username, lỗi DB...)
            throw new Exception("Kích hoạt thất bại. Vui lòng kiểm tra lại tên đăng nhập hoặc liên hệ Admin.");
        }
        
        // Nếu chạy đến đây là thành công: 
        // 1. Tài khoản đã tạo & mã hóa BCrypt.
        // 2. Quyền R_STAFF_SALE đã được gán.
        // 3. Nhân viên trên UI Quản lý đã hiện "Đã cấp".
        // 4. Mã token đã bị thu hồi (vô hiệu hóa)
    }
}