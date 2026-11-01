package com.schoolms.website;

import com.schoolms.TestSecurity;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.enums.SchoolStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.school.School;
import com.schoolms.website.dto.EnquiryDto;
import com.schoolms.website.dto.SubmitEnquiryRequest;
import com.schoolms.website.dto.UpdateEnquiryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnquiryServiceTest {

    private static final UUID CLASS_ID = UUID.fromString("30000000-0000-0000-0000-000000000010");
    private static final UUID ENQUIRY_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");

    @Mock private EnquiryRepository enquiryRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private PublicSiteService publicSiteService;
    @Mock private NotificationTriggerService notifications;

    private EnquiryService service;

    @BeforeEach
    void setUp() {
        service = new EnquiryService(enquiryRepository, classRepository, publicSiteService, notifications);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("ENQUIRY_READ", "ENQUIRY_MANAGE"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void submitCreatesEnquiryAndNotifies() {
        School school = school();
        when(publicSiteService.requireActiveSchool("DEMO")).thenReturn(school);
        SchoolClass klass = new SchoolClass();
        klass.setId(CLASS_ID);
        klass.setName("Class 1");
        when(classRepository.findByIdAndSchoolId(CLASS_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(klass));
        when(enquiryRepository.save(any())).thenAnswer(invocation -> {
            Enquiry saved = invocation.getArgument(0);
            saved.setId(ENQUIRY_ID);
            return saved;
        });

        EnquiryDto dto = service.submit("DEMO", new SubmitEnquiryRequest(
                "Anita Sharma", "9876543210", "anita@example.com", CLASS_ID, null, "Need admission"));

        assertEquals("Anita Sharma", dto.parentName());
        assertEquals("Class 1", dto.className());
        assertEquals("NEW", dto.status());
        verify(publicSiteService).bindPublishedSchool(school);
        verify(notifications).onEnquirySubmitted(
                eq(TestSecurity.SCHOOL_ID), eq("Anita Sharma"), eq("Class 1"), eq("9876543210"), eq(ENQUIRY_ID));
    }

    @Test
    void submitRejectsInvalidPhone() {
        when(publicSiteService.requireActiveSchool("DEMO")).thenReturn(school());
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.submit("DEMO", new SubmitEnquiryRequest("Anita", "abc", null, null, null, null)));
        assertEquals("enquiry.phone_invalid", ex.getCode());
    }

    @Test
    void listFiltersByStatus() {
        Enquiry row = new Enquiry();
        row.setId(ENQUIRY_ID);
        row.setParentName("Anita Sharma");
        row.setPhone("9876543210");
        row.setStatus(EnquiryStatus.NEW);
        when(enquiryRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(
                eq(TestSecurity.SCHOOL_ID), eq(EnquiryStatus.NEW), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        PagedResponse<EnquiryDto> page = service.list(EnquiryStatus.NEW, 0, 20);

        assertEquals(1, page.content().size());
        assertEquals("NEW", page.content().get(0).status());
    }

    @Test
    void updateMarksContacted() {
        Enquiry row = new Enquiry();
        row.setId(ENQUIRY_ID);
        row.setSchoolId(TestSecurity.SCHOOL_ID);
        row.setParentName("Anita Sharma");
        row.setPhone("9876543210");
        row.setStatus(EnquiryStatus.NEW);
        when(enquiryRepository.findByIdAndSchoolId(ENQUIRY_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(row));
        when(enquiryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EnquiryDto dto = service.update(ENQUIRY_ID, new UpdateEnquiryRequest("CONTACTED", "Called parent"));

        assertEquals("CONTACTED", dto.status());
        assertEquals("Called parent", dto.notes());
    }

    private School school() {
        School school = new School();
        school.setId(TestSecurity.SCHOOL_ID);
        school.setCode("DEMO");
        school.setName("Demo Public School");
        school.setStatus(SchoolStatus.ACTIVE);
        return school;
    }
}
