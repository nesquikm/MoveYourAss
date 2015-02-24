package mya.greenlog.com.moveyourass;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

import myaddd.greenlog.com.shared.TimeUtils;

public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {
    private int mHourOfDay;
    private int mMinute;
    private OnTimeSetListener mOnTimeSetListener;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, mHourOfDay, mMinute,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    public void setTime(final Calendar time) {
        mHourOfDay = time.get(Calendar.HOUR_OF_DAY);
        mMinute = time.get(Calendar.MINUTE);
    }

    public void setOnTimeSetListener(final OnTimeSetListener onTimeSetListener) {
        mOnTimeSetListener = onTimeSetListener;
    }

    public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
        if (mOnTimeSetListener != null) {
            mOnTimeSetListener.onTimeSet(TimeUtils.getTime(hourOfDay, minute));
        }
    }

    public interface OnTimeSetListener {
        void onTimeSet(Calendar time);
    }

}