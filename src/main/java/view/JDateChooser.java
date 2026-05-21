package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class JDateChooser extends JPanel {

    private JTextField txtDate;
    private JButton btnPopup;
    private Date selectedDate;
    private SimpleDateFormat sdf;
    private JPopupMenu popup;
    private Calendar currentViewCal;

    public JDateChooser() {
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new LineBorder(new Color(226, 232, 240))); // Viền giống màu của dự án

        txtDate = new JTextField();
        txtDate.setEditable(false);
        txtDate.setBackground(Color.WHITE);
        txtDate.setBorder(new EmptyBorder(0, 10, 0, 10));
        txtDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        btnPopup = new JButton("📅");
        btnPopup.setFocusPainted(false);
        btnPopup.setPreferredSize(new Dimension(40, 0));
        btnPopup.setBackground(new Color(243, 246, 250));
        btnPopup.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnPopup.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        btnPopup.addActionListener(e -> showPopup());

        add(txtDate, BorderLayout.CENTER);
        add(btnPopup, BorderLayout.EAST);

        currentViewCal = Calendar.getInstance();
    }

    public void setDateFormatString(String format) {
        this.sdf = new SimpleDateFormat(format);
        updateTextField();
    }

    public Date getDate() {
        return selectedDate;
    }

    public void setDate(Date date) {
        this.selectedDate = date;
        if (date != null) {
            currentViewCal.setTime(date);
        } else {
            currentViewCal = Calendar.getInstance(); // Nếu xóa ngày, hiển thị lịch tháng hiện tại
        }
        updateTextField();
    }

    private void updateTextField() {
        if (selectedDate != null) {
            txtDate.setText(sdf.format(selectedDate));
        } else {
            txtDate.setText("");
        }
    }

    private void showPopup() {
        if (popup == null) {
            popup = new JPopupMenu();
            popup.setLayout(new BorderLayout());
            popup.setBorder(new LineBorder(new Color(226, 232, 240)));
        }
        updateCalendar();
        popup.show(this, 0, getHeight());
    }

    private void updateCalendar() {
        popup.removeAll();
        popup.add(createCalendarPanel(), BorderLayout.CENTER);
        popup.revalidate();
        popup.repaint();
        popup.pack();
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Phần Tiêu đề: Nút chuyển tháng và tên tháng
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        
        JButton btnPrev = new JButton("<");
        btnPrev.setFocusPainted(false);
        btnPrev.setContentAreaFilled(false);
        btnPrev.setBorderPainted(false);
        btnPrev.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPrev.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnPrev.addActionListener(e -> {
            currentViewCal.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        JButton btnNext = new JButton(">");
        btnNext.setFocusPainted(false);
        btnNext.setContentAreaFilled(false);
        btnNext.setBorderPainted(false);
        btnNext.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNext.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnNext.addActionListener(e -> {
            currentViewCal.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        String monthYearStr = "Tháng " + (currentViewCal.get(Calendar.MONTH) + 1) + " - " + currentViewCal.get(Calendar.YEAR);
        JLabel lblMonth = new JLabel(monthYearStr, SwingConstants.CENTER);
        lblMonth.setFont(new Font("Segoe UI", Font.BOLD, 14));

        header.add(btnPrev, BorderLayout.WEST);
        header.add(lblMonth, BorderLayout.CENTER);
        header.add(btnNext, BorderLayout.EAST);

        // Lưới hiển thị các ngày
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String day : days) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(new Color(143, 154, 179));
            grid.add(lbl);
        }

        Calendar cal = (Calendar) currentViewCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Lấp đầy khoảng trống ở các ô trước ngày 1
        for (int i = 1; i < firstDayOfWeek; i++) {
            grid.add(new JLabel(""));
        }

        // Vẽ các nút ngày
        for (int i = 1; i <= daysInMonth; i++) {
            final int day = i;
            JButton btnDay = new JButton(String.valueOf(day));
            btnDay.setMargin(new Insets(2, 2, 2, 2));
            btnDay.setFocusPainted(false);
            btnDay.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            btnDay.setBackground(Color.WHITE);
            btnDay.setBorder(new LineBorder(new Color(240, 240, 240)));
            btnDay.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

            // Tô xanh ngày đang được chọn
            if (selectedDate != null) {
                Calendar selCal = Calendar.getInstance();
                selCal.setTime(selectedDate);
                if (selCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    selCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                    selCal.get(Calendar.DAY_OF_MONTH) == day) {
                    
                    btnDay.setBackground(new Color(37, 99, 235)); // Màu blue
                    btnDay.setForeground(Color.WHITE);
                }
            }

            btnDay.addActionListener(e -> {
                Calendar chosen = (Calendar) currentViewCal.clone();
                chosen.set(Calendar.DAY_OF_MONTH, day);
                setDate(chosen.getTime());
                popup.setVisible(false); // Đóng popup khi đã chọn
            });
            grid.add(btnDay);
        }

        panel.add(header, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);

        // Nút Xóa (Clear)
        JButton btnClear = new JButton("Xóa chọn (Clear)");
        btnClear.setFocusPainted(false);
        btnClear.setBackground(new Color(254, 242, 242));
        btnClear.setForeground(new Color(239, 68, 68));
        btnClear.setBorder(new EmptyBorder(8, 0, 8, 0));
        btnClear.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> {
            setDate(null);
            popup.setVisible(false);
        });
        panel.add(btnClear, BorderLayout.SOUTH);

        return panel;
    }
}