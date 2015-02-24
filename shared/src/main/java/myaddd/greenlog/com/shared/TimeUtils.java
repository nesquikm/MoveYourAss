package myaddd.greenlog.com.shared;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public final class TimeUtils {
    public static Calendar getTime(final int hourOfDay, final int minute) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 0);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Calendar getTime(Calendar src) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 0);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, src.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, src.get(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, src.get(Calendar.MILLISECOND));
        return calendar;
    }

    public static String formatTime(final Context context, final Calendar time) {
        final String skeleton = DateFormat.is24HourFormat(context) ? "Hm" : "hma";
        final String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    public static String formatTime(final Context context, final int hourOfDay, final int minute) {
        return formatTime(context, getTime(hourOfDay, minute));
    }

    public static boolean isInPeriod(final Calendar startTime, final Calendar endTime) {
        return isInPeriod(startTime, endTime, Calendar.getInstance());
    }

    public static boolean isInPeriod(final Calendar startTime, final Calendar endTime, final Calendar time) {
        Calendar onlyTime = getTime(time);

        return endTime.after(startTime) ? (onlyTime.after(startTime) && onlyTime.before(endTime)) :
                (onlyTime.after(startTime) || onlyTime.before(endTime));
//        final Calendar actualStartTime = (Calendar) time.clone();
//        actualStartTime.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
//        actualStartTime.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));
//        actualStartTime.set(Calendar.SECOND, 0);
//        actualStartTime.set(Calendar.MILLISECOND, 0);
//
//        if (time.before(actualStartTime)) {
//            return false;
//        }
//
//        final Calendar actualEndTime = (Calendar) time.clone();
//        actualEndTime.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
//        actualEndTime.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
//        actualEndTime.set(Calendar.SECOND, 0);
//        actualEndTime.set(Calendar.MILLISECOND, 0);
//
//        if (actualEndTime.before(actualStartTime)) {
//            actualEndTime.add(Calendar.DATE, 1);
//        }
//
//        if (time.after(actualEndTime)) {
//            return false;
//        }
//
//        return true;
    }

    public static boolean isAfterEndOfPeriod(final Calendar whenSet, final Calendar endTime) {
        return isAfterEndOfPeriod(whenSet, endTime, Calendar.getInstance());
    }

    public static boolean isAfterEndOfPeriod(final Calendar whenSet, final Calendar endTime, final Calendar time) {
        final Calendar actualEndTime = (Calendar) whenSet.clone();
        actualEndTime.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
        actualEndTime.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
        actualEndTime.set(Calendar.SECOND, 0);
        actualEndTime.set(Calendar.MILLISECOND, 0);

        if (actualEndTime.before(whenSet)) {
            actualEndTime.add(Calendar.DATE, 1);
        }

        return (time.after(actualEndTime));
    }
}
