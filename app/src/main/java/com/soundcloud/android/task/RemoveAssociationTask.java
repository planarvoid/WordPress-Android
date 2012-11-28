
package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Sound;
import com.soundcloud.api.Request;

import java.io.IOException;

public class RemoveAssociationTask extends AssociatedSoundTask {
    public RemoveAssociationTask(AndroidCloudAPI api, Sound sound) {
        super(api, sound);
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
