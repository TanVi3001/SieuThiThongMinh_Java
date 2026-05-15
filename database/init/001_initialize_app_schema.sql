WHENEVER SQLERROR EXIT SQL.SQLCODE

CONNECT appuser/apppass@//localhost/FREEPDB1

@/workspace-database/KhoiTaoCacBang.sql
@/workspace-database/create_kpi_history_table.sql
@/workspace-database/migration_invoice_payment.sql
@/workspace-database/seed_local_dev_accounts.sql
@/workspace-database/seed_data.sql
@/workspace-database/seed_kpi_criteria.sql
@/workspace-database/seed_invoice_report.sql
