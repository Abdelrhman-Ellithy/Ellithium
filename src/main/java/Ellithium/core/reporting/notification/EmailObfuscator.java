package Ellithium.core.reporting.notification;

/**
 * Utility class for obfuscating email addresses in logs and messages.
 * Provides a centralized way to mask sensitive email information.
 */
public final class EmailObfuscator {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private EmailObfuscator() {
    }
    
    /**
     * Obfuscates an email address for logging purposes.
     * Shows only first character, last character, and domain.
     * 
     * @param email The email address to obfuscate
     * @return Obfuscated email address (e.g., "a***@gmail.com")
     */
    public static String obfuscate(String email) {
        if (email == null || email.isEmpty()) {
            return "NULL/EMPTY";
        }
        
        if (!email.contains("@")) {
            return email;
        }
        
        int atIndex = email.indexOf("@");
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return email;
        }
        
        String obfuscatedLocal = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        return obfuscatedLocal + domain;
    }
}
