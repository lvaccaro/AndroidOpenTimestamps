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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by luca on 23/09/2017.
 */

public class Ots {

    public static DetachedTimestampFile read(byte[] ots) {
        StreamDeserializationContext sdc = new StreamDeserializationContext(ots);
        DetachedTimestampFile detached = DetachedTimestampFile.deserialize(sdc);
        return detached;
    }
    public static void write(DetachedTimestampFile detached, String filepath) throws IOException, NoSuchAlgorithmException {

        File file = new File(filepath);
        FileOutputStream fos = new FileOutputStream(file);

        StreamSerializationContext ssc = new StreamSerializationContext();
        detached.serialize(ssc);

        fos.write(ssc.getOutput());
        fos.close();
    }

    public static void write(ZipOutputStream out, DetachedTimestampFile detached, String filepath) throws IOException, NoSuchAlgorithmException {

        StreamSerializationContext ctx = new StreamSerializationContext();
        detached.serialize(ctx);
        ZipEntry entry = new ZipEntry(filepath);
        try {
            out.putNextEntry(entry);
            out.write(ctx.getOutput());
            out.closeEntry();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static DetachedTimestampFile hashing(File file) throws IOException, NoSuchAlgorithmException {
        Hash sha256 = new Hash(IOUtil.readFileSHA256(file));
        return DetachedTimestampFile.from(new OpSHA256(), sha256);
    }
}
