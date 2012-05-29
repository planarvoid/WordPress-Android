
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.AsyncTask;

import java.io.IOException;

public class FavoriteTask extends AsyncTask<Track, String, Boolean> {
    protected  SoundCloudApplication mScApp;
    private FavoriteListener mFavoriteListener;
    private Exception mException;
    private long trackId = -1;

    public FavoriteTask(SoundCloudApplication scApp) {
        this.mScApp = scApp;
    }

    public void setOnFavoriteListener(FavoriteListener favoriteListener){
        mFavoriteListener = favoriteListener;
    }

    @Override
    protected Boolean doInBackground(Track... params) {
        trackId = params[0].id;
        try {
            return processResponse(executeResponse(params[0]));
        } catch (IOException e) {
            setException(e);
            return processResponse(0);
        }
    }

    protected int executeResponse(Track t) throws IOException{
        return mScApp.put(Request.to(Endpoints.MY_FAVORITE, t.id))
                .getStatusLine().getStatusCode();
    }

    private void setException(Exception e){
        mException = e;
    }

    protected boolean processResponse(int i){
        return false;
    }

    @Override
    protected void onPostExecute(Boolean favorite) {
        if (mFavoriteListener != null) {
            mFavoriteListener.onNewFavoriteStatus(trackId, favorite);
        }

        if (mException != null && mFavoriteListener != null){
            mFavoriteListener.onException(trackId,mException);
        }
    }

    public interface FavoriteListener {
        void onNewFavoriteStatus(long trackId, boolean isFavorite);
        void onException(long trackId, Exception e);
    }
}
