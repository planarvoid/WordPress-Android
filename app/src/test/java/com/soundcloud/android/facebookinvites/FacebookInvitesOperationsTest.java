package com.soundcloud.android.facebookinvites;

import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.CLICK_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.CREATOR_DISMISS_FOR_LISTENERS_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.CREATOR_DISMISS_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.DISMISS_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.REST_AFTER_DISMISS_COUNT;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.SHOW_AFTER_OPENS_COUNT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.facebookapi.FacebookApiHelper;
import com.soundcloud.android.profile.LastPostedTrack;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.TestDateProvider;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class FacebookInvitesOperationsTest {

    private FacebookInvitesOperations operations;

    @Mock private FacebookInvitesStorage storage;
    @Mock private FacebookApi facebookApi;
    @Mock private FacebookApiHelper facebookApiHelper;
    @Mock private ConnectionHelper connectionHelper;
    @Mock private MyProfileOperations myProfileOperations;

    private TestDateProvider dateProvider = new TestDateProvider();

    @Before
    public void setUp() throws Exception {
        operations = new FacebookInvitesOperations(storage,
                                                   facebookApiHelper,
                                                   connectionHelper,
                                                   dateProvider,
                                                   myProfileOperations);
        dateProvider.setTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);

        when(storage.getMillisSinceLastClick()).thenReturn(CLICK_INTERVAL_MS);
        when(storage.getTimesAppOpened()).thenReturn(SHOW_AFTER_OPENS_COUNT);
        when(storage.getMillisSinceLastListenerDismiss()).thenReturn(DISMISS_INTERVAL_MS);
        when(storage.getTimesListenerDismissed()).thenReturn(0);
        when(storage.getMillisSinceLastCreatorDismiss()).thenReturn(CREATOR_DISMISS_INTERVAL_MS);
        when(facebookApiHelper.canShowAppInviteDialog()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
    }

    @Test
    public void canShowForListenersOnSetup() throws Exception {
        assertThat(shouldShowForListeners()).isTrue();
    }

    @Test
    public void shouldNotShowForListenersWhenAppInviteDialogIsFalse() throws Exception {
        when(facebookApiHelper.canShowAppInviteDialog()).thenReturn(false);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldNotShowForListenersWhenAppNotOpenedEnoughTimes() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(SHOW_AFTER_OPENS_COUNT - 1);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldNotShowForListenersTheFirstTime() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(0);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldPersistForListenersAfterAppOpenedEnoughTimes() throws Exception {
        when(storage.getTimesAppOpened()).thenReturn(SHOW_AFTER_OPENS_COUNT + 1);

        assertThat(shouldShowForListeners()).isTrue();
    }

    @Test
    public void shouldNotShowForListenersWhenLastClickTooEarly() throws Exception {
        when(storage.getMillisSinceLastClick()).thenReturn(CLICK_INTERVAL_MS - 1);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldNotShowForListenersWhenConnectionNotAvailable() throws Exception {
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldNotShowForListenersWhenDismissedBeforeClickInterval() throws Exception {
        when(storage.getTimesListenerDismissed()).thenReturn(REST_AFTER_DISMISS_COUNT - 1);
        when(storage.getMillisSinceLastListenerDismiss()).thenReturn(CLICK_INTERVAL_MS - 1);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldShowForListenersWhenDismissedAfterClickInterval() throws Exception {
        when(storage.getTimesListenerDismissed()).thenReturn(REST_AFTER_DISMISS_COUNT - 1);
        when(storage.getMillisSinceLastListenerDismiss()).thenReturn(CLICK_INTERVAL_MS);

        assertThat(shouldShowForListeners()).isTrue();
    }

    @Test
    public void shouldNotForListenersShowWhenDismissedBeforeDismissInterval() throws Exception {
        when(storage.getTimesListenerDismissed()).thenReturn(REST_AFTER_DISMISS_COUNT);
        when(storage.getMillisSinceLastListenerDismiss()).thenReturn(DISMISS_INTERVAL_MS - 1);

        assertThat(shouldShowForListeners()).isFalse();
    }

    @Test
    public void shouldShowForListenersWhenDismissedAfterDismissInterval() throws Exception {
        when(storage.getTimesListenerDismissed()).thenReturn(REST_AFTER_DISMISS_COUNT);
        when(storage.getMillisSinceLastListenerDismiss()).thenReturn(DISMISS_INTERVAL_MS);

        assertThat(shouldShowForListeners()).isTrue();
    }

    @Test
    public void shouldShowForListenersWhenLastClickAfter() throws Exception {
        when(storage.getMillisSinceLastClick()).thenReturn(CLICK_INTERVAL_MS + 1);

        assertThat(shouldShowForListeners()).isTrue();
    }

    @Test
    public void shouldNotShowForListenersWhenCreatorDismissedBeforeDismissInterval() throws Exception {
        when(storage.getMillisSinceLastCreatorDismiss()).thenReturn(CREATOR_DISMISS_FOR_LISTENERS_INTERVAL_MS - 1);

        assertThat(shouldShowForListeners()).isFalse();
    }

    private boolean shouldShowForListeners() {
        final TestObserver<Boolean> subscriber = operations.canShowForListeners().test();
        return subscriber.values().get(0);
    }

    @Test
    public void shouldLoadForCreatorsWhenRecentPost() throws Exception {
        LastPostedTrack track = PlayableFixtures.expectedLastPostedTrackForPostsScreen();
        when(myProfileOperations.lastPublicPostedTrack()).thenReturn(Observable.just(track));

        final TestObserver<StreamItem> subscriber = operations.creatorInvites().test();

        final StreamItem.FacebookCreatorInvites invitesItem = (StreamItem.FacebookCreatorInvites) subscriber.values().get(0);
        assertThat(invitesItem.trackUrn()).isEqualTo(track.urn());
    }

}
