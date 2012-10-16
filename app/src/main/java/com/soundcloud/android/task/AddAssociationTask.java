
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;

import java.io.IOException;

public class AddAssociationTask extends AssociatedTrackTask {

    public AddAssociationTask(SoundCloudApplication scApp, Track track) {
        super(scApp, track);
    }

    @Override
    protected int executeResponse(Request request) throws IOException{
        return mScApp.put(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean processResponse(int responseCode){
        return (responseCode == 200 || responseCode == 201);
    }
    
}
