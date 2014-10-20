package com.soundcloud.android.accounts;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.UserStorage;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class UserOperations extends ScheduledOperations {

    private final RxHttpClient httpClient;
    private final UserStorage userStorage;

    @Inject
    public UserOperations(RxHttpClient httpClient, UserStorage userStorage) {
        this.httpClient = httpClient;
        this.userStorage = userStorage;
    }


    public Observable<PublicApiUser> refreshCurrentUser() {
        final ApiRequest<PublicApiUser> request = ApiRequest.Builder.<PublicApiUser>get(ApiEndpoints.CURRENT_USER.path())
                .forPublicApi()
                .forResource(PublicApiUser.class)
                .build();
        return httpClient.<PublicApiUser>fetchModels(request).mergeMap(new Func1<PublicApiUser, Observable<PublicApiUser>>() {
            @Override
            public Observable<PublicApiUser> call(PublicApiUser user) {
                return userStorage.createOrUpdateAsync(user);
            }
        });
    }

}
