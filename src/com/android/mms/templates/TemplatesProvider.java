
package com.android.mms.templates;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class TemplatesProvider extends ContentProvider {

    private static final String TYPE = "vnd.android.cursor.dir/vnd.com.android.cm.mms.template";

    private static final String DB_NAME = "message_templates.db";

    private static final String TABLE_NAME = "message_template";

    private static final int DB_VERSION = 1;

    private static final int TEMPLATES = 1;

    private static final int TEMPLATE_ID = 2;

    private static UriMatcher sMatcher;

    SQLiteDatabase mDb;

    public static class Template implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.android.cm.mms/templates");
        public static final String TEXT = "text";
    }

    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatcher.addURI("com.android.cm.mms", "templates", TEMPLATES);
        sMatcher.addURI("com.android.cm.mms", "templates/#", TEMPLATE_ID);
    }

    private static final String TEMPLATE_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME + " (" + Template._ID
            + " integer primary key autoincrement, " + Template.TEXT
            + " text not null);";

    @Override
    public boolean onCreate() {
        DbHelper dbHelper = new DbHelper(getContext(), DB_NAME, null, DB_VERSION);
        mDb = dbHelper.getWritableDatabase();
        return mDb != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        queryBuilder.setTables(TABLE_NAME);

        if (sMatcher.match(uri) == TEMPLATE_ID) {
            queryBuilder.appendWhere(Template._ID + "=" + uri.getPathSegments().get(1));
        }

        Cursor cursor = queryBuilder.query(mDb, projection, selection, selectionArgs,
                null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        if (sMatcher.match(uri) != TEMPLATES) {
            throw new IllegalArgumentException("Unknown URL " + uri);
        }

        if (!values.containsKey(Template.TEXT)) {
            throw new IllegalArgumentException("Text is missing");
        }

        long rowID = mDb.insert(TABLE_NAME, null, values);

        if (rowID > 0) {
            getContext().getContentResolver().notifyChange(Template.CONTENT_URI, null);
            return ContentUris.withAppendedId(Template.CONTENT_URI, rowID);
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count;

        if (sMatcher.match(uri) == TEMPLATES) {
            count = mDb.delete(TABLE_NAME, selection, selectionArgs);
        }
        else {
            String segment = uri.getPathSegments().get(1);
            count = mDb
                    .delete(TABLE_NAME, Template._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                    + ')' : ""), selectionArgs);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;

        if (sMatcher.match(uri) == TEMPLATES) {
            count = mDb.update(TABLE_NAME, values, selection, selectionArgs);
        }
        else {
            String segment = uri.getPathSegments().get(1);
            count = mDb
                    .update(TABLE_NAME, values, Template._ID + "="
                            + segment
                            + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                    + ')' : ""), selectionArgs);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TEMPLATE_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

    }

}
