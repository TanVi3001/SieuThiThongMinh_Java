package view;

import view.components.AdminDataPanel;

public class PromotionManagementPanel extends AdminDataPanel {
    public PromotionManagementPanel() {
        super("Quản lý khuyến mãi", "Quản trị chương trình và cấu hình khuyến mãi toàn hệ thống",
            new String[]{"Tổng chương trình","Đang diễn ra","Đã kết thúc","Tạm ngưng"},
            new String[]{"SELECT COUNT(*) FROM PROMOTION_CAMPAIGNS WHERE NVL(is_deleted,0)=0","SELECT COUNT(*) FROM PROMOTION_CAMPAIGNS WHERE NVL(is_deleted,0)=0 AND TRUNC(SYSDATE) BETWEEN NVL(start_date,TRUNC(SYSDATE)) AND NVL(end_date,TRUNC(SYSDATE))","SELECT COUNT(*) FROM PROMOTION_CAMPAIGNS WHERE NVL(is_deleted,0)=0 AND end_date < TRUNC(SYSDATE)","SELECT COUNT(*) FROM PROMOTIONS WHERE NVL(is_deleted,0)=0 AND UPPER(NVL(status,'ACTIVE')) <> 'ACTIVE'"},
            "Danh sách khuyến mãi",
            "SELECT p.promotion_id, p.promotion_name, c.campaign_name, NVL(p.status,'ACTIVE'), NVL(p.discount_amount,0) FROM PROMOTIONS p LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id=c.campaign_id WHERE NVL(p.is_deleted,0)=0 ORDER BY p.promotion_id",
            new String[]{"Mã KM","Tên khuyến mãi","Chiến dịch","Trạng thái","Giảm giá"}
        );
    }
}
