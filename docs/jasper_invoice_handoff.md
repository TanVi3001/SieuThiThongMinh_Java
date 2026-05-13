# Jasper Sales Invoice Handoff

## Project
- Path: `D:\UIT\HocTrenTruong\HK4\Lap_Trinh_Java_IS216\DoAn\SieuThiOnline_Java`
- Stack: Java Swing + Maven + Oracle
- Feature: JasperReports Sales Invoice / Bill report

## Files Involved
- `pom.xml`
- `src/main/java/common/report/ReportViewer.java`
- `src/main/resources/reports/SalesInvoiceReport.jrxml`
- `src/main/java/view/OrderView.java`
- `database/seed_invoice_report.sql`

## Current State
- JasperReports dependency has been added.
- `ReportViewer.java` exists and compiles/fills `.jrxml` using `common.db.DatabaseConnection`.
- `OrderView` button `Xuat hoa don` opens `SalesInvoiceReport.jrxml`.
- Report parameter: `ORDER_ID`.
- `SalesInvoiceReport.jrxml` was checked with `JasperCompileManager` and compiled OK.
- `mvn compile` passed earlier after Maven network/cache access was allowed.

## Test Data
- Test `ORDER_ID` values:
  - `INV_RPT001`
  - `INV_RPT002`
  - `INV_RPT003`
- Seed file: `database/seed_invoice_report.sql`
- Seed uses `WHERE NOT EXISTS` to avoid duplicate primary keys on rerun.

## Warning
- The real Oracle DB schema differs from `database/KhoiTaoCacBang.sql`.
- Verify real DB columns before fixing seed/report errors.
- `PAYMENT_METHOD_NAME` mismatch was already fixed by using `payment_method_id`.

## Next Steps
- Run `database/seed_invoice_report.sql`.
- Verify orders and order details exist.
- Open app -> `OrderView` -> select invoice -> click `Xuat hoa don`.
- Debug DB/schema mismatches if errors appear.

## Prompt For Next Codex Session
Continue the Jasper Sales Invoice work from `docs/jasper_invoice_handoff.md`. Inspect the current files, verify the real Oracle DB schema before changing SQL, run/fix `database/seed_invoice_report.sql`, then test `ORDER_ID` values `INV_RPT001`, `INV_RPT002`, and `INV_RPT003` through `OrderView` -> `Xuat hoa don`. Do not modify unrelated files and do not commit unless explicitly asked.
