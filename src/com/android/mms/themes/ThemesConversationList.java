
package com.android.mms.themes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.text.Spannable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.mms.R;
import com.android.mms.ui.ColorPickerPreference;

public class ThemesConversationList extends PreferenceActivity implements
            OnPreferenceChangeListener {
    // Menu entries
    private static final int THEMES_RESTORE_DEFAULTS = 1;
    private static final int THEMES_CUSTOM_IMAGE_DELETE = 0;

    // background
    public static final String PREF_CONV_LIST_BG = "pref_conv_list_bg";
    private static final String CUSTOM_IMAGE = "conversation_list_image.jpg";
    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int SELECT_WALLPAPER = 5;

    // Colorpicker read
    public static final String PREF_READ_BG = "pref_read_bg";
    public static final String PREF_READ_CONTACT = "pref_read_contact";
    public static final String PREF_READ_SUBJECT = "pref_read_subject";
    public static final String PREF_READ_DATE = "pref_read_date";
    public static final String PREF_READ_COUNT = "pref_read_count";
    public static final String PREF_READ_SMILEY = "pref_read_smiley";

    // Colorpicker unread
    public static final String PREF_UNREAD_BG = "pref_unread_bg";
    public static final String PREF_UNREAD_CONTACT = "pref_unread_contact";
    public static final String PREF_UNREAD_SUBJECT = "pref_unread_subject";
    public static final String PREF_UNREAD_DATE = "pref_unread_date";
    public static final String PREF_UNREAD_COUNT = "pref_unread_count";
    public static final String PREF_UNREAD_SMILEY = "pref_unread_smiley";

    // background
    ColorPickerPreference mConvListBackground;
    // conv list read
    ColorPickerPreference mReadBg;
    ColorPickerPreference mReadContact;
    ColorPickerPreference mReadSubject;
    ColorPickerPreference mReadDate;
    ColorPickerPreference mReadCount;
    ColorPickerPreference mReadSmiley;
    // conv list read
    ColorPickerPreference mUnreadBg;
    ColorPickerPreference mUnreadContact;
    ColorPickerPreference mUnreadSubject;
    ColorPickerPreference mUnreadDate;
    ColorPickerPreference mUnreadCount;
    ColorPickerPreference mUnreadSmiley;

    private Preference mCustomImage;
    private SharedPreferences sp;

    private int seekbarProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen prefSet = getPreferenceScreen();
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        loadThemePrefs();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    public void loadThemePrefs() {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_themes_convlist);

        mCustomImage = findPreference("pref_custom_image");

        mConvListBackground = (ColorPickerPreference) findPreference(PREF_CONV_LIST_BG);
        mConvListBackground.setOnPreferenceChangeListener(this);

        mReadBg = (ColorPickerPreference) findPreference(PREF_READ_BG);
        mReadBg.setOnPreferenceChangeListener(this);

        mReadContact = (ColorPickerPreference) findPreference(PREF_READ_CONTACT);
        mReadContact.setOnPreferenceChangeListener(this);

        mReadCount = (ColorPickerPreference) findPreference(PREF_READ_COUNT);
        mReadCount.setOnPreferenceChangeListener(this);

        mReadDate = (ColorPickerPreference) findPreference(PREF_READ_DATE);
        mReadDate.setOnPreferenceChangeListener(this);

        mReadSubject = (ColorPickerPreference) findPreference(PREF_READ_SUBJECT);
        mReadSubject.setOnPreferenceChangeListener(this);

        mReadSmiley = (ColorPickerPreference) findPreference(PREF_READ_SMILEY);
        mReadSmiley.setOnPreferenceChangeListener(this);

        mUnreadBg = (ColorPickerPreference) findPreference(PREF_UNREAD_BG);
        mUnreadBg.setOnPreferenceChangeListener(this);

        mUnreadContact = (ColorPickerPreference) findPreference(PREF_UNREAD_CONTACT);
        mUnreadContact.setOnPreferenceChangeListener(this);

        mUnreadCount = (ColorPickerPreference) findPreference(PREF_UNREAD_COUNT);
        mUnreadCount.setOnPreferenceChangeListener(this);

        mUnreadDate = (ColorPickerPreference) findPreference(PREF_UNREAD_DATE);
        mUnreadDate.setOnPreferenceChangeListener(this);

        mUnreadSubject = (ColorPickerPreference) findPreference(PREF_UNREAD_SUBJECT);
        mUnreadSubject.setOnPreferenceChangeListener(this);

        mUnreadSmiley = (ColorPickerPreference) findPreference(PREF_UNREAD_SMILEY);
        mUnreadSmiley.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mConvListBackground) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mConvListBackground.setSummary(hex);

        } else if (preference == mReadBg) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mReadBg.setSummary(hex);

        } else if (preference == mReadContact) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mReadContact.setSummary(hex);

        } else if (preference == mReadCount) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mReadCount.setSummary(hex);

        } else if (preference == mReadDate) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mReadDate.setSummary(hex);

        } else if (preference == mReadSubject) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mReadSubject.setSummary(hex);

        } else if (preference == mReadSmiley) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mReadSmiley.setSummary(hex);

        } else if (preference == mUnreadBg) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mUnreadBg.setSummary(hex);

        } else if (preference == mUnreadContact) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mUnreadContact.setSummary(hex);

        } else if (preference == mUnreadCount) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mUnreadCount.setSummary(hex);

        } else if (preference == mUnreadDate) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mUnreadDate.setSummary(hex);

        } else if (preference == mUnreadSubject) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mUnreadSubject.setSummary(hex);

        } else if (preference == mUnreadSmiley) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mUnreadSmiley.setSummary(hex);

        }
        return result;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mCustomImage) {
            Display display = this.getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = this.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                    .getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("outputX", width);
            intent.putExtra("outputY", height);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getCustomImageExternalUri());
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

            startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreThemeConversationListDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        loadThemePrefs();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, THEMES_RESTORE_DEFAULTS, 0, R.string.restore_default);
        menu.add(0, THEMES_CUSTOM_IMAGE_DELETE, 0, R.string.delete_custom_image);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case THEMES_RESTORE_DEFAULTS:
                restoreThemeConversationListDefaultPreferences();
                return true;

            case THEMES_CUSTOM_IMAGE_DELETE:
                deleteCustomImage();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    private void deleteCustomImage() {
        this.deleteFile(CUSTOM_IMAGE);
    }

    private Uri getCustomImageExternalUri() {
        File dir = this.getExternalCacheDir();
        File wallpaper = new File(dir, CUSTOM_IMAGE);

        return Uri.fromFile(wallpaper);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_WALLPAPER) {

                FileOutputStream wallpaperStream = null;
                try {
                    wallpaperStream = this.openFileOutput(CUSTOM_IMAGE,
                            Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }

                Uri selectedImageUri = getCustomImageExternalUri();
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, wallpaperStream);
            }
        }
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
