package com.soundcloud.android.more;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ChartsExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserItem;
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

public class MoreTabPresenterTest extends AndroidUnitTest {

    private static final UserItem USER = UserItem.from(TestPropertySets.user());
    private static final Urn USER_URN = USER.getUrn();

    private MoreTabPresenter presenter;

    @Mock private MoreViewFactory moreViewFactory;
    @Mock private MoreFragment fragment;
    @Mock private View fragmentView;
    @Mock private MoreView moreView;
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
    @Mock private OfflineSettingsStorage storage;
    @Mock private ChartsExperiment chartsExperiment;

    @Captor private ArgumentCaptor<MoreView.Listener> listenerArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        presenter = new MoreTabPresenter(moreViewFactory,
                                         userRepository,
                                         accountOperations,
                                         imageOperations,
                                         resources(),
                                         eventBus,
                                         featureOperations,
                                         offlineContentOperations,
                                         navigator,
                                         bugReporter,
                                         appProperties,
                                         storage,
                                         featureFlags,
                                         chartsExperiment);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(moreViewFactory.create(same(fragmentView), listenerArgumentCaptor.capture())).thenReturn(moreView);
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(USER));
        when(featureFlags.isEnabled(Flag.EXPLORE)).thenReturn(true);
        when(chartsExperiment.isEnabled()).thenReturn(false);
    }

    @Test
    public void onCreateDoesNothingWithNoView() {
        presenter.onCreate(fragment, null);

        verifyZeroInteractions(moreViewFactory);
        verifyZeroInteractions(moreView);
    }

    @Test
    public void onViewCreatedBindsLoadedUserToView() {
        setupForegroundFragment();

        verifyUserBound();
    }

    @Test
    public void onViewCreatedBindsUserToViewWhenLoadedAfterViewCreated() {
        final PublishSubject<UserItem> subject = PublishSubject.create();

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
        assertThat(trackingEvents.get(0).getKind()).isEqualTo(UpgradeFunnelEvent.KIND_UPSELL_IMPRESSION);
    }

    @Test
    public void onViewCreatedSendsNoUpsellImpressionIfNotUpselling() {
        setupForegroundFragment();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onViewCreatedShowsGoIndicatorForGoUsers() {
        when(featureOperations.hasGoPlan()).thenReturn(true);

        setupForegroundFragment();

        verify(moreView).showGoIndicator(true);
    }

    @Test
    public void onViewCreatedDoesNotShowGoIndicatorForFreeUsers() {
        when(featureOperations.hasGoPlan()).thenReturn(false);

        setupForegroundFragment();

        verify(moreView).showGoIndicator(false);
    }

    @Test
    public void hidesOfflineSettingsWithNoOfflineContentOrAccess() {
        setupForegroundFragment();

        verify(moreView).hideOfflineSettings();
    }

    @Test
    public void showsOfflineSettingsWithUpgrade() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        setupForegroundFragment();

        verify(moreView).showOfflineSettings();
    }

    @Test
    public void hidesExploreWhenExploreDisabled() {
        when(featureFlags.isEnabled(Flag.EXPLORE)).thenReturn(false);

        setupForegroundFragment();

        verify(moreView).hideExplore();
    }

    @Test
    public void hidesExploreWhenChartsExperimentEnabled() {
        when(chartsExperiment.isEnabled()).thenReturn(true);

        setupForegroundFragment();

        verify(moreView).hideExplore();
    }

    @Test
    public void showsExploreWhenFlagEnabled() {
        setupForegroundFragment();

        verify(moreView).showExplore();
    }

    @Test
    public void showsOfflineSettingsWhenOfflineContentEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        setupForegroundFragment();

        verify(moreView).showOfflineSettings();
    }

    @Test
    public void showsOfflineSettingsWithOfflineContent() {
        when(offlineContentOperations.hasOfflineContent()).thenReturn(true);

        setupForegroundFragment();

        verify(moreView).showOfflineSettings();
    }

    @Test
    public void showsShowsReportBugForConfiguredBuilds() {
        when(appProperties.shouldAllowFeedback()).thenReturn(true);

        setupForegroundFragment();

        verify(moreView).showReportBug();
    }

    @Test
    public void onOfflineSettingsClickSendsUpsellClickEventIfUpselling() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(trackingEvents).hasSize(2);
        assertThat(trackingEvents.get(1).getKind()).isEqualTo(UpgradeFunnelEvent.KIND_UPSELL_CLICK);
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

        verify(navigator).openExplore(context(), Screen.MORE);
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

        verify(navigator).openRecord(context(), Screen.MORE);
    }

    @Test
    public void onProfileClickedNavigatesToProfile() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onProfileClicked(new View(context()));

        verify(navigator).legacyOpenProfile(context(), USER_URN);
    }

    @Test
    public void onOfflineSettingsClickedShowsOfflineSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettings(context());
    }

    @Test
    public void onOfflineSettingsClickedShowsOnboardingWhenHasNotBeenSeenBefore() {
        setupForegroundFragment();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(storage.hasSeenOfflineSettingsOnboarding()).thenReturn(false);

        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettingsOnboarding(context());
    }

    @Test
    public void onOfflineSettingsClickedDoesNotShowOnboardingWhenHasBeenSeenBefore() {
        setupForegroundFragment();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(storage.hasSeenOfflineSettingsOnboarding()).thenReturn(true);

        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettings(context());
    }

    @Test
    public void onNotificationSettingsClickedShowsNotificationSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onNotificationPreferencesClicked(new View(context()));

        verify(navigator).openNotificationPreferences(context());
    }

    @Test
    public void onBasicSettingsClickedShowsSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onBasicSettingsClicked(new View(context()));

        verify(navigator).openBasicSettings(context());
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

        verify(moreView).unbind();
    }

    @Test
    public void resetScrollDoesNothingWithNoView() {
        presenter.resetScroll();

        verifyZeroInteractions(moreView);
    }

    @Test
    public void resetScrollResetsScrollOnView() {
        setupForegroundFragment();

        presenter.resetScroll();

        verify(moreView).resetScroll();
    }

    private void setupForegroundFragment() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);
    }

    private void verifyUserBound() {
        verify(moreView).setUsername(USER.getName());
    }
}
