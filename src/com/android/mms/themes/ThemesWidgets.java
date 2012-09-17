
package com.android.mms.themes;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.mms.R;
import com.android.mms.ui.ColorPickerPreference;

public class ThemesWidgets extends PreferenceActivity implements
            OnPreferenceChangeListener {

    // Menu entries
    private static final int THEMES_RESTORE_DEFAULTS = 1;

    // widget layout style
    public static final String PREF_WIDGET_LAYOUT = "pref_widget_layout";

    /** With the way remoteviews works couldn't figure out a way yet to include this
     *  but leaving code in just in case for now.
     */
    // read/unread backgrounds
    // public static final String PREF_WIDGET_READ_BG = "pref_widget_bg_read";
    // public static final String PREF_WIDGET_UNREAD_BG = "pref_widget_bg_unread";

    // widget text colors
    public static final String PREF_SENDERS_TEXTCOLOR_READ = "pref_senders_textcolor_read";
    public static final String PREF_SENDERS_TEXTCOLOR_UNREAD = "pref_senders_textcolor_unread";
    public static final String PREF_SUBJECT_TEXTCOLOR_READ = "pref_subject_textcolor_read";
    public static final String PREF_SUBJECT_TEXTCOLOR_UNREAD = "pref_subject_textcolor_unread";
    public static final String PREF_DATE_TEXTCOLOR_READ = "pref_date_textcolor_read";
    public static final String PREF_DATE_TEXTCOLOR_UNREAD = "pref_date_textcolor_unread";

    ColorPickerPreference mSendersRead;
    ColorPickerPreference mSendersUnread;
    ColorPickerPreference mSubjectRead;
    ColorPickerPreference mSubjectUnread;
    ColorPickerPreference mDateRead;
    ColorPickerPreference mDateUnread;
    // ColorPickerPreference mWidgetBgRead;
    // ColorPickerPreference mWidgetBgUnread;
    ListPreference mWidgetLayout;

    private SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadThemePrefs();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    public void loadThemePrefs() {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_themes_widgets);

        PreferenceScreen prefSet = getPreferenceScreen();
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        mWidgetLayout = (ListPreference) findPreference(PREF_WIDGET_LAYOUT);
        mWidgetLayout.setOnPreferenceChangeListener(this);
        mWidgetLayout.setSummary(mWidgetLayout.getEntry());

        mSendersRead = (ColorPickerPreference) findPreference(PREF_SENDERS_TEXTCOLOR_READ);
        mSendersRead.setOnPreferenceChangeListener(this);

        mSendersUnread = (ColorPickerPreference) findPreference(PREF_SENDERS_TEXTCOLOR_UNREAD);
        mSendersUnread.setOnPreferenceChangeListener(this);

        mSubjectRead = (ColorPickerPreference) findPreference(PREF_SUBJECT_TEXTCOLOR_READ);
        mSubjectRead.setOnPreferenceChangeListener(this);

        mSubjectUnread = (ColorPickerPreference) findPreference(PREF_SUBJECT_TEXTCOLOR_UNREAD);
        mSubjectUnread.setOnPreferenceChangeListener(this);

        mDateRead = (ColorPickerPreference) findPreference(PREF_DATE_TEXTCOLOR_READ);
        mDateRead.setOnPreferenceChangeListener(this);

        mDateUnread = (ColorPickerPreference) findPreference(PREF_DATE_TEXTCOLOR_UNREAD);
        mDateUnread.setOnPreferenceChangeListener(this);

        /* mWidgetBgRead = (ColorPickerPreference) findPreference(PREF_WIDGET_READ_BG);
        mWidgetBgRead.setOnPreferenceChangeListener(this);

        mWidgetBgUnread = (ColorPickerPreference) findPreference(PREF_WIDGET_UNREAD_BG);
        mWidgetBgUnread.setOnPreferenceChangeListener(this); */
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mSendersRead) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSendersRead.setSummary(hex);

        } else if (preference == mSendersUnread) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSendersUnread.setSummary(hex);

        } else if (preference == mSubjectRead) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSubjectRead.setSummary(hex);

        } else if (preference == mSubjectUnread) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSubjectUnread.setSummary(hex);

        } else if (preference == mDateRead) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mDateRead.setSummary(hex);

        } else if (preference == mDateUnread) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mDateUnread.setSummary(hex);

        /* } else if (preference == mWidgetBgRead) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mWidgetBgRead.setSummary(hex);

        } else if (preference == mWidgetBgUnread) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mWidgetBgUnread.setSummary(hex); */

        } else if (preference == mWidgetLayout) {
            int index = mWidgetLayout.findIndexOfValue((String) newValue);
            mWidgetLayout.setSummary(mWidgetLayout.getEntries()[index]);
            return true;

        }
        return result;
    }

    private void restoreThemeWidgetsDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        loadThemePrefs();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, THEMES_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case THEMES_RESTORE_DEFAULTS:
                restoreThemeWidgetsDefaultPreferences();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }
}
