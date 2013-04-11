package com.soundcloud.android.task;

import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadCommentsTask extends AsyncApiTask<Long, Void, List<Comment>> {

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
                           .add("limit", Consts.MAX_COMMENTS_TO_LOAD));
    }

    @Override
    protected void onPostExecute(List<Comment> comments) {
        if (comments != null) {
            Track cached =  SoundCloudApplication.MODEL_MANAGER.getTrack(mTrackId);

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

    private List<Comment> list(Request path) {
        try {
            return mApi.readList(path);
        } catch (IOException e) {
            Log.w(TAG, "error fetching JSON", e);
            return null;
        }
    }
}
