package com.eternitywall.opentimestamps.dbs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eternitywall.opentimestamps.models.SerializedTimestamp;
import com.eternitywall.ots.StreamDeserializationContext;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.attestation.TimeAttestation;
import com.eternitywall.ots.op.Op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by luca on 09/06/2017.
 */

public class SerializedTimestampDBHelper extends DBHelper {


    public SerializedTimestampDBHelper(Context context) {
        super(context);
    }

    public long create(SerializedTimestamp stamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MSG, stamp.msg);
        values.put(KEY_SERIALIZE, stamp.serialized);

        // insert row
        long id = db.insert(TABLE_TIMESTAMPS, null, values);
        return id;
    }

    public SerializedTimestamp get(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_TIMESTAMPS + " WHERE "
                + KEY_ID + " = " + id;

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        SerializedTimestamp stamp = new SerializedTimestamp();
        stamp.id = c.getInt(c.getColumnIndex(KEY_ID));
        stamp.msg = c.getBlob(c.getColumnIndex(KEY_MSG));
        stamp.serialized = c.getBlob(c.getColumnIndex(KEY_SERIALIZE));
        return stamp;
    }

    public List<SerializedTimestamp> getAll() {
        List<SerializedTimestamp> stamps = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_TIMESTAMPS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                SerializedTimestamp stamp = new SerializedTimestamp();
                stamp.id = c.getInt(c.getColumnIndex(KEY_ID));
                stamp.msg = c.getBlob(c.getColumnIndex(KEY_MSG));
                stamp.serialized = c.getBlob(c.getColumnIndex(KEY_SERIALIZE));
                // adding to todo list
                stamps.add(stamp);
            } while (c.moveToNext());
        }

        return stamps;
    }

    public int update(SerializedTimestamp stamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MSG, stamp.msg);
        values.put(KEY_SERIALIZE, stamp.serialized);

        // updating row
        return db.update(TABLE_TIMESTAMPS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(stamp.id) });
    }


    public SerializedTimestamp get(byte[] msg){
        /*SQLiteDatab ase db = this.getWritableDatabase();
        SQLiteStatement p = db.compileStatement("SELECT * FROM "+TABLE_TIMESTAMPS+" WHERE id=?");
        p.bindBlob(1, msg);
        p.execute();*/

        List<SerializedTimestamp> stamps = getAll();
        for (SerializedTimestamp stamp : stamps){
            if (Arrays.equals(stamp.msg,msg)){
                return stamp;
            }
        }
        return null;
    }

}