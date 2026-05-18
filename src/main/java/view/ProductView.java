package view;

import business.sql.prod_inventory.ProductsSql;
import business.sql.prod_inventory.ProductUnitsSql;
import business.service.UnitOfMeasureService;
import business.service.AuthorizationService;
import common.utils.Validator;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import model.product.Product;
import model.product.ProductUnit;

import javax.swing.filechooser.FileNameExtensionFilter;
 
import view.components.IconHelper;
 
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import javax.swing.table.DefaultTableCellRenderer;


public class ProductView extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private JTextField txtName, txtPrice, txtQuantity;
    private JComboBox<String> cbCategory, cbSearch;
    private JLabel lblImagePreview;
    private String selectedImagePath = null;
    private String selectedSupplierId = "SUP_01";  
    private String selectedStoreId = "ST01";     

    private JTable tblProducts;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch, btnExportPDF, btnUnitConfig, btnImport;
    private JPanel btnGrid;

    private List<String> categoryList = new ArrayList<>();
    private List<String> productNameList = new ArrayList<>();
    
    private JLabel lblImageSectionTitle;
    private JButton btnChooseImage;

    public ProductView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        loadAutoCompleteData();
        initUI();
        initEvents();
        loadDataToTable();

        // ---------------------------------------------------------
        // ĐÃ FIX LỖI REAL-TIME Ở ĐÂY: Bọc SwingUtilities.invokeLater
        // ---------------------------------------------------------
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.PRODUCTS || e.getType() == AppEventType.INVENTORY) {
                // Bắt buộc phải dùng invokeLater để đưa luồng phụ về luồng UI chính
                SwingUtilities.invokeLater(() -> {
                    refreshTable();
                });
            }
        });

        if (AuthorizationService.isCashier()) {
            btnAdd.setVisible(false);
            btnUpdate.setVisible(false);
            btnDelete.setVisible(false);
            btnUnitConfig.setVisible(false);
            btnImport.setVisible(false);
            // Giữ lại nút Làm mới — chỉ xóa các nút thừa khỏi grid
            btnGrid.removeAll();
            btnGrid.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
            btnGrid.setPreferredSize(new Dimension(280, 50));
            btnGrid.setMinimumSize(new Dimension(280, 50));
            btnGrid.setMaximumSize(new Dimension(280, 50));
            btnGrid.add(btnClear);
            btnGrid.revalidate();
            btnGrid.repaint();
        }
        
        // Manager chỉ xem + làm mới + ẩn nút CSV 
        if (AuthorizationService.isStoreManager()) {
            btnAdd.setVisible(false);
            btnDelete.setVisible(false);
            btnUnitConfig.setVisible(false);
            btnImport.setVisible(false);
            // Chỉ giữ Cập nhật + Làm mới
            btnGrid.removeAll();
            btnGrid.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
            btnGrid.setPreferredSize(new Dimension(280, 50));
            btnGrid.setMinimumSize(new Dimension(280, 50));
            btnGrid.setMaximumSize(new Dimension(280, 50));
            btnGrid.add(btnUpdate);
            btnGrid.add(btnClear);
            btnGrid.revalidate();
            btnGrid.repaint();
        }       

        // ẨN PHẦN HÌNH ẢNH VỚI NHÂN VIÊN KHO
        if (AuthorizationService.isWarehouseStaff()) {
            lblImagePreview.setVisible(false);
            btnChooseImage.setVisible(false);
            lblImageSectionTitle.setVisible(false);
            tblProducts.getColumnModel().getColumn(5).setMinWidth(0);
            tblProducts.getColumnModel().getColumn(5).setMaxWidth(0);
            tblProducts.getColumnModel().getColumn(5).setWidth(0);
            // Chỉ giữ Import CSV + Làm mới
            btnAdd.setVisible(false);
            btnUpdate.setVisible(false);
            btnDelete.setVisible(false);
            btnUnitConfig.setVisible(false);
            btnGrid.removeAll();
            btnGrid.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
            btnGrid.setPreferredSize(new Dimension(280, 50));
            btnGrid.setMinimumSize(new Dimension(280, 50));
            btnGrid.setMaximumSize(new Dimension(280, 50));
            btnGrid.add(btnClear);
            btnGrid.revalidate();
            btnGrid.repaint();
            btnExportPDF.setEnabled(false);
            btnExportPDF.setVisible(false);
        }
    }

    private void loadAutoCompleteData() {
        categoryList.clear();
        // Load từ DB thay vì hardcode
        try {
            List<model.product.Category> cats = business.sql.prod_inventory.CategoriesSql.getInstance().selectAll();
            for (var c : cats) {
                categoryList.add(c.getCategoryId() + " - " + c.getCategoryName());
            }
        } catch (Exception e) {
            // fallback hardcode nếu lỗi
            categoryList.add("CAT001 - Thực phẩm khô");
            categoryList.add("CAT002 - Đồ uống");
            categoryList.add("CAT003 - Hóa mỹ phẩm");
            categoryList.add("CAT004 - Bánh kẹo");
            categoryList.add("CAT005 - Thực phẩm tươi sống");
        }

        productNameList.clear();
        try {
            List<Product> list = ProductsSql.getInstance().selectAll();
            for (Product p : list) {
                if (p.getProductName() != null && !p.getProductName().isBlank()) {
                    productNameList.add(p.getProductName().trim());
                    // Thêm cả mã SP vào gợi ý
                    productNameList.add(p.getProductId() + " - " + p.getProductName().trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Danh Mục Sản Phẩm");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Quản lý thông tin, giá bán và số lượng tồn kho");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        toolPanel.setOpaque(false);

        cbSearch = new JComboBox<>();
        styleSearchBox(cbSearch, "Nhập tên sản phẩm để tìm...");
        setupAutoComplete(cbSearch, productNameList);

        JPanel searchFieldWrapper = new JPanel(new BorderLayout(5, 0));
        searchFieldWrapper.setBackground(Color.WHITE);
        searchFieldWrapper.setPreferredSize(new Dimension(300, 45));
        searchFieldWrapper.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 25),
                new EmptyBorder(0, 15, 0, 15)
        ));
        JLabel searchIconLabel = new JLabel(IconHelper.search(16));
        searchFieldWrapper.add(searchIconLabel, BorderLayout.WEST);
        searchFieldWrapper.add(cbSearch, BorderLayout.CENTER);

        btnSearch   = createCustomButton("Tìm kiếm", primaryBlue, Color.WHITE, IconHelper.search(20));
        btnExportPDF = createCustomButton("Xuất Excel", new Color(0, 163, 108), Color.WHITE, IconHelper.export(20));
        btnImport   = createCustomButton("Nhập CSV", new Color(103, 58, 183), Color.WHITE, IconHelper.file(20));

        toolPanel.add(searchFieldWrapper);
        toolPanel.add(btnSearch);
        toolPanel.add(btnExportPDF);
        toolPanel.add(btnImport);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(toolPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(20, 0));
        centerPanel.setOpaque(false);

        RoundedPanel formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(350, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        txtName     = createTextField("Nhập tên...");
        txtPrice    = createTextField("Nhập giá (VNĐ)...");
        txtQuantity = createTextField("Nhập số lượng...");

        cbCategory = new JComboBox<>();
        styleComboBox(cbCategory, "Chọn hoặc nhập mã/tên loại...");
        setupAutoComplete(cbCategory, categoryList);
        cbCategory.setRenderer(new view.components.CategoryComboRenderer(18));

        int y = 0;
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Tên sản phẩm (*)"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtName, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Giá bán (*)"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtPrice, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Số lượng (*)"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtQuantity, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Loại sản phẩm (*)"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(cbCategory, gbc);

        // Hình ảnh sản phẩm
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        lblImageSectionTitle = createLabel("Hình ảnh");
        formCard.add(lblImageSectionTitle, gbc);

        lblImagePreview = new JLabel("Chưa chọn ảnh", SwingConstants.CENTER);
        lblImagePreview.setPreferredSize(new Dimension(180, 120));
        lblImagePreview.setBorder(BorderFactory.createDashedBorder(new Color(180, 180, 180), 2, 4));
        lblImagePreview.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblImagePreview.setForeground(new Color(150, 150, 150));
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(lblImagePreview, gbc);

        // FIX: dùng lambda thay vì anonymous ActionListener class để tránh thiếu import
        btnChooseImage = createCustomButton("Chọn ảnh", new Color(108, 117, 125), Color.WHITE, null);
        btnChooseImage.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Ảnh (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
            if (fc.showOpenDialog(ProductView.this) == JFileChooser.APPROVE_OPTION) {
                java.io.File srcFile = fc.getSelectedFile();
                String fileName = srcFile.getName();
                try {
                    // Lưu vào src/main/resources để không bị mất khi Clean and Build
                    java.io.File destDir = new java.io.File("src/main/resources/view/image/products/");
                    destDir.mkdirs();
                    java.nio.file.Files.copy(srcFile.toPath(),
                            new java.io.File(destDir, fileName).toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    // Copy luôn vào target để dùng được ngay không cần build lại
                    java.net.URL resUrl = ProductView.this.getClass().getClassLoader().getResource("view/image/products");
                    if (resUrl != null) {
                        java.io.File targetDir = new java.io.File(resUrl.toURI());
                        java.nio.file.Files.copy(srcFile.toPath(),
                                new java.io.File(targetDir, fileName).toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // FIX: gán selectedImagePath và hiển thị preview nằm TRONG block if, sau try-catch
                selectedImagePath = fileName;
                ImageIcon icon = new ImageIcon(
                        new ImageIcon(srcFile.getAbsolutePath())
                                .getImage()
                                .getScaledInstance(180, 120, Image.SCALE_SMOOTH)
                );
                lblImagePreview.setIcon(icon);
                lblImagePreview.setText("");
            }
        });
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 30, 0);
        formCard.add(btnChooseImage, gbc);

        btnAdd       = createCustomButton("Thêm", primaryBlue, Color.WHITE, IconHelper.add(20));
        btnUpdate    = createCustomButton("Cập nhật", new Color(255, 153, 0), Color.BLACK, IconHelper.edit(20));
        btnDelete    = createCustomButton("Xóa", new Color(220, 53, 69), Color.WHITE, IconHelper.delete(20));
        btnClear     = createCustomButton("Làm mới", new Color(165, 177, 194), Color.WHITE, IconHelper.refresh(20));
        btnUnitConfig = createCustomButton("Đơn vị", new Color(103, 58, 183), Color.WHITE, IconHelper.settings(20));

        btnGrid = new JPanel(new GridLayout(3, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);
        btnGrid.add(btnUnitConfig);

        gbc.gridy = y++;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        formCard.add(btnGrid, gbc);
        centerPanel.add(formCard, BorderLayout.WEST);

        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(new Object[]{"Mã SP", "Tên sản phẩm", "Giá", "Số lượng", "Loại SP", "Ảnh"}, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 5) return ImageIcon.class; // cột Ảnh
                return Object.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblProducts = new JTable(tableModel);
        tblProducts.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblProducts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblProducts.getTableHeader().setBackground(bgLight);
        tblProducts.getTableHeader().setReorderingAllowed(false);
        tblProducts.setShowVerticalLines(false);
        tblProducts.setSelectionBackground(new Color(237, 242, 255));
        tblProducts.setSelectionForeground(textDark);
        tblProducts.setRowHeight(60); // tăng chiều cao hàng cho ảnh

        tblProducts.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblProducts.getColumnModel().getColumn(5).setMinWidth(80);
        tblProducts.setRowHeight(70);
        
        // Cột "Loại SP" (index 4) hiển thị icon + text
        tblProducts.getColumnModel()
                .getColumn(4)
                .setCellRenderer(new view.components.CategoryTableRenderer(20));
        
        // Tô màu hàng gần hết
        tblProducts.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int qty = Integer.parseInt(table.getValueAt(row, 3).toString());
                    if (!isSelected) {
                        if (qty <= 0) {
                            c.setBackground(new Color(255, 205, 210)); // đỏ — hết hàng
                            c.setForeground(new Color(180, 0, 0));
                        } else if (qty < 20) {
                            c.setBackground(new Color(255, 243, 205)); // vàng — sắp hết
                            c.setForeground(new Color(160, 90, 0));
                        } else {
                            c.setBackground(Color.WHITE);
                            c.setForeground(textDark);
                        }
                    }
                } catch (Exception ignored) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblProducts);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void styleComboBox(JComboBox<String> cb, String placeholder) {
        cb.setPreferredSize(new Dimension(280, 40));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setBackground(Color.WHITE);
        cb.setEditable(true);

        JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
        editor.putClientProperty("JTextField.placeholderText", placeholder);
        editor.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8), new EmptyBorder(5, 5, 5, 5)
        ));
    }

    private void setupAutoComplete(JComboBox<String> comboBox, List<String> originalItems) {
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();

        for (String item : originalItems) {
            comboBox.addItem(item);
        }
        comboBox.setSelectedItem("");

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN
                        || e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    comboBox.removeAllItems();

                    if (text.isEmpty()) {
                        for (String item : originalItems) {
                            comboBox.addItem(item);
                        }
                        comboBox.hidePopup();
                    } else {
                        boolean hasSuggestion = false;
                        for (String item : originalItems) {
                            if (item.toLowerCase().contains(text.toLowerCase())) {
                                comboBox.addItem(item);
                                hasSuggestion = true;
                            }
                        }
                        if (hasSuggestion) {
                            comboBox.showPopup();
                        } else {
                            comboBox.hidePopup();
                        }
                    }
                    editor.setText(text);
                });
            }
        });
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(textDark);
        return lbl;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(200, 40));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)
        ));
        return txt;
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
            btn.setIconTextGap(8);
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 25, 25);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return btn;
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
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundBorder implements javax.swing.border.Border {

        private Color color;
        private int radius;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    private void initEvents() {
        tblProducts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                tblProductsMouseClicked(evt);
            }
        });
        btnAdd.addActionListener(e -> btnAddActionPerformed());
        btnUpdate.addActionListener(e -> btnUpdateActionPerformed());
        btnDelete.addActionListener(e -> btnDeleteActionPerformed());
        btnClear.addActionListener(e -> btnClearActionPerformed());
        btnSearch.addActionListener(e -> btnSearchActionPerformed());
        btnExportPDF.addActionListener(e -> btnExportPDFActionPerformed());
        btnUnitConfig.addActionListener(e -> showUnitConfigDialog());
        btnImport.addActionListener(e -> handleImportCSV());
    }

    private void btnAddActionPerformed() {
        if (!validateInput()) {
            return;
        }

        Product p = getProductFromForm();
        if (p == null) {
            return;
        }

        ProductsSql dao = ProductsSql.getInstance();
        Product existingProduct = dao.findByExactName(p.getProductName());

        if (existingProduct != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Sản phẩm '" + p.getProductName() + "' đã có trong kho.\nBạn có muốn cộng dồn thêm " + p.getQuantity() + " vào số lượng hiện tại không?",
                    "Phát hiện trùng lặp", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (dao.addQuantity(existingProduct.getProductId(), p.getQuantity(), existingProduct.getStoreId())) {

                    SyncVersionDao.bumpVersion("INVENTORY");
                    SyncVersionDao.bumpVersion("PRODUCTS");

                    // REALTIME
                    RealtimeClient.send("PRODUCTS_CHANGED");
                    RealtimeClient.send("INVENTORY_CHANGED");

                    JOptionPane.showMessageDialog(this, "✅ Đã cộng dồn số lượng thành công!");
                    loadDataToTable();
                    btnClearActionPerformed();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Lỗi khi cộng dồn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            p.setProductId(dao.generateNextProductId());
            if (p.getSupplierId() == null || p.getSupplierId().trim().isEmpty()) {
                p.setSupplierId("SUP_01");
            }
            if (p.getStoreId() == null || p.getStoreId().trim().isEmpty()) {
                p.setStoreId("ST01");
            }
            if (p.getUnit() == null || p.getUnit().trim().isEmpty()) {
                p.setUnit("Cái");
            }

            if (dao.insert(p)) {

                SyncVersionDao.bumpVersion("PRODUCTS");
                SyncVersionDao.bumpVersion("INVENTORY");

                // REALTIME
                RealtimeClient.send("PRODUCTS_CHANGED");
                RealtimeClient.send("INVENTORY_CHANGED");

                JOptionPane.showMessageDialog(this, "✅ Thêm sản phẩm mới thành công!\nMã tự cấp: " + p.getProductId());
                loadDataToTable();
                if (!productNameList.contains(p.getProductName())) {
                    productNameList.add(p.getProductName());
                }
                btnClearActionPerformed();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Thêm thất bại! Vui lòng kiểm tra dữ liệu đầu vào.", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void btnUpdateActionPerformed() {
        int row = tblProducts.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một sản phẩm trong bảng để cập nhật!", "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idOld = tblProducts.getValueAt(row, 0).toString().trim();
        Product p = getProductFromForm();
        if (p == null) {
            return;
        }

        p.setProductId(idOld);

        if (ProductsSql.getInstance().update(p)) {

            SyncVersionDao.bumpVersion("PRODUCTS");
            SyncVersionDao.bumpVersion("INVENTORY");

            // REALTIME
            RealtimeClient.send("PRODUCTS_CHANGED");
            RealtimeClient.send("INVENTORY_CHANGED");

            JOptionPane.showMessageDialog(this, "✅ Cập nhật sản phẩm thành công!");
            loadDataToTable();
            btnClearActionPerformed();
        } else {
            JOptionPane.showMessageDialog(this, "❌ Cập nhật thất bại! Vui lòng kiểm tra Database.", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnDeleteActionPerformed() {
        int row = tblProducts.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một sản phẩm trong bảng để xóa!", "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id   = tblProducts.getValueAt(row, 0).toString().trim();
        String name = tblProducts.getValueAt(row, 1).toString().trim();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn ngừng kinh doanh và xóa sản phẩm: " + name + " (" + id + ")?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean usedInOrders = ProductsSql.getInstance().isUsedInOrders(id);

            if (ProductsSql.getInstance().delete(id)) {

                SyncVersionDao.bumpVersion("PRODUCTS");
                SyncVersionDao.bumpVersion("INVENTORY");

                // REALTIME
                RealtimeClient.send("PRODUCTS_CHANGED");
                RealtimeClient.send("INVENTORY_CHANGED");

                if (usedInOrders) {
                    JOptionPane.showMessageDialog(this,
                            "Sản phẩm [" + name + "] đã từng được bán/nhập kho.\nHệ thống đã chuyển sang trạng thái ẨN (Ngừng kinh doanh) thay vì xóa mất dữ liệu.",
                            "Đã ẩn sản phẩm an toàn",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "✅ Xóa sản phẩm [" + name + "] thành công!");
                }
                loadDataToTable();
                btnClearActionPerformed();
            } else {
                JOptionPane.showMessageDialog(this,
                        "❌ Không thể xóa sản phẩm.\nVui lòng kiểm tra cửa sổ Output Console để xem lỗi chi tiết!",
                        "Lỗi hệ thống",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void btnClearActionPerformed() {
        txtName.setText("");
        txtPrice.setText("");
        txtQuantity.setText("");

        ((JTextField) cbCategory.getEditor().getEditorComponent()).setText("");
        ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");

        selectedImagePath = null;
        lblImagePreview.setIcon(null);
        lblImagePreview.setText("Chưa chọn ảnh");

        tblProducts.clearSelection();
    }

    private void btnSearchActionPerformed() {
        JTextField editor = (JTextField) cbSearch.getEditor().getEditorComponent();
        String keyword = editor.getText().trim();

        List<Product> list = ProductsSql.getInstance().searchByName(keyword);
        fillTable(list);
    }

    private void btnExportPDFActionPerformed() {
        try {
            List<Map<String, Object>> productList = getAllProductsFromTable();

            if (productList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu file Excel");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(new java.io.File("SanPham_" + System.currentTimeMillis() + ".xlsx"));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                common.report.ExcelExporter.exportInventoryFromMap(productList, filePath);
                JOptionPane.showMessageDialog(this, "✅ Xuất Excel thành công!\nFile: " + filePath, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi xuất Excel: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void tblProductsMouseClicked(MouseEvent evt) {
        int row = tblProducts.getSelectedRow();
        if (row < 0) {
            return;
        }

        Object nameObj     = tblProducts.getValueAt(row, 1);
        Object priceObj    = tblProducts.getValueAt(row, 2);
        Object qtyObj      = tblProducts.getValueAt(row, 3);
        Object categoryObj = tblProducts.getValueAt(row, 4);

        txtName.setText(nameObj == null ? "" : nameObj.toString().trim());
        txtPrice.setText(priceObj == null ? "" : priceObj.toString().trim());
        txtQuantity.setText(qtyObj == null ? "" : qtyObj.toString().trim());

        JTextField editor = (JTextField) cbCategory.getEditor().getEditorComponent();
        editor.setText(categoryObj == null ? "" : categoryObj.toString().trim());

        // Hiển thị ảnh nếu có
        String productId = tblProducts.getValueAt(row, 0).toString().trim();
        Product selected = ProductsSql.getInstance().findById(productId);
        if (selected != null) {
            selectedSupplierId = selected.getSupplierId() != null ? selected.getSupplierId() : "SUP_01";
            selectedStoreId    = selected.getStoreId()    != null ? selected.getStoreId()    : "ST01";
        }

        if (selected != null && selected.getImagePath() != null && !selected.getImagePath().isEmpty()) {
            selectedImagePath = selected.getImagePath();
            // Dùng helper — hỗ trợ tên file có dấu tiếng Việt
            ImageIcon icon = loadImageThumb(selectedImagePath, 180, 120);
            if (icon != null) {
                lblImagePreview.setIcon(icon);
                lblImagePreview.setText("");
            } else {
                lblImagePreview.setIcon(null);
                lblImagePreview.setText("Không tìm thấy: " + selectedImagePath);
            }
        } else {
            selectedImagePath = null;
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Chưa có ảnh");
        }
    }

    private List<Map<String, Object>> getAllProductsFromTable() {
        List<Map<String, Object>> list = new ArrayList<>();
        int rowCount = tblProducts.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("productId",   tblProducts.getValueAt(i, 0));
            row.put("productName", tblProducts.getValueAt(i, 1));
            row.put("price",       tblProducts.getValueAt(i, 2));
            row.put("quantity",    tblProducts.getValueAt(i, 3));
            list.add(row);
        }
        return list;
    }

    public void loadDataToTable() {
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        tableModel.setRowCount(0);
        try {
            List<Product> list = ProductsSql.getInstance().selectAll();
            for (Product p : list) {
                ImageIcon thumb = null;
                System.out.println("SP: " + p.getProductId() + " | image: " + p.getImagePath()); // thêm dòng này
                if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
                    // DEBUG - xem đường dẫn thực tế
                    java.io.File debug = new java.io.File("src/main/resources/view/image/" + p.getImagePath());
                    System.out.println("=== DEBUG ảnh ===");
                    System.out.println("imagePath từ DB : " + p.getImagePath());
                    System.out.println("Tìm file ở      : " + debug.getAbsolutePath());
                    System.out.println("File tồn tại?   : " + debug.exists());
                
                    thumb = loadImageThumb(p.getImagePath(), 60, 45);
                    System.out.println("thumb loaded?   : " + (thumb != null));
                }
                Object[] row = {p.getProductId(), p.getProductName(), p.getBasePrice(), p.getQuantity(), p.getCategoryId(), thumb};
                tableModel.addRow(row);
                
                // Gửi alert real-time đến nhân viên kho nếu sắp hết
                if ((AuthorizationService.isStoreManager() || AuthorizationService.isAdmin())
                        && p.getQuantity() < 20) {
                    RealtimeClient.send("INVENTORY_ALERT:" + p.getProductId()
                            + ":" + p.getProductName()
                            + ":" + p.getQuantity());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshTable() {
        loadDataToTable();
    }

    private void fillTable(List<Product> list) {
        tableModel.setRowCount(0);
        for (Product p : list) {
            tableModel.addRow(new Object[]{p.getProductId(), p.getProductName(), p.getBasePrice(), p.getQuantity(), p.getCategoryId()});
        }
    }

    private boolean validateInput() {
        if (Validator.isEmpty(txtName.getText())) {
            JOptionPane.showMessageDialog(this, "Tên sản phẩm không được rỗng!");
            return false;
        }
        if (!Validator.isPositiveInteger(txtQuantity.getText())) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương!");
            return false;
        }
        try {
            new BigDecimal(txtPrice.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Giá không hợp lệ! Vui lòng chỉ nhập số.");
            return false;
        }
        return true;
    }

    private Product getProductFromForm() {
        Product p = new Product();
        String name      = txtName.getText().trim();
        String priceText = txtPrice.getText().trim();
        String qtyText   = txtQuantity.getText().trim();

        JTextField editor = (JTextField) cbCategory.getEditor().getEditorComponent();
        String categoryId = editor.getText().trim();

        if (categoryId.contains(" - ")) {
            categoryId = categoryId.split(" - ")[0].trim();
        }

        if (name.isEmpty() || priceText.isEmpty() || qtyText.isEmpty() || categoryId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ Tên, Giá, Số lượng và Loại SP!");
            return null;
        }

        try {
            p.setProductName(name);
            p.setBasePrice(new BigDecimal(priceText));
            p.setQuantity(Integer.parseInt(qtyText));
            p.setCategoryId(categoryId);
            p.setImagePath(selectedImagePath);
            p.setSupplierId(selectedSupplierId);
            p.setStoreId(selectedStoreId);
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private String getSelectedProductId() {
        int viewRow = tblProducts.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }

        int modelRow = tblProducts.convertRowIndexToModel(viewRow);
        Object value = tblProducts.getModel().getValueAt(modelRow, 0);
        return value == null ? null : value.toString().trim();
    }

    private void showUnitConfigDialog() {
        String productId = getSelectedProductId();
        if (productId == null || productId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một sản phẩm trong bảng để cấu hình đơn vị!", "Chưa chọn sản phẩm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Cấu hình Đơn vị tính", true);
        dialog.setSize(600, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(bgLight);

        int selectedRow = tblProducts.getSelectedRow();
        String productName = tblProducts.getValueAt(selectedRow, 1).toString();

        JLabel lblTitle = new JLabel("Cấu hình quy đổi: " + productName + " (" + productId + ")");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(textDark);
        lblTitle.setBorder(new EmptyBorder(15, 20, 10, 20));
        dialog.add(lblTitle, BorderLayout.NORTH);

        RoundedPanel tablePanel = new RoundedPanel(15, Color.WHITE);
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        DefaultTableModel unitModel = new DefaultTableModel(new Object[]{"Tên Đơn vị", "Tỷ lệ quy đổi", "Là ĐV Gốc"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable unitTable = new JTable(unitModel);
        unitTable.setRowHeight(30);
        unitTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        unitTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        unitTable.getTableHeader().setBackground(bgLight);
        unitTable.setShowVerticalLines(false);
        unitTable.setSelectionBackground(new Color(237, 242, 255));
        unitTable.setSelectionForeground(textDark);

        loadProductUnits(productId, unitModel);

        JScrollPane scrollPane = new JScrollPane(unitTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(0, 20, 15, 20));
        centerWrapper.add(tablePanel, BorderLayout.CENTER);
        dialog.add(centerWrapper, BorderLayout.CENTER);

        RoundedPanel formPanel = new RoundedPanel(15, Color.WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtUnitName = createTextField("VD: Thùng, Lốc, Hộp...");
        JTextField txtRate     = createTextField("VD: 1, 6, 24...");
        JCheckBox  chkBase     = new JCheckBox("Đây là đơn vị gốc (Tỷ lệ = 1)");
        chkBase.setOpaque(false);
        chkBase.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chkBase.setForeground(textDark);

        chkBase.addActionListener(e -> {
            if (chkBase.isSelected()) {
                txtRate.setText("1");
                txtRate.setEnabled(false);
            } else {
                txtRate.setEnabled(true);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.6;
        formPanel.add(createLabel("Tên đơn vị mới:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        formPanel.add(createLabel("Tỷ lệ quy đổi:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(txtUnitName, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(txtRate, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(chkBase, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = createCustomButton("Đóng", new Color(165, 177, 194), Color.WHITE, null);
        btnCancel.setPreferredSize(new Dimension(100, 40));
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createCustomButton("Lưu đơn vị", primaryBlue, Color.WHITE, IconHelper.add(18));
        btnSave.setPreferredSize(new Dimension(140, 40));

        btnSave.addActionListener(e -> {
            String uName = txtUnitName.getText().trim();
            String uRate = txtRate.getText().trim();

            if (uName.isEmpty() || uRate.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đủ Tên đơn vị và Tỷ lệ quy đổi!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                BigDecimal rate = new BigDecimal(uRate);
                if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Tỷ lệ quy đổi phải là số lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean ok = new UnitOfMeasureService().configureProductUnit(productId, uName, rate, chkBase.isSelected());
                if (ok) {
                    SyncVersionDao.bumpVersion("PRODUCTS");

                    // REALTIME: báo "product metadata" đổi
                    RealtimeClient.send("PRODUCTS_CHANGED");

                    JOptionPane.showMessageDialog(dialog, "✅ Đã lưu cấu hình đơn vị tính thành công!");
                    loadProductUnits(productId, unitModel);

                    txtUnitName.setText("");
                    txtRate.setText("");
                    chkBase.setSelected(false);
                    txtRate.setEnabled(true);
                } else {
                    JOptionPane.showMessageDialog(dialog, "❌ Lỗi cập nhật! (Tên đơn vị này có thể đã tồn tại)", "Lỗi Data", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Tỷ lệ quy đổi phải là một số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        gbc.gridy = 3;
        gbc.insets = new Insets(15, 5, 5, 5);
        formPanel.add(btnPanel, gbc);

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.setOpaque(false);
        southWrapper.setBorder(new EmptyBorder(0, 20, 20, 20));
        southWrapper.add(formPanel, BorderLayout.CENTER);

        dialog.add(southWrapper, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void loadProductUnits(String productId, DefaultTableModel model) {
        model.setRowCount(0);
        List<ProductUnit> units = ProductUnitsSql.getInstance().selectByProductId(productId);
        for (ProductUnit unit : units) {
            model.addRow(new Object[]{unit.getUnitId(), unit.getConversionRateToBase(), unit.getIsBaseUnit() == 1 ? "Có" : ""});
        }
    }

    private void handleImportCSV() {
        // Lấy frame cha chứa View này
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        
        // Gọi Dialog Import chuyên dụng mà chúng ta đã sửa (có ProgressBar chạy ngầm)
        // Truyền 'this' (ProductView) vào để Dialog có thể gọi refresh bảng sau khi xong
        ImportProductDialog dialog = new ImportProductDialog(topFrame, this);
        dialog.setVisible(true);
    }
    
    private void styleSearchBox(JComboBox<String> cb, String placeholder) {
        cb.setEditable(true);
        cb.setBorder(null);
        cb.setBackground(Color.WHITE);
        JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
        editor.putClientProperty("JTextField.placeholderText", placeholder);
        editor.setBorder(new EmptyBorder(0, 5, 0, 5));
    }

    // Load ảnh từ nhiều nguồn — ưu tiên File trực tiếp để tránh lỗi tên có dấu tiếng Việt
    private ImageIcon loadImageThumb(String imageName, int w, int h) {
        try {
            java.io.File f = new java.io.File("src/main/resources/view/image/products/" + imageName);
            if (f.exists() && f.isFile()) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                System.out.println("ImageIO.read result: " + img); // thêm dòng này
                if (img != null) {
                    return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
                }
                Image img2 = Toolkit.getDefaultToolkit().getImage(f.getAbsolutePath());
                MediaTracker tracker = new MediaTracker(new java.awt.Canvas());
                tracker.addImage(img2, 0);
                tracker.waitForID(0);
                System.out.println("Toolkit width: " + img2.getWidth(null)); // thêm dòng này
                if (img2 != null && img2.getWidth(null) > 0) {
                    return new ImageIcon(img2.getScaledInstance(w, h, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ImageIcon scale(ImageIcon icon, int w, int h) {
        return new ImageIcon(icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }
}