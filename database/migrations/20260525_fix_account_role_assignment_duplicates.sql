-- ==========================================================
-- Fix Account Role Assignment duplicate history rows
-- Date: 2026-05-25
--
-- Problem:
--   AccountRoleAssignmentPanel -> AccountSql.updateAccountRole(...)
--   can hit ORA-00001 on ACCOUNT_ASSIGN_ROLE when old soft-deleted
--   rows still contain the target role_id for the same account_id.
--
-- Goal:
--   Keep account role assignment data canonical before running the app:
--   - one active direct role per account
--   - remove stale inactive duplicate role rows
--   - keep the newest/active row as the canonical row
--
-- Run this once in DataGrip after pulling the branch.
-- ==========================================================

SET SERVEROUTPUT ON;

BEGIN
    DBMS_OUTPUT.PUT_LINE('[MIGRATION] Start cleanup ACCOUNT_ASSIGN_ROLE duplicates');
END;
/

-- 1) If the same account_id + role_id exists multiple times, keep one row only.
-- Prefer active rows first, then newest ROWID as fallback.
DELETE FROM ACCOUNT_ASSIGN_ROLE aar
WHERE ROWID IN (
    SELECT rid
    FROM (
        SELECT ROWID AS rid,
               ROW_NUMBER() OVER (
                   PARTITION BY account_id, role_id
                   ORDER BY NVL(is_deleted, 0), ROWID DESC
               ) AS rn
        FROM ACCOUNT_ASSIGN_ROLE
    ) x
    WHERE x.rn > 1
);

-- 2) If one account has many active direct roles, keep only one active row.
-- Prefer Admin > Manager > Warehouse > Sale, then fallback by role_id.
UPDATE ACCOUNT_ASSIGN_ROLE aar
SET is_deleted = 1
WHERE NVL(aar.is_deleted, 0) = 0
  AND aar.ROWID IN (
      SELECT rid
      FROM (
          SELECT ROWID AS rid,
                 ROW_NUMBER() OVER (
                     PARTITION BY account_id
                     ORDER BY CASE UPPER(TRIM(role_id))
                                  WHEN 'R_ADMIN_ALL' THEN 1
                                  WHEN 'R_STORE_MNG' THEN 2
                                  WHEN 'R_STAFF_VIEW_PROD' THEN 3
                                  WHEN 'R_STAFF_STOCK' THEN 3
                                  WHEN 'R_STAFF_SALE' THEN 4
                                  ELSE 9
                              END,
                              role_id
                 ) AS rn
          FROM ACCOUNT_ASSIGN_ROLE
          WHERE NVL(is_deleted, 0) = 0
      ) x
      WHERE x.rn > 1
  );

-- 3) Remove inactive history rows for accounts that already have exactly one active role.
-- This prevents AccountSql.updateAccountRole from updating a stale row into a role_id
-- that already exists for the same account and hitting ORA-00001.
DELETE FROM ACCOUNT_ASSIGN_ROLE aar
WHERE NVL(aar.is_deleted, 0) = 1
  AND EXISTS (
      SELECT 1
      FROM ACCOUNT_ASSIGN_ROLE active_aar
      WHERE active_aar.account_id = aar.account_id
        AND NVL(active_aar.is_deleted, 0) = 0
  );

COMMIT;

BEGIN
    DBMS_OUTPUT.PUT_LINE('[MIGRATION] Done cleanup ACCOUNT_ASSIGN_ROLE duplicates');
END;
/
