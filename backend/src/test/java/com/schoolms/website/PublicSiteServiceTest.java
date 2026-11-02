package com.schoolms.website;

import com.schoolms.TestSecurity;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.common.enums.EventVisibility;
import com.schoolms.common.enums.NoticeStatus;
import com.schoolms.common.enums.NoticeVisibility;
import com.schoolms.common.enums.SchoolStatus;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.event.SchoolEvent;
import com.schoolms.event.SchoolEventRepository;
import com.schoolms.notice.Notice;
import com.schoolms.notice.NoticeRepository;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.tenant.TenantBinder;
import com.schoolms.website.dto.PublicCopyUpdateRequest;
import com.schoolms.website.dto.PublicSiteAdminDto;
import com.schoolms.website.dto.PublicSiteDto;
import com.schoolms.website.dto.PublicSiteUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicSiteServiceTest {

    @Mock private SchoolRepository schoolRepository;
    @Mock private PublicSiteRepository siteRepository;
    @Mock private PublicSiteCopyRepository copyRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private SchoolEventRepository eventRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private TenantBinder tenantBinder;

    private PublicSiteService service;

    @BeforeEach
    void setUp() {
        service = new PublicSiteService(
                schoolRepository, siteRepository, copyRepository, noticeRepository,
                eventRepository, classRepository, tenantBinder);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("PUBLIC_SITE_MANAGE"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void publicSiteReturnsPublishedContent() {
        School school = school();
        when(schoolRepository.findByCode("DEMO")).thenReturn(Optional.of(school));
        PublicSite site = site(true);
        when(siteRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(site));
        PublicSiteCopy copy = new PublicSiteCopy();
        copy.setLocale("en");
        copy.setTagline("Quality education");
        copy.setAbout("About the school");
        when(copyRepository.findBySchoolIdAndLocale(TestSecurity.SCHOOL_ID, "en")).thenReturn(Optional.of(copy));
        Notice notice = new Notice();
        notice.setId(UUID.randomUUID());
        notice.setTitle("Holiday");
        notice.setBody("School closed");
        notice.setVisibilityScope(NoticeVisibility.SCHOOL_WIDE);
        notice.setStatus(NoticeStatus.PUBLISHED);
        notice.setPublishAt(Instant.now().minusSeconds(60));
        when(noticeRepository.findBySchoolIdAndStatusAndPublishAtLessThanEqualOrderByPublishAtDesc(
                eq(TestSecurity.SCHOOL_ID), eq(NoticeStatus.PUBLISHED), any(Instant.class)))
                .thenReturn(List.of(notice));
        SchoolEvent event = new SchoolEvent();
        event.setId(UUID.randomUUID());
        event.setTitle("Sports day");
        event.setVisibilityScope(EventVisibility.SCHOOL_WIDE);
        event.setStartDate(LocalDate.now().plusDays(3));
        when(eventRepository.findUpcoming(eq(TestSecurity.SCHOOL_ID), any(LocalDate.class)))
                .thenReturn(List.of(event));
        SchoolClass klass = new SchoolClass();
        klass.setId(UUID.randomUUID());
        klass.setName("Class 1");
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(klass));

        PublicSiteDto dto = service.publicSite("DEMO", "en");

        assertEquals("DEMO", dto.code());
        assertEquals("Quality education", dto.copy().tagline());
        assertEquals(1, dto.notices().size());
        assertEquals(1, dto.events().size());
        assertEquals(1, dto.classes().size());
        verify(tenantBinder).bind(TestSecurity.SCHOOL_ID);
    }

    @Test
    void unpublishedSiteIsNotFound() {
        when(schoolRepository.findByCode("DEMO")).thenReturn(Optional.of(school()));
        when(siteRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(site(false)));
        assertThrows(ResourceNotFoundException.class, () -> service.publicSite("DEMO", "en"));
    }

    @Test
    void unknownSchoolIsNotFound() {
        when(schoolRepository.findByCode("MISSING")).thenReturn(Optional.empty());
        when(schoolRepository.findByCode("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.publicSite("missing", "en"));
    }

    @Test
    void adminUpdateSavesCopy() {
        PublicSite site = site(false);
        when(siteRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(site));
        when(siteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(copyRepository.findBySchoolIdAndLocale(TestSecurity.SCHOOL_ID, "en")).thenReturn(Optional.empty());
        when(copyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PublicSiteCopy savedCopy = new PublicSiteCopy();
        savedCopy.setLocale("en");
        savedCopy.setTagline("Hello");
        when(copyRepository.findBySchoolIdOrderByLocaleAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(savedCopy));

        PublicSiteAdminDto dto = service.adminUpdate(new PublicSiteUpdateRequest(
                true, null, "Principal",
                List.of(new PublicCopyUpdateRequest("en", "Hello", "About", "Message", "Labs"))));

        assertTrue(dto.published());
        assertEquals("Hello", dto.copies().get(0).tagline());
    }

    private School school() {
        School school = new School();
        school.setId(TestSecurity.SCHOOL_ID);
        school.setCode("DEMO");
        school.setName("Demo Public School");
        school.setStatus(SchoolStatus.ACTIVE);
        school.setDefaultLocale("en");
        school.setAddress("1 Demo Road");
        school.setPhone("01100000000");
        school.setEmail("office@demo.school");
        return school;
    }

    private PublicSite site(boolean published) {
        PublicSite site = new PublicSite();
        site.setId(UUID.fromString("e1000000-0000-0000-0000-000000000001"));
        site.setSchoolId(TestSecurity.SCHOOL_ID);
        site.setPublished(published);
        site.setPrincipalName("Dr. Meera Joshi");
        return site;
    }
}
