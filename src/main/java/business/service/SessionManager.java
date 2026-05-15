package business.service;

import model.account.Account;

public class SessionManager {

    private static String token;
    private static Account currentUser;
    private static String currentToken;
    private static String currentSessionId;

    private SessionManager() {
    }

    public static void startSession(Account user, String tk) {
        currentUser = user;
        token = tk;
    }

    public static void startSession(Account user, String token, String sessionId) {
        currentUser = user;
        currentToken = token;
        currentSessionId = sessionId;
    }

    public static Account getCurrentUser() {
        return currentUser;
    }

    public static String getToken() {
        return token;
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static boolean isLoggedIn() {
        return currentUser != null && token != null && !token.isBlank();
    }

    public static void clear() {
        currentUser = null;
        token = null;
    }
}
