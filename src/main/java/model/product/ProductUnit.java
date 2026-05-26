package model.product;

import java.math.BigDecimal;

public class ProductUnit {

    private String productId;
    private String unitId;
    private String unitName;
    private BigDecimal conversionRateToBase;
    private BigDecimal sellingPrice;
    private int isBaseUnit;
    private int isDeleted;

    public ProductUnit() {
    }

    public ProductUnit(String productId, String unitId, BigDecimal conversionRateToBase,
            int isBaseUnit, int isDeleted) {
        this.productId = productId;
        this.unitId = unitId;
        this.conversionRateToBase = conversionRateToBase;
        this.isBaseUnit = isBaseUnit;
        this.isDeleted = isDeleted;
    }

    public ProductUnit(String productId, String unitId, String unitName,
            BigDecimal conversionRateToBase, BigDecimal sellingPrice,
            int isBaseUnit, int isDeleted) {
        this.productId = productId;
        this.unitId = unitId;
        this.unitName = unitName;
        this.conversionRateToBase = conversionRateToBase;
        this.sellingPrice = sellingPrice;
        this.isBaseUnit = isBaseUnit;
        this.isDeleted = isDeleted;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public BigDecimal getConversionRateToBase() {
        return conversionRateToBase;
    }

    public void setConversionRateToBase(BigDecimal conversionRateToBase) {
        this.conversionRateToBase = conversionRateToBase;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getIsBaseUnit() {
        return isBaseUnit;
    }

    public void setIsBaseUnit(int isBaseUnit) {
        this.isBaseUnit = isBaseUnit;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        if (unitName != null && !unitName.isBlank()) {
            return unitName;
        }
        return unitId != null ? unitId : "";
    }
}
