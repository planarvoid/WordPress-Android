
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.AsyncTask;

import java.io.IOException;

public abstract class AssociatedTrackTask extends AsyncTask<String, String, Boolean> {
    protected  SoundCloudApplication mScApp;
    private AssociatedListener mAssociatedListener;
    private Exception mException;

    protected Track track;

    public AssociatedTrackTask(SoundCloudApplication scApp, Track track) {
        this.mScApp = scApp;
        this.track = track;
    }

    public void setOnAssociatedListener(AssociatedListener favoriteListener){
        mAssociatedListener = favoriteListener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            return processResponse(executeResponse(Request.to(params[0], track.id)));
        } catch (IOException e) {
            setException(e);
            return processResponse(0);
        }
    }

    protected abstract int executeResponse(Request request) throws IOException;

    private void setException(Exception e){
        mException = e;
    }

    protected boolean processResponse(int i){
        return false;
    }

    @Override
    protected void onPostExecute(Boolean favorite) {
        if (mAssociatedListener != null) {
            mAssociatedListener.onNewStatus(track, favorite);
        }

        if (mException != null && mAssociatedListener != null){
            mAssociatedListener.onException(track,mException);
        }
    }

    public interface AssociatedListener {
        void onNewStatus(Track track, boolean isAssociated);
        void onException(Track track, Exception e);
    }
}
