
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.io.IOException;

public class RemoveAssociationTask extends AssociatedTrackTask {
    public RemoveAssociationTask(SoundCloudApplication scApp, Track track) {
        super(scApp, track);
    }

    @Override
    protected int executeResponse(Request request) throws IOException {
        return mApi.delete(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean isAssociated(int responseCode) {
        return !((responseCode >= 200 && responseCode < 300) || responseCode == 404);
    }

}
