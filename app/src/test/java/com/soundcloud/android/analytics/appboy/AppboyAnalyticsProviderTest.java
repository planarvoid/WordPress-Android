package com.soundcloud.android.analytics.appboy;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appboy.AppboyUser;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AppboyAnalyticsProviderTest extends AndroidUnitTest {

    private AppboyAnalyticsProvider appboyAnalyticsProvider;

    @Mock private AppboyWrapper appboy;
    @Mock private AccountOperations accountOperations;
    @Mock private AppboyUser appboyUser;
    @Mock private AppboyEventHandler eventHandler;

    private Urn userUrn = Urn.forUser(123L);
    private Urn otherUserUrn = Urn.forUser(234L);

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        when(appboyUser.getUserId()).thenReturn(userUrn.toString());

        appboyAnalyticsProvider = new AppboyAnalyticsProvider(appboy, eventHandler, accountOperations);
    }

    @Test
    public void shouldChangeUserIdWhenUserChangedOnConstructed() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(otherUserUrn);

        appboyAnalyticsProvider = new AppboyAnalyticsProvider(appboy, eventHandler, accountOperations);

        verify(appboy).changeUser(otherUserUrn.toString());
    }

    @Test
    public void shouldNotChangeUserIdWhenUserLoggedOutOnConstructed() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.NOT_SET);

        appboyAnalyticsProvider = new AppboyAnalyticsProvider(appboy, eventHandler, accountOperations);

        verify(appboy, never()).changeUser(Urn.NOT_SET.toString());
    }

    @Test
    public void shouldForwardFlushCallToAppboy() {
        appboyAnalyticsProvider.flush();
        verify(appboy).requestImmediateDataFlush();
    }

    @Test
    public void shouldHandleUserChangeEvents() {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(otherUserUrn);

        appboyAnalyticsProvider.handleCurrentUserChangedEvent(event);

        verify(appboy).changeUser(otherUserUrn.toString());
    }

}
