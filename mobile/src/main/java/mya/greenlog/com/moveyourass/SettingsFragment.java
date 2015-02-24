package mya.greenlog.com.moveyourass;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;

import myaddd.greenlog.com.shared.SettingsManager;
import myaddd.greenlog.com.shared.TimeUtils;

public class SettingsFragment extends Fragment {
    private SettingsManager mSettingsManager;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final boolean hasSavedInstanceState = savedInstanceState != null;

        mSettingsManager = new SettingsManager(getActivity());

        inactivityTimeoutInit(!hasSavedInstanceState);
        minimumRestStepsInit(!hasSavedInstanceState);
        dndDrivingInit(!hasSavedInstanceState);
        dndHoursInit(!hasSavedInstanceState);
    }

    private void inactivityTimeoutInit(final boolean readFromSettings) {
        final LinearLayout layout = (LinearLayout) getView().findViewById(R.id.layout_inactivity_timeout);
        final Spinner spinner = (Spinner) getView().findViewById(R.id.spinner_inactivity_timeout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                spinner.performClick();
            }
        });

        final int inactivityTimeoutValues[] = getActivity().getResources().getIntArray(R.array.inactivity_timeout_values);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
        for (final int inactivityTimeoutValue : inactivityTimeoutValues) {
            adapter.add(getActivity().getResources().getQuantityString(R.plurals.minutes, inactivityTimeoutValue, inactivityTimeoutValue));
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (readFromSettings) {
            final int timeout = mSettingsManager.readInactivityTimeout();

            int position = 0;
            for (int i = 0; i < inactivityTimeoutValues.length; i++) {
                if (inactivityTimeoutValues[i] == timeout) {
                    position = i;
                    break;
                }
            }
            spinner.setSelection(position);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                mSettingsManager.writeInactivityTimeout(inactivityTimeoutValues[position]);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {

            }
        });
    }

    private void minimumRestStepsInit(final boolean readFromSettings) {
        final LinearLayout layout = (LinearLayout) getView().findViewById(R.id.layout_minimum_rest_steps);
        final Spinner spinner = (Spinner) getView().findViewById(R.id.spinner_minimum_rest_steps);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                spinner.performClick();
            }
        });

        final int restStepsValues[] = getActivity().getResources().getIntArray(R.array.rest_steps_values);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
        for (final int restStepsValue : restStepsValues) {
            adapter.add(getActivity().getResources().getQuantityString(R.plurals.steps, restStepsValue, restStepsValue));
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (readFromSettings) {
            final int steps = mSettingsManager.readMinimumRestSteps();

            int position = 0;
            for (int i = 0; i < restStepsValues.length; i++) {
                if (restStepsValues[i] == steps) {
                    position = i;
                    break;
                }
            }
            spinner.setSelection(position);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                mSettingsManager.writeMinimumRestSteps(restStepsValues[position]);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
            }
        });
    }

    private void dndDrivingInit(final boolean readFromSettings) {
        final LinearLayout layout = (LinearLayout) getView().findViewById(R.id.layout_dnd_driving);
        final Switch button = (Switch) getView().findViewById(R.id.switch_dnd_driving);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                button.performClick();
            }
        });

        if (readFromSettings) {
            button.setChecked(mSettingsManager.readDNDDrivingEnabled());
        }
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                mSettingsManager.writeDNDDrivingEnabled(isChecked);
            }
        });
    }

    private void dndHoursInit(final boolean readFromSettings) {
        final LinearLayout layoutSwitch = (LinearLayout) getView().findViewById(R.id.layout_dnd_hours);
        final LinearLayout layoutStart = (LinearLayout) getView().findViewById(R.id.layout_dnd_start_time);
        final LinearLayout layoutEnd = (LinearLayout) getView().findViewById(R.id.layout_dnd_end_time);

        final Switch button = (Switch) getView().findViewById(R.id.switch_dnd_hours);

        layoutSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                button.performClick();
            }
        });

        final CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                mSettingsManager.writeDNDHoursEnabled(isChecked);
                layoutStart.setAlpha(isChecked ? 1.0f : 0.5f);
                layoutStart.setEnabled(isChecked);
                layoutEnd.setAlpha(isChecked ? 1.0f : 0.5f);
                layoutEnd.setEnabled(isChecked);
            }
        };

        if (readFromSettings) {
            final boolean enabled = mSettingsManager.readDNDHoursEnabled();
            button.setChecked(enabled);
        }

        switchListener.onCheckedChanged(button, button.isChecked());

        button.setOnCheckedChangeListener(switchListener);

        final TimePickerFragment.OnTimeSetListener startTimeListener = new TimePickerFragment.OnTimeSetListener() {
            @Override
            public void onTimeSet(final Calendar time) {
                dndHoursTextUpdate(time, mSettingsManager.readDNDHoursEnd());
                mSettingsManager.writeDNDHoursStart(time);
            }
        };

        final TimePickerFragment.OnTimeSetListener endTimeListener = new TimePickerFragment.OnTimeSetListener() {
            @Override
            public void onTimeSet(final Calendar time) {
                dndHoursTextUpdate(mSettingsManager.readDNDHoursStart(), time);
                mSettingsManager.writeDNDHoursEnd(time);
            }
        };

        dndHoursTextUpdate(mSettingsManager.readDNDHoursStart(), mSettingsManager.readDNDHoursEnd());

        layoutStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final TimePickerFragment timePicker = new TimePickerFragment();
                timePicker.setTime(mSettingsManager.readDNDHoursStart());
                timePicker.setOnTimeSetListener(startTimeListener);
                timePicker.show(getActivity().getFragmentManager(), "startTimePicker");
            }
        });

        layoutEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final TimePickerFragment timePicker = new TimePickerFragment();
                timePicker.setTime(mSettingsManager.readDNDHoursEnd());
                timePicker.setOnTimeSetListener(endTimeListener);
                timePicker.show(getActivity().getFragmentManager(), "endTimePicker");
            }
        });
    }

    private void dndHoursTextUpdate(final Calendar startTime, final Calendar endTime) {
        final TextView textViewStart = (TextView) getView().findViewById(R.id.text_dnd_start_time);
        final TextView textViewEnd = (TextView) getView().findViewById(R.id.text_dnd_end_time);

        textViewStart.setText(TimeUtils.formatTime(getActivity(), startTime));
        final String endTimeText = TimeUtils.formatTime(getActivity(), endTime);
        final String endText = endTime.after(startTime) ?
                endTimeText :
                getActivity().getString(R.string.dnd_hours_end_text, endTimeText, getActivity().getString(R.string.next_day));
        textViewEnd.setText(endText);
    }
}
