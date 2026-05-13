/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package common.exception;

import java.util.Map;

/**
 * Exception tùy chỉnh được ném ra khi số lượng tồn kho trong Database bị thay
 * đổi (không đủ hàng) ngay trong lúc đang tiến hành thanh toán.
 *
 * * @author Le Tan Vi
 */
public class InventoryChangedException extends Exception {

    // Danh sách các sản phẩm bị lỗi tồn kho
    // Key: Mã sản phẩm (product_id)
    // Value: Số lượng tồn kho thực tế còn lại trong kho
    private Map<String, Integer> failedProducts;

    /**
     * Khởi tạo Exception với câu báo lỗi và danh sách sản phẩm hụt kho.
     *
     * * @param message Câu thông báo lỗi
     * @param failedProducts Map chứa Mã SP và Số tồn kho thực tế
     */
    public InventoryChangedException(String message, Map<String, Integer> failedProducts) {
        super(message);
        this.failedProducts = failedProducts;
    }

    /**
     * Lấy danh sách sản phẩm bị lỗi để giao diện (SellPanel) tự động cập nhật
     * lại giỏ hàng.
     */
    public Map<String, Integer> getFailedProducts() {
        return failedProducts;
    }

    public void setFailedProducts(Map<String, Integer> failedProducts) {
        this.failedProducts = failedProducts;
    }
}
