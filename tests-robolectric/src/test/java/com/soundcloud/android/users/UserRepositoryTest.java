package com.soundcloud.android.users;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class UserRepositoryTest {

    private UserRepository userRepository;

    private final Urn userUrn = Urn.forUser(123L);
    private final PropertySet user = PropertySet.from(UserProperty.URN.bind(Urn.forUser(123L)));
    private final PropertySet updatedUser = PropertySet.create()
            .put(UserProperty.URN, Urn.forUser(123L))
            .put(UserProperty.USERNAME, "name");


    private TestObserver<PropertySet> observer = new TestObserver<>();

    @Mock private ApiClientRx apiClientRx;
    @Mock private UserStorage userStorage;
    @Mock private SyncInitiator syncInitiator;

    @Before
    public void setup() {
        userRepository = new UserRepository(userStorage, syncInitiator, Schedulers.immediate());
    }

    @Test
    public void localUserInfoReturnsUserInfoFromStorage() {
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(updatedUser));

        userRepository.localUserInfo(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(updatedUser);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void syncedUserInfoReturnsUserInfoFromStorageAfterSync() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_USERS, true)));
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(updatedUser));

        userRepository.syncedUserInfo(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoFromStorage() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.<SyncResult>empty());
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user));

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(user);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoAgainFromStorageAfterSync() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_USERS, true)));
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user), Observable.just(updatedUser));

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(user, updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsDoesNotEmitMissingUser() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_USERS, true)));
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(PropertySet.create()), Observable.just(updatedUser));

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(updatedUser);
    }

}
