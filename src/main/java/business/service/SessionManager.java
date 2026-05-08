package business.service;

import model.account.Account;

public class SessionManager {

    private static String token;
    private static Account currentUser;

    private SessionManager() {
    }

    public static void startSession(Account user, String tk) {
        currentUser = user;
        token = tk;
    }

    public static Account getCurrentUser() {
        return currentUser;
    }

    public static String getToken() {
        return token;
    }

    public static boolean isLoggedIn() {
        return currentUser != null && token != null && !token.isBlank();
    }

    public static void clear() {
        currentUser = null;
        token = null;
    }
}
