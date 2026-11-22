package org.hameed.hameedmoneycli.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void parsesAppDefaultFormat() {
        assertEquals(LocalDate.of(2026, 10, 1), DateUtil.parseUserDate("01-10-2026"));
    }

    @Test
    void parsesIsoFormat() {
        assertEquals(LocalDate.of(2026, 10, 1), DateUtil.parseUserDate(" 2026-10-01 "));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> DateUtil.parseUserDate("2026/10/01"));
        assertThrows(IllegalArgumentException.class, () -> DateUtil.parseUserDate("32-01-2026"));
        assertThrows(IllegalArgumentException.class, () -> DateUtil.parseUserDate(""));
    }
}
