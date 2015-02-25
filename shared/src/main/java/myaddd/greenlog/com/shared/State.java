package myaddd.greenlog.com.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// TODO: remove <item>5</item> from inactivity_timeout.xml!

public class State {
    private static final String TAG = State.class.getSimpleName();
    private static final String STATE_FILE_KEY = "com.greenlog.moveyourass.STATE_FILE_KEY";

    private static final long SEND_STATE_PERIOD_MS = 10 * 1000;
    private static final long ANALYZE_PERIOD_MS = 1 * 60 * 1000;

    private final Context mContext;

    private final OnInactivityExpiredListener mOnInactivityExpiredListener;
    private final OnRestCompletedListener mOnRestCompletedListener;
    private OnDataSyncListener mOnDataSyncListener;
    private final OnStateChangedListener mOnStateChangedListener;
    private final OnSuspendStateChangedListener mOnSuspendStateChangedListener;
    private final OnStepDetectedListener mOnStepDetectedListener;

    private final SettingsManager mSettingsManager;

    public static enum SuspendState {
        NotSuspended,
        Forever,
        DNDHours,
        Timeout,
        UntilTomorrow,
        NextOverstay,
        DNDDriving;

        public static SuspendState valueOf(final int index) {
            final SuspendState[] values = SuspendState.values();
            if (index < 0 || index >= values.length) {
                throw new ArrayIndexOutOfBoundsException("SuspendState out of bounds");
            }
            return values[index];
        }
    }

    private Timer mSendStateTimer;
    private boolean mDirty = false;
    private final Timer mAnalyzePeriodicallyTimer;
    private long mLastAnalyzedTime = 0;

    // State data begin =============
    private long mLastStepDetectedTime;

    private long mStepCounterStartTime;
    private long mStepCounterCount = 1000 * 1000 * 1000; // by default, we don't need to rest

    private long mIsDrivingChangedTime;
    private boolean mIsDriving;

    private long mSuspendStateChangedTime;
    private SuspendState mSuspendState = SuspendState.NotSuspended;
    private long mSuspendTimeoutSeconds;
    // State data end =============

    private SuspendState mOldSuspendState = null;

    private boolean mIsInactivityTimeoutExpired = false;
    private boolean mIsRestCompleted = true; // we don't need rest
    private long mLastStepsNeed = -1;

    public State(final Context context, final OnDataSyncListener onDataSyncListener, final OnSuspendStateChangedListener onSuspendStateChangedListener, final OnInactivityExpiredListener onInactivityExpiredListener, final OnRestCompletedListener onRestCompletedListener, final OnStateChangedListener onStateChangedListener, final OnStepDetectedListener onStepDetectedListener, final SettingsManager settingsManager) {
        mContext = context;

        mOnInactivityExpiredListener = onInactivityExpiredListener;
        mOnRestCompletedListener = onRestCompletedListener;
        mOnDataSyncListener = onDataSyncListener;
        mOnSuspendStateChangedListener = onSuspendStateChangedListener;
        mOnStateChangedListener = onStateChangedListener;
        mOnStepDetectedListener = onStepDetectedListener;

        mSettingsManager = settingsManager;

        mAnalyzePeriodicallyTimer = new Timer();
        mAnalyzePeriodicallyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (now() > mLastAnalyzedTime + ANALYZE_PERIOD_MS) {
                    analyzeState(false);
                }
            }
        }, ANALYZE_PERIOD_MS, ANALYZE_PERIOD_MS);

        readState();
        analyzeState(true);
    }

    public synchronized void stop() {
        mAnalyzePeriodicallyTimer.cancel();
        mAnalyzePeriodicallyTimer.purge();

        sendState();
        saveState();

        if (mSendStateTimer != null) {
            mSendStateTimer.cancel();
            mSendStateTimer.purge();
            mSendStateTimer = null;
        }

        mOnDataSyncListener = null;
    }


    public synchronized void registerStep(final long increment) {
        updateLastStepDetected(now());
        mStepCounterCount += increment;
        analyzeState(true);
    }


    public synchronized void setDriving(boolean isDriving) {
        updateIsDriving(now(), isDriving);
        analyzeState(true);
    }

    private synchronized void updateLastStepDetected(final long time) {
        if (mLastStepDetectedTime < time) {
            mLastStepDetectedTime = time;
        }
    }

    /**
     * Suspend state machine. Sorry, but it's ugly now.
     *
     * @param suspendState
     * @param timeoutSeconds
     */
    public synchronized void setSuspend(final SuspendState suspendState, final long timeoutSeconds) {
        Log.d("TAG", "setSuspend " + suspendState.name() + " timeoutSeconds " + timeoutSeconds);
        switch (suspendState) {
            case NotSuspended:
                switch (mSuspendState) {
                    case DNDDriving:
                    case DNDHours:
                    case NotSuspended:
                        Log.e(TAG, "setSuspend " + suspendState.name() + ": called when " + mSuspendState.name());
                        break;
                    case Forever:
                    case Timeout:
                    case UntilTomorrow:
                    case NextOverstay:
                        updateSuspend(now(), suspendState, timeoutSeconds);
                        analyzeState(true);
                        break;
                }
                break;
            case Forever:
            case Timeout:
            case UntilTomorrow:
            case NextOverstay:
                switch (mSuspendState) {
                    case NotSuspended:
                        updateSuspend(now(), suspendState, timeoutSeconds);
                        analyzeState(true);
                        break;
                    case Forever:
                    case DNDHours:
                    case Timeout:
                    case UntilTomorrow:
                    case NextOverstay:
                    case DNDDriving:
                        Log.e(TAG, "setSuspend " + suspendState.name() + ": called when " + mSuspendState.name());
                        break;
                }
                break;
            case DNDHours:
            case DNDDriving:
                Log.e(TAG, "setSuspend User can't manually set " + suspendState.name());
                break;
        }
    }

    public void syncStateReceived(final DataMap dataMap) {
        updateLastStepDetected(dataMap.getLong("mLastStepDetectedTime", 0));

        updateStepCounter(dataMap.getLong("mStepCounterStartTime", 0),
                dataMap.getLong("mStepCounterCount", 1000 * 1000 * 1000));

        updateIsDriving(
                dataMap.getLong("mIsDrivingChangedTime", 0),
                dataMap.getBoolean("mIsDriving", false));

        updateSuspend(
                dataMap.getLong("mSuspendStateChangedTime", 0),
                SuspendState.valueOf(dataMap.getInt("mSuspendState", SuspendState.NotSuspended.ordinal())),
                dataMap.getLong("mSuspendTimeoutSeconds", 0));

        analyzeState(false);
    }

    public void settingsChanged() {
        analyzeState(false);
    }

    private long getLastStepDetectedAgo() {
        return now() - mLastStepDetectedTime;
    }

    private long getRestStepsNeed() {
        if (mSettingsManager.readMinimumRestSteps() > mStepCounterCount) {
            return mSettingsManager.readMinimumRestSteps() - mStepCounterCount;
        }
        return -1;
    }

    private synchronized void updateStepCounter(final long startTime, final long count) {
        if (mStepCounterStartTime < startTime) {
            mStepCounterStartTime = startTime;
            mStepCounterCount = count;
        } else if (mStepCounterStartTime == startTime) {
            if (mStepCounterCount < count) {
                mStepCounterCount = count;
            }
        }
    }

    private synchronized void updateIsDriving(final long time, final boolean isDriving) {
        if (mIsDrivingChangedTime < time) {
            mIsDrivingChangedTime = time;
            mIsDriving = isDriving;
        }
    }

    private synchronized void updateSuspend(final long time, final SuspendState suspendState, final long timeoutSeconds) {
        if (mSuspendStateChangedTime < time) {
            mSuspendStateChangedTime = time;
            mSuspendState = suspendState;
            mSuspendTimeoutSeconds = timeoutSeconds;
        }
    }

    private synchronized void setDirty() {
        mDirty = true;
        if (mSendStateTimer == null) {
            mSendStateTimer = new Timer();
            mSendStateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mSendStateTimer = null;
                    sendState();
                }
            }, SEND_STATE_PERIOD_MS);
            sendState();
        }
    }

    private synchronized void sendState() {
        if (!mDirty)
            return;
        if (mOnDataSyncListener != null) {
            final DataMap dataMap = new DataMap();
            dataMap.putLong("mLastStepDetectedTime", mLastStepDetectedTime);

            dataMap.putLong("mStepCounterStartTime", mStepCounterStartTime);
            dataMap.putLong("mStepCounterCount", mStepCounterCount);

            dataMap.putLong("mIsDrivingChangedTime", mIsDrivingChangedTime);
            dataMap.putBoolean("mIsDriving", mIsDriving);

            dataMap.putLong("mSuspendStateChangedTime", mSuspendStateChangedTime);
            dataMap.putInt("mSuspendState", mSuspendState.ordinal());
            dataMap.putLong("mSuspendTimeoutSeconds", mSuspendTimeoutSeconds);

            mOnDataSyncListener.OnDataSync(dataMap);
        }
        mDirty = false;
    }

    private void readState() {
        final SharedPreferences sharedPreferences = mContext.getSharedPreferences(STATE_FILE_KEY,
                Context.MODE_PRIVATE);

        // When first start, set it to something...
        mLastStepDetectedTime = sharedPreferences.getLong("mLastStepDetectedTime", now());

        mStepCounterStartTime = sharedPreferences.getLong("mStepCounterStartTime", 0);
        mStepCounterCount = sharedPreferences.getLong("mStepCounterCount", 1000 * 1000 * 1000); // by default, we don't need to rest

        mIsDrivingChangedTime = sharedPreferences.getLong("mIsDrivingChangedTime", 0);
        mIsDriving = sharedPreferences.getBoolean("mIsDriving", false);

        mSuspendStateChangedTime = sharedPreferences.getLong("mSuspendStateChangedTime", 0);
        mSuspendState = SuspendState.valueOf(sharedPreferences.getInt("mSuspendState", SuspendState.NotSuspended.ordinal()));
        mSuspendTimeoutSeconds = sharedPreferences.getLong("mSuspendTimeoutSeconds", 0);
    }

    private void saveState() {
        final SharedPreferences sharedPreferences = mContext.getSharedPreferences(STATE_FILE_KEY,
                Context.MODE_PRIVATE);

        final SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong("mLastStepDetectedTime", mLastStepDetectedTime);

        editor.putLong("mStepCounterStartTime", mStepCounterStartTime);
        editor.putLong("mStepCounterCount", mStepCounterCount);

        editor.putLong("mIsDrivingChangedTime", mIsDrivingChangedTime);
        editor.putBoolean("mIsDriving", mIsDriving);

        editor.putLong("mSuspendStateChangedTime", mSuspendStateChangedTime);
        editor.putInt("mSuspendState", mSuspendState.ordinal());
        editor.putLong("mSuspendTimeoutSeconds", mSuspendTimeoutSeconds);

        editor.commit();
    }

    private synchronized void analyzeState(final boolean forceDirty) {
        boolean isSuspendStateChanged = false;
        final long now = now();
        // Check for DND hours, set or clear
        if (mSuspendState != SuspendState.Forever) {
            if (mSettingsManager.readDNDHoursEnabled()
                    && TimeUtils.isInPeriod(mSettingsManager.readDNDHoursStart(), mSettingsManager.readDNDHoursEnd())) {
                // set only when not yet
                if (mSuspendState != SuspendState.DNDHours) {
                    updateSuspend(now, SuspendState.DNDHours, 0);
                }
                isSuspendStateChanged = true;
            } else {
                // clear only when was in DNDHours
                if (mSuspendState == SuspendState.DNDHours) {
                    updateSuspend(now, SuspendState.NotSuspended, 0);
                    updateLastStepDetected(now);
                    isSuspendStateChanged = true;
                }
            }
        }

        // Check for UntilTomorrow, only clear (set manually)
        if (mSuspendState == SuspendState.UntilTomorrow) {
            final Calendar whenSet = Calendar.getInstance();
            whenSet.setTimeInMillis(mSuspendStateChangedTime);
            if (TimeUtils.isAfterEndOfPeriod(whenSet, mSettingsManager.readDNDHoursEnd())) {
                updateSuspend(now, SuspendState.NotSuspended, 0);
                isSuspendStateChanged = true;
            }
        }

        // Check for Timeout, only clear (set manually)
        if (mSuspendState == SuspendState.Timeout) {
            if (now > (mSuspendStateChangedTime + mSuspendTimeoutSeconds * 1000)) {
                updateSuspend(now, SuspendState.NotSuspended, 0);
                isSuspendStateChanged = true;
            }
        }

        // Check for DNDDriving
        if (mSuspendState == SuspendState.DNDDriving) {
            if (!mSettingsManager.readDNDDrivingEnabled() || !mIsDriving) {
                updateSuspend(now, SuspendState.NotSuspended, 0);
                isSuspendStateChanged = true;
            }
        } else {
            if (mSuspendState == SuspendState.NotSuspended && mSettingsManager.readDNDDrivingEnabled() && mIsDriving) {
                updateSuspend(now, SuspendState.DNDDriving, 0);
                isSuspendStateChanged = true;
            }
        }


        final boolean isInactivityTimeoutExpired = (getRestStepsNeed() == -1) && (now > mLastStepDetectedTime + mSettingsManager.readInactivityTimeout() * 60 * 1000);
        final boolean inactivityTimeoutExpirationTrigger = !mIsInactivityTimeoutExpired && isInactivityTimeoutExpired;
        mIsInactivityTimeoutExpired = isInactivityTimeoutExpired;

        // Inactivity period expired, start step count for the rest
        if (inactivityTimeoutExpirationTrigger) {
            updateStepCounter(now, 0);
        }

        final boolean isRestCompleted = (getRestStepsNeed() == -1);
        boolean restCompletedTrigger = !mIsRestCompleted && isRestCompleted;
        mIsRestCompleted = isRestCompleted;

        // When a user just completed the rest (for example, 50 steps) and set minimum steps
        // for completed above previous (for example, 100), he shouldn't see 'need rest'
        // notification again. So, increase step count here :)
        if (restCompletedTrigger) {
            updateStepCounter(now, 1000 * 1000 * 1000);
        }

        // Check for NextOverstay, only clear
        if (mSuspendState == SuspendState.NextOverstay) {
            if (restCompletedTrigger) {
                updateSuspend(now, SuspendState.NotSuspended, 0);
                restCompletedTrigger = false; // Do not notify!
                isSuspendStateChanged = true;
            }
        }

        boolean inactivityTimeoutExpirationAfterTimeoutTrigger = false;
        if (mOldSuspendState == SuspendState.Timeout && mSuspendState == SuspendState.NotSuspended && mStepCounterCount == 0) {
            inactivityTimeoutExpirationAfterTimeoutTrigger = true;
        }

        boolean stepDetectedTrigger = false;
        if (mLastStepsNeed != getRestStepsNeed() && getRestStepsNeed() > 0 && mStepCounterCount > 0) {
            stepDetectedTrigger = true;
            mLastStepsNeed = getRestStepsNeed();
        }

        mOldSuspendState = mSuspendState;

        if (mSuspendState == SuspendState.NotSuspended && (inactivityTimeoutExpirationTrigger || inactivityTimeoutExpirationAfterTimeoutTrigger) && mOnInactivityExpiredListener != null) {
            mOnInactivityExpiredListener.onInactivityExpired(mSuspendState, getLastStepDetectedAgo(), getRestStepsNeed());
        }

        if (mSuspendState == SuspendState.NotSuspended && restCompletedTrigger && mOnRestCompletedListener != null) {
            mOnRestCompletedListener.onRestCompleted(mSuspendState, getLastStepDetectedAgo(), getRestStepsNeed());
        }

        if (isSuspendStateChanged && mOnSuspendStateChangedListener != null) {
            mOnSuspendStateChangedListener.onSuspendStateChanged(mSuspendState, getLastStepDetectedAgo(), getRestStepsNeed());
        }

        if (mSuspendState == SuspendState.NotSuspended && !restCompletedTrigger && stepDetectedTrigger) {
            mOnStepDetectedListener.onStepDetected(mSuspendState, getLastStepDetectedAgo(), getRestStepsNeed());
        }

        if (forceDirty || isSuspendStateChanged) {
            if (mOnStateChangedListener != null) {
                mOnStateChangedListener.onStateChanged(mSuspendState, getLastStepDetectedAgo(), getRestStepsNeed());
            }
            setDirty();
        }

        mLastAnalyzedTime = now;
    }

    public void callOnStateChanged() {
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(mSuspendState, getLastStepDetectedAgo(), getRestStepsNeed());
        }
    }

    /**
     * Only for user-interactive (foreground) updates -- too frequently for background communication
     */
    public interface OnStateChangedListener {
        void onStateChanged(SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
    }

    public interface OnSuspendStateChangedListener {
        void onSuspendStateChanged(SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
    }

    public interface OnInactivityExpiredListener {
        void onInactivityExpired(SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
    }

    public interface OnRestCompletedListener {
        void onRestCompleted(SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
    }

    public interface OnStepDetectedListener {
        void onStepDetected(SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
    }

    public interface OnDataSyncListener {
        void OnDataSync(DataMap dataMap);

    }

    private long now() {
        return (new Date()).getTime();
    }
}
