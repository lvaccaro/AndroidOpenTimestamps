package com.eternitywall.opentimestamps;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.Hash;
import com.eternitywall.ots.Merkle;
import com.eternitywall.ots.OpenTimestamps;
import com.eternitywall.ots.StreamSerializationContext;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.Utils;
import com.eternitywall.ots.op.Op;
import com.eternitywall.ots.op.OpAppend;
import com.eternitywall.ots.op.OpSHA256;
import com.google.common.io.Files;
import com.sromku.simple.storage.SimpleStorage;
import com.sromku.simple.storage.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    final int PERMISSION_EXTERNAL_STORAGE=100;
    final String LAST_TIMESTAMP_STAMPED = "LAST_TIMESTAMP_STAMPED";
    final String LAST_OTS = "LAST_OTS";
    final String COUNT_FILES = "COUNT_FILES";
    Storage storage;

    private RecyclerView mRecyclerView;
    private ItemAdapter mAdapter;
    LinkedHashMap<String,String> myDataset = new LinkedHashMap<String,String>();
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

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(this, R.drawable.divider_grey);
        horizontalDecoration.setDrawable(horizontalDivider);
        mRecyclerView.addItemDecoration(horizontalDecoration);

        // specify an adapter (see also next example)
        mAdapter = new ItemAdapter(myDataset);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

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


    private void init(){

        if (SimpleStorage.isExternalStorageWritable()) {
            storage = SimpleStorage.getExternalStorage();
        }
        else {
            storage = SimpleStorage.getInternalStorage(this);
        }
        refreshUI();
    }

    List<File> files = new ArrayList<>();
    List<DetachedTimestampFile> fileTimestamps = new ArrayList<>();

    private void run(){

        String roodDir=".";
        files = storage.getNestedFiles(roodDir);
        Log.d("LIST",files.toString());

        // Build hash digest list
        List<Hash> hashes = new ArrayList<>();
        for (File file : files ) {
            try {
                byte[] bytes = IOUtil.readFile(file);
                Hash sha256 = new Hash(SHA256(bytes));
                hashes.add(sha256);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Build detached file to timestamp from hash
        fileTimestamps = new ArrayList<>();
        for (Hash hash : hashes ) {
            fileTimestamps.add( DetachedTimestampFile.from(new OpSHA256(),hash));
        }

        // Stamp the markled list
        Timestamp merkleTip = OpenTimestamps.makeMerkleTree(fileTimestamps);
        byte[] ots = OpenTimestamps.stamp(merkleTip,null , 0, null);
        Log.d("OTS",Utils.bytesToHex(ots));

        // Stamp proof info
        String info = OpenTimestamps.info(ots);
        Log.d("OTS",info);

        // Save the ots
        getSharedPreferences(getPackageName().toLowerCase(),MODE_PRIVATE).edit()
                .putLong(LAST_TIMESTAMP_STAMPED,System.currentTimeMillis())
                .putString(LAST_OTS,Utils.bytesToHex(ots))
                .putLong(COUNT_FILES,files.size())
                .commit();

        // Refresh UI
        myDataset.clear();
        for (int i=0;i<files.size();i++){
            StreamSerializationContext css = new StreamSerializationContext();
            fileTimestamps.get(i).serialize(css);
            String hex = Utils.bytesToHex(css.getOutput());
            Log.d(files.get(i).getPath(),hex);
            myDataset.put(files.get(i).getPath(),hex);
        }
        refreshUI();
    }

    void refreshUI(){
        Long millisec=getSharedPreferences(getPackageName().toLowerCase(),MODE_PRIVATE)
                .getLong(LAST_TIMESTAMP_STAMPED,0);
        Long countFiles =getSharedPreferences(getPackageName().toLowerCase(),MODE_PRIVATE)
                .getLong(COUNT_FILES,0);
        String dateString = new SimpleDateFormat("dd, MMM, yyyy").format(new Date(millisec));
        tvStatus.setText("Files: "+String.valueOf(countFiles) +" at "+dateString);
        mAdapter = new ItemAdapter(myDataset);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }




    public static byte[] SHA256 (byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bytes);
        return md.digest();
    }

    @Override
    public void onItemClick(View view, int position, String id) {

        StreamSerializationContext css = new StreamSerializationContext();
        fileTimestamps.get(position).serialize(css);
        String ots = Utils.bytesToHex(css.getOutput());

        String url = "https://opentimestamps.org/info.html?ots=";
        url += ots;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_stamp:
                run();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
