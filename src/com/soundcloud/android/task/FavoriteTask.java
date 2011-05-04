
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.AsyncTask;

import java.io.IOException;

public class FavoriteTask extends AsyncTask<Track, String, Boolean> {

    SoundCloudApplication mScApp;
    FavoriteListener mFavoriteListener;
    Exception mException;
    Long trackId = (long) -1;

    public FavoriteTask(SoundCloudApplication scApp) {
        this.mScApp = scApp;
    }

    public void setOnFavoriteListener(FavoriteListener favoriteListener){
        mFavoriteListener = favoriteListener;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(String... updates) {
    }

    @Override
    protected Boolean doInBackground(Track... params) {
        trackId = params[0].id;
        try {
            return processResponse(executeResponse(params[0]));
        } catch (IOException e) {
            setException(e);
        }
        return processResponse(0);
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
        if (mFavoriteListener != null){
            mFavoriteListener.onNewFavoriteStatus(trackId, favorite);
        }

        if (mException != null && mFavoriteListener != null){
                mFavoriteListener.onException(trackId,mException);
        }

    }

    // Define our custom Listener interface
    public interface FavoriteListener {
        public abstract void onNewFavoriteStatus(long trackId, boolean isFavorite);
        public abstract void onException(long trackId, Exception e);
    }

}
