
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
import com.android.mms.data.Conversation;
import com.android.mms.util.EmojiParser;
import com.android.mms.util.SmileyParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class QuickReply extends Activity implements OnDismissListener, OnClickListener,
        OnMenuItemClickListener {
    Context mContext = this;

    private Bitmap avatar;
    private Drawable icon;
    private ImageView contactIcon;
    private String phoneNumber;
    private String contactName;
    private String textBody;
    private CharSequence textBodies;
    private int messageType;
    private long messageId;
    private long threadId;
    private ImageButton sendReply;
    private ImageButton qrMenu;
    private TextView nameContact;
    private TextView prevText;
    private EditText textBox;
    private KeyguardManager.KeyguardLock kl;
    private boolean typing;
    private boolean wasLocked = false;

    private AlertDialog mSmileyDialog;
    private AlertDialog mEmojiDialog;
    private View mEmojiView;

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
        textBodies = extras.getCharSequence("bodies");
        messageId = extras.getLong("msgId");
        threadId = extras.getLong("threadId");
        messageType = extras.getInt("count");
        nameContact = (TextView) mView.findViewById(R.id.contact_name);
        nameContact.setText(contactName);
        prevText = (TextView) mView.findViewById(R.id.prev_text_body);
        if (messageType == 1) {
            prevText.setText(replaceWithEmotes(textBodies));
        } else {
            prevText.setText(replaceWithEmotes(textBody));   
        }
        contactIcon = (ImageView) mView.findViewById(R.id.contact_avatar);
        icon = null;
        icon = getConactAvatar(avatar);
        if (icon != null) {
            contactIcon.setBackgroundDrawable(icon);
        } else {
            contactIcon.setBackgroundResource(R.drawable.ic_contact_picture);
        }
        sendReply = (ImageButton) mView.findViewById(R.id.reply_button);
        sendReply.setOnClickListener(this);
        qrMenu = (ImageButton) mView.findViewById(R.id.menu_button);
        qrMenu.setOnClickListener(this);
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

    /**
     * Use smiley parser and emoji parser to show emotes in text body this
     * method is modified from the MessageListItem.java
     * 
     * @param: body of text from an SMS in String format
     * @return: formatted text to use smiley emotes & emoji
     */
    private CharSequence replaceWithEmotes(String body) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        boolean enableEmojis = prefs.getBoolean(MessagingPreferenceActivity.ENABLE_EMOJIS, false);

        if (!TextUtils.isEmpty(body)) {
            SmileyParser parser = SmileyParser.getInstance();
            CharSequence smileyBody = parser.addSmileySpans(body);
            if (enableEmojis) {
                EmojiParser emojiParser = EmojiParser.getInstance();
                smileyBody = emojiParser.addEmojiSpans(smileyBody);
            }
            buf.append(smileyBody);
        }
        return buf;
    }
    
    /**
     * Use smiley parser and emoji parser to show emotes in text body this
     * method is modified from the MessageListItem.java
     * 
     * @param: body of text from an SMS in CharSequence format
     * @return: formatted text to use smiley emotes & emoji
     */
    private CharSequence replaceWithEmotes(CharSequence body) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        boolean enableEmojis = prefs.getBoolean(MessagingPreferenceActivity.ENABLE_EMOJIS, false);

        if (!TextUtils.isEmpty(body)) {
            SmileyParser parser = SmileyParser.getInstance();
            CharSequence smileyBody = parser.addSmileySpans(body);
            if (enableEmojis) {
                EmojiParser emojiParser = EmojiParser.getInstance();
                smileyBody = emojiParser.addEmojiSpans(smileyBody);
            }
            buf.append(smileyBody);
        }
        return buf;
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

    /**
     * mark messages as read single or multi threaded messages and gets the
     * threadId to use Conversation data to clear the entire thread if the SMS
     * is multi threaded from a single person
     */
    private void setRead() {
        if (messageType == 1) {
            Conversation cnv = Conversation.get(mContext, threadId, true);
            cnv.markAsRead();
        } else {
            ContentValues values = new ContentValues();
            values.put("READ", 1);
            values.put("SEEN", 1);
            getContentResolver().update(Uri.parse("content://sms/"),
                    values, "_id=" + messageId, null);
        }

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
        } else if (v == qrMenu) {
            showPopup(v);
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
                if (messageType == 1) {
                    prevText.setText(replaceWithEmotes(textBodies));
                } else {
                    prevText.setText(replaceWithEmotes(textBody));   
                }
                contactIcon = (ImageView) mView.findViewById(R.id.contact_avatar);
                icon = null;
                icon = getConactAvatar(avatar);
                if (icon != null) {
                    contactIcon.setBackgroundDrawable(icon);
                } else {
                    contactIcon.setBackgroundResource(R.drawable.ic_contact_picture);
                }
                sendReply = (ImageButton) mView.findViewById(R.id.reply_button);
                sendReply.setOnClickListener(QuickReply.this);
                qrMenu = (ImageButton) mView.findViewById(R.id.menu_button);
                qrMenu.setOnClickListener(QuickReply.this);
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

    /**
     * Make our own option menu since we have no action bar
     * 
     * @param view
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.qr_menu);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.qr_menu_close:
                finish();
                return true;
            case R.id.qr_menu_read:
                setRead();
                finish();
                return true;
            case R.id.qr_menu_smiley:
                showSmileyDialog();
                return true;
            case R.id.qr_menu_emoji:
                showEmojiDialog();
                return true;
            default:
                return false;
        }
    }

    /**
     * Pulled Smiley & Emoji insert from ComposeMessageActivity.java Then
     * modified for use on quick reply
     */
    private void showSmileyDialog() {
        if (mSmileyDialog == null) {
            int[] icons = SmileyParser.DEFAULT_SMILEY_RES_IDS;
            String[] names = getResources().getStringArray(
                    SmileyParser.DEFAULT_SMILEY_NAMES);
            final String[] texts = getResources().getStringArray(
                    SmileyParser.DEFAULT_SMILEY_TEXTS);

            final int N = names.length;

            List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
            for (int i = 0; i < N; i++) {
                // We might have different ASCII for the same icon, skip it if
                // the icon is already added.
                boolean added = false;
                for (int j = 0; j < i; j++) {
                    if (icons[i] == icons[j]) {
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    HashMap<String, Object> entry = new HashMap<String, Object>();

                    entry.put("icon", icons[i]);
                    entry.put("name", names[i]);
                    entry.put("text", texts[i]);

                    entries.add(entry);
                }
            }

            final SimpleAdapter a = new SimpleAdapter(
                    this,
                    entries,
                    R.layout.smiley_menu_item,
                    new String[] {
                            "icon", "name", "text"
                    },
                    new int[] {
                            R.id.smiley_icon, R.id.smiley_name, R.id.smiley_text
                    });
            SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data, String textRepresentation) {
                    if (view instanceof ImageView) {
                        Drawable img = getResources().getDrawable((Integer) data);
                        ((ImageView) view).setImageDrawable(img);
                        return true;
                    }
                    return false;
                }
            };
            a.setViewBinder(viewBinder);

            AlertDialog.Builder b = new AlertDialog.Builder(this);

            b.setTitle(getString(R.string.menu_insert_smiley));

            b.setCancelable(true);
            b.setAdapter(a, new DialogInterface.OnClickListener() {
                @Override
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    HashMap<String, Object> item = (HashMap<String, Object>) a.getItem(which);

                    String smiley = (String) item.get("text");
                    textBox.append(smiley);

                    dialog.dismiss();
                }
            });

            mSmileyDialog = b.create();
        }

        mSmileyDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mSmileyDialog.show();
    }

    private void showEmojiDialog() {
        if (mEmojiDialog == null) {
            int[] icons = EmojiParser.DEFAULT_EMOJI_RES_IDS;

            int layout = R.layout.emoji_insert_view;
            mEmojiView = getLayoutInflater().inflate(layout, null);

            final GridView gridView = (GridView) mEmojiView.findViewById(R.id.emoji_grid_view);
            gridView.setAdapter(new ImageAdapter(this, icons));
            final EditText editText = (EditText) mEmojiView.findViewById(R.id.emoji_edit_text);
            final Button button = (Button) mEmojiView.findViewById(R.id.emoji_button);

            gridView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    // We use the new unified Unicode 6.1 emoji code points
                    CharSequence emoji = EmojiParser.getInstance().addEmojiSpans(
                            EmojiParser.mEmojiTexts[position]);
                    editText.append(emoji);
                }
            });

            gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    // We use the new unified Unicode 6.1 emoji code points
                    CharSequence emoji = EmojiParser.getInstance().addEmojiSpans(
                            EmojiParser.mEmojiTexts[position]);
                    textBox.append(emoji);

                    mEmojiDialog.dismiss();
                    return true;
                }
            });

            button.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textBox.append(editText.getText());
                    mEmojiDialog.dismiss();
                }
            });

            AlertDialog.Builder b = new AlertDialog.Builder(this);

            b.setTitle(getString(R.string.menu_insert_emoji));

            b.setCancelable(true);
            b.setView(mEmojiView);

            mEmojiDialog = b.create();
        }

        final EditText editText = (EditText) mEmojiView.findViewById(R.id.emoji_edit_text);
        editText.setText("");

        mEmojiDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mEmojiDialog.show();
    }
}
