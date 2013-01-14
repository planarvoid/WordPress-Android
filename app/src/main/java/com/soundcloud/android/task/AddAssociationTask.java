
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Sound;
import com.soundcloud.api.Request;

import java.io.IOException;

public class AddAssociationTask extends AssociatedSoundTask {
    public AddAssociationTask(SoundCloudApplication scApp, Sound sound) {
        super(scApp, sound);
    }

    @Override
    protected int executeResponse(Request request) throws IOException{
        return mApi.put(request).getStatusLine().getStatusCode();
    }

    @Override
    protected boolean isAssociated(int responseCode){
        mChanged = responseCode == 201;
        return (responseCode == 200 /* if was already associated */ || mChanged /* new association */);
    }
}
