package mya.greenlog.com.moveyourass;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MYAServiceAutostartMobile.startService(this);

        final FragmentManager fragmentManager = getFragmentManager();

        StatusFragment statusFragment = (StatusFragment) fragmentManager.findFragmentById(R.id.cardview_status);
        if (statusFragment == null) {
            statusFragment = new StatusFragment();
            fragmentManager.beginTransaction().add(R.id.cardview_status, statusFragment).commit();
        }

        SettingsFragment settingsFragment = (SettingsFragment) fragmentManager.findFragmentById(R.id.cardview_settings);
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
            fragmentManager.beginTransaction().add(R.id.cardview_settings, settingsFragment).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
