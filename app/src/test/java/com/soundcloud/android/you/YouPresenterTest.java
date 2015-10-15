package com.soundcloud.android.you;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.same;
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
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
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
    @Mock private OfflineSettingsOperations offlineSettingsOperations;
    @Mock private Navigator navigator;

    @Captor private ArgumentCaptor<YouView.Listener> listenerArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        presenter = new YouPresenter(youViewFactory, userRepository, accountOperations, imageOperations, resources(),
                eventBus, featureOperations, offlineSettingsOperations, navigator);
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
        final PublishSubject<PropertySet> subject = PublishSubject.<PropertySet>create();
        when(userRepository.localAndSyncedUserInfo(USER_URN)).thenReturn(subject);

        setupForegroundFragment();

        subject.onNext(USER);

        verifyUserBound();
    }

    @Test
    public void onViewCreatedSendsUpsellImpressionIfUpselling() {
        when(featureOperations.upsellMidTier()).thenReturn(true);

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
        when(featureOperations.upsellMidTier()).thenReturn(true);

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
        when(offlineSettingsOperations.hasOfflineContent()).thenReturn(true);

        setupForegroundFragment();

        verify(youView).showOfflineSettings();
    }

    @Test
    public void onOfflineSettingsClickSendsUpsellClickEventIfUpselling() {
        when(featureOperations.upsellMidTier()).thenReturn(true);

        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(trackingEvents).hasSize(2);
        assertThat(trackingEvents.get(1).getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_CLICK);
    }

    @Test
    public void OnOfflineSettingsClickSendsNoUpsellClickEventIfNotUpselling() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void OnActivitiesClickedNavigatesToActivities() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onActivitiesClicked(new View(context()));

        verify(navigator).openActivities(context());
    }

    @Test
    public void OnRecordClickedNavigatesToRecord() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onRecordClicked(new View(context()));

        verify(navigator).openRecord(context(), Screen.YOU);
    }

    @Test
    public void OnProfileClickedNavigatesToProfile() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onProfileClicked(new View(context()));

        verify(navigator).openProfile(context(), USER_URN);
    }

    @Test
    public void OnOfflineSettingsClickedShowsOfflineSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onOfflineSettingsClicked(new View(context()));

        verify(navigator).openOfflineSettings(context());
    }

    @Test
    public void OnNotificationSettingsClickedShowsNotificationSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onNotificationSettingsClicked(new View(context()));

        verify(navigator).openNotificationSettings(context());
    }

    @Test
    public void OnBasicSettingsClickedShowsSettings() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onBasicSettingsClicked(new View(context()));

        verify(navigator).openSettings(context());
    }

    @Test
    public void OnHelpCenterClickedShowsHelpCenter() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onHelpCenterClicked(new View(context()));

        verify(navigator).openHelpCenter(context());
    }

    @Test
    public void OnLegalClickedShowsLegal() {
        setupForegroundFragment();
        listenerArgumentCaptor.getValue().onLegalClicked(new View(context()));

        verify(navigator).openLegal(context());
    }

    @Test
    public void unbindsHeaderViewInOnDestroyView() {
        final PublishSubject<PropertySet> subject = PublishSubject.<PropertySet>create();
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
