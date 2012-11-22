
package com.android.mms.templates;

import java.util.ArrayList;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.CursorLoader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.mms.R;
import com.android.mms.templates.TemplatesProvider.Template;
import com.android.mms.ui.MessagingPreferenceActivity;

public class TemplateEditor extends Activity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_DISPLAY_TYPE = "display_type";

    public static final String KEY_TEMPLATE_ID = "template_id";

    public static final int DISPLAY_TYPE_NEW_TEMPLATE = 1;

    public static final int DISPLAY_TYPE_EDIT_TEMPLATE = 2;

    private static final String KEY_GESTURE = "gesture";

    private static final float LENGTH_THRESHOLD = 120.0f;

    private Gesture mGesture;

    private EditText mTemplateTextField;

    private long mCurrentTemplateEditingId = -1;

    private boolean editingMode = false;

    private double mGestureSensitivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.template_editor);

        final Bundle bundle = getIntent().getExtras();
        processActivityIntent(bundle);

        initViews();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar that contains the "done" button
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, null);
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveTemplate();
                }
            });
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                            ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mGestureSensitivity = prefs
                .getInt(MessagingPreferenceActivity.GESTURE_SENSITIVITY_VALUE, 3);
    }

    private void initViews() {

        final GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());

        mTemplateTextField = (EditText) findViewById(R.id.template_text);

        if (editingMode) {
            loadExistingTemplate();
            mGesture = loadGestureIfExists(String.valueOf(mCurrentTemplateEditingId));
            if (mGesture != null)
                paintGesture(overlay, mGesture);
        }

    }

    private Gesture loadGestureIfExists(String name) {
        final GestureLibrary store = TemplateGesturesLibrary.getStore(this);

        final ArrayList<Gesture> gestures = store.getGestures(name);

        if (gestures != null && gestures.size() > 0) {
            return gestures.get(0);
        } else {
            return null;
        }
    }

    private void loadExistingTemplate() {
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mGesture = savedInstanceState.getParcelable(KEY_GESTURE);
        if (mGesture != null) {
            final GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
            paintGesture(overlay, mGesture);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGesture != null) {
            outState.putParcelable(KEY_GESTURE, mGesture);
        }
    }

    private void paintGesture(final GestureOverlayView overlay, final Gesture gesture) {

        if (gesture == null)
            return;

        overlay.post(new Runnable() {
            public void run() {
                overlay.setGesture(gesture);
            }
        });
    }

    private void processActivityIntent(Bundle bundle) {
        if (bundle != null) {
            final int displayType = bundle.getInt(KEY_DISPLAY_TYPE, DISPLAY_TYPE_NEW_TEMPLATE);

            if (displayType == DISPLAY_TYPE_EDIT_TEMPLATE) {
                editingMode = true;

                mCurrentTemplateEditingId = bundle.getLong(KEY_TEMPLATE_ID, Long.MIN_VALUE);

                if (mCurrentTemplateEditingId == Long.MIN_VALUE) {
                    throw new IllegalArgumentException(
                            "In editing mode you have to pass the message id");
                }
            }
        }
    }

    protected void saveTemplate() {

        final String templateText = mTemplateTextField.getText().toString();
        long messageId;

        if (templateText.trim().length() == 0) {
            Toast.makeText(this, R.string.template_empty_text, Toast.LENGTH_SHORT).show();
            return;
        } else {

            ContentValues cv = new ContentValues();
            cv.put(Template.TEXT, templateText);

            if (editingMode) {
                messageId = mCurrentTemplateEditingId;
                Uri uriToUpdate = ContentUris
                        .withAppendedId(Template.CONTENT_URI, mCurrentTemplateEditingId);
                getContentResolver().update(uriToUpdate, cv, null, null);
            } else {
                Uri newUri = getContentResolver().insert(Template.CONTENT_URI, cv);
                messageId = ContentUris.parseId(newUri);
            }

        }

        final GestureLibrary store = TemplateGesturesLibrary.getStore(this);

        if (editingMode) {
            store.removeEntry(String.valueOf(messageId));
        }

        if (mGesture != null) {
            store.addGesture(String.valueOf(messageId), mGesture);
            store.save();
        }

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uriToQuery = ContentUris
                .withAppendedId(Template.CONTENT_URI, mCurrentTemplateEditingId);
        return new CursorLoader(this, uriToQuery, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            data.moveToFirst();
            String templateText = data.getString(data.getColumnIndex(Template.TEXT));
            mTemplateTextField.setText(templateText);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // TODO Auto-generated method stub

    }

    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {

        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD) {
                overlay.clear(false);
            }

            if (isThereASimilarGesture(mGesture)) {
                Toast.makeText(TemplateEditor.this, R.string.gestures_already_present,
                        Toast.LENGTH_SHORT).show();
            }
        }

        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mGesture = null;
        }
    }

    public boolean isThereASimilarGesture(Gesture gesture) {

        final GestureLibrary store = TemplateGesturesLibrary.getStore(this);
        ArrayList<Prediction> predictions = store.recognize(gesture);

        for (Prediction prediction : predictions) {
            if (prediction.score > mGestureSensitivity) {

                if (editingMode
                        && prediction.name.equals(String.valueOf(mCurrentTemplateEditingId)))
                    continue;
                else
                    return true;
            }
        }

        return false;
    }

}
