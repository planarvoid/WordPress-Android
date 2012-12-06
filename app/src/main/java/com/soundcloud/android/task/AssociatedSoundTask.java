
package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Sound;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;

import java.io.IOException;

public abstract class AssociatedSoundTask extends AsyncTask<String, String, Boolean> {
    protected AndroidCloudAPI mApi;
    private AssociatedListener mAssociatedListener;

    protected Sound sound;

    public AssociatedSoundTask(AndroidCloudAPI api, Sound sound) {
        this.mApi = api;
        this.sound = sound;
    }

    public void setOnAssociatedListener(AssociatedListener likeListener){
        mAssociatedListener = likeListener;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            return isAssociated(executeResponse(Request.to(params[0], sound.id)));
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
            mAssociatedListener.onNewStatus(sound, associated);
        }
    }

    public interface AssociatedListener {
        void onNewStatus(Sound sound, boolean isAssociated);
    }
}
