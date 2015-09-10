package com.soundcloud.android.facebookapi;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.share.widget.AppInviteDialog;

import android.os.Bundle;

import javax.inject.Inject;

public class FacebookApiHelper {

    private static final String GRAPH_FIELDS_PARAM = "fields";

    @Inject
    public FacebookApiHelper() {
        // dagger
    }

    public boolean canShowAppInviteDialog() {
        return AppInviteDialog.canShow();
    }

    public boolean hasAccessToken() {
        return AccessToken.getCurrentAccessToken() != null;
    }

    public FacebookApiResponse graphRequest(FacebookApiEndpoints apiEndpoint) {
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                apiEndpoint.getPath(),
                null,
                HttpMethod.GET);

        request.setParameters(buildGraphFieldsBundle(apiEndpoint.getFields()));
        return new FacebookApiResponse(request.executeAndWait());
    }

    private Bundle buildGraphFieldsBundle(String fields) {
        Bundle parameters = new Bundle();
        parameters.putString(GRAPH_FIELDS_PARAM, fields);
        return parameters;
    }

}
