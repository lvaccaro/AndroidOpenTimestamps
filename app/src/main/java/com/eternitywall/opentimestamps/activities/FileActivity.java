package com.eternitywall.opentimestamps.activities;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eternitywall.opentimestamps.GoogleUrlShortener;
import com.eternitywall.opentimestamps.IOUtil;
import com.eternitywall.opentimestamps.R;
import com.eternitywall.opentimestamps.adapters.FolderAdapter;
import com.eternitywall.opentimestamps.adapters.ItemAdapter;
import com.eternitywall.opentimestamps.dbs.TimestampDBHelper;
import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.opentimestamps.models.Ots;
import com.eternitywall.ots.Calendar;
import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.Hash;
import com.eternitywall.ots.OpenTimestamps;
import com.eternitywall.ots.StreamDeserializationContext;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.eternitywall.opentimestamps.IOUtil.getMimeType;

public class FileActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ItemAdapter mAdapter;
    private LinkedHashMap<String,String> mDataset;
    private RecyclerView.LayoutManager mLayoutManager;
    ProgressBar mProgressBar;

    TimestampDBHelper timestampDBHelper;
    ContentResolver mContentResolver;
    Timestamp timestamp;
    byte[] ots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

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

        // Init content
        mContentResolver = getContentResolver();

        // Get intent file
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
                String scheme = uri.getScheme();
                if (scheme.equals("content")) {
                    /*String mimeType = intent.getType();
                    ContentResolver contentResolver = getContentResolver();
                    Cursor cursor = contentResolver.query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    refresh(filePath);*/
                    load(uri);
                } else {
                    load(uri);
                }
            }
        }
    }



    private void load (final Uri uri) {

        new AsyncTask<Void, Void, Boolean>() {
            Hash sha256;
            Long date;

            @Override
            protected Boolean doInBackground(Void... params) {

                try {
                    // Read file
                    InputStream inputStream = mContentResolver.openInputStream(uri);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count = inputStream.read(buffer);
                    while (count >= 0) {
                        outputStream.write(buffer,0,count);
                        count = inputStream.read(buffer);
                    }

                    // Calculate Hash
                    sha256 = new Hash( IOUtil.SHA256(outputStream.toByteArray()) );
                    Log.d("FILE", "HASH: "+IOUtil.bytesToHex(sha256.getValue()));

                    // check hash into DB
                    timestamp = timestampDBHelper.getTimestamp(sha256.getValue());
                    if(timestamp == null){
                        Log.d("FILE", "File not found");
                        return true;
                    }
                    Log.d("FILE", Timestamp.strTreeExtended(timestamp,0));
                    ots = getOts(timestamp);

                    // verify OTS
                    date = OpenTimestamps.verify(ots,sha256);

                    // upgrade
                    if (date == null || date == 0){
                        ots = OpenTimestamps.upgrade(ots);
                        date = OpenTimestamps.verify(ots,sha256);
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
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressBar.setVisibility(View.VISIBLE);

            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                mProgressBar.setVisibility(View.GONE);

                if(success==false){
                    // generic error
                    AlertDialog alert = new AlertDialog.Builder(FileActivity.this)
                            .setTitle(getString(R.string.warning))
                            .setMessage(getString(R.string.file_or_timestamp_error)+uri.getPath().toString())
                            .show();
                } else if(timestamp == null){
                    // not timestamped -> stamp
                    stamp(uri);
                } else {
                    refresh(uri,sha256,timestamp,date);
                }
            }
        }.execute();
    }

    private void stamp (final Uri uri) {

        new AsyncTask<Void, Void, Boolean>() {
            Hash sha256;

            @Override
            protected Boolean doInBackground(Void... params) {

                // Read file
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    InputStream inputStream = mContentResolver.openInputStream(uri);
                    byte[] buffer = new byte[1024];
                    int count = inputStream.read(buffer);
                    while (count >= 0) {
                        outputStream.write(buffer, 0, count);
                        count = inputStream.read(buffer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                // Calculate Hash
                final List<DetachedTimestampFile> fileTimestamps = new ArrayList<>();
                try {
                    sha256 = new Hash(IOUtil.SHA256(outputStream.toByteArray()));
                    Log.d("FILE", "HASH: " + IOUtil.bytesToHex(sha256.getValue()));
                    fileTimestamps.add(DetachedTimestampFile.from(new OpSHA256(), sha256));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Stamp the markled list
                try {
                    Timestamp merkleTip = OpenTimestamps.makeMerkleTree(fileTimestamps);
                    byte[] hash = merkleTip.getDigest();
                    Log.d("STAMP", "MERKLE: " + IOUtil.bytesToHex(hash));
                    //private static Timestamp create(Timestamp timestamp, List<String> calendarUrls, Integer m, HashMap<String,String> privateCalendarUrls) {
                    byte[] ots = OpenTimestamps.stamp(merkleTip, null, 0, null);
                    Log.d("STAMP", "OTS: " + IOUtil.bytesToHex(ots));
                    // Stamp proof info
                    String info = OpenTimestamps.info(ots);
                    Log.d("STAMP", "INFO: " + info);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                // Store the timestamp
                try {
                    for (DetachedTimestampFile file : fileTimestamps) {
                        timestampDBHelper.addTimestamp(file.getTimestamp());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                mProgressBar.setVisibility(View.GONE);
                load(uri);

            }
        }.execute();
    }

    public void refresh(Uri uri, Hash hash, Timestamp timestamp, Long date){
        mDataset.put(getString(R.string.name),uri.getLastPathSegment());
        mDataset.put(getString(R.string.uri),uri.toString());
        mDataset.put(getString(R.string.type),getMimeType(uri.toString()));
        mDataset.put(getString(R.string.hash), IOUtil.bytesToHex(hash.getValue()));
        if(timestamp == null){
            mDataset.put(getString(R.string.ots_proof), getString(R.string.file_not_timestamped));
        } else {
            mDataset.put(getString(R.string.ots_proof), IOUtil.bytesToHex(ots));

            if (date == null || date == 0) {
                mDataset.put(getString(R.string.attestation), getString(R.string.pending_or_bad_attestation));
            } else {
                try {
                    //Thu May 28 2015 17:41:18 GMT+0200 (CEST)
                    DateFormat sdf = new SimpleDateFormat(getString(R.string.date_format));
                    Date netDate = new Date(date * 1000);
                    mDataset.put(getString(R.string.attestation), getString(R.string.bitcoin_attests) + " " + sdf.format(netDate));
                } catch (Exception ex) {
                    mDataset.put(getString(R.string.attestation), getString(R.string.invalid_date));
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    public byte[] getOts(Timestamp timestamp){
        if(timestamp == null){
            return null;
        }
        DetachedTimestampFile detachedTimestampFile = new DetachedTimestampFile(new OpSHA256(),timestamp);
        StreamSerializationContext ctx = new StreamSerializationContext();
        detachedTimestampFile.serialize(ctx);
        return ctx.getOutput();
    }

    public void onDownloadClick() {
        new AlertDialog.Builder(FileActivity.this)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.file_download_alertdialog))
                .setPositiveButton(R.string.saving, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onSavingClick();
                    }
                })
                .setNegativeButton(R.string.sharing, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onSharingClick();
                    }
                })
                .show();
    }

    public void onSavingClick() {
        StreamDeserializationContext ctx = new StreamDeserializationContext(ots);
        DetachedTimestampFile detachedTimestampFile = DetachedTimestampFile.deserialize(ctx);
        String filename = Utils.bytesToHex(detachedTimestampFile.fileDigest())+".ots";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filepath = dir.getAbsolutePath()+"/"+filename;

        try {
            Ots.write(detachedTimestampFile,filepath);
            Toast.makeText(this,getString(R.string.file_proof_saving)+" "+filepath,Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.file_proof_saving_error),Toast.LENGTH_LONG).show();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(this,getString(R.string.file_proof_saving_error),Toast.LENGTH_LONG).show();
        }
    }

    public void onSharingClick() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, ots);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_proof_to)));
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
