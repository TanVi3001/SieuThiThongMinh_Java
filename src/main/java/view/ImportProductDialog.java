package view;

import business.service.ProductImportService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import view.components.IconHelper;

public class ImportProductDialog extends JDialog {

    private JProgressBar progressBar;
    private JButton btnChooseCsv;
    private JButton btnChooseImageFolder;
    private JButton btnStartImport;
    private JLabel lblStatus;
    private JLabel lblCsvPath;
    private JLabel lblImageFolderPath;

    private File selectedCsvFile;
    private File selectedImageFolder;

    private ProductView parentView;

    public ImportProductDialog(JFrame owner) {
        super(owner, "Import Hệ Thống", true);
        initComponents();
    }

    public ImportProductDialog(JFrame owner, ProductView parentView) {
        super(owner, "Import Hệ Thống", true);
        this.parentView = parentView;
        initComponents();
    }

    private void initComponents() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(new EmptyBorder(25, 25, 25, 25));
        setContentPane(contentPane);

        lblStatus = new JLabel("Sẵn sàng import sản phẩm và ảnh.");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblCsvPath = new JLabel("CSV: data/products1_1m.csv");
        lblCsvPath.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCsvPath.setForeground(new Color(90, 90, 90));
        lblCsvPath.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblImageFolderPath = new JLabel("Thư mục ảnh: Chưa chọn");
        lblImageFolderPath.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblImageFolderPath.setForeground(new Color(90, 90, 90));
        lblImageFolderPath.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(420, 24));

        btnChooseCsv = new JButton("Chọn file CSV");
        btnChooseCsv.setIcon(IconHelper.file(18));
        btnChooseCsv.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChooseCsv.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChooseCsv.addActionListener(e -> chooseCsvFile());

        btnChooseImageFolder = new JButton("Chọn thư mục ảnh");
        btnChooseImageFolder.setIcon(IconHelper.file(18));
        btnChooseImageFolder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChooseImageFolder.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChooseImageFolder.addActionListener(e -> chooseImageFolder());

        btnStartImport = new JButton("Bắt đầu Import");
        btnStartImport.setIcon(IconHelper.upload(20));
        btnStartImport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStartImport.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStartImport.addActionListener(e -> startImportProcess());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(btnChooseCsv);
        buttonRow.add(btnChooseImageFolder);
        buttonRow.add(btnStartImport);

        contentPane.add(lblStatus);
        contentPane.add(Box.createVerticalStrut(10));
        contentPane.add(lblCsvPath);
        contentPane.add(Box.createVerticalStrut(5));
        contentPane.add(lblImageFolderPath);
        contentPane.add(Box.createVerticalStrut(15));
        contentPane.add(progressBar);
        contentPane.add(Box.createVerticalStrut(20));
        contentPane.add(buttonRow);

        // Default CSV cũ
        File defaultCsv = new File("data" + File.separator + "products1_1m.csv");
        if (defaultCsv.exists()) {
            selectedCsvFile = defaultCsv;
            lblCsvPath.setText("CSV: " + selectedCsvFile.getAbsolutePath());
        }

        pack();
        setSize(Math.max(getWidth(), 560), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private void chooseCsvFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn file CSV sản phẩm");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        File defaultDir = new File("data");
        if (defaultDir.exists()) {
            chooser.setCurrentDirectory(defaultDir);
        }

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedCsvFile = chooser.getSelectedFile();
            lblCsvPath.setText("CSV: " + selectedCsvFile.getAbsolutePath());
        }
    }

    private void chooseImageFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục chứa ảnh sản phẩm");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFolder = chooser.getSelectedFile();
            lblImageFolderPath.setText("Thư mục ảnh: " + selectedImageFolder.getAbsolutePath());
        }
    }

    private void startImportProcess() {
        if (selectedCsvFile == null || !selectedCsvFile.exists()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không tìm thấy file CSV. Vui lòng chọn file CSV trước.",
                    "Thiếu file CSV",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        btnStartImport.setEnabled(false);
        btnChooseCsv.setEnabled(false);
        btnChooseImageFolder.setEnabled(false);
        lblStatus.setText("Đang xử lý import... vui lòng đợi.");
        progressBar.setValue(0);

        CompletableFuture.runAsync(() -> {
            try {
                if (selectedImageFolder != null && selectedImageFolder.exists()) {
                    SwingUtilities.invokeLater(() -> lblStatus.setText("Đang copy ảnh vào resources..."));
                    copyImagesToResourceFolder(selectedImageFolder);
                }

                SwingUtilities.invokeLater(() -> lblStatus.setText("Đang import CSV vào database..."));

                new ProductImportService().importProductCSV(
                        selectedCsvFile.getAbsolutePath(),
                        progress -> SwingUtilities.invokeLater(() -> progressBar.setValue(progress))
                );

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }).thenRun(() -> {
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                lblStatus.setText("Hoàn tất!");

                if (parentView != null) {
                    parentView.loadDataToTable();
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Import sản phẩm và ảnh hoàn tất!",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE
                );

                dispose();
            });

        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Lỗi import: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );

                btnStartImport.setEnabled(true);
                btnChooseCsv.setEnabled(true);
                btnChooseImageFolder.setEnabled(true);
                lblStatus.setText("Import thất bại.");
            });
            return null;
        });
    }

    private void copyImagesToResourceFolder(File sourceFolder) throws Exception {
        File destFolder = new File("src/main/resources/view/image/products");

        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        File[] files = sourceFolder.listFiles();

        if (files == null) {
            return;
        }

        int copied = 0;

        for (File file : files) {
            if (file == null || !file.isFile()) {
                continue;
            }

            String name = file.getName().toLowerCase();

            if (!(name.endsWith(".png")
                    || name.endsWith(".jpg")
                    || name.endsWith(".jpeg")
                    || name.endsWith(".gif"))) {
                continue;
            }

            File destFile = new File(destFolder, file.getName());

            Files.copy(
                    file.toPath(),
                    destFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            copied++;
        }

        System.out.println("[ImportProductDialog] Copied images: " + copied);
        System.out.println("[ImportProductDialog] Destination: " + destFolder.getAbsolutePath());
    }
}
