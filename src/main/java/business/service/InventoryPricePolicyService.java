package business.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InventoryPricePolicyService {

    private InventoryPricePolicyService() {
    }

    public static BigDecimal calculateImportPriceAfterVat(
            BigDecimal importPriceBeforeVat,
            BigDecimal vatRate
    ) {
        if (importPriceBeforeVat == null) {
            importPriceBeforeVat = BigDecimal.ZERO;
        }

        if (vatRate == null) {
            vatRate = BigDecimal.ZERO;
        }

        return importPriceBeforeVat
                .multiply(
                        BigDecimal.ONE.add(
                                vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        )
                )
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static void validateImportPriceLessThanSalePrice(
            BigDecimal importPriceBeforeVat,
            BigDecimal vatRate,
            BigDecimal salePrice
    ) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán sản phẩm không hợp lệ.");
        }

        if (importPriceBeforeVat == null || importPriceBeforeVat.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá nhập chưa VAT phải lớn hơn 0.");
        }

        BigDecimal importPriceAfterVat = calculateImportPriceAfterVat(
                importPriceBeforeVat,
                vatRate
        );

        if (importPriceAfterVat.compareTo(salePrice) >= 0) {
            throw new IllegalArgumentException(
                    "Không thể nhập hàng vì giá nhập sau VAT phải nhỏ hơn giá bán ra.\n\n"
                    + "Giá nhập chưa VAT: " + money(importPriceBeforeVat) + " VNĐ\n"
                    + "VAT: " + percent(vatRate) + "\n"
                    + "Giá nhập sau VAT: " + money(importPriceAfterVat) + " VNĐ\n"
                    + "Giá bán ra: " + money(salePrice) + " VNĐ"
            );
        }
    }

    public static BigDecimal calculateLineBeforeTax(
            BigDecimal importPriceBeforeVat,
            int quantity
    ) {
        return importPriceBeforeVat
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateLineTax(
            BigDecimal lineBeforeTax,
            BigDecimal vatRate
    ) {
        if (vatRate == null) {
            vatRate = BigDecimal.ZERO;
        }

        return lineBeforeTax
                .multiply(vatRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateLineAfterTax(
            BigDecimal lineBeforeTax,
            BigDecimal lineTax
    ) {
        return lineBeforeTax
                .add(lineTax)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal resolveVatRateByCategory(String categoryId) {
        if (categoryId == null) {
            return new BigDecimal("8");
        }

        return switch (categoryId.trim().toUpperCase()) {
            case "CAT001" ->
                new BigDecimal("8");
            case "CAT002" ->
                new BigDecimal("10");
            case "CAT003" ->
                new BigDecimal("10");
            case "CAT004" ->
                new BigDecimal("8");
            case "CAT005" ->
                new BigDecimal("5");
            default ->
                new BigDecimal("8");
        };
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return String.format("%,.0f", value);
    }

    private static String percent(BigDecimal value) {
        if (value == null) {
            return "0%";
        }

        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
