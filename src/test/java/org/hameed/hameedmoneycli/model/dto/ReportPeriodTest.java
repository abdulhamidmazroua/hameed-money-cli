package org.hameed.hameedmoneycli.model.dto;

import org.hameed.hameedmoneycli.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReportPeriodTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);

    @Test
    void defaultsToThisMonth() {
        ReportPeriod p = ReportPeriod.resolve(null, null, null, TODAY);
        assertEquals(LocalDate.of(2026, 11, 1), p.start());
        assertEquals(LocalDate.of(2026, 11, 30), p.end());
    }

    @Test
    void lastMonth() {
        ReportPeriod p = ReportPeriod.resolve("last-month", null, null, TODAY);
        assertEquals(LocalDate.of(2026, 10, 1), p.start());
        assertEquals(LocalDate.of(2026, 10, 31), p.end());
    }

    @Test
    void lastMonthAcrossYearBoundary() {
        ReportPeriod p = ReportPeriod.resolve("last-month", null, null, LocalDate.of(2026, 1, 15));
        assertEquals(LocalDate.of(2025, 12, 1), p.start());
        assertEquals(LocalDate.of(2025, 12, 31), p.end());
    }

    @Test
    void thisYearAndLastYear() {
        ReportPeriod thisYear = ReportPeriod.resolve("this-year", null, null, TODAY);
        assertEquals(LocalDate.of(2026, 1, 1), thisYear.start());
        assertEquals(LocalDate.of(2026, 12, 31), thisYear.end());

        ReportPeriod lastYear = ReportPeriod.resolve("LAST-YEAR", null, null, TODAY);
        assertEquals(LocalDate.of(2025, 1, 1), lastYear.start());
        assertEquals(LocalDate.of(2025, 12, 31), lastYear.end());
    }

    @Test
    void allIsUnbounded() {
        ReportPeriod p = ReportPeriod.resolve("all", null, null, TODAY);
        assertNull(p.start());
        assertNull(p.end());
        assertEquals(Long.MIN_VALUE, p.startMillis());
        assertEquals(Long.MAX_VALUE, p.endExclusiveMillis());
        assertEquals("All time", p.describe());
    }

    @Test
    void customRangeAcceptsBothFormats() {
        ReportPeriod app = ReportPeriod.resolve(null, "01-01-2026", "31-03-2026", TODAY);
        ReportPeriod iso = ReportPeriod.resolve(null, "2026-01-01", "2026-03-31", TODAY);
        assertEquals(LocalDate.of(2026, 1, 1), app.start());
        assertEquals(LocalDate.of(2026, 3, 31), app.end());
        assertEquals(app.start(), iso.start());
        assertEquals(app.end(), iso.end());
    }

    @Test
    void onlyStartRunsThroughToday() {
        ReportPeriod p = ReportPeriod.resolve(null, "01-06-2026", null, TODAY);
        assertEquals(LocalDate.of(2026, 6, 1), p.start());
        assertEquals(TODAY, p.end());
    }

    @Test
    void onlyEndIsOpenAtStart() {
        ReportPeriod p = ReportPeriod.resolve(null, null, "31-03-2026", TODAY);
        assertNull(p.start());
        assertEquals(LocalDate.of(2026, 3, 31), p.end());
        assertEquals(Long.MIN_VALUE, p.startMillis());
    }

    @Test
    void endIsInclusiveWholeDay() {
        ReportPeriod p = ReportPeriod.resolve(null, "01-03-2026", "31-03-2026", TODAY);
        long expected = LocalDate.of(2026, 4, 1).atStartOfDay(DateUtil.zone()).toInstant().toEpochMilli();
        assertEquals(expected, p.endExclusiveMillis());
    }

    @Test
    void rejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.resolve(null, "31-03-2026", "01-01-2026", TODAY));
    }

    @Test
    void rejectsBadDate() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.resolve(null, "foo", null, TODAY));
        assertTrue(e.getMessage().contains("dd-MM-yyyy"));
    }

    @Test
    void rejectsUnknownPreset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.resolve("next-week", null, null, TODAY));
        assertTrue(e.getMessage().contains("this-month"));
    }

    @Test
    void rejectsPresetCombinedWithDates() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.resolve("last-month", "01-01-2026", null, TODAY));
    }

    @Test
    void isPreset() {
        assertTrue(ReportPeriod.isPreset("this-month"));
        assertTrue(ReportPeriod.isPreset("All"));
        assertFalse(ReportPeriod.isPreset("EGP"));
        assertFalse(ReportPeriod.isPreset(null));
    }
}
