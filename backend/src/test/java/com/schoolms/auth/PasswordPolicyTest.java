package com.schoolms.auth;

import com.schoolms.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsMixedPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Alis@4323"));
    }

    @Test
    void rejectsShortOrSimplePasswords() {
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("Ab1@"));
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("alllower1@"));
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("ALLUPPER1@"));
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("NoDigits@@"));
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("NoSymbol12"));
    }

    @Test
    void normalizesAnswers() {
        assertEquals("delhi", PasswordPolicy.normalizeAnswer("  Delhi "));
    }
}
