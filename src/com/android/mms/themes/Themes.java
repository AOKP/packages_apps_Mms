
package com.android.mms.themes;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
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

public class Themes extends PreferenceActivity implements
            OnPreferenceChangeListener {

    // add Signature
    public static final String PREF_SIGNATURE = "pref_signature";

    // restart mms
    private static final String PREF_RESTART_MMS = "pref_restart_mms";

    private EditTextPreference mAddSignature;
    private Preference mRestartMms;
    private SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_themes);

        PreferenceScreen prefSet = getPreferenceScreen();
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        mRestartMms = (Preference) findPreference(PREF_RESTART_MMS);

        mAddSignature = (EditTextPreference) findPreference(PREF_SIGNATURE);
        mAddSignature.setOnPreferenceChangeListener(this);
        mAddSignature.setText(sp.getString(PREF_SIGNATURE,""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mAddSignature) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(PREF_SIGNATURE, (String) newValue);
            editor.commit();
        }
        return result;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mRestartMms) {
            Process.killProcess(Process.myPid());
            restartFirstActivity();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restartFirstActivity() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(
                getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity(i);
    }
}
