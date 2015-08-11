package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class UserRepositoryTest extends AndroidUnitTest {

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

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void userInfoReturnsUserInfoFromStorage() {
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user));
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.<SyncResult>never());

        userRepository.userInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(user);
    }

    @Test
    public void userInfoReturnsUserInfoFromSyncerIfStorageEmpty() {
        final PublishSubject<SyncResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.<PropertySet>empty(), Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.userInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncResult.success(SyncActions.SYNC_USERS, true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
    }

    @Test
    public void syncedUserInfoReturnsUserInfoFromStorageAfterSync() {
        final PublishSubject<SyncResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.syncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncResult.success(SyncActions.SYNC_USERS, true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoFromStorage() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.<SyncResult>never());
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user));

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(user);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoAgainFromStorageAfterSync() {
        final PublishSubject<SyncResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user), Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(user);

        subject.onNext(SyncResult.success(SyncActions.SYNC_USERS, true));

        assertThat(observer.getOnNextEvents()).containsExactly(user, updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsDoesNotEmitMissingUser() {
        final PublishSubject<SyncResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(PropertySet.create()), Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncResult.success(SyncActions.SYNC_USERS, true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
    }

}
