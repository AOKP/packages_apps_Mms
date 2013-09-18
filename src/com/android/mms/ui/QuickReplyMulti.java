
package com.android.mms.ui;

/*
 * Copyright (C) 2012 Android Open Kang Project (Adam Fisch)
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.R;

import android.database.sqlite.SqliteWrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class QuickReplyMulti extends Activity implements OnDismissListener {
    Context mContext = this;

    private HashMap<Long, NotificationInfo> noteHash;
    private HashMap<Long, ArrayList<NotificationInfo>> noteHashs;
    Set<Long> threads;
    private ArrayList<Long> keyArray;
    private String[] nameList;
    private boolean wasLocked = false;

    private KeyguardManager.KeyguardLock kl;

    private static final Uri SMS_INBOX_URI = Uri.withAppendedPath(
            Uri.parse("content://sms"), "inbox");

    // This must be consistent with the column constants below.
    private static final String[] SMS_STATUS_PROJECTION = new String[] {
            Sms.THREAD_ID, Sms.DATE, Sms.ADDRESS, Sms.SUBJECT, Sms.BODY
    };

    private static final String NEW_INCOMING_SM_CONSTRAINT =
            "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
                    + " AND " + Sms.SEEN + " = 0)";

    // These must be consistent with MMS_STATUS_PROJECTION and
    // SMS_STATUS_PROJECTION.
    private static final int COLUMN_THREAD_ID = 0;
    private static final int COLUMN_DATE = 1;
    private static final int COLUMN_MMS_ID = 2;
    private static final int COLUMN_SMS_ADDRESS = 2;
    private static final int COLUMN_SUBJECT = 3;
    private static final int COLUMN_SUBJECT_CS = 4;
    private static final int COLUMN_SMS_BODY = 4;

    private static final NotificationInfoComparator INFO_COMPARATOR =
            new NotificationInfoComparator();

    private static SortedSet<NotificationInfo> sNotificationSet =
            new TreeSet<NotificationInfo>(INFO_COMPARATOR);

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // find if the phone is on the lockscreen to allow dialog popup above it
        // if needed
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = km.inKeyguardRestrictedInputMode();

        final boolean markSmsRead = getIntent().getBooleanExtra("makeAndClose", false);

        kl = km.newKeyguardLock("QuickReply");

        if (!sNotificationSet.isEmpty()) {
            sNotificationSet.clear();
        }
        noteHash = new HashMap<Long, NotificationInfo>();
        noteHashs = new HashMap<Long, ArrayList<NotificationInfo>>();
        keyArray = new ArrayList<Long>();
        threads = new HashSet<Long>(4);
        addSmsNotificationInfos(mContext, threads);

        nameList = getNoteNames();

        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        if (markSmsRead) {
            b.setTitle(R.string.qr_multi_read);
        } else {
            b.setTitle(R.string.qr_multi_ask);
        }
        b.setItems(nameList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Log.d("QuickReplyMulti", "item= " + item + ". nameList length= " + nameList.length
                        + ". keyArray size= " + keyArray.size() + ".");
                if (item == keyArray.size()) {
                    if (wasLocked) {
                        kl.reenableKeyguard();
                    }
                    finish();
                } else {
                    Intent quickReply = null;
                    long key = keyArray.get(item);
                    NotificationInfo mostRecentNotification = noteHash.get(key);
                    boolean multi = noteHashs.containsKey(key);
                    SpannableStringBuilder buf = new SpannableStringBuilder();
                    if (multi) {
                        ArrayList<String> textBody = new ArrayList<String>();
                        ArrayList<NotificationInfo> multiNotification = new ArrayList<NotificationInfo>();
                        multiNotification = noteHashs.get(key);
                        int count = noteHashs.size() + 1;
                        for (int i = 0; i > count; i++) {
                            if (i == 0) {
                                textBody.add(noteHash.get(key).mMessage.toString());
                            } else {
                                textBody.add(multiNotification.get(i).mMessage.toString());
                            }
                        }
                        for (int i = 0; i > 0; i++) {
                            buf.append(formatBigMessage(textBody.get(i)));
                            if (i != 0) {
                                buf.append('\n');
                            }
                        }
                    }

                    if (markSmsRead) {
                        Conversation cnv = Conversation.get(mContext, mostRecentNotification.mThreadId, true);
                        cnv.markAsRead();
                    }

                    quickReply = new Intent();
                    quickReply.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    quickReply.setClass(mContext, com.android.mms.ui.QuickReply.class);
                    quickReply.putExtra("number", mostRecentNotification.mSender.getNumber());
                    quickReply.putExtra("name", mostRecentNotification.mSender.getName());
                    quickReply.putExtra("threadId", mostRecentNotification.mThreadId);
                    if (multi) {
                        quickReply.putExtra("count", 1);
                        quickReply.putExtra("bodies", buf);
                    } else {
                        quickReply.putExtra("count", 0);
                        quickReply.putExtra("body", mostRecentNotification.mMessage.toString());
                    }
                    quickReply.putExtra("makeAndClose", markSmsRead);
                    quickReply.putExtra("from", true);

                    // get the contact avatar
                    BitmapDrawable contactDrawable = (BitmapDrawable) mostRecentNotification.mSender
                            .getAvatar(mContext, null);
                    Bitmap contactPic = null;
                    if (contactDrawable != null) {
                        contactPic = contactDrawable.getBitmap();
                        if (contactPic != null) {
                            quickReply.putExtra("avatar", contactPic);
                        }
                    }
                    startActivity(quickReply);
                    finish();
                }
            }
        });

        AlertDialog alert = b.create();
        if (isLocked) {
            kl.disableKeyguard();
            wasLocked = true;
        }
        // kill activity once only the "close" button is left
        if (nameList.length == 1) {
            if (wasLocked) {
                kl.reenableKeyguard();
            }
            finish();
        }
        alert.setOnDismissListener(this);
        alert.show();

    }

    private String[] getNoteNames() {
        int count = sNotificationSet.size();
        String[] list;
        ArrayList<String> names = new ArrayList<String>(count + 1);
        ArrayList<NotificationInfo> noteArray = new ArrayList<NotificationInfo>();
        Iterator<NotificationInfo> notifications = sNotificationSet.iterator();
        while (notifications.hasNext()) {
            NotificationInfo notificationInfo = notifications.next();
            if (!names.contains(notificationInfo.mSender.getName())) {
                names.add(notificationInfo.mSender.getName());
                if (!keyArray.contains(notificationInfo.mThreadId)) {
                    keyArray.add(notificationInfo.mThreadId);
                    noteHash.put(notificationInfo.mThreadId, notificationInfo);
                } else {
                    noteArray.add(notificationInfo);
                    noteHashs.put(notificationInfo.mThreadId, noteArray);
                }
            }
        }

        names.add("Close");
        list = new String[names.size()];
        names.toArray(list);

        return list;
    }

    private static final void addSmsNotificationInfos(
            Context context, Set<Long> threads) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, Sms.CONTENT_URI,
                SMS_STATUS_PROJECTION, NEW_INCOMING_SM_CONSTRAINT,
                null, Sms.DATE + " desc");

        if (cursor == null) {
            return;
        }

        try {
            while (cursor.moveToNext()) {
                String address = cursor.getString(COLUMN_SMS_ADDRESS);

                Contact contact = Contact.get(address, false);
                if (contact.getSendToVoicemail()) {
                    // don't notify, skip this one
                    continue;
                }

                String message = cursor.getString(COLUMN_SMS_BODY);
                long threadId = cursor.getLong(COLUMN_THREAD_ID);
                long timeMillis = cursor.getLong(COLUMN_DATE);

                NotificationInfo info = getNewMessageNotificationInfo(context, true /* isSms */,
                        address, message, null /* subject */,
                        threadId, timeMillis, null /* attachmentBitmap */,
                        contact, WorkingMessage.TEXT);

                sNotificationSet.add(info);

                threads.add(threadId);
                threads.add(cursor.getLong(COLUMN_THREAD_ID));
            }
        } finally {
            cursor.close();
        }
    }

    private static final NotificationInfo getNewMessageNotificationInfo(
            Context context,
            boolean isSms,
            String address,
            String message,
            String subject,
            long threadId,
            long timeMillis,
            Bitmap attachmentBitmap,
            Contact contact,
            int attachmentType) {
        Intent clickIntent = ComposeMessageActivity.createIntent(context, threadId);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String senderInfo = buildTickerMessage(
                context, address, null, null).toString();
        String senderInfoName = senderInfo.substring(
                0, senderInfo.length() - 2);
        CharSequence ticker = buildTickerMessage(
                context, address, subject, message);

        return new NotificationInfo(isSms,
                clickIntent, message, subject, ticker, timeMillis,
                senderInfoName, attachmentBitmap, contact, attachmentType, threadId);
    }

    private static final class NotificationInfo {
        public final Intent mClickIntent;
        public final String mMessage;
        public final CharSequence mTicker;
        public final long mTimeMillis;
        public final String mTitle;
        public final Bitmap mAttachmentBitmap;
        public final Contact mSender;
        public final boolean mIsSms;
        public final int mAttachmentType;
        public final String mSubject;
        public final long mThreadId;

        /**
         * @param isSms true if sms, false if mms
         * @param clickIntent where to go when the user taps the notification
         * @param message for a single message, this is the message text
         * @param subject text of mms subject
         * @param ticker text displayed ticker-style across the notification,
         *            typically formatted as sender: message
         * @param timeMillis date the message was received
         * @param title for a single message, this is the sender
         * @param attachmentBitmap a bitmap of an attachment, such as a picture
         *            or video
         * @param sender contact of the sender
         * @param attachmentType of the mms attachment
         * @param threadId thread this message belongs to
         */
        public NotificationInfo(boolean isSms,
                Intent clickIntent, String message, String subject,
                CharSequence ticker, long timeMillis, String title,
                Bitmap attachmentBitmap, Contact sender,
                int attachmentType, long threadId) {
            mIsSms = isSms;
            mClickIntent = clickIntent;
            mMessage = message;
            mSubject = subject;
            mTicker = ticker;
            mTimeMillis = timeMillis;
            mTitle = title;
            mAttachmentBitmap = attachmentBitmap;
            mSender = sender;
            mAttachmentType = attachmentType;
            mThreadId = threadId;
        }

        public long getTime() {
            return mTimeMillis;
        }
    }

    public CharSequence formatBigMessage(String body) {
        final TextAppearanceSpan notificationSubjectSpan = new TextAppearanceSpan(
                mContext, R.style.NotificationPrimaryText);

        String text = body;
        // Change multiple newlines (with potential white space between),
        // into a single new line
        final String message =
                !TextUtils.isEmpty(text) ? text.replaceAll("\\n\\s+", "\n") : "";

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        if (text != null) {
            if (spannableStringBuilder.length() > 0) {
                spannableStringBuilder.append('\n');
            }
            spannableStringBuilder.append(text);
        }
        return spannableStringBuilder;
    }

    protected static CharSequence buildTickerMessage(
            Context context, String address, String subject, String body) {
        String displayAddress = Contact.get(address, true).getName();

        StringBuilder buf = new StringBuilder(
                displayAddress == null
                        ? ""
                        : displayAddress.replace('\n', ' ').replace('\r', ' '));
        buf.append(':').append(' ');

        int offset = buf.length();
        if (!TextUtils.isEmpty(subject)) {
            subject = subject.replace('\n', ' ').replace('\r', ' ');
            buf.append(subject);
            buf.append(' ');
        }

        if (!TextUtils.isEmpty(body)) {
            body = body.replace('\n', ' ').replace('\r', ' ');
            buf.append(body);
        }

        SpannableString spanText = new SpannableString(buf.toString());
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, offset,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanText;
    }

    /**
     * Sorts by the time a notification was received in descending order --
     * newer first.
     */
    private static final class NotificationInfoComparator
            implements Comparator<NotificationInfo> {
        @Override
        public int compare(
                NotificationInfo info1, NotificationInfo info2) {
            return Long.signum(info2.getTime() - info1.getTime());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
