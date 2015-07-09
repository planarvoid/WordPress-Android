package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

public class IntentResolverTest extends AndroidUnitTest {
    @Mock private ResolveOperations resolveOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private ReferrerResolver referrerResolver;
    @Mock private EventBus eventBus;
    @Mock private Navigator navigator;

    @InjectMocks private IntentResolver resolver;

    private Uri uri;
    private PublicApiResource resource;
    private Intent intent;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = context();
        setupIntent("soundcloud://playlists:1");
        setupResource(PublicApiPlaylist.class);
        setupReferrer(Referrer.OTHER);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(playbackOperations.startPlayback(any(PublicApiTrack.class), any(Screen.class), any(boolean.class))).thenReturn(Observable.<PlaybackResult>empty());
    }

    @Test
    public void shouldPlayTrack() throws CreateModelException {
        setupResource(PublicApiTrack.class);

        resolver.handleIntent(intent, context);

        verify(playbackOperations).startPlayback((PublicApiTrack) resource, Screen.DEEPLINK, PlaybackOperations.WITH_RELATED);
    }

    @Test
    public void shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        setupResource(PublicApiTrack.class);

        resolver.handleIntent(intent, context);

        verify(navigator).openStreamWithExpandedPlayer(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldGotoPlaylistDetails() throws CreateModelException {
        setupResource(PublicApiPlaylist.class);

        resolver.handleIntent(intent, context);

        verify(navigator).openPlaylist(context, resource.getUrn(), Screen.DEEPLINK);
    }

    @Test
    public void shouldGotoUserProfile() throws CreateModelException {
        setupResource(PublicApiUser.class);

        resolver.handleIntent(intent, context);

        verify(navigator).openProfile(context, resource.getUrn(), Screen.DEEPLINK);
    }

    @Test
    public void shouldTrackForegroundEventsWithResources() throws Exception {
        setupReferrer(Referrer.TWITTER);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(resource.getUrn(), Referrer.TWITTER);
    }

    @Test
    public void shouldTrackForegroundEventsWithResourceWhenUserIsNotLoggedIn() throws Exception {
        setupReferrer(Referrer.FACEBOOK);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(resource.getUrn(), Referrer.FACEBOOK);
    }

    @Test
    public void shouldTrackForegroundEventsWithoutResourcesForHome() throws Exception {
        intent = new Intent();

        resolver.handleIntent(intent, context);

        verify(navigator).openStream(context, Screen.DEEPLINK);
        verifyTrackingEvent(Referrer.OTHER);
    }

    @Test
    public void shouldLoginCrawler() throws Exception {
        setupResource(PublicApiPlaylist.class);
        setupReferrer(Referrer.GOOGLE_CRAWLER);

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verify(navigator).openPlaylist(context, resource.getUrn(), Screen.DEEPLINK);
    }

    @Test
    public void shouldStartPlaybackForTracksOnCrawlersWithoutRelated() throws Exception {
        setupResource(PublicApiTrack.class);
        setupReferrer(Referrer.GOOGLE_CRAWLER);

        resolver.handleIntent(intent, context);

        verify(playbackOperations).startPlayback((PublicApiTrack) resource, Screen.DEEPLINK, PlaybackOperations.WITHOUT_RELATED);
    }

    @Test
    public void shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(resource.getUrn(), Referrer.OTHER);
        verify(navigator).openOnboarding(context, resource.getUrn(), Screen.DEEPLINK);
    }

    public void setupIntent(String url) {
        uri = Uri.parse(url);
        intent = new Intent().setData(uri);
    }

    private void setupResource(Class resourceClass) {
        resource = (PublicApiResource) ModelFixtures.create(resourceClass);
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(resource));
    }

    private void setupReferrer(Referrer referrer) {
        when(referrerResolver.getReferrerFromIntent(any(Intent.class), any(Resources.class))).thenReturn(referrer);
    }

    private ForegroundEvent captureForegroundEvent() {
        ArgumentCaptor<ForegroundEvent> captor = ArgumentCaptor.forClass(ForegroundEvent.class);
        verify(eventBus).publish(eq(EventQueue.TRACKING), captor.capture());
        return captor.getValue();
    }

    private void verifyTrackingEvent(Urn urn, Referrer referrer) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.getKind()).isEqualTo(ForegroundEvent.KIND_OPEN);
        assertThat(event.get(ForegroundEvent.KEY_REFERRER)).isEqualTo(referrer.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_NAME)).isEqualTo(Screen.DEEPLINK.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_URN)).isEqualTo(urn.toString());
    }

    private void verifyTrackingEvent(Referrer referrer) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.getKind()).isEqualTo(ForegroundEvent.KIND_OPEN);
        assertThat(event.get(ForegroundEvent.KEY_REFERRER)).isEqualTo(referrer.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_NAME)).isEqualTo(Screen.DEEPLINK.get());
    }

}
