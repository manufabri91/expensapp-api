package com.manuelfabri.expenses.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.RecurrentTransaction;

class RecurrenceDateCalculatorTest {

  private final RecurrenceDateCalculator calculator = new RecurrenceDateCalculator();

  private static OffsetDateTime atUtc(LocalDate date) {
    return date.atStartOfDay().atOffset(ZoneOffset.UTC);
  }

  private static RecurrentTransaction intervalRecurrence(LocalDate startDate, int intervalDays) {
    RecurrentTransaction recurrence = new RecurrentTransaction();
    recurrence.setFrequency(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    recurrence.setStartDate(atUtc(startDate));
    recurrence.setIntervalDays(intervalDays);
    return recurrence;
  }

  private static RecurrentTransaction monthlyRecurrence(LocalDate startDate, Integer... daysOfMonth) {
    RecurrentTransaction recurrence = new RecurrentTransaction();
    recurrence.setFrequency(RecurrenceFrequencyEnum.MONTHLY_DAYS);
    recurrence.setStartDate(atUtc(startDate));
    recurrence.setDaysOfMonth(Set.of(daysOfMonth));
    return recurrence;
  }

  @Test
  void intervalStepping_crossesMonthBoundary() {
    RecurrentTransaction recurrence = intervalRecurrence(LocalDate.of(2024, 1, 25), 10);

    List<LocalDate> occurrences = calculator.occurrencesToGenerate(recurrence, LocalDate.of(2024, 2, 20));

    assertThat(occurrences).containsExactly(LocalDate.of(2024, 1, 25), LocalDate.of(2024, 2, 4),
        LocalDate.of(2024, 2, 14));
  }

  @Test
  void monthlyDay31_clampsToLastDayOfShortMonth_nonLeapYear() {
    RecurrentTransaction recurrence = monthlyRecurrence(LocalDate.of(2023, 1, 31), 31);

    List<LocalDate> occurrences = calculator.occurrencesToGenerate(recurrence, LocalDate.of(2023, 3, 31));

    assertThat(occurrences).containsExactly(LocalDate.of(2023, 1, 31), LocalDate.of(2023, 2, 28),
        LocalDate.of(2023, 3, 31));
  }

  @Test
  void monthlyDay31_clampsToFeb29_onLeapYear() {
    RecurrentTransaction recurrence = monthlyRecurrence(LocalDate.of(2024, 1, 31), 31);

    List<LocalDate> occurrences = calculator.occurrencesToGenerate(recurrence, LocalDate.of(2024, 3, 31));

    assertThat(occurrences).containsExactly(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29),
        LocalDate.of(2024, 3, 31));
  }

  @Test
  void monthlyMultipleDays_generatesEachConfiguredDay() {
    RecurrentTransaction recurrence = monthlyRecurrence(LocalDate.of(2024, 1, 1), 1, 15);

    List<LocalDate> occurrences = calculator.occurrencesToGenerate(recurrence, LocalDate.of(2024, 2, 16));

    assertThat(occurrences).containsExactly(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15),
        LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 15));
  }

  @Test
  void endDate_cutsOffOccurrencesEvenWhenAsOfIsLater() {
    RecurrentTransaction recurrence = intervalRecurrence(LocalDate.of(2024, 1, 1), 7);
    recurrence.setEndDate(atUtc(LocalDate.of(2024, 1, 20)));

    List<LocalDate> occurrences = calculator.occurrencesToGenerate(recurrence, LocalDate.of(2024, 2, 1));

    assertThat(occurrences).containsExactly(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8),
        LocalDate.of(2024, 1, 15));
  }

  @Test
  void backfillsMultipleMissedOccurrencesInOneCall() {
    RecurrentTransaction recurrence = intervalRecurrence(LocalDate.of(2024, 1, 1), 1);
    recurrence.setLastGeneratedDate(atUtc(LocalDate.of(2024, 1, 10)));

    List<LocalDate> occurrences = calculator.occurrencesToGenerate(recurrence, LocalDate.of(2024, 1, 14));

    assertThat(occurrences).containsExactly(LocalDate.of(2024, 1, 11), LocalDate.of(2024, 1, 12),
        LocalDate.of(2024, 1, 13), LocalDate.of(2024, 1, 14));
  }

  @Test
  void nextDueDate_returnsFollowingOccurrenceAfterLastGenerated() {
    RecurrentTransaction recurrence = intervalRecurrence(LocalDate.of(2024, 1, 1), 30);
    recurrence.setLastGeneratedDate(atUtc(LocalDate.of(2024, 1, 1)));

    Optional<LocalDate> nextDueDate = calculator.nextDueDate(recurrence);

    assertThat(nextDueDate).contains(LocalDate.of(2024, 1, 31));
  }

  @Test
  void nextDueDate_isEmptyOncePastEndDate() {
    RecurrentTransaction recurrence = intervalRecurrence(LocalDate.of(2024, 1, 1), 30);
    recurrence.setEndDate(atUtc(LocalDate.of(2024, 1, 15)));
    recurrence.setLastGeneratedDate(atUtc(LocalDate.of(2024, 1, 1)));

    Optional<LocalDate> nextDueDate = calculator.nextDueDate(recurrence);

    assertThat(nextDueDate).isEmpty();
  }
}
