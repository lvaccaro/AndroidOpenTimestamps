package com.eternitywall.opentimestamps.dbs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.opentimestamps.models.SerializedTimestamp;
import com.eternitywall.ots.StreamDeserializationContext;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.Utils;
import com.eternitywall.ots.attestation.TimeAttestation;
import com.eternitywall.ots.op.Op;
import com.eternitywall.ots.op.OpPrepend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimestampDBHelper extends SerializedTimestampDBHelper {


    public TimestampDBHelper(Context context) {
        super(context);
    }

    private Timestamp popTimestamp(byte[] msg){
        // Get a timestamp, non-recursively
        SerializedTimestamp serializedTimestamp = null;
        try {
            serializedTimestamp = getByHashcode( Arrays.hashCode(msg) );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        StreamDeserializationContext ctx = new StreamDeserializationContext(serializedTimestamp.serialized);

        Timestamp timestamp = new Timestamp(msg);
        int count = ctx.readVaruint();
        for (int i = 0; i < count; i++){
            if (i>0){
                Log.d("","i>0");
            }
            TimeAttestation attestation = TimeAttestation.deserialize(ctx);
            if (timestamp.attestations.contains(attestation)){
                timestamp.attestations.set(timestamp.attestations.indexOf(attestation) , attestation);
            } else {
                timestamp.attestations.add(attestation);
            }
        }
        count = ctx.readVaruint();
        if(count>1){
            Log.d("","i>0");
        }
        for (int i = 0; i < count; i++){
            Op op = Op.deserialize(ctx);
            if (op instanceof OpPrepend && Arrays.equals( ((OpPrepend)op).arg , Utils.hexToBytes("5942fd9a") )) {
                Log.d("","===");
            }
            timestamp.add(op);
        }
        Log.d("","");
        return timestamp;
    }

    public Timestamp getTimestamp(byte[] msg){
        // Get the timestamp for a given message
        Timestamp timestamp = popTimestamp(msg);
        if (timestamp == null){
            return null;
        }

        Set<Op> keys = timestamp.ops.keySet();
        for (Op op : keys) {
            if (op instanceof OpPrepend && Arrays.equals( ((OpPrepend)op).arg , Utils.hexToBytes("5942fd9a") )) {
                Log.d("","===");
            }

            if(timestamp.ops.containsKey(op)) {
                Log.d("", "");
                Timestamp stamp = getTimestamp(timestamp.ops.get(op).msg);
                timestamp.ops.put(op, stamp);
            } else {
                Timestamp stamp = getTimestamp(timestamp.ops.get(op).msg);
                timestamp.ops.put(op, stamp);
            }
        }
        /*
        Set<Map.Entry<Op, Timestamp>> entries = timestamp.ops.entrySet();
        for (int i = 0 ; i < entries.size() ; i++){
            Map.Entry<Op, Timestamp> entry = entries
        }
        for (Map.Entry<Op, Timestamp> entry : entries) {
            Timestamp stamp = entry.getValue();
            Op op = entry.getKey();
            timestamp.put(op, getTimestamp(stamp.msg));
        }*/
        return timestamp;
    }


    private void pushTimestamp(Timestamp new_timestamp){
        // Write a single timestamp, non-recursively
        StreamSerializationContext ctx = new StreamSerializationContext();
        ctx.writeVaruint(new_timestamp.attestations.size());
        for (TimeAttestation attestation : new_timestamp.attestations){
            attestation.serialize(ctx);
        }

        ctx.writeVaruint(new_timestamp.ops.size());
        for (Map.Entry<Op, Timestamp> entry : new_timestamp.ops.entrySet()) {
            Timestamp stamp = entry.getValue();
            Op op = entry.getKey();
            op.serialize(ctx);
        }

        try {
            SerializedTimestamp serializedTimestamp = getByHashcode( Arrays.hashCode(new_timestamp.msg) );
            // check if just exist -> update
            if (!Arrays.equals(serializedTimestamp.serialized, ctx.getOutput())) {
                serializedTimestamp.serialized = ctx.getOutput();
                update(serializedTimestamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // check if not exist -> create
            SerializedTimestamp serializedTimestamp = new SerializedTimestamp();
            serializedTimestamp.msg = new_timestamp.msg;
            serializedTimestamp.serialized = ctx.getOutput();
            serializedTimestamp.id = create(serializedTimestamp);
        }
    }

    public void addTimestamp(Timestamp new_timestamp){
        Timestamp existingTimestamp = getTimestamp(new_timestamp.msg);
        if (existingTimestamp == null){
            existingTimestamp = new Timestamp(new_timestamp.msg);
        }

        // Update the existing timestamps attestations with those from the new
        // timestamp
        for (TimeAttestation attestation : new_timestamp.attestations){
            if (existingTimestamp.attestations.contains(attestation)){
                existingTimestamp.attestations.set(existingTimestamp.attestations.indexOf(attestation) , attestation);
            } else {
                existingTimestamp.attestations.add(attestation);
            }
        }

        for (Map.Entry<Op, Timestamp> entry : new_timestamp.ops.entrySet()) {
            Timestamp stamp = entry.getValue();
            Op op = entry.getKey();
            // Make sure the existing timestamp has this operation
            existingTimestamp.add(op);
            // Add the results timestamp to the calendar
            addTimestamp(stamp);
        }
        pushTimestamp(existingTimestamp);
    }
}

