package com.soundcloud.android.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;
import android.content.Intent;

public class ShareOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123l);
    private static final String SCREEN_TAG = "screen_tag";
    private static final PropertySet TRACK = TestPropertySets.expectedTrackForPlayer();
    private static final PropertySet PRIVATE_TRACK = TestPropertySets.expectedPrivateTrackForPlayer();
    public static final PropertySet PLAYLIST = TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen();
    private static final Urn PRIVATE_TRACK_URN = Urn.forTrack(234l);

    @Mock TrackRepository trackRepository;

    private ShareOperations operations;
    private Activity activityContext;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        activityContext = new Activity();
        operations = new ShareOperations(eventBus, trackRepository);

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(TRACK));
        when(trackRepository.track(PRIVATE_TRACK_URN)).thenReturn(Observable.just(PRIVATE_TRACK));
    }

    @Test
    public void shareTrackStartsShareActivity() throws Exception {
        operations.shareTrack(activityContext, TRACK_URN, SCREEN_TAG);

        Assertions.assertThat(activityContext)
                .nextStartedIntent()
                .containsAction(Intent.ACTION_SEND)
                .containsExtra(Intent.EXTRA_SUBJECT, "dubstep anthem - SoundCloud")
                .containsExtra(Intent.EXTRA_TEXT, "Listen to dubstep anthem by squirlex #np on #SoundCloud\\nhttp://permalink.url");
    }

    @Test
    public void sharePlayableStartsShareActivity() throws Exception {
        operations.share(activityContext, PLAYLIST, SCREEN_TAG);

        Assertions.assertThat(activityContext)
                .nextStartedIntent()
                .containsAction(Intent.ACTION_SEND)
                .containsExtra(Intent.EXTRA_SUBJECT, "squirlex galore - SoundCloud")
                .containsExtra(Intent.EXTRA_TEXT, "Listen to squirlex galore by avieciie #np on #SoundCloud\\nhttp://permalink.url");
    }


    @Test
    public void shareTrackPublishesTrackingEvent() throws Exception {
        operations.shareTrack(activityContext, TRACK_URN, SCREEN_TAG);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_SHARE);
        assertThat(uiEvent.getAttributes().get("context")).isEqualTo(SCREEN_TAG);
    }

    @Test
    public void shouldNotSharePrivateTracks() throws Exception {
        operations.shareTrack(activityContext, PRIVATE_TRACK_URN, SCREEN_TAG);

        Assertions.assertThat(activityContext).hasNoNextStartedIntent();
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

}