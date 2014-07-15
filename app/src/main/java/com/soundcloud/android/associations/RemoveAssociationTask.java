
package com.soundcloud.android.associations;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.api.Request;

import java.io.IOException;

public class RemoveAssociationTask extends AssociatedSoundTask {
    public RemoveAssociationTask(PublicCloudAPI api, Playable playable) {
        super(api, playable);
    }

    @Override
    protected int executeResponse(Request request) throws IOException {
        return api.delete(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean isAssociated(int responseCode) {
        changed = responseCode >= 200 && responseCode < 300;
        return !(changed || responseCode == 404);
    }
}
