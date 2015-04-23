package com.soundcloud.android.accounts;

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
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class UserRepositoryTest {

    private UserRepository userRepository;

    @Mock private ApiClientRx apiClientRx;
    @Mock private UserStorage userStorage;
    @Mock private Observer<PublicApiUser> userObserver;


    @Before
    public void setup() {
        userRepository = new UserRepository(apiClientRx, userStorage, Schedulers.immediate());
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
        verify(userStorage).createOrUpdate(currentUser);
    }
}
