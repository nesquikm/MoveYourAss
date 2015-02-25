package mya.greenlog.com.moveyourass;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import myaddd.greenlog.com.shared.MYAService;
import myaddd.greenlog.com.shared.State;

public class MYAServiceWear extends MYAService {
    private static final String TAG = MYAServiceWear.class.getSimpleName();

    private NotificationManager mNotificationManager;
    private static final long[] VIBRATION_PATTERN_NEED_REST = {0, 150, 500, 150, 500, 150};
    private static final long[] VIBRATION_PATTERN_REST_COMPLETED = {0, 300};
    private static final long NOTIFICATION_REST_COMPLETED_TIMEOUT_MS = 1000 * 15;

    private Timer mDelayedNotifyCancelTimer;

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onInactivityExpired() {
        showNotificationNeedRest();
    }

    @Override
    protected void onRestCompleted() {
        showNotificationRestCompleted();
    }

    @Override
    protected void onStepDetected(final State.SuspendState suspendState, final long lastStepDetectedAgo, final long restStepsNeed, final long minimumRestSteps) {
        showNotificationStepDetected(restStepsNeed, minimumRestSteps);
    }

    @Override
    protected void onSuspendStateChanged(final State.SuspendState suspendState, final long lastStepDetectedAgo, final long restStepsNeed) {
        if (suspendState != State.SuspendState.NotSuspended) {
            mNotificationManager.cancelAll();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case COMMAND_SET_SUSPEND:
                        // cancel all notifications
                        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
                        break;
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void showNotificationNeedRest() {
        mNotificationManager.cancelAll();
//
//        final Intent intentStartApp = new Intent(this, MainActivity.class);
//        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(intentStartApp);
//        final PendingIntent pendingIntentStartApp = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        int lastSnoozeId = mSettingsManager.readSnoozeModeLastId();
//        String lastSnoozeText = getResources().getStringArray(R.array.snooze_modes_values)[lastSnoozeId];
//
//        final Intent intentDefaultSnooze = new Intent(this, MYAServiceMobile.class);
//        intentDefaultSnooze.setAction(MYAService.COMMAND_SET_SUSPEND);
//        intentDefaultSnooze.putExtra(MYAService.COMMAND_SET_SUSPEND_STATE,
//                SnoozeModes.getSuspendState(lastSnoozeId).ordinal());
//        intentDefaultSnooze.putExtra(MYAService.COMMAND_SET_SUSPEND_TIMEOUT,
//                SnoozeModes.getSuspendTimeoutSeconds(lastSnoozeId));
//        final PendingIntent pendingIntentDefaultSnooze = PendingIntent.getService(this, 0, intentDefaultSnooze, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        final Intent intentSnoozeSelect = new Intent(this, SnoozePickerActivity.class);
//        final PendingIntent pendingSnoozeSelect = PendingIntent.getActivity(this, 0, intentSnoozeSelect, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = (new NotificationCompat.Builder(this))
                .setContentTitle(getString(R.string.need_rest_title))
                .setContentText(getString(R.string.need_rest_content))
                .setWhen((new Date()).getTime())
//                .setContentIntent(pendingIntentStartApp)
//                .addAction(R.drawable.ic_snooze_white_24dp, lastSnoozeText, pendingIntentDefaultSnooze)
//                .addAction(R.drawable.ic_list_white_24dp, getString(R.string.need_rest_action_snooze_select), pendingSnoozeSelect)
//                .setDeleteIntent(pendingIntentDefaultSnooze)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLights(0xff3f51b5, 500, 500)
                .setVibrate(VIBRATION_PATTERN_NEED_REST)
                .build();
        mNotificationManager.notify(1, notification);
    }

    private void showNotificationRestCompleted() {
        mNotificationManager.cancelAll();

//        final Intent intentStartApp = new Intent(this, MainActivity.class);
//        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(intentStartApp);
//        final PendingIntent pendingIntentStartApp = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = (new NotificationCompat.Builder(this))
                .setContentTitle(getString(R.string.rest_completed_title))
                .setContentText(getString(R.string.rest_completed_content))
                .setWhen((new Date()).getTime())
//                .setContentIntent(pendingIntentStartApp)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(VIBRATION_PATTERN_REST_COMPLETED)
                .build();
        mNotificationManager.notify(1, notification);

        if (mDelayedNotifyCancelTimer != null) {
            mDelayedNotifyCancelTimer.cancel();
            mDelayedNotifyCancelTimer.purge();
        }
        mDelayedNotifyCancelTimer = new Timer();
        mDelayedNotifyCancelTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mNotificationManager.cancelAll();
                mDelayedNotifyCancelTimer = null;
            }
        }, NOTIFICATION_REST_COMPLETED_TIMEOUT_MS);
    }


    private void showNotificationStepDetected(final long restStepsNeed, final long minimumRestSteps) {
        mNotificationManager.cancel(1);

//        final Intent intentStartApp = new Intent(this, MainActivity.class);
//        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(intentStartApp);
//        final PendingIntent pendingIntentStartApp = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = (new NotificationCompat.Builder(this))
                .setContentTitle(getString(R.string.keep_going))
                .setContentText(getString(R.string.steps_to_complete_rest, restStepsNeed))
                .setWhen((new Date()).getTime())
//                .setContentIntent(pendingIntentStartApp)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setProgress((int) minimumRestSteps, (int) (minimumRestSteps - restStepsNeed), false)
                .build();
        mNotificationManager.notify(2, notification);
    }
}
