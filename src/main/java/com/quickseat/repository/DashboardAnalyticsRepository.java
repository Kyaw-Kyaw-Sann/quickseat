package com.quickseat.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardAnalyticsRepository {
    private static final String VALID_REVENUE = "p.status = 'SUCCESS' AND b.status IN ('CONFIRMED', 'USED')";
    private final EntityManager entityManager;

    public SummaryRow getSummary(Filter filter, Instant todayStart, Instant tomorrowStart) {
        String sql = """
                SELECT COUNT(b.id),
                       COUNT(b.id) FILTER (WHERE b.status IN ('CONFIRMED', 'USED')),
                       COUNT(b.id) FILTER (WHERE b.created_at >= :todayStart AND b.created_at < :tomorrowStart),
                       COALESCE(SUM(p.amount) FILTER (WHERE %s), 0),
                       COUNT(b.id) FILTER (WHERE b.status = 'CANCELLED')
                FROM bookings b
                JOIN showtimes sh ON sh.id = b.showtime_id
                JOIN screens sc ON sc.id = sh.screen_id
                LEFT JOIN payments p ON p.booking_id = b.id
                WHERE 1 = 1
                """.formatted(VALID_REVENUE) + scope(filter, "sh", "sc");
        Query query = createQuery(sql, filter)
                .setParameter("todayStart", todayStart)
                .setParameter("tomorrowStart", tomorrowStart);
        Object[] row = (Object[]) query.getSingleResult();
        return new SummaryRow(asLong(row[0]), asLong(row[1]), asLong(row[2]), asDecimal(row[3]), asLong(row[4]));
    }

    public SeatTotalsRow getSeatTotals(Filter filter) {
        String sql = """
                SELECT
                    (SELECT COUNT(bs.id)
                     FROM booking_seats bs
                     JOIN bookings b ON b.id = bs.booking_id
                     JOIN showtimes sh ON sh.id = b.showtime_id
                     JOIN screens sc ON sc.id = sh.screen_id
                     WHERE b.status IN ('CONFIRMED', 'USED') AND sh.status <> 'CANCELLED'
                """ + scope(filter, "sh", "sc") + """
                    ),
                    (SELECT COUNT(ss.id)
                     FROM showtime_seats ss
                     JOIN showtimes sh ON sh.id = ss.showtime_id
                     JOIN screens sc ON sc.id = sh.screen_id
                     WHERE ss.status <> 'UNAVAILABLE' AND sh.status <> 'CANCELLED'
                """ + scope(filter, "sh", "sc") + ")";
        Object[] row = (Object[]) createQuery(sql, filter).getSingleResult();
        return new SeatTotalsRow(asLong(row[0]), asLong(row[1]));
    }

    public List<PerformanceRow> getMoviePerformance(Filter filter, int limit) {
        String sql = performanceCtes(filter, "movie_id") + """
                SELECT m.id, m.title,
                       COALESCE(bm.total_bookings, 0), COALESCE(bm.confirmed_bookings, 0),
                       COALESCE(bm.cancelled_bookings, 0), COALESCE(bm.revenue, 0),
                       COALESCE(bsm.booked_seats, 0), COALESCE(cm.capacity, 0)
                FROM movies m
                JOIN (SELECT DISTINCT movie_id FROM scoped_showtimes) scope_item ON scope_item.movie_id = m.id
                LEFT JOIN booking_metrics bm ON bm.group_id = m.id
                LEFT JOIN booked_seat_metrics bsm ON bsm.group_id = m.id
                LEFT JOIN capacity_metrics cm ON cm.group_id = m.id
                ORDER BY COALESCE(bm.confirmed_bookings, 0) DESC,
                         COALESCE(bm.total_bookings, 0) DESC, m.title
                """;
        return performanceRows(createQuery(sql, filter).setMaxResults(limit));
    }

    public List<PerformanceRow> getCinemaPerformance(Filter filter) {
        String sql = performanceCtes(filter, "cinema_id") + """
                SELECT c.id, c.name,
                       COALESCE(bm.total_bookings, 0), COALESCE(bm.confirmed_bookings, 0),
                       COALESCE(bm.cancelled_bookings, 0), COALESCE(bm.revenue, 0),
                       COALESCE(bsm.booked_seats, 0), COALESCE(cm.capacity, 0)
                FROM cinemas c
                JOIN (SELECT DISTINCT cinema_id FROM scoped_showtimes) scope_item ON scope_item.cinema_id = c.id
                LEFT JOIN booking_metrics bm ON bm.group_id = c.id
                LEFT JOIN booked_seat_metrics bsm ON bsm.group_id = c.id
                LEFT JOIN capacity_metrics cm ON cm.group_id = c.id
                ORDER BY COALESCE(bm.revenue, 0) DESC, c.name
                """;
        return performanceRows(createQuery(sql, filter));
    }

    public List<TrendRow> getRevenueTrend(Filter filter, boolean monthly) {
        String unit = monthly ? "month" : "day";
        String sql = """
                SELECT CAST(date_trunc('%s', timezone('Asia/Yangon', sh.start_time)) AS date),
                       COALESCE(SUM(p.amount), 0), COUNT(b.id)
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                JOIN showtimes sh ON sh.id = b.showtime_id
                JOIN screens sc ON sc.id = sh.screen_id
                WHERE p.status = 'SUCCESS' AND b.status IN ('CONFIRMED', 'USED')
                """.formatted(unit) + scope(filter, "sh", "sc") + """
                GROUP BY 1
                ORDER BY 1
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = createQuery(sql, filter).getResultList();
        return rows.stream().map(row -> new TrendRow(asLocalDate(row[0]), asDecimal(row[1]), asLong(row[2]))).toList();
    }

    private String performanceCtes(Filter filter, String groupColumn) {
        String scopedWhere = scope(filter, "sh", "sc");
        return """
                WITH scoped_showtimes AS (
                    SELECT sh.id, sh.movie_id, sc.cinema_id, sh.status
                    FROM showtimes sh
                    JOIN screens sc ON sc.id = sh.screen_id
                    WHERE 1 = 1
                """ + scopedWhere + """
                ), booking_metrics AS (
                    SELECT ss.%s AS group_id,
                           COUNT(b.id) AS total_bookings,
                           COUNT(b.id) FILTER (WHERE b.status IN ('CONFIRMED', 'USED')) AS confirmed_bookings,
                           COUNT(b.id) FILTER (WHERE b.status = 'CANCELLED') AS cancelled_bookings,
                           COALESCE(SUM(p.amount) FILTER (WHERE p.status = 'SUCCESS'
                               AND b.status IN ('CONFIRMED', 'USED')), 0) AS revenue
                    FROM scoped_showtimes ss
                    JOIN bookings b ON b.showtime_id = ss.id
                    LEFT JOIN payments p ON p.booking_id = b.id
                    GROUP BY ss.%s
                ), booked_seat_metrics AS (
                    SELECT ss.%s AS group_id, COUNT(bs.id) AS booked_seats
                    FROM scoped_showtimes ss
                    JOIN bookings b ON b.showtime_id = ss.id
                    JOIN booking_seats bs ON bs.booking_id = b.id
                    WHERE b.status IN ('CONFIRMED', 'USED') AND ss.status <> 'CANCELLED'
                    GROUP BY ss.%s
                ), capacity_metrics AS (
                    SELECT ss.%s AS group_id, COUNT(sts.id) AS capacity
                    FROM scoped_showtimes ss
                    JOIN showtime_seats sts ON sts.showtime_id = ss.id
                    WHERE sts.status <> 'UNAVAILABLE' AND ss.status <> 'CANCELLED'
                    GROUP BY ss.%s
                )
                """.formatted(groupColumn, groupColumn, groupColumn, groupColumn, groupColumn, groupColumn);
    }

    private List<PerformanceRow> performanceRows(Query query) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new PerformanceRow(asLong(row[0]), String.valueOf(row[1]),
                asLong(row[2]), asLong(row[3]), asLong(row[4]), asDecimal(row[5]),
                asLong(row[6]), asLong(row[7]))).toList();
    }

    private Query createQuery(String sql, Filter filter) {
        Query query = entityManager.createNativeQuery(sql);
        if (filter.fromTime() != null) query.setParameter("fromTime", filter.fromTime());
        if (filter.toTimeExclusive() != null) query.setParameter("toTime", filter.toTimeExclusive());
        if (filter.cinemaId() != null) query.setParameter("cinemaId", filter.cinemaId());
        if (filter.movieId() != null) query.setParameter("movieId", filter.movieId());
        return query;
    }

    private String scope(Filter filter, String showtimeAlias, String screenAlias) {
        StringBuilder sql = new StringBuilder();
        if (filter.fromTime() != null) sql.append(" AND ").append(showtimeAlias).append(".start_time >= :fromTime");
        if (filter.toTimeExclusive() != null) sql.append(" AND ").append(showtimeAlias).append(".start_time < :toTime");
        if (filter.cinemaId() != null) sql.append(" AND ").append(screenAlias).append(".cinema_id = :cinemaId");
        if (filter.movieId() != null) sql.append(" AND ").append(showtimeAlias).append(".movie_id = :movieId");
        return sql.toString();
    }

    private static long asLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal asDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private static LocalDate asLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    public record Filter(Instant fromTime, Instant toTimeExclusive, Long cinemaId, Long movieId) {}
    public record SummaryRow(long totalBookings, long confirmedBookings, long todayBookings,
                             BigDecimal revenue, long cancelledBookings) {}
    public record SeatTotalsRow(long bookedSeatUnits, long capacitySeatUnits) {}
    public record PerformanceRow(long id, String name, long totalBookings, long confirmedBookings,
                                 long cancelledBookings, BigDecimal revenue,
                                 long bookedSeatUnits, long capacitySeatUnits) {}
    public record TrendRow(LocalDate periodStart, BigDecimal revenue, long confirmedBookings) {}
}
