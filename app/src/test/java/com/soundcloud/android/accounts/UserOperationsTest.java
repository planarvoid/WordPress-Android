package com.soundcloud.android.accounts;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.SoundCloudRxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.storage.UserStorage;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class UserOperationsTest {

    private UserOperations userOperations;

    @Mock
    private SoundCloudRxHttpClient httpClient;
    @Mock
    private UserStorage userStorage;
    @Mock
    private Observer<PublicApiUser> userObserver;


    @Before
    public void setup() {
        userOperations = new UserOperations(httpClient, userStorage);
    }

    @Test
    public void shouldRefreshTheUserFromTheApiAndPersistToLocalStorage() throws CreateModelException {
        PublicApiUser currentUser = TestHelper.getModelFactory().createModel(PublicApiUser.class);
        when(httpClient.<PublicApiUser>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(currentUser));

        userOperations.refreshCurrentUser().subscribe(userObserver);

        verify(httpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.CURRENT_USER.path())));
        verify(userStorage).createOrUpdateAsync(currentUser);
    }
}
