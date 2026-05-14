# 📊 Hướng Dẫn Nhập Dữ Liệu KPI Nhân Viên

## 🎯 Tổng Quan Tính Năng

Hệ thống Smart Supermarket đã được cập nhật với chức năng nhập dữ liệu KPI (Key Performance Indicator) từ file CSV. Bạn có thể:

1. **Nhập dữ liệu KPI từ file CSV**
2. **Xem trước dữ liệu trước khi import**
3. **Lưu lịch sử KPI trong database**
4. **Tạo báo cáo Power BI từ dữ liệu KPI**

---

## 📋 Các File Được Tạo

### 1. **business/kpi/KpiCsvParser.java**
   - Parser CSV file chứa dữ liệu KPI
   - Hỗ trợ dấu phẩy (,) và dấu chấm phẩy (;) làm delimiter
   - Kiểm tra lỗi dữ liệu chi tiết

### 2. **business/kpi/KpiDataService.java**
   - Service lưu dữ liệu KPI vào database
   - Kiểm tra nhân viên tồn tại
   - Xóa dữ liệu KPI theo thời gian

### 3. **view/ImportKpiDialog.java**
   - Dialog giao diện để import KPI
   - Xem trước dữ liệu trước import
   - Tạo file CSV mẫu

### 4. **database/create_kpi_history_table.sql**
   - Script tạo bảng EMPLOYEE_KPI_HISTORY
   - Lưu lịch sử dữ liệu KPI

---

## 🚀 Cách Sử Dụng

### Bước 1: Tạo Bảng Database (Lần Đầu)

```sql
-- Chạy script này trên database Oracle của bạn
-- File: database/create_kpi_history_table.sql

-- Hoặc chạy SQL trong SQL Developer/SQL*Plus:
@database/create_kpi_history_table.sql
```

### Bước 2: Chuẩn Bị File KPI CSV

**Format file:**
```
Mã NV,Tên NV,Số Đơn,Doanh Thu,Tỷ Lệ Hoàn Thành (%),Tỷ Lệ Giao Hàng (%),Điểm Chuyên Cần
EMP001,Nguyễn Văn A,50,10000000,95,98,8.5
EMP002,Trần Thị B,45,9500000,92,95,8.0
EMP003,Phạm Văn C,55,11000000,98,99,9.0
```

**Hướng Dẫn Chi Tiết:**
- **Mã NV**: Mã nhân viên (phải tồn tại trong hệ thống)
- **Tên NV**: Tên đầy đủ nhân viên
- **Số Đơn**: Số lượng đơn hàng hoàn thành (số nguyên)
- **Doanh Thu**: Tổng doanh thu (số thực, VNĐ)
- **Tỷ Lệ Hoàn Thành (%)**: Phần trăm đơn hoàn thành (0-100)
- **Tỷ Lệ Giao Hàng (%)**: Phần trăm giao hàng thành công (0-100)
- **Điểm Chuyên Cần**: Điểm từ 0-10

### Bước 3: Import Dữ Liệu

1. Mở ứng dụng Smart Supermarket
2. Vào tab **"Báo cáo & Thống kê"** → **"Hiệu suất Nhân viên"**
3. Nhấn nút **"📥 Nhập KPI từ File"**
4. Chọn file CSV hoặc nhấn **"Tạo File Mẫu"** để có mẫu
5. File sẽ được phân tích hiển thị trong bảng preview
6. Nhấn **"✓ Nhập Dữ Liệu"** để lưu vào database

---

## 📊 Power BI Integration

### Kết Nối Power BI với Database

**Cách 1: Kết nối trực tiếp Oracle**

1. Mở Power BI Desktop
2. Chọn **"Get Data"** → **"More..."** → **"Oracle Database"**
3. Nhập thông tin kết nối:
   - **Server**: IP/Hostname của Oracle server
   - **Database**: Tên database (SID)
   - Username & Password
4. Chọn bảng: `EMPLOYEES`, `EMPLOYEE_KPI_HISTORY`, `ORDERS`

**Cách 2: Xuất CSV rồi Import vào Power BI**

1. Trong ứng dụng, nhấn **"📤 Xuất Excel"**
2. Mở Power BI
3. **"Get Data"** → **"Excel"** → Chọn file đã xuất

### Mô Hình Dữ Liệu Power BI

```
EMPLOYEES
    ↓ (1:N relationship)
EMPLOYEE_KPI_HISTORY
    ↓
ORDERS
```

### Các Visualizations Gợi Ý

1. **KPI Card**: Top Sale Employee Revenue
2. **Column Chart**: KPI Score by Employee
3. **Gauge Chart**: Attendance Score vs Target
4. **Scatter Plot**: Completion Rate vs Delivery Success Rate
5. **Table**: Full KPI Details with Ranking
6. **Line Chart**: KPI Trend Over Time (if tracking monthly)

---

## 🔧 Công Thức Tính Điểm KPI

Điểm KPI được tính theo công thức:

```
Performance Score = (Completion Rate × 0.4) 
                  + (Delivery Success Rate × 0.3) 
                  + (Attendance Score × 10 × 0.3)
```

**Ví dụ:**
```
Completion Rate = 95%
Delivery Success Rate = 98%
Attendance Score = 8.5/10

Performance Score = (95 × 0.4) + (98 × 0.3) + (8.5 × 10 × 0.3)
                  = 38 + 29.4 + 25.5
                  = 92.9
```

---

## ⚙️ Tùy Chỉnh

### 1. Thay Đổi Công Thức KPI

File: `src/main/java/model/employee/EmployeePerformance.java`

```java
public void calculatePerformanceScore() {
    // Thay đổi trọng số tại đây
    this.performanceScore = (this.completionRate * 0.4)
            + (this.deliverySuccessRate * 0.3)
            + (this.attendanceScore * 10 * 0.3);
}
```

### 2. Thêm Cột KPI Mới

1. Thêm field vào `EmployeePerformance.java`:
   ```java
   private double customerSatisfactionScore;
   
   public double getCustomerSatisfactionScore() { ... }
   public void setCustomerSatisfactionScore(double score) { ... }
   ```

2. Cập nhật CSV parser trong `KpiCsvParser.java`

3. Cập nhật bảng database

### 3. Thay Đổi Giao Diện Import Dialog

File: `src/main/java/view/ImportKpiDialog.java`

Bạn có thể thay đổi:
- Kích thước dialog
- Các nút bấm
- Màu sắc
- Các cột preview

---

## 🐛 Khắc Phục Sự Cố

### Lỗi: "Mã nhân viên không tồn tại"

**Nguyên nhân**: Mã NV trong file CSV không tồn tại trong bảng EMPLOYEES

**Giải pháp**: 
- Kiểm tra danh sách nhân viên hợp lệ
- Sửa mã NV trong file CSV
- Hoặc thêm nhân viên mới vào hệ thống trước

### Lỗi: "Định dạng số không hợp lệ"

**Nguyên nhân**: Dữ liệu số không đúng định dạng (ký tự chữ, ký tự đặc biệt)

**Giải pháp**:
- Kiểm tra file CSV
- Sử dụng dấu chấm (.) cho phần thập phân, không phải dấu phẩy
- Tham khảo file mẫu

### File CSV không mở được

**Nguyên nhân**: Encoding hoặc delimiter sai

**Giải pháp**:
- Tạo file bằng Notepad/Excel, lưu dạng CSV
- Đảm bảo sử dụng UTF-8 encoding
- Sử dụng dấu phẩy (,) hoặc dấu chấm phẩy (;) làm delimiter

---

## 📈 Ví Dụ Báo Cáo Power BI

### Dashboard Tổng Quát

```
┌─────────────────────────────────────────┐
│  Smart Supermarket - Employee KPI      │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────────┐ ┌──────────────┐   │
│  │ Top Salesman │ │ Top Delivery │   │
│  │ Nguyễn Văn A │ │ Trần Thị B   │   │
│  │ ₫ 120M       │ │ 99% Success  │   │
│  └──────────────┘ └──────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  KPI Score Distribution         │  │
│  │  [Chart]                        │  │
│  └─────────────────────────────────┘  │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  Employee Rankings              │  │
│  │  1. Nguyễn Văn A  92.9  ⭐⭐⭐  │  │
│  │  2. Trần Thị B    90.5  ⭐⭐⭐  │  │
│  │  3. Phạm Văn C    88.2  ⭐⭐    │  │
│  └─────────────────────────────────┘  │
│                                         │
└─────────────────────────────────────────┘
```

---

## 📞 Hỗ Trợ

Nếu gặp vấn đề:

1. Kiểm tra log lỗi trong console
2. Xem file mẫu để hiểu format đúng
3. Đảm bảo nhân viên tồn tại trong hệ thống
4. Liên hệ team support

---

## 🎉 Tính Năng Sắp Tới

- [ ] Xuất dữ liệu KPI ra Excel
- [ ] Lịch sử thay đổi KPI (trend chart)
- [ ] So sánh KPI tháng/quý/năm
- [ ] Cảnh báo tự động khi KPI thấp
- [ ] Tích hợp trực tiếp Power BI

---

**Phiên bản**: 1.0  
**Cập nhật**: Tháng 5, 2026  
**Hỗ trợ**: Vietnamese Language
