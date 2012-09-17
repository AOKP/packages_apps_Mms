
package com.android.mms.themes;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.mms.themes.ThemesMessageList;

class ThemesCustomImageMsgList extends FrameLayout {

    private final String TAG = "ThemesCustomImageMsgList";

    private final String CUSTOM_IMAGE_PATH = "/data/data/com.android.mms/files/message_list_image.jpg";

    private ImageView mCustomImage;

    private SharedPreferences sp;

    Bitmap bitmapCustomImage;

    public ThemesCustomImageMsgList(Context context, AttributeSet attrs) {
        super(context);

        sp = PreferenceManager.getDefaultSharedPreferences(context);

        setBackgroundColor(sp.getInt(ThemesMessageList.PREF_MESSAGE_BG, 0x00000000)); // Message List background
        setCustomImageBackground();
    }

    public void setCustomImageBackground() {
        File file = new File(CUSTOM_IMAGE_PATH);

        if (file.exists()) {
            mCustomImage = new ImageView(getContext());
            mCustomImage.setScaleType(ScaleType.CENTER_CROP);
            addView(mCustomImage, -1, -1);
            bitmapCustomImage = BitmapFactory.decodeFile(CUSTOM_IMAGE_PATH);
            Drawable d = new BitmapDrawable(getResources(), bitmapCustomImage);
            mCustomImage.setImageDrawable(d);
        } else {
            removeAllViews();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (bitmapCustomImage != null)
            bitmapCustomImage.recycle();

        System.gc();
        super.onDetachedFromWindow();
    }
}
