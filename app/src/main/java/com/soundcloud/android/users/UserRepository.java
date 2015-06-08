package com.soundcloud.android.users;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class UserRepository {

    private final ApiClientRx apiClientRx;
    private final LegacyUserStorage legacyUserStorage;
    private final UserStorage userStorage;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;

    private final Action1<? super PublicApiUser> cacheUser = new Action1<PublicApiUser>() {
        @Override
        public void call(PublicApiUser publicApiUser) {
            legacyUserStorage.createOrUpdate(publicApiUser);
        }
    };

    private static final Func1<PropertySet, Boolean> IS_NOT_EMPTY = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet user) {
            return !user.isEmpty();
        }
    };

    @Inject
    public UserRepository(ApiClientRx apiClientRx, LegacyUserStorage legacyUserStorage, UserStorage userStorage, SyncInitiator syncInitiator,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.legacyUserStorage = legacyUserStorage;
        this.userStorage = userStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public Observable<PublicApiUser> refreshCurrentUser() {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.CURRENT_USER.path())
                .forPublicApi()
                .build();
        return apiClientRx.mappedResponse(request, PublicApiUser.class).subscribeOn(scheduler).doOnNext(cacheUser);
    }

    public Observable<PropertySet> userInfoWithUpdate(Urn urn) {
        return Observable.concat(
                userInfoFromStorage(urn),
                syncInitiator.syncUser(urn).flatMap(continueWith(userInfoFromStorage(urn)))
        );
    }

    private Observable<PropertySet> userInfoFromStorage(Urn urn) {
        return userStorage.loadUser(urn).filter(IS_NOT_EMPTY);
    }

}
