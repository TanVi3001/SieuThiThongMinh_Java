package model.order;

/**
 * Model khách hàng.
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

    /*
     * Ưu tiên hạng thành viên được load từ DB qua setMemberRank().
     * Nếu DB không truyền memberRank thì mới tự suy theo totalSpending.
     */
    public String getMemberRank() {
        if (memberRank != null && !memberRank.trim().isEmpty()) {
            return memberRank.trim();
        }

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

    public double getDiscountRate() {
        String rank = getMemberRank();

        if (rank == null || rank.trim().isEmpty()) {
            return 0.0;
        }

        String normalized = java.text.Normalizer
                .normalize(rank, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .trim()
                .toLowerCase();

        return switch (normalized) {
            case "kim cuong" ->
                0.12;
            case "vang" ->
                0.08;
            case "bac" ->
                0.05;
            case "dong" ->
                0.02;
            default ->
                0.0;
        };
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    // Alias cho code cũ
    public String getCustomerID() {
        return customerId;
    }

    // Alias cho code cũ
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
