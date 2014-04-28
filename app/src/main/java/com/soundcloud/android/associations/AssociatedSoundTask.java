
package com.soundcloud.android.associations;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.Playable;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;

import java.io.IOException;

public abstract class AssociatedSoundTask extends AsyncTask<String, String, Boolean> {
    protected PublicCloudAPI api;
    private AssociatedListener associatedListener;
    protected boolean changed;

    protected Playable playable;

    public AssociatedSoundTask(PublicCloudAPI api, Playable playable) {
        this.api = api;
        this.playable = playable;
    }

    public void setOnAssociatedListener(AssociatedListener likeListener){
        associatedListener = likeListener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            return isAssociated(executeResponse(Request.to(params[0], playable.getId())));
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
        if (associatedListener != null) {
            associatedListener.onNewStatus(playable, associated, changed);
        }
    }

    public interface AssociatedListener {
        void onNewStatus(Playable playable, boolean isAssociated, boolean changed);
    }
}
