package com.soundcloud.android.api;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.api.ApiWrapper;

import android.content.Context;

import javax.inject.Inject;
import java.util.Locale;

class ApiWrapperFactory {
    private final Context context;
    private final HttpProperties httpProperties;
    private final AccountOperations accountOperations;
    private final ApplicationProperties applicationProperties;

    @Deprecated
    public ApiWrapperFactory(Context context) {
        this(context, new HttpProperties(context.getResources()),
                SoundCloudApplication.fromContext(context).getAccountOperations(),
                new ApplicationProperties(context.getResources()));
    }

    @Inject
    ApiWrapperFactory(Context context, HttpProperties httpProperties, AccountOperations accountOperations,
                             ApplicationProperties applicationProperties) {
        this.context = context;
        this.httpProperties = httpProperties;
        this.accountOperations = accountOperations;
        this.applicationProperties = applicationProperties;
    }

    public ApiWrapper createWrapper(ApiRequest apiRequest) {
        PublicApiWrapper publicApiWrapper = new PublicApiWrapper(context, httpProperties, accountOperations, applicationProperties);
        String acceptContentType = apiRequest.isPrivate()
                ? String.format(Locale.US, ApiClient.PRIVATE_API_ACCEPT_CONTENT_TYPE, apiRequest.getVersion())
                : ApiClient.PUBLIC_API_ACCEPT_CONTENT_TYPE;
        publicApiWrapper.setDefaultContentType(acceptContentType);
        return publicApiWrapper;
    }
}
