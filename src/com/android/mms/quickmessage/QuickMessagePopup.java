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

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Profile;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.templates.TemplatesProvider.Template;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.MessagingNotification.NotificationInfo;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.UnicodeFilter;
import com.google.android.mms.MmsException;
//import com.android.mms.ui.ImageAdapter;
//import com.android.mms.util.EmojiParser;
//import com.android.mms.util.SmileyParser;

public class QuickMessagePopup extends Activity implements
    LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = "QuickMessagePopup";

    private boolean DEBUG = false;

    // Intent bungle fields
    public static final String SMS_FROM_NAME_EXTRA =
            "com.android.mms.SMS_FROM_NAME";
    public static final String SMS_FROM_NUMBER_EXTRA =
            "com.android.mms.SMS_FROM_NUMBER";
    public static final String SMS_NOTIFICATION_OBJECT_EXTRA =
            "com.android.mms.NOTIFICATION_OBJECT";
    public static final String QR_SHOW_KEYBOARD_EXTRA =
            "com.android.mms.QR_SHOW_KEYBOARD";

    // Message removal
    public static final String QR_REMOVE_MESSAGES_EXTRA =
            "com.android.mms.QR_REMOVE_MESSAGES";
    public static final String QR_THREAD_ID_EXTRA =
            "com.android.mms.QR_THREAD_ID";

    // Templates support
    private static final int DIALOG_TEMPLATE_SELECT        = 1;
    private static final int DIALOG_TEMPLATE_NOT_AVAILABLE = 2;
    private SimpleCursorAdapter mTemplatesCursorAdapter;
    private int mNumTemplates = 0;

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
    private PowerManager mPowerManager;

    // Message list items
    private ArrayList<QuickMessage> mMessageList;
    private QuickMessage mCurrentQm = null;
    private int mCurrentPage = -1; // Set to an invalid index

    // Configuration
    private boolean mCloseClosesAll = false;
    private boolean mWakeAndUnlock = false;
    private boolean mDarkTheme = false;
    private boolean mFullTimestamp = false;
    private int mUnicodeStripping = MessagingPreferenceActivity.UNICODE_STRIPPING_LEAVE_INTACT;
    private UnicodeFilter mUnicodeFilter = null;
    private int mInputMethod;

    // Message pager
    private ViewPager mMessagePager;
    private MessagePagerAdapter mPagerAdapter;

    // Options menu items
    private static final int MENU_ADD_TEMPLATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialise the message list and other variables
        mContext = this;
        mMessageList = new ArrayList<QuickMessage>();
        mDefaultContactImage = getResources().getDrawable(R.drawable.ic_contact_picture);
        mNumTemplates = getTemplatesCount();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        // Get the preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mFullTimestamp = prefs.getBoolean(MessagingPreferenceActivity.FULL_TIMESTAMP, false);
        mCloseClosesAll = prefs.getBoolean(MessagingPreferenceActivity.QM_CLOSE_ALL_ENABLED, false);
        mWakeAndUnlock = prefs.getBoolean(MessagingPreferenceActivity.QM_LOCKSCREEN_ENABLED, false);
        mDarkTheme = prefs.getBoolean(MessagingPreferenceActivity.QM_DARK_THEME_ENABLED, false);
        mUnicodeStripping = prefs.getInt(MessagingPreferenceActivity.UNICODE_STRIPPING_VALUE,
                MessagingPreferenceActivity.UNICODE_STRIPPING_LEAVE_INTACT);
        mInputMethod = Integer.parseInt(prefs.getString(MessagingPreferenceActivity.INPUT_TYPE,
                Integer.toString(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE)));

        // Set the window features and layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_quickmessage);

        // Turn on the Options Menu
        invalidateOptionsMenu();

        // Load the views and Parse the intent to show the QuickMessage
        setupViews();
        parseIntent(getIntent().getExtras(), false);
    }

    private void setupViews() {

        // Load the main views
        mQmPagerArrow = (ImageView) findViewById(R.id.pager_arrow);
        mQmMessageCounter = (TextView) findViewById(R.id.message_counter);
        mCloseButton = (Button) findViewById(R.id.button_close);
        mViewButton = (Button) findViewById(R.id.button_view);

        // Set the theme color on the pager arrow
        Resources res = getResources();
        if (mDarkTheme) {
            mQmPagerArrow.setBackgroundColor(res.getColor(R.color.quickmessage_body_dark_bg));
        } else {
            mQmPagerArrow.setBackgroundColor(res.getColor(R.color.quickmessage_body_light_bg));
        }

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
                    QuickMessage qm = mMessageList.get(mCurrentPage);
                    if (qm != null) {
                        dismissKeyboard(qm);

                        if (mCurrentPage < numMessages-1) {
                            showNextMessageWithRemove(qm);
                        } else {
                            showPreviousMessageWithRemove(qm);
                        }
                    }
                }
            }
        });

        // View button
        mViewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // Override the re-lock if the screen was unlocked
                if (mScreenUnlocked) {
                    // Cancel the receiver that will clear the wake locks
                    ClearAllReceiver.removeCancel(getApplicationContext());
                    ClearAllReceiver.clearAll(false);
                    mScreenUnlocked = false;
                }

                // Trigger the view intent
                mCurrentQm = mMessageList.get(mCurrentPage);
                Intent vi = mCurrentQm.getViewIntent();
                if (vi != null) {
                    mCurrentQm.saveReplyText();
                    vi.putExtra("sms_body", mCurrentQm.getReplyText());

                    startActivity(vi);
                }
                clearNotification(false);
                finish();
            }
        });
    }

    private void parseIntent(Bundle extras, boolean newMessage) {
        if (extras == null) {
            return;
        }

        // Check if we are being called to remove messages already showing
        if (extras.getBoolean(QR_REMOVE_MESSAGES_EXTRA, false)) {
            // Get the ID
            long threadId = extras.getLong(QR_THREAD_ID_EXTRA, -1);
            if (threadId != -1) {
                removeMatchingMessages(threadId);
            }
        } else {
            // Parse the intent and ensure we have a notification object to work with
            NotificationInfo nm = (NotificationInfo) extras.getParcelable(SMS_NOTIFICATION_OBJECT_EXTRA);
            if (nm != null) {
                QuickMessage qm = new QuickMessage(extras.getString(SMS_FROM_NAME_EXTRA),
                        extras.getString(SMS_FROM_NUMBER_EXTRA), nm);
                mMessageList.add(qm);
                mPagerAdapter.notifyDataSetChanged();

                // If triggered from Quick Reply the keyboard should be visible immediately
                if (extras.getBoolean(QR_SHOW_KEYBOARD_EXTRA, false)) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

                if (newMessage && mCurrentPage != -1) {
                    // There is already a message showing
                    // Stay on the current message
                    mMessagePager.setCurrentItem(mCurrentPage);
                } else {
                    // Set the current message to the last message received
                    mCurrentPage = mMessageList.size()-1;
                    mMessagePager.setCurrentItem(mCurrentPage);
                }

                if (DEBUG)
                    Log.d(LOG_TAG, "parseIntent(): New message from " + qm.getFromName().toString()
                            + " added. Number of messages = " + mMessageList.size()
                            + ". Displaying page #" + (mCurrentPage+1));

                // Make sure the counter is accurate
                updateMessageCounter();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DEBUG)
            Log.d(LOG_TAG, "onNewIntent() called");

        // Set new intent
        setIntent(intent);

        // Load and display the new message
        parseIntent(intent.getExtras(), true);
        unlockScreen();
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
            ClearAllReceiver.clearAll(true);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.clear();
        // Templates menu item, if there are defined templates
        if (mNumTemplates > 0) {
            menu.add(0, MENU_ADD_TEMPLATE, 0, R.string.template_insert)
            .setIcon(android.R.drawable.ic_menu_add)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_TEMPLATE:
                selectTemplate();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    //==========================================================
    // Utility methods
    //==========================================================

    /**
     * Copied from ComposeMessageActivity.java, this method displays the available
     * templates and allows the user to select and append it to the reply text. It
     * has been modified to work with this class
     */
    private void selectTemplate() {
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * This method dismisses the on screen keyboard if it is visible for the supplied qm
     *
     * @param qm - qm to check against
     */
    private void dismissKeyboard(QuickMessage qm) {
        if (qm != null) {
            EditText editView = qm.getEditText();
            if (editView != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editView.getApplicationWindowToken(), 0);
            }
        }
    }

    /**
     * If 'Wake and unlock' is enabled, this method will unlock the screen
     */
    private void unlockScreen() {
        // See if the lock screen should be disabled
        if (!mWakeAndUnlock) {
            return;
        }

        // See if the screen is locked or if no lock set and the screen is off
        // and get the wake lock to turn on the screen.
        boolean isScreenOn = mPowerManager.isScreenOn();
        boolean inKeyguardRestrictedInputMode = mKeyguardManager.inKeyguardRestrictedInputMode();
        if (inKeyguardRestrictedInputMode || ((!inKeyguardRestrictedInputMode) && !isScreenOn)) {
            ManageWakeLock.acquireFull(mContext);
            mScreenUnlocked = true;
        }
    }

    /**
     * Update the page indicator counter to show the currently selected visible page number
     */
    public void updateMessageCounter() {
        String separator = mContext.getString(R.string.message_counter_separator);
        mQmMessageCounter.setText((mCurrentPage + 1) + " " + separator + " " + mMessageList.size());

        if (DEBUG)
            Log.d(LOG_TAG, "updateMessageCounter() called, counter text set to " + (mCurrentPage + 1)
                    + " of " + mMessageList.size());
    }

    /**
     * Remove the supplied qm from the ViewPager and show the previous/older message
     *
     * @param qm
     */
    public void showPreviousMessageWithRemove(QuickMessage qm) {
        if (qm != null) {
            if (DEBUG)
                Log.d(LOG_TAG, "showPreviousMessageWithRemove()");

            markCurrentMessageRead(qm);
            if (mCurrentPage > 0) {
                updatePages(mCurrentPage-1, qm);
            }
        }
    }

    /**
     * Remove the supplied qm from the ViewPager and show the next/newer message
     *
     * @param qm
     */
    public void showNextMessageWithRemove(QuickMessage qm) {
        if (qm != null) {
            if (DEBUG)
                Log.d(LOG_TAG, "showNextMessageWithRemove()");

            markCurrentMessageRead(qm);
            if (mCurrentPage < (mMessageList.size() - 1)) {
                updatePages(mCurrentPage, qm);
            }
        }
    }

    /**
     * Handle qm removal and the move to and display of the appropriate page
     *
     * @param gotoPage - page number to display after the removal
     * @param removeMsg - qm to remove from ViewPager
     */
    private void updatePages(int gotoPage, QuickMessage removeMsg) {
        mMessageList.remove(removeMsg);
        mPagerAdapter.notifyDataSetChanged();
        mMessagePager.setCurrentItem(gotoPage);
        updateMessageCounter();

        if (DEBUG)
            Log.d(LOG_TAG, "updatePages(): Removed message " + removeMsg.getThreadId()
                    + " and changed to page #" + (gotoPage+1) + ". Remaining messages = "
                    + mMessageList.size());
    }

    /**
     * Remove all matching quickmessages for the supplied thread id
     *
     * @param threadId
     */
    public void removeMatchingMessages(long threadId) {
        if (DEBUG)
            Log.d(LOG_TAG, "removeMatchingMessages() looking for match with threadID = " + threadId);

        Iterator<QuickMessage> itr = mMessageList.iterator();
        QuickMessage qmElement = null;

        // Iterate through the list and remove the messages that match
        while(itr.hasNext()){
            qmElement = itr.next();
            if(qmElement.getThreadId() == threadId) {
                itr.remove();
            }
        }

        // See if there are any remaining messages and update the pager
        if (mMessageList.size() > 0) {
            mPagerAdapter.notifyDataSetChanged();
            mMessagePager.setCurrentItem(1); // First message
            updateMessageCounter();
        } else {
            // we are done
            finish();
        }
    }

    /**
     * Marks the supplied qm as read
     *
     * @param qm
     */
    private void markCurrentMessageRead(QuickMessage qm) {
        if (qm != null) {
            Conversation con = Conversation.get(mContext, qm.getThreadId(), true);
            if (con != null) {
                con.markAsRead(false);
                if (DEBUG)
                    Log.d(LOG_TAG, "markCurrentMessageRead(): Marked message " + qm.getThreadId()
                            + " as read");
            }
        }
    }

    /**
     * Marks all qm's in the message list as read
     */
    private void markAllMessagesRead() {
        // This iterates through our MessageList and marks the contained threads as read
        for (QuickMessage qm : mMessageList) {
            Conversation con = Conversation.get(mContext, qm.getThreadId(), true);
            if (con != null) {
                con.markAsRead(false);
                if (DEBUG)
                    Log.d(LOG_TAG, "markAllMessagesRead(): Marked message " + qm.getThreadId()
                            + " as read");
            }
        }
    }

    /**
     * Show the appropriate image for the QuickContact badge
     *
     * @param badge
     * @param addr
     * @param isSelf
     */
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

    /**
     * Use standard api to send the supplied message
     *
     * @param message - message to send
     * @param qm - qm to reply to (for sender details)
     */
    private void sendQuickMessage(String message, QuickMessage qm) {
        if (message != null && qm != null) {
            long threadId = qm.getThreadId();
            SmsMessageSender sender = new SmsMessageSender(getBaseContext(),
                    qm.getFromNumber(), message, threadId);
            try {
                if (DEBUG)
                    Log.d(LOG_TAG, "sendQuickMessage(): Sending message to " + qm.getFromName()
                            + ", with threadID = " + threadId + ". Current page is #" + (mCurrentPage+1));

                sender.sendMessage(threadId);
                Toast.makeText(mContext, R.string.toast_sending_message, Toast.LENGTH_SHORT).show();
            } catch (MmsException e) {
                Log.e(LOG_TAG, "Error sending message to " + qm.getFromName());
            }
        }
    }

    /**
     * Clears the status bar notification and, optionally, mark all messages as read
     * This is used to clean up when we are done with all qm's
     *
     * @param markAsRead - should remaining qm's be maked as read?
     */
    private void clearNotification(boolean markAsRead) {
        // Dismiss the notification that brought us here.
        NotificationManager notificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MessagingNotification.NOTIFICATION_ID);

        // Mark all contained conversations as seen
        if (markAsRead) {
            markAllMessagesRead();
        }

        // Clear the messages list
        mMessageList.clear();
        mPagerAdapter.notifyDataSetChanged();

        if (DEBUG)
            Log.d(LOG_TAG, "clearNotification(): Message list cleared. Size = " + mMessageList.size());
    }

    /**
     * This method queries the Templates database and returns the count of templates
     *
     * @return - number of templates
     */
    private int getTemplatesCount() {
        Cursor cur = getContentResolver().query(Template.CONTENT_URI, null, null, null, null);
        int numColumns = cur.getCount();
        cur.close();
        return numColumns;
    }

    /**
     * Async data loader used for loading and displaying Templates
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Template.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data != null && data.getCount() > 0){
            showDialog(DIALOG_TEMPLATE_SELECT);
            mTemplatesCursorAdapter.swapCursor(data);
        } else {
            showDialog(DIALOG_TEMPLATE_NOT_AVAILABLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * This displays the Templates selection dialog
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_TEMPLATE_NOT_AVAILABLE:
                builder.setTitle(R.string.template_not_present_error_title);
                builder.setMessage(R.string.template_not_present_error);
                return builder.create();

            case DIALOG_TEMPLATE_SELECT:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.template_select);
                mTemplatesCursorAdapter  = new SimpleCursorAdapter(this,
                        android.R.layout.simple_list_item_1, null, new String[] {
                        Template.TEXT
                    }, new int[] {
                        android.R.id.text1
                    }, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                builder.setAdapter(mTemplatesCursorAdapter, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       // Get the selected template and text
                       Cursor c = (Cursor) mTemplatesCursorAdapter.getItem(which);
                       String text = c.getString(c.getColumnIndex(Template.TEXT));

                       // Get the currently visible message and append text
                       QuickMessage qm = mMessageList.get(mCurrentPage);
                       if (qm != null) {
                           // insert the template text at the cursor location or replace selected
                           int start = qm.getEditText().getSelectionStart();
                           int end = qm.getEditText().getSelectionEnd();
                           qm.getEditText().getText().replace(Math.min(start, end),
                                   Math.max(start, end), text);
                       }
                    }
                });
                return builder.create();
        }
        return super.onCreateDialog(id, args);
    }

    //==========================================================
    // Inner classes
    //==========================================================

    /**
     * Message Pager class, used to display and navigate through the ViewPager pages
     */
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
            View layout;
            if (mDarkTheme) {
                layout = inflater.inflate(R.layout.quickmessage_content_dark, null);
            } else {
                layout = inflater.inflate(R.layout.quickmessage_content_light, null);
            }

            // Load the main views
            EditText qmReplyText = (EditText) layout.findViewById(R.id.embedded_text_editor);
            TextView qmTextCounter = (TextView) layout.findViewById(R.id.text_counter);
            ImageButton qmSendButton = (ImageButton) layout.findViewById(R.id.send_button_sms);
            ImageButton qmTemplatesButton = (ImageButton) layout.findViewById(R.id.templates_button);
            TextView qmMessageText = (TextView) layout.findViewById(R.id.messageTextView);
            TextView qmFromName = (TextView) layout.findViewById(R.id.fromTextView);
            TextView qmTimestamp = (TextView) layout.findViewById(R.id.timestampTextView);
            QuickContactBadge qmContactBadge = (QuickContactBadge) layout.findViewById(R.id.contactBadge);

            // Retrieve the current message
            QuickMessage qm = mMessageList.get(position);
            if (qm != null) {
                if (DEBUG)
                    Log.d(LOG_TAG, "instantiateItem(): Creating page #" + (position + 1) + " for message from "
                            + qm.getFromName() + ". Number of pages to create = " + getCount());

                // Set the general fields
                qmFromName.setText(qm.getFromName());
                qmTimestamp.setText(MessageUtils.formatTimeStampString(mContext, qm.getTimestamp(), mFullTimestamp));
                updateContactBadge(qmContactBadge, qm.getFromNumber()[0], false);
                qmMessageText.setText(qm.getMessageBody());

                if (!mDarkTheme) {
                    // We are using a holo.light background with a holo.dark activity theme
                    // Override the EditText background to use the holo.light theme
                    qmReplyText.setBackgroundResource(R.drawable.edit_text_holo_light);
                }

                // Set the remaining values
                qmReplyText.setInputType(InputType.TYPE_CLASS_TEXT | mInputMethod
                        | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                qmReplyText.setText(qm.getReplyText());
                qmReplyText.setSelection(qm.getReplyText().length());

                if (mUnicodeStripping != MessagingPreferenceActivity.UNICODE_STRIPPING_LEAVE_INTACT) {
                    boolean stripNonDecodableOnly =
                            mUnicodeStripping == MessagingPreferenceActivity.UNICODE_STRIPPING_NON_DECODABLE;
                    mUnicodeFilter = new UnicodeFilter(stripNonDecodableOnly);
                }

                qmReplyText.addTextChangedListener(new QmTextWatcher(mContext, qmTextCounter, qmSendButton,
                        qmTemplatesButton, mNumTemplates, mUnicodeFilter));
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
                                QuickMessage qm = mMessageList.get(mCurrentPage);
                                if (qm != null) {
                                    sendMessageAndMoveOn(v.getText().toString(), qm);
                                }
                            }
                            return true;
                        }
                        return true;
                    }
                });

                LengthFilter lengthFilter = new LengthFilter(MmsConfig.getMaxTextLimit());
                qmReplyText.setFilters(new InputFilter[] { lengthFilter });

                QmTextWatcher.getQuickReplyCounterText(qmReplyText.getText().toString(),
                        qmTextCounter, qmSendButton, qmTemplatesButton, mNumTemplates);

                // Add the context menu
                registerForContextMenu(qmReplyText);

                // Store the EditText object for future use
                qm.setEditText(qmReplyText);

                // Templates button
                qmTemplatesButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectTemplate();
                    }
                });

                // Send button
                qmSendButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QuickMessage qm = mMessageList.get(mCurrentPage);
                        if (qm != null) {
                            EditText editView = qm.getEditText();
                            if (editView != null) {
                                sendMessageAndMoveOn(editView.getText().toString(), qm);
                            }
                        }
                    }
                });

                // Add the layout to the viewpager
                ((ViewPager) collection).addView(layout);
            }
            return layout;
        }

        /**
         * This method sends the supplied message in reply to the supplied qm and then
         * moves to the next or previous message as appropriate. If this is the last qm
         * in the MessageList, we end by clearing the notification and calling finish()
         *
         * @param message - message to send
         * @param qm - qm we are replying to (for sender details)
         */
        private void sendMessageAndMoveOn(String message, QuickMessage qm) {
            if (mUnicodeFilter != null) {
                message = mUnicodeFilter.filter(message).toString();
            }
            sendQuickMessage(message, qm);
            // Close the current QM and move on
            int numMessages = mMessageList.size();
            if (numMessages == 1) {
                // No more messages
                clearNotification(true);
                finish();
            } else {
                // Dismiss the keyboard if it is shown
                dismissKeyboard(qm);

                if (mCurrentPage < numMessages-1) {
                    showNextMessageWithRemove(qm);
                } else {
                    showPreviousMessageWithRemove(qm);
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
            mCurrentPage = position;
            mCurrentQm = mMessageList.get(position);

            if (DEBUG)
                Log.d(LOG_TAG, "onPageSelected(): Current page is #" + (position+1)
                        + " of " + getCount() + " pages. Currenty visible message is from "
                        + mCurrentQm.getFromName());

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
                return view == ((LinearLayout)object);
        }

        @Override
        public void finishUpdate(View arg0) {}

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {}

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
