package com.android.mms.preferences;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.ui.MessagingPreferenceActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import android.widget.EditText;
import android.widget.Toast;

public class CustomVibrateListPreference extends ListPreference {
    private Context mContext;
    private String mCustomVibrate;
    private boolean mDialogShowing;
    private EditText mCustomVibrateOption;

    public CustomVibrateListPreference(Context context) {
        super(context);
        mContext = context;
    }

    public CustomVibrateListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            if (prefs.getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_PATTERN, null).equals("custom"))
                showDialog();
        }
    }

    private void showDialog() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.custom_vibrate_dialog, null);
        mCustomVibrateOption = (EditText) v.findViewById(R.id.CustomVibrateEditText);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mCustomVibrateOption.setText(prefs.getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_PATTERN_CUSTOM, "0,1200"));

        new AlertDialog.Builder(mContext).setIcon(
                android.R.drawable.ic_dialog_info).setTitle(
                R.string.pref_title_mms_notification_vibrate_custom).setView(v)
                .setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mDialogShowing = false;
                    }
                }).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mDialogShowing = false;
                            }
                        }).setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mDialogShowing = false;
                                SharedPreferences prefs = PreferenceManager
                                        .getDefaultSharedPreferences(mContext);
                                SharedPreferences.Editor settings = prefs
                                        .edit();
                                settings
                                        .putString(
                                                MessagingPreferenceActivity.NOTIFICATION_VIBRATE_PATTERN_CUSTOM,
                                                mCustomVibrateOption.getText().toString());
                                settings.commit();
                            }
                        }).show();
        mDialogShowing = true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (mDialogShowing) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            if (prefs.getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_PATTERN, null).equals("custom"))
                showDialog();
        }
    }

    @Override
    protected View onCreateDialogView() {
        mDialogShowing = false;
        return super.onCreateDialogView();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
