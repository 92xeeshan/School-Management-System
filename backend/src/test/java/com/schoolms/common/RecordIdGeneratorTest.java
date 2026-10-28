package com.schoolms.common;

import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.staff.NonTeachingStaffRepository;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordIdGeneratorTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherProfileRepository teacherRepository;
    @Mock
    private NonTeachingStaffRepository nonTeachingStaffRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    private RecordIdGenerator generator;
    private final UUID schoolId = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        generator = new RecordIdGenerator(studentRepository, teacherRepository,
                nonTeachingStaffRepository, enrollmentRepository);
    }

    @Test
    void nextAdmissionNoStartsAt001() {
        String year = String.valueOf(Year.now().getValue());
        when(studentRepository.findAdmissionNosByPrefix(schoolId, "ADM" + year)).thenReturn(List.of());
        assertEquals("ADM" + year + "001", generator.nextAdmissionNo(schoolId));
    }

    @Test
    void nextAdmissionNoIncrementsHighestSequence() {
        String year = String.valueOf(Year.now().getValue());
        when(studentRepository.findAdmissionNosByPrefix(schoolId, "ADM" + year))
                .thenReturn(List.of("ADM" + year + "001", "ADM" + year + "012", "ADM" + year + "3"));
        assertEquals("ADM" + year + "013", generator.nextAdmissionNo(schoolId));
    }

    @Test
    void nextTeacherAndNonTeachingUseSeparatePrefixes() {
        String year = String.valueOf(Year.now().getValue());
        when(teacherRepository.findEmployeeNosByPrefix(schoolId, "EMP" + year))
                .thenReturn(List.of("EMP" + year + "004"));
        when(nonTeachingStaffRepository.findEmployeeNosByPrefix(schoolId, "NTS" + year))
                .thenReturn(List.of());
        assertEquals("EMP" + year + "005", generator.nextTeacherEmployeeNo(schoolId));
        assertEquals("NTS" + year + "001", generator.nextNonTeachingEmployeeNo(schoolId));
    }

    @Test
    void nextRollNumberIsMaxPlusOnePerSection() {
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        when(enrollmentRepository.findMaxRollNumber(sectionId, yearId)).thenReturn(2);
        assertEquals(3, generator.nextRollNumber(sectionId, yearId));
    }

    @Test
    void nextRollNumberStartsAtOneWhenEmpty() {
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        when(enrollmentRepository.findMaxRollNumber(sectionId, yearId)).thenReturn(null);
        assertEquals(1, generator.nextRollNumber(sectionId, yearId));
    }
}
