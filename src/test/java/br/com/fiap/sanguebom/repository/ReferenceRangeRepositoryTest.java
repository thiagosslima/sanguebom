package br.com.fiap.sanguebom.repository;

import br.com.fiap.sanguebom.model.entities.ReferenceRange;
import br.com.fiap.sanguebom.model.enums.Sex;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Transactional
class ReferenceRangeRepositoryTest {

    @Autowired
    private ReferenceRangeRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migratedAllRangeAppliesToEveryPatientSex() {
        assertUniversalRange(Sex.ALL);
        assertThat(repository.findApplicableRangeForUserByExamItem(1L, Sex.M, 17L)).isEmpty();
    }

    @Test
    void migrationConvertsExistingNullSexToAll() {
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM reference_range WHERE sex IS NULL", Long.class)).isZero();
        assertThat(jdbc.queryForList("""
                SELECT rr.sex FROM reference_range rr
                JOIN exam_item ei ON ei.id = rr.exam_item_id
                WHERE ei.code IN ('GLUCOSE', 'HBA1C', 'INSULIN')
                """, String.class)).hasSize(3).containsOnly("ALL");
    }

    @Test
    void databaseRejectsNullSex() {
        assertThatThrownBy(() -> jdbc.update("UPDATE reference_range SET sex = NULL WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void timelineStillAcceptsUnknownPatientSex() {
        assertThat(repository.findApplicableRangeForTimeline(
                1L, null, BigDecimal.valueOf(40), LocalDate.of(2026, 9, 29)))
                .extracting(ReferenceRange::getId).containsExactly(1L);
    }

    @Test
    void specificRangeDoesNotApplyToAnotherSex() {
        jdbc.update("UPDATE reference_range SET sex = 'F' WHERE id = 1");
        assertThat(repository.findApplicableRangeForUserByExamItem(1L, Sex.F, 40L)).isPresent();
        assertThat(repository.findApplicableRangeForUserByExamItem(1L, Sex.M, 40L)).isEmpty();
        assertThat(repository.findApplicableRangeForTimeline(
                1L, Sex.M, BigDecimal.valueOf(40), LocalDate.of(2026, 9, 29))).isEmpty();
    }

    private void assertUniversalRange(Sex expected) {
        for (Sex patientSex : new Sex[]{Sex.M, Sex.F, Sex.MALE, Sex.FEMALE}) {
            assertThat(repository.findApplicableRangeForUserByExamItem(1L, patientSex, 40L))
                    .hasValueSatisfying(range -> assertThat(range.getSex()).isEqualTo(expected));
            assertThat(repository.findApplicableRangeForTimeline(
                    1L, patientSex, BigDecimal.valueOf(40), LocalDate.of(2026, 9, 29)))
                    .extracting(ReferenceRange::getId).containsExactly(1L);
        }
    }
}
