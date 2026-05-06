package model.account;

public class StoreManagerDTO {
    private String managerId;
    private String fullName;
    private String phone;
    private String email;
    private String gender;
    private String accountStatus; // "Đã cấp" hoặc "Chưa cấp"

    // Constructor
    public StoreManagerDTO(String managerId, String fullName, String phone, String email, String gender, String accountStatus) {
        this.managerId = managerId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.gender = gender;
        this.accountStatus = accountStatus;
    }

    // Getter và Setter (Ông dùng IDE generate ra cho lẹ nhé)
    public String getManagerId() { return managerId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public String getAccountStatus() { return accountStatus; }
}