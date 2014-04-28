package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.storage.UserStorage;
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

    private PublicCloudAPI oldCloudAPI;

    private String username;
    private File avatarFile;

    protected AddUserInfoTask(SoundCloudApplication app, String username, File avatarFile, UserStorage userStorage,
                              PublicCloudAPI oldCloudAPI) {
        super(app, userStorage);
        this.oldCloudAPI = oldCloudAPI;
        this.username = username;
        this.avatarFile = avatarFile;
    }

    public AddUserInfoTask(SoundCloudApplication application, String username, File avatarFile) {
        this(application, username, avatarFile, new UserStorage(), new PublicApi(application));
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        try {
            Request updateMe = Request.to(Endpoints.MY_DETAILS).with(
                    Params.User.NAME, username,
                    Params.User.PERMALINK, username);

            // resize and attach file if present
            if (avatarFile != null && avatarFile.canWrite()) {
                updateMe.withFile(Params.User.AVATAR, avatarFile);
            }
            SoundCloudApplication app = getSoundCloudApplication();
            HttpResponse resp = oldCloudAPI.put(updateMe);
            switch (resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    User u = oldCloudAPI.getMapper().readValue(resp.getEntity().getContent(), User.class);
                    addAccount(u, oldCloudAPI.getToken(), SignupVia.API);
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
