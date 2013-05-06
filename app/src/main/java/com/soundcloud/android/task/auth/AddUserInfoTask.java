package com.soundcloud.android.task.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.Bundle;

import java.io.File;
import java.io.IOException;

public class AddUserInfoTask extends AuthTask {

    private User mUpdatedUser;
    private File mAvatarFile;

    protected AddUserInfoTask(SoundCloudApplication app, User updatedUser, File avatarFile, UserStorage userStorage) {
        super(app, userStorage);
        mUpdatedUser = updatedUser;
        mAvatarFile = avatarFile;
    }

    public AddUserInfoTask(SoundCloudApplication application, User updatedUser, File avatarFile){
        this(application, updatedUser, avatarFile, new UserStorage());
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        try {
            Request updateMe = Request.to(Endpoints.MY_DETAILS).with(
                    Params.User.NAME, mUpdatedUser.username,
                    Params.User.PERMALINK, mUpdatedUser.permalink);

            // resize and attach file if present
            if (mAvatarFile != null && mAvatarFile.canWrite()) {
                updateMe.withFile(Params.User.AVATAR, mAvatarFile);
            }
            SoundCloudApplication app = getSoundCloudApplication();
            HttpResponse resp = app.put(updateMe);
            switch (resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    User u = app.getMapper().readValue(resp.getEntity().getContent(), User.class);
                    addAccount(u, getSoundCloudApplication().getToken(), SignupVia.API);
                    return AuthTaskResult.success(u, SignupVia.API);

                case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                    return AuthTaskResult.failure(extractErrors(resp));

                default:
                    Log.e("unexpected response: " + resp);
                    return AuthTaskResult.failure(app.getString(R.string.authentication_add_info_error));
            }
        } catch (IOException e) {
            Log.e("IOException while adding user details: " + e.getMessage());
            return AuthTaskResult.failure(e);
        }
    }
}
