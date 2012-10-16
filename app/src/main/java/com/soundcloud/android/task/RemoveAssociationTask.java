
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;

import java.io.IOException;

public class RemoveAssociationTask extends AssociatedTrackTask {
    public RemoveAssociationTask(SoundCloudApplication scApp, Track track) {
        super(scApp, track);
    }

    @Override
    protected int executeResponse(Request request) throws IOException {
        return mScApp.delete(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean processResponse(int responseCode) {
        return (responseCode == 0) || !(responseCode == 200 || responseCode == 404);
    }

}
