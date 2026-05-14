/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package view;

import java.util.List;

/**
 *
 * @author Admin
 */
public class StatisticView extends javax.swing.JPanel {

    /**
     * Creates new form StatisticView
     */
    public StatisticView() {
        initComponents();
        initPowerBIGuide();
    }
    
    /**
     * Khởi tạo hướng dẫn chèn link Power BI
     */
    private void initPowerBIGuide() {
        // Tạo panel hướng dẫn Power BI
        javax.swing.JPanel pnPowerBIGuide = new javax.swing.JPanel();
        pnPowerBIGuide.setBackground(new java.awt.Color(255, 255, 230));
        pnPowerBIGuide.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 204, 0), 2),
            javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        pnPowerBIGuide.setLayout(new java.awt.BorderLayout(10, 10));
        
        // Tiêu đề
        javax.swing.JLabel lblTitle = new javax.swing.JLabel("📊 HƯỚNG DẪN TÍCH HỢP BÁO CÁO POWER BI");
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        lblTitle.setForeground(new java.awt.Color(204, 153, 0));
        lblTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Nội dung hướng dẫn chi tiết
        String guideText = "<html><body style='width: 700px; font-family: Segoe UI; font-size: 13px; line-height: 1.6;'>" +
            
            "<div style='background: #fff; padding: 15px; border-left: 4px solid #3498db; margin-bottom: 15px;'>" +
            "<b style='color: #2c3e50; font-size: 14px;'>📌 CÁCH LẤY LINK BÁO CÁO POWER BI:</b><br>" +
            "<span style='color: #555;'>" +
            "1. Truy cập <b>Power BI Service</b> tại <code>app.powerbi.com</code><br>" +
            "2. Mở báo cáo bạn muốn chia sẻ<br>" +
            "3. Chọn menu <b>File</b> → <b>Embed Report</b> → <b>Publish to Web (Public)</b><br>" +
            "4. Xác nhận bằng cách chọn <b>Create Embed Code</b><br>" +
            "5. Sao chép URL từ iframe (phần trong dấu nháy đơn của thuộc tính src)<br>" +
            "<i style='color: #e74c3c;'>⚠️ Lưu ý: Tính năng này chỉ khả dụng với tài khoản Power BI Pro hoặc Premium</i>" +
            "</span></div>" +
            
            "<div style='background: #f0f9ff; padding: 15px; border-left: 4px solid #2ecc71; margin-bottom: 15px;'>" +
            "<b style='color: #2c3e50; font-size: 14px;'>🔧 PHƯƠNG ÁN A: NHÚNG IFRAME VÀO JEditorPane (ĐƠN GIẢN)</b><br>" +
            "<span style='color: #555;'>Sử dụng JEditorPane để hiển thị nội dung HTML chứa iframe Power BI:<br><br>" +
            "<code style='background: #2c3e50; color: #ecf0f1; padding: 8px; display: block; border-radius: 4px;'>" +
            "// Code mẫu nhúng Power BI vào Swing<br>" +
            "String powerBIUrl = \"https://app.powerbi.com/reportEmbed?reportId=YOUR_REPORT_ID\";<br>" +
            "String htmlContent = \"&lt;iframe width='100%' height='600' src='\" + powerBIUrl + \"' frameborder='0'&gt;&lt;/iframe&gt;\";<br>" +
            "JEditorPane editorPane = new JEditorPane(\"text/html\", htmlContent);<br>" +
            "editorPane.setEditable(false);<br>" +
            "editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);<br>" +
            "JScrollPane scrollPane = new JScrollPane(editorPane);</code></span></div>" +
            
            "<div style='background: #fff9e6; padding: 15px; border-left: 4px solid #f39c12; margin-bottom: 15px;'>" +
            "<b style='color: #2c3e50; font-size: 14px;'>🚀 PHƯƠNG ÁN B: SỬ DỤNG JXBrowser (CHUYÊN NGHIỆP)</b><br>" +
            "<span style='color: #555;'>Thêm thư viện JXBrowser vào pom.xml và sử dụng Browser component:<br><br>" +
            "<code style='background: #2c3e50; color: #ecf0f1; padding: 8px; display: block; border-radius: 4px;'>" +
            "&lt;!-- Thêm vào pom.xml --&gt;<br>" +
            "&lt;dependency&gt;<br>" +
            "  &lt;groupId&gt;com.teamdev.jxbrowser&lt;/groupId&gt;<br>" +
            "  &lt;artifactId&gt;jxbrowser&lt;/artifactId&gt;<br>" +
            "  &lt;version&gt;7.33&lt;/version&gt;<br>" +
            "&lt;/dependency&gt;<br><br>" +
            "// Code mẫu với JXBrowser<br>" +
            "Browser browser = Browser.newInstance();<br>" +
            "browser.mainFrame().loadUrl(powerBIUrl);<br>" +
            "SwingBrowserComponent browserComponent = new SwingBrowserComponent(browser);<br>" +
            "panel.add(browserComponent, BorderLayout.CENTER);</code></span></div>" +
            
            "<div style='background: #ffeaea; padding: 15px; border-left: 4px solid #e74c3c;'>" +
            "<b style='color: #c0392b; font-size: 14px;'>⚠️ LƯU Ý QUAN TRỌNG:</b><br>" +
            "<span style='color: #555;'>" +
            "• Báo cáo Power BI phải được cấu hình ở chế độ <b>Public</b> hoặc có authentication phù hợp<br>" +
            "• Với dữ liệu nhạy cảm, nên sử dụng phương án xác thực qua Azure AD<br>" +
            "• Kiểm tra firewall và CORS settings nếu gặp lỗi khi nhúng<br>" +
            "• Đảm bảo kết nối internet ổn định khi hiển thị báo cáo trực tuyến" +
            "</span></div>" +
            
            "</body></html>";
        
        javax.swing.JLabel lblContent = new javax.swing.JLabel(guideText);
        lblContent.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        
        pnPowerBIGuide.add(lblTitle, BorderLayout.NORTH);
        pnPowerBIGuide.add(lblContent, BorderLayout.CENTER);
        
        // Thêm panel hướng dẫn vào layout chính
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnTop, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnPowerBIGuide, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(tbStatistic)
        );
        
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnPowerBIGuide, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbStatistic, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE))
        );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tbStatistic = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        pnTop = new javax.swing.JPanel();
        pnTotalRevenue = new javax.swing.JPanel();
        TotalRevenue = new javax.swing.JLabel();
        pnTotalCustomer = new javax.swing.JPanel();
        TotalCustomer = new javax.swing.JLabel();
        pnTotalOrder = new javax.swing.JPanel();
        TotalOrder = new javax.swing.JLabel();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Mã sản phẩm", "Tên sản phẩm", "Số lượng bán", "Doanh thu"
            }
        ));
        tbStatistic.setViewportView(jTable1);

        pnTop.setLayout(new java.awt.GridLayout(1, 0));

        pnTotalRevenue.setBackground(new java.awt.Color(204, 255, 204));
        pnTotalRevenue.setPreferredSize(new java.awt.Dimension(92, 38));

        TotalRevenue.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        TotalRevenue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TotalRevenue.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view/image/money-Photoroom.png"))); // NOI18N
        TotalRevenue.setText("Tổng doanh thu");

        javax.swing.GroupLayout pnTotalRevenueLayout = new javax.swing.GroupLayout(pnTotalRevenue);
        pnTotalRevenue.setLayout(pnTotalRevenueLayout);
        pnTotalRevenueLayout.setHorizontalGroup(
            pnTotalRevenueLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TotalRevenue, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
        );
        pnTotalRevenueLayout.setVerticalGroup(
            pnTotalRevenueLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TotalRevenue, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
        );

        pnTop.add(pnTotalRevenue);

        pnTotalCustomer.setBackground(new java.awt.Color(255, 204, 153));

        TotalCustomer.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        TotalCustomer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TotalCustomer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view/image/multiple-users-silhouette-Photoroom.png"))); // NOI18N
        TotalCustomer.setText("Tổng khách hàng");

        javax.swing.GroupLayout pnTotalCustomerLayout = new javax.swing.GroupLayout(pnTotalCustomer);
        pnTotalCustomer.setLayout(pnTotalCustomerLayout);
        pnTotalCustomerLayout.setHorizontalGroup(
            pnTotalCustomerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TotalCustomer, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
        );
        pnTotalCustomerLayout.setVerticalGroup(
            pnTotalCustomerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TotalCustomer, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
        );

        pnTop.add(pnTotalCustomer);

        pnTotalOrder.setBackground(new java.awt.Color(153, 204, 255));
        pnTotalOrder.setPreferredSize(new java.awt.Dimension(92, 38));

        TotalOrder.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        TotalOrder.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        TotalOrder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view/image/trolley-Photoroom.png"))); // NOI18N
        TotalOrder.setText("Tổng đơn hàng");

        javax.swing.GroupLayout pnTotalOrderLayout = new javax.swing.GroupLayout(pnTotalOrder);
        pnTotalOrder.setLayout(pnTotalOrderLayout);
        pnTotalOrderLayout.setHorizontalGroup(
            pnTotalOrderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TotalOrder, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
        );
        pnTotalOrderLayout.setVerticalGroup(
            pnTotalOrderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TotalOrder, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
        );

        pnTop.add(pnTotalOrder);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnTop, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
            .addComponent(tbStatistic)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbStatistic, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel TotalCustomer;
    private javax.swing.JLabel TotalOrder;
    private javax.swing.JLabel TotalRevenue;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel pnTop;
    private javax.swing.JPanel pnTotalCustomer;
    private javax.swing.JPanel pnTotalOrder;
    private javax.swing.JPanel pnTotalRevenue;
    private javax.swing.JScrollPane tbStatistic;
    // End of variables declaration//GEN-END:variables
}
