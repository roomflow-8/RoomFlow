package com.goorm.roomflow.domain.user.validator;

import java.util.regex.Pattern;

public final class UserValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern KOREAN_NAME_PATTERN =
            Pattern.compile("^[가-힣]{2,10}$");

    private static final Pattern ENGLISH_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\\s]{2,20}$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()])[a-zA-Z\\d!@#$%^&*()]{8,12}$");

    private UserValidator() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }

        String trimmedName = name.trim();
        return KOREAN_NAME_PATTERN.matcher(trimmedName).matches()
                || ENGLISH_NAME_PATTERN.matcher(trimmedName).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
}
