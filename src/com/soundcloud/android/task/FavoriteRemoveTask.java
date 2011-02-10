
package com.soundcloud.android.task;

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
    protected InputStream executeResponse(Track t) throws IOException{
        return mScApp
        .deleteContent(
                SoundCloudApplication.PATH_MY_FAVORITES + "/"
                        + t.id);
    }
    
    @Override
    protected Boolean processResponse(String response){
        Log.i("RemoveFavorite","process response " + response);
        boolean favorite = true;
        if (response != null) {
            if (response.contains("200 - OK")
                    || response.contains("201 - Created")
                    || response.contains("404 - Not Found")) {
                favorite = false;
            } else {
                favorite = true;
            }
        }
        return favorite;
    }

}
