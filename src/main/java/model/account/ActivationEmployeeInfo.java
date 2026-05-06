package model.account;

/**
 * DTO chứa thông tin nhân viên phục vụ màn hình kích hoạt tài khoản.
 */
public class ActivationEmployeeInfo {
    private final String empId;
    private final String fullName;
    private final String phone;
    private final String email;

    public ActivationEmployeeInfo(String empId, String fullName, String phone, String email) {
        this.empId = empId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    public String getEmpId() {
        return empId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}