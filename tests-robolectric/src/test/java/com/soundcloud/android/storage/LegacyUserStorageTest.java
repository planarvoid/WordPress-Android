package com.soundcloud.android.storage;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class LegacyUserStorageTest {

    private LegacyUserStorage userStorage;
    private PublicApiUser user = new PublicApiUser();

    @Mock
    private UserDAO userDAO;

    @Mock
    private Observer<PublicApiUser> observer;

    @Before
    public void setup() {
        userStorage = new LegacyUserStorage(userDAO, Schedulers.immediate());
    }

    @Test
    public void shouldLoadUserFromLocalStorage() {
        when(userDAO.queryById(123L)).thenReturn(user);

        userStorage.getUserAsync(123L).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(user);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

}
