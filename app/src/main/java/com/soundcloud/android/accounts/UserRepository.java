package com.soundcloud.android.accounts;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.UserStorage;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Named;

public class UserRepository {

    private final ApiClientRx apiClientRx;
    private final UserStorage userStorage;
    private final Scheduler scheduler;
    private final Action1<? super PublicApiUser> cacheUser = new Action1<PublicApiUser>() {
        @Override
        public void call(PublicApiUser publicApiUser) {
            userStorage.createOrUpdate(publicApiUser);
        }
    };

    @Inject
    public UserRepository(ApiClientRx apiClientRx, UserStorage userStorage, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.userStorage = userStorage;
        this.scheduler = scheduler;
    }


    public Observable<PublicApiUser> refreshCurrentUser() {
        final ApiRequest request = ApiRequest.Builder.<PublicApiUser>get(ApiEndpoints.CURRENT_USER.path())
                .forPublicApi()
                .build();
        return apiClientRx.mappedResponse(request, PublicApiUser.class).subscribeOn(scheduler).doOnNext(cacheUser);
    }

}
