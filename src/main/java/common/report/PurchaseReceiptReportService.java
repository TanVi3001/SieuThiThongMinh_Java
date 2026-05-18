package common.report;

import business.sql.prod_inventory.InventoryTransactionSql;
import common.db.DatabaseConnection;
import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;

public class PurchaseReceiptReportService {

    private static final String REPORT_FILE = "PurchaseReceiptReport.jrxml";
    private static final String[] DIGITS = {
        "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] UNITS = {"", "nghìn", "triệu", "tỷ"};

    private PurchaseReceiptReportService() {
    }

    public static void showPurchaseReceipt(String receiptId) {
        ReportRequest request = buildRequest(receiptId);

        if (request == null) {
            return;
        }

        ReportViewer.showReport(getReportPath(), request.parameters);
    }

    public static void printPurchaseReceipt(String receiptId) {
        ReportRequest request = buildRequest(receiptId);

        if (request == null) {
            return;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (connection == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "Không thể kết nối database. Kiểm tra cấu hình Oracle.",
                        "Lỗi in phiếu",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            File reportFile = new File(getReportPath());
            JasperReport report = JasperCompileManager.compileReport(reportFile.getAbsolutePath());
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, request.parameters, connection);
            JasperPrintManager.printReport(jasperPrint, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Không thể in phiếu nhập:\n" + ex.getMessage(),
                    "Lỗi in phiếu",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static ReportRequest buildRequest(String receiptId) {
        String cleanReceiptId = receiptId == null ? "" : receiptId.trim();

        if (cleanReceiptId.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Vui lòng chọn phiếu nhập cần xuất.");
            return null;
        }

        List<InventoryTransactionSql.PurchaseReceiptLineDTO> lines
                = InventoryTransactionSql.getInstance().getReceiptLines(cleanReceiptId);

        if (lines == null || lines.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Không tìm thấy chi tiết phiếu nhập: " + cleanReceiptId,
                    "Không có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (InventoryTransactionSql.PurchaseReceiptLineDTO line : lines) {
            if (line.afterTax != null) {
                grandTotal = grandTotal.add(line.afterTax);
            }
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("RECEIPT_ID", cleanReceiptId);
        params.put("AMOUNT_IN_WORDS", toVietnameseMoneyWords(grandTotal));

        return new ReportRequest(params);
    }

    private static String getReportPath() {
        return Paths.get(
                "src", "main", "resources", "reports", REPORT_FILE
        ).toAbsolutePath().toString();
    }

    private static String toVietnameseMoneyWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Không đồng.";
        }

        long value = amount.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        String words = readNumber(value);

        if (words.isEmpty()) {
            return "Không đồng.";
        }

        return Character.toUpperCase(words.charAt(0)) + words.substring(1) + " đồng.";
    }

    private static String readNumber(long value) {
        if (value == 0) {
            return DIGITS[0];
        }

        StringBuilder result = new StringBuilder();
        int unitIndex = 0;
        boolean hasHigherGroup = false;

        while (value > 0 && unitIndex < UNITS.length) {
            int group = (int) (value % 1000);

            if (group > 0) {
                String groupWords = readThreeDigits(group, hasHigherGroup);
                if (!UNITS[unitIndex].isEmpty()) {
                    groupWords += " " + UNITS[unitIndex];
                }

                if (result.length() > 0) {
                    result.insert(0, " ");
                }
                result.insert(0, groupWords);
            }

            hasHigherGroup = hasHigherGroup || group > 0;
            value /= 1000;
            unitIndex++;
        }

        return result.toString().trim().replaceAll("\\s+", " ");
    }

    private static String readThreeDigits(int number, boolean hasHigherGroup) {
        int hundred = number / 100;
        int ten = (number % 100) / 10;
        int unit = number % 10;

        StringBuilder words = new StringBuilder();

        if (hundred > 0) {
            words.append(DIGITS[hundred]).append(" trăm");
        } else if (hasHigherGroup && (ten > 0 || unit > 0)) {
            words.append("không trăm");
        }

        if (ten > 1) {
            appendSpace(words);
            words.append(DIGITS[ten]).append(" mươi");

            if (unit == 1) {
                words.append(" mốt");
            } else if (unit == 5) {
                words.append(" lăm");
            } else if (unit > 0) {
                words.append(" ").append(DIGITS[unit]);
            }
        } else if (ten == 1) {
            appendSpace(words);
            words.append("mười");

            if (unit == 5) {
                words.append(" lăm");
            } else if (unit > 0) {
                words.append(" ").append(DIGITS[unit]);
            }
        } else if (unit > 0) {
            if (hundred > 0 || hasHigherGroup) {
                appendSpace(words);
                words.append("lẻ ");
            }
            words.append(DIGITS[unit]);
        }

        return words.toString();
    }

    private static void appendSpace(StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
    }

    private record ReportRequest(HashMap<String, Object> parameters) {
    }
}
