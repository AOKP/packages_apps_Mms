/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchRecentSuggestions;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.templates.TemplatesListActivity;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.Recycler;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity
            implements OnPreferenceChangeListener {
    // Symbolic names for the keys used for preference lookup
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    public static final String PRIORITY                 = "pref_key_mms_priority";
    public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    public static final String NOTIFICATION_VIBRATE     = "pref_key_vibrate";
    public static final String NOTIFICATION_VIBRATE_WHEN= "pref_key_vibrateWhen";
    public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    public static final String AUTO_DELETE              = "pref_key_auto_delete";
    public static final String GROUP_MMS_MODE           = "pref_key_mms_group_mms";

    // Unicode
    public static final String UNICODE_STRIPPING            = "pref_key_unicode_stripping";
    public static final String UNICODE_STRIPPING_VALUE      = "pref_key_unicode_stripping_value";
    public static final int UNICODE_STRIPPING_LEAVE_INTACT  = 0;
    public static final int UNICODE_STRIPPING_NON_DECODABLE = 1;

    // Split sms
    public static final String SMS_SPLIT_COUNTER        = "pref_key_sms_split_counter";

    // Templates
    public static final String MANAGE_TEMPLATES         = "pref_key_templates_manage";
    public static final String SHOW_GESTURE             = "pref_key_templates_show_gesture";
    public static final String GESTURE_SENSITIVITY      = "pref_key_templates_gestures_sensitivity";
    public static final String GESTURE_SENSITIVITY_VALUE = "pref_key_templates_gestures_sensitivity_value";

    // Timestamps
    public static final String FULL_TIMESTAMP            = "pref_key_mms_full_timestamp";
    public static final String SENT_TIMESTAMP            = "pref_key_mms_use_sent_timestamp";

    // Privacy mode
    public static final String PRIVACY_MODE_ENABLED = "pref_key_enable_privacy_mode";

    // Keyboard input type
    public static final String INPUT_TYPE                = "pref_key_mms_input_type";

    // QuickMessage
    public static final String QUICKMESSAGE_ENABLED      = "pref_key_quickmessage";
    public static final String QM_LOCKSCREEN_ENABLED     = "pref_key_qm_lockscreen";
    public static final String QM_CLOSE_ALL_ENABLED      = "pref_key_close_all";
    public static final String QM_DARK_THEME_ENABLED     = "pref_dark_theme";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS    = 1;

    // Preferences for enabling and disabling SMS
    private Preference mSmsDisabledPref;
    private Preference mSmsEnabledPref;

    private PreferenceCategory mStoragePrefCategory;
    private PreferenceCategory mSmsPrefCategory;
    private PreferenceCategory mMmsPrefCategory;
    private PreferenceCategory mNotificationPrefCategory;

    private Preference mSmsLimitPref;
    private Preference mSmsDeliveryReportPref;
    private CheckBoxPreference mSmsSplitCounterPref;
    private Preference mMmsLimitPref;
    private Preference mMmsDeliveryReportPref;
    private Preference mMmsGroupMmsPref;
    private Preference mMmsReadReportPref;
    private Preference mManageSimPref;
    private Preference mClearHistoryPref;
    private CheckBoxPreference mVibratePref;
    private CheckBoxPreference mEnableNotificationsPref;
    private CheckBoxPreference mEnablePrivacyModePref;
    private CheckBoxPreference mMmsAutoRetrievialPref;
    private RingtonePreference mRingtonePref;
    private Recycler mSmsRecycler;
    private Recycler mMmsRecycler;
    private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;

    // Templates
    private Preference mManageTemplate;
    private ListPreference mGestureSensitivity;
    private ListPreference mUnicodeStripping;
    private CharSequence[] mUnicodeStrippingEntries;

    // Keyboard input type
    private ListPreference mInputTypePref;
    private CharSequence[] mInputTypeEntries;
    private CharSequence[] mInputTypeValues;

    // Whether or not we are currently enabled for SMS. This field is updated in onResume to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;

    // QuickMessage
    private CheckBoxPreference mEnableQuickMessagePref;
    private CheckBoxPreference mEnableQmLockscreenPref;
    private CheckBoxPreference mEnableQmCloseAllPref;
    private CheckBoxPreference mEnableQmDarkThemePref;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        loadPrefs();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }

        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
        registerListeners();
        updateSmsEnabledState();
    }

    private void updateSmsEnabledState() {
        // Show the right pref (SMS Disabled or SMS Enabled)
        PreferenceScreen prefRoot = (PreferenceScreen)findPreference("pref_key_root");
        if (!mIsSmsEnabled) {
            prefRoot.addPreference(mSmsDisabledPref);
            prefRoot.removePreference(mSmsEnabledPref);
        } else {
            prefRoot.removePreference(mSmsDisabledPref);
            prefRoot.addPreference(mSmsEnabledPref);
        }

        // Enable or Disable the settings as appropriate
        mStoragePrefCategory.setEnabled(mIsSmsEnabled);
        mSmsPrefCategory.setEnabled(mIsSmsEnabled);
        mMmsPrefCategory.setEnabled(mIsSmsEnabled);
        mNotificationPrefCategory.setEnabled(mIsSmsEnabled);
    }

    private void loadPrefs() {
        addPreferencesFromResource(R.xml.preferences);

        mSmsDisabledPref = findPreference("pref_key_sms_disabled");
        mSmsEnabledPref = findPreference("pref_key_sms_enabled");

        mStoragePrefCategory = (PreferenceCategory)findPreference("pref_key_storage_settings");
        mSmsPrefCategory = (PreferenceCategory)findPreference("pref_key_sms_settings");
        mMmsPrefCategory = (PreferenceCategory)findPreference("pref_key_mms_settings");
        mNotificationPrefCategory =
                (PreferenceCategory)findPreference("pref_key_notification_settings");

        mManageSimPref = findPreference("pref_key_manage_sim_messages");
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
        mSmsDeliveryReportPref = findPreference("pref_key_sms_delivery_reports");
        mSmsSplitCounterPref = (CheckBoxPreference) findPreference("pref_key_sms_split_counter");
        mMmsDeliveryReportPref = findPreference("pref_key_mms_delivery_reports");
        mMmsGroupMmsPref = findPreference("pref_key_mms_group_mms");
        mMmsReadReportPref = findPreference("pref_key_mms_read_reports");
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        mMmsAutoRetrievialPref = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mEnablePrivacyModePref = (CheckBoxPreference) findPreference(PRIVACY_MODE_ENABLED);
        mVibratePref = (CheckBoxPreference) findPreference(NOTIFICATION_VIBRATE);
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);

        mManageTemplate = findPreference(MANAGE_TEMPLATES);
        mGestureSensitivity = (ListPreference) findPreference(GESTURE_SENSITIVITY);
        mUnicodeStripping = (ListPreference) findPreference(UNICODE_STRIPPING);
        mUnicodeStrippingEntries = getResources().getTextArray(R.array.pref_unicode_stripping_entries);

        // QuickMessage
        mEnableQuickMessagePref = (CheckBoxPreference) findPreference(QUICKMESSAGE_ENABLED);
        mEnableQmLockscreenPref = (CheckBoxPreference) findPreference(QM_LOCKSCREEN_ENABLED);
        mEnableQmCloseAllPref = (CheckBoxPreference) findPreference(QM_CLOSE_ALL_ENABLED);
        mEnableQmDarkThemePref = (CheckBoxPreference) findPreference(QM_DARK_THEME_ENABLED);

        // Keyboard input type
        mInputTypePref = (ListPreference) findPreference(INPUT_TYPE);
        mInputTypeEntries = getResources().getTextArray(R.array.pref_entries_input_type);
        mInputTypeValues = getResources().getTextArray(R.array.pref_values_input_type);

        setMessagePreferences();
    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        loadPrefs();
        updateSmsEnabledState();

        // NOTE: After restoring preferences, the auto delete function (i.e. message recycler)
        // will be turned off by default. However, we really want the default to be turned on.
        // Because all the prefs are cleared, that'll cause:
        // ConversationList.runOneTimeStorageLimitCheckForLegacyMessages to get executed the
        // next time the user runs the Messaging app and it will either turn on the setting
        // by default, or if the user is over the limits, encourage them to turn on the setting
        // manually.
    }

    private void setMessagePreferences() {
        if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
            // No SIM card, remove the SIM-related prefs
            mSmsPrefCategory.removePreference(mManageSimPref);
        }

        if (!MmsConfig.getSMSDeliveryReportsEnabled()) {
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
            if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                getPreferenceScreen().removePreference(mSmsPrefCategory);
            }
        }

        if (!MmsConfig.getSplitSmsEnabled()) {
            // SMS Split disabled, remove SplitCounter pref
            PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
            smsCategory.removePreference(mSmsSplitCounterPref);
        }

        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            getPreferenceScreen().removePreference(mMmsPrefCategory);

            mStoragePrefCategory.removePreference(findPreference("pref_key_mms_delete_limit"));
        } else {
            if (!MmsConfig.getMMSDeliveryReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsDeliveryReportPref);
            }
            if (!MmsConfig.getMMSReadReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsReadReportPref);
            }
            // If the phone's SIM doesn't know it's own number, disable group mms.
            if (!MmsConfig.getGroupMmsEnabled() ||
                    TextUtils.isEmpty(MessageUtils.getLocalNumber())) {
                mMmsPrefCategory.removePreference(mMmsGroupMmsPref);
            }
        }

        setEnabledNotificationsPref();

        // Privacy mode
        setEnabledPrivacyModePref();

        // QuickMessage
        setEnabledQuickMessagePref();
        setEnabledQmLockscreenPref();
        setEnabledQmCloseAllPref();
        setEnabledQmDarkThemePref();

        // If needed, migrate vibration setting from the previous tri-state setting stored in
        // NOTIFICATION_VIBRATE_WHEN to the boolean setting stored in NOTIFICATION_VIBRATE.
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains(NOTIFICATION_VIBRATE_WHEN)) {
            String vibrateWhen = sharedPreferences.
                    getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
            boolean vibrate = "always".equals(vibrateWhen);
            SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
            prefsEditor.putBoolean(NOTIFICATION_VIBRATE, vibrate);
            prefsEditor.remove(NOTIFICATION_VIBRATE_WHEN);  // remove obsolete setting
            prefsEditor.apply();
            mVibratePref.setChecked(vibrate);
        }

        mManageTemplate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(MessagingPreferenceActivity.this,
                        TemplatesListActivity.class);
                startActivity(intent);
                return false;
            }
        });

        String gestureSensitivity = String.valueOf(sharedPreferences.getInt(GESTURE_SENSITIVITY_VALUE, 3));
        mGestureSensitivity.setSummary(gestureSensitivity);
        mGestureSensitivity.setValue(gestureSensitivity);
        mGestureSensitivity.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.parseInt((String) newValue);
                sharedPreferences.edit().putInt(GESTURE_SENSITIVITY_VALUE, value).commit();
                mGestureSensitivity.setSummary(String.valueOf(value));
                return true;
            }
        });

        int unicodeStripping = sharedPreferences.getInt(UNICODE_STRIPPING_VALUE, UNICODE_STRIPPING_LEAVE_INTACT);
        mUnicodeStripping.setValue(String.valueOf(unicodeStripping));
        mUnicodeStripping.setSummary(mUnicodeStrippingEntries[unicodeStripping]);
        mUnicodeStripping.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.parseInt((String) newValue);
                sharedPreferences.edit().putInt(UNICODE_STRIPPING_VALUE, value).commit();
                mUnicodeStripping.setSummary(mUnicodeStrippingEntries[value]);
                return true;
            }
        });

        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();

        String soundValue = sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
        setRingtoneSummary(soundValue);

        // Read the input type value and set the summary
        String inputType = sharedPreferences.getString(MessagingPreferenceActivity.INPUT_TYPE,
                Integer.toString(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE));
        mInputTypePref.setValue(inputType);
        adjustInputTypeSummary(mInputTypePref.getValue());
        mInputTypePref.setOnPreferenceChangeListener(this);
    }

    private void setRingtoneSummary(String soundValue) {
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        mRingtonePref.setSummary(tone != null ? tone.getTitle(this)
                : getResources().getString(R.string.silent_ringtone));
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    private void setEnabledPrivacyModePref() {
        // The "enable privacy mode" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        boolean isPrivacyModeEnabled = getPrivacyModeEnabled(this);
        mEnablePrivacyModePref.setChecked(isPrivacyModeEnabled);

        // Enable/Disable the "enable quickmessage" setting according to
        // the "enable privacy mode" setting state
        mEnableQuickMessagePref.setEnabled(!isPrivacyModeEnabled);

        // Enable/Disable the "enable dark theme" setting according to
        // the "enable privacy mode" setting state
        mEnableQmDarkThemePref.setEnabled(!isPrivacyModeEnabled);
    }

    private void setEnabledQuickMessagePref() {
        // The "enable quickmessage" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQuickMessagePref.setChecked(getQuickMessageEnabled(this));
    }

    private void setEnabledQmLockscreenPref() {
        // The "enable quickmessage on lock screen " setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQmLockscreenPref.setChecked(getQmLockscreenEnabled(this));
    }

    private void setEnabledQmCloseAllPref() {
        // The "enable close all" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQmCloseAllPref.setChecked(getQmCloseAllEnabled(this));
    }

    private void setEnabledQmDarkThemePref() {
        // The "Use dark theme" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQmDarkThemePref.setChecked(getQmDarkThemeEnabled(this));
    }

    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (mIsSmsEnabled) {
            menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mSmsLimitPref) {
            new NumberPickerDialog(this,
                    mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this),
                    mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_sms_delete).show();

        } else if (preference == mMmsLimitPref) {
            new NumberPickerDialog(this,
                    mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this),
                    mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_mms_delete).show();

        } else if (preference == mManageSimPref) {
            startActivity(new Intent(this, ManageSimMessages.class));

        } else if (preference == mClearHistoryPref) {
            showDialog(CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG);
            return true;

        } else if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);

        } else if (preference == mEnablePrivacyModePref) {
            // Update the actual "enable private mode" value that is stored in secure settings.
            enablePrivacyMode(mEnablePrivacyModePref.isChecked(), this);

            // Update "enable quickmessage" checkbox state
            mEnableQuickMessagePref.setEnabled(!mEnablePrivacyModePref.isChecked());

            // Update "enable dark theme" checkbox state
            mEnableQmDarkThemePref.setEnabled(!mEnablePrivacyModePref.isChecked());

        } else if (preference == mEnableQuickMessagePref) {
            // Update the actual "enable quickmessage" value that is stored in secure settings.
            enableQuickMessage(mEnableQuickMessagePref.isChecked(), this);

        } else if (preference == mEnableQmLockscreenPref) {
            // Update the actual "enable quickmessage on lockscreen" value that is stored in secure settings.
            enableQmLockscreen(mEnableQmLockscreenPref.isChecked(), this);

        } else if (preference == mEnableQmCloseAllPref) {
            // Update the actual "enable close all" value that is stored in secure settings.
            enableQmCloseAll(mEnableQmCloseAllPref.isChecked(), this);

        } else if (preference == mEnableQmDarkThemePref) {
            // Update the actual "enable dark theme" value that is stored in secure settings.
            enableQmDarkTheme(mEnableQmDarkThemePref.isChecked(), this);

        } else if (preference == mMmsAutoRetrievialPref) {
            if (mMmsAutoRetrievialPref.isChecked()) {
                startMmsDownload();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger the TransactionService to download any outstanding messages.
     */
    private void startMmsDownload() {
        startService(new Intent(TransactionService.ACTION_ENABLE_AUTO_RETRIEVE, null, this,
                TransactionService.class));
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setSmsDisplayLimit();
            }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setMmsDisplayLimit();
            }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG:
                return new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.confirm_clear_search_title)
                    .setMessage(R.string.confirm_clear_search_text)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SearchRecentSuggestions recent =
                                ((MmsApp)getApplication()).getRecentSuggestions();
                            if (recent != null) {
                                recent.clearHistory();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .create();
        }
        return super.onCreateDialog(id);
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, enabled);

        editor.apply();
    }

    public static boolean getPrivacyModeEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean privacyModeEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.PRIVACY_MODE_ENABLED, false);
        return privacyModeEnabled;
    }

    public static void enablePrivacyMode(boolean enabled, Context context) {
        // Store the value of private mode in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.PRIVACY_MODE_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQuickMessageEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean quickMessageEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QUICKMESSAGE_ENABLED, false);
        return quickMessageEnabled;
    }

    public static void enableQuickMessage(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QUICKMESSAGE_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQmLockscreenEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean qmLockscreenEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QM_LOCKSCREEN_ENABLED, false);
        return qmLockscreenEnabled;
    }

    public static void enableQmLockscreen(boolean enabled, Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QM_LOCKSCREEN_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQmCloseAllEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean qmCloseAllEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QM_CLOSE_ALL_ENABLED, false);
        return qmCloseAllEnabled;
    }

    public static void enableQmCloseAll(boolean enabled, Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QM_CLOSE_ALL_ENABLED, enabled);
        editor.apply();
    }

    public static void enableQmDarkTheme(boolean enabled, Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QM_DARK_THEME_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQmDarkThemeEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean qmDarkThemeEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QM_DARK_THEME_ENABLED, false);
        return qmDarkThemeEnabled;
    }

    private void registerListeners() {
        mRingtonePref.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mRingtonePref) {
            setRingtoneSummary((String)newValue);
            result = true;
        } else if (preference == mInputTypePref) {
            adjustInputTypeSummary((String)newValue);
            result = true;
        }
        return result;
    }

    private void adjustInputTypeSummary(String value) {
        int len = mInputTypeValues.length;
        for (int i = 0; i < len; i++) {
            if (mInputTypeValues[i].equals(value)) {
                mInputTypePref.setSummary(mInputTypeEntries[i]);
                return;
            }
        }
        mInputTypePref.setSummary(R.string.pref_keyboard_unknown);
    }

    // For the group mms feature to be enabled, the following must be true:
    //  1. the feature is enabled in mms_config.xml (currently on by default)
    //  2. the feature is enabled in the mms settings page
    //  3. the SIM knows its own phone number
    public static boolean getIsGroupMmsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean groupMmsPrefOn = prefs.getBoolean(
                MessagingPreferenceActivity.GROUP_MMS_MODE, true);
        return MmsConfig.getGroupMmsEnabled() &&
                groupMmsPrefOn &&
                !TextUtils.isEmpty(MessageUtils.getLocalNumber());
    }
}
