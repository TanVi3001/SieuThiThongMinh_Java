
package view.components;

import common.db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminDataPanel extends JPanel {
    private final String[] statTitles;
    private final String[] statSql;
    private final String tableSql;
    private final String[] columns;
    private final JPanel statPanel = new JPanel(new GridLayout(1, 1, 14, 14));
    private final DefaultTableModel model;
    private final JTable table;

    public AdminDataPanel(String title, String subtitle, String[] statTitles, String[] statSql, String tableTitle, String tableSql, String[] columns) {
        super(new BorderLayout());
        this.statTitles = statTitles;
        this.statSql = statSql;
        this.tableSql = tableSql;
        this.columns = columns;
        JPanel page = AdminUIFactory.page(title, subtitle);
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);

        statPanel.setOpaque(false);
        statPanel.setLayout(new GridLayout(1, Math.max(1, statTitles.length), 14, 14));
        center.add(statPanel, BorderLayout.NORTH);

        AdminUIFactory.RoundedPanel card = AdminUIFactory.card();
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(AdminUIFactory.cardTitle(tableTitle), BorderLayout.WEST);
        JButton refresh = AdminUIFactory.button("Lam moi", AdminUIFactory.SECONDARY);
        refresh.addActionListener(e -> reload());
        top.add(refresh, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        AdminUIFactory.setupTable(table);
        if (columns.length > 0) table.getColumnModel().getColumn(columns.length - 1).setCellRenderer(new AdminUIFactory.BadgeRenderer());
        card.add(AdminUIFactory.scroll(table), BorderLayout.CENTER);

        center.add(card, BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
        reload();
    }

    public final void reload() {
        loadStats();
        loadTable();
    }

    private void loadStats() {
        statPanel.removeAll();
        try (Connection con = DatabaseConnection.getConnection()) {
            for (int i = 0; i < statTitles.length; i++) {
                String value = "0";
                try (PreparedStatement ps = con.prepareStatement(statSql[i]); ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) value = String.valueOf(rs.getObject(1));
                } catch (Exception ex) {
                    value = "0";
                }
                Color accent = i == 1 ? AdminUIFactory.SUCCESS : i == 2 ? AdminUIFactory.WARNING : i == 3 ? AdminUIFactory.DANGER : AdminUIFactory.PRIMARY;
                statPanel.add(AdminUIFactory.statCard(statTitles[i], value, accent));
            }
        } catch (Exception e) {
            for (String statTitle : statTitles) statPanel.add(AdminUIFactory.statCard(statTitle, "0", AdminUIFactory.PRIMARY));
        }
        statPanel.revalidate();
        statPanel.repaint();
    }

    private void loadTable() {
        model.setRowCount(0);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(tableSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 0; i < columns.length; i++) row[i] = rs.getObject(i + 1);
                model.addRow(row);
            }
        } catch (Exception e) {
            System.err.println("[AdminDataPanel] " + e.getMessage());
        }
    }
}
