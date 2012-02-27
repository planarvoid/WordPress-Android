package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoadCommentsTask extends LoadJsonTask<Long, Comment> {
    private List<WeakReference<LoadCommentsListener>> mListenerRefs;
    private long mTrackId;

    public LoadCommentsTask(AndroidCloudAPI api) {
        super(api);
        mListenerRefs = new ArrayList<WeakReference<LoadCommentsListener>>();
    }

    public void addListener(LoadCommentsListener listener) {
        for (WeakReference<LoadCommentsListener> listenerRef : mListenerRefs){
            if (listener != null && listenerRef.get() != null && listenerRef.get() == listener) return;
        }
        mListenerRefs.add(new WeakReference<LoadCommentsListener>(listener));
    }

    @Override
    protected List<Comment> doInBackground(Long... params) {
        mTrackId = params[0];
        return list(Request.to(Endpoints.TRACK_COMMENTS, mTrackId)
                           .add("limit", Consts.MAX_COMMENTS_TO_LOAD), Comment.class);
    }

    @Override
    protected void onPostExecute(List<Comment> comments) {
        if (comments != null) {
            Track cached =  SoundCloudApplication.TRACK_CACHE.get(mTrackId);

            if (cached != null) {
                cached.comments = comments;
                for (Comment c : comments) {
                    c.track = cached;
                }
            }

            for (WeakReference<LoadCommentsListener> listenerRef : mListenerRefs) {
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
