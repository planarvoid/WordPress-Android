package com.soundcloud.android.you;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.view.View;

import java.util.List;

public class YouPresenterTest extends AndroidUnitTest {

    private static final PropertySet USER = TestPropertySets.user();
    private static final Urn USER_URN = USER.get(UserProperty.URN);

    private YouPresenter presenter;

    @Mock private YouViewFactory youViewFactory;
    @Mock private YouFragment fragment;
    @Mock private View fragmentView;
    @Mock private YouView youView;
    @Mock private UserRepository userRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private Navigator navigator;
    @Mock private BugReporter bugReporter;
    @Mock private ApplicationProperties appProperties;
    @Mock private SyncConfig syncConfig;
    @Mock private FeatureFlags featureFlags;

    @Captor private ArgumentCaptor<YouView.Listener> listenerArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        presenter = new YouPresenter(youViewFactory, userRepository, accountOperations, imageOperations, resources(),
                eventBus, featureOperations, offlineContentOperations, navigator, bugReporter, appProperties, syncConfig,
                featureFlags);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(youViewFactory.create(same(fragmentView), listenerArgumentCaptor.capture())).thenReturn(youView);
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(USER));
    }

    @Test
    public void onCreateDoesNothingWithNoView() {
        presenter.onCreate(fragment, null);

        verifyZeroInteractions(youViewFactory);
        verifyZeroInteractions(youView);
    }

    @Test
    public void onViewCreatedBindsLoadedUserToView() {
        setupForegroundFragment();

        verifyUserBound();
    }

    @Test
    public void onViewCreatedBindsUserToViewWhenLoadedAfterViewCreated() {
        final PublishSubject<PropertySet> subject = PublishSubject.create();
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(subject);

        setupForegroundFragment();

        subject.onNext(USER);

        verifyUserBound();
    }

    @Test
    public void onViewCreatedSendsUpsellImpressionIfUpselling() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        setupForegroundFragment();

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(trackingEvents).hasSize(1);
        assertThat(trackingEvents.get(0).getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
    }

    @Test
    public void onViewCreatedSendsNoUpsellImpressionIfNotUpselling() {
        setupForegroundFragment();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void hidesOfflineSettingsWithNoOfflineContentOrAccess() {
        setupForegroundFragment();

        verify(youView).hideOfflineSettings();
    }

    @Test
    public void showsOfflineSettingsWithUpgrade() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        setupForegroundFragment();

        verify(youView).showOfflineSettings();
    }

    @Test
    public void showsOfflineSettingsWhenOfflineContentEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setupForegroundFragment();

        verify(youView).showOfflineSettings();
    }

    @Test
    public void showsOfflineSettingsWithOfflineContent() {
        when(offlineContentOperations.hasOfflineContent()).thenReturn(true);

        setupForegroundFragment();

        verify(youView).showOfflineSettings();
    }

    @Test
    public void showsShowsReportBugForConfiguredBuilds() {
        when(appProperties.shouldAllowFeedback()).thenReturn(true);

        setupForegroundFragment();

        verify(youView).showReportBug();
    }

    @Test
    public void onOfflineSettingsClickSendsUpsellClickEventIfUpselling() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(trackingEvents).hasSize(2);
        assertThat(trackingEvents.get(1).getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
    }

    @Test
    public void onOfflineSettingsClickSendsNoUpsellClickEventIfNotUpselling() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onExploreClickedNavigatesToExplore() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onExploreClicked(new View(context()));

        verify(navigator).openExplore(context(), Screen.YOU);
    }

    @Test
    public void onActivitiesClickedNavigatesToActivities() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onActivitiesClicked(new View(context()));

        verify(navigator).openActivities(context());
    }

    @Test
    public void onRecordClickedNavigatesToRecord() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onRecordClicked(new View(context()));

        verify(navigator).openRecord(context(), Screen.YOU);
    }

    @Test
    public void onProfileClickedNavigatesToProfile() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onProfileClicked(new View(context()));

        verify(navigator).openProfile(context(), USER_URN);
    }

    @Test
    public void onOfflineSettingsClickedShowsOfflineSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettings(context());
    }

    @Test
    public void onNotificationSettingsClickedShowsNotificationSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onNotificationSettingsClicked(new View(context()));

        verify(navigator).openNotificationSettings(context());
    }

    @Test
    public void onNewNotificationSettingsClickedShowsNewNotificationSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onNewNotificationSettingsClicked(new View(context()));

        verify(navigator).openNewNotificationSettings(context());
    }

    @Test
    public void onServerSideNotificationsNotificationSettingsIsHidden() {
        when(syncConfig.isServerSideNotifications()).thenReturn(true);

        setupForegroundFragment();

        verify(youView).hideNotificationSettings();
    }

    @Test
    public void onClientSideNotificationsNotificationSettingsIsNotHidden() {
        when(syncConfig.isServerSideNotifications()).thenReturn(false);

        setupForegroundFragment();

        verify(youView, never()).hideNotificationSettings();
    }

    @Test
    public void onBasicSettingsClickedShowsSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onBasicSettingsClicked(new View(context()));

        verify(navigator).openSettings(context());
    }

    @Test
    public void onBugReportClickedShowsReportDialog() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onReportBugClicked(new View(context()));

        verify(bugReporter).showGeneralFeedbackDialog(context());
    }

    @Test
    public void onHelpCenterClickedShowsHelpCenter() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onHelpCenterClicked(new View(context()));

        verify(navigator).openHelpCenter(context());
    }

    @Test
    public void onLegalClickedShowsLegal() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onLegalClicked(new View(context()));

        verify(navigator).openLegal(context());
    }

    @Test
    public void unbindsHeaderViewInOnDestroyView() {
        final PublishSubject<PropertySet> subject = PublishSubject.create();
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(subject);

        setupForegroundFragment();
        presenter.onDestroyView(fragment);

        verify(youView).unbind();
    }

    @Test
    public void resetScrollDoesNothingWithNoView() {
        presenter.resetScroll();

        verifyZeroInteractions(youView);
    }

    @Test
    public void resetScrollResetsScrollOnView() {
        setupForegroundFragment();

        presenter.resetScroll();

        verify(youView).resetScroll();
    }

    private void setupForegroundFragment() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);
    }

    private void verifyUserBound() {
        verify(youView).setUsername(USER.get(UserProperty.USERNAME));
    }
}
