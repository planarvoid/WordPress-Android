
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;

import java.io.IOException;

public class FavoriteRemoveTask extends FavoriteTask {

    public FavoriteRemoveTask(SoundCloudApplication scApp) {
        super(scApp);
    }
    
    @Override
    protected int executeResponse(Track t) throws IOException{
        return mScApp.delete(Request.to(Endpoints.MY_FAVORITES, t.id))
                .getStatusLine().getStatusCode();
    }
    
    @Override
    protected boolean processResponse(int responseCode){
        Log.i("RemoveFavorite","process response " + responseCode);
        boolean favorite = true;
        if (responseCode != 0) {
            favorite = !(responseCode == 200 || responseCode == 404);
        }
        return favorite;
    }

}
