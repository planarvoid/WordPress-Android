package com.soundcloud.android.profile;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import io.reactivex.subjects.BehaviorSubject;

@RunWith(MockitoJUnitRunner.class)
public class FollowersPresenterTest {
    private static final Screen currentUserScreen = Screen.YOUR_FOLLOWERS;
    private static final Screen otherUserScreen = Screen.USER_FOLLOWERS;
    private static final Urn userUrn = new Urn("soundcloud:users:12345678");
    private static final Optional<ReferringEvent> referringEventOptional = Optional.of(ReferringEvent.create("123", "kind"));

    @Mock EventTracker eventTracker;
    @Mock ReferringEventProvider referringEventProvider;
    @Mock AccountOperations accountOperations;
    @Mock FollowersPresenter.FollowersView view;

    private final BehaviorSubject<Long> enterScreen = BehaviorSubject.create();

    private FollowersPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new FollowersPresenter(eventTracker, referringEventProvider, accountOperations);
        when(view.enterScreenTimestamp()).thenReturn(enterScreen);
        when(view.getUserUrn()).thenReturn(userUrn);
        when(referringEventProvider.getReferringEvent()).thenReturn(referringEventOptional);
    }

    @Test
    public void emitsScreenEventForCurrentUserWithUserUrnOnEnterScreen() throws Exception {
        when(accountOperations.isLoggedInUser(userUrn)).thenReturn(true);

        presenter.attachView(view);

        enterScreen.onNext(123L);

        verify(eventTracker).trackScreen(eq(ScreenEvent.create(currentUserScreen, userUrn)), eq(referringEventOptional));
    }

    @Test
    public void emitsScreenEventForOtherUserWithUserUrnOnEnterScreen() throws Exception {
        when(accountOperations.isLoggedInUser(userUrn)).thenReturn(false);

        presenter.attachView(view);

        enterScreen.onNext(123L);

        verify(eventTracker).trackScreen(eq(ScreenEvent.create(otherUserScreen, userUrn)), eq(referringEventOptional));
    }

    @Test
    public void visitsFollowersScreenForCurrentUser() throws Exception {
        when(accountOperations.isLoggedInUser(userUrn)).thenReturn(true);

        presenter.visitFollowersScreen(view);

        verify(view).visitFollowersScreenForCurrentUser(currentUserScreen);
    }

    @Test
    public void visitsFollowersScreenForOtherUser() throws Exception {
        when(accountOperations.isLoggedInUser(userUrn)).thenReturn(false);

        presenter.visitFollowersScreen(view);

        verify(view).visitFollowersScreenForOtherUser(otherUserScreen);
    }
}
