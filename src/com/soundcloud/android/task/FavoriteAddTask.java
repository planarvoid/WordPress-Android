
package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class FavoriteAddTask extends FavoriteTask {

    public FavoriteAddTask(SoundCloudApplication scApp) {
        super(scApp);
    }
    
    @Override
    protected InputStream executeResponse(Track t) throws IOException{
        return mScApp
        .putContent(
                CloudAPI.Enddpoints.MY_FAVORITES + "/"
                        + t.id);
    }

    @Override
    protected Boolean processResponse(String response){
        Log.i("AddFavorite","process response " + response);
        boolean favorite = false;
        if (response != null) {
            if (response.contains("200 - OK")
                    || response.contains("201 - Created")) {
                favorite = true;
            } else {
                favorite = false;
            }
        }
        return favorite;
    }
    
}
