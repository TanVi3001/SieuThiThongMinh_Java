package model.order;

/**
 *
 * @author nguye
 */
public class Customer {

    private String customerId;
    private String customerName;
    private String roleId;
    private String phone;
    private String email;
    private String address;
    private int rewardPoints;
    private int isDeleted;
    private double totalSpending; // Tổng chi tiêu
    private String memberRank;    // Hạng thành viên

    public Customer() {
    }

    public Customer(String customerId, String customerName, String roleId,
            String phone, String email, String address,
            int rewardPoints, int isDeleted) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.roleId = roleId;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.rewardPoints = rewardPoints;
        this.isDeleted = isDeleted;
    }

    // ===== ID =====
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    // Alias để tương thích code cũ đang dùng getCustomerID/setCustomerID
    public String getCustomerID() {
        return customerId;
    }

    public void setCustomerID(String customerId) {
        this.customerId = customerId;
    }

    // ===== Name =====
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    // ===== Role =====
    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    // ===== Phone =====
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // ===== Email =====
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // ===== Address =====
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // ===== Reward =====
    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    // ===== Soft delete =====
    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    // Getter và Setter

    public double getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(double totalSpending) {
        this.totalSpending = totalSpending;
    }

    public String getMemberRank() {
        return memberRank;
    }

    public void setMemberRank(String memberRank) {
        this.memberRank = memberRank;
    }
}
