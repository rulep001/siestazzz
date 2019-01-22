package com.ucr.buzuka.siestazzz;

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;
import java.util.Calendar;
import java.util.Date;

public class TimePickerFragment extends DialogFragment {

  public static final String EXTRA_TIME = "com.ucr.buzuka.android.siestazzz.time";

  private static final String ARG_TIME = "time";

  private TimePicker mTimePicker;

  // Allow new instance of date picker fragment to be called with current date.
  public static TimePickerFragment newInstance(Date time) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TIME, time);

    TimePickerFragment fragment = new TimePickerFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @TargetApi(Build.VERSION_CODES.N)
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    checkNotNull(getArguments());
    Date time = (Date) getArguments().getSerializable(ARG_TIME);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);

    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);

    View v = LayoutInflater.from(getActivity())
        .inflate(R.layout.dialog_time, null);

    mTimePicker = (TimePicker) v.findViewById(R.id.dialog_time_picker);
    mTimePicker.setHour(hour);
    mTimePicker.setMinute(minute);

    return new AlertDialog.Builder(getActivity())
        .setView(v)
        .setTitle(R.string.time_picker_title)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            int hour = mTimePicker.getHour();
            int minute = mTimePicker.getMinute();

            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, hour);
            //time.set(Calendar.HOUR, hour);
            time.set(Calendar.MINUTE, minute);
            Date retTime = time.getTime();

            sendResult(Activity.RESULT_OK, retTime);
          }
        })
        .create();
  }

  private void sendResult(int resultCode, Date time) {
    if (getTargetFragment() == null) {
      return;
    }

    Intent intent = new Intent();
    intent.putExtra(EXTRA_TIME, time);

    getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
  }
}
