package business.sql.hr_kpi;

import model.employee.Shift; // Giả định bạn sẽ tạo model trong package tương ứng
import business.sql.SqlInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShiftSql implements SqlInterface<Shift> {
    public static ShiftSql getInstance() {
        return new ShiftSql();
    }

    @Override
    public int insert(Shift t) {
        String sql = "INSERT INTO SHIFTS (shift_id, shift_name, start_time, end_time, is_deleted) "
                + "VALUES (?, ?, ?, ?, 0)";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, t.getShiftId());
            pst.setString(2, t.getShiftName());
            pst.setDate(3, t.getStartTime());
            pst.setDate(4, t.getEndTime());
            return pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
    @Override public int update(Shift t) { return 0; }
    @Override public int delete(String id) { return 0; }
    @Override public Shift selectById(String id) { return null; }

    @Override
    public List<Shift> selectByCondition(String condition) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    @Override
    public ArrayList<Shift> selectAll() {
        ensureDefaultShifts();
        ArrayList<Shift> list = new ArrayList<>();
        String sql = "SELECT * FROM SHIFTS WHERE is_deleted = 0";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                Shift s = new Shift();
                s.setShiftId(rs.getString("shift_id"));
                s.setShiftName(rs.getString("shift_name"));
                java.sql.Timestamp start = rs.getTimestamp("start_time");
                java.sql.Timestamp end = rs.getTimestamp("end_time");
                if (start != null) {
                    s.setStartTime(new java.sql.Date(start.getTime()));
                }
                if (end != null) {
                    s.setEndTime(new java.sql.Date(end.getTime()));
                }
                s.setIsDeleted(rs.getInt("is_deleted"));
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void ensureDefaultShifts() {
        String countSql = "SELECT COUNT(*) FROM SHIFTS WHERE NVL(is_deleted, 0) = 0";
        String insertSql = "INSERT INTO SHIFTS (shift_id, shift_name, start_time, end_time, is_deleted) "
                + "VALUES (?, ?, TO_DATE(?, 'HH24:MI'), TO_DATE(?, 'HH24:MI'), 0)";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement countPs = con.prepareStatement(countSql);
             ResultSet rs = countPs.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }

            try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                insertDefaultShift(insertPs, "SHIFT_MORNING", "Ca sáng", "07:00", "15:00");
                insertDefaultShift(insertPs, "SHIFT_AFTERNOON", "Ca chiều", "15:00", "23:00");
                insertDefaultShift(insertPs, "SHIFT_NIGHT", "Ca tối", "23:00", "07:00");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertDefaultShift(PreparedStatement ps, String id, String name, String start, String end)
            throws SQLException {
        ps.setString(1, id);
        ps.setString(2, name);
        ps.setString(3, start);
        ps.setString(4, end);
        ps.executeUpdate();
    }
}
