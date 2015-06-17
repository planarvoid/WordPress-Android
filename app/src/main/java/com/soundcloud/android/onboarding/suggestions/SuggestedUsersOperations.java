package com.soundcloud.android.onboarding.suggestions;


import com.google.common.reflect.TypeToken;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;


public class SuggestedUsersOperations {

    private static final Func1<Throwable, CategoryGroup> EMPTY_FACEBOOK_GROUP = new Func1<Throwable, CategoryGroup>() {
        @Override
        public CategoryGroup call(Throwable e) {
            return CategoryGroup.createErrorGroup(CategoryGroup.KEY_FACEBOOK);
        }
    };
    private static final Func1<List<CategoryGroup>, Observable<? extends CategoryGroup>> flattenGroupList = new Func1<List<CategoryGroup>, Observable<? extends CategoryGroup>>() {
        @Override
        public Observable<? extends CategoryGroup> call(List<CategoryGroup> groups) {
            return Observable.from(groups);
        }
    };

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;

    @Inject
    public SuggestedUsersOperations(ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
    }

    Observable<CategoryGroup> getMusicAndSoundsSuggestions() {
        ApiRequest request = ApiRequest.get(ApiEndpoints.SUGGESTED_USER_CATEGORIES.path())
                .forPrivateApi(1)
                .build();
        return apiClientRx.mappedResponse(request, new CategoryGroupListToken())
                .subscribeOn(scheduler)
                .flatMap(flattenGroupList);
    }

    Observable<CategoryGroup> getFacebookSuggestions() {
        ApiRequest request = ApiRequest.get(ApiEndpoints.SUGGESTED_USER_FACEBOOK_CATEGORIES.path())
                .forPrivateApi(1)
                .build();
        return apiClientRx.mappedResponse(request, new CategoryGroupListToken())
                .subscribeOn(scheduler)
                .flatMap(flattenGroupList)
                .onErrorReturn(EMPTY_FACEBOOK_GROUP);
    }

    Observable<CategoryGroup> getCategoryGroups() {
        return Observable.merge(getMusicAndSoundsSuggestions(), getFacebookSuggestions());
    }

    static class CategoryGroupListToken extends TypeToken<List<CategoryGroup>> {
        //Needed because of a reflection issue on 2.2 devices, Exception is raised when logging happens in RxHttpClient
        //http://stackoverflow.com/questions/8041142/reflection-not-fully-implemented-in-android-2-2
        //This will prevent it from calling toString in TypeToken
        @Override
        public String toString() {
            return "List<CategoryGroup>";
        }
    }
}
