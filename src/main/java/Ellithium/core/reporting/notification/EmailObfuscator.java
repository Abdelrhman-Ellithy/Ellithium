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
     * Obfuscates one or more comma-separated email addresses for logging purposes.
     * Each address is masked independently, showing only its first character, last
     * character, and domain (e.g., "a***@gmail.com").
     *
     * @param emails One email address, or several separated by commas (as accepted by the
     *               {@code notification.email.to} / {@code notification.email.cc} properties)
     * @return The same list with every address obfuscated
     */
    static String obfuscate(String emails) {
        if (emails == null || emails.isEmpty()) {
            return "NULL/EMPTY";
        }

        String[] parts = emails.split(",");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(obfuscateSingle(parts[i].trim()));
        }
        return result.toString();
    }

    private static String obfuscateSingle(String email) {
        if (email.isEmpty()) {
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
