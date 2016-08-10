package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.FilePart;
import com.soundcloud.android.api.StringPart;
import com.soundcloud.android.api.legacy.Params;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.java.strings.Strings;

import android.os.Bundle;

import java.io.File;
import java.io.IOException;

public class AddUserInfoTask extends AuthTask {

    private final ApiClient apiClient;
    private final String username;
    private final String permalink;
    private final File avatarFile;
    private final AccountOperations accountOperations;

    public AddUserInfoTask(SoundCloudApplication app,
                           String permalink,
                           String username,
                           File avatarFile,
                           StoreUsersCommand storeUsersCommand,
                           ApiClient apiClient,
                           AccountOperations accountOperations) {
        super(app, storeUsersCommand);
        this.apiClient = apiClient;
        this.username = username;
        this.permalink = permalink;
        this.avatarFile = avatarFile;
        this.accountOperations = accountOperations;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        try {
            ApiRequest.Builder request = ApiRequest.put(ApiEndpoints.CURRENT_USER.path())
                                                   .forPublicApi();

            if (Strings.isNotBlank(username)) {
                request.withFormPart(StringPart.from(Params.User.NAME, username))
                       .withFormPart(StringPart.from(Params.User.PERMALINK, permalink));
            }

            // resize and attach file if present
            if (avatarFile != null && avatarFile.canWrite()) {
                request.withFormPart(FilePart.from(Params.User.AVATAR, avatarFile, FilePart.BLOB_MEDIA_TYPE));
            }
            ApiUser updatedUser = apiClient.fetchMappedResponse(request.build(), PublicApiUser.class).toApiMobileUser();
            addAccount(updatedUser, accountOperations.getSoundCloudToken(), SignupVia.API);
            return AuthTaskResult.success(updatedUser, SignupVia.API, false);
        } catch (ApiRequestException e) {
            return AuthTaskResult.failure(e);
        } catch (IOException | ApiMapperException e) {
            return AuthTaskResult.failure(e);
        }
    }
}