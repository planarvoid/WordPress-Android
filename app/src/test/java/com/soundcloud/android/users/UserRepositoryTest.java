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
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
    private final User user = ModelFixtures.userBuilder(false)
                                           .urn(Urn.forUser(123L)).build();
    private final User updatedUser =  ModelFixtures.userBuilder(false)
                                                   .urn(Urn.forUser(123L))
                                                   .username("updated-name")
                                                   .build();


    private TestSubscriber<User> observer = new TestSubscriber<>();

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
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.never());

        userRepository.userInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(user);
    }

    @Test
    public void userInfoReturnsUserInfoFromSyncerIfStorageEmpty() {
        final PublishSubject<SyncJobResult> subject = PublishSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.empty(),
                                                       Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.userInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncJobResult.success(Syncable.USERS.name(), true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
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
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.empty(),
                                                       Observable.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        userRepository.localAndSyncedUserInfo(userUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();

        subject.onNext(SyncJobResult.success(Syncable.USERS.name(), true));

        assertThat(observer.getOnNextEvents()).containsExactly(updatedUser);
    }

}
