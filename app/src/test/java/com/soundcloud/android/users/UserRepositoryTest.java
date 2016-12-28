package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class UserRepositoryTest extends AndroidUnitTest {

    private UserRepository userRepository;

    private final Urn userUrn = Urn.forUser(123L);
    private final PropertySet user = PropertySet.from(UserProperty.URN.bind(Urn.forUser(123L)));
    private final UserItem userItem = UserItem.from(user);
    private final PropertySet updatedUser = PropertySet.create()
                                                       .put(UserProperty.URN, Urn.forUser(123L))
                                                       .put(UserProperty.USERNAME, "name");


    private TestSubscriber<PropertySet> observer = new TestSubscriber<>();
    private TestSubscriber<UserItem> userObserver = new TestSubscriber<>();

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
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.<SyncJobResult>never());

        userRepository.userInfo(userUrn).subscribe(userObserver);

        assertThat(userObserver.getOnNextEvents()).containsExactly(userItem);
    }

    @Test
    public void userInfoReturnsUserInfoFromSyncerIfStorageEmpty() {
        final PublishSubject<SyncJobResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(PropertySet.create()),
                                                       Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.userInfo(userUrn).subscribe(userObserver);

        assertThat(userObserver.getOnNextEvents()).isEmpty();

        subject.onNext(SyncJobResult.success(Syncable.USERS.name(), true));

        assertThat(userObserver.getOnNextEvents()).containsExactly(UserItem.from(updatedUser));
    }

    @Test
    public void syncedUserInfoReturnsUserInfoFromStorageAfterSync() {
        final PublishSubject<SyncJobResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.syncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncJobResult.success(Syncable.USERS.name(), true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoFromStorage() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.<SyncJobResult>never());
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user));

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(user);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoAgainFromStorageAfterSync() {
        final PublishSubject<SyncJobResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user), Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(user);

        subject.onNext(SyncJobResult.success(Syncable.USERS.name(), true));

        assertThat(observer.getOnNextEvents()).containsExactly(user, updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsDoesNotEmitMissingUser() {
        final PublishSubject<SyncJobResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(PropertySet.create()),
                                                       Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncJobResult.success(Syncable.USERS.name(), true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
    }

}
