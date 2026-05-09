package common.exception;

import java.util.Map;

public class ConcurrentCheckoutException extends Exception {

    private final Map<String, Integer> failedProducts; // Lưu danh sách Mã SP và Số tồn kho thực tế

    public ConcurrentCheckoutException(Map<String, Integer> failedProducts) {
        super("Tồn kho không đủ để thực hiện giao dịch!");
        this.failedProducts = failedProducts;
    }

    public Map<String, Integer> getFailedProducts() {
        return failedProducts;
    }
}
