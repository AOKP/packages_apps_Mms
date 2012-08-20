/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Profile;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.transaction.MessagingNotification.NotificationInfo;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.EmojiParser;
import com.android.mms.util.SmileyParser;
import com.google.android.mms.MmsException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class QuickMessage extends Activity {
    private static final String LOG_TAG = "QuickMessage";

    public static final String SMS_FROM_NAME_EXTRA =
            "com.android.mms.SMS_FROM_NAME";
    public static final String SMS_FROM_NUMBER_EXTRA =
            "com.android.mms.SMS_FROM_NUMBER";
    public static final String SMS_NOTIFICATION_ID_EXTRA =
            "com.android.mms.NOTIFICATION_ID";
    public static final String SMS_NOTIFICATION_OBJECT_EXTRA =
            "com.android.mms.NOTIFICATION_OBJECT";

    // View items
    private ImageView mQmPagerArrow;
    private TextView mQmMessageCounter;
    private Button mCloseButton;
    private Button mViewButton;

    // General items
    private Drawable mDefaultContactImage;
    private Context mContext;
    private boolean mScreenUnlocked = false;
    private KeyguardManager mKeyguardManager = null;

    // Message list items
    private ArrayList<QuickMessageContent> mMessageList;
    private QuickMessageContent mCurrentQm = null;
    private int mCurrentQmIndex = -1; // Set to an invalid index

    // Configuration
    private boolean mCloseClosesAll = false;
    private boolean mWakeAndUnlock = false;
    private boolean mPageCounterAlwaysVisible = true;

    // Message pager
    private ViewPager mMessagePager;
    private MessagePagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialise the message list and other variables
        mContext = this;
        mMessageList = new ArrayList<QuickMessageContent>();
        mDefaultContactImage = getResources().getDrawable(R.drawable.ic_contact_picture);
        mCloseClosesAll = MessagingPreferenceActivity.getQmCloseAllEnabled(mContext);
        mWakeAndUnlock = MessagingPreferenceActivity.getQmLockscreenEnabled(mContext);

        // Set the window features and layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_quickmessage);

        // Load the views and Parse the intent to show the QuickMessage
        setupViews();
        parseIntent(getIntent(), false);
    }

    private void setupViews() {

        // Load the main views
        mQmPagerArrow = (ImageView) findViewById(R.id.pager_arrow);
        mQmMessageCounter = (TextView) findViewById(R.id.message_counter);
        mCloseButton = (Button) findViewById(R.id.button_close);
        mViewButton = (Button) findViewById(R.id.button_view);

        // ViewPager Support
        mPagerAdapter = new MessagePagerAdapter();
        mMessagePager = (ViewPager) findViewById(R.id.message_pager);
        mMessagePager.setAdapter(mPagerAdapter);
        mMessagePager.setOnPageChangeListener(mPagerAdapter);

        // Close button
        mCloseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // If not closing all, close the current QM and move on
                int numMessages = mMessageList.size();
                if (mCloseClosesAll || numMessages == 1) {
                    clearNotification(true);
                    finish();
                } else {
                    // Dismiss the keyboard if it is shown
                    dismissKeyboard();

                    if (mCurrentQmIndex < numMessages-1) {
                        showNextMessageWithRemove();
                    } else {
                        showPreviousMessageWithRemove();
                    }
                }
            }
        });

        // View button
        mViewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentQm = mMessageList.get(mCurrentQmIndex);
                Intent vi = mCurrentQm.getViewIntent();
                if (vi != null) {
                    startActivity(vi);
                }
                clearNotification(false);
                finish();
            }
        });
    }

    private void parseIntent(Intent intent, boolean newMessage) {
        if (intent == null) {
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        // Parse the intent and ensure we have a notification object to work with
        NotificationInfo nm = (NotificationInfo) extras.getParcelable(SMS_NOTIFICATION_OBJECT_EXTRA);
        if (nm != null) {
            QuickMessageContent qm = new QuickMessageContent(extras.getString(SMS_FROM_NAME_EXTRA),
                    extras.getString(SMS_FROM_NUMBER_EXTRA), nm,
                    extras.getInt(SMS_NOTIFICATION_ID_EXTRA));
            mMessageList.add(qm);

            if (newMessage && mCurrentQmIndex != -1) {
                // There is already a message showing
                // Stay on the current message
                mMessagePager.setCurrentItem(mCurrentQmIndex);
            } else {
                // Set the current message to the last message received
                mCurrentQmIndex = mMessageList.size()-1;
                mMessagePager.setCurrentItem(mCurrentQmIndex);
            }

            // Make sure the counter is accurate
            updateMessageCounter();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Load and display the new message
        parseIntent(intent, true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Unlock the screen if needed
        unlockScreen();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mScreenUnlocked) {
            // Cancel the receiver that will clear the wake locks
            ClearAllReceiver.removeCancel(getApplicationContext());
            ClearAllReceiver.clearAll(mScreenUnlocked);
        }
    }

    /**
     * Supporting Utility functions
     */

    private void dismissKeyboard() {
        if (mCurrentQm == null) {
            mCurrentQm = mMessageList.get(mCurrentQmIndex);
        }

        EditText editView = mCurrentQm.getEditText();
        if (editView != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editView.getApplicationWindowToken(), 0);
        }
    }

    private void unlockScreen() {
        // See if the lock screen should be disabled
        if (!mWakeAndUnlock) {
            return;
        }

        // See if the screen is locked and get the wake lock to turn on the screen
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (mKeyguardManager.inKeyguardRestrictedInputMode()) {
            ManageWakeLock.acquireFull(mContext);
            mScreenUnlocked = true;
        }
    }

    private void updateMessageCounter() {
        if (mPageCounterAlwaysVisible || mMessageList.size() > 1) {
            String separator = mContext.getString(R.string.message_counter_separator);
            mQmMessageCounter.setText((mCurrentQmIndex + 1) + " " + separator + " " + mMessageList.size());
            mQmMessageCounter.setVisibility(View.VISIBLE);
            mQmPagerArrow.setVisibility(View.VISIBLE);
        } else {
            mQmMessageCounter.setVisibility(View.GONE);
            mQmPagerArrow.setVisibility(View.GONE);
        }
    }

    public void showPreviousMessageWithRemove() {
        markCurrentMessageRead();
        if (mCurrentQmIndex > 0) {
            updatePages(mCurrentQmIndex-1, mCurrentQmIndex);
        }
    }

    public void showNextMessageWithRemove() {
        markCurrentMessageRead();
        if (mCurrentQmIndex < (mMessageList.size() - 1)) {
            updatePages(mCurrentQmIndex, mCurrentQmIndex);
        }
    }

    private void updatePages(int gotoMsg, int removeMsg) {
        mMessageList.remove(removeMsg);
        mPagerAdapter.notifyDataSetChanged();
        mMessagePager.setCurrentItem(gotoMsg);
        updateMessageCounter();
    }

    private void markCurrentMessageRead() {
        mCurrentQm = mMessageList.get(mCurrentQmIndex);
        if (mCurrentQm != null) {
            Conversation con = Conversation.get(mContext, mCurrentQm.getThreadId(), true);
            if (con != null) {
                con.markAsRead();
            }
        }
    }

    private void markAllMessagesRead() {
        // This iterates through our MessageList and marks the contained threads as read
        for (QuickMessageContent qmc : mMessageList) {
            Conversation con = Conversation.get(mContext, qmc.getThreadId(), true);
            if (con != null) {
                con.markAsRead();
            }
        }
    }

    private void updateContactBadge(QuickContactBadge badge, String addr, boolean isSelf) {
        Drawable avatarDrawable;
        if (isSelf || !TextUtils.isEmpty(addr)) {
            Contact contact = isSelf ? Contact.getMe(false) : Contact.get(addr, false);
            avatarDrawable = contact.getAvatar(mContext, mDefaultContactImage);

            if (isSelf) {
                badge.assignContactUri(Profile.CONTENT_URI);
            } else {
                if (contact.existsInDatabase()) {
                    badge.assignContactUri(contact.getUri());
                } else {
                    badge.assignContactFromPhone(contact.getNumber(), true);
                }
            }
        } else {
            avatarDrawable = mDefaultContactImage;
        }
        badge.setImageDrawable(avatarDrawable);
    }

    private void sendQuickMessage(String message) {
        if (message != null) {
            mCurrentQm = mMessageList.get(mCurrentQmIndex);
            long threadId = mCurrentQm.getThreadId();
            SmsMessageSender sender = new SmsMessageSender(getBaseContext(),
                    mCurrentQm.getFromNumber(), message, threadId);
            try {
                sender.sendMessage(threadId);
                Toast.makeText(mContext, R.string.toast_sending_message, Toast.LENGTH_SHORT).show();
                markCurrentMessageRead();
            } catch (MmsException e) {
                // Do nothing
            }
        }
    }

    private void clearNotification(boolean markAsRead) {
        // Dismiss the notification that brought us here.
        mCurrentQm = mMessageList.get(mCurrentQmIndex);
        NotificationManager notificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mCurrentQm.getNotificationId());

        // Mark all contained conversations as seen
        if (markAsRead) {
            markAllMessagesRead();
        }

        // Clear the messages list
        mMessageList.clear();
    }

    private CharSequence formatMessage(String message) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        // Get the emojis  preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean enableEmojis = prefs.getBoolean(MessagingPreferenceActivity.ENABLE_EMOJIS, false);

        if (!TextUtils.isEmpty(message)) {
            SmileyParser parser = SmileyParser.getInstance();
            CharSequence smileyBody = parser.addSmileySpans(message);
            if (enableEmojis) {
                EmojiParser emojiParser = EmojiParser.getInstance();
                smileyBody = emojiParser.addEmojiSpans(smileyBody);
            }
            buf.append(smileyBody);
        }
        return buf;
    }

    /**
     * Supporting Classes
     */

    private class QuickMessageContent {
        private String mFromName;
        private String[] mFromNumber;
        private NotificationInfo mContent;
        private int mNotificationId;
        private String mReplyText;
        private String mTimestamp;
        private EditText mEditText = null;

        public QuickMessageContent(String fromName, String fromNumber, NotificationInfo nInfo, int notificationId) {
            mFromName = fromName;
            mFromNumber = new String[1];
            mFromNumber[0] = fromNumber;
            mContent = nInfo;
            mNotificationId = notificationId;
            mReplyText = "";
            makeTimestamp();
        }

        public void setEditText(EditText object) {
            mEditText = object;
        }

        public EditText getEditText() {
            return mEditText;
        }

        public String getFromName() {
            return mFromName;
        }

        public String[] getFromNumber() {
            return mFromNumber;
        }

        public String getMessageBody() {
            return mContent.mMessage;
        }

        public int getNotificationId() {
            return mNotificationId;
        }

        public String getReplyText() {
            return mReplyText;
        }

        public void setReplyText(String reply) {
            mReplyText = reply;
        }

        public void saveReplyText() {
            if (mEditText != null) {
                mReplyText = mEditText.getText().toString();
            }
        }

        public String getTimestamp() {
            return mTimestamp;
        }

        public Intent getViewIntent() {
            return mContent.mClickIntent;
        }

        public long getThreadId() {
            return mContent.mThreadId;
        }

        private void makeTimestamp() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(mContent.mTimeMillis);
            DateFormat formatter = new SimpleDateFormat("MMM dd, HH:mm:ss zzz");
            mTimestamp = formatter.format(calendar.getTime());
        }
    }

    private class MessagePagerAdapter extends PagerAdapter
                    implements ViewPager.OnPageChangeListener {

        protected LinearLayout mCurrentPrimaryLayout = null; 

        @Override
        public int getCount() {
            return mMessageList.size();
        }

        @Override
        public Object instantiateItem(View collection, int position) {

            // Load the layout to be used
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.quickmessage_content, null);

            // Load the main views
            EditText qmReplyText = (EditText) layout.findViewById(R.id.embedded_text_editor);
            TextView qmTextCounter = (TextView) layout.findViewById(R.id.text_counter);
            ImageButton qmSendButton = (ImageButton) layout.findViewById(R.id.send_button_sms);
            TextView qmMessageText = (TextView) layout.findViewById(R.id.messageTextView);
            TextView qmFromName = (TextView) layout.findViewById(R.id.fromTextView);
            TextView qmTimestamp = (TextView) layout.findViewById(R.id.timestampTextView);
            QuickContactBadge qmContactBadge = (QuickContactBadge) layout.findViewById(R.id.contactBadge);

            // Retrieve the current message
            QuickMessageContent qm = mMessageList.get(position);
            if (qm != null) {
                // Set the general fields
                qmFromName.setText(qm.getFromName());
                qmTimestamp.setText(qm.getTimestamp());
                updateContactBadge(qmContactBadge, qm.getFromNumber()[0], false);
                qmMessageText.setText(formatMessage(qm.getMessageBody()));

                // We are using a holo.light background with a holo.dark activity theme
                // Override the EditText background to use the holo.light theme
                qmReplyText.setBackgroundResource(android.R.drawable.edit_text_holo_light);

                // Set the remaining values
                qmReplyText.setText(qm.getReplyText());
                qmReplyText.setSelection(qm.getReplyText().length());
                qmReplyText.addTextChangedListener(new QmTextWatcher(mContext, qmTextCounter, qmSendButton));
                qmReplyText.setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (event != null) {
                            // event != null means enter key pressed
                            if (!event.isShiftPressed()) {
                                // if shift is not pressed then move focus to send button
                                if (v != null) {
                                    View focusableView = v.focusSearch(View.FOCUS_RIGHT);
                                    if (focusableView != null) {
                                        focusableView.requestFocus();
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
                        if (actionId == EditorInfo.IME_ACTION_SEND) {
                            if (v != null) {
                                sendMessageAndMoveOn(v.getText().toString());
                            }
                            return true;
                        }
                        return true;
                    }
                });

                QmTextWatcher.getQuickReplyCounterText(qmReplyText.getText().toString(),
                        qmTextCounter, qmSendButton);

                // Store the EditText object for future use
                qm.setEditText(qmReplyText);

                // Send button
                qmSendButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCurrentQm = mMessageList.get(mCurrentQmIndex);
                        EditText editView = mCurrentQm.getEditText();
                        if (editView != null) {
                            sendMessageAndMoveOn(editView.getText().toString());
                        }
                    }
                });

                // Add the layout to the viewpager
                ((ViewPager) collection).addView(layout);
            }
            return layout;
        }

        private void sendMessageAndMoveOn(String message) {
            sendQuickMessage(message);
            // Close the current QM and move on
            int numMessages = mMessageList.size();
            if (numMessages == 1) {
                // No more messages
                clearNotification(true);
                finish();
            } else {
                // Dismiss the keyboard if it is shown
                dismissKeyboard();

                if (mCurrentQmIndex < numMessages-1) {
                    showNextMessageWithRemove();
                } else {
                    showPreviousMessageWithRemove();
                }
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            LinearLayout view = ((LinearLayout)object);
            if (view != mCurrentPrimaryLayout) {
                mCurrentPrimaryLayout = view;
            }
        }

        @Override
        public void onPageSelected(int position) {
            // The user had scrolled to a new message
            if (mCurrentQm != null) {
                mCurrentQm.saveReplyText();
            }

            // Set the new 'active' QuickMessage
            mCurrentQmIndex = position;
            mCurrentQm = mMessageList.get(position);

            updateMessageCounter();
        }

        @Override
        public int getItemPosition(Object object) {
            // This is needed to force notifyDatasetChanged() to rebuild the pages
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((LinearLayout) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
                return view==((LinearLayout)object);
        }

        @Override
        public void finishUpdate(View arg0) {}

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {}

        @Override
        public void onPageScrollStateChanged(int arg0) {}

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {}

    }

}