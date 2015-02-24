package mya.greenlog.com.moveyourass;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import myaddd.greenlog.com.shared.MYAServiceConnect;
import myaddd.greenlog.com.shared.SettingsManager;
import myaddd.greenlog.com.shared.State;

public class StatusFragment extends Fragment {
    private static final long SEND_REQUEST_PERIOD_MS = 1 * 1000;

    private MYAServiceConnectMobile mMYAServiceConnectMobile;
    private Timer mSendRequestTimer;
    private SettingsManager mSettingsManager;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, container, false);
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

        // TODO: check hasSavedInstanceState!!!
    }

    @Override
    public void onStart() {
        super.onStart();

        mMYAServiceConnectMobile = new MYAServiceConnectMobile(getActivity(), new MYAServiceConnect.OnStateChangedListener() {
            @Override
            public void onStateChanged(final State.SuspendState suspendState, final long lastStepDetectedAgo, final long restStepsNeed) {
                final View statusLoading = (View) getView().findViewById(R.id.status_loading);
                final View statusInfo = (View) getView().findViewById(R.id.status_info);

                if (statusInfo.getVisibility() == View.GONE) {
                    statusLoading.setVisibility(View.GONE);
                    statusInfo.setVisibility(View.VISIBLE);
                }

                final TextView statusTitle = (TextView) getView().findViewById(R.id.status_title);
                final TextView statusText = (TextView) getView().findViewById(R.id.status_text);
                final ProgressBar statusProgress = (ProgressBar) getView().findViewById(R.id.status_progress);
                final ImageView statusImage = (ImageView) getView().findViewById(R.id.status_image);
                final View statusButtons = (View) getView().findViewById(R.id.status_buttons);
                final Button statusButton = (Button) getView().findViewById(R.id.status_button);

                boolean isMoving = lastStepDetectedAgo < 30 * 1000;
                // Title
                if (suspendState == State.SuspendState.NotSuspended) {
                    if (restStepsNeed < 0) {
                        if (isMoving) {
                            statusTitle.setText(R.string.you_moving);
                        } else {
                            statusTitle.setText(R.string.you_not_moving);
                        }
                    } else {
                        statusTitle.setText(R.string.you_need_rest);
                    }
                } else {
                    statusTitle.setText(R.string.notifications_suspended);
                }

                // Text
                switch (suspendState) {
                    case NotSuspended: {
                        int minutesAgo = (int) (lastStepDetectedAgo / 60 / 1000);
                        String text =
                                text = (minutesAgo < 1) ? getString(R.string.last_step_detected_less_minute_ago) :
                                        getString(R.string.last_step_detected_minutes_ago,
                                                getResources().getQuantityString(R.plurals.minutes, minutesAgo, minutesAgo));
                        if (restStepsNeed >= 0) {
                            text += " " + getString(R.string.steps_to_complete_rest, restStepsNeed);
                        }
                        statusText.setText(text);
                    }
                    break;
                    case Forever:
                        statusText.setText(R.string.notification_suspended_forever);
                        break;
                    case DNDHours:
                        statusText.setText(R.string.notification_suspended_dnd_hours);
                        break;
                    case Timeout:
                        statusText.setText(R.string.notification_suspended_temporarily);
                        break;
                    case UntilTomorrow:
                        statusText.setText(R.string.notification_suspended_until_tomorrow);
                        break;
                    case NextOverstay:
                        statusText.setText(R.string.notification_suspended_until_next_overstay);
                        break;
                    case DNDDriving:
                        statusText.setText(R.string.notification_suspended_dnd_driving);
                        break;
                }

                // progress bar
                if (restStepsNeed >= 0 && suspendState == State.SuspendState.NotSuspended) {
                    statusProgress.setVisibility(View.VISIBLE);
                    statusProgress.setProgress(mSettingsManager.readMinimumRestSteps() - (int) restStepsNeed);
                    statusProgress.setMax(mSettingsManager.readMinimumRestSteps());
                } else {
                    statusProgress.setVisibility(View.GONE);
                }


                // Image
                if (suspendState == State.SuspendState.DNDDriving) {
                    statusImage.setImageResource(R.drawable.cat_driving);
                } else if (suspendState == State.SuspendState.NotSuspended) {
                    if (isMoving) {
                        statusImage.setImageResource(R.drawable.cat_walking);
                    } else {
                        statusImage.setImageResource(R.drawable.cat_sitting);
                    }
                } else {
                    statusImage.setImageResource(R.drawable.cat_lying);
                }

                // Button
                switch (suspendState) {
                    case NotSuspended: {
                        statusButtons.setVisibility(View.VISIBLE);
                        statusButton.setText(R.string.snooze_notifications_select);
                        statusButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                SnoozePickerFragment fragment = new SnoozePickerFragment();
                                fragment.show(getActivity().getFragmentManager(), "snoozePicker");
                            }
                        });
                    }
                    break;
                    case Forever:
                    case Timeout:
                    case UntilTomorrow:
                    case NextOverstay:
                        statusButtons.setVisibility(View.VISIBLE);
                        statusButton.setText(R.string.enable_notifications);
                        statusButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                MYAServiceConnectMobile.sendToServiceSuspend(getActivity(), State.SuspendState.NotSuspended, 0);
                            }
                        });
                        break;
                    case DNDHours:
                    case DNDDriving:
                        statusButtons.setVisibility(View.GONE);
                        break;
                }
            }
        });

        mSendRequestTimer = new Timer();

        mSendRequestTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mMYAServiceConnectMobile.sendStateRequest();
            }
        }, SEND_REQUEST_PERIOD_MS, SEND_REQUEST_PERIOD_MS);
    }

    @Override
    public void onStop() {
        super.onStop();

        mSendRequestTimer.cancel();
        mSendRequestTimer.purge();

        mMYAServiceConnectMobile.destroy();
    }
}
