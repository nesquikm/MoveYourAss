package myaddd.greenlog.com.shared;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public abstract class MYAService extends Service implements SensorEventListener, DataApi.DataListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = MYAService.class.getSimpleName();

    private static final String DATA_PATH_SETTINGS = "/settings";
    private static final String DATA_PATH_STATE = "/state";

    public static final String COMMAND_SET_SUSPEND = "myaddd.greenlog.com.shared.COMMAND_SET_SUSPEND";
    public static final String COMMAND_SET_SUSPEND_STATE = "myaddd.greenlog.com.shared.COMMAND_SET_SUSPEND_STATE";
    public static final String COMMAND_SET_SUSPEND_TIMEOUT = "myaddd.greenlog.com.shared.COMMAND_SET_SUSPEND_TIMEOUT";

    private static final long SYNC_DELAY_SETTINGS_MS = 1000 * 1;

    private static final int STEP_COUNTER_RATE_US = 1000 * 1000 * 30;

    protected SettingsManager mSettingsManager;
    private Timer mSettingsSyncTimer;
    private DataMap mLastReceivedSettingsDataMap;

    private GoogleApiClient mGoogleApiClientWear;

    private SensorManager mSensorManager;
    private Sensor mSensorStepCounter;

    private State mState;
    private long mLastStepCount = -1;

    private Messenger mIncomingMessenger;
    private final ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mSettingsManager = new SettingsManager(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        startWearConnection();
        startSettingsSynchronizer();
        startState();
        // TODO: Check status (don't start when suspended and check when unsuspended)
        startStepCounter();

        startClientIO();
    }


    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case COMMAND_SET_SUSPEND:
                        mState.setSuspend(
                                State.SuspendState.valueOf(intent.getIntExtra(COMMAND_SET_SUSPEND_STATE, State.SuspendState.NotSuspended.ordinal())),
                                intent.getLongExtra(COMMAND_SET_SUSPEND_TIMEOUT, 0)
                        );
                        break;
                }
            }
        }
        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(final Intent intent) {
        return mIncomingMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        return false; // We don't want to onRebind() will be called
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        stopClientIO();

        stopStepCounter();
        stopState();
        stopSettingsSynchronizer();
        stopWearConnection();

        super.onDestroy();
    }

    private void startWearConnection() {
        mGoogleApiClientWear = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

                    @Override
                    public void onConnected(final Bundle bundle) {
                        Log.d(TAG, "Wear connection: onConnected");
                        Wearable.DataApi.addListener(mGoogleApiClientWear, MYAService.this);
                    }

                    @Override
                    public void onConnectionSuspended(final int i) {
                        Log.d(TAG, "Wear connection: onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(final ConnectionResult connectionResult) {
                        Log.d(TAG, "Wear connection: onConnectionFailed " + connectionResult);
                        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
                            Log.d(TAG, "Wear connection: onConnectionFailed: The Android Wear app is not installed");
                        }
                    }
                })
                .addApi(Wearable.API)
                .build();
        if (mGoogleApiClientWear != null) {
            mGoogleApiClientWear.connect();
        }
    }

    private void stopWearConnection() {
        if (mGoogleApiClientWear != null) {
            Wearable.DataApi.removeListener(mGoogleApiClientWear, this);
            mGoogleApiClientWear.disconnect();
        }
    }

    private void startSettingsSynchronizer() {
        mSettingsManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void stopSettingsSynchronizer() {
        if (mSettingsSyncTimer != null) {
            mSettingsSyncTimer.cancel();
            mSettingsSyncTimer.purge();
            mSettingsSyncTimer = null;
        }
        mSettingsManager.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void startStepCounter() {
        synchronized (mSensorManager) {
            if (mSensorStepCounter == null) {
                mSensorStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (mSensorStepCounter != null) {
                    mSensorManager.registerListener(this, mSensorStepCounter, STEP_COUNTER_RATE_US, STEP_COUNTER_RATE_US);
                }
            }
        }
    }

    private void stopStepCounter() {
        synchronized (mSensorManager) {
            if (mSensorStepCounter != null) {
                mSensorManager.unregisterListener(this, mSensorStepCounter);
                mSensorStepCounter = null;
            }
        }
    }

    private void startState() {
        mState = new State(this, new State.OnDataSyncListener() {
            @Override
            public void OnDataSync(final DataMap dataMap) {
                if (mGoogleApiClientWear != null) {
                    final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(DATA_PATH_STATE);
                    putDataMapRequest.getDataMap().putAll(dataMap);
                    final PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                    final PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClientWear, putDataRequest);
                }
            }
        }, new State.OnSuspendStateChangedListener() {
            @Override
            public void onSuspendStateChanged(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed) {
                // TODO: start/stop step counter and activity detection when Forever or DNDHours?
                MYAService.this.onSuspendStateChanged(suspendState, lastStepDetectedAgo, restStepsNeed);
            }
        }, new State.OnInactivityExpiredListener() {
            @Override
            public void onInactivityExpired(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed) {
                MYAService.this.onInactivityExpired();
            }
        }, new State.OnRestCompletedListener() {
            @Override
            public void onRestCompleted(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed) {
                MYAService.this.onRestCompleted();
            }
        }, new State.OnStateChangedListener() {
            @Override
            public void onStateChanged(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed) {
                // Send always
                sendStateToClients(suspendState, lastStepDetectedAgo, restStepsNeed);
            }
        }, new State.OnStepDetectedListener() {
            @Override
            public void onStepDetected(final State.SuspendState suspendState, final long lastStepDetectedAgo, final long restStepsNeed) {
                MYAService.this.onStepDetected(suspendState, lastStepDetectedAgo, restStepsNeed, mSettingsManager.readMinimumRestSteps());
            }
        }, mSettingsManager);
    }

    private void stopState() {
        mState.stop();
    }


    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (Sensor.TYPE_STEP_COUNTER == event.sensor.getType()) {
            if (mLastStepCount > 0) {
                mState.registerStep((long) event.values[0] - mLastStepCount);
            }

            mLastStepCount = (long) event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {

    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEvents) {
        for (final DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                final DataItem dataItem = dataEvent.getDataItem();
                if (dataItem.getUri().getPath().compareTo(DATA_PATH_SETTINGS) == 0) {
                    mLastReceivedSettingsDataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    mSettingsManager.writeAll(mLastReceivedSettingsDataMap);
                } else if (dataItem.getUri().getPath().compareTo(DATA_PATH_STATE) == 0) {
                    mState.syncStateReceived(DataMapItem.fromDataItem(dataItem).getDataMap());
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (mSettingsSyncTimer != null) {
            mSettingsSyncTimer.cancel();
            mSettingsSyncTimer.purge();
        }

        mSettingsSyncTimer = new Timer();
        mSettingsSyncTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mGoogleApiClientWear != null) {
                    final DataMap readedDataMap = mSettingsManager.readAll();
                    if (!SettingsManager.isSame(readedDataMap, mLastReceivedSettingsDataMap)) {
                        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(DATA_PATH_SETTINGS);
                        putDataMapRequest.getDataMap().putAll(readedDataMap);
                        final PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                        final PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClientWear, putDataRequest);
                    }
                }

                mState.settingsChanged();
                mSettingsSyncTimer = null;
            }
        }, SYNC_DELAY_SETTINGS_MS);
    }

    private void startClientIO() {
        mIncomingMessenger = new Messenger(new IncomingHandler());
    }

    private void stopClientIO() {
        mClients.clear();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MYAServiceConnect.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MYAServiceConnect.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MYAServiceConnect.MSG_STATE_REQUEST:
                    mState.callOnStateChanged();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    }

    private void sendStateToClients(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed) {
        // removing inside the loop! :)
        for (int i = mClients.size() - 1; i >= 0; i--) {
            sendStateToClient(mClients.get(i), suspendState, lastStepDetectedAgo, restStepsNeed);
        }
    }

    private void sendStateToClient(final Messenger to, State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed) {
        final Bundle bundle = new Bundle();

        bundle.putInt("suspendState", suspendState.ordinal());
        bundle.putLong("lastStepDetectedAgo", lastStepDetectedAgo);
        bundle.putLong("restStepsNeed", restStepsNeed);

        try {
            final Message msg = Message.obtain(null, MYAServiceConnect.MSG_STATE_CHANGED);
            msg.replyTo = mIncomingMessenger;
            msg.setData(bundle);
            to.send(msg);
        } catch (final RemoteException e) {
            mClients.remove(to);
        }
    }

    protected void setDriving(boolean isDriving) {
        mState.setDriving(isDriving);
    }

    protected abstract void onInactivityExpired();

    protected abstract void onRestCompleted();

    protected abstract void onStepDetected(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed, long minimumRestSteps);

    protected abstract void onSuspendStateChanged(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
}
