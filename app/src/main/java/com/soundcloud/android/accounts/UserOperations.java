package com.soundcloud.android.accounts;

import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.User;
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


    public Observable<User> refreshCurrentUser() {
        final APIRequest<User> request = RequestBuilder.<User>get(APIEndpoints.CURRENT_USER.path())
                .forPublicAPI()
                .forResource(User.class)
                .build();
        return httpClient.<User>fetchModels(request).mergeMap(new Func1<User, Observable<User>>() {
            @Override
            public Observable<User> call(User user) {
                return userStorage.createOrUpdateAsync(user);
            }
        });
    }

}
