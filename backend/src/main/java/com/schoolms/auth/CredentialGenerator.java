package com.schoolms.auth;

import com.schoolms.auth.dto.GeneratedCredentials;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CredentialGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";

    private final UserRepository userRepository;

    public GeneratedCredentials generate(UUID schoolId, String roleCode, String firstName) {
        String username = nextUsername(schoolId, roleCode);
        return new GeneratedCredentials(username, temporaryPassword(firstName));
    }

    public String temporaryPassword(String firstName) {
        String prefix = firstName == null ? "" : firstName.replaceAll("[^A-Za-z]", "");
        if (prefix.length() < 4) {
            prefix = (prefix + "User").substring(0, 4);
        } else {
            prefix = prefix.substring(0, 4);
        }
        prefix = prefix.substring(0, 1).toUpperCase(Locale.ROOT)
                + prefix.substring(1).toLowerCase(Locale.ROOT);
        int number = 1000 + RANDOM.nextInt(9000);
        return prefix + "@" + number;
    }

    public String nextUsername(UUID schoolId, String roleCode) {
        String prefix = switch (roleCode == null ? "" : roleCode.toUpperCase(Locale.ROOT)) {
            case "ADMIN", "SUPER_ADMIN" -> "ADM";
            case "TEACHER" -> "TCH";
            case "PARENT" -> "PRN";
            default -> "STD";
        };
        String year = String.valueOf(Year.now().getValue());
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + year + String.format("%03d", sequence);
            sequence++;
        } while (userRepository.existsByUsernameAndSchoolId(candidate, schoolId) && sequence < 1000);
        if (userRepository.existsByUsernameAndSchoolId(candidate, schoolId)) {
            candidate = prefix + year + randomSuffix();
            while (userRepository.existsByUsernameAndSchoolId(candidate, schoolId)) {
                candidate = prefix + year + randomSuffix();
            }
        }
        return candidate;
    }

    private String randomSuffix() {
        char a = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        char b = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        char c = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        return "" + a + b + c;
    }
}
