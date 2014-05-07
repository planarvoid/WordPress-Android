package com.soundcloud.android.storage;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class UserStorageTest {

    private UserStorage userStorage;
    private User user = new User();

    @Mock
    private UserDAO userDAO;

    @Mock
    private Observer<User> observer;

    @Before
    public void setup() {
        userStorage = new UserStorage(userDAO, Schedulers.immediate());
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
