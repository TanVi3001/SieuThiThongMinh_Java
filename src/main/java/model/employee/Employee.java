package model.employee;

import java.math.BigDecimal;
import java.sql.Date;

public class Employee {

    private String employeeId;
    private String employeeName;
    private Date hireDate;
    private BigDecimal salaryCoefficient;
    private int totalCompletedOrders;
    private String roleId;
    private String shiftId;
    private int isDeleted;
    private String accountStatus;
    
    // ===== thêm field để khớp form EmployeeView =====
    private String phone;
    private String email;
    private String role;   // chức vụ hiển thị trên form
    private String gender;
    private String onlineStatus;
    private int activeSessions;
    
    // ===== BỔ SUNG LIÊN KẾT CHI NHÁNH (ERP) =====
    private String storeId;
    private String storeName;

    public Employee() {
    }

    public Employee(String employeeId, String employeeName, Date hireDate,
            BigDecimal salaryCoefficient, int totalCompletedOrders,
            String roleId, String shiftId, int isDeleted) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.hireDate = hireDate;
        this.salaryCoefficient = salaryCoefficient;
        this.totalCompletedOrders = totalCompletedOrders;
        this.roleId = roleId;
        this.shiftId = shiftId;
        this.isDeleted = isDeleted;
    }

    // ===== constructor tiện dùng cho form =====
    public Employee(String employeeId, String employeeName, String phone, String email, String role, String gender) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.gender = gender;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public BigDecimal getSalaryCoefficient() {
        return salaryCoefficient;
    }

    public void setSalaryCoefficient(BigDecimal salaryCoefficient) {
        this.salaryCoefficient = salaryCoefficient;
    }

    public int getTotalCompletedOrders() {
        return totalCompletedOrders;
    }

    public void setTotalCompletedOrders(int totalCompletedOrders) {
        this.totalCompletedOrders = totalCompletedOrders;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    // ===== các getter/setter từng bị throw lỗi -> sửa thật =====
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        // ưu tiên role hiển thị; nếu null thì fallback roleId
        return (role != null) ? role : roleId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    // ===== GETTER/SETTER CHO CHI NHÁNH =====
    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}