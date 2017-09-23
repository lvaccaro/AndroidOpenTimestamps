package com.eternitywall.opentimestamps.models;

import com.eternitywall.opentimestamps.IOUtil;
import com.eternitywall.opentimestamps.dbs.TimestampDBHelper;
import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.Hash;
import com.eternitywall.ots.StreamDeserializationContext;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.op.OpSHA256;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by luca on 23/09/2017.
 */

public class Ots {

    public static Timestamp read(byte[] ots) {
        StreamDeserializationContext sdc = new StreamDeserializationContext(ots);
        DetachedTimestampFile detached = DetachedTimestampFile.deserialize(sdc);
        Timestamp stamp = detached.getTimestamp();
        return stamp;
    }

    public static void write(Timestamp stamp, ZipOutputStream out, String filename) throws IOException, NoSuchAlgorithmException {

        int MAXSIZE= 1024*1024;
        byte[] buffer = new byte[MAXSIZE];
        StreamSerializationContext ctx = new StreamSerializationContext();
        stamp.serialize(ctx);

        ZipEntry entry = new ZipEntry(filename);
        try {
            out.putNextEntry(entry);
            ByteArrayInputStream is = new ByteArrayInputStream(ctx.getOutput());
            BufferedInputStream origin = new BufferedInputStream(is, MAXSIZE);

            int count;
            while ((count = origin.read(buffer, 0, MAXSIZE)) != -1) {
                out.write(buffer, 0, count);
            }
            origin.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static DetachedTimestampFile hashing(File file) throws IOException, NoSuchAlgorithmException {
        Hash sha256 = new Hash(IOUtil.readFileSHA256(file));
        return DetachedTimestampFile.from(new OpSHA256(), sha256);
    }
}
