
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;

import java.io.IOException;

public class FavoriteAddTask extends FavoriteTask {

    public FavoriteAddTask(SoundCloudApplication scApp) {
        super(scApp);
    }
    
    @Override
    protected int executeResponse(Track t) throws IOException{
        return mScApp.put(Request.to(Endpoints.MY_FAVORITE, t.id)).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean processResponse(int responseCode){
        Log.i("AddFavorite","process response " + responseCode);
        return (responseCode == 200 || responseCode == 201);
    }
    
}
