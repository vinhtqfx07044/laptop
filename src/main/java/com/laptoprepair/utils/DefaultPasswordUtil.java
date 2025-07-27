package com.laptoprepair.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

public class DefaultPasswordUtil {
    
    private DefaultPasswordUtil() {
    }
    
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    private static final Map<String, String> DEFAULT_PASSWORDS = new HashMap<>();
    
    static {
        DEFAULT_PASSWORDS.put("staff", "staff123");
        DEFAULT_PASSWORDS.put("admin", "admin123");
        DEFAULT_PASSWORDS.put("manager", "manager123");
    }
    
    public static Map<String, String> getAllEncodedPasswords() {
        Map<String, String> encodedPasswords = new HashMap<>();
        DEFAULT_PASSWORDS.forEach((role, rawPassword) -> 
            encodedPasswords.put(role, passwordEncoder.encode(rawPassword))
        );
        return encodedPasswords;
    }
    
    public static void main(String[] args) {
        System.out.println("Encoded passwords:");
        getAllEncodedPasswords().forEach((role, encoded) -> 
            System.out.println(role + ": " + encoded)
        );
    }
}