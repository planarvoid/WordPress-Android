
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Track;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.provider.Content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CommitTracksTask extends AsyncTask<Track, Parcelable, Boolean> {

    private ContentResolver contentResolver;
    protected Track[] tracks;

    public CommitTracksTask(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    @Override
    protected void onPreExecute() {
        Log.i(getClass().getName(), "Starting playlist commit");
    }

    @Override
    protected void onProgressUpdate(Parcelable... updates) {
    }

    @Override
    protected Boolean doInBackground(Track... params) {
        List<ContentValues> tracksCV = new ArrayList<ContentValues>();
        List<ContentValues> usersCV = new ArrayList<ContentValues>();
        HashSet<Long> usersSet = new HashSet<Long>();

        for (Track param : params) {
            if (param != null) {
                if (!SoundCloudDB.isTrackInDb(contentResolver, param.id)) {
                    tracksCV.add(param.buildContentValues());
                }
                if (!usersSet.contains(param.user.id)) {
                    usersSet.add(param.user.id);
                    if (!SoundCloudDB.isUserInDb(contentResolver, param.user.id)) {
                        usersCV.add(param.user.buildContentValues(false));
                    }
                }
            }
        }

        // builk insert uses replace (insert or update) instead of insert,
        // so it won't fail from a unique key constraint
        if (tracksCV.size() > 0) {
            contentResolver.bulkInsert(Content.TRACKS.uri,
                    tracksCV.toArray(new ContentValues[tracksCV.size()]));
        }
        if (usersCV.size() > 0) {
            contentResolver.bulkInsert(Content.USERS.uri,
                    usersCV.toArray(new ContentValues[usersCV.size()]));
        }

        afterCommitInBg();
        return true;

    }

    protected void afterCommitInBg() {
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.i(getClass().getName(), "Done playlist commit");
    }

}
