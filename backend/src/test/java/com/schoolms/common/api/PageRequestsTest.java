package com.schoolms.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageRequestsTest {

    @Test
    void clampsPageAndSize() {
        PageRequest pageable = PageRequests.of(-3, 500);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    void mapsAllowedSortAndDefaultsUnknownKeys() {
        PageRequest pageable = PageRequests.of(1, 25, "name", "desc", "username",
                Map.of("name", "firstName", "username", "username"));
        Sort.Order order = pageable.getSort().getOrderFor("firstName");
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void slicesInMemoryList() {
        PagedResponse<Integer> page = PageRequests.fromList(List.of(1, 2, 3, 4, 5), 1, 2);
        assertEquals(List.of(3, 4), page.content());
        assertEquals(5, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(1, page.page());
        assertEquals(2, page.size());
    }

    @Test
    void likePatternLowercasesQuery() {
        assertEquals("%ali%", PageRequests.likePattern(" Ali "));
        assertTrue(PageRequests.normalizeQuery("  ") == null);
    }
}
