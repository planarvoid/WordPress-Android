package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadCommentsTask extends LoadJsonTask<Comment> {
    private WeakReference<ScPlayer> mPlayerRef;

    private SoundCloudApplication mApp;

    private long mTrackId;

    public LoadCommentsTask(SoundCloudApplication app, long trackId) {
        super(app);
        mApp = app;
        mTrackId = trackId;
    }

    public void setPlayer(ScPlayer player) {
        mPlayerRef = new WeakReference<ScPlayer>(player);
    }

    @Override
    protected List<Comment> doInBackground(Request... path) {
        return list(Request.to(Endpoints.TRACK_COMMENTS, mTrackId), Comment.class);
    }

    @Override
    protected void onPostExecute(List<Comment> comments) {
        if (comments != null) {
            if (mApp.getTrackFromCache(mTrackId) != null) {
                mApp.getTrackFromCache(mTrackId).comments = comments;
                mApp.getTrackFromCache(mTrackId).comments_loaded = true;
                if (mPlayerRef != null && mPlayerRef.get() != null)
                    mPlayerRef.get().onCommentsLoaded(mTrackId, comments);
            }
        }
    }
}
