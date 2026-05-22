package view;

import business.kpi.KpiCsvParser;
import business.kpi.KpiDataService;
import model.employee.EmployeePerformance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import view.components.IconHelper;

/**
 * Dialog để import dữ liệu KPI từ file CSV. Đây là JDialog phụ, không phải
 * JFrame chính nên không dùng AppCloseHandler.
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
        setSize(900, 600);
        setResizable(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        mainPanel.add(createFilePanel(), BorderLayout.NORTH);
        mainPanel.add(createPreviewPanel(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Chọn File KPI (CSV)"));

        txtFilePath = new JTextField();
        txtFilePath.setEditable(false);
        txtFilePath.setPreferredSize(new Dimension(0, 30));

        btnBrowse = new JButton("Duyệt...");
        btnBrowse.setIcon(IconHelper.folder(18));
        btnBrowse.setPreferredSize(new Dimension(100, 30));
        btnBrowse.addActionListener(e -> browseFile());

        JButton btnSample = new JButton("Tạo File Mẫu");
        btnSample.setIcon(IconHelper.template(18));
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

        String[] columns = {
            "Mã NV",
            "Tên NV",
            "Số Đơn",
            "Doanh Thu",
            "Hoàn Thành (%)",
            "Giao Hàng (%)",
            "Chuyên Cần",
            "Điểm KPI"
        };

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

        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));

        lblStatus = new JLabel("Chưa chọn file");

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(220, 22));

        statusPanel.add(lblStatus, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.EAST);

        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        btnImport = new JButton("✓ Nhập Dữ Liệu");
        btnImport.setIcon(IconHelper.upload(20));
        btnImport.setFont(new Font("Arial", Font.BOLD, 12));
        btnImport.setBackground(new Color(39, 174, 96));
        btnImport.setForeground(Color.WHITE);
        btnImport.setPreferredSize(new Dimension(130, 35));
        btnImport.setFocusPainted(false);
        btnImport.setEnabled(false);
        btnImport.addActionListener(e -> importData());

        btnCancel = new JButton("✗ Hủy");
        btnCancel.setIcon(IconHelper.close(18));
        btnCancel.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancel.setBackground(new Color(231, 76, 60));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> dispose());

        panel.add(btnImport);
        panel.add(btnCancel);

        return panel;
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv")
        );
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            txtFilePath.setText(filePath);
            parseAndPreviewFile(filePath);
        }
    }

    private void parseAndPreviewFile(String filePath) {
        setLoadingState(true, "Đang phân tích file...");

        new SwingWorker<List<EmployeePerformance>, Void>() {
            @Override
            protected List<EmployeePerformance> doInBackground() throws Exception {
                return KpiCsvParser.parseKpiFile(filePath);
            }

            @Override
            protected void done() {
                try {
                    kpiData = get();
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
                    btnImport.setEnabled(!kpiData.isEmpty());

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            ImportKpiDialog.this,
                            "Lỗi: " + e.getMessage(),
                            "Lỗi tải file",
                            JOptionPane.ERROR_MESSAGE
                    );

                    lblStatus.setText("Lỗi: " + e.getMessage());
                    btnImport.setEnabled(false);

                } finally {
                    setLoadingState(false, lblStatus.getText());
                }
            }
        }.execute();
    }

    private void importData() {
        if (kpiData == null || kpiData.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không có dữ liệu để nhập",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btnImport.setEnabled(false);
        btnBrowse.setEnabled(false);
        btnCancel.setEnabled(false);

        lblStatus.setText("Đang nhập dữ liệu KPI...");
        progressBar.setMaximum(kpiData.size());
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                KpiDataService.importKpiData(kpiData);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    progressBar.setValue(kpiData.size());
                    lblStatus.setText("✓ Nhập thành công " + kpiData.size() + " nhân viên");

                    JOptionPane.showMessageDialog(
                            ImportKpiDialog.this,
                            "✅ Nhập dữ liệu KPI thành công!\n\n"
                            + "Số nhân viên: " + kpiData.size() + "\n"
                            + "Ghi chú: Nhân viên chưa bán được đơn nào sẽ không được tính đạt KPI",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    dispose();

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            ImportKpiDialog.this,
                            "Lỗi nhập dữ liệu KPI: " + e.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );

                    lblStatus.setText("Lỗi nhập dữ liệu KPI");
                    btnImport.setEnabled(true);
                    btnBrowse.setEnabled(true);
                    btnCancel.setEnabled(true);
                    progressBar.setIndeterminate(false);

                }
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

                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }

                KpiCsvParser.createSampleCsvFile(filePath);

                JOptionPane.showMessageDialog(
                        this,
                        "File mẫu đã tạo tại:\n" + filePath,
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Lỗi tạo file mẫu: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void setLoadingState(boolean loading, String message) {
        lblStatus.setText(message);
        progressBar.setIndeterminate(loading);
        btnBrowse.setEnabled(!loading);
        btnImport.setEnabled(!loading && kpiData != null && !kpiData.isEmpty());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImportKpiDialog dialog = new ImportKpiDialog(null);
            dialog.setVisible(true);
        });
    }
}
