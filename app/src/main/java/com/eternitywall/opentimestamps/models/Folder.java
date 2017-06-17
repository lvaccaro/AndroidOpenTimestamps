package com.eternitywall.opentimestamps.models;

import android.content.Context;

import com.sromku.simple.storage.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luca on 09/06/2017.
 */

public class Folder {
    public long id;
    public String name;
    public String roodDir;
    public boolean enabled = false;
    public State state = State.NOTHING;
    public long lastSync = 0;
    public long countFiles = 0;
    public byte[] ots;
    public byte[] hash;

    public static enum State  {
        NOTHING, CHECKING, STAMPED, STAMPING, NOTUPDATED, EXPORTING, EXPORTED
    };

    public List<File> getNestedFiles(Storage storage){
        List<File> files = new ArrayList<>();
        files = storage.getNestedFiles(roodDir);
        return files;
    }

    public List<File> getNestedNotSynchedFiles(Storage storage) {
        List<File> notSynchedFiles = new ArrayList<>();
        List<File> files = storage.getNestedFiles(roodDir);
        for (File file : files) {
            if (file.lastModified() > lastSync) {
                notSynchedFiles.add(file);
            }
        }
        return notSynchedFiles;
    }

    public String zipPath(Context context){
        return  context.getExternalCacheDir()+"/"+this.name.replace(" ","_")+".zip";
    }
    public boolean isReady() {
        if (this.enabled == false)
            return false;
        if (this.state == Folder.State.STAMPING ||
                this.state == Folder.State.CHECKING ||
                this.state == Folder.State.EXPORTING)
            return false;
        return true;
    }


}
