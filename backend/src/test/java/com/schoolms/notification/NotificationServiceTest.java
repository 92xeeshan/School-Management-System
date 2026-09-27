package com.schoolms.notification;

import com.schoolms.TestSecurity;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.notification.dto.NotificationDto;
import com.schoolms.notification.dto.UnreadCountDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    private NotificationService service;
    private final UUID otherUser = UUID.fromString("20000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void listReturnsOnlyCurrentUserInbox() {
        Notification row = row(TestSecurity.USER_ID, false);
        when(notificationRepository.findBySchoolIdAndRecipientIdOrderByCreatedAtDesc(
                eq(TestSecurity.SCHOOL_ID), eq(TestSecurity.USER_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        PagedResponse<NotificationDto> page = service.list(null, 0, 20);

        assertEquals(1, page.content().size());
        assertEquals(row.getId(), page.content().get(0).id());
        assertFalse(page.content().get(0).read());
    }

    @Test
    void unreadCountMatchesRepository() {
        when(notificationRepository.countBySchoolIdAndRecipientIdAndReadFalse(
                TestSecurity.SCHOOL_ID, TestSecurity.USER_ID)).thenReturn(4L);

        UnreadCountDto dto = service.unreadCount();

        assertEquals(4L, dto.unreadCount());
    }

    @Test
    void markReadUpdatesUnreadItem() {
        Notification row = row(TestSecurity.USER_ID, false);
        when(notificationRepository.findByIdAndSchoolIdAndRecipientId(
                row.getId(), TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(row));
        when(notificationRepository.save(row)).thenReturn(row);

        NotificationDto dto = service.markRead(row.getId());

        assertTrue(dto.read());
        assertTrue(row.isRead());
        verify(notificationRepository).save(row);
    }

    @Test
    void markReadDoesNotTouchAnotherUsersNotification() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndSchoolIdAndRecipientId(
                id, TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markRead(id));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllReadClearsInbox() {
        Notification first = row(TestSecurity.USER_ID, false);
        Notification second = row(TestSecurity.USER_ID, false);
        when(notificationRepository.findBySchoolIdAndRecipientIdAndReadFalse(
                TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(List.of(first, second));

        UnreadCountDto dto = service.markAllRead();

        assertEquals(0L, dto.unreadCount());
        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(notificationRepository).saveAll(List.of(first, second));
    }

    @Test
    void notifyUsersSkipsDuplicateSourceKey() {
        UUID recipient = TestSecurity.USER_ID;
        when(notificationRepository.existsBySchoolIdAndRecipientIdAndSourceKey(
                TestSecurity.SCHOOL_ID, recipient, "FEE:DUE:1")).thenReturn(true);

        service.notifyUsers(TestSecurity.SCHOOL_ID, List.of(recipient), "Fee due", "due", "FEE", "/fees", "FEE:DUE:1");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyUsersWritesOneRowPerRecipient() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyUsers(TestSecurity.SCHOOL_ID, List.of(TestSecurity.USER_ID, otherUser, TestSecurity.USER_ID),
                "Marks submitted for review", "Class 7-A marks submitted by Asha Sharma for review.",
                "EXAM", "/downloads/marksheet", null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(TestSecurity.USER_ID, captor.getAllValues().get(0).getRecipientId());
        assertEquals(otherUser, captor.getAllValues().get(1).getRecipientId());
        assertEquals("EXAM", captor.getAllValues().get(0).getCategory());
    }

    @Test
    void classLabelFormatsSection() {
        assertEquals("Class 7-A", NotificationTriggerService.classLabel("Class 7", "A"));
        assertEquals("Class 10-A", NotificationTriggerService.classLabel("10", "A"));
        assertEquals("Mid-Term Examinations", NotificationTriggerService.termLabel("MIDTERM"));
    }

    private Notification row(UUID recipientId, boolean read) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setSchoolId(TestSecurity.SCHOOL_ID);
        notification.setRecipientId(recipientId);
        notification.setTitle("Marks submitted for review");
        notification.setMessage("Class 7-A marks submitted by Asha Sharma for review.");
        notification.setCategory("EXAM");
        notification.setActionUrl("/downloads/marksheet");
        notification.setRead(read);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
