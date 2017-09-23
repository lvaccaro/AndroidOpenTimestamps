package com.eternitywall.opentimestamps.dbs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.eternitywall.opentimestamps.models.Folder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luca on 09/06/2017.
 */

public class FolderDBHelper extends DBHelper {


    public FolderDBHelper(Context context) {
        super(context);
    }

    public long create(Folder folder) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, folder.name);
        values.put(KEY_ROOTDIR, folder.roodDir);
        values.put(KEY_ENABLED, (folder.enabled)?1:0);
        //values.put(KEY_STATE, Folder.stateToInt(folder.state));
        values.put(KEY_LASTSYNC, folder.lastSync);
        values.put(KEY_COUNTFILES, folder.countFiles);
        values.put(KEY_OTS, folder.ots);
        values.put(KEY_HASH, folder.hash);

        // insert row
        long id = db.insert(TABLE_FOLDERS, null, values);
        return id;
    }

    /*
 * get single
 */
    public Folder get(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_FOLDERS + " WHERE "
                + KEY_ID + " = " + id;

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        Folder folder = new Folder();
        folder.id = c.getInt(c.getColumnIndex(KEY_ID));
        folder.name = c.getString(c.getColumnIndex(KEY_NAME));
        folder.roodDir = c.getString(c.getColumnIndex(KEY_ROOTDIR));
        folder.enabled = (c.getInt(c.getColumnIndex(KEY_ENABLED)) == 1);
        //folder.state = Folder.intToState(c.getInt(c.getColumnIndex(KEY_STATE)));
        folder.lastSync = c.getLong(c.getColumnIndex(KEY_LASTSYNC));
        folder.countFiles = c.getLong(c.getColumnIndex(KEY_COUNTFILES));
        folder.ots = c.getBlob(c.getColumnIndex(KEY_OTS));
        folder.hash = c.getBlob(c.getColumnIndex(KEY_HASH));

        c.close();
        return folder;
    }

    /*
 * getting all
 * */
    public List<Folder> getAll() {
        List<Folder> folders = new ArrayList<Folder>();
        String selectQuery = "SELECT  * FROM " + TABLE_FOLDERS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Folder folder = new Folder();
                folder.id = c.getInt(c.getColumnIndex(KEY_ID));
                folder.name = c.getString(c.getColumnIndex(KEY_NAME));
                folder.roodDir = c.getString(c.getColumnIndex(KEY_ROOTDIR));
                folder.enabled = (c.getInt(c.getColumnIndex(KEY_ENABLED)) == 1);
                //folder.state = Folder.intToState(c.getInt(c.getColumnIndex(KEY_STATE)));
                folder.lastSync = c.getLong(c.getColumnIndex(KEY_LASTSYNC));
                folder.countFiles = c.getLong(c.getColumnIndex(KEY_COUNTFILES));
                folder.ots = c.getBlob(c.getColumnIndex(KEY_OTS));
                folder.hash = c.getBlob(c.getColumnIndex(KEY_HASH));
                // adding to todo list
                folders.add(folder);
            } while (c.moveToNext());
        }

        c.close();
        return folders;
    }

    /*
 * Updating
 */
    public int update(Folder folder) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, folder.name);
        values.put(KEY_ROOTDIR, folder.roodDir);
        values.put(KEY_ENABLED, (folder.enabled)?1:0);
        //values.put(KEY_STATE, Folder.stateToInt(folder.state));
        values.put(KEY_LASTSYNC, folder.lastSync);
        values.put(KEY_COUNTFILES, folder.countFiles);
        values.put(KEY_OTS, folder.ots);
        values.put(KEY_HASH, folder.hash);

        // updating row
        return db.update(TABLE_FOLDERS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(folder.id) });
    }
}
