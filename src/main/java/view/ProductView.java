package view;

import business.sql.prod_inventory.ProductsSql;
import business.sql.prod_inventory.ProductUnitsSql;
import business.service.UnitOfMeasureService;
import business.service.AuthorizationService;
import common.utils.Validator;
import business.sql.prod_inventory.InventoryNotificationSql;
import business.service.SessionManager;

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
import javax.swing.SwingUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import model.product.Product;
import model.product.ProductUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import javax.swing.filechooser.FileNameExtensionFilter;

import view.components.IconHelper;

// Events + realtime + sync
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
    private static final int TABLE_IMAGE_WIDTH = 110;
    private static final int TABLE_IMAGE_HEIGHT = 80;

    private static final int DETAIL_IMAGE_WIDTH = 190;
    private static final int DETAIL_IMAGE_HEIGHT = 145;

    private JTextField txtName, txtPrice, txtQuantity;
    private JComboBox<String> cbCategory, cbSearch;
    private JLabel lblImagePreview;
    private String selectedImagePath = null;

    private JTable tblProducts;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch, btnExportPDF, btnUnitConfig, btnImport, btnEmergencyAlert;
    private JPanel productToolPanel;

    private RoundedPanel formCard;
    private JPanel tabContentPanel;
    private JPanel detailPanel;
    private JPanel overviewPanel;
    private JButton btnOverviewTab;
    private JButton btnDetailTab;
    private String currentProductTab = "DETAIL";
    private JPanel categoryCardPanel;
    private JScrollPane categoryScrollPane;
    private JLabel lblCurrentCategory;
    private JButton btnShowAllProducts;
    private final java.util.List<view.components.AnimatedRevealPanel> productOverviewAnimations = new java.util.ArrayList<>();

    private String selectedCategoryId = null;
    private final Map<String, String> categoryNameMap = new java.util.LinkedHashMap<>();
    private final List<Product> cachedProducts = new ArrayList<>();

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

        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.PRODUCTS || e.getType() == AppEventType.INVENTORY) {
                SwingUtilities.invokeLater(() -> {
                    refreshTable();
                });
            }
        });

        applyProductRolePermission();
    }

    private void loadAutoCompleteData() {
        categoryList.clear();
        categoryNameMap.clear();

        try {
            List<model.product.Category> cats = business.sql.prod_inventory.CategoriesSql.getInstance().selectAll();

            for (model.product.Category c : cats) {
                String categoryId = c.getCategoryId();
                String categoryName = c.getCategoryName();

                if (categoryId == null || categoryId.trim().isEmpty()) {
                    continue;
                }

                categoryId = categoryId.trim();
                categoryName = categoryName == null ? "" : categoryName.trim();

                categoryNameMap.put(categoryId, categoryName);
                categoryList.add(categoryId + " - " + categoryName);
            }
        } catch (Exception e) {
            categoryNameMap.put("CAT001", "Thực phẩm khô");
            categoryNameMap.put("CAT002", "Đồ uống");
            categoryNameMap.put("CAT003", "Hóa mỹ phẩm");
            categoryNameMap.put("CAT004", "Bánh kẹo");
            categoryNameMap.put("CAT005", "Thực phẩm tươi sống");

            categoryList.add("CAT001 - Thực phẩm khô");
            categoryList.add("CAT002 - Đồ uống");
            categoryList.add("CAT003 - Hóa mỹ phẩm");
            categoryList.add("CAT004 - Bánh kẹo");
            categoryList.add("CAT005 - Thực phẩm tươi sống");
        }
        sortCategoryNameMap();

        productNameList.clear();

        try {
            List<Product> list = loadProductsByCurrentScope();

            for (Product p : list) {
                if (p.getProductName() != null && !p.getProductName().isBlank()) {
                    productNameList.add(p.getProductName().trim());
                    productNameList.add(p.getProductId() + " - " + p.getProductName().trim());
                }

                String categoryId = p.getCategoryId();
                if (categoryId != null && !categoryId.trim().isEmpty() && !categoryNameMap.containsKey(categoryId.trim())) {
                    categoryNameMap.put(categoryId.trim(), "Danh mục " + categoryId.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sortCategoryNameMap() {
        List<Map.Entry<String, String>> entries = new ArrayList<>(categoryNameMap.entrySet());

        entries.sort((a, b) -> {
            int numA = extractCategoryNumber(a.getKey());
            int numB = extractCategoryNumber(b.getKey());
            return Integer.compare(numA, numB);
        });

        categoryNameMap.clear();
        categoryList.clear();

        for (Map.Entry<String, String> entry : entries) {
            String categoryId = entry.getKey();
            String categoryName = entry.getValue();

            categoryNameMap.put(categoryId, categoryName);
            categoryList.add(categoryId + " - " + categoryName);
        }
    }

    private int extractCategoryNumber(String categoryId) {
        if (categoryId == null) {
            return Integer.MAX_VALUE;
        }

        try {
            String digits = categoryId.replaceAll("\\D+", "");
            if (digits.isEmpty()) {
                return Integer.MAX_VALUE;
            }
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Sản phẩm & Danh mục");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Theo dõi danh mục, tồn kho");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        productToolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        productToolPanel.setOpaque(false);

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

        btnSearch = createCustomButton("Tìm kiếm", primaryBlue, Color.WHITE, IconHelper.search(20));
        btnExportPDF = createCustomButton("Xuất Excel", new Color(0, 163, 108), Color.WHITE, IconHelper.export(20));
        btnImport = createCustomButton("Nhập CSV", new Color(103, 58, 183), Color.WHITE, IconHelper.file(20));
//        btnEmergencyAlert = createCustomButton("Báo hết hàng", new Color(220, 53, 69), Color.WHITE, IconHelper.warning(20));
        btnEmergencyAlert = createCustomButton("Báo hết hàng", new Color(220, 53, 69), Color.WHITE, null);
        btnEmergencyAlert.setVisible(false);

        productToolPanel.add(searchFieldWrapper);
        productToolPanel.add(btnSearch);
        productToolPanel.add(btnExportPDF);
//        productToolPanel.add(btnImport);
        productToolPanel.add(btnEmergencyAlert);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(productToolPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);

        JPanel tabBar = buildTabBar();
        centerPanel.add(tabBar, BorderLayout.NORTH);

        tabContentPanel = new JPanel(new CardLayout());
        tabContentPanel.setOpaque(false);

        detailPanel = new JPanel(new BorderLayout(0, 15));
        detailPanel.setOpaque(false);

        JPanel categorySection = buildCategorySection();
        detailPanel.add(categorySection, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setOpaque(false);

        formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(350, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        txtName = createTextField("Nhập tên...");
        txtPrice = createTextField("Nhập giá (VNĐ)...");
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

        btnChooseImage = createCustomButton("Chọn ảnh", new Color(108, 117, 125), Color.WHITE, null);
        btnChooseImage.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ảnh (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File srcFile = fc.getSelectedFile();
                String fileName = srcFile.getName();
                // Copy ảnh vào resources/view/image/
                try {
                    java.net.URL resUrl = getClass().getClassLoader().getResource("view/image");
                    if (resUrl != null) {
                        java.io.File destDir = new java.io.File(resUrl.toURI());
                        java.io.File destFile = new java.io.File(destDir, fileName);
                        java.nio.file.Files.copy(srcFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                selectedImagePath = fileName; // Chỉ lưu tên file
                ImageIcon icon = new ImageIcon(new ImageIcon(srcFile.getAbsolutePath()).getImage().getScaledInstance(180, 120, java.awt.Image.SCALE_SMOOTH));
                lblImagePreview.setIcon(icon);
                lblImagePreview.setText("");
            }
        });
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 30, 0);
        formCard.add(btnChooseImage, gbc);

        btnAdd = createCustomButton("Thêm", primaryBlue, Color.WHITE, IconHelper.add(20));
        btnUpdate = createCustomButton("Cập nhật", new Color(255, 153, 0), Color.BLACK, IconHelper.edit(20));
        btnDelete = createCustomButton("Xóa", new Color(220, 53, 69), Color.WHITE, IconHelper.delete(20));
        btnClear = createCustomButton("Làm mới", new Color(165, 177, 194), Color.WHITE, IconHelper.refresh(20));
        btnUnitConfig = createCustomButton("Đơn vị", new Color(103, 58, 183), Color.WHITE, IconHelper.settings(20));

        JPanel btnGrid = new JPanel(new GridLayout(3, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);
        btnGrid.add(btnUnitConfig);

        gbc.gridy = y++;
        formCard.add(btnGrid, gbc);
        contentPanel.add(formCard, BorderLayout.WEST);

        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        /*
         * Layout bảng theo phong cách mẫu cũ:
         * Mã SP | Ảnh SP | Tên sản phẩm | SL trong kho | Giá bán | Loại hàng
         */
        tableModel = new DefaultTableModel(new Object[]{
            "Mã SP", "Ảnh SP", "Tên sản phẩm", "SL trong kho", "Giá bán", "Loại hàng"
        }, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 1) {
                    return ImageIcon.class;
                }
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblProducts = new JTable(tableModel);
        tblProducts.setRowHeight(96);
        tblProducts.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblProducts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblProducts.getTableHeader().setBackground(new Color(0, 105, 92));
        tblProducts.getTableHeader().setForeground(Color.WHITE);
        tblProducts.getTableHeader().setPreferredSize(new Dimension(0, 42));
        tblProducts.getTableHeader().setReorderingAllowed(false);
        tblProducts.setShowHorizontalLines(true);
        tblProducts.setShowVerticalLines(false);
        tblProducts.setGridColor(new Color(225, 225, 225));
        tblProducts.setIntercellSpacing(new Dimension(0, 1));
        tblProducts.setSelectionBackground(new Color(232, 245, 255));
        tblProducts.setSelectionForeground(textDark);
        tblProducts.setFillsViewportHeight(true);
        tblProducts.setAutoCreateRowSorter(true);

        tblProducts.getColumnModel().getColumn(0).setPreferredWidth(110); // Mã SP
        tblProducts.getColumnModel().getColumn(1).setPreferredWidth(150); // Ảnh SP
        tblProducts.getColumnModel().getColumn(2).setPreferredWidth(300); // Tên sản phẩm
        tblProducts.getColumnModel().getColumn(3).setPreferredWidth(130); // SL trong kho
        tblProducts.getColumnModel().getColumn(4).setPreferredWidth(130); // Giá bán
        tblProducts.getColumnModel().getColumn(5).setPreferredWidth(150); // Loại hàng

        tblProducts.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setOpaque(true);

                if (isSelected) {
                    panel.setBackground(table.getSelectionBackground());
                } else {
                    panel.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                }

                JLabel lbl = new JLabel();
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setVerticalAlignment(SwingConstants.CENTER);

                if (value instanceof ImageIcon imageIcon) {
                    lbl.setIcon(imageIcon);
                    lbl.setText("");
                } else {
                    lbl.setText("—");
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    lbl.setForeground(textGray);
                }

                panel.add(lbl);
                return panel;
            }
        });

        // Cột "Loại hàng" hiển thị icon + text.
        tblProducts.getColumnModel()
                .getColumn(5)
                .setCellRenderer(new view.components.CategoryTableRenderer(20));

        applyStockRowRenderer();
        JScrollPane scrollPane = new JScrollPane(tblProducts);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(tableCard, BorderLayout.CENTER);
        detailPanel.add(contentPanel, BorderLayout.CENTER);

        overviewPanel = buildOverviewPanel();

        tabContentPanel.add(overviewPanel, "OVERVIEW");
        tabContentPanel.add(detailPanel, "DETAIL");

        centerPanel.add(tabContentPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        if (AuthorizationService.isStoreManager() || AuthorizationService.isAdmin()) {
            switchProductTab("OVERVIEW");
        } else {
            switchProductTab("DETAIL");
        }

    }

    private JPanel buildTabBar() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        wrapper.setOpaque(false);

        btnOverviewTab = createCustomButton("Tổng quan", primaryBlue, Color.WHITE, null);
        btnDetailTab = createCustomButton("Chi tiết", Color.WHITE, textDark, null);

        btnOverviewTab.setPreferredSize(new Dimension(130, 40));
        btnDetailTab.setPreferredSize(new Dimension(110, 40));

        btnOverviewTab.addActionListener(e -> switchProductTab("OVERVIEW"));
        btnDetailTab.addActionListener(e -> switchProductTab("DETAIL"));

        if (AuthorizationService.isStoreManager() || AuthorizationService.isAdmin()) {
            wrapper.add(btnOverviewTab);
        }

        wrapper.add(btnDetailTab);

        return wrapper;
    }

    private void switchProductTab(String tab) {
        if (tab == null) {
            tab = "DETAIL";
        }

        if ("OVERVIEW".equals(tab) && !(AuthorizationService.isStoreManager() || AuthorizationService.isAdmin())) {
            tab = "DETAIL";
        }

        currentProductTab = tab;

        if (productToolPanel != null) {
            productToolPanel.setVisible("DETAIL".equals(tab));
        }

        if (tabContentPanel != null) {
            CardLayout cl = (CardLayout) tabContentPanel.getLayout();
            cl.show(tabContentPanel, tab);
        }

        if (btnOverviewTab != null) {
            boolean active = "OVERVIEW".equals(tab);
            btnOverviewTab.setBackground(active ? primaryBlue : Color.WHITE);
            btnOverviewTab.setForeground(active ? Color.WHITE : textDark);
            btnOverviewTab.repaint();
        }

        if (btnDetailTab != null) {
            boolean active = "DETAIL".equals(tab);
            btnDetailTab.setBackground(active ? primaryBlue : Color.WHITE);
            btnDetailTab.setForeground(active ? Color.WHITE : textDark);
            btnDetailTab.repaint();
        }

        if ("OVERVIEW".equals(tab)) {
            restartProductOverviewAnimations();
        }

        revalidate();
        repaint();
    }

    private JPanel buildCategorySection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Bộ lọc danh mục");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(textDark);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        lblCurrentCategory = new JLabel("Đang xem: Tất cả sản phẩm");
        lblCurrentCategory.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCurrentCategory.setForeground(textDark);
        lblCurrentCategory.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(210, 220, 245), 20),
                new EmptyBorder(8, 14, 8, 14)
        ));

        btnShowAllProducts = createCustomButton("Xem tất cả", primaryBlue, Color.WHITE, null);
        btnShowAllProducts.setPreferredSize(new Dimension(115, 36));
        btnShowAllProducts.addActionListener(e -> {
            selectedCategoryId = null;
            updateCurrentCategoryLabel();
            loadDataToTable();
        });

        filterPanel.add(lblCurrentCategory);
        filterPanel.add(btnShowAllProducts);

        top.add(title, BorderLayout.WEST);
        top.add(filterPanel, BorderLayout.EAST);

        categoryCardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        categoryCardPanel.setOpaque(false);

        categoryScrollPane = new JScrollPane(categoryCardPanel);
        categoryScrollPane.setBorder(BorderFactory.createEmptyBorder());
        categoryScrollPane.setOpaque(false);
        categoryScrollPane.getViewport().setOpaque(false);
        categoryScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        categoryScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        categoryScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        categoryScrollPane.setPreferredSize(new Dimension(0, 145));

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(categoryScrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel animateProductOverview(Component child, int delayMs, int distance) {
        view.components.AnimatedRevealPanel animated
                = new view.components.AnimatedRevealPanel(child, 900, delayMs, distance);
        productOverviewAnimations.add(animated);
        return animated;
    }

    private void restartProductOverviewAnimations() {
        SwingUtilities.invokeLater(() -> {
            for (view.components.AnimatedRevealPanel p : productOverviewAnimations) {
                if (p != null && p.isShowing()) {
                    p.restartAnimation();
                }
            }
        });
    }

    private JPanel buildOverviewPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setOpaque(false);

        JPanel kpiPanel = new JPanel(new GridLayout(1, 5, 15, 0));
        kpiPanel.setOpaque(false);

        int totalCategories = categoryNameMap.size();
        int totalProducts = cachedProducts.size();
        int totalQuantity = 0;
        int outOfStock = 0;
        int lowStock = 0;

        for (Product p : cachedProducts) {
            int qty = p.getQuantity();
            totalQuantity += qty;

            if (qty <= 0) {
                outOfStock++;
            } else if (qty <= 10) {
                lowStock++;
            }
        }

        kpiPanel.add(animateProductOverview(
                createOverviewKpiCard("Tổng danh mục", String.valueOf(totalCategories), new Color(67, 97, 238)),
                0,
                80
        ));
        kpiPanel.add(animateProductOverview(
                createOverviewKpiCard("Tổng mặt hàng", String.valueOf(totalProducts), new Color(0, 163, 108)),
                80,
                80
        ));
        kpiPanel.add(animateProductOverview(
                createOverviewKpiCard("Tổng tồn kho", String.valueOf(totalQuantity), new Color(103, 58, 183)),
                160,
                80
        ));
        kpiPanel.add(animateProductOverview(
                createOverviewKpiCard("Hết hàng", String.valueOf(outOfStock), new Color(220, 53, 69)),
                240,
                80
        ));
        kpiPanel.add(animateProductOverview(
                createOverviewKpiCard("Sắp hết", String.valueOf(lowStock), new Color(255, 153, 0)),
                320,
                80
        ));

        JPanel analyticsPanel = buildPowerBIPlaceholderPanel();

        JPanel centerContent = new JPanel(new BorderLayout(0, 15));
        centerContent.setOpaque(false);

        centerContent.add(
                animateProductOverview(buildOverviewCategoryAnalyticsPanel(), 120, 110),
                BorderLayout.NORTH
        );
        centerContent.add(analyticsPanel, BorderLayout.CENTER);

        root.add(kpiPanel, BorderLayout.NORTH);
        root.add(centerContent, BorderLayout.CENTER);

        return root;
    }

    private JPanel buildPowerBIPlaceholderPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setOpaque(false);

        JLabel title = new JLabel("Power BI Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(textDark);

        RoundedPanel card = new RoundedPanel(22, Color.WHITE);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setOpaque(false);

        JLabel mainTitle = new JLabel("Biểu đồ xu hướng tồn kho theo danh mục");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        mainTitle.setForeground(textDark);

        JLabel subTitle = new JLabel("Mô phỏng biểu đồ phân tích dạng Curve Line Chart - có thể thay bằng Power BI thật sau này.");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitle.setForeground(new Color(90, 100, 130));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        titleBox.add(mainTitle);
        titleBox.add(subTitle);

        JPanel chartPanel = new InventoryCurveChartPanel();

        leftPanel.add(titleBox, BorderLayout.NORTH);
        leftPanel.add(
                animateProductOverview(chartPanel, 0, 160),
                BorderLayout.CENTER
        );

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(330, 0));
        rightPanel.setLayout(new GridLayout(3, 1, 0, 12));

        rightPanel.add(animateProductOverview(
                createInsightBox(
                        "[TREND] Xu hướng tồn kho",
                        "Theo dõi mức tồn kho giữa các nhóm sản phẩm.",
                        new Color(237, 242, 255)
                ),
                180,
                120
        ));

        rightPanel.add(animateProductOverview(
                createInsightBox(
                        "[RISK] Rủi ro thiếu hàng",
                        "Điểm thấp thể hiện nhóm hàng cần theo dõi.",
                        new Color(255, 247, 230)
                ),
                280,
                120
        ));

        rightPanel.add(animateProductOverview(
                createInsightBox(
                        "[BI] Tích hợp Power BI",
                        "Có thể thay bằng WebView hoặc ảnh báo cáo Power BI.",
                        new Color(255, 235, 238)
                ),
                380,
                120
        ));

        card.add(leftPanel, BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(card, BorderLayout.CENTER);

        return animateProductOverview(wrapper, 0, 150);
    }

    private JPanel createInsightBox(String title, String desc, Color bg) {
        RoundedPanel box = new RoundedPanel(16, bg);
        box.setLayout(new BorderLayout(0, 8));
        box.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel lblTitle = new JLabel(
                "<html><div style='width:250px;'>"
                + escapeHtml(title)
                + "</div></html>"
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(textDark);

        JLabel lblDesc = new JLabel(
                "<html><div style='width:250px; color:#555555; line-height:1.45;'>"
                + escapeHtml(desc)
                + "</div></html>"
        );
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(90, 100, 130));

        box.add(lblTitle, BorderLayout.NORTH);
        box.add(lblDesc, BorderLayout.CENTER);

        return box;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private JPanel createOverviewKpiCard(String title, String value, Color accent) {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 18),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(textGray);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(accent);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildOverviewCategoryAnalyticsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JLabel title = new JLabel("Phân tích theo loại sản phẩm");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(textDark);

        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        cardsPanel.setOpaque(false);

        int totalInventory = 0;
        for (Product p : cachedProducts) {
            totalInventory += p.getQuantity();
        }

        for (String categoryId : categoryNameMap.keySet()) {
            String categoryName = getCategoryNameById(categoryId);

            int totalItems = 0;
            int totalQuantity = 0;
            int outOfStock = 0;
            int lowStock = 0;

            for (Product p : cachedProducts) {
                String pCategoryId = p.getCategoryId();

                if (pCategoryId == null || !pCategoryId.trim().equals(categoryId)) {
                    continue;
                }

                int qty = p.getQuantity();

                totalItems++;
                totalQuantity += qty;

                if (qty <= 0) {
                    outOfStock++;
                } else if (qty <= 10) {
                    lowStock++;
                }
            }

            double percent = totalInventory == 0 ? 0 : (totalQuantity * 100.0 / totalInventory);

            JPanel card = createOverviewCategoryCard(
                    categoryId,
                    categoryName,
                    totalItems,
                    totalQuantity,
                    outOfStock,
                    lowStock,
                    percent
            );

            cardsPanel.add(card);
        }

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        scroll.setPreferredSize(new Dimension(0, 155));

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createOverviewCategoryCard(
            String categoryId,
            String categoryName,
            int totalItems,
            int totalQuantity,
            int outOfStock,
            int lowStock,
            double percent
    ) {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setPreferredSize(new Dimension(300, 125));
        card.setLayout(new BorderLayout(12, 0));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 18),
                new EmptyBorder(15, 16, 15, 16)
        ));

        JLabel icon = new JLabel(getCategoryEmoji(categoryId), SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 34));
        icon.setPreferredSize(new Dimension(64, 70));

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel lblId = new JLabel(categoryId);
        lblId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblId.setForeground(primaryBlue);

        JLabel lblName = new JLabel(categoryName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblName.setForeground(textDark);

        JLabel lblMain = new JLabel(totalItems + " mặt hàng • " + totalQuantity + " tồn kho");
        lblMain.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMain.setForeground(new Color(90, 100, 130));

        JLabel lblStatus = new JLabel(
                "Hết: " + outOfStock
                + " | Sắp hết: " + lowStock
                + " | " + String.format("%.1f", percent) + "%"
        );
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));

        if (outOfStock > 0) {
            lblStatus.setForeground(new Color(220, 53, 69));
        } else if (lowStock > 0) {
            lblStatus.setForeground(new Color(255, 153, 0));
        } else {
            lblStatus.setForeground(new Color(25, 135, 84));
        }

        infoPanel.add(lblId);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(lblMain);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(lblStatus);

        card.add(icon, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);

        Color normalBg = Color.WHITE;
        Color hoverBg = new Color(248, 250, 255);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCategoryId = categoryId;
                updateCurrentCategoryLabel();
                filterProductsByCategory(categoryId);
                switchProductTab("DETAIL");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.bgColor = hoverBg;
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.bgColor = normalBg;
                card.repaint();
            }
        });

        return card;
    }

    private void refreshCategoryCards() {
        if (categoryCardPanel == null) {
            return;
        }

        categoryCardPanel.removeAll();

        List<Product> products = new ArrayList<>(cachedProducts);

        for (String categoryId : categoryNameMap.keySet()) {
            String categoryName = getCategoryNameById(categoryId);

            int totalItems = 0;
            int totalQuantity = 0;
            int outOfStock = 0;
            int lowStock = 0;

            for (Product p : products) {
                String pCategoryId = p.getCategoryId();

                if (pCategoryId == null || !pCategoryId.trim().equals(categoryId)) {
                    continue;
                }

                int qty = p.getQuantity();

                totalItems++;
                totalQuantity += qty;

                if (qty <= 0) {
                    outOfStock++;
                } else if (qty <= 10) {
                    lowStock++;
                }
            }

            JPanel card = createCategoryCard(
                    categoryId,
                    categoryName,
                    totalItems,
                    totalQuantity,
                    outOfStock,
                    lowStock
            );

            categoryCardPanel.add(card);
        }

        categoryCardPanel.revalidate();
        categoryCardPanel.repaint();
    }

    private JPanel createCategoryCard(
            String categoryId,
            String categoryName,
            int totalItems,
            int totalQuantity,
            int outOfStock,
            int lowStock
    ) {
        boolean selected = categoryId != null && categoryId.equals(selectedCategoryId);

        Color normalBg = selected ? new Color(237, 242, 255) : Color.WHITE;
        Color hoverBg = selected ? new Color(225, 233, 255) : new Color(248, 250, 255);
        Color borderColor = selected ? primaryBlue : borderGray;

        RoundedPanel card = new RoundedPanel(18, normalBg);
        card.setLayout(new BorderLayout(12, 0));
        card.setPreferredSize(new Dimension(285, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderColor, 18),
                new EmptyBorder(15, 16, 15, 16)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(getCategoryEmoji(categoryId), SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 34));
        icon.setPreferredSize(new Dimension(54, 70));

        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(62, 70));
        iconWrap.add(icon);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel lblId = new JLabel(categoryId);
        lblId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblId.setForeground(primaryBlue);

        JLabel lblName = new JLabel(categoryName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblName.setForeground(textDark);

        JLabel lblTotal = new JLabel(totalItems + " mặt hàng • " + totalQuantity + " tồn kho");
        lblTotal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotal.setForeground(new Color(90, 100, 130));

        JLabel lblWarning = new JLabel("Hết hàng: " + outOfStock + "  |  Sắp hết: " + lowStock);
        lblWarning.setFont(new Font("Segoe UI", Font.BOLD, 12));

        if (outOfStock > 0) {
            lblWarning.setForeground(new Color(220, 53, 69));
        } else if (lowStock > 0) {
            lblWarning.setForeground(new Color(230, 120, 0));
        } else {
            lblWarning.setForeground(new Color(25, 135, 84));
        }

        textPanel.add(lblId);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(lblName);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(lblTotal);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(lblWarning);

        card.add(iconWrap, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                filterProductsByCategory(categoryId);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.bgColor = hoverBg;
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.bgColor = normalBg;
                card.repaint();
            }
        });

        return card;
    }

    private void filterProductsByCategory(String categoryId) {
        selectedCategoryId = categoryId;
        updateCurrentCategoryLabel();

        List<Product> filtered = new ArrayList<>();

        for (Product p : cachedProducts) {
            String pCategoryId = p.getCategoryId();

            if (pCategoryId != null && pCategoryId.trim().equals(categoryId)) {
                filtered.add(p);
            }
        }

        fillTable(filtered);
        refreshCategoryCards();
    }

    private void updateCurrentCategoryLabel() {
        if (lblCurrentCategory == null) {
            return;
        }

        if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
            lblCurrentCategory.setText("Đang xem: Tất cả sản phẩm");
        } else {
            lblCurrentCategory.setText("Đang xem: " + selectedCategoryId + " - " + getCategoryNameById(selectedCategoryId));
        }
    }

    private String getCategoryNameById(String categoryId) {
        if (categoryId == null) {
            return "Không xác định";
        }

        String name = categoryNameMap.get(categoryId.trim());

        if (name == null || name.trim().isEmpty()) {
            return "Danh mục " + categoryId;
        }

        return name;
    }

    private String getCategoryEmoji(String categoryId) {
        if (categoryId == null) {
            return "📦";
        }

        return switch (categoryId.trim()) {
            case "CAT001" ->
                "🍚";
            case "CAT002" ->
                "🥤";
            case "CAT003" ->
                "🧴";
            case "CAT004" ->
                "🍪";
            case "CAT005" ->
                "🥩";
            default ->
                "📦";
        };
    }

    private ImageIcon loadProductImageIcon(String imageNameOrPath) {
        return loadProductImageIcon(imageNameOrPath, TABLE_IMAGE_WIDTH, TABLE_IMAGE_HEIGHT);
    }

    private ImageIcon loadProductImageIcon(String imageNameOrPath, int width, int height) {
        if (imageNameOrPath == null || imageNameOrPath.trim().isEmpty()) {
            return null;
        }

        String path = imageNameOrPath.trim().replace("\\", "/");

        try {
            // DB lưu: products/a.png
            // Java tìm: src/main/resources/view/image/products/a.png
            java.net.URL url = getClass().getClassLoader().getResource("view/image/" + path);

            if (url != null) {
                Image img = new ImageIcon(url)
                        .getImage()
                        .getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            File file = new File(path);

            if (!file.exists()) {
                file = new File("src/main/resources/view/image/" + path);
            }

            if (file.exists()) {
                Image img = new ImageIcon(file.getAbsolutePath())
                        .getImage()
                        .getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void applyStockRowRenderer() {
        DefaultTableCellRenderer stockRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                int modelRow = table.convertRowIndexToModel(row);
                Object qtyObj = table.getModel().getValueAt(modelRow, 3);

                int quantity = 0;

                try {
                    quantity = Integer.parseInt(String.valueOf(qtyObj).trim());
                } catch (Exception ignored) {
                }

                if (isSelected) {
                    c.setBackground(new Color(237, 242, 255));
                    c.setForeground(textDark);
                } else if (quantity <= 0) {
                    c.setBackground(new Color(255, 230, 230));
                    c.setForeground(new Color(200, 0, 0));
                } else if (quantity <= 10) {
                    c.setBackground(new Color(255, 246, 220));
                    c.setForeground(new Color(150, 95, 0));
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                    c.setForeground(Color.BLACK);
                }

                if (c instanceof JLabel lbl) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setVerticalAlignment(SwingConstants.CENTER);

                    if (column == 2) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                        lbl.setBorder(new EmptyBorder(0, 16, 0, 8));
                    } else {
                        lbl.setBorder(new EmptyBorder(0, 6, 0, 6));
                    }
                }

                return c;
            }
        };

        for (int i = 0; i < tblProducts.getColumnCount(); i++) {
            // Không đè renderer cột ảnh và cột loại hàng.
            if (i != 1 && i != 5) {
                tblProducts.getColumnModel().getColumn(i).setCellRenderer(stockRenderer);
            }
        }

        tblProducts.getColumnModel()
                .getColumn(5)
                .setCellRenderer(new view.components.CategoryTableRenderer(20));
    }

    private void applyProductRolePermission() {
        boolean canManageProductData = AuthorizationService.canManageProducts();
        boolean canSendAlert = AuthorizationService.canSendInventoryAlert();

        if (!canManageProductData) {
            setProductFormVisible(false);
            setProductMutationButtonsVisible(false);

            if (btnImport != null) {
                btnImport.setVisible(false);
                btnImport.setEnabled(false);
            }

            if (btnEmergencyAlert != null) {
                btnEmergencyAlert.setVisible(canSendAlert);
                btnEmergencyAlert.setEnabled(canSendAlert);
            }

            if (btnExportPDF != null) {
                btnExportPDF.setVisible(true);
                btnExportPDF.setEnabled(true);
            }

            revalidate();
            repaint();
            return;
        }

        setProductFormVisible(true);
        setProductMutationButtonsVisible(true);

        if (btnImport != null) {
            btnImport.setVisible(false);
            btnImport.setEnabled(false);
        }

        if (btnEmergencyAlert != null) {
            btnEmergencyAlert.setVisible(false);
            btnEmergencyAlert.setEnabled(false);
        }

        if (!AuthorizationService.isAdmin()) {
            if (lblImagePreview != null) {
                lblImagePreview.setVisible(false);
            }

            if (btnChooseImage != null) {
                btnChooseImage.setVisible(false);
            }

            if (lblImageSectionTitle != null) {
                lblImageSectionTitle.setVisible(false);
            }
        }

        revalidate();
        repaint();
    }

    private void setProductMutationButtonsVisible(boolean visible) {
        if (btnAdd != null) {
            btnAdd.setVisible(visible);
            btnAdd.setEnabled(visible);
        }

        if (btnUpdate != null) {
            btnUpdate.setVisible(visible);
            btnUpdate.setEnabled(visible);
        }

        if (btnDelete != null) {
            btnDelete.setVisible(visible);
            btnDelete.setEnabled(visible);
        }

        if (btnClear != null) {
            btnClear.setVisible(visible);
            btnClear.setEnabled(visible);
        }

        /*
     * Nút Đơn vị nằm trong form bên trái.
     * Manager/Staff Sale không thấy form nhưng vẫn mở cấu hình bằng double click sản phẩm.
     * Nhân viên kho/Admin thấy nút Đơn vị khi có quyền quản lý sản phẩm.
         */
        if (btnUnitConfig != null) {
            boolean canUseButton = visible && canViewProductUnitConfig();
            btnUnitConfig.setVisible(canUseButton);
            btnUnitConfig.setEnabled(canUseButton);
        }
    }

    private void setProductFormVisible(boolean visible) {
        if (formCard != null) {
            formCard.setVisible(visible);
        }

        if (btnChooseImage != null) {
            btnChooseImage.setVisible(visible);
        }

        if (lblImagePreview != null) {
            lblImagePreview.setVisible(visible);
        }

        if (lblImageSectionTitle != null) {
            lblImageSectionTitle.setVisible(visible);
        }
    }

    private boolean canMutateProductData() {
        return AuthorizationService.canManageProducts();
    }

    private boolean canViewProductUnitConfig() {
        return AuthorizationService.isAdmin()
                || AuthorizationService.isStoreManager()
                || AuthorizationService.isCashier()
                || AuthorizationService.isProductStaff()
                || AuthorizationService.canManageProducts()
                || AuthorizationService.canManageStock();
    }

    private boolean canEditProductUnitConfig() {
        return AuthorizationService.isAdmin()
                || AuthorizationService.isStoreManager()
                || AuthorizationService.isProductStaff()
                || AuthorizationService.canManageProducts()
                || AuthorizationService.canManageStock();
    }

    private boolean requireProductMutationPermission() {
        if (canMutateProductData()) {
            return true;
        }

        JOptionPane.showMessageDialog(
                this,
                "Bạn chỉ được xem sản phẩm và gửi cảnh báo tồn kho, không được thêm/sửa/xóa sản phẩm.",
                "Không có quyền",
                JOptionPane.WARNING_MESSAGE
        );
        return false;
    }

    private boolean requireStockImportPermission() {
        if (AuthorizationService.canManageStock()) {
            return true;
        }

        JOptionPane.showMessageDialog(
                this,
                "Bạn không có quyền nhập CSV/nhập kho. Chức năng này dành cho Staff Product hoặc Admin.",
                "Không có quyền",
                JOptionPane.WARNING_MESSAGE
        );
        return false;
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
        Color bgColor;

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
//        btnImport.addActionListener(e -> handleImportCSV());
        btnEmergencyAlert.addActionListener(e -> sendLowStockNotification());
    }

    private void btnSearchActionPerformed() {
        JTextField editor = (JTextField) cbSearch.getEditor().getEditorComponent();
        String keyword = editor.getText();

        if (keyword == null) {
            keyword = "";
        }

        keyword = keyword.trim().toLowerCase();

        try {
            if (cachedProducts.isEmpty()) {
                cachedProducts.clear();
                cachedProducts.addAll(loadProductsByCurrentScope());
            }

            List<Product> result = new ArrayList<>();

            for (Product p : cachedProducts) {
                if (p == null) {
                    continue;
                }

                boolean matchCategory = true;

                if (selectedCategoryId != null && !selectedCategoryId.trim().isEmpty()) {
                    matchCategory = p.getCategoryId() != null
                            && p.getCategoryId().trim().equalsIgnoreCase(selectedCategoryId.trim());
                }

                String productId = p.getProductId() == null ? "" : p.getProductId().toLowerCase();
                String productName = p.getProductName() == null ? "" : p.getProductName().toLowerCase();
                String categoryId = p.getCategoryId() == null ? "" : p.getCategoryId().toLowerCase();

                boolean matchKeyword = keyword.isEmpty()
                        || productId.contains(keyword)
                        || productName.contains(keyword)
                        || categoryId.contains(keyword);

                if (matchCategory && matchKeyword) {
                    result.add(p);
                }
            }

            fillTable(result);
            updateCurrentCategoryLabel();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi tìm kiếm sản phẩm: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void sendLowStockNotification() {
        if (!AuthorizationService.canSendInventoryAlert()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền gửi cảnh báo tồn kho.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int row = tblProducts.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn sản phẩm cần báo hết hàng!",
                    "Chưa chọn sản phẩm",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = tblProducts.convertRowIndexToModel(row);

        String productId = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String productName = String.valueOf(tableModel.getValueAt(modelRow, 2));
        int quantity = parseIntSafe(tableModel.getValueAt(modelRow, 3));

        if (quantity > 20) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Sản phẩm này còn " + quantity + " sản phẩm, chưa nằm trong ngưỡng sắp hết.\n"
                    + "Bạn vẫn muốn gửi thông báo cho kho không?",
                    "Xác nhận báo tồn kho",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String createdBy = "UNKNOWN";
        if (SessionManager.getCurrentUser() != null) {
            createdBy = SessionManager.getCurrentUser().getAccountId();
            if (createdBy == null || createdBy.isBlank()) {
                createdBy = SessionManager.getCurrentUser().getUsername();
            }
        }

        InventoryNotificationSql.NotifyResult result = InventoryNotificationSql.getInstance()
                .createOrIncreaseLowStockNotificationWithCooldown(
                        productId,
                        productName,
                        quantity,
                        createdBy
                );

        if (!result.success) {
            JOptionPane.showMessageDialog(
                    this,
                    result.message,
                    "Thông báo tồn kho",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            SyncVersionDao.bumpVersion("INVENTORY");

            String alertMessage = "INVENTORY_ALERT:"
                    + productId + ":"
                    + productName.replace(":", " ") + ":"
                    + quantity;

            /*
             * Gửi đúng payload có productId để NotificationBell / WarehouseDashboardView
             * có thể click vào thông báo và nhảy thẳng tới sản phẩm trong InventoryView.
             */
            RealtimeClient.send(alertMessage);

            EventBus.publish(new AppDataChangedEvent(
                    AppEventType.INVENTORY_ALERT,
                    alertMessage
            ));
        } catch (Exception ignored) {
        }

        String waitText = result.waitMinutes <= 0
                ? "Manager/Admin được phép nhắc lại ngay khi cần."
                : "Quy định thời gian báo lại: " + result.waitMinutes + " phút/lần.";

        JOptionPane.showMessageDialog(
                this,
                result.message + "\n"
                + "Sản phẩm: " + productName + "\n"
                + waitText,
                "Gửi thông báo thành công",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private int parseIntSafe(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void btnAddActionPerformed() {
        if (!requireProductMutationPermission()) {
            return;
        }

        if (!validateInput()) {
            return;
        }

        Product p = getProductFromForm();
        if (p == null) {
            return;
        }

        ProductsSql dao = ProductsSql.getInstance();

        /*
     * Logic chuẩn:
     * - Nếu sản phẩm đã tồn tại theo Tên + Loại SP => chỉ cộng dồn số lượng, KHÔNG sinh mã mới.
     * - Nếu là mặt hàng mới hoàn toàn => mới sinh mã SP tiếp theo.
         */
        Product existingProduct = dao.findByExactNameAndCategory(
                p.getProductName(),
                p.getCategoryId()
        );

        if (existingProduct != null) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Sản phẩm '" + p.getProductName() + "' đã tồn tại trong kho.\n"
                    + "Mã hiện tại: " + existingProduct.getProductId() + "\n"
                    + "Bạn có muốn cộng thêm " + p.getQuantity() + " vào số lượng hiện tại không?",
                    "Phát hiện sản phẩm trùng",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            String storeId = existingProduct.getStoreId();

            if (storeId == null || storeId.trim().isEmpty()) {
                storeId = business.service.SessionManager.getCurrentStoreId();
            }

            if (storeId == null || storeId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập lại.",
                        "Thiếu chi nhánh",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            storeId = storeId.trim();

            boolean success = dao.addQuantity(
                    existingProduct.getProductId(),
                    p.getQuantity(),
                    storeId
            );

            if (success) {
                SyncVersionDao.bumpVersion("INVENTORY");
                SyncVersionDao.bumpVersion("PRODUCTS");

                RealtimeClient.send("PRODUCTS_CHANGED");
                RealtimeClient.send("INVENTORY_CHANGED");

                JOptionPane.showMessageDialog(
                        this,
                        "✅ Đã cộng dồn số lượng thành công!\n"
                        + "Mã sản phẩm giữ nguyên: " + existingProduct.getProductId()
                );

                loadDataToTable();
                btnClearActionPerformed();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "❌ Lỗi khi cộng dồn số lượng!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }

            return;
        }

        // Chỉ sinh mã mới khi đây là sản phẩm mới hoàn toàn
        p.setProductId(dao.generateNextProductId());

        if (p.getSupplierId() == null || p.getSupplierId().trim().isEmpty()) {
            p.setSupplierId("SUP001");
        }

        if (p.getStoreId() == null || p.getStoreId().trim().isEmpty()) {
            String currentStoreId = business.service.SessionManager.getCurrentStoreId();

            if (currentStoreId == null || currentStoreId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập lại.",
                        "Thiếu chi nhánh",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            p.setStoreId(currentStoreId.trim());
        }

        if (p.getUnit() == null || p.getUnit().trim().isEmpty()) {
            p.setUnit("Cái");
        }

        boolean inserted = dao.insert(p);

        if (inserted) {
            SyncVersionDao.bumpVersion("PRODUCTS");
            SyncVersionDao.bumpVersion("INVENTORY");

            RealtimeClient.send("PRODUCTS_CHANGED");
            RealtimeClient.send("INVENTORY_CHANGED");

            JOptionPane.showMessageDialog(
                    this,
                    "✅ Thêm sản phẩm mới thành công!\n"
                    + "Mã tự cấp: " + p.getProductId()
            );

            loadDataToTable();

            if (!productNameList.contains(p.getProductName())) {
                productNameList.add(p.getProductName());
            }

            btnClearActionPerformed();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "❌ Thêm thất bại! Vui lòng kiểm tra dữ liệu đầu vào.",
                    "Lỗi hệ thống",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void btnUpdateActionPerformed() {
        if (!requireProductMutationPermission()) {
            return;
        }

        int row = tblProducts.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một sản phẩm trong bảng để cập nhật!", "Chưa chọn dòng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tblProducts.convertRowIndexToModel(row);
        String idOld = tblProducts.getModel().getValueAt(modelRow, 0).toString().trim();

        Product p = getProductFromForm();
        if (p == null) {
            return;
        }

        // BẮT BUỘC giữ lại mã sản phẩm đang chọn.
        // Trước đó bị thiếu dòng này nên updateInStore/update có thể không biết update sản phẩm nào.
        p.setProductId(idOld);

        boolean updated;

        if (!SessionManager.isAdmin()) {
            String storeId = getCurrentStoreIdOrWarn();

            if (storeId == null) {
                return;
            }

            p.setStoreId(storeId);
            updated = ProductsSql.getInstance().updateInStore(p, storeId);
        } else {
            updated = ProductsSql.getInstance().update(p);
        }

        if (updated) {
            SyncVersionDao.bumpVersion("PRODUCTS");
            SyncVersionDao.bumpVersion("INVENTORY");

            RealtimeClient.send("PRODUCTS_CHANGED");
            RealtimeClient.send("INVENTORY_CHANGED");

            JOptionPane.showMessageDialog(this, "✅ Cập nhật sản phẩm thành công!");
            loadDataToTable();
            btnClearActionPerformed();
        } else {
            JOptionPane.showMessageDialog(this, "❌ Cập nhật thất bại! Vui lòng kiểm tra Database.", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void notifyProductInventoryChangedAsync() {
        Thread t = new Thread(() -> {
            try {
                SyncVersionDao.bumpVersion("PRODUCTS");
                SyncVersionDao.bumpVersion("INVENTORY");

                RealtimeClient.send("PRODUCTS_CHANGED");
                RealtimeClient.send("INVENTORY_CHANGED");
            } catch (Exception ex) {
                System.err.println("[ProductView] Realtime notify failed: " + ex.getMessage());
            }
        }, "product-inventory-notifier");

        t.setDaemon(true);
        t.start();
    }

    private void setProductActionButtonsEnabled(boolean enabled) {
        if (btnAdd != null) {
            btnAdd.setEnabled(enabled);
        }
        if (btnUpdate != null) {
            btnUpdate.setEnabled(enabled);
        }
        if (btnDelete != null) {
            btnDelete.setEnabled(enabled);
        }
        if (btnClear != null) {
            btnClear.setEnabled(enabled);
        }
        if (btnUnitConfig != null) {
            btnUnitConfig.setEnabled(enabled);
        }
        if (btnSearch != null) {
            btnSearch.setEnabled(enabled);
        }
        if (btnExportPDF != null) {
            btnExportPDF.setEnabled(enabled);
        }
    }

    private void btnDeleteActionPerformed() {
        if (!requireProductMutationPermission()) {
            return;
        }

        int row = tblProducts.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "⚠️ Vui lòng chọn một sản phẩm trong bảng để xóa!",
                    "Chưa chọn dòng",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = tblProducts.convertRowIndexToModel(row);
        String id = String.valueOf(tblProducts.getModel().getValueAt(modelRow, 0)).trim();
        String name = String.valueOf(tblProducts.getModel().getValueAt(modelRow, 2)).trim();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn ngừng kinh doanh sản phẩm: " + name + " (" + id + ")?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        final String storeId;

        if (!SessionManager.isAdmin()) {
            storeId = getCurrentStoreIdOrWarn();

            if (storeId == null) {
                return;
            }
        } else {
            storeId = null;
        }

        setProductActionButtonsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Product>, Void>() {
            private boolean usedInOrders = false;
            private boolean deleted = false;
            private String errorMessage = null;

            @Override
            protected List<Product> doInBackground() {
                try {
                    usedInOrders = ProductsSql.getInstance().isUsedInOrders(id);

                    if (!SessionManager.isAdmin()) {
                        deleted = ProductsSql.getInstance().deleteFromStore(id, storeId);
                    } else {
                        deleted = ProductsSql.getInstance().delete(id);
                    }

                    if (!deleted) {
                        errorMessage = "Không thể xóa sản phẩm. Vui lòng kiểm tra Database hoặc quyền chi nhánh.";
                        return null;
                    }

                    /*
                 * Query lại danh sách ở background.
                 * Không gọi loadDataToTable() trong done vì loadDataToTable rebuild UI và load ảnh,
                 * dễ gây đơ.
                     */
                    return loadProductsByCurrentScope();

                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                setProductActionButtonsEnabled(true);
                setCursor(Cursor.getDefaultCursor());

                try {
                    List<Product> newList = get();

                    if (!deleted) {
                        JOptionPane.showMessageDialog(
                                ProductView.this,
                                "❌ " + (errorMessage == null ? "Không thể xóa sản phẩm." : errorMessage),
                                "Lỗi hệ thống",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    if (newList != null) {
                        cachedProducts.clear();
                        cachedProducts.addAll(newList);

                        for (Product p : newList) {
                            String categoryId = p.getCategoryId();

                            if (categoryId != null
                                    && !categoryId.trim().isEmpty()
                                    && !categoryNameMap.containsKey(categoryId.trim())) {
                                categoryNameMap.put(categoryId.trim(), "Danh mục " + categoryId.trim());
                            }
                        }

                        if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
                            fillTable(newList);
                        } else {
                            List<Product> filtered = new ArrayList<>();

                            for (Product p : newList) {
                                String pCategoryId = p.getCategoryId();

                                if (pCategoryId != null && pCategoryId.trim().equals(selectedCategoryId)) {
                                    filtered.add(p);
                                }
                            }

                            fillTable(filtered);
                        }

                        refreshCategoryCards();
                        updateCurrentCategoryLabel();

                        /*
                     * Không rebuild overview ngay sau khi xóa ở tab Chi tiết.
                     * Rebuild overview chỉ khi user đang ở tab Tổng quan để tránh lag vô ích.
                         */
                        if ("OVERVIEW".equals(currentProductTab) && tabContentPanel != null) {
                            overviewPanel = buildOverviewPanel();
                            tabContentPanel.removeAll();
                            tabContentPanel.add(overviewPanel, "OVERVIEW");
                            tabContentPanel.add(detailPanel, "DETAIL");
                            switchProductTab("OVERVIEW");
                        }
                    }

                    btnClearActionPerformed();

                    /*
                 * Giữ realtime nhưng chạy nền, không khóa UI.
                     */
                    notifyProductInventoryChangedAsync();

                    if (usedInOrders) {
                        JOptionPane.showMessageDialog(
                                ProductView.this,
                                "Sản phẩm [" + name + "] đã từng được bán/nhập kho.\n"
                                + "Hệ thống đã chuyển sang trạng thái ẨN an toàn.",
                                "Đã ẩn sản phẩm",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                                ProductView.this,
                                "✅ Xóa sản phẩm [" + name + "] thành công!"
                        );
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            ProductView.this,
                            "❌ Lỗi sau khi xóa sản phẩm: " + ex.getMessage(),
                            "Lỗi hệ thống",
                            JOptionPane.ERROR_MESSAGE
                    );
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void btnClearActionPerformed() {
        txtName.setText("");
        txtPrice.setText("");
        txtQuantity.setText("");

        if (cbCategory != null && cbCategory.getEditor() != null) {
            ((JTextField) cbCategory.getEditor().getEditorComponent()).setText("");
            cbCategory.setSelectedItem("");
        }

        if (cbSearch != null && cbSearch.getEditor() != null) {
            ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
            cbSearch.setSelectedItem("");
        }

        selectedImagePath = null;

        if (lblImagePreview != null) {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Chưa chọn ảnh");
        }

        if (tblProducts != null) {
            tblProducts.clearSelection();
        }

        if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
            fillTable(cachedProducts);
        } else {
            filterProductsByCategory(selectedCategoryId);
        }
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

        int modelRow = tblProducts.convertRowIndexToModel(row);

        String productId = String.valueOf(tableModel.getValueAt(modelRow, 0));
        Object imageObj = tableModel.getValueAt(modelRow, 1);
        String productName = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String quantityText = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String price = String.valueOf(tableModel.getValueAt(modelRow, 4));
        String categoryId = String.valueOf(tableModel.getValueAt(modelRow, 5));

        int quantity = 0;

        try {
            quantity = Integer.parseInt(quantityText.trim());
        } catch (Exception ignored) {
        }

        Product selected = ProductsSql.getInstance().findById(productId);

        ImageIcon detailImage = null;

        if (selected != null && selected.getImagePath() != null && !selected.getImagePath().trim().isEmpty()) {
            selectedImagePath = selected.getImagePath().trim();
            detailImage = loadProductImageIcon(selectedImagePath, DETAIL_IMAGE_WIDTH, DETAIL_IMAGE_HEIGHT);
        } else {
            selectedImagePath = null;
        }

        if (detailImage == null && imageObj instanceof ImageIcon img) {
            detailImage = img;
        }

        /*
     * Chuẩn mới:
     * - Double click sản phẩm => mở cấu hình đơn vị bán.
     * - Manager / Staff Sale được xem.
     * - Admin / Nhân viên kho được sửa.
         */
        if (evt.getClickCount() == 2) {
            if (canViewProductUnitConfig()) {
                showUnitConfigDialog();
            } else {
                showProductDetailDialog(productId, productName, price, quantity, categoryId, detailImage);
            }
            return;
        }

        /*
     * Manager / Staff Sale chỉ xem danh sách, không fill form sửa sản phẩm.
         */
        if (AuthorizationService.isStoreManager() || AuthorizationService.isCashier()) {
            return;
        }

        txtName.setText(productName);
        txtPrice.setText(price.replace("VND", "").replace("VNĐ", "").replace("đ", "").trim());
        txtQuantity.setText(quantityText);

        JTextField editor = (JTextField) cbCategory.getEditor().getEditorComponent();
        editor.setText(categoryId);

        if (lblImagePreview != null) {
            if (detailImage != null) {
                Image img = detailImage.getImage()
                        .getScaledInstance(180, 120, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new ImageIcon(img));
                lblImagePreview.setText("");
            } else {
                lblImagePreview.setIcon(null);
                lblImagePreview.setText("Chưa có ảnh");
            }
        }
    }

    private void showProductDetailDialog(
            String productId,
            String productName,
            String price,
            int quantity,
            String categoryId,
            ImageIcon productImage
    ) {
        String status;
        Color statusColor;

        if (quantity <= 0) {
            status = "HẾT HÀNG";
            statusColor = new Color(220, 53, 69);
        } else if (quantity <= 10) {
            status = "SẮP HẾT HÀNG";
            statusColor = new Color(255, 153, 0);
        } else {
            status = "CÒN HÀNG";
            statusColor = new Color(25, 135, 84);
        }

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Chi tiết sản phẩm",
                true
        );

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        dialog.setSize(620, 430);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(bgLight);

        RoundedPanel card = new RoundedPanel(22, Color.WHITE);
        card.setLayout(new BorderLayout(24, 0));
        card.setBorder(new EmptyBorder(28, 30, 24, 30));

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(220, 260));

        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(210, 170));
        imgLabel.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 18),
                new EmptyBorder(10, 10, 10, 10)
        ));

        if (productImage != null) {
            Image scaled = productImage.getImage()
                    .getScaledInstance(DETAIL_IMAGE_WIDTH, DETAIL_IMAGE_HEIGHT, Image.SCALE_SMOOTH);
            imgLabel.setIcon(new ImageIcon(scaled));
            imgLabel.setText("");
        } else {
            imgLabel.setText("Chưa có ảnh");
            imgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            imgLabel.setForeground(textGray);
        }

        JLabel imageCaption = new JLabel(productId, SwingConstants.CENTER);
        imageCaption.setFont(new Font("Segoe UI", Font.BOLD, 13));
        imageCaption.setForeground(primaryBlue);

        imagePanel.add(imgLabel, BorderLayout.CENTER);
        imagePanel.add(imageCaption, BorderLayout.SOUTH);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;

        JLabel title = new JLabel("Chi tiết sản phẩm");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(textDark);
        gbc.gridy = 0;
        infoPanel.add(title, gbc);

        gbc.gridy++;
        infoPanel.add(detailLine("Mã sản phẩm", productId), gbc);

        gbc.gridy++;
        infoPanel.add(detailLine("Tên sản phẩm", productName), gbc);

        gbc.gridy++;
        infoPanel.add(detailLine("Giá bán", price), gbc);

        gbc.gridy++;
        infoPanel.add(detailLine("Số lượng tồn", String.valueOf(quantity)), gbc);

        gbc.gridy++;
        infoPanel.add(detailLine("Loại sản phẩm", categoryId), gbc);

        gbc.gridy++;
        JLabel statusLabel = new JLabel("Trạng thái: " + status);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(statusColor);
        infoPanel.add(statusLabel, gbc);

        card.add(imagePanel, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);

        JButton btnClose = createCustomButton("Đóng", new Color(165, 177, 194), Color.WHITE, null);
        btnClose.setPreferredSize(new Dimension(140, 45));
        btnClose.addActionListener(e -> dialog.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        bottom.setOpaque(false);
        bottom.add(btnClose);

        dialog.add(card, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void fillProductUnitsToModel(List<ProductUnit> units, DefaultTableModel model) {
        model.setRowCount(0);

        java.text.DecimalFormat money = new java.text.DecimalFormat("#,##0 đ");

        if (units == null) {
            return;
        }

        for (ProductUnit unit : units) {
            String unitId = unit.getUnitId();
            String unitName = unit.getUnitName();

            if (unitName == null || unitName.trim().isEmpty()) {
                unitName = unitId;
            }

            BigDecimal price = unit.getSellingPrice();
            String priceText = price == null ? "—" : money.format(price);

            model.addRow(new Object[]{
                unitId,
                unitName,
                unit.getConversionRateToBase(),
                priceText,
                unit.getIsBaseUnit() == 1 ? "Có" : ""
            });
        }
    }

    private JPanel detailLine(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label + ": ");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(textDark);

        JLabel val = new JLabel(value == null ? "" : value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        val.setForeground(Color.BLACK);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private List<Map<String, Object>> getAllProductsFromTable() {
        List<Map<String, Object>> list = new ArrayList<>();
        int rowCount = tblProducts.getRowCount();

        for (int viewRow = 0; viewRow < rowCount; viewRow++) {
            int modelRow = tblProducts.convertRowIndexToModel(viewRow);

            Map<String, Object> row = new HashMap<>();
            row.put("productId", tableModel.getValueAt(modelRow, 0));
            row.put("productName", tableModel.getValueAt(modelRow, 2));
            row.put("quantity", tableModel.getValueAt(modelRow, 3));
            row.put("price", tableModel.getValueAt(modelRow, 4));
            row.put("categoryId", tableModel.getValueAt(modelRow, 5));
            list.add(row);
        }

        return list;
    }

    public void loadDataToTable() {
        try {
            List<Product> list = loadProductsByCurrentScope();

            cachedProducts.clear();
            cachedProducts.addAll(list);

            for (Product p : list) {
                String categoryId = p.getCategoryId();

                if (categoryId != null
                        && !categoryId.trim().isEmpty()
                        && !categoryNameMap.containsKey(categoryId.trim())) {
                    categoryNameMap.put(categoryId.trim(), "Danh mục " + categoryId.trim());
                }
            }

            if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
                fillTable(list);
            } else {
                List<Product> filtered = new ArrayList<>();

                for (Product p : list) {
                    String pCategoryId = p.getCategoryId();

                    if (pCategoryId != null && pCategoryId.trim().equals(selectedCategoryId)) {
                        filtered.add(p);
                    }
                }

                fillTable(filtered);
            }

            refreshCategoryCards();
            updateCurrentCategoryLabel();

            if (tabContentPanel != null) {
                String oldTab = currentProductTab;
                overviewPanel = buildOverviewPanel();
                tabContentPanel.removeAll();
                tabContentPanel.add(overviewPanel, "OVERVIEW");
                tabContentPanel.add(detailPanel, "DETAIL");
                switchProductTab(oldTab);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Product> loadProductsByCurrentScope() {
        String storeId = SessionManager.getScopedStoreIdOrNull();

        if (storeId != null && !storeId.trim().isEmpty()) {
            return ProductsSql.getInstance().selectAllByStore(storeId.trim());
        }

        return ProductsSql.getInstance().selectAll();
    }

    private String getCurrentStoreIdOrWarn() {
        if (SessionManager.isAdmin()) {
            return null;
        }

        String storeId = SessionManager.getCurrentStoreId();

        if (storeId == null || storeId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Tài khoản chưa được phân chi nhánh. Vui lòng đăng nhập lại hoặc liên hệ Admin.",
                    "Thiếu chi nhánh",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        return storeId.trim();
    }

    public void refreshTable() {
        loadAutoCompleteData();
        loadDataToTable();
    }

    private void fillTable(List<Product> list) {
        tableModel.setRowCount(0);

        if (list == null) {
            return;
        }

        for (Product p : list) {
            ImageIcon thumb = loadProductImageIcon(p.getImagePath());

            Object[] row = {
                p.getProductId(),
                thumb,
                p.getProductName(),
                p.getQuantity(),
                p.getBasePrice(),
                p.getCategoryId()
            };

            tableModel.addRow(row);
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
        String name = txtName.getText().trim();
        String priceText = txtPrice.getText().trim();
        String qtyText = txtQuantity.getText().trim();

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
        if (!canViewProductUnitConfig()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền xem cấu hình đơn vị bán.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String productId = getSelectedProductId();

        if (productId == null || productId.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn một sản phẩm trong bảng để cấu hình đơn vị!",
                    "Chưa chọn sản phẩm",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        boolean editable = canEditProductUnitConfig();

        int selectedRow = tblProducts.getSelectedRow();
        int selectedModelRow = tblProducts.convertRowIndexToModel(selectedRow);
        String productName = String.valueOf(tableModel.getValueAt(selectedModelRow, 2));

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Cấu hình Đơn vị bán",
                Dialog.ModalityType.MODELESS
        );

        final boolean[] dialogClosing = {false};

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dialogClosing[0] = true;
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                dialogClosing[0] = true;
            }
        });

        dialog.setSize(840, editable ? 650 : 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 12));
        dialog.getContentPane().setBackground(bgLight);

        JLabel lblTitle = new JLabel("Cấu hình quy đổi: " + productName + " (" + productId + ")");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(textDark);
        lblTitle.setBorder(new EmptyBorder(18, 22, 8, 22));
        dialog.add(lblTitle, BorderLayout.NORTH);

        RoundedPanel tablePanel = new RoundedPanel(16, Color.WHITE);
        tablePanel.setLayout(new BorderLayout(0, 8));
        tablePanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        DefaultTableModel unitModel = new DefaultTableModel(
                new Object[]{"Mã ĐV", "Tên đơn vị", "Tỷ lệ quy đổi", "Giá bán", "ĐV gốc"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable unitTable = new JTable(unitModel);
        unitTable.setRowHeight(34);
        unitTable.setFont(new Font("Segoe UI", Font.BOLD, 13));
        unitTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        unitTable.getTableHeader().setBackground(new Color(245, 247, 252));
        unitTable.getTableHeader().setForeground(textDark);
        unitTable.getTableHeader().setReorderingAllowed(false);
        unitTable.setShowVerticalLines(false);
        unitTable.setShowHorizontalLines(true);
        unitTable.setGridColor(new Color(235, 238, 245));
        unitTable.setSelectionBackground(new Color(237, 242, 255));
        unitTable.setSelectionForeground(textDark);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < unitTable.getColumnCount(); i++) {
            unitTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        unitTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        unitTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        unitTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        unitTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        unitTable.getColumnModel().getColumn(4).setPreferredWidth(90);

        loadProductUnits(productId, unitModel);

        JScrollPane scrollPane = new JScrollPane(unitTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderGray));
        scrollPane.getViewport().setBackground(Color.WHITE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JLabel lblStatus = new JLabel(
                editable
                        ? "Gợi ý: Chọn một dòng để chỉnh sửa. Đơn vị gốc là đơn vị nhỏ nhất để quản lý tồn kho."
                        : "Bạn chỉ được xem cấu hình đơn vị bán. Không được thêm/sửa/xóa."
        );
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblStatus.setForeground(textGray);
        lblStatus.setBorder(new EmptyBorder(4, 4, 0, 4));
        tablePanel.add(lblStatus, BorderLayout.SOUTH);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(0, 22, 0, 22));
        centerWrapper.add(tablePanel, BorderLayout.CENTER);
        dialog.add(centerWrapper, BorderLayout.CENTER);

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.setOpaque(false);
        southWrapper.setBorder(new EmptyBorder(0, 22, 20, 22));

        if (editable) {
            RoundedPanel formPanel = new RoundedPanel(16, Color.WHITE);
            formPanel.setLayout(new GridBagLayout());
            formPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            JTextField txtUnitName = createTextField("VD: Túi, Chai, Hộp, Lốc, Thùng...");
            JTextField txtRate = createTextField("VD: 1, 4, 6, 24...");
            JTextField txtSellingPrice = createTextField("VD: 35000...");

            JCheckBox chkBase = new JCheckBox("Đây là đơn vị gốc (Tỷ lệ = 1)");
            chkBase.setOpaque(false);
            chkBase.setFont(new Font("Segoe UI", Font.BOLD, 13));
            chkBase.setForeground(textDark);

            final String[] editingUnitId = {null};

            chkBase.addActionListener(e -> {
                if (chkBase.isSelected()) {
                    txtRate.setText("1");
                    txtRate.setEnabled(false);
                } else {
                    txtRate.setEnabled(true);
                }
            });

            unitTable.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                int viewRow = unitTable.getSelectedRow();
                if (viewRow < 0) {
                    return;
                }

                int modelRow = unitTable.convertRowIndexToModel(viewRow);

                editingUnitId[0] = String.valueOf(unitModel.getValueAt(modelRow, 0));
                txtUnitName.setText(String.valueOf(unitModel.getValueAt(modelRow, 1)));
                txtRate.setText(String.valueOf(unitModel.getValueAt(modelRow, 2)));

                String priceText = String.valueOf(unitModel.getValueAt(modelRow, 3))
                        .replace("đ", "")
                        .replace(".", "")
                        .replace(",", "")
                        .trim();

                txtSellingPrice.setText(priceText);

                String baseText = String.valueOf(unitModel.getValueAt(modelRow, 4)).trim();
                chkBase.setSelected("Có".equalsIgnoreCase(baseText));
                txtRate.setEnabled(!chkBase.isSelected());

                lblStatus.setForeground(textGray);
                lblStatus.setText("Đang chọn đơn vị: " + editingUnitId[0] + ". Có thể bấm Cập nhật hoặc Xóa.");
            });

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.34;
            formPanel.add(createLabel("Tên đơn vị:"), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 0.33;
            formPanel.add(createLabel("Tỷ lệ quy đổi:"), gbc);

            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 0.33;
            formPanel.add(createLabel("Giá bán:"), gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(txtUnitName, gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            formPanel.add(txtRate, gbc);

            gbc.gridx = 2;
            gbc.gridy = 1;
            formPanel.add(txtSellingPrice, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 3;
            formPanel.add(chkBase, gbc);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnPanel.setOpaque(false);

            JButton btnClose = createCustomButton("Đóng", new Color(165, 177, 194), Color.WHITE, null);
            btnClose.setPreferredSize(new Dimension(95, 40));
            btnClose.addActionListener(e -> {
                dialogClosing[0] = true;
                dialog.dispose();
            });

            JButton btnClearEdit = createCustomButton("Làm mới", new Color(108, 117, 125), Color.WHITE, null);
            btnClearEdit.setPreferredSize(new Dimension(105, 40));

            JButton btnDeleteUnit = createCustomButton("Xóa", new Color(220, 53, 69), Color.WHITE, null);
            btnDeleteUnit.setPreferredSize(new Dimension(90, 40));

            JButton btnUpdateUnit = createCustomButton("Cập nhật", new Color(255, 153, 0), Color.BLACK, IconHelper.edit(18));
            btnUpdateUnit.setPreferredSize(new Dimension(125, 40));

            JButton btnSave = createCustomButton("Thêm", primaryBlue, Color.WHITE, IconHelper.add(18));
            btnSave.setPreferredSize(new Dimension(105, 40));

            Runnable clearEditForm = () -> {
                editingUnitId[0] = null;
                unitTable.clearSelection();
                txtUnitName.setText("");
                txtRate.setText("");
                txtSellingPrice.setText("");
                chkBase.setSelected(false);
                txtRate.setEnabled(true);
                lblStatus.setForeground(textGray);
                lblStatus.setText("Đã làm mới form. Nhập dữ liệu để thêm đơn vị mới.");
            };

            Runnable disableMutationButtons = () -> {
                btnSave.setEnabled(false);
                btnUpdateUnit.setEnabled(false);
                btnDeleteUnit.setEnabled(false);
                btnClearEdit.setEnabled(false);
            };

            Runnable enableMutationButtons = () -> {
                btnSave.setEnabled(true);
                btnUpdateUnit.setEnabled(true);
                btnDeleteUnit.setEnabled(true);
                btnClearEdit.setEnabled(true);
            };

            btnClearEdit.addActionListener(e -> clearEditForm.run());

            btnSave.addActionListener(e -> {
                String unitName = txtUnitName.getText().trim();
                String rateText = txtRate.getText().trim();
                String priceText = txtSellingPrice.getText().trim();

                if (unitName.isEmpty() || rateText.isEmpty() || priceText.isEmpty()) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Vui lòng nhập đủ Tên đơn vị, Tỷ lệ quy đổi và Giá bán.");
                    return;
                }

                try {
                    BigDecimal rate = new BigDecimal(rateText);
                    BigDecimal sellingPrice = new BigDecimal(priceText);

                    if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                        lblStatus.setForeground(new Color(220, 53, 69));
                        lblStatus.setText("Tỷ lệ quy đổi phải lớn hơn 0.");
                        return;
                    }

                    if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                        lblStatus.setForeground(new Color(220, 53, 69));
                        lblStatus.setText("Giá bán không được âm.");
                        return;
                    }

                    disableMutationButtons.run();
                    lblStatus.setForeground(textGray);
                    lblStatus.setText("Đang thêm đơn vị...");

                    new SwingWorker<List<ProductUnit>, Void>() {
                        private boolean ok = false;
                        private String errorMessage = null;

                        @Override
                        protected List<ProductUnit> doInBackground() {
                            try {
                                ok = new UnitOfMeasureService().configureProductUnit(
                                        productId,
                                        unitName,
                                        rate,
                                        sellingPrice,
                                        chkBase.isSelected()
                                );

                                if (!ok) {
                                    errorMessage = "Thêm thất bại. Vui lòng kiểm tra dữ liệu hoặc database.";
                                    return null;
                                }

                                return ProductUnitsSql.getInstance().selectByProductId(productId);

                            } catch (Exception ex) {
                                errorMessage = ex.getMessage();
                                return null;
                            }
                        }

                        @Override
                        protected void done() {
                            if (dialogClosing[0] || !dialog.isDisplayable()) {
                                return;
                            }

                            enableMutationButtons.run();

                            try {
                                List<ProductUnit> units = get();

                                if (ok) {
                                    fillProductUnitsToModel(units, unitModel);
                                    clearEditForm.run();

                                    lblStatus.setForeground(new Color(25, 135, 84));
                                    lblStatus.setText("Đã thêm đơn vị bán thành công.");
                                } else {
                                    lblStatus.setForeground(new Color(220, 53, 69));
                                    lblStatus.setText(errorMessage == null ? "Thêm thất bại." : errorMessage);
                                }

                            } catch (Exception ex) {
                                lblStatus.setForeground(new Color(220, 53, 69));
                                lblStatus.setText("Lỗi hệ thống: " + ex.getMessage());
                            }
                        }
                    }.execute();

                } catch (NumberFormatException ex) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Tỷ lệ quy đổi và Giá bán phải là số hợp lệ.");
                }
            });

            btnUpdateUnit.addActionListener(e -> {
                if (editingUnitId[0] == null || editingUnitId[0].trim().isEmpty()) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Vui lòng chọn một đơn vị trong bảng để cập nhật.");
                    return;
                }

                String oldUnitId = editingUnitId[0];
                String unitName = txtUnitName.getText().trim();
                String rateText = txtRate.getText().trim();
                String priceText = txtSellingPrice.getText().trim();

                if (unitName.isEmpty() || rateText.isEmpty() || priceText.isEmpty()) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Vui lòng nhập đủ Tên đơn vị, Tỷ lệ quy đổi và Giá bán.");
                    return;
                }

                try {
                    BigDecimal rate = new BigDecimal(rateText);
                    BigDecimal sellingPrice = new BigDecimal(priceText);

                    if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                        lblStatus.setForeground(new Color(220, 53, 69));
                        lblStatus.setText("Tỷ lệ quy đổi phải lớn hơn 0.");
                        return;
                    }

                    if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                        lblStatus.setForeground(new Color(220, 53, 69));
                        lblStatus.setText("Giá bán không được âm.");
                        return;
                    }

                    disableMutationButtons.run();
                    lblStatus.setForeground(textGray);
                    lblStatus.setText("Đang cập nhật đơn vị...");

                    new SwingWorker<List<ProductUnit>, Void>() {
                        private boolean ok = false;
                        private String errorMessage = null;

                        @Override
                        protected List<ProductUnit> doInBackground() {
                            try {
                                ok = new UnitOfMeasureService().updateProductUnit(
                                        productId,
                                        oldUnitId,
                                        unitName,
                                        rate,
                                        sellingPrice,
                                        chkBase.isSelected()
                                );

                                if (!ok) {
                                    errorMessage = "Cập nhật thất bại. Vui lòng kiểm tra dữ liệu hoặc database.";
                                    return null;
                                }

                                return ProductUnitsSql.getInstance().selectByProductId(productId);

                            } catch (Exception ex) {
                                errorMessage = ex.getMessage();
                                return null;
                            }
                        }

                        @Override
                        protected void done() {
                            if (dialogClosing[0] || !dialog.isDisplayable()) {
                                return;
                            }

                            enableMutationButtons.run();

                            try {
                                List<ProductUnit> units = get();

                                if (ok) {
                                    fillProductUnitsToModel(units, unitModel);
                                    clearEditForm.run();

                                    lblStatus.setForeground(new Color(25, 135, 84));
                                    lblStatus.setText("Đã cập nhật đơn vị bán thành công.");
                                } else {
                                    lblStatus.setForeground(new Color(220, 53, 69));
                                    lblStatus.setText(errorMessage == null ? "Cập nhật thất bại." : errorMessage);
                                }

                            } catch (Exception ex) {
                                lblStatus.setForeground(new Color(220, 53, 69));
                                lblStatus.setText("Lỗi hệ thống: " + ex.getMessage());
                            }
                        }
                    }.execute();

                } catch (NumberFormatException ex) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Tỷ lệ quy đổi và Giá bán phải là số hợp lệ.");
                }
            });

            btnDeleteUnit.addActionListener(e -> {
                int selectedUnitRow = unitTable.getSelectedRow();

                if (selectedUnitRow < 0) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Vui lòng chọn một đơn vị trong bảng để xóa.");
                    return;
                }

                int modelRow = unitTable.convertRowIndexToModel(selectedUnitRow);

                String unitId = String.valueOf(unitModel.getValueAt(modelRow, 0));
                String unitName = String.valueOf(unitModel.getValueAt(modelRow, 1));
                String isBaseText = String.valueOf(unitModel.getValueAt(modelRow, 4));

                if ("Có".equalsIgnoreCase(isBaseText.trim())) {
                    lblStatus.setForeground(new Color(220, 53, 69));
                    lblStatus.setText("Không thể xóa đơn vị gốc. Hãy chọn đơn vị khác làm gốc trước.");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        dialog,
                        "Bạn có chắc muốn xóa đơn vị bán này không?\n\n"
                        + "Sản phẩm: " + productName + "\n"
                        + "Đơn vị: " + unitName + " (" + unitId + ")",
                        "Xác nhận xóa đơn vị",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                disableMutationButtons.run();

                lblStatus.setForeground(textGray);
                lblStatus.setText("Đang xóa đơn vị...");

                new SwingWorker<List<ProductUnit>, Void>() {
                    private boolean ok = false;
                    private String errorMessage = null;

                    @Override
                    protected List<ProductUnit> doInBackground() {
                        try {
                            ok = new UnitOfMeasureService().deleteProductUnit(productId, unitId);

                            if (!ok) {
                                errorMessage = "Xóa thất bại. Không được xóa đơn vị gốc hoặc đơn vị cuối cùng.";
                                return null;
                            }

                            return ProductUnitsSql.getInstance().selectByProductId(productId);

                        } catch (Exception ex) {
                            errorMessage = ex.getMessage();
                            return null;
                        }
                    }

                    @Override
                    protected void done() {
                        if (dialogClosing[0] || !dialog.isDisplayable()) {
                            return;
                        }

                        enableMutationButtons.run();

                        try {
                            List<ProductUnit> units = get();

                            if (ok) {
                                fillProductUnitsToModel(units, unitModel);
                                clearEditForm.run();

                                lblStatus.setForeground(new Color(25, 135, 84));
                                lblStatus.setText("Đã xóa đơn vị bán: " + unitName + ".");
                            } else {
                                lblStatus.setForeground(new Color(220, 53, 69));
                                lblStatus.setText(errorMessage == null ? "Xóa thất bại." : errorMessage);
                            }

                        } catch (Exception ex) {
                            lblStatus.setForeground(new Color(220, 53, 69));
                            lblStatus.setText("Lỗi hệ thống: " + ex.getMessage());
                        }
                    }
                }.execute();
            });

            btnPanel.add(btnClose);
            btnPanel.add(btnClearEdit);
            btnPanel.add(btnDeleteUnit);
            btnPanel.add(btnUpdateUnit);
            btnPanel.add(btnSave);

            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.gridwidth = 3;
            gbc.insets = new Insets(14, 5, 5, 5);
            formPanel.add(btnPanel, gbc);

            southWrapper.add(formPanel, BorderLayout.CENTER);

        } else {
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnPanel.setOpaque(false);

            JButton btnClose = createCustomButton("Đóng", new Color(165, 177, 194), Color.WHITE, null);
            btnClose.setPreferredSize(new Dimension(110, 40));
            btnClose.addActionListener(e -> {
                dialogClosing[0] = true;
                dialog.dispose();
            });

            btnPanel.add(btnClose);
            southWrapper.add(btnPanel, BorderLayout.CENTER);
        }

        dialog.add(southWrapper, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void loadProductUnits(String productId, DefaultTableModel model) {
        List<ProductUnit> units = ProductUnitsSql.getInstance().selectByProductId(productId);
        fillProductUnitsToModel(units, model);
    }

    private void handleImportCSV() {
        if (!requireStockImportPermission()) {
            return;
        }

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

    private class InventoryCurveChartPanel extends JPanel {

        private final Color chartBg = new Color(20, 35, 52);
        private final Color gridColor = new Color(55, 75, 95);
        private final Color lineColor = new Color(116, 95, 255);
        private final Color pointColor = new Color(255, 92, 122);

        public InventoryCurveChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(700, 320));
            setBorder(new EmptyBorder(18, 18, 18, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 10;
            int y = 10;
            int w = getWidth() - 20;
            int h = getHeight() - 20;

            g2.setColor(chartBg);
            g2.fillRoundRect(x, y, w, h, 22, 22);

            drawChartTitle(g2, x, y, w);
            drawGrid(g2, x, y, w, h);
            drawCurve(g2, x, y, w, h);
            drawLegend(g2, x, y, w);

            g2.dispose();
        }

        private void drawChartTitle(Graphics2D g2, int x, int y, int w) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.setColor(Color.WHITE);
            g2.drawString("Inventory Trend by Category", x + 22, y + 34);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(180, 195, 210));
            g2.drawString("Curve Line Chart - mô phỏng dashboard phân tích sản phẩm", x + 22, y + 54);
        }

        private void drawGrid(Graphics2D g2, int x, int y, int w, int h) {
            int chartX = x + 60;
            int chartY = y + 75;
            int chartW = w - 95;
            int chartH = h - 120;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(new Color(150, 165, 180));

            int lines = 5;
            for (int i = 0; i <= lines; i++) {
                int gy = chartY + (chartH * i / lines);

                g2.setColor(gridColor);
                g2.drawLine(chartX, gy, chartX + chartW, gy);

                g2.setColor(new Color(150, 165, 180));
                int value = (lines - i) * 20;
                g2.drawString(String.valueOf(value) + "%", x + 22, gy + 4);
            }

            g2.setColor(new Color(150, 165, 180));

            List<String> labels = getCategoryLabelsForChart();
            int n = labels.size();

            if (n == 0) {
                return;
            }

            for (int i = 0; i < n; i++) {
                int px = chartX + (n == 1 ? chartW / 2 : i * chartW / (n - 1));
                g2.drawString(labels.get(i), px - 18, chartY + chartH + 28);
            }
        }

        private void drawCurve(Graphics2D g2, int x, int y, int w, int h) {
            int chartX = x + 60;
            int chartY = y + 75;
            int chartW = w - 95;
            int chartH = h - 120;

            List<Integer> values = getCategoryPercentValuesForChart();

            if (values.isEmpty()) {
                drawEmptyChart(g2, chartX, chartY, chartW, chartH);
                return;
            }

            int n = values.size();

            int[] xs = new int[n];
            int[] ys = new int[n];

            for (int i = 0; i < n; i++) {
                xs[i] = chartX + (n == 1 ? chartW / 2 : i * chartW / (n - 1));
                int percent = Math.max(0, Math.min(100, values.get(i)));
                ys[i] = chartY + chartH - (percent * chartH / 100);
            }

            // vùng glow nhẹ
            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(116, 95, 255, 55));
            drawSmoothLine(g2, xs, ys);

            // line chính
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(lineColor);
            drawSmoothLine(g2, xs, ys);

            // điểm
            for (int i = 0; i < n; i++) {
                g2.setColor(pointColor);
                g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(xs[i] - 5, ys[i] - 5, 10, 10);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                g2.drawString(values.get(i) + "%", xs[i] - 10, ys[i] - 12);
            }
        }

        private void drawSmoothLine(Graphics2D g2, int[] xs, int[] ys) {
            if (xs.length == 1) {
                g2.drawLine(xs[0], ys[0], xs[0], ys[0]);
                return;
            }

            java.awt.geom.Path2D path = new java.awt.geom.Path2D.Double();
            path.moveTo(xs[0], ys[0]);

            for (int i = 0; i < xs.length - 1; i++) {
                int x1 = xs[i];
                int y1 = ys[i];
                int x2 = xs[i + 1];
                int y2 = ys[i + 1];

                int ctrlX1 = x1 + (x2 - x1) / 2;
                int ctrlY1 = y1;
                int ctrlX2 = x1 + (x2 - x1) / 2;
                int ctrlY2 = y2;

                path.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x2, y2);
            }

            g2.draw(path);
        }

        private void drawLegend(Graphics2D g2, int x, int y, int w) {
            int lx = x + w - 210;
            int ly = y + 30;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            g2.setColor(lineColor);
            g2.fillOval(lx, ly - 8, 8, 8);
            g2.setColor(new Color(210, 220, 235));
            g2.drawString("Tồn kho", lx + 14, ly);

            g2.setColor(pointColor);
            g2.fillOval(lx + 80, ly - 8, 8, 8);
            g2.setColor(new Color(210, 220, 235));
            g2.drawString("Cảnh báo", lx + 94, ly);
        }

        private void drawEmptyChart(Graphics2D g2, int chartX, int chartY, int chartW, int chartH) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("Chưa có dữ liệu sản phẩm để hiển thị biểu đồ.", chartX + 30, chartY + chartH / 2);
        }

        private List<String> getCategoryLabelsForChart() {
            List<String> labels = new ArrayList<>();

            for (String categoryId : categoryNameMap.keySet()) {
                labels.add(categoryId);
            }

            return labels;
        }

        private List<Integer> getCategoryPercentValuesForChart() {
            List<Integer> values = new ArrayList<>();

            int totalInventory = 0;
            Map<String, Integer> quantityByCategory = new java.util.LinkedHashMap<>();

            for (String categoryId : categoryNameMap.keySet()) {
                quantityByCategory.put(categoryId, 0);
            }

            for (Product p : cachedProducts) {
                String categoryId = p.getCategoryId();

                if (categoryId == null) {
                    continue;
                }

                categoryId = categoryId.trim();
                int qty = p.getQuantity();

                totalInventory += qty;
                quantityByCategory.put(categoryId, quantityByCategory.getOrDefault(categoryId, 0) + qty);
            }

            for (String categoryId : categoryNameMap.keySet()) {
                int qty = quantityByCategory.getOrDefault(categoryId, 0);
                int percent = totalInventory == 0 ? 0 : (int) Math.round(qty * 100.0 / totalInventory);
                values.add(percent);
            }

            return values;
        }
    }

}
