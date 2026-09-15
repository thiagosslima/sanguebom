package br.com.fiap.sanguebom.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateRangeUtils {

    private static final int NEXT_DAY_OFFSET = 1;

    private DateRangeUtils() {
    }

    public static OffsetDateTime startOfDayUtc(final LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    public static OffsetDateTime startOfNextDayUtc(final LocalDate date) {
        return date == null ? null : startOfDayUtc(date.plusDays(NEXT_DAY_OFFSET));
    }

    public static boolean isInvalidPeriod(final LocalDate from, final LocalDate to) {
        return from != null && to != null && from.isAfter(to);
    }
}
