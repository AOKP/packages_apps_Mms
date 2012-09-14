
package com.android.mms.ui;

/*
 * Copyright (C) 2012 Android Open Kang Project (Adam Fisch & James Roberts)
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

import com.android.mms.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class QuickReply extends Activity implements OnDismissListener, OnClickListener {
    Context mContext = this;

    private Bitmap avatar;
    private Drawable icon;
    private ImageView contactIcon;
    private String phoneNumber;
    private String contactName;
    private String textBody;
    private long messageId;
    private ImageButton sendReply;
    private TextView nameContact;
    private TextView prevText;
    private EditText textBox;
    private KeyguardManager.KeyguardLock kl;
    private boolean typing;
    private boolean wasLocked = false;

    private static final int NOTIFICATION_ID = 123;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // find if the phone is on the lockscreen to allow dialog popup above it
        // if needed
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = km.inKeyguardRestrictedInputMode();

        kl = km.newKeyguardLock("QuickReply");

        LayoutInflater inflater = LayoutInflater.from(this);
        final View mView = inflater.inflate(R.layout.quick_reply_sms, null);
        AlertDialog alert = new AlertDialog.Builder(this).setView(mView).create();

        Bundle extras = getIntent().getExtras();
        avatar = (Bitmap) extras.get("avatar");
        phoneNumber = extras.getString("number");
        contactName = extras.getString("name");
        textBody = extras.getString("body");
        messageId = extras.getLong("msgId");
        nameContact = (TextView) mView.findViewById(R.id.contact_name);
        nameContact.setText(contactName);
        prevText = (TextView) mView.findViewById(R.id.prev_text_body);
        prevText.setText(textBody);
        contactIcon = (ImageView) mView.findViewById(R.id.contact_avatar);
        icon = null;
        icon = getConactAvatar(avatar);
        if (icon != null) {
            contactIcon.setBackgroundDrawable(icon);
        } else {
            contactIcon.setBackgroundResource(R.drawable.ic_contact_picture);
        }
        sendReply = (ImageButton) mView.findViewById(R.id.reply_button);
        sendReply.setImageResource(R.drawable.ic_send_holo_light);
        sendReply.setOnClickListener(this);
        textBox = (EditText) mView.findViewById(R.id.edit_box);
        textBox.setOnClickListener(this);
        alert.setOnDismissListener(this);
        // set alert system to make sure it is always on top, permission
        // required.
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        if (isLocked) {
            kl.disableKeyguard();
            wasLocked = true;
        }
        alert.show();
    }

    private Drawable getConactAvatar(Bitmap bitmap) {
        Drawable icon;
        Resources res = mContext.getResources();
        if (bitmap != null) {
            final int idealIconHeight =
                    res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
            final int idealIconWidth =
                    res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
            if (bitmap.getHeight() < idealIconHeight) {
                // Scale this image to fit the intended size
                bitmap = Bitmap.createScaledBitmap(
                        bitmap, idealIconWidth, idealIconHeight, true);
            }
            icon = new BitmapDrawable(getResources(), bitmap);
        } else {
            icon = null;
        }
        return icon;
    }

    private void sendSms() {
        String message = null;
        message = textBox.getText().toString();
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
        setRead();
        addMessageToSent(message);
        finish();
    }

    private void setRead() {
        ContentValues values = new ContentValues();
        values.put("READ", 1);
        values.put("SEEN", 1);
        getContentResolver().update(Uri.parse("content://sms/"),
                values, "_id=" + messageId, null);

        // clear the notification
        NotificationManager nm = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    private void addMessageToSent(String messageSent) {
        ContentValues sentSms = new ContentValues();
        sentSms.put("address", phoneNumber);
        sentSms.put("body", messageSent);
        getContentResolver().insert(Uri.parse("content://sms/sent"), sentSms);
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v == sendReply) {
            if (typing) {
                imm.hideSoftInputFromWindow(textBox.getWindowToken(), 0);
                typing = false;
            }
            sendSms();
            String nyan = getResources().getString(R.string.quick_reply_sending);
            Toast.makeText(this, nyan + ": " + contactName, Toast.LENGTH_SHORT).show();
        } else if (v == textBox) {
            imm.showSoftInput(textBox, 0);
            typing = true;
            textBox.requestFocus();
        }

    }

    @Override
    public void onDestroy() {
        if (wasLocked) {
            kl.reenableKeyguard();
        }
        finish();
        super.onDestroy();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.qr_alert_title);
        alert.setMessage(R.string.qr_alert_message);

        alert.setPositiveButton(R.string.qr_alert_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });

        alert.setNegativeButton(R.string.qr_alert_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                LayoutInflater inflater = LayoutInflater.from(QuickReply.this);
                final View mView = inflater.inflate(R.layout.quick_reply_sms, null);
                AlertDialog alert = new AlertDialog.Builder(QuickReply.this).setView(mView)
                        .create();

                nameContact = (TextView) mView.findViewById(R.id.contact_name);
                nameContact.setText(contactName);
                prevText = (TextView) mView.findViewById(R.id.prev_text_body);
                prevText.setText(textBody);
                contactIcon = (ImageView) mView.findViewById(R.id.contact_avatar);
                icon = null;
                icon = getConactAvatar(avatar);
                if (icon != null) {
                    contactIcon.setBackgroundDrawable(icon);
                } else {
                    contactIcon.setBackgroundResource(R.drawable.ic_contact_picture);
                }
                sendReply = (ImageButton) mView.findViewById(R.id.reply_button);
                sendReply.setImageResource(R.drawable.ic_send_holo_light);
                sendReply.setOnClickListener(QuickReply.this);
                textBox = (EditText) mView.findViewById(R.id.edit_box);
                textBox.setOnClickListener(QuickReply.this);
                alert.setOnDismissListener(QuickReply.this);
                alert.show();
            }
        });

        alert.setNeutralButton(R.string.qr_alert_read, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                setRead();
                finish();
            }
        });

        alert.show();
    }
}
