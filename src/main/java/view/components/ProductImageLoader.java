package view.components;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.File;
import java.net.URL;

/**
 * Helper tập trung để load ảnh sản phẩm.
 *
 * Logic ưu tiên theo thứ tự:
 *   1. Đường dẫn tuyệt đối (khi imagePath là full path, ví dụ user vừa chọn)
 *   2. File system: target/classes/view/image/products/<tên file>
 *   3. File system: src/main/resources/view/image/products/<tên file>
 *   4. ClassLoader resource (jar, classpath chuẩn)
 *   5. Trả về null nếu không tìm thấy → UI hiển thị "—"
 *
 * Dùng chung cho SellPanel, ProductView, InventoryView.
 */
public class ProductImageLoader {

    private static final String RESOURCE_PREFIX = "view/image/products/";

    /**
     * Load ảnh sản phẩm và scale về kích thước (w x h).
     *
     * @param imagePath tên file ảnh (hoặc path tuyệt đối)
     * @param w         chiều rộng đích (px)
     * @param h         chiều cao đích (px)
     * @return ImageIcon đã scale, hoặc null nếu không load được
     */
    public static ImageIcon load(String imagePath, int w, int h) {
        if (imagePath == null || imagePath.isBlank()) return null;

        String trimmed = imagePath.trim();

        // 1. Thử đường dẫn tuyệt đối (full path file)
        File absFile = new File(trimmed);
        if (absFile.isAbsolute() && absFile.exists()) {
            return scale(new ImageIcon(absFile.getAbsolutePath()), w, h);
        }

        // Lấy chỉ tên file (phòng trường hợp lưu full path trong DB)
        String fileName = new File(trimmed).getName();

        // 2. target/classes (runtime khi chạy từ IDE / Maven)
        String projectDir = System.getProperty("user.dir");
        File targetFile = new File(projectDir,
                "target/classes/view/image/products/" + fileName);
        if (targetFile.exists()) {
            return scale(new ImageIcon(targetFile.getAbsolutePath()), w, h);
        }

        // 3. src/main/resources (dev fallback)
        File srcFile = new File(projectDir,
                "src/main/resources/view/image/products/" + fileName);
        if (srcFile.exists()) {
            return scale(new ImageIcon(srcFile.getAbsolutePath()), w, h);
        }

        // 4. ClassLoader / JAR resource
        //    Dùng Thread.currentThread().getContextClassLoader() để tránh
        //    trả về null khi chạy trong một số môi trường khác nhau.
        URL url = Thread.currentThread()
                .getContextClassLoader()
                .getResource(RESOURCE_PREFIX + fileName);
        if (url == null) {
            // fallback: ClassLoader của chính class này
            url = ProductImageLoader.class
                    .getClassLoader()
                    .getResource(RESOURCE_PREFIX + fileName);
        }
        if (url != null) {
            return scale(new ImageIcon(url), w, h);
        }

        return null;
    }

    /**
     * Bản rút gọn không cần w/h (dùng kích thước mặc định 55x45 cho table).
     */
    public static ImageIcon loadThumb(String imagePath) {
        return load(imagePath, 55, 45);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private static ImageIcon scale(ImageIcon src, int w, int h) {
        if (src == null || src.getIconWidth() <= 0) return null;
        try {
            Image scaled = src.getImage()
                    .getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }
}