package business.sql.rbac;

import java.util.ArrayList;
import java.sql.Timestamp;

public class AccountAssignRoleGroupSql {

    // Áp dụng Singleton Pattern
    public static AccountAssignRoleGroupSql getInstance() {
        return new AccountAssignRoleGroupSql();
    }

    /**
     * Gán một tài khoản vào một nhóm quyền
     * @param accountId
     * @param roleGroupId
     * @return 
     */
    public int assignAccountToGroup(String accountId, String roleGroupId) {
        // TODO: INSERT INTO ACCOUNT_ASSIGN_ROLE_GROUP (account_id, role_group_id) VALUES (?, ?)
        return 0;
    }

    /**
     * Gỡ tài khoản khỏi nhóm quyền (Xóa vật lý hoặc xóa mềm tùy bạn chọn)
     * @param accountId
     * @param roleGroupId
     * @return 
     */
    public int removeAccountFromGroup(String accountId, String roleGroupId) {
        // TODO: DELETE FROM ACCOUNT_ASSIGN_ROLE_GROUP WHERE account_id = ? AND role_group_id = ?
        // Hoặc UPDATE is_deleted = 1
        return 0;
    }

    /**
     * Lấy danh sách ID các nhóm quyền mà một tài khoản đang tham gia
     * @param accountId
     * @return 
     */
    public ArrayList<String> getGroupsByAccountId(String accountId) {
        // TODO: SELECT role_group_id FROM ACCOUNT_ASSIGN_ROLE_GROUP WHERE account_id = ? AND is_deleted = 0
        return new ArrayList<>();
    }

    /**
     * Lấy danh sách ID các tài khoản thuộc một nhóm quyền nào đó
     * @param roleGroupId
     * @return 
     */
    public ArrayList<String> getAccountsByGroupId(String roleGroupId) {
        // TODO: SELECT account_id FROM ACCOUNT_ASSIGN_ROLE_GROUP WHERE role_group_id = ? AND is_deleted = 0
        return new ArrayList<>();
    }
    
    
}