package com.eternitywall.opentimestamps.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.eternitywall.opentimestamps.IOUtil;
import com.eternitywall.opentimestamps.R;
import com.eternitywall.opentimestamps.adapters.FolderAdapter;
import com.eternitywall.opentimestamps.adapters.ItemAdapter;
import com.eternitywall.opentimestamps.dbs.DBHelper;
import com.eternitywall.opentimestamps.dbs.FolderDBHelper;
import com.eternitywall.opentimestamps.dbs.TimestampDBHelper;
import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.Hash;
import com.eternitywall.ots.OpenTimestamps;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.Utils;
import com.eternitywall.ots.op.OpSHA256;
import com.squareup.okhttp.internal.Util;
import com.sromku.simple.storage.SimpleStorage;
import com.sromku.simple.storage.Storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity implements FolderAdapter.OnItemClickListener {

    final int PERMISSION_EXTERNAL_STORAGE=100;
    Storage storage;
    FolderDBHelper dbHelper;
    TimestampDBHelper timestampDBHelper;

    private RecyclerView mRecyclerView;
    private FolderAdapter mAdapter;
    private List<Folder> mFolders;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // disable update animation
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(this, R.drawable.divider_grey);
        horizontalDecoration.setDrawable(horizontalDivider);
        mRecyclerView.addItemDecoration(horizontalDecoration);

        // check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }
        else {
            init();
        }
    }


    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_EXTERNAL_STORAGE);

        } else {
            init();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSION_EXTERNAL_STORAGE: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)     {
                    init();
                } else {

                    checkPermission();
                }
                return;
            }
        }
    }

    private void clearDB(){
        if (dbHelper == null) {
            dbHelper = new FolderDBHelper(this);
        }
        if (timestampDBHelper == null){
            timestampDBHelper = new TimestampDBHelper(this);
        }
        dbHelper.clearAll();
        timestampDBHelper.clearAll();

        mFolders.clear();
        initDB();
        mAdapter.notifyDataSetChanged();


    }
    private void initDB(){
        // FOLDER ROOT
        {
            Folder folder = new Folder();
            folder.name = "External Storage";
            folder.roodDir = ".";
            folder.id = dbHelper.create(folder);
            mFolders.add(folder);
        }

        {
            Folder folder = new Folder();
            folder.name = "Pictures";
            folder.roodDir = Environment.DIRECTORY_PICTURES;
            folder.id = dbHelper.create(folder);
            folder.getNestedFiles(storage);
            mFolders.add(folder);
        }

        {
            Folder folder = new Folder();
            folder.name = "Documents";
            folder.roodDir = Environment.DIRECTORY_DOCUMENTS;
            folder.id = dbHelper.create(folder);
            folder.getNestedFiles(storage);
            mFolders.add(folder);
        }
        {
            Folder folder = new Folder();
            folder.name = "Camera";
            folder.roodDir = Environment.DIRECTORY_DCIM;
            folder.id = dbHelper.create(folder);
            folder.getNestedFiles(storage);
            mFolders.add(folder);
        }
        {
            Folder folder = new Folder();
            folder.name = "Downloads";
            folder.roodDir = Environment.DIRECTORY_DOWNLOADS;
            folder.id = dbHelper.create(folder);
            folder.getNestedFiles(storage);
            mFolders.add(folder);
        }

    }

    private void init(){
        // Boot time procedure
        if (SimpleStorage.isExternalStorageWritable()) {
            storage = SimpleStorage.getExternalStorage();
        }
        else {
            storage = SimpleStorage.getInternalStorage(this);
        }

        // Check DB
        dbHelper = new FolderDBHelper(this);
        mFolders = dbHelper.getAll();
        if (mFolders.size()==0){
            initDB();
        }
        timestampDBHelper = new TimestampDBHelper(this);

        // Specify and fill adapter from db
        mAdapter = new FolderAdapter(this, mFolders);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

        // checking folders
        checking();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

        switch (item.getItemId()) {
            case R.id.action_check:
                checking();
                return true;
            case R.id.action_clear:
                alert.setTitle(R.string.warning)
                        .setMessage(R.string.are_you_sure_to_reset)
                        .setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearDB();
                                checking();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
                return true;
            case R.id.action_export:

                alert.setTitle(R.string.warning)
                        .setMessage(R.string.are_you_sure_to_export_all_your_proof_files)
                        .setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                exporting();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDetailClick(View view, int position, long id) {
        /*if (mFolders.get(position).ots.length == 0)
            return;
        String ots = IOUtil.bytesToHex(mFolders.get(position).ots);
        String url = "https://opentimestamps.org/info.html?ots=";
        url += ots;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);*/
        onCheckingClick(view,position,id);
    }

    @Override
    public void onCheckingClick(View view, int position, long id) {
        Folder folder = mFolders.get(position);
        hashing(folder);
    }

    @Override
    public void onEnableClick(View view, int position, long id) {
        Folder folder = mFolders.get(position);
        folder.enabled = true;
        dbHelper.update(folder);
        //checking(folder);
    }

    @Override
    public void onDisableClick(View view, int position, long id) {
        mFolders.get(position).enabled = false;
        dbHelper.update(mFolders.get(position));
    }

    // checking files in all folders
    private void checking(){
        for (Folder folder : mFolders){
            checking(folder);
        }
    }

    // checking files in a single folders
    private void checking(final Folder folder){
        if (!folder.isReady()){
            return;
        }
        new AsyncTask<Void,Void,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                List<File> files = folder.getNestedNotSynchedFiles(storage);
                if(files.size()>0)
                    return false;
                else
                    return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                folder.state = Folder.State.CHECKING;
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }

            @Override
            protected void onPostExecute(Boolean isAllStamped) {
                super.onPostExecute(isAllStamped);

                if (folder.lastSync == 0 ){
                    folder.state = Folder.State.NOTHING;
                } else if (isAllStamped) {
                    folder.state = Folder.State.STAMPED;
                } else {
                    folder.state = Folder.State.NOTUPDATED;
                }

                try {
                    mAdapter.notifyItemChanged(mFolders.indexOf(folder));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }

        }.execute();
    }


    // Generate hashes of all files in a single folder
    private void hashing(final Folder folder) {
        if (!folder.isReady()){
            return;
        }
        final List<DetachedTimestampFile> fileTimestamps = new ArrayList<>();

        new AsyncTask<Void,Integer,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                List<File> files = folder.getNestedNotSynchedFiles(storage);
                int countFiles = 0;
                for (File file : files) {
                    countFiles++;
                    publishProgress(countFiles);
                    Log.d("STAMP", "FILE: "+file.getName());

                    try {
                        Hash sha256 = new Hash(IOUtil.readFileSHA256(file));
                        fileTimestamps.add(DetachedTimestampFile.from(new OpSHA256(), sha256));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                folder.state = Folder.State.CHECKING;
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean==false)
                    return;
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
                stamping(folder,fileTimestamps);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                folder.countFiles = values[0];
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }
        }.execute();
    }

    // Stamping pre-hashed files of a single folder
    private void stamping(final Folder folder, final List<DetachedTimestampFile> fileTimestamps){

        new AsyncTask<Void,Integer,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                if(fileTimestamps == null || fileTimestamps.size() == 0){
                    publishProgress(0);
                    return true;
                }

                try {

                    // Stamp the markled list
                    Timestamp merkleTip = OpenTimestamps.makeMerkleTree(fileTimestamps);
                    folder.hash = merkleTip.getDigest();
                    Log.d("STAMP", "MERKLE: " + IOUtil.bytesToHex(folder.hash));
                    //private static Timestamp create(Timestamp timestamp, List<String> calendarUrls, Integer m, HashMap<String,String> privateCalendarUrls) {
                    folder.ots = OpenTimestamps.stamp(merkleTip, null, 0, null);
                    Log.d("STAMP", "OTS: " + IOUtil.bytesToHex(folder.ots));
                    // Stamp proof info
                    String info = OpenTimestamps.info(folder.ots);
                    Log.d("STAMP", "INFO: " + info);
                }catch(Exception e){
                    e.printStackTrace();
                    return false;
                }
                // Save the ots
                int countFiles = 0;
                for (DetachedTimestampFile file : fileTimestamps) {
                    countFiles++;
                    publishProgress(countFiles);
                    timestampDBHelper.addTimestamp(file.getTimestamp());
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                folder.state = Folder.State.STAMPING;
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);

                if (aBoolean==false)
                    return;
                folder.state = Folder.State.STAMPED;
                folder.lastSync = System.currentTimeMillis();
                dbHelper.update(folder);
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                folder.countFiles = values[0];
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }
        }.execute();
    }



    // Exporting all proof-files of all folders in a more zip file
    private void exporting(){
        for (Folder folder : mFolders){
            exporting(folder, folder.zipPath(this));
        }
    }

    // Exporting all proof-files of a single folder in a one zip file
    private void exporting(final Folder folder, final String zipFilePath){
        if (!folder.isReady()){
            return;
        }

        AsyncTask<Void,Integer,Boolean> asyncTask = new AsyncTask<Void,Integer,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {

                // check files
                List<File> files = folder.getNestedFiles(storage);
                if (files.size() == 0){
                    return true;
                }

                ZipOutputStream out = null;
                int countFiles = 0;
                int MAXSIZE= 1024*1024;
                byte[] buffer = new byte[MAXSIZE];

                try {
                    FileOutputStream dest = new FileOutputStream(zipFilePath);
                    out = new ZipOutputStream(new BufferedOutputStream(dest));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }

                try {
                    for (File file : files) {

                        Log.d("STAMP", "FILE: " + file.getName());

                        Hash sha256 = new Hash(IOUtil.readFileSHA256(file));
                        Timestamp stamp = timestampDBHelper.getTimestamp(sha256.getValue());
                        StreamSerializationContext ctx = new StreamSerializationContext();
                        stamp.serialize(ctx);


                        String name = IOUtil.bytesToHex(sha256.getValue()) + ".ots";
                        ZipEntry entry = new ZipEntry(name);
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

                        countFiles++;
                        publishProgress(countFiles);
                    }

                    out.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        out.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return false;
                }

                return true;

            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                folder.state = Folder.State.EXPORTING;
                folder.countFiles = 0;
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                folder.state = Folder.State.EXPORTED;
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));

            }
            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                folder.countFiles = values[0];
                mAdapter.notifyItemChanged(mFolders.indexOf(folder));
            }
        };

        asyncTask.execute();
    }
}
