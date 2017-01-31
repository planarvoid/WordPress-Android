package com.soundcloud.android.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.firebase.FirebaseDynamicLinksApi;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;
import android.content.Intent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class ShareOperationsTest extends AndroidUnitTest {

    private static final String SCREEN_TAG = "screen_tag";
    private static final String PAGE_NAME = "page_name";
    private static final Urn PAGE_URN = Urn.forPlaylist(234L);
    private static final TrackItem TRACK = TestPropertySets.expectedTrackForPlayer();
    private static final TrackItem PRIVATE_TRACK = TestPropertySets.expectedPrivateTrackForPlayer();
    private static final PlaylistItem PLAYLIST = TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen();
    private static final PromotedTrackItem PROMOTED_TRACK = TestPropertySets.expectedPromotedTrack();
    private static final PromotedSourceInfo PROMOTED_SOURCE_INFO = PromotedSourceInfo.fromItem(PROMOTED_TRACK);

    private ShareOperations operations;
    private Activity activityContext;
    private TestEventBus eventBus = new TestEventBus();
    @Mock private FeatureFlags features;
    @Mock private EventTracker tracker;
    @Mock private FirebaseDynamicLinksApi firebaseApi;
    @Captor ArgumentCaptor<UIEvent> uiEventCaptor;

    @Before
    public void setUp() {
        activityContext = activity();
        operations = new ShareOperations(features, tracker, firebaseApi);
    }

    @Test
    public void sharePlayableStartsShareActivity() throws Exception {
        operations.share(activityContext, PLAYLIST, eventContext(), PROMOTED_SOURCE_INFO);
        assertShareActivityStarted(PLAYLIST, "http://permalink.url");
    }

    @Test
    public void shareTrackPublishesTrackingEvent() throws Exception {
        operations.share(activityContext, TRACK, eventContext(), null);

        verify(tracker).trackEngagement(uiEventCaptor.capture());

        final UIEvent uiEvent = uiEventCaptor.getValue();

        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.SHARE);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN_TAG);
        assertThat(uiEvent.originScreen().get()).isEqualTo(PAGE_NAME);
        assertThat(uiEvent.clickObjectUrn().get().toString()).isEqualTo("soundcloud:tracks:123");
    }

    @Test
    public void sharePromotedTrackPublishesTrackingEvent() throws Exception {
        operations.share(activityContext, PROMOTED_TRACK, eventContext(), PROMOTED_SOURCE_INFO);

        verify(tracker).trackEngagement(uiEventCaptor.capture());

        final UIEvent uiEvent = uiEventCaptor.getValue();

        assertThat(uiEvent.kind()).isSameAs(UIEvent.Kind.SHARE);
        assertThat(uiEvent.contextScreen().get()).isEqualTo(SCREEN_TAG);
        assertThat(uiEvent.originScreen().get()).isEqualTo(PAGE_NAME);
        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get().toString()).isEqualTo("soundcloud:tracks:12345");
        assertThat(uiEvent.adUrn().get()).isEqualTo("ad:urn:123");
        assertThat(uiEvent.monetizationType().get()).isEqualTo(UIEvent.MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get().toString()).isEqualTo("soundcloud:users:193");
    }

    @Test
    public void shouldNotSharePrivateTracks() throws Exception {
        operations.share(activityContext, PRIVATE_TRACK, eventContext(), PROMOTED_SOURCE_INFO);

        Assertions.assertThat(activityContext).hasNoNextStartedIntent();
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void shareStartsShareActivityWithDynamicLinkWhenFeatureEnabled() throws Exception {
        when(features.isEnabled(Flag.DYNAMIC_LINKS)).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.just("http://goo.gl/foo"));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareActivityStarted(TRACK, "http://goo.gl/foo?/somepath");
    }

    @Test
    public void shareStartsShareActivityWithNonDynamicLinkWhenFeatureDisabled() throws Exception {
        when(features.isEnabled(Flag.DYNAMIC_LINKS)).thenReturn(false);
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareActivityStarted(TRACK, "http://foo.com/somepath");
    }

    @Test
    public void shareStartsShareActivityWithNonDynamicLinkWhenLookupFailsDueToNetworkIssue() throws Exception {
        when(features.isEnabled(Flag.DYNAMIC_LINKS)).thenReturn(true);
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.error(new IOException()));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertShareActivityStarted(TRACK, "http://foo.com/somepath");
    }

    @Test
    public void shareCrashesAppWhenLookupFailsDueToAppBug() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((t, throwable) -> error.set(throwable));
        when(features.isEnabled(Flag.DYNAMIC_LINKS)).thenReturn(true);
        NullPointerException expectedException = new NullPointerException();
        when(firebaseApi.createDynamicLink("http://foo.com/somepath")).thenReturn(Observable.error(expectedException));
        operations.share(activityContext, "http://foo.com/somepath", eventContext(), PROMOTED_SOURCE_INFO, EntityMetadata.from(TRACK));
        assertThat(error.get()).isNotNull();
        assertThat(getRootCause(error.get())).isSameAs(expectedException);
    }

    private Throwable getRootCause(Throwable throwable) {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    private EventContextMetadata eventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(SCREEN_TAG)
                                   .pageName(PAGE_NAME)
                                   .pageUrn(PAGE_URN).build();
    }

    private void assertShareActivityStarted(PlayableItem playable, String url) {
        Assertions.assertThat(activityContext)
                  .nextStartedIntent()
                  .containsAction(Intent.ACTION_CHOOSER)
                  .wrappedIntent()
                  .containsAction(Intent.ACTION_SEND)
                  .containsExtra(Intent.EXTRA_SUBJECT, playable.getTitle() + " - SoundCloud")
                  .containsExtra(Intent.EXTRA_TEXT,
                                 "Listen to " + playable.getTitle() + " by " + playable.getCreatorName() + " #np on #SoundCloud\n" + url);
    }
}
