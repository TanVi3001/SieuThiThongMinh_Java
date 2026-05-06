package business.service;

import business.sql.rbac.ActivationTokenSql;
import common.db.DatabaseConnection;

import java.sql.Connection;

public class ActivationTokenService {

    private final ActivationTokenSql tokenSql = new ActivationTokenSql();

    /**
     * Ở hệ hiện tại: code kích hoạt = employeeId (EMP... / MNG...)
     */
    public void issueToken(String employeeId) throws Exception {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId rỗng.");
        }

        String code = employeeId.trim(); // CODE = EMP.../MNG...

        try (Connection con = DatabaseConnection.getConnection()) {
            if (con == null) {
                throw new Exception("Không thể kết nối DB.");
            }

            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            try {
                // Log để debug đúng DB/schema
                System.out.println("[DB] user=" + con.getMetaData().getUserName()
                        + " url=" + con.getMetaData().getURL());

                // OPTION A: không cho trùng code
                if (tokenSql.existsCode(con, code)) {
                    throw new IllegalStateException("Mã kích hoạt đã tồn tại (CODE trùng): " + code);
                }

                tokenSql.createToken(con, code, code, 24);
                con.commit();

                System.out.println("[TOKEN] Inserted ACTIVATION_TOKENS: empId=" + code + ", code=" + code);
            } catch (Exception ex) {
                try {
                    con.rollback();
                } catch (Exception ignore) {
                }
                throw ex;
            } finally {
                try {
                    con.setAutoCommit(oldAutoCommit);
                } catch (Exception ignore) {
                }
            }
        }
    }
}
