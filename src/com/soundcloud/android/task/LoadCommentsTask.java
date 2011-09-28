package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoadCommentsTask extends LoadJsonTask<Comment> {
    private List<WeakReference<LoadCommentsListener>> mListenerRefs;
    private SoundCloudApplication mApp;

    private long mTrackId;

    public LoadCommentsTask(SoundCloudApplication app, long trackId) {
        super(app);
        mApp = app;
        mTrackId = trackId;
        mListenerRefs = new ArrayList<WeakReference<LoadCommentsListener>>();
    }

    public void addListener(LoadCommentsListener listener) {
        for (WeakReference<LoadCommentsListener> listenerRef : mListenerRefs){
            if (listener != null && listenerRef.get() != null && listenerRef.get() == listener) return;
        }
        mListenerRefs.add(new WeakReference<LoadCommentsListener>(listener));
    }

    @Override
    protected List<Comment> doInBackground(Request... path) {
        return list(Request.to(Endpoints.TRACK_COMMENTS, mTrackId), Comment.class);
    }

    @Override
    protected void onPostExecute(List<Comment> comments) {
        if (comments != null) {
            Track cached =  mApp.getTrackFromCache(mTrackId);

            if (cached != null) {
                cached.comments = comments;
                cached.comments_loaded = true;
            }

            for (WeakReference<LoadCommentsListener> listenerRef : mListenerRefs){
                if (listenerRef != null && listenerRef.get() != null){
                    listenerRef.get().onCommentsLoaded(mTrackId,comments);
                }
            }
        }
    }

     // Define our custom Listener interface
    public interface LoadCommentsListener {
        void onCommentsLoaded(long track_id, List<Comment> comments);
    }
}
