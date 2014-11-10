package com.soundcloud.android.accounts;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.UserStorage;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class UserOperations extends ScheduledOperations {

    private final ApiScheduler apiScheduler;
    private final UserStorage userStorage;

    @Inject
    public UserOperations(ApiScheduler apiScheduler, UserStorage userStorage) {
        this.apiScheduler = apiScheduler;
        this.userStorage = userStorage;
    }


    public Observable<PublicApiUser> refreshCurrentUser() {
        final ApiRequest<PublicApiUser> request = ApiRequest.Builder.<PublicApiUser>get(ApiEndpoints.CURRENT_USER.path())
                .forPublicApi()
                .forResource(PublicApiUser.class)
                .build();
        return apiScheduler.mappedResponse(request).flatMap(new Func1<PublicApiUser, Observable<PublicApiUser>>() {
            @Override
            public Observable<PublicApiUser> call(PublicApiUser user) {
                return userStorage.createOrUpdateAsync(user);
            }
        });
    }

}
