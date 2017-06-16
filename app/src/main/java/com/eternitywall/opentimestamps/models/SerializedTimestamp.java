package com.eternitywall.opentimestamps.models;

import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.StreamDeserializationContext;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.op.OpSHA256;

import org.spongycastle.util.Times;

import java.util.Arrays;

/**
 * Created by luca on 11/06/2017.
 */

public class SerializedTimestamp {
    public long id;
    public byte[] msg;
    public byte[] serialized;

    public Timestamp deserialize (){
        StreamDeserializationContext ctx = new StreamDeserializationContext(serialized);
        Timestamp timestamp = Timestamp.deserialize(ctx,msg);
        return timestamp;
    }

    public void serialize(Timestamp timestamp){
        StreamSerializationContext ctx = new StreamSerializationContext();
        timestamp.serialize(ctx);
        serialized = ctx.getOutput();
        msg = timestamp.getDigest();
    }

    public int getHashcode(){
        return Arrays.hashCode(msg);
    }
}
