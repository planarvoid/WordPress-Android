
package com.soundcloud.android.associations;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.Playable;
import com.soundcloud.api.Request;

import java.io.IOException;

public class AddAssociationTask extends AssociatedSoundTask {
    public AddAssociationTask(PublicCloudAPI api, Playable playable) {
        super(api, playable);
    }

    @Override
    protected int executeResponse(Request request) throws IOException{
        return api.put(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean isAssociated(int responseCode){
        changed = responseCode == 201;
        return (responseCode == 200 /* if was already associated */ || changed /* new association */);
    }
}
