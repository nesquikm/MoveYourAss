package mya.greenlog.com.moveyourass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MYAServiceAutostartMobile extends BroadcastReceiver {
    private static final String TAG = MYAServiceAutostartMobile.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        startService(context);
    }

    public static void startService(final Context context) {
        final Intent newIntent = new Intent(context, MYAServiceMobile.class);
        context.startService(newIntent);
    }
}
