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
        NOTHING, CHECKING, STAMPED, STAMPING, NOTUPDATED, EXPORTING
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

/*
    public static int stateToInt(State state){
        if (state==State.NOTHING)
            return 0;
        else if (state==State.CHECKING)
            return 1;
        else if (state==State.STAMPED)
            return 2;
        else if (state==State.STAMPING)
            return 3;
        else if (state==State.NOTUPDATED)
            return 4;
        return 0;
    }
    public static State intToState(int state){
        if (state==0)
            return State.NOTHING;
        else if (state==1)
            return State.CHECKING;
        else if (state==2)
            return State.STAMPED;
        else if (state==3)
            return State.STAMPING;
        else if (state==4)
            return State.NOTUPDATED;
        return State.NOTHING;
    }*/

}
