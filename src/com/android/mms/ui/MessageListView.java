/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.File;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.*;
import android.widget.ImageView.ScaleType;

import com.android.mms.R;
import com.android.mms.themes.ThemesMessageList;

public final class MessageListView extends ListView {

    private OnSizeChangedListener mOnSizeChangedListener;

    private SharedPreferences sp;

    public MessageListView(Context context) {
        super(context);
    }

    public MessageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_C:
            MessageListItem view = (MessageListItem)getSelectedView();
            if (view == null) {
                break;
            }
            MessageItem item = view.getMessageItem();
            if (item != null && item.isSms()) {
                ClipboardManager clip =
                    (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setText(item.mBody);
                return true;
            }
            break;
        }

        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
    }

    /**
     * Set the listener which will be triggered when the size of
     * the view is changed.
     */
    void setOnSizeChangedListener(OnSizeChangedListener l) {
        mOnSizeChangedListener = l;
    }

    public interface OnSizeChangedListener {
        void onSizeChanged(int width, int height, int oldWidth, int oldHeight);
    }
}

