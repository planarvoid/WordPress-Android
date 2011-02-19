
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.objects.Track;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

public class CommitTracksTask extends AsyncTask<Track, Parcelable, Boolean> {

    private ContentResolver contentResolver;

    private long currentUserId;


    protected Track[] tracks;

    public CommitTracksTask(ContentResolver contentResolver, Long userId) {
        this.contentResolver = contentResolver;

        if (userId != null)
            this.currentUserId = userId;
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

        for (int i = 0; i < params.length; i++) {
                SoundCloudDB.getInstance().resolveTrack(contentResolver, params[i],
                        WriteState.all, currentUserId);
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
