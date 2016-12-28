package com.soundcloud.android.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.app.Activity;
import android.content.Intent;

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
    @Mock private EventTracker tracker;
    @Captor ArgumentCaptor<UIEvent> uiEventCaptor;

    @Before
    public void setUp() {
        activityContext = activity();
        operations = new ShareOperations(tracker);
    }

    @Test
    public void sharePlayableStartsShareActivity() throws Exception {
        operations.share(activityContext, PLAYLIST, eventContext(), PROMOTED_SOURCE_INFO);

        Assertions.assertThat(activityContext)
                  .nextStartedIntent()
                  .containsAction(Intent.ACTION_CHOOSER)
                  .wrappedIntent()
                  .containsAction(Intent.ACTION_SEND)
                  .containsExtra(Intent.EXTRA_SUBJECT, "squirlex galore - SoundCloud")
                  .containsExtra(Intent.EXTRA_TEXT,
                                 "Listen to squirlex galore by avieciie #np on #SoundCloud\nhttp://permalink.url");
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

    private EventContextMetadata eventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(SCREEN_TAG)
                                   .pageName(PAGE_NAME)
                                   .pageUrn(PAGE_URN).build();
    }

}
