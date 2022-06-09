package io.kyligence.kap.gateway.utils;

import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

	private static final ImmutableMap<String, TimeUnit> timeSuffixes = ImmutableMap.<String, TimeUnit>builder()
			.put("us", TimeUnit.MICROSECONDS).put("ms", TimeUnit.MILLISECONDS).put("s", TimeUnit.SECONDS)
			.put("m", TimeUnit.MINUTES).put("min", TimeUnit.MINUTES).put("h", TimeUnit.HOURS).put("d", TimeUnit.DAYS)
			.build();
	private static final long SECOND_TO_MILLI = 1000L;
	private static final long ONE_MINUTE_TS = 60 * 1000L;
	private static final long ONE_HOUR_TS = 60 * ONE_MINUTE_TS;

	public static long getSecondTime() {
		return System.currentTimeMillis() / SECOND_TO_MILLI;
	}

	public static long getMinuteStart(long ts) {
		return ts / ONE_MINUTE_TS * ONE_MINUTE_TS;
	}

	public static long getHourStart(long ts) {
		return ts / ONE_HOUR_TS * ONE_HOUR_TS;
	}

	public static int getHour(long ts) {
		return (int) ((ts - getDayStart(ts)) / ONE_HOUR_TS);
	}

	public static long getDayStart(long ts) {
		ZoneId zoneId = TimeZone.getDefault().toZoneId();
		LocalDate localDate = Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate();
		return localDate.atStartOfDay().atZone(zoneId).toInstant().toEpochMilli();
	}

	public static long getWeekStart(long ts) {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		calendar.setTimeInMillis(getDayStart(ts));
		calendar.add(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek() - calendar.get(Calendar.DAY_OF_WEEK));
		return calendar.getTimeInMillis();
	}

	public static long getMonthStart(long ts) {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		calendar.setTimeInMillis(ts);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		calendar.clear();
		calendar.set(year, month, 1);
		return calendar.getTimeInMillis();
	}

	public static long getQuarterStart(long ts) {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		calendar.setTimeInMillis(ts);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		calendar.clear();
		calendar.set(year, month / 3 * 3, 1);
		return calendar.getTimeInMillis();
	}

	public static long getYearStart(long ts) {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		calendar.setTimeInMillis(ts);
		int year = calendar.get(Calendar.YEAR);
		calendar.clear();
		calendar.set(year, 0, 1);
		return calendar.getTimeInMillis();
	}

	public static long timeStringAs(String str, TimeUnit unit) {
		String lower = str.toLowerCase(Locale.ROOT).trim();

		try {
			Matcher m = Pattern.compile("(-?[0-9]+)([a-z]+)?").matcher(lower);
			if (!m.matches()) {
				throw new NumberFormatException("Failed to parse time string: " + str);
			}

			long val = Long.parseLong(m.group(1));
			String suffix = m.group(2);

			// Check for invalid suffixes
			if (suffix != null && !timeSuffixes.containsKey(suffix)) {
				throw new NumberFormatException("Invalid suffix: \"" + suffix + "\"");
			}

			// If suffix is valid use that, otherwise none was provided and use the default passed
			return unit.convert(val, suffix != null ? timeSuffixes.get(suffix) : unit);
		} catch (NumberFormatException e) {
			String timeError = "Time must be specified as seconds (s), "
					+ "milliseconds (ms), microseconds (us), minutes (m or min), hour (h), or day (d). "
					+ "E.g. 50s, 100ms, or 250us.";

			throw new NumberFormatException(timeError + "\n" + e.getMessage());
		}
	}

	public static long minusDays(long ts, int days) {
		ZoneId zoneId = TimeZone.getDefault().toZoneId();
		ZonedDateTime zonedDateTime = Instant.ofEpochMilli(ts).atZone(zoneId);
		return zonedDateTime.minusDays(days).toInstant().toEpochMilli();
	}

}
