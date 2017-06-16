package com.eternitywall.opentimestamps;

/**
 * Created by luca on 13/05/2017.
 */

import android.webkit.MimeTypeMap;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class IOUtil {


    public static byte[] SHA256 (byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bytes);
        return md.digest();
    }

    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    public static byte[] readFileSHA256(File file) throws IOException, NoSuchAlgorithmException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        int maxBuffer=1024*1024;
        byte[] buffer = new byte[maxBuffer];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Get and check length

            int count = 0;
            count = f.read(buffer,0,maxBuffer);
            while(count >=0 ){
                md.update(buffer,0,count);
                count = f.read(buffer,0,maxBuffer);
            }
            return md.digest();
        } finally {
            f.close();
        }
    }

    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static void arrayFill(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

}
