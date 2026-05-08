package model.order;

/**
 * @author nguye
 */
public class Customer {

    private String customerId;
    private String customerName;
    private String phone;
    private String email;
    private String address;
    private int rewardPoints;
    private int isDeleted;
    private double totalSpending;
    private String memberRank;

    public Customer() {
    }

    // --- LOGIC HẠNG THÀNH VIÊN ĐỘNG ---
    // Tự động tính hạng dựa trên số tiền đã nạp vào totalSpending
    public String getMemberRank() {
        if (totalSpending >= 80_000_000) {
            return "Kim cương";
        }
        if (totalSpending >= 40_000_000) {
            return "Vàng";
        }
        if (totalSpending >= 15_000_000) {
            return "Bạc";
        }
        if (totalSpending >= 5_000_000) {
            return "Đồng";
        }
        return "Thường";
    }

    // --- TỶ LỆ GIẢM GIÁ (ÁP DỤNG ĐƠN SAU) ---
    public double getDiscountRate() {
        String rank = getMemberRank();
        return switch (rank) {
            case "Kim cương" ->
                0.12;
            case "Vàng" ->
                0.08;
            case "Bạc" ->
                0.05;
            case "Đồng" ->
                0.02;
            default ->
                0.00;
        };
    }

    // --- GETTERS & SETTERS (Đã dọn dẹp trùng lặp) ---
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerID() {
        return customerId;
    } // Alias cho code cũ

    public void setCustomerID(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(double totalSpending) {
        this.totalSpending = totalSpending;
    }

    public void setMemberRank(String memberRank) {
        this.memberRank = memberRank;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
