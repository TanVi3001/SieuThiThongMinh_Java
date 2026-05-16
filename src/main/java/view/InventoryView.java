package view;

import business.service.AuthorizationService;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class InventoryView extends JPanel {

    // --- CẤU HÌNH MÀU SẮC MODERN UI ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color colorWarning = new Color(255, 152, 0); // Cam cảnh báo
    private final Color colorDanger = new Color(244, 67, 54);  // Đỏ hết hàng
    private final Color colorSuccess = new Color(76, 175, 80); // Xanh nhập hàng

    // --- UI COMPONENTS ---
    private JTable tblInventory;
    private DefaultTableModel tableModel;
    private JComboBox<String> cbStoreFilter;
    private JButton btnInbound, btnOutbound, btnAuditLog;
    private JLabel lblTotalItems, lblLowStock, lblOutOfStock;

    public InventoryView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        
        // Load dữ liệu mẫu (Tùng sẽ thay bằng hàm gọi Database sau)
        loadInventoryData(); 
    }

    private void initUI() {
        // ==========================================
        // 1. HEADER (Tiêu đề + Bộ lọc chi nhánh)
        // ==========================================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản Lý Tồn Kho");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Theo dõi số lượng thực tế & Điều chỉnh kho hàng");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        filterPanel.setOpaque(false);
        JLabel lblFilter = new JLabel("Lọc theo kho:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFilter.setForeground(textDark);
        
        // Combobox chọn Kho (Sẽ load từ bảng STORES)
        cbStoreFilter = new JComboBox<>(new String[]{"Tất cả chi nhánh", "Kho Trung Tâm (ST001)", "Kho Thủ Đức (ST002)"});
        cbStoreFilter.setPreferredSize(new Dimension(200, 38));
        cbStoreFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStoreFilter.setBackground(Color.WHITE);

        filterPanel.add(lblFilter);
        filterPanel.add(cbStoreFilter);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER TỔNG (Thẻ Thống kê + Bảng dữ liệu)
        // ==========================================
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);

        // --- 2.1 THẺ THỐNG KÊ (Summary Cards) ---
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setPreferredSize(new Dimension(0, 100));

        lblTotalItems = new JLabel("0", SwingConstants.LEFT);
        lblLowStock = new JLabel("0", SwingConstants.LEFT);
        lblOutOfStock = new JLabel("0", SwingConstants.LEFT);

        cardsPanel.add(createSummaryCard("Tổng mặt hàng", lblTotalItems, primaryBlue));
        cardsPanel.add(createSummaryCard("Sắp hết hàng (<10)", lblLowStock, colorWarning));
        cardsPanel.add(createSummaryCard("Hết sạch hàng (0)", lblOutOfStock, colorDanger));

        centerPanel.add(cardsPanel, BorderLayout.NORTH);

        // --- 2.2 BẢNG TỒN KHO & CÁC NÚT THAO TÁC ---
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Thanh công cụ phía trên bảng (Nút Nhập / Xuất / Lịch sử)
        JPanel tableToolsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableToolsPanel.setOpaque(false);
        
        btnInbound  = createCustomButton("Nhập Kho", colorSuccess, Color.WHITE, IconHelper.add(20));
        btnOutbound = createCustomButton("Xuất / Hủy", colorDanger,  Color.WHITE, IconHelper.delete(20));
        btnAuditLog = createCustomButton("Lịch sử biến động", textGray,     Color.WHITE, IconHelper.history(20));
        
        tableToolsPanel.add(btnInbound);
        tableToolsPanel.add(btnOutbound);
        tableToolsPanel.add(btnAuditLog);
        tableCard.add(tableToolsPanel, BorderLayout.NORTH);

        // Khởi tạo bảng
        tableModel = new DefaultTableModel(new Object[]{"Mã SP", "Tên Sản Phẩm", "Số Lượng Tồn", "Đơn Vị", "Chi Nhánh", "Cập Nhật Cuối"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblInventory = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblInventory);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ GIAO DIỆN (UI HELPERS)
    // ==========================================
    private JPanel createSummaryCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(15, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accentColor), // Vạch màu bên trái
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(textGray);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(textDark);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18,18,Image.SCALE_SMOOTH)));
            btn.setIconTextGap(8);
        }
        btn.setForeground(fg);
        btn.setPreferredSize(new Dimension(140, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setupTableStyle() {
        tblInventory.setRowHeight(35);
        tblInventory.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblInventory.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblInventory.getTableHeader().setBackground(bgLight);
        tblInventory.getTableHeader().setReorderingAllowed(false);
        tblInventory.setShowVerticalLines(false);
        tblInventory.setSelectionBackground(new Color(237, 242, 255));
        tblInventory.setSelectionForeground(textDark);

        // THUẬT TOÁN ĐỔ MÀU CẢNH BÁO TỰ ĐỘNG
        tblInventory.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Lấy giá trị cột "Số Lượng Tồn" (Cột số 2)
                int quantity = Integer.parseInt(table.getModel().getValueAt(row, 2).toString());
                
                if (!isSelected) {
                    if (quantity == 0) {
                        c.setForeground(colorDanger); // Đỏ nếu hết hàng
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if (quantity < 10) {
                        c.setForeground(colorWarning); // Cam nếu sắp hết
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setForeground(textDark); // Đen bình thường
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                }
                return c;
            }
        });
    }

    class RoundedPanel extends JPanel {
        private int radius;
        private Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ SỰ KIỆN & LOGIC NGHIỆP VỤ
    // ==========================================
    private void initEvents() {
        btnInbound.addActionListener(e -> handleStockAdjustment(true));
        btnOutbound.addActionListener(e -> handleStockAdjustment(false));
        
        btnAuditLog.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Chức năng xem Lịch sử biến động đang được phát triển!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void handleStockAdjustment(boolean isInbound) {
        int row = tblInventory.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một mặt hàng từ bảng để thao tác!", "Nhắc nhở", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String prodId = tblInventory.getValueAt(row, 0).toString();
        String prodName = tblInventory.getValueAt(row, 1).toString();
        int currentQty = Integer.parseInt(tblInventory.getValueAt(row, 2).toString());

        String actionName = isInbound ? "NHẬP KHO" : "XUẤT / HỦY KHO";
        
        // Tạo hộp thoại nhập liệu tùy chỉnh
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 10));
        panel.add(new JLabel("Đang thao tác: " + prodName + " (" + prodId + ")"));
        panel.add(new JLabel("Tồn kho hiện tại: " + currentQty));
        
        JTextField txtQty = new JTextField();
        panel.add(new JLabel("Nhập số lượng " + (isInbound ? "cộng thêm:" : "trừ đi:")));
        panel.add(txtQty);

        JTextField txtReason = new JTextField();
        panel.add(new JLabel("Lý do (Bắt buộc):"));
        panel.add(txtReason);

        int result = JOptionPane.showConfirmDialog(this, panel, actionName, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int adjustQty = Integer.parseInt(txtQty.getText().trim());
                String reason = txtReason.getText().trim();

                if (adjustQty <= 0) {
                    JOptionPane.showMessageDialog(this, "Số lượng phải lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (reason.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Vui lòng ghi rõ lý do để lưu vào nhật ký!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!isInbound && adjustQty > currentQty) {
                    JOptionPane.showMessageDialog(this, "Số lượng xuất/hủy không được lớn hơn tồn kho hiện tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // TODO: Gọi lệnh SQL cập nhật bảng INVENTORY và chèn vào bảng AUDIT_LOG ở đây
                // Ví dụ: InventorySql.getInstance().adjustStock(prodId, adjustQty, isInbound, reason, currentUser);

                JOptionPane.showMessageDialog(this, "Cập nhật kho thành công!\nLý do: " + reason, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadInventoryData(); 

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số lượng nhập vào không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // ==========================================
    // HÀM LẤY DỮ LIỆU TỒN KHO THỰC TẾ TỪ DATABASE
    // ==========================================
    private void loadInventoryData() {
        tableModel.setRowCount(0); // Xóa sạch bảng trắng tinh
        
        int totalItems = 0;
        int lowStock = 0;
        int outOfStock = 0;

        try {
            // Lấy danh sách sản phẩm thật từ Database (những món bạn vừa import từ CSV)
            java.util.List<model.product.Product> list = business.sql.prod_inventory.ProductsSql.getInstance().selectAll();
            
            for (model.product.Product p : list) {
                String id = p.getProductId() != null ? p.getProductId() : "";
                String name = p.getProductName() != null ? p.getProductName() : "";
                int qty = p.getQuantity();
                String unit = p.getUnit() != null ? p.getUnit() : "Cái";
                String store = p.getStoreId() != null ? p.getStoreId() : "ST001";
                
                // Nếu class Product của bạn có biến ngày tháng thì thay vào, tạm thời mình để cứng
                String lastUpdated = "30/04/2026"; 

                // Đẩy dữ liệu thật vào bảng
                tableModel.addRow(new Object[]{id, name, qty, unit, store, lastUpdated});

                // Đếm số lượng cho các thẻ thống kê cảnh báo
                totalItems++;
                if (qty == 0) {
                    outOfStock++;
                } else if (qty < 10) {
                    lowStock++;
                }
            }

            // Cập nhật các con số lên 3 thẻ thống kê trên cùng
            lblTotalItems.setText(String.valueOf(totalItems));
            lblLowStock.setText(String.valueOf(lowStock));
            lblOutOfStock.setText(String.valueOf(outOfStock));

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải dữ liệu Tồn kho!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}