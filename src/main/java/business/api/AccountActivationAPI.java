package business.api;

import business.service.AccountActivationService;
import model.account.ActivationEmployeeInfo;

public class AccountActivationAPI {

    private final AccountActivationService service = new AccountActivationService();

    public ActivationEmployeeInfo check(String code) throws Exception {
        ActivationEmployeeInfo info = service.checkAndFetchActivation(code != null ? code.trim() : "");
        if (info == null) {
            throw new Exception("Mã kích hoạt không hợp lệ, đã được sử dụng hoặc hết hạn.");
        }
        return info;
    }

    public void activate(String code, String username, String passwordPlain) throws Exception {
        service.activateAccount(code, username, passwordPlain);
    }
}
