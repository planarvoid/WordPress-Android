package com.soundcloud.android.facebookinvites;

import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.CLICK_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.CREATOR_DISMISS_FOR_LISTENERS_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.CREATOR_DISMISS_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.DISMISS_INTERVAL_MS;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.REST_AFTER_DISMISS_COUNT;
import static com.soundcloud.android.facebookinvites.FacebookInvitesOperations.SHOW_AFTER_OPENS_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.facebookapi.FacebookApiHelper;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;

public class FacebookInvitesOperationsTest extends AndroidUnitTest {

    private FacebookInvitesOperations operations;

    @Mock private FacebookInvitesStorage storage;
    @Mock private FacebookApi facebookApi;
    @Mock private FacebookApiHelper facebookApiHelper;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private MyProfileOperations myProfileOperations;

    private TestDateProvider dateProvider = new TestDateProvider();

    @Before
    public void setUp() throws Exception {
        operations = new FacebookInvitesOperations(storage,
                                                   facebookApiHelper,
                                                   networkConnectionHelper,
                                                   dateProvider,
                                                   myProfileOperations);
        dateProvider.setTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);

        when(storage.getMillisSinceLastClick()).thenReturn(CLICK_INTERVAL_MS);
        when(storage.getTimesAppOpened()).thenReturn(SHOW_AFTER_OPENS_COUNT);
        when(storage.getMillisSinceLastListenerDismiss()).thenReturn(DISMISS_INTERVAL_MS);
        when(storage.getTimesListenerDismissed()).thenReturn(0);
        when(storage.getMillisSinceLastCreatorDismiss()).thenReturn(CREATOR_DISMISS_INTERVAL_MS);
        when(facebookApiHelper.canShowAppInviteDialog()).thenReturn(true);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
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
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);

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
        final TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        operations.canShowForListeners().subscribe(subscriber);
        return subscriber.getOnNextEvents().get(0);
    }

    @Test
    public void shouldLoadForCreatorsWhenRecentPost() throws Exception {
        final TestSubscriber<StreamItem> subscriber = new TestSubscriber<>();
        PropertySet track = TestPropertySets.expectedPostedTrackForPostsScreen();
        when(myProfileOperations.lastPublicPostedTrack()).thenReturn(Observable.just(track));

        operations.creatorInvites().subscribe(subscriber);

        final StreamItem.FacebookCreatorInvites invitesItem = (StreamItem.FacebookCreatorInvites) subscriber.getOnNextEvents()
                                                                                                            .get(0);
        assertThat(invitesItem.trackUrn()).isEqualTo(track.get(PlayableProperty.URN));
    }

}
