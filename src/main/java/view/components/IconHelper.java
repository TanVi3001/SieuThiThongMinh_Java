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
 */
public class IconHelper {

    private static final String RESOURCE_PATH = "view/image/";
    private static final String FILE_PATH = "src/main/resources/view/image/";
    private static final Map<String, ImageIcon> categoryIconCache = new HashMap<>();
    private static final String CATEGORY_ICON_PATH = "/view/image/categories/";

    private IconHelper() {
    }

    public static ImageIcon getIcon(String fileName, int width, int height) {
        try {
            URL url = IconHelper.class.getClassLoader().getResource(RESOURCE_PATH + fileName);
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
            Image scaled = new ImageIcon(url).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            System.err.println("[IconHelper] LỖI load icon '" + fileName + "': " + e.getMessage());
            return null;
        }
    }

    public static ImageIcon load(String fileName, int size) {
        return getIcon(fileName, size, size);
    }

    public static ImageIcon notification(int size) {
        ImageIcon icon = load("notification.png", size);
        return icon != null ? icon : stock(size);
    }

    public static Icon view(int size) {
        ImageIcon icon = load("view.png", size);
        return icon != null ? icon : file(size);
    }

    // ==================================================================
    // DANH SÁCH ICON 
    // ==================================================================
    public static ImageIcon export(int size) {
        return load("export.png", size);
    }

    public static ImageIcon printer(int size) {
        return load("printer.png", size);
    }

    public static ImageIcon file(int size) {
        return load("file.png", size);
    }

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

    public static ImageIcon search(int size) {
        return load("search-interface-symbol.png", size);
    }

    public static ImageIcon remove(int size) {
        return load("remove.png", size);
    }
    
    public static ImageIcon getCategoryIcon(String categoryId, int size) {
        String key = categoryId + "_" + size;
        if (categoryIconCache.containsKey(key)) {
            return categoryIconCache.get(key);
        }
        ImageIcon icon = loadAndScale(CATEGORY_ICON_PATH + categoryId + ".png", size);
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
        if (url == null) {
            return null;
        }
        Image img = new ImageIcon(url).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

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

    public static ImageIcon history(int size) {
        return load("history.png", size);
    }
    
    public static ImageIcon close(int size) {
        return load("close.png", size);
    }
    
    public static ImageIcon save(int size) {
        return load("save-instagram.png", size);
    }
    
    public static ImageIcon warning(int size) {
        return load("warning.png", size);
    }
    
    public static ImageIcon upload(int size) {
        return load("upload.png", size);
    }

    public static ImageIcon folder(int size) {
        return load("folder-open.png", size);
    }

    public static ImageIcon template(int size) {
        return load("new-file.png", size);
    }

    public static ImageIcon play(int size) {
        return load("play.png", size);
    }

    public static ImageIcon shield(int size) {
        return load("security.png", size);
    }

    public static ImageIcon accessDenied(int size) {
        ImageIcon originalIcon = load("access_denied_icon.png", size);
        if (originalIcon == null) {
            return null;
        }
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.drawImage(originalIcon.getImage(), 0, 0, null);
        g2.setComposite(java.awt.AlphaComposite.SrcAtop);
        g2.setColor(new java.awt.Color(220, 53, 69));
        g2.fillRect(0, 0, size, size);
        g2.dispose();
        return new ImageIcon(img);
    }
}
