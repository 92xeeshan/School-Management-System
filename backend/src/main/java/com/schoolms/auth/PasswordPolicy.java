package com.schoolms.auth;

import com.schoolms.common.exception.BusinessException;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || !password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)
                || password.chars().allMatch(ch -> Character.isLetterOrDigit(ch))) {
            throw new BusinessException("auth.password_weak");
        }
    }

    public static String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase();
    }
}
