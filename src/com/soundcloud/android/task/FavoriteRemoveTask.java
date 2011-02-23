
package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class FavoriteRemoveTask extends FavoriteTask {

    public FavoriteRemoveTask(SoundCloudApplication scApp) {
        super(scApp);
    }
    
    @Override
    protected int executeResponse(Track t) throws IOException{
        return mScApp
        .deleteContent(
                CloudAPI.Enddpoints.MY_FAVORITES + "/"
                        + t.id).getStatusLine().getStatusCode();
    }
    
    @Override
    protected boolean processResponse(int responseCode){
        Log.i("RemoveFavorite","process response " + responseCode);
        boolean favorite = true;
        if (responseCode != 0) {
            if (responseCode == 200 || responseCode == 404) {
                favorite = false;
            } else {
                favorite = true;
            }
        }
        return favorite;
    }

}
