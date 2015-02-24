package mya.greenlog.com.moveyourass;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

public class SnoozePickerActivity extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fragmentManager = getFragmentManager();

        SnoozePickerFragment snoozePickerFragment = (SnoozePickerFragment) fragmentManager.findFragmentByTag("snoozePickerFragment");
        if (snoozePickerFragment == null) {
            snoozePickerFragment = new SnoozePickerFragment();
            snoozePickerFragment.setSendOnDismiss(true);
            snoozePickerFragment.setOnDismissListener(new SnoozePickerFragment.OnDismissListener() {
                @Override
                public void OnDismiss() {
                    SnoozePickerActivity.this.finish();
                }
            });
            fragmentManager.beginTransaction().add(snoozePickerFragment, "snoozePickerFragment").commit();
        }
    }
}
