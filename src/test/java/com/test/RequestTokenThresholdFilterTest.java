package com.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestTokenThresholdFilterTest {

    private static final String TOKEN_1 = "TOKEN1";
    private static final String TOKEN_2 = "TOKEN2";

    @Test
    public void shouldReturnFailWhenThresholdCountBreaks() {
        ClockService clockService = mock(ClockService.class);
        when(clockService.currentTimeMillis()).thenReturn(0L, 1L, 2L, 3L, 4L, 5L);
        RequestTokenThresholdFilter requestTokenThresholdFilter = new RequestTokenThresholdFilter(2, 1, clockService);

        assertTrue(requestTokenThresholdFilter.filter(TOKEN_1));
        assertTrue(requestTokenThresholdFilter.filter(TOKEN_1));
        assertFalse(requestTokenThresholdFilter.filter(TOKEN_1));
    }

    @Test
    public void shouldRemoveOldRecordsAndCleanMap() {
        ClockService clockService = mock(ClockService.class);
        when(clockService.currentTimeMillis()).thenReturn(0L, 10000L);
        RequestTokenThresholdFilter requestTokenThresholdFilter = new RequestTokenThresholdFilter(2, 1, clockService);

        requestTokenThresholdFilter.filter(TOKEN_1);
        requestTokenThresholdFilter.filter(TOKEN_2);

        assertNull(requestTokenThresholdFilter.tokensMap.get(TOKEN_1));
        assertEquals(1, requestTokenThresholdFilter.inputDeque.size());
        assertNotNull(requestTokenThresholdFilter.inputDeque.getFirst());
        assertEquals(TOKEN_2, requestTokenThresholdFilter.inputDeque.getFirst().getToken());
    }

}