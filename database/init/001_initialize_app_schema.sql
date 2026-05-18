WHENEVER SQLERROR EXIT SQL.SQLCODE

CONNECT system/Admin123@//localhost/FREEPDB1

@/workspace-database/KhoiTaoCacBang.sql
@/workspace-database/migration_invoice_payment.sql
@/workspace-database/seed_local_dev_accounts.sql
@/workspace-database/seed_data.sql
@/workspace-database/seed_kpi_criteria.sql
@/workspace-database/seed_invoice_report.sql