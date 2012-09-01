/*
 * Copyright (C) 2012 Adam K (SMSPopup)
 * Modifications Copyright (C) 2012 The CyanogenMod Project
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

package com.android.mms.quickmessage;

import android.content.Context;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class QmTextWatcher implements TextWatcher {
    private TextView mTextView;
    private ImageButton mSendButton;
    private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 30;

    public QmTextWatcher(Context context, TextView updateTextView, ImageButton sendButton) {
        mTextView = updateTextView;
        mSendButton = sendButton;
    }

    public QmTextWatcher(Context context, TextView updateTextView) {
        mTextView = updateTextView;
        mSendButton = null;
    }

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        getQuickReplyCounterText(s, mTextView, mSendButton);
    }

    public static void getQuickReplyCounterText(CharSequence s, TextView mTextView,
            ImageButton mSendButton) {
        if (mSendButton != null) {
            if (s.length() > 0) {
                mSendButton.setEnabled(true);
            } else {
                mSendButton.setEnabled(false);
            }
        }

        if (s.length() < (80 - CHARS_REMAINING_BEFORE_COUNTER_SHOWN)) {
            mTextView.setVisibility(View.GONE);
            return;
        }

        /*
         * SmsMessage.calculateLength returns an int[4] with: int[0] being the number of SMS's
         * required, int[1] the number of code units used, int[2] is the number of code units
         * remaining until the next message. int[3] is the encoding type that should be used for the
         * message.
         */
        int[] params = SmsMessage.calculateLength(s, false);
        int msgCount = params[0];
        int remainingInCurrentMessage = params[2];

        if (msgCount > 1 || remainingInCurrentMessage <= CHARS_REMAINING_BEFORE_COUNTER_SHOWN) {
            mTextView.setText(remainingInCurrentMessage + " / " + msgCount);
            mTextView.setVisibility(View.VISIBLE);
        } else {
            mTextView.setVisibility(View.GONE);
        }
    }
}
