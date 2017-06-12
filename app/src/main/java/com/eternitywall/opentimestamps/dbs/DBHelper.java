package com.eternitywall.opentimestamps.dbs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.ots.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luca on 09/06/2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    // Database
    protected static final String DATABASE_NAME = "opentimestamps.db";
    protected static final int DATABASE_VERSION = 1;

    // Table Names
    protected static final String TABLE_FOLDERS = "folders";
    protected static final String TABLE_TIMESTAMPS = "timestamps";

    // Column names
    protected static final String KEY_ID = "id";
    protected static final String KEY_NAME = "name";
    protected static final String KEY_ROOTDIR = "roodDir";
    protected static final String KEY_ENABLED = "enabled";
    protected static final String KEY_STATE = "state";
    protected static final String KEY_LASTSYNC = "lastSync";
    protected static final String KEY_COUNTFILES = "countFiles";
    protected static final String KEY_OTS = "ots";
    protected static final String KEY_HASH = "hash";

    protected static final String KEY_MSG = "msg";
    protected static final String KEY_SERIALIZE = "serialize";


    // table create statement
    protected static final String SQL_CREATE_FOLDERS = "CREATE TABLE " + TABLE_FOLDERS + " (" +
            " " + KEY_ID + " INTEGER PRIMARY KEY," +
            " " + KEY_NAME + " TEXT," +
            " " + KEY_ROOTDIR + " TEXT, " +
            " " + KEY_ENABLED + " INTEGER, " +
            " " + KEY_STATE + " INTEGER, " +
            " " + KEY_LASTSYNC + " lastSync LONG, " +
            " " + KEY_COUNTFILES + " countFiles LONG, " +
            " " + KEY_OTS + " ots BLOB, " +
            " " + KEY_HASH + " hash BLOB )";

    protected static final String SQL_CREATE_TIMESTAMPS = "CREATE TABLE " + TABLE_TIMESTAMPS + " (" +
            " " + KEY_ID + " INTEGER PRIMARY KEY," +
            " " + KEY_MSG + " BLOB, " +
            " " + KEY_SERIALIZE + " BLOB )";

    // table delete statement
    protected static final String SQL_DELETE_FOLDERS = "DROP TABLE IF EXISTS " + TABLE_FOLDERS + " ";
    protected static final String SQL_DELETE_TIMESTAMPS = "DROP TABLE IF EXISTS " + TABLE_TIMESTAMPS + " ";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FOLDERS);
        db.execSQL(SQL_CREATE_TIMESTAMPS);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_FOLDERS);
        db.execSQL(SQL_DELETE_TIMESTAMPS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_DELETE_FOLDERS);
        db.execSQL(SQL_DELETE_TIMESTAMPS);
        db.execSQL(SQL_CREATE_FOLDERS);
        db.execSQL(SQL_CREATE_TIMESTAMPS);
    }

}