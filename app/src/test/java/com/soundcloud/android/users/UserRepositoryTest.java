package com.soundcloud.android.users;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UserRepositoryTest extends AndroidUnitTest {

    private UserRepository userRepository;

    private final Urn userUrn = Urn.forUser(123L);
    private final User user = ModelFixtures.userBuilder(false)
                                           .urn(Urn.forUser(123L)).build();
    private final User updatedUser = ModelFixtures.userBuilder(false)
                                                  .urn(Urn.forUser(123L))
                                                  .username("updated-name")
                                                  .build();

    @Mock private UserStorage userStorage;
    @Mock private SyncInitiator syncInitiator;

    @Before
    public void setup() {
        userRepository = new UserRepository(userStorage, syncInitiator, Schedulers.trampoline());
    }

    @Test
    public void localUserInfoReturnsUserInfoFromStorage() {
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(updatedUser));

        userRepository.localUserInfo(userUrn)
                      .test()
                      .assertValue(updatedUser);

        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void userInfoReturnsUserInfoFromStorage() {
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(user));
        when(syncInitiator.syncUser(userUrn)).thenReturn(Single.never());

        userRepository.userInfo(userUrn)
                      .test()
                      .assertValue(user);
    }

    @Test
    public void userInfoReturnsUserInfoFromSyncerIfStorageEmpty() {
        final SingleSubject<SyncJobResult> subject = SingleSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.empty(),
                                                       Maybe.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        TestObserver<User> testObserver = userRepository.userInfo(userUrn)
                                                            .test()
                                                            .assertNoValues();

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name(), true));

        testObserver.assertValue(updatedUser);
    }

    @Test
    public void syncedUserInfoReturnsUserInfoFromStorageAfterSync() {
        final SingleSubject<SyncJobResult> subject = SingleSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        TestObserver<User> testObserver = userRepository.syncedUserInfo(userUrn).test()
                                                            .assertNoValues();

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name(), true));

        testObserver.assertValue(updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoFromStorage() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Single.never());
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(user));

        userRepository.localAndSyncedUserInfo(userUrn)
                      .test()
                      .assertValue(user);
    }

    @Test
    public void localAndSyncedUserInfoReturnsUserInfoAgainFromStorageAfterSync() {
        final SingleSubject<SyncJobResult> subject = SingleSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(user), Maybe.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        TestObserver<User> testObserver = userRepository.localAndSyncedUserInfo(userUrn)
                                                            .test()
                                                            .assertValue(user);

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name(), true));

        testObserver.assertValues(user, updatedUser);
    }

    @Test
    public void localAndSyncedUserInfoReturnsDoesNotEmitMissingUser() {
        final SingleSubject<SyncJobResult> subject = SingleSubject.create();
        when(userStorage.loadUser(userUrn)).thenReturn(Maybe.empty(),
                                                       Maybe.just(updatedUser));
        when(syncInitiator.syncUser(userUrn)).thenReturn(subject);

        TestObserver<User> testObserver = userRepository.localAndSyncedUserInfo(userUrn)
                                                            .test()
                                                            .assertNoValues();

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name(), true));

        testObserver.assertValue(updatedUser);
    }

}
