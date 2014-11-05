package com.soundcloud.android.onboarding.suggestions;


import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;


public class SuggestedUsersOperations extends ScheduledOperations {

    private static final Func1<Throwable, CategoryGroup> EMPTY_FACEBOOK_GROUP = new Func1<Throwable, CategoryGroup>() {
        @Override
        public CategoryGroup call(Throwable e) {
            return CategoryGroup.createErrorGroup(CategoryGroup.KEY_FACEBOOK);
        }
    };

    private final RxHttpClient rxHttpClient;

    @Inject
    public SuggestedUsersOperations(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    public Observable<CategoryGroup> getMusicAndSoundsSuggestions() {
        ApiRequest<List<CategoryGroup>> request = ApiRequest.Builder.<List<CategoryGroup>>get(ApiEndpoints.SUGGESTED_USER_CATEGORIES.path())
                .forPrivateApi(1)
                .forResource(new CategoryGroupListToken())
                .build();
        return schedule(rxHttpClient.<CategoryGroup>fetchModels(request));
    }

    public Observable<CategoryGroup> getFacebookSuggestions() {
        ApiRequest<List<CategoryGroup>> request = ApiRequest.Builder.<List<CategoryGroup>>get(ApiEndpoints.SUGGESTED_USER_FACEBOOK_CATEGORIES.path())
                .forPrivateApi(1)
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
