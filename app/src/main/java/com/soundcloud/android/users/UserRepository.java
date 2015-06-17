package com.soundcloud.android.users;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class UserRepository {

    private final UserStorage userStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    private static final Func1<PropertySet, Boolean> IS_NOT_EMPTY = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet user) {
            return !user.isEmpty();
        }
    };

    @Inject
    public UserRepository(UserStorage userStorage, SyncInitiator syncInitiator, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.userStorage = userStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> syncedUserInfo(Urn userUrn) {
        return syncInitiator.syncUser(userUrn).flatMap(continueWith(localUserInfo(userUrn)));
    }

    public Observable<PropertySet> localAndSyncedUserInfo(Urn userUrn) {
        return Observable.concat(
                localUserInfo(userUrn),
                syncedUserInfo(userUrn)
        );
    }

    public Observable<PropertySet> localUserInfo(Urn urn) {
        return userStorage.loadUser(urn).filter(IS_NOT_EMPTY).subscribeOn(scheduler);
    }

}
