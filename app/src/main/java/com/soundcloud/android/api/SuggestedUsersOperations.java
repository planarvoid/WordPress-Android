package com.soundcloud.android.api;


import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import rx.Observable;

import java.util.List;


public class SuggestedUsersOperations extends ScheduledOperations {

    private RxHttpClient mRxHttpClient;

    public SuggestedUsersOperations() {
        this(new SoundCloudRxHttpClient());
    }

    @VisibleForTesting
    protected SuggestedUsersOperations(RxHttpClient rxHttpClient) {
        this.mRxHttpClient = rxHttpClient;
    }

    public Observable<CategoryGroup> getMusicAndSoundsSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_CATEGORIES.path())
                .forVersion(1)
                .forPrivateAPI()
                .forResource(new TypeToken<List<CategoryGroup>>() {})
                .build();
        return mRxHttpClient.<CategoryGroup>executeAPIRequest(request);
    }

    public Observable<CategoryGroup> getFacebookSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_FACEBOOK_CATEGORIES.path())
                .forVersion(1)
                .forPrivateAPI()
                .forResource(new TypeToken<List<CategoryGroup>>() {})
                .build();
        return mRxHttpClient.<CategoryGroup>executeAPIRequest(request);
    }

    public Observable<CategoryGroup> getCategoryGroups() {
        return Observable.merge(getMusicAndSoundsSuggestions(), getFacebookSuggestions());
    }
}
