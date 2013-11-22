package com.soundcloud.android.accounts;

import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.UserStorage;
import rx.Observable;
import rx.util.functions.Func1;

import javax.inject.Inject;

public class UserOperations extends ScheduledOperations {

    private final RxHttpClient mHttpClient;
    private final UserStorage mUserStorage;

    @Inject
    public UserOperations(RxHttpClient httpClient, UserStorage userStorage) {
        mHttpClient = httpClient;
        mUserStorage = userStorage;
    }


    public Observable<User> refreshCurrentUser() {
        final APIRequest<User> request = RequestBuilder.<User>get(APIEndpoints.CURRENT_USER.path())
                .forPublicAPI()
                .forResource(User.class)
                .build();
        return mHttpClient.<User>fetchModels(request).mapMany(new Func1<User, Observable<User>>() {
            @Override
            public Observable<User> call(User user) {
                return mUserStorage.createOrUpdateAsync(user);
            }
        });
    }

}
