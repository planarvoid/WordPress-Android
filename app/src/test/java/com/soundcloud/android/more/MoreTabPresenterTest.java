package com.soundcloud.android.more;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
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
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.view.View;

public class MoreTabPresenterTest extends AndroidUnitTest {

    private static final User USER = ModelFixtures.user();
    private static final Urn USER_URN = USER.urn();

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
    @Mock private OfflineSettingsStorage storage;
    @Mock private ConfigurationManager configurationManager;
    @Mock private FeatureFlags flags;

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
                                         configurationManager,
                                         flags);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(moreViewFactory.create(same(fragmentView), listenerArgumentCaptor.capture())).thenReturn(moreView);
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(USER));
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(flags.isEnabled(Flag.MID_TIER)).thenReturn(true);
    }

    @Test
    public void onCreateDoesNothingWithNoView() {
        presenter.onCreate(fragment, null);

        verifyZeroInteractions(moreViewFactory);
        verifyZeroInteractions(moreView);
    }

    @Test
    public void onViewCreatedBindsLoadedUserToView() {
        initFragment();

        verifyUserBound();
    }

    @Test
    public void onViewCreatedBindsUserToViewWhenLoadedAfterViewCreated() {
        final PublishSubject<User> subject = PublishSubject.create();
        initFragment();

        subject.onNext(USER);

        verifyUserBound();
    }

    @Test
    public void onViewCreatedSendsNoUpsellImpressionIfNotUpselling() {
        initFragment();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void onTabFocusSendsUpsellImpressionIfUpselling() {
        when(moreView.isUpsellVisible()).thenReturn(true);
        initFragment();

        presenter.onFocusChange(true);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).get(0).getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
    }

    @Test
    public void showsShowsReportBugForConfiguredBuilds() {
        when(appProperties.shouldAllowFeedback()).thenReturn(true);

        initFragment();

        verify(moreView).showReportBug();
    }

    @Test
    public void onActivitiesClickedNavigatesToActivities() {
        initFragment();
        listenerArgumentCaptor.getValue().onActivitiesClicked(new View(context()));

        verify(navigator).openActivities(context());
    }

    @Test
    public void onRecordClickedNavigatesToRecord() {
        initFragment();
        listenerArgumentCaptor.getValue().onRecordClicked(new View(context()));

        verify(navigator).openRecord(context(), Screen.MORE);
    }

    @Test
    public void onProfileClickedNavigatesToProfile() {
        initFragment();
        listenerArgumentCaptor.getValue().onProfileClicked(new View(context()));

        verify(navigator).legacyOpenProfile(context(), USER_URN);
    }

    @Test
    public void onOfflineSettingsClickedShowsOfflineSettings() {
        initFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettings(context());
    }

    @Test
    public void onOfflineSettingsClickedShowsOnboardingWhenHasNotBeenSeenBefore() {
        initFragment();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(storage.hasSeenOfflineSettingsOnboarding()).thenReturn(false);

        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettingsOnboarding(context());
    }

    @Test
    public void onOfflineSettingsClickedDoesNotShowOnboardingWhenHasBeenSeenBefore() {
        initFragment();
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(storage.hasSeenOfflineSettingsOnboarding()).thenReturn(true);

        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettings(context());
    }

    @Test
    public void onNotificationSettingsClickedShowsNotificationSettings() {
        initFragment();
        listenerArgumentCaptor.getValue().onNotificationPreferencesClicked(new View(context()));

        verify(navigator).openNotificationPreferences(context());
    }

    @Test
    public void onBasicSettingsClickedShowsSettings() {
        initFragment();
        listenerArgumentCaptor.getValue().onBasicSettingsClicked(new View(context()));

        verify(navigator).openBasicSettings(context());
    }

    @Test
    public void onUpsellClickedOpensUpgradeScreen() {
        initFragment();
        listenerArgumentCaptor.getValue().onUpsellClicked(new View(context()));

        verify(navigator).openUpgrade(context());
        assertThat(eventBus.eventsOn(EventQueue.TRACKING).get(0).getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
    }

    @Test
    public void onRestoreSubscriptionClickedUpdatesConfiguration() {
        initFragment();
        listenerArgumentCaptor.getValue().onRestoreSubscriptionClicked(new View(context()));

        verify(configurationManager).forceConfigurationUpdate();
        verify(moreView).disableRestoreSubscription();
    }

    @Test
    public void onBugReportClickedShowsReportDialog() {
        initFragment();
        listenerArgumentCaptor.getValue().onReportBugClicked(new View(context()));

        verify(bugReporter).showGeneralFeedbackDialog(context());
    }

    @Test
    public void onHelpCenterClickedShowsHelpCenter() {
        initFragment();
        listenerArgumentCaptor.getValue().onHelpCenterClicked(new View(context()));

        verify(navigator).openHelpCenter(context());
    }

    @Test
    public void onLegalClickedShowsLegal() {
        initFragment();
        listenerArgumentCaptor.getValue().onLegalClicked(new View(context()));

        verify(navigator).openLegal(context());
    }

    @Test
    public void unbindsHeaderViewInOnDestroyView() {
        final PublishSubject<User> subject = PublishSubject.create();
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(subject);

        initFragment();
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
        initFragment();

        presenter.resetScroll();

        verify(moreView).resetScroll();
    }

    @Test
    public void configureSubsSettingsForFreeUserWithNoUpsell() {
        initFragment();

        verify(moreView, never()).showRestoreSubscription();
        verify(moreView, never()).setSubscriptionTier(anyString());
        verify(moreView, never()).showHighTierUpsell(anyString());
        verify(moreView, never()).showOfflineSettings();
    }

    @Test
    public void configureSubsSettingsForFreeUserWithHighTierUpsell() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        initFragment();

        verify(moreView).showRestoreSubscription();
        verify(moreView).setSubscriptionTier(resources().getString(R.string.tier_free));
        verify(moreView).showHighTierUpsell(resources().getString(R.string.more_upsell));
        verify(moreView, never()).showOfflineSettings();
    }

    @Test
    public void configureSubsSettingsForMidTierUser() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        initFragment();

        verify(moreView, never()).showRestoreSubscription();
        verify(moreView).setSubscriptionTier(resources().getString(R.string.tier_go));
        verify(moreView).showHighTierUpsell(resources().getString(R.string.more_upsell));
        verify(moreView).showOfflineSettings();
    }

    @Test
    public void configureSubsSettingsForHighTierUser() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        when(flags.isEnabled(Flag.MID_TIER)).thenReturn(true);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        initFragment();

        verify(moreView, never()).showRestoreSubscription();
        verify(moreView).setSubscriptionTier(resources().getString(R.string.tier_plus));
        verify(moreView, never()).showHighTierUpsell(anyString());
        verify(moreView).showOfflineSettings();
    }

    @Test
    public void usesLegacyHighTierLabelIfMidTierIsNotEnabled() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        when(flags.isEnabled(Flag.MID_TIER)).thenReturn(false);

        initFragment();

        verify(moreView).setSubscriptionTier(resources().getString(R.string.tier_go));
    }

    @Test
    public void usesLegacyUpsellLabelIfMidTierIsNotEnabled() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        when(flags.isEnabled(Flag.MID_TIER)).thenReturn(false);

        initFragment();

        verify(moreView).showHighTierUpsell(resources().getString(R.string.more_upsell_legacy));
    }

    private void initFragment() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);
    }

    private void verifyUserBound() {
        verify(moreView).setUsername(USER.username());
    }
}
