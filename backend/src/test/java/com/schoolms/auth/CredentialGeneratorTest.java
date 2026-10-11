package com.schoolms.auth;

import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialGeneratorTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void generatesRolePrefixedUsernameAndTempPassword() {
        when(userRepository.existsByUsernameAndSchoolId(any(), isNull())).thenReturn(false);
        CredentialGenerator generator = new CredentialGenerator(userRepository);
        UUID schoolId = null;
        String username = generator.nextUsername(schoolId, "TEACHER");
        assertTrue(username.startsWith("TCH" + Year.now().getValue()));
        String password = generator.temporaryPassword("Mohammed");
        assertTrue(password.startsWith("Moha@"));
        assertTrue(password.matches("Moha@\\d{4}"));
    }
}
