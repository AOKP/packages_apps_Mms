/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.mms.templates;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.mms.R;
import com.android.mms.templates.TemplatesProvider.Template;
import com.android.mms.ui.MessagingPreferenceActivity;

public class TemplatesListActivity extends ListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // codes for dialogs
    private static final int LOAD_TEMPLATES = 1;

    private ListView mListView;

    private SimpleCursorAdapter mAdapter;

    protected void createNewTemplate() {
        final Intent intent = new Intent(this, TemplateEditor.class);
        intent.putExtra(TemplateEditor.KEY_DISPLAY_TYPE, TemplateEditor.DISPLAY_TYPE_NEW_TEMPLATE);
        startActivity(intent);
    }

    protected void doDeleteTemplates(long[] templatesToDeleteIds) {
        int count = templatesToDeleteIds.length;
        for (int i = 0; i < count; i++) {
            Uri uriToDelete = ContentUris
                    .withAppendedId(Template.CONTENT_URI, templatesToDeleteIds[i]);
            getContentResolver().delete(uriToDelete, null, null);
            TemplateGesturesLibrary.getStore(this)
                    .removeEntry(String.valueOf(templatesToDeleteIds[i]));
        }
    }

    protected void modifyTemplate(long id) {
        final Intent intent = new Intent(this, TemplateEditor.class);
        intent.putExtra(TemplateEditor.KEY_DISPLAY_TYPE, TemplateEditor.DISPLAY_TYPE_EDIT_TEMPLATE);
        intent.putExtra(TemplateEditor.KEY_TEMPLATE_ID, id);
        startActivity(intent);
    }

    private void confirmDeleteTemplates() {
        final long[] templatesToDeleteIds = mListView.getCheckedItemIds();
        int count = templatesToDeleteIds.length;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.template_confirm_dialog_title);
        builder.setMessage(getResources().getQuantityString(R.plurals.template_confirm_delete_conversation, count, count));

        builder.setPositiveButton(R.string.delete, new Dialog.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                doDeleteTemplates(templatesToDeleteIds);
            }
        });

        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.templates_list);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new ModeCallback());

        getLoaderManager().initLoader(LOAD_TEMPLATES, null, this);

        mAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_activated_1, null, new String[] {
                        Template.TEXT
                }, new int[] {
                        android.R.id.text1
                }, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(findViewById(R.id.empty));
        mListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id) {
                modifyTemplate(id);
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.template_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MessagingPreferenceActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.action_template_new:
                createNewTemplate();
                return true;
            default:
                return false;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Template.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private TextView mSelectedTemplatesCount;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();

            inflater.inflate(R.menu.template_list_multi_select_menu, menu);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(TemplatesListActivity.this)
                        .inflate(R.layout.template_list_multi_select_actionbar, null);

                mSelectedTemplatesCount = (TextView) mMultiSelectActionBarView
                        .findViewById(R.id.selected_template_count);
            }

            mode.setCustomView(mMultiSelectActionBarView);

            ((TextView) mMultiSelectActionBarView.findViewById(R.id.title))
                    .setText(R.string.select_template);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup) LayoutInflater.from(TemplatesListActivity.this)
                        .inflate(R.layout.template_list_multi_select_actionbar, null);

                mode.setCustomView(v);
                mSelectedTemplatesCount = (TextView) v.findViewById(R.id.selected_template_count);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    if (mListView.getCheckedItemCount() > 0) {
                        confirmDeleteTemplates();
                    }
                    mode.finish();
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mSelectedTemplatesCount.setText(Integer.toString(mListView.getCheckedItemCount()));
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Do nothing here
        }
    }
}
