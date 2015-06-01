package com.soundcloud.android.users;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
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
    @Mock private LegacyUserStorage legacyUserStorage;
    @Mock private UserStorage userStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Observer<PublicApiUser> userObserver;


    @Before
    public void setup() {
        userRepository = new UserRepository(apiClientRx, legacyUserStorage, userStorage, syncInitiator, Schedulers.immediate());
    }

    @Test
    public void shouldRefreshTheUserFromTheApiAndPersistToLocalStorage() throws CreateModelException {
        PublicApiUser currentUser = ModelFixtures.create(PublicApiUser.class);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(PublicApiUser.class)))
                .thenReturn(Observable.just(currentUser));

        userRepository.refreshCurrentUser().subscribe(userObserver);

        verify(apiClientRx).mappedResponse(
                argThat(isPublicApiRequestTo("GET", ApiEndpoints.CURRENT_USER.path())),
                eq(PublicApiUser.class));
        verify(legacyUserStorage).createOrUpdate(currentUser);
    }

    @Test
    public void userInfoWithUpdateReturnsUserInfoFromStorage() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.<SyncResult>empty());
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user));

        userRepository.userInfoWithUpdate(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(user);
    }

    @Test
    public void userInfoWithUpdateReturnsUserInfoAgainFromStorageAfterSync() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_USERS, true)));
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(user), Observable.just(updatedUser));

        userRepository.userInfoWithUpdate(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(user, updatedUser);
    }

    @Test
    public void userInfoWithUpdateReturnsDoesNotEmitMissingUser() {
        when(syncInitiator.syncUser(userUrn)).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_USERS, true)));
        when(userStorage.loadUser(userUrn)).thenReturn(Observable.just(PropertySet.create()), Observable.just(updatedUser));

        userRepository.userInfoWithUpdate(userUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(updatedUser);
    }

}
