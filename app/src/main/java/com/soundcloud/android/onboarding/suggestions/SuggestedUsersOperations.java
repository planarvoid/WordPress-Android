package com.soundcloud.android.onboarding.suggestions;


import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudRxHttpClient;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;


public class SuggestedUsersOperations extends ScheduledOperations {

    private static final Func1<Throwable, CategoryGroup> EMPTY_FACEBOOK_GROUP = new Func1<Throwable, CategoryGroup>() {
        @Override
        public CategoryGroup call(Throwable e) {
            return CategoryGroup.createErrorGroup(CategoryGroup.KEY_FACEBOOK);
        }
    };

    private final RxHttpClient rxHttpClient;

    public SuggestedUsersOperations() {
        this(new SoundCloudRxHttpClient());
    }

    @VisibleForTesting
    protected SuggestedUsersOperations(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    public Observable<CategoryGroup> getMusicAndSoundsSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new CategoryGroupListToken())
                .build();
        return schedule(rxHttpClient.<CategoryGroup>fetchModels(request));
    }

    public Observable<CategoryGroup> getFacebookSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_FACEBOOK_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new CategoryGroupListToken())
                .build();
        return schedule(rxHttpClient.<CategoryGroup>fetchModels(request).onErrorReturn(EMPTY_FACEBOOK_GROUP));
    }

    public Observable<CategoryGroup> getCategoryGroups() {
        return schedule(Observable.merge(getMusicAndSoundsSuggestions(), getFacebookSuggestions()));
    }

    private static class CategoryGroupListToken extends TypeToken<List<CategoryGroup>> {
        //Needed because of a reflection issue on 2.2 devices, Exception is raised when logging happens in RxHttpClient
        //http://stackoverflow.com/questions/8041142/reflection-not-fully-implemented-in-android-2-2
        //This will prevent it from calling toString in TypeToken
        @Override
        public String toString() {
            return "List<CategoryGroup>";
        }
    }
}
