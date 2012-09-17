
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.android.mms.R;
import com.android.mms.ui.ColorPickerPreference;

public class ThemesMessageList extends PreferenceActivity implements
            Preference.OnPreferenceChangeListener {
    // Menu entries
    private static final int THEMES_RESTORE_DEFAULTS = 1;
    private static final int THEMES_CUSTOM_IMAGE_DELETE = 0;

    // Layout Style
    public static final String PREF_TEXT_CONV_LAYOUT = "pref_text_conv_layout";

    // Msg background
    public static final String PREF_MESSAGE_BG = "pref_message_bg";

    private static final String CUSTOM_IMAGE = "message_list_image.jpg";
    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int SELECT_WALLPAPER = 5;

    // Bubble types
    public static final String PREF_BUBBLE_TYPE = "pref_bubble_type";
    public static final String PREF_BUBBLE_FILL_PARENT = "pref_bubble_fill_parent";

    // Checkbox preferences
    public static final String PREF_USE_CONTACT = "pref_use_contact";
    public static final String PREF_SHOW_AVATAR = "pref_show_avatar";

    // Colorpicker preferences send
    public static final String PREF_SENT_TEXTCOLOR = "pref_sent_textcolor";
    public static final String PREF_SENT_CONTACT_COLOR = "pref_sent_contact_color";
    public static final String PREF_SENT_DATE_COLOR = "pref_sent_date_color";
    public static final String PREF_SENT_TEXT_BG = "pref_sent_text_bg";
    public static final String PREF_SENT_SMILEY = "pref_sent_smiley";
    // Colorpicker preferences received
    public static final String PREF_RECV_TEXTCOLOR = "pref_recv_textcolor";
    public static final String PREF_RECV_CONTACT_COLOR = "pref_recv_contact_color";
    public static final String PREF_RECV_DATE_COLOR = "pref_recv_date_color";
    public static final String PREF_RECV_TEXT_BG = "pref_recv_text_bg";
    public static final String PREF_RECV_SMILEY = "pref_recv_smiley";

    // message background
    ColorPickerPreference mMessageBackground;
    // send
    ColorPickerPreference mSentTextColor;
    ColorPickerPreference mSentDateColor;
    ColorPickerPreference mSentContactColor;
    ColorPickerPreference mSentTextBgColor;
    ColorPickerPreference mSentSmiley;
    // received
    ColorPickerPreference mRecvTextColor;
    ColorPickerPreference mRecvContactColor;
    ColorPickerPreference mRecvDateColor;
    ColorPickerPreference mRecvTextBgColor;
    ColorPickerPreference mRecvSmiley;

    private CheckBoxPreference mUseContact;
    private CheckBoxPreference mShowAvatar;
    private CheckBoxPreference mBubbleFillParent;
    private ListPreference mTextLayout;
    private ListPreference mBubbleType;
    private Preference mCustomImage;
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
        addPreferencesFromResource(R.xml.preferences_themes_msglist);

        PreferenceScreen prefSet = getPreferenceScreen();
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        mUseContact = (CheckBoxPreference) prefSet.findPreference(PREF_USE_CONTACT);
        mShowAvatar = (CheckBoxPreference) prefSet.findPreference(PREF_SHOW_AVATAR);
        mBubbleFillParent = (CheckBoxPreference) prefSet.findPreference(PREF_BUBBLE_FILL_PARENT);

        mTextLayout = (ListPreference) findPreference(PREF_TEXT_CONV_LAYOUT);
        mTextLayout.setOnPreferenceChangeListener(this);
        mTextLayout.setSummary(mTextLayout.getEntry());

        mBubbleType = (ListPreference) findPreference(PREF_BUBBLE_TYPE);
        mBubbleType.setOnPreferenceChangeListener(this);
        mBubbleType.setSummary(mBubbleType.getEntry());

        mCustomImage = findPreference("pref_custom_image");

        mMessageBackground = (ColorPickerPreference) findPreference(PREF_MESSAGE_BG);
        mMessageBackground.setOnPreferenceChangeListener(this);

        mSentTextColor = (ColorPickerPreference) findPreference(PREF_SENT_TEXTCOLOR);
        mSentTextColor.setOnPreferenceChangeListener(this);

        mSentContactColor = (ColorPickerPreference) findPreference(PREF_SENT_TEXTCOLOR);
        mSentContactColor.setOnPreferenceChangeListener(this);

        mSentDateColor = (ColorPickerPreference) findPreference(PREF_SENT_TEXTCOLOR);
        mSentDateColor.setOnPreferenceChangeListener(this);

        mSentTextBgColor = (ColorPickerPreference) findPreference(PREF_SENT_TEXTCOLOR);
        mSentTextBgColor.setOnPreferenceChangeListener(this);

        mSentSmiley = (ColorPickerPreference) findPreference(PREF_SENT_SMILEY);
        mSentSmiley.setOnPreferenceChangeListener(this);

        mRecvTextColor = (ColorPickerPreference) findPreference(PREF_RECV_TEXTCOLOR);
        mRecvTextColor.setOnPreferenceChangeListener(this);

        mRecvContactColor = (ColorPickerPreference) findPreference(PREF_RECV_TEXTCOLOR);
        mRecvContactColor.setOnPreferenceChangeListener(this);

        mRecvDateColor = (ColorPickerPreference) findPreference(PREF_RECV_TEXTCOLOR);
        mRecvDateColor.setOnPreferenceChangeListener(this);

        mRecvTextBgColor = (ColorPickerPreference) findPreference(PREF_RECV_TEXT_BG);
        mRecvTextBgColor.setOnPreferenceChangeListener(this);

        mRecvSmiley = (ColorPickerPreference) findPreference(PREF_RECV_SMILEY);
        mRecvSmiley.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mMessageBackground) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mMessageBackground.setSummary(hex);


        } else if (preference == mSentTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSentTextColor.setSummary(hex);

        } else if (preference == mSentContactColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSentContactColor.setSummary(hex);

        } else if (preference == mSentDateColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSentDateColor.setSummary(hex);

        } else if (preference == mSentTextBgColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSentTextBgColor.setSummary(hex);

        } else if (preference == mSentSmiley) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mSentSmiley.setSummary(hex);

        } else if (preference == mRecvTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mRecvTextColor.setSummary(hex);

        } else if (preference == mRecvContactColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mRecvContactColor.setSummary(hex);

        } else if (preference == mRecvDateColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mRecvDateColor.setSummary(hex);

        } else if (preference == mRecvTextBgColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mRecvTextBgColor.setSummary(hex);

        } else if (preference == mRecvSmiley) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            mRecvSmiley.setSummary(hex);

        } else if (preference == mTextLayout) {
            int index = mTextLayout.findIndexOfValue((String) newValue);
            mTextLayout.setSummary(mTextLayout.getEntries()[index]);
            return true;

        } else if (preference == mBubbleType) {
            int index = mBubbleType.findIndexOfValue((String) newValue);
            mBubbleType.setSummary(mBubbleType.getEntries()[index]);
            return true;

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

        } else if (preference == mUseContact) {
            value = mUseContact.isChecked();

        } else if (preference == mShowAvatar) {
            value = mShowAvatar.isChecked();

        } else if (preference == mBubbleFillParent) {
            value = mShowAvatar.isChecked();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreThemeMessageListDefaultPreferences() {
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
                restoreThemeMessageListDefaultPreferences();
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
