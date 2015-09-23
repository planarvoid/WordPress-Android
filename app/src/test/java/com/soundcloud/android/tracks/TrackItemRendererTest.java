package com.soundcloud.android.tracks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.util.Arrays;

public class TrackItemRendererTest extends AndroidUnitTest {

    @InjectMocks
    private TrackItemRenderer renderer;

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;
    @Mock private Navigator navigator;
    @Mock private View itemView;
    @Mock private TrackItemView trackItemView;
    @Mock private ImageView imageView;

    private PropertySet propertySet;
    private TrackItem trackItem;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                TrackProperty.TITLE.bind("title"),
                TrackProperty.CREATOR_NAME.bind("creator"),
                TrackProperty.DURATION.bind(227000L),
                TrackProperty.URN.bind(Urn.forTrack(123)),
                TrackProperty.PLAY_COUNT.bind(870)
        );
        trackItem = TrackItem.from(propertySet);

        when(trackItemView.getImage()).thenReturn(imageView);
        when(trackItemView.getContext()).thenReturn(context());
        when(imageView.getContext()).thenReturn(context());
        when(itemView.getContext()).thenReturn(context());
        when(itemView.getTag()).thenReturn(trackItemView);
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).setTitle("title");
    }

    @Test
    public void shouldBindDurationToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).setDuration("3:47");

    }

    @Test
    public void shouldBindCreatorToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).setCreator("creator");
    }

    @Test
    public void shouldBindPlayCountToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).showPlaycount(870);
    }

    @Test
    public void shouldBindReposterIfAny() {
        propertySet.put(TrackProperty.REPOSTER, "reposter");
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).showReposter("reposter");
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).hideReposter();
    }

    @Test
    public void shouldShowPrivateIndicatorIfTrackIsPrivate() {
        propertySet.put(TrackProperty.IS_PRIVATE, true);
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).showPrivateIndicator();
    }

    @Test
    public void shouldHighlightCurrentlyPlayingTrack() {
        renderer.setPlayingTrack(Urn.forTrack(123));
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));

        verify(trackItemView).showNowPlaying();
    }

    @Test
    public void shouldLoadTrackArtwork() {
        renderer.bindItemView(0, itemView, Arrays.asList(trackItem));
        verify(imageOperations).displayInAdapterView(
                Urn.forTrack(123),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                imageView);
    }

    @Test
    public void shouldShowPromotedIndicator() {
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        renderer.bindItemView(0, itemView, Arrays.asList(promotedTrackItem));

        verify(trackItemView).showPromotedTrack("Promoted");
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        renderer.bindItemView(0, itemView, Arrays.asList(promotedTrackItem));

        verify(trackItemView).showPromotedTrack("Promoted by SoundCloud");
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        PromotedListItem promotedListItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        renderer.bindItemView(0, itemView, Arrays.asList((TrackItem) promotedListItem));

        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(trackItemView).setPromotedClickable(captor.capture());
        captor.getValue().onClick(itemView);

        verify(navigator).openProfile(any(Context.class), eq(Urn.forUser(193L)));
        verify(eventBus).publish(eq(EventQueue.TRACKING), any(PromotedTrackingEvent.class));
    }
}
