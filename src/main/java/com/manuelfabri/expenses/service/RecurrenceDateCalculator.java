package com.manuelfabri.expenses.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.manuelfabri.expenses.model.RecurrentTransaction;

/**
 * Pure due-date math for {@link RecurrentTransaction}. Has no repository dependencies so it can be unit tested
 * without a Spring context or a database.
 */
@Component
public class RecurrenceDateCalculator {

  /**
   * Every date the recurrence should fire on, strictly after the last generated occurrence (or the recurrence's
   * start date if it never generated one) through {@code asOf} inclusive, bounded by the recurrence's end date.
   * Used by the scheduled generator to backfill any occurrences missed while the job wasn't running.
   */
  public List<LocalDate> occurrencesToGenerate(RecurrentTransaction recurrence, LocalDate asOf) {
    List<LocalDate> occurrences = new ArrayList<>();
    LocalDate endCap = recurrence.getEndDate() != null ? recurrence.getEndDate().toLocalDate() : null;
    LocalDate next = firstOccurrenceAfter(recurrence, anchorDate(recurrence));

    while (!next.isAfter(asOf) && (endCap == null || !next.isAfter(endCap))) {
      occurrences.add(next);
      next = firstOccurrenceAfter(recurrence, next);
    }

    return occurrences;
  }

  /**
   * The next occurrence in the recurrence's sequence, regardless of whether it has already come due. Empty if the
   * recurrence's end date has already passed. Used to populate the management UI's "next occurrence" display.
   */
  public Optional<LocalDate> nextDueDate(RecurrentTransaction recurrence) {
    LocalDate next = firstOccurrenceAfter(recurrence, anchorDate(recurrence));
    if (recurrence.getEndDate() != null && next.isAfter(recurrence.getEndDate().toLocalDate())) {
      return Optional.empty();
    }
    return Optional.of(next);
  }

  private LocalDate anchorDate(RecurrentTransaction recurrence) {
    return recurrence.getLastGeneratedDate() != null ? recurrence.getLastGeneratedDate().toLocalDate()
        : recurrence.getStartDate().toLocalDate().minusDays(1);
  }

  private LocalDate firstOccurrenceAfter(RecurrentTransaction recurrence, LocalDate afterExclusive) {
    return switch (recurrence.getFrequency()) {
      case INTERVAL_DAYS -> firstIntervalOccurrenceAfter(recurrence, afterExclusive);
      case MONTHLY_DAYS -> firstMonthlyOccurrenceAfter(recurrence, afterExclusive);
    };
  }

  private LocalDate firstIntervalOccurrenceAfter(RecurrentTransaction recurrence, LocalDate afterExclusive) {
    LocalDate start = recurrence.getStartDate().toLocalDate();
    if (afterExclusive.isBefore(start)) {
      return start;
    }
    int interval = recurrence.getIntervalDays();
    long daysSinceStart = ChronoUnit.DAYS.between(start, afterExclusive);
    long steps = daysSinceStart / interval + 1;
    return start.plusDays(steps * interval);
  }

  private LocalDate firstMonthlyOccurrenceAfter(RecurrentTransaction recurrence, LocalDate afterExclusive) {
    LocalDate start = recurrence.getStartDate().toLocalDate();
    YearMonth month = YearMonth.from(afterExclusive.isBefore(start) ? start : afterExclusive);

    while (true) {
      YearMonth currentMonth = month;
      Optional<LocalDate> candidate = recurrence.getDaysOfMonth().stream()
          .map(day -> currentMonth.atDay(Math.min(day, currentMonth.lengthOfMonth())))
          .filter(date -> date.isAfter(afterExclusive) && !date.isBefore(start))
          .min(Comparator.naturalOrder());
      if (candidate.isPresent()) {
        return candidate.get();
      }
      month = month.plusMonths(1);
    }
  }
}
