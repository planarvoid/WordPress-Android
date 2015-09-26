package com.soundcloud.android.analytics.appboy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appboy.AppboyUser;
import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.eventlogger.EventLoggerJsonDataBuilder;
import com.soundcloud.android.analytics.eventlogger.EventLoggerV1JsonDataBuilder;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.app.Activity;
import android.content.SharedPreferences;

public class AppboyAnalyticsProviderTest extends AndroidUnitTest {

    private AppboyAnalyticsProvider appboyAnalyticsProvider;

    @Mock private AppboyWrapper appboy;
    @Mock private Activity activity;
    @Mock private AccountOperations accountOperations;
    @Mock private EventLoggerJsonDataBuilder dataBuilderv0;
    @Mock private EventLoggerV1JsonDataBuilder dataBuilderv1;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private FeatureFlags featureFlags;
    @Mock private AppboyUser appboyUser;

    private Urn userUrn = Urn.forUser(123L);
    private Urn otherUserUrn = Urn.forUser(234L);

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        when(appboy.getCurrentUser()).thenReturn(appboyUser);
        when(appboyUser.getUserId()).thenReturn(userUrn.toString());

        appboyAnalyticsProvider = new AppboyAnalyticsProvider(appboy, accountOperations);
    }

    @Test
    public void shouldChangeUserIdWhenUserChangedOnConstructed() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(otherUserUrn);

        appboyAnalyticsProvider = new AppboyAnalyticsProvider(appboy, accountOperations);

        verify(appboy).changeUser(otherUserUrn.toString());
    }

    @Test
    public void shouldNotChangeUserIdWhenUserLoggedOutOnConstructed() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(AccountOperations.ANONYMOUS_USER_URN);

        appboyAnalyticsProvider = new AppboyAnalyticsProvider(appboy, accountOperations);

        verify(appboy, never()).changeUser(AccountOperations.ANONYMOUS_USER_URN.toString());
    }

    @Test
    public void shouldForwardFlushCallToAppboy() {
        appboyAnalyticsProvider.flush();
        verify(appboy).requestImmediateDataFlush();
    }

    @Test
    public void shouldHandleStartLifeCycleEvents() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnStart(activity);

        appboyAnalyticsProvider.handleActivityLifeCycleEvent(event);

        verify(appboy).openSession(activity);
    }

    @Test
    public void shouldHandleStopLifeCycleEvents() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnStop(activity);

        appboyAnalyticsProvider.handleActivityLifeCycleEvent(event);

        verify(appboy).closeSession(activity);
    }

    @Test
    public void shouldHandleUserChangeEvents() {
        PublicApiUser user = ModelFixtures.create(PublicApiUser.class);
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(user);

        appboyAnalyticsProvider.handleCurrentUserChangedEvent(event);

        verify(appboy).changeUser(user.getUrn().toString());
    }

    @Test
    public void shouldTrackLikeEvents() {
        PlayableItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        UIEvent event = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", "page_name",
                Urn.forTrack(123), Urn.NOT_SET, null, promotedTrack);

        AppboyProperties expectedProperties = new AppboyProperties()
                .addProperty("creator_display_name", promotedTrack.getCreatorName())
                .addProperty("creator_urn", promotedTrack.getCreatorUrn().toString())
                .addProperty("playable_title", promotedTrack.getTitle())
                .addProperty("playable_urn", promotedTrack.getEntityUrn().toString())
                .addProperty("playable_type", "track");

        appboyAnalyticsProvider.handleTrackingEvent(event);

        expectCustomEvent("like", expectedProperties);
    }

    @Test
    public void shouldNotTrackUnLikeEvents() {
        PlayableItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        UIEvent event = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", "page_name",
                Urn.forTrack(123), Urn.NOT_SET, null, promotedTrack);

        appboyAnalyticsProvider.handleTrackingEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    private void expectCustomEvent(String eventName, AppboyProperties expectedProperties) {
        ArgumentCaptor<AppboyProperties> captor = ArgumentCaptor.forClass(AppboyProperties.class);

        verify(appboy).logCustomEvent(eq(eventName), captor.capture());

        String generatedJson = captor.getValue().forJsonPut().toString();
        String expectedJson = expectedProperties.forJsonPut().toString();

        assertThat(generatedJson).isEqualTo(expectedJson);
    }

}