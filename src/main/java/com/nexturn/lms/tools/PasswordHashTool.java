package com.nexturn.lms.tools;
import java.io.Console;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** Offline account provisioning utility; reads passwords without command-line or console echo. */
public class PasswordHashTool {
    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run from an interactive terminal");
        char[] password = console.readPassword("Password (12-72 ASCII characters): ");
        if (password == null || password.length < 12 || password.length > 72)
            throw new IllegalArgumentException("Use a password of 12 to 72 ASCII characters");
        String value = new String(password);
        Arrays.fill(password, '\0');
        if (!value.chars().allMatch(c -> c >= 32 && c <= 126)) throw new IllegalArgumentException("Use ASCII characters");
        console.printf("%s%n", new BCryptPasswordEncoder(12).encode(value));
    }
}
