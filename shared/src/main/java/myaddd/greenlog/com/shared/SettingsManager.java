package myaddd.greenlog.com.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

public class SettingsManager {
    private static final String TAG = SettingsManager.class.getSimpleName();

    private static final String PREFERENCE_FILE_KEY = "com.greenlog.moveyourass.PREFERENCE_FILE_KEY";

    private static final String INACTIVITY_TIMEOUT = "INACTIVITY_TIMEOUT";

    private static final String MINIMUM_REST_STEPS = "MINIMUM_REST_STEPS";

    private static final String DND_DRIVING_ENABLED = "DND_DRIVING_ENABLED";

    private static final String DND_HOURS_ENABLED = "DND_HOURS_ENABLED";
    private static final String DND_HOURS_START_HOUR_OF_DAY = "DND_HOURS_START_HOUR_OF_DAY";
    private static final String DND_HOURS_START_MINUTE = "DND_HOURS_START_MINUTE";
    private static final String DND_HOURS_END_HOUR_OF_DAY = "DND_HOURS_END_HOUR_OF_DAY";
    private static final String DND_HOURS_END_MINUTE = "DND_HOURS_END_MINUTE";

    private static final String SNOOZE_MODE_LAST = "SNOOZE_MODE_LAST";
    private static final String SNOOZE_MODE_LAST_TIMEOUT_SECONDS = "SNOOZE_MODE_LAST_TIMEOUT_SECONDS";

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public SettingsManager(final Context context) {
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_FILE_KEY,
                Context.MODE_PRIVATE);
    }

    public int readSnoozeModeLastId() {
        return mSharedPreferences.getInt(SNOOZE_MODE_LAST, mContext.getResources().getInteger(R.integer.snooze_modes_value_default_id));
    }

    public void writeSnoozeModeLastId(final int mode) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(SNOOZE_MODE_LAST, mode);
        editor.apply();
    }

    public int readInactivityTimeout() {
        return mSharedPreferences.getInt(INACTIVITY_TIMEOUT, mContext.getResources().getInteger(R.integer.inactivity_timeout_value_default));
    }

    public synchronized void writeInactivityTimeout(final int timeout) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(INACTIVITY_TIMEOUT, timeout);
        editor.apply();
    }

    public int readMinimumRestSteps() {
        return mSharedPreferences.getInt(MINIMUM_REST_STEPS, mContext.getResources().getInteger(R.integer.rest_steps_value_default));
    }

    public synchronized void writeMinimumRestSteps(final int steps) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(MINIMUM_REST_STEPS, steps);
        editor.apply();
    }

    public boolean readDNDDrivingEnabled() {
        return mSharedPreferences.getBoolean(DND_DRIVING_ENABLED, true);
    }

    public synchronized void writeDNDDrivingEnabled(final boolean enabled) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(DND_DRIVING_ENABLED, enabled);
        editor.apply();
    }

    public boolean readDNDHoursEnabled() {
        return mSharedPreferences.getBoolean(DND_HOURS_ENABLED, true);
    }

    public synchronized void writeDNDHoursEnabled(final boolean enabled) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(DND_HOURS_ENABLED, enabled);
        editor.apply();
    }

    public Calendar readDNDHoursStart() {
        final int hourOfDay = mSharedPreferences.getInt(DND_HOURS_START_HOUR_OF_DAY, 21);
        final int minute = mSharedPreferences.getInt(DND_HOURS_START_MINUTE, 0);
        return TimeUtils.getTime(hourOfDay, minute);
    }

    public synchronized void writeDNDHoursStart(final Calendar time) {
        final int hourOfDay = time.get(Calendar.HOUR_OF_DAY);
        final int minute = time.get(Calendar.MINUTE);

        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(DND_HOURS_START_HOUR_OF_DAY, hourOfDay);
        editor.putInt(DND_HOURS_START_MINUTE, minute);
        editor.apply();
    }

    public Calendar readDNDHoursEnd() {
        final int hourOfDay = mSharedPreferences.getInt(DND_HOURS_END_HOUR_OF_DAY, 9);
        final int minute = mSharedPreferences.getInt(DND_HOURS_END_MINUTE, 0);
        return TimeUtils.getTime(hourOfDay, minute);
    }

    public synchronized void writeDNDHoursEnd(final Calendar time) {
        final int hourOfDay = time.get(Calendar.HOUR_OF_DAY);
        final int minute = time.get(Calendar.MINUTE);

        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(DND_HOURS_END_HOUR_OF_DAY, hourOfDay);
        editor.putInt(DND_HOURS_END_MINUTE, minute);
        editor.apply();
    }

    private void copyValue(final String key, final Map<String, ?> srcDataMap, final DataMap dstDataMap) {
        final Object value = srcDataMap.get(key);
        if (Boolean.class.isInstance(value)) {
            dstDataMap.putBoolean(key, (Boolean) value);
        } else if (Integer.class.isInstance(value)) {
            dstDataMap.putInt(key, (Integer) value);
        } else if (String.class.isInstance(value)) {
            dstDataMap.putString(key, (String) value);
        } else if (Long.class.isInstance(value)) {
            dstDataMap.putLong(key, (Long) value);
        } else {
            Log.e(TAG, "copyValue: unhandled data type for key " + key);
        }
    }

    private void copyValue(final String key, final DataMap srcDataMap, final SharedPreferences.Editor dstEditor) {
        final Object value = srcDataMap.get(key);
        if (Boolean.class.isInstance(value)) {
            dstEditor.putBoolean(key, (Boolean) value);
        } else if (Integer.class.isInstance(value)) {
            dstEditor.putInt(key, (Integer) value);
        } else if (String.class.isInstance(value)) {
            dstEditor.putString(key, (String) value);
        } else if (Long.class.isInstance(value)) {
            dstEditor.putLong(key, (Long) value);
        } else {
            Log.e(TAG, "copyValue: unhandled data type for key " + key);
        }
    }

//    public DataMap readByKey(String key) {
//        DataMap resultDataMap = new DataMap();
//        final Map<String, ?> localDataMap = mSharedPreferences.getAll();
//        copyValue(key, localDataMap, resultDataMap);
//        return resultDataMap;
//    }

    public synchronized DataMap readAll() {
        final DataMap resultDataMap = new DataMap();
        final Map<String, ?> localDataMap = mSharedPreferences.getAll();
        final Set<String> keySet = localDataMap.keySet();
        for (final String key : keySet) {
            copyValue(key, localDataMap, resultDataMap);
        }
        return resultDataMap;
    }

    public synchronized void writeAll(final DataMap dataMap) {
        if (isSame(readAll(), dataMap)) {
            return;
        }

        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        final Set<String> keySet = dataMap.keySet();
        for (final String key : keySet) {
            copyValue(key, dataMap, editor);
        }
        editor.apply();
    }

    public static boolean isSame(final DataMap dataMap1, final DataMap dataMap2) {
        if (dataMap1 == null || dataMap2 == null) {
            return false;
        }
        return dataMap1.equals(dataMap2);
    }


    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }
}
