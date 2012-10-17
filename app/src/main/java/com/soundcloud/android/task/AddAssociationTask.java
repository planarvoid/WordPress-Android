
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Request;

import java.io.IOException;

public class AddAssociationTask extends AssociatedTrackTask {

    public AddAssociationTask(SoundCloudApplication scApp, Track track) {
        super(scApp, track);
    }

    @Override
    protected int executeResponse(Request request) throws IOException{
        return mApi.put(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean processResponse(int responseCode){
        return (responseCode == 200 || responseCode == 201);
    }
    
}
