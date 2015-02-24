package mya.greenlog.com.moveyourass;

import android.content.Context;
import android.content.Intent;

import myaddd.greenlog.com.shared.MYAService;
import myaddd.greenlog.com.shared.MYAServiceConnect;
import myaddd.greenlog.com.shared.State;

public class MYAServiceConnectMobile extends MYAServiceConnect {
    public MYAServiceConnectMobile(final Context context, final OnStateChangedListener onStateChangedListener) {
        super(context, onStateChangedListener);
        MYAServiceAutostartMobile.startService(context);
    }

    public static void sendToServiceSuspend(Context context, State.SuspendState suspendState, long timeoutSeconds) {
        final Intent intent = new Intent(context, MYAServiceMobile.class);
        intent.setAction(MYAService.COMMAND_SET_SUSPEND);
        intent.putExtra(MYAService.COMMAND_SET_SUSPEND_STATE, suspendState.ordinal());
        intent.putExtra(MYAService.COMMAND_SET_SUSPEND_TIMEOUT, timeoutSeconds);
        context.startService(intent);
    }

    @Override
    protected Class getServiceClass() {
        return MYAServiceMobile.class;
    }
}
