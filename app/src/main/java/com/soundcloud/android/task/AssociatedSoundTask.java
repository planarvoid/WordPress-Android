
package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Playable;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;

import java.io.IOException;

public abstract class AssociatedSoundTask extends AsyncTask<String, String, Boolean> {
    protected AndroidCloudAPI mApi;
    private AssociatedListener mAssociatedListener;

    protected Playable playable;

    public AssociatedSoundTask(AndroidCloudAPI api, Playable playable) {
        this.mApi = api;
        this.playable = playable;
    }

    public void setOnAssociatedListener(AssociatedListener likeListener){
        mAssociatedListener = likeListener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            return isAssociated(executeResponse(Request.to(params[0], playable.id)));
        } catch (IOException e) {
            return isAssociated(HttpStatus.SC_NOT_MODIFIED);
        }
    }

    protected abstract int executeResponse(Request request) throws IOException;

    protected boolean isAssociated(int i) {
        return false;
    }

    @Override
    protected void onPostExecute(Boolean associated) {
        if (mAssociatedListener != null) {
            mAssociatedListener.onNewStatus(playable, associated);
        }
    }

    public interface AssociatedListener {
        void onNewStatus(Playable playable, boolean isAssociated);
    }
}
