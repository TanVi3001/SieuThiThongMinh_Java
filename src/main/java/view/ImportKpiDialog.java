package view;

import business.kpi.KpiCsvParser;
import business.kpi.KpiDataService;
import model.employee.EmployeePerformance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Dialog để import dữ liệu KPI từ file CSV
 */
public class ImportKpiDialog extends JDialog {

    private JTextField txtFilePath;
    private JButton btnBrowse, btnImport, btnCancel;
    private JTable tblPreview;
    private DefaultTableModel previewModel;
    private List<EmployeePerformance> kpiData;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    public ImportKpiDialog(Frame owner) {
        super(owner, "Nhập dữ liệu KPI từ File", true);
        initUI();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Panel chọn file
        JPanel filePanel = createFilePanel();
        mainPanel.add(filePanel, BorderLayout.NORTH);

        // 2. Panel preview dữ liệu
        JPanel previewPanel = createPreviewPanel();
        mainPanel.add(previewPanel, BorderLayout.CENTER);

        // 3. Panel nút bấm
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Chọn File KPI (CSV)"));

        txtFilePath = new JTextField();
        txtFilePath.setEditable(false);
        txtFilePath.setPreferredSize(new Dimension(0, 30));

        btnBrowse = new JButton("Duyệt...");
        btnBrowse.setPreferredSize(new Dimension(100, 30));
        btnBrowse.addActionListener(e -> browseFile());

        JButton btnSample = new JButton("Tạo File Mẫu");
        btnSample.setPreferredSize(new Dimension(120, 30));
        btnSample.addActionListener(e -> createSampleFile());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(txtFilePath, BorderLayout.CENTER);
        inputPanel.add(btnBrowse, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(btnSample, BorderLayout.EAST);

        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Xem trước dữ liệu (Preview)"));

        // Bảng preview
        String[] columns = {"Mã NV", "Tên NV", "Số Đơn", "Doanh Thu", "Hoàn Thành (%)", "Giao Hàng (%)", "Chuyên Cần", "Điểm KPI"};
        previewModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblPreview = new JTable(previewModel);
        tblPreview.setRowHeight(25);
        tblPreview.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(tblPreview);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        lblStatus = new JLabel("Chưa chọn file");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        statusPanel.add(lblStatus, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);

        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        btnImport = new JButton("✓ Nhập Dữ Liệu");
        btnImport.setFont(new Font("Arial", Font.BOLD, 12));
        btnImport.setBackground(new Color(39, 174, 96));
        btnImport.setForeground(Color.WHITE);
        btnImport.setPreferredSize(new Dimension(130, 35));
        btnImport.setEnabled(false);
        btnImport.addActionListener(e -> importData());

        btnCancel = new JButton("✗ Hủy");
        btnCancel.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancel.setBackground(new Color(231, 76, 60));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnCancel.addActionListener(e -> dispose());

        panel.add(btnImport);
        panel.add(btnCancel);

        return panel;
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            txtFilePath.setText(filePath);
            parseAndPreviewFile(filePath);
        }
    }

    private void parseAndPreviewFile(String filePath) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    lblStatus.setText("Đang phân tích file...");
                    progressBar.setIndeterminate(true);

                    kpiData = KpiCsvParser.parseKpiFile(filePath);
                    
                    SwingUtilities.invokeLater(() -> {
                        previewModel.setRowCount(0);
                        for (EmployeePerformance ep : kpiData) {
                            previewModel.addRow(new Object[]{
                                ep.getEmployeeId(),
                                ep.getEmployeeName(),
                                ep.getTotalOrders(),
                                String.format("%.0f", ep.getRevenue()),
                                String.format("%.2f", ep.getCompletionRate()),
                                String.format("%.2f", ep.getDeliverySuccessRate()),
                                String.format("%.2f", ep.getAttendanceScore()),
                                String.format("%.2f", ep.getPerformanceScore())
                            });
                        }
                        
                        lblStatus.setText(String.format("Tải thành công: %d nhân viên", kpiData.size()));
                        btnImport.setEnabled(true);
                        progressBar.setIndeterminate(false);
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(ImportKpiDialog.this,
                            "Lỗi: " + e.getMessage(),
                            "Lỗi tải file",
                            JOptionPane.ERROR_MESSAGE);
                        lblStatus.setText("Lỗi: " + e.getMessage());
                        btnImport.setEnabled(false);
                        progressBar.setIndeterminate(false);
                    });
                }
                return null;
            }
        }.execute();
    }

    private void importData() {
        if (kpiData == null || kpiData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để nhập", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                lblStatus.setText("Đang nhập dữ liệu KPI...");
                progressBar.setMaximum(kpiData.size());
                progressBar.setValue(0);
                progressBar.setIndeterminate(false);

                try {
                    // Call KpiDataService.importKpiData - now returns void
                    KpiDataService.importKpiData(kpiData);
                    progressBar.setValue(kpiData.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(ImportKpiDialog.this,
                    "✅ Nhập dữ liệu KPI thành công!\n\n" +
                    "Số nhân viên: " + kpiData.size() + "\n" +
                    "Ghi chú: Nhân viên chưa bán được đơn nào sẽ không được tính đạt KPI",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
                lblStatus.setText("✓ Nhập thành công " + kpiData.size() + " nhân viên");
                progressBar.setValue(kpiData.size());
                dispose();
            }
        }.execute();
    }

    private void createSampleFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setSelectedFile(new File("KPI_Sample.csv"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                KpiCsvParser.createSampleCsvFile(filePath);
                JOptionPane.showMessageDialog(this,
                    "File mẫu đã tạo tại:\n" + filePath,
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Lỗi tạo file mẫu: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Main method để test
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImportKpiDialog dialog = new ImportKpiDialog(null);
            dialog.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
            dialog.setVisible(true);
        });
    }
}
