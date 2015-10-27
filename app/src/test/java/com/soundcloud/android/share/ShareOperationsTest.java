package com.soundcloud.android.share;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;

import android.app.Activity;
import android.content.Intent;

import java.util.Map;

public class ShareOperationsTest extends AndroidUnitTest {

    private static final String SCREEN_TAG = "screen_tag";
    private static final String PAGE_NAME = "page_name";
    private static final Urn PAGE_URN = Urn.forPlaylist(234l);
    private static final PropertySet TRACK = TestPropertySets.expectedTrackForPlayer();
    private static final PropertySet PRIVATE_TRACK = TestPropertySets.expectedPrivateTrackForPlayer();
    private static final PropertySet PLAYLIST = TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen();
    private static final PropertySet PROMOTED_TRACK = TestPropertySets.expectedPromotedTrack();
    private static final PromotedSourceInfo PROMOTED_SOURCE_INFO = PromotedSourceInfo.fromItem(PromotedTrackItem.from(PROMOTED_TRACK));

    private ShareOperations operations;
    private Activity activityContext;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        activityContext = new Activity();
        operations = new ShareOperations(eventBus);
    }

    @Test
    public void sharePlayableStartsShareActivity() throws Exception {
        operations.share(activityContext, PLAYLIST, SCREEN_TAG, PAGE_NAME, PAGE_URN, PROMOTED_SOURCE_INFO);

        Assertions.assertThat(activityContext)
                .nextStartedIntent()
                .containsAction(Intent.ACTION_SEND)
                .containsExtra(Intent.EXTRA_SUBJECT, "squirlex galore - SoundCloud")
                .containsExtra(Intent.EXTRA_TEXT, "Listen to squirlex galore by avieciie #np on #SoundCloud\\nhttp://permalink.url");
    }

    @Test
    public void shareTrackPublishesTrackingEvent() throws Exception {
        operations.share(activityContext, TRACK, SCREEN_TAG, PAGE_NAME, PAGE_URN, null);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        Map<String, String> attributes = uiEvent.getAttributes();

        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_SHARE);
        assertThat(attributes.get("context")).isEqualTo(SCREEN_TAG);
        assertThat(attributes.get("origin_screen")).isEqualTo(PAGE_NAME);
        assertThat(attributes.get("click_object_urn")).isEqualTo("soundcloud:tracks:123");
    }

    @Test
    public void sharePromotedTrackPublishesTrackingEvent() throws Exception {
        operations.share(activityContext, PROMOTED_TRACK, SCREEN_TAG, PAGE_NAME, PAGE_URN, PROMOTED_SOURCE_INFO);

        TrackingEvent uiEvent = eventBus.firstEventOn(EventQueue.TRACKING);
        Map<String, String> attributes = uiEvent.getAttributes();

        assertThat(uiEvent.getKind()).isSameAs(UIEvent.KIND_SHARE);
        assertThat(attributes.get("context")).isEqualTo(SCREEN_TAG);
        assertThat(attributes.get("origin_screen")).isEqualTo(PAGE_NAME);
        assertThat(attributes.get("page_urn")).isEqualTo(PAGE_URN.toString());
        assertThat(attributes.get("click_object_urn")).isEqualTo("soundcloud:tracks:12345");
        assertThat(attributes.get("ad_urn")).isEqualTo("ad:urn:123");
        assertThat(attributes.get("monetization_type")).isEqualTo("promoted");
        assertThat(attributes.get("promoter_urn")).isEqualTo("soundcloud:users:193");
    }

    @Test
    public void shouldNotSharePrivateTracks() throws Exception {
        operations.share(activityContext, PRIVATE_TRACK, SCREEN_TAG, PAGE_NAME, PAGE_URN, PROMOTED_SOURCE_INFO);

        Assertions.assertThat(activityContext).hasNoNextStartedIntent();
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

}