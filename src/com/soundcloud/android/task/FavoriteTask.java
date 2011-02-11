
package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;

public class FavoriteTask extends AsyncTask<Track, String, Boolean> {

    SoundCloudApplication mScApp;
    FavoriteListener mFavoriteListener;
    Exception mException;

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
        try {
            return processResponse(CloudUtils.streamToString(executeResponse(params[0])));
        } catch (IOException e) {
            e.printStackTrace();
            setException(e);
        }

        return processResponse(null);
    }
    
    protected InputStream executeResponse(Track t) throws IOException{
        return mScApp
        .putContent(
                CloudAPI.Enddpoints.MY_FAVORITES + "/"
                        + t.id);
    }
    
    
    private void setException(Exception e){
        mException = e;
    }
    
    protected Boolean processResponse(String response){
        return false;
    }
    
    @Override
    protected void onPostExecute(Boolean favorite) {
        if (mFavoriteListener != null)
            mFavoriteListener.onNewFavoriteStatus(favorite);
        
        if (mException != null && mFavoriteListener != null)
                mFavoriteListener.onException(mException);
    }
    
    // Define our custom Listener interface
    public interface FavoriteListener {
        public abstract void onNewFavoriteStatus(boolean isFavorite);
        public abstract void onException(Exception e);
    }

}
