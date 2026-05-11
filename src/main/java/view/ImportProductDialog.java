package view;

import business.service.ProductImportService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ImportProductDialog extends JDialog {

    private JProgressBar progressBar;
    private JButton btnStartImport;
    private JLabel lblStatus;

    private ProductView parentView;

    public ImportProductDialog(JFrame owner) {
        super(owner, "Import Hệ Thống", true);
        initComponents();
    }

    private void initComponents() {
        // 1. Dùng vertical BoxLayout để các component xếp chồng lên nhau
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(new EmptyBorder(25, 25, 25, 25)); // Thêm padding cho đẹp
        setContentPane(contentPane);

        // 2. Setup Label trạng thái
        lblStatus = new JLabel("Sẵn sàng nhập dữ liệu từ: data/products1_1m.csv");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa

        // 3. Setup JProgressBar (Gỡ bỏ kích thước cố định)
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa

        // 4. Setup Nút bấm
        btnStartImport = new JButton("Bắt đầu Import");
        btnStartImport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStartImport.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa

        // Sự kiện nút bấm
        btnStartImport.addActionListener(e -> startImportProcess());

        // 5. Thêm các component vào và kiểm soát khoảng cách bằng Rigid Area
        contentPane.add(lblStatus);
        contentPane.add(Box.createVerticalStrut(15)); // Khoảng cách sau label
        contentPane.add(progressBar);
        contentPane.add(Box.createVerticalStrut(20)); // Khoảng cách trước nút bấm
        contentPane.add(btnStartImport);

        // 6. Tự động tính toán kích thước tự nhiên và hiển thị
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void startImportProcess() {
        // Logic xử lý import (Giữ nguyên như cũ)
        String relativePath = "data" + File.separator + "products1_1m.csv";
        File file = new File(relativePath);

        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy file tại: " + file.getAbsolutePath(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnStartImport.setEnabled(false);
        lblStatus.setText("Đang xử lý song song... vui lòng đợi.");

        // Chạy ngầm bằng CompletableFuture
        CompletableFuture.runAsync(() -> {
            new ProductImportService().importProductCSV(file.getAbsolutePath(), progress -> {
                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
            });
        }).thenRun(() -> {
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("Hoàn tất!");
                parentView.loadDataToTable(); // <--- GỌI DÒNG NÀY ĐỂ REFRESH BẢNG
                this.dispose();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
                btnStartImport.setEnabled(true);
            });
            return null;
        });
    }

    public ImportProductDialog(JFrame owner, ProductView parentView) {
        super(owner, "Import Hệ Thống", true);
        this.parentView = parentView;
        initComponents(); 
    }
}
