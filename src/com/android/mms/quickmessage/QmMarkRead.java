/*
 * Copyright (C) 2012 The CyanogenMod Project (DvTonder)
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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.mms.data.Conversation;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.MessagingPreferenceActivity;

public class QmMarkRead extends BroadcastReceiver {
    private static final String LOG_TAG = "QmMarkRead";

    // Intent bungle fields
    public static final String SMS_THREAD_ID =
            "com.android.mms.SMS_THREAD_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras == null) {
            // We have nothing, abort
            return;
        }

        // Parse the intent and ensure we have a message Id to work with
        long threadId = extras.getLong(SMS_THREAD_ID, -1);
        if (threadId != -1) {
            Conversation con = Conversation.get(context, threadId, true);
            if (con != null) {
                // Mark thread as read
                con.markAsRead(false);

                // See if we have to also remove the popup messages and do so
                if (MessagingPreferenceActivity.getQuickMessageEnabled(context)) {
                    Intent qmIntent = new Intent();
                    qmIntent.setClass(context, QuickMessagePopup.class);
                    qmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    qmIntent.putExtra(QuickMessagePopup.QR_REMOVE_MESSAGES_EXTRA, true);
                    qmIntent.putExtra(QuickMessagePopup.QR_THREAD_ID_EXTRA, threadId);
                    context.startActivity(qmIntent);
                }

                // Dismiss the notification that brought us here.
                NotificationManager notificationManager =
                    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(MessagingNotification.NOTIFICATION_ID);
            }
        }
    }

}
