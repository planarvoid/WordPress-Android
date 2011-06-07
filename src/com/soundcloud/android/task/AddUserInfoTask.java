package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;

import android.util.Pair;

import java.io.File;
import java.io.IOException;

public class AddUserInfoTask extends AsyncApiTask<Pair<User,File>, Void, User> {
    public AddUserInfoTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected User doInBackground(final Pair<User, File>... params) {
        final User u = params[0].first;
        final File file = params[0].second;
        try {
            Request updateMe = Request.to(MY_DETAILS).with(
                    Params.User.NAME, u.username,
                    Params.User.PERMALINK, u.permalink);

            // resize and attach file if present
            if (file != null && file.canWrite()) {
                // XXX really overwrite file?
                // ImageUtils.resizeImageFile(file, file, 800, 800);

                updateMe.withFile(Params.User.AVATAR, file);
            }
            HttpResponse resp = mApi.put(updateMe);
            switch (resp.getStatusLine().getStatusCode()) {
                case SC_OK:
                    return mApi.getMapper().readValue(resp.getEntity().getContent(), User.class);
                case SC_UNPROCESSABLE_ENTITY:
                    extractErrors(resp);
                default:
                    warn("unexpected response", resp);
                    return null;
            }
        } catch (IOException e) {
            warn("error updating details", e);
            return null;
        }
    }
}
