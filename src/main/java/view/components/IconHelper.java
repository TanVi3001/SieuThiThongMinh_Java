package view.components;

import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * IconHelper — Tiện ích load icon PNG dùng chung toàn project.
 *
 * Cách dùng ở bất kỳ panel nào: ImageIcon icon = IconHelper.dashboard(22);
 *
 * File PNG phải nằm trong: src/main/resources/view/image/
 */
public class IconHelper {

    // Thư mục chứa icon trong resources
    private static final String RESOURCE_PATH = "view/image/";
    private static final String FILE_PATH = "src/main/resources/view/image/";

    public static Icon getIcon(String iconPath, int i, int i0) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public static ImageIcon export(int i) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public static ImageIcon printer(int i) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // Không cho khởi tạo
    private IconHelper() {
    }

    /**
     * Load icon PNG và scale về kích thước mong muốn.
     *
     * @param fileName Tên file, ví dụ: "money.png"
     * @param size Kích thước pixel (vuông), ví dụ: 22
     * @return ImageIcon đã scale, hoặc null nếu không tìm thấy file
     */
    public static ImageIcon load(String fileName, int size) {
        try {
            // Cách 1: load qua classpath (khi build JAR)
            URL url = IconHelper.class.getClassLoader()
                    .getResource(RESOURCE_PATH + fileName);

            // Cách 2: load trực tiếp từ file khi chạy trong NetBeans/IDE
            if (url == null) {
                File f = new File(FILE_PATH + fileName);
                if (f.exists()) {
                    url = f.toURI().toURL();
                }
            }

            if (url == null) {
                System.err.println("[IconHelper] CẢNH BÁO: Không tìm thấy icon: " + fileName);
                return null;
            }

            Image scaled = new ImageIcon(url).getImage()
                    .getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);

        } catch (Exception e) {
            System.err.println("[IconHelper] LỖI load icon '" + fileName + "': " + e.getMessage());
            return null;
        }
    }

    // ==================================================================
    // DANH SÁCH ICON CHO CÁC NÚT THAO TÁC (CRUD)
    // ==================================================================
    public static ImageIcon add(int size) {
        return load("add.png", size);
    }

    public static ImageIcon edit(int size) {
        return load("edit.png", size);
    }

    public static ImageIcon delete(int size) {
        return load("delete.png", size);
    }

    public static ImageIcon refresh(int size) {
        return load("refresh.png", size);
    }

    // Lưu ý: Đã sửa đúng tên file tìm kiếm có trên Git
    public static ImageIcon search(int size) {
        return load("search-interface-symbol.png", size);
    }
    
    // ===== THÊM MỚI: Category icon support =====
    private static final Map<String, ImageIcon> categoryIconCache = new HashMap<>();
    private static final String CATEGORY_ICON_PATH = "/view/image/categories/";

    public static ImageIcon getCategoryIcon(String categoryId, int size) {
        String key = categoryId + "_" + size;
        if (categoryIconCache.containsKey(key)) {
            return categoryIconCache.get(key);
        }

        // Thử load theo categoryId (VD: CAT001.png)
        ImageIcon icon = loadAndScale(CATEGORY_ICON_PATH + categoryId + ".png", size);

        // Fallback về default nếu không có
        if (icon == null) {
            icon = loadAndScale(CATEGORY_ICON_PATH + "default.png", size);
        }

        if (icon != null) {
            categoryIconCache.put(key, icon);
        }
        return icon != null ? icon : new ImageIcon();
    }

    private static ImageIcon loadAndScale(String path, int size) {
        URL url = IconHelper.class.getResource(path);
        if (url == null) return null;
        Image img = new ImageIcon(url).getImage()
                        .getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    // ==================================================================
    // DANH SÁCH ICON CHO SIDEBAR MENU
    // ==================================================================
    public static ImageIcon dashboard(int size) {
        return load("monitor.png", size);
    }

    public static ImageIcon product(int size) {
        return load("storage.png", size);
    }

    public static ImageIcon employee(int size) {
        return load("employee.png", size);
    }

    public static ImageIcon customer(int size) {
        return load("customer.png", size);
    }

    public static ImageIcon bill(int size) {
        return load("bill.png", size);
    }

    public static ImageIcon settings(int size) {
        return load("settings.png", size);
    }

    public static ImageIcon logout(int size) {
        return load("checkout.png", size);
    }

    // ==================================================================
    // DANH SÁCH ICON CHO CÁC NGHIỆP VỤ KHÁC (THỐNG KÊ, TỔNG QUAN, GIAO HÀNG...)
    // ==================================================================
    public static ImageIcon revenue(int size) {
        return load("money.png", size);
    }

    public static ImageIcon order(int size) {
        return load("shopping-cart.png", size);
    }

    public static ImageIcon barChart(int size) {
        return load("bar-chart.png", size);
    }

    public static ImageIcon lineChart(int size) {
        return load("chart.png", size);
    }

    public static ImageIcon pieChart(int size) {
        return load("public-service.png", size);
    }

    public static ImageIcon stock(int size) {
        return load("in-stock.png", size);
    }

    public static ImageIcon delivery(int size) {
        return load("delivery-truck.png", size);
    }

    public static ImageIcon coupon(int size) {
        return load("coupon.png", size);
    }

    public static ImageIcon saveMoney(int size) {
        return load("save-money.png", size);
    }
    
    public static ImageIcon accessDenied(int size) {
        ImageIcon originalIcon = load("access_denied_icon.png", size);
        if (originalIcon == null) return null;

        // Tạo một bức tranh trống hỗ trợ nền trong suốt (ARGB)
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        // 1. Vẽ cái ảnh gốc (màu đen) lên bức tranh
        g2.drawImage(originalIcon.getImage(), 0, 0, null);
        
        // 2. Bật chế độ "Chỉ tô màu lên những nét vẽ có sẵn, bỏ qua nền trong suốt"
        g2.setComposite(java.awt.AlphaComposite.SrcAtop);
        
        // 3. Quét một lớp sơn màu đỏ (Màu chuẩn của báo lỗi) lên toàn bộ
        g2.setColor(new java.awt.Color(220, 53, 69)); 
        g2.fillRect(0, 0, size, size);
        g2.dispose();

        return new ImageIcon(img);
    }
}
