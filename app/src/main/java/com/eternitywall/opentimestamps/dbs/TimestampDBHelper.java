package com.eternitywall.opentimestamps.dbs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.opentimestamps.models.SerializedTimestamp;
import com.eternitywall.ots.StreamDeserializationContext;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.attestation.TimeAttestation;
import com.eternitywall.ots.op.Op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimestampDBHelper extends SerializedTimestampDBHelper {


    public TimestampDBHelper(Context context) {
        super(context);
    }

    private Timestamp popTimestamp(byte[] msg){
        // Get a timestamp, non-recursively
        SerializedTimestamp serializedTimestamp = super.get(msg);
        StreamDeserializationContext ctx = new StreamDeserializationContext(serializedTimestamp.serialized);

        Timestamp timestamp = new Timestamp(msg);
        for (int i = 0; i < ctx.readVaruint(); i++){
            TimeAttestation attestation = TimeAttestation.deserialize(ctx);
            timestamp.attestations.add(attestation);
        }
        for (int i = 0; i < ctx.readVaruint(); i++){
            Op op = Op.deserialize(ctx);
            timestamp.add(op);
        }
        return timestamp;
    }

    public Timestamp getTimestamp(byte[] msg){
        // Get the timestamp for a given message
        Timestamp timestamp = popTimestamp(msg);

        for (Map.Entry<Op, Timestamp> entry : timestamp.ops.entrySet()) {
            Timestamp stamp = entry.getValue();
            Op op = entry.getKey();
            timestamp.ops.put(op, getTimestamp(stamp.msg));
        }
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

        SerializedTimestamp serializedTimestamp = new SerializedTimestamp();
        serializedTimestamp.msg = new_timestamp.msg;
        serializedTimestamp.serialized = ctx.getOutput();
        create(serializedTimestamp);

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

