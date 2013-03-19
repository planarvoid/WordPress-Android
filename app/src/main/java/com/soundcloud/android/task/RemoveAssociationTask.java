
package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Playable;
import com.soundcloud.api.Request;

import java.io.IOException;

public class RemoveAssociationTask extends AssociatedSoundTask {
    public RemoveAssociationTask(AndroidCloudAPI api, Playable playable) {
        super(api, playable);
    }

    @Override
    protected int executeResponse(Request request) throws IOException {
        return mApi.delete(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean isAssociated(int responseCode) {
        mChanged = responseCode >= 200 && responseCode < 300;
        return !(mChanged || responseCode == 404);
    }
}
