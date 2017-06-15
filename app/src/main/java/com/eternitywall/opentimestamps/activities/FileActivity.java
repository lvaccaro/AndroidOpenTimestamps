package com.eternitywall.opentimestamps.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eternitywall.opentimestamps.GoogleUrlShortener;
import com.eternitywall.opentimestamps.IOUtil;
import com.eternitywall.opentimestamps.R;
import com.eternitywall.opentimestamps.adapters.FolderAdapter;
import com.eternitywall.opentimestamps.adapters.ItemAdapter;
import com.eternitywall.opentimestamps.dbs.TimestampDBHelper;
import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.ots.Calendar;
import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.Hash;
import com.eternitywall.ots.OpenTimestamps;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.Utils;
import com.eternitywall.ots.op.OpSHA256;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ItemAdapter mAdapter;
    private LinkedHashMap<String,String> mDataset;
    private RecyclerView.LayoutManager mLayoutManager;
    TimestampDBHelper timestampDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(this, R.drawable.divider_grey);
        horizontalDecoration.setDrawable(horizontalDivider);
        mRecyclerView.addItemDecoration(horizontalDecoration);

        // Specify and fill the adapter
        mDataset = new LinkedHashMap<>();
        mAdapter = new ItemAdapter(mDataset);
        mRecyclerView.setAdapter(mAdapter);

        // Set button
        findViewById(R.id.btnInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onInfoClick();
            }
        });
        findViewById(R.id.btnDownload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadClick();
            }
        });

        // Check DB
        timestampDBHelper = new TimestampDBHelper(this);

        // Get intent file
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                String scheme = uri.getScheme();
                if (scheme.equals("content")) {
                    /*String mimeType = intent.getType();
                    ContentResolver contentResolver = getContentResolver();
                    Cursor cursor = contentResolver.query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    refresh(filePath);*/
                    refresh(uri);
                } else {
                    refresh(uri);
                }
            }
        }
    }

    Timestamp timestamp;
    byte[] hash;
    byte[] ots;
    Long date;



    private void refresh (final Uri uri) {
        final ContentResolver contentResolver = getContentResolver();

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {


                try {
                    // Read file
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count = inputStream.read(buffer);
                    while (count >= 0) {
                        outputStream.write(buffer,0,count);
                        count = inputStream.read(buffer);
                    }

                    // Calculate Hash
                    hash = IOUtil.SHA256(outputStream.toByteArray());
                    Log.d("FILE", "HASH: "+IOUtil.bytesToHex(hash));

                    // check hash into DB
                    timestamp = timestampDBHelper.getTimestamp(hash);
                    if(timestamp != null){
                        Log.d("FILE", timestamp.strTree(0));
                        ots = getOts();
                    }

                    // verify OTS
                    date = OpenTimestamps.verify(ots,hash);

                    // upgrade
                    if (date == null || date == 0){
                        ots = OpenTimestamps.upgrade(ots);
                        date = OpenTimestamps.verify(ots,hash);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mAdapter.notifyDataSetChanged();
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);

                mDataset.put("Name",uri.getLastPathSegment());
                mDataset.put("Uri",uri.toString());
                mDataset.put("Type",contentResolver.getType(uri).toString());
                mDataset.put("HASH", IOUtil.bytesToHex(hash));
                if(timestamp == null){
                    mDataset.put("OTS PROOF", "File not timestamped");
                } else {
                    mDataset.put("OTS PROOF", IOUtil.bytesToHex(ots));
                }
                if(date == null || date == 0){
                    mDataset.put("Attestation", "Pending or Bad attestation");
                } else {
                    try{
                        //Thu May 28 2015 17:41:18 GMT+0200 (CEST)
                        DateFormat sdf = new SimpleDateFormat("EE MMM dd yyyy hh:mm:ss z");
                        Date netDate = new Date(date*1000);
                        mDataset.put("Attestation", "Bitcoin attests data existed as of "+sdf.format(netDate));
                    }
                    catch(Exception ex){
                        mDataset.put("Attestation", "Invalid date");
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        }.execute();
    }


    public byte[] getOts(){
        if(timestamp == null){
            return null;
        }
        DetachedTimestampFile detachedTimestampFile = new DetachedTimestampFile(new OpSHA256(),timestamp);
        StreamSerializationContext ctx = new StreamSerializationContext();
        detachedTimestampFile.serialize(ctx);
        return ctx.getOutput();
    }

    public void onDownloadClick() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, ots);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, "Share proof to.."));
    }

    public void onInfoClick() {
        new AsyncTask<Void, Void, Boolean>() {
            String shortUrl = "";

            @Override
            protected Boolean doInBackground(Void... params) {
                String otsString = IOUtil.bytesToHex(ots);
                String url = "https://opentimestamps.org/info.html?ots=";
                url += otsString;
                shortUrl = GoogleUrlShortener.shorten(url);
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(shortUrl));
                startActivity(i);
            }
        }.execute();
    }


}
