package mya.greenlog.com.moveyourass;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import myaddd.greenlog.com.shared.SettingsManager;
import myaddd.greenlog.com.shared.SnoozeModes;
import myaddd.greenlog.com.shared.State;

public class SnoozePickerFragment extends DialogFragment {
    private boolean mItemSelected = false;
    private OnDismissListener mOnDismissListener;
    private SettingsManager mSettingsManager;
    private boolean mSendOnDismiss = false;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        mSettingsManager = new SettingsManager(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.snooze_for)
                .setItems(R.array.snooze_modes_values, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        mItemSelected = true;
                        sendToServiceSuspendById(which);
                    }
                });
        return builder.create();
    }

    public void setSendOnDismiss(boolean sendOnDismiss) {
        mSendOnDismiss = sendOnDismiss;
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

    @Override
    public void onDismiss(final DialogInterface dialog) {
        if (!mItemSelected && mSendOnDismiss) {
            sendToServiceSuspendById(getActivity().getResources().getInteger(R.integer.snooze_modes_value_default_id));
        }
        super.onDismiss(dialog);

        if (mOnDismissListener != null) {
            mOnDismissListener.OnDismiss();
        }
    }

    private void sendToServiceSuspendById(final int id) {
        State.SuspendState suspendState = SnoozeModes.getSuspendState(id);
        long timeoutSeconds = SnoozeModes.getSuspendTimeoutSeconds(id);
        MYAServiceConnectMobile.sendToServiceSuspend(getActivity(),
                suspendState,
                timeoutSeconds);
        // Do not write "Forever" as default
        mSettingsManager.writeSnoozeModeLastId((suspendState == State.SuspendState.Forever) ?
                getActivity().getResources().getInteger(R.integer.snooze_modes_value_default_id) : id);
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    public interface OnDismissListener {
        void OnDismiss();
    }
}