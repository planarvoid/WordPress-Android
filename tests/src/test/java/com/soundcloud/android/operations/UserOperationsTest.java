package com.soundcloud.android.operations;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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
    private Observer<User> userObserver;


    @Before
    public void setup() {
        userOperations = new UserOperations(httpClient, userStorage);
    }

    @Test
    public void shouldRefreshTheUserFromTheApiAndPersistToLocalStorage() throws CreateModelException {
        User currentUser = TestHelper.getModelFactory().createModel(User.class);
        when(httpClient.<User>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(currentUser));

        userOperations.refreshCurrentUser().subscribe(userObserver);

        verify(httpClient).fetchModels(argThat(isApiRequestTo(APIEndpoints.CURRENT_USER.path())));
        verify(userStorage).createOrUpdateAsync(currentUser);
    }
}
