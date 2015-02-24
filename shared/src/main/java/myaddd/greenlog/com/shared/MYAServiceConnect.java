package myaddd.greenlog.com.shared;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public abstract class MYAServiceConnect {
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;

    public static final int MSG_STATE_REQUEST = 3;
    public static final int MSG_STATE_CHANGED = 4;

    private final Context mContext;
    private final Messenger mIncomingMessenger;
    private Messenger mOutgoingMessenger;
    private final ServiceConnection mServiceConnection;

    private OnStateChangedListener mOnStateChangedListener;

    public MYAServiceConnect(final Context context, final OnStateChangedListener onStateChangedListener) {
        mContext = context;

        mOnStateChangedListener = onStateChangedListener;

        mIncomingMessenger = new Messenger(new IncomingHandler());
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                mOutgoingMessenger = new Messenger(service);

                send(MSG_REGISTER_CLIENT);
                send(MSG_STATE_REQUEST);
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
                mOutgoingMessenger = null;
            }
        };

        mContext.bindService(new Intent(mContext, getServiceClass()), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void destroy() {
        mOnStateChangedListener = null;
        send(MSG_UNREGISTER_CLIENT);
        if (mOutgoingMessenger != null) {
            mContext.unbindService(mServiceConnection);
        }
    }

    public void sendStateRequest() {
        send(MSG_STATE_REQUEST);
    }

    private void send(final int what) {
        if (mOutgoingMessenger != null) {
            try {
                final Message msg = Message.obtain(null, what);
                msg.replyTo = mIncomingMessenger;
                mOutgoingMessenger.send(msg);
            } catch (final RemoteException e) {
            }
        }
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_STATE_CHANGED:
                    if (mOnStateChangedListener != null) {
                        final Bundle bundle = msg.getData();
                        final State.SuspendState suspendState = State.SuspendState.valueOf(bundle.getInt("suspendState", State.SuspendState.NotSuspended.ordinal()));
                        final long lastStepDetectedAgo = bundle.getLong("lastStepDetectedAgo", 0);
                        final long restStepsNeed = bundle.getLong("restStepsNeed");
                        mOnStateChangedListener.onStateChanged(suspendState, lastStepDetectedAgo, restStepsNeed);
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    public interface OnStateChangedListener {
        void onStateChanged(State.SuspendState suspendState, long lastStepDetectedAgo, long restStepsNeed);
    }

    protected abstract Class getServiceClass();
}
