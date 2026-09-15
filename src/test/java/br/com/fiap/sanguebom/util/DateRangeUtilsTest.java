package br.com.fiap.sanguebom.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeUtilsTest {

    @Test
    @DisplayName("startOfDayUtc retorna inicio do dia em UTC")
    void shouldReturnStartOfDayAtUtc() {
        assertThat(DateRangeUtils.startOfDayUtc(LocalDate.of(2026, 8, 15)))
                .isEqualTo(OffsetDateTime.of(2026, 8, 15, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("startOfNextDayUtc retorna inicio do dia seguinte em UTC")
    void shouldReturnStartOfNextDayAtUtc() {
        assertThat(DateRangeUtils.startOfNextDayUtc(LocalDate.of(2026, 8, 15)))
                .isEqualTo(OffsetDateTime.of(2026, 8, 16, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("metodos de data retornam null quando data nao for informada")
    void shouldReturnNullWhenDateIsNull() {
        assertThat(DateRangeUtils.startOfDayUtc(null)).isNull();
        assertThat(DateRangeUtils.startOfNextDayUtc(null)).isNull();
    }

    @Test
    @DisplayName("periodo invalido somente quando inicio for maior que fim")
    void shouldDetectInvalidPeriod() {
        assertThat(DateRangeUtils.isInvalidPeriod(LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(DateRangeUtils.isInvalidPeriod(LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 15))).isFalse();
        assertThat(DateRangeUtils.isInvalidPeriod(null, LocalDate.of(2026, 8, 15))).isFalse();
        assertThat(DateRangeUtils.isInvalidPeriod(LocalDate.of(2026, 8, 15), null)).isFalse();
    }
}
