package com.eternitywall.opentimestamps.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.eternitywall.opentimestamps.IOUtil;
import com.eternitywall.opentimestamps.R;
import com.eternitywall.opentimestamps.adapters.FolderAdapter;
import com.eternitywall.opentimestamps.adapters.ItemAdapter;
import com.eternitywall.opentimestamps.models.Folder;
import com.eternitywall.ots.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;

public class FileActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ItemAdapter mAdapter;
    private LinkedHashMap<String,String> mDataset;
    private RecyclerView.LayoutManager mLayoutManager;

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
                }
            }
        }
    }

    void refresh (Uri uri){
        ContentResolver contentResolver = getContentResolver();

        mDataset.put("Name",uri.getLastPathSegment());
        mDataset.put("Uri",uri.toString());
        mDataset.put("Type",contentResolver.getType(uri).toString());

        try {
            // Read file
            InputStream inputStream = contentResolver.openInputStream(uri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count = inputStream.read(buffer);
            outputStream.write(buffer);
            while (count >= 0) {
                count = inputStream.read(buffer);
                outputStream.write(buffer);
            }

            // Calculate Hash
            byte[] hash = IOUtil.SHA256(outputStream.toByteArray());
            mDataset.put("SHA256", Utils.bytesToHex(hash));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        mAdapter.notifyDataSetChanged();
    }

}
