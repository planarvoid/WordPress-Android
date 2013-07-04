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
import rx.util.functions.Func1;

import java.util.List;


public class SuggestedUsersOperations extends ScheduledOperations {

    private static final Func1<Exception, CategoryGroup> EMPTY_FACEBOOK_GROUP = new Func1<Exception, CategoryGroup>() {
        @Override
        public CategoryGroup call(Exception e) {
            return CategoryGroup.createErrorGroup(CategoryGroup.KEY_FACEBOOK);
        }
    };

    private final RxHttpClient mRxHttpClient;

    public SuggestedUsersOperations() {
        this(new SoundCloudRxHttpClient());
    }

    @VisibleForTesting
    public SuggestedUsersOperations(RxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<CategoryGroup> getMusicAndSoundsSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new TypeToken<List<CategoryGroup>>() {})
                .build();
        return schedule(mRxHttpClient.<CategoryGroup>executeAPIRequest(request));
    }

    public Observable<CategoryGroup> getFacebookSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_FACEBOOK_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new TypeToken<List<CategoryGroup>>() {})
                .build();
        return schedule(mRxHttpClient.<CategoryGroup>executeAPIRequest(request).onErrorReturn(EMPTY_FACEBOOK_GROUP));
    }

    public Observable<CategoryGroup> getCategoryGroups() {
        return schedule(Observable.merge(getMusicAndSoundsSuggestions(), getFacebookSuggestions()));
    }

}
