package com.soundcloud.android.tracks;

import static com.soundcloud.android.api.model.ChartType.TOP;
import static com.soundcloud.android.api.model.ChartType.TRENDING;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.discovery.charts.ChartTrackItem;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.util.Locale;

public class TrackItemRendererTest extends AndroidUnitTest {

    private final static ChartCategory CATEGORY = ChartCategory.MUSIC;
    private final static Urn GENRE_URN = new Urn("soundcloud:genre:rock");
    private final static Optional<Urn> QUERY_URN = Optional.of(new Urn("soundcloud:charts:1236"));

    private TrackItemRenderer renderer;

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;
    @Mock private Navigator navigator;
    @Mock private View itemView;
    @Mock private ImageView imageView;
    @Mock private TrackItemView trackItemView;
    @Mock private TrackItemView.Factory trackItemViewFactory;

    private PropertySet propertySet;
    private TrackItem trackItem;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());

    @Before
    public void setUp() throws Exception {
        renderer = new TrackItemRenderer(imageOperations, numberFormatter, null, eventBus,
                                         screenProvider, navigator, featureOperations, trackItemViewFactory);

        propertySet = PropertySet.from(
                TrackProperty.TITLE.bind("title"),
                TrackProperty.CREATOR_NAME.bind("creator"),
                TrackProperty.SNIPPET_DURATION.bind(227000L),
                TrackProperty.FULL_DURATION.bind(237000L),
                TrackProperty.SNIPPED.bind(true),
                TrackProperty.URN.bind(Urn.forTrack(123)),
                TrackProperty.PLAY_COUNT.bind(870)
        );
        trackItem = TrackItem.from(propertySet);

        when(trackItemViewFactory.getPrimaryTitleColor()).thenReturn(R.color.list_primary);
        when(trackItemView.getImage()).thenReturn(imageView);
        when(trackItemView.getContext()).thenReturn(context());
        when(trackItemView.getResources()).thenReturn(resources());
        when(imageView.getContext()).thenReturn(context());
        when(itemView.getResources()).thenReturn(resources());
        when(itemView.getTag()).thenReturn(trackItemView);
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).setTitle("title", R.color.list_primary);
    }

    @Test
    public void shouldBindDurationToViewAndHideOtherLabelsIfTrackIsNeitherSnippedNorPrivate() {
        trackItem = TrackItem.from(propertySet.put(TrackProperty.SNIPPED, false).put(TrackProperty.IS_PRIVATE, false));
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).hideInfoViewsRight();
        verify(trackItemView).showDuration("3:57");
    }

    @Test
    public void shouldShowGoLabelIfTrackIsHighTierPreview() {
        trackItem = TestPropertySets.upsellableTrack();
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).hideInfoViewsRight();
        verify(trackItemView).showPreviewLabel();
        verify(trackItemView, never()).showDuration(anyString());
        verify(trackItemView).showGoLabel();
    }

    @Test
    public void shouldShowGoLabelIfTrackIsFullHighTierTrack() {
        trackItem = TestPropertySets.highTierTrack();
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showGoLabel();
    }

    @Test
    public void shouldShowGoLabelForNowPlayingTrack() {
        trackItem = TestPropertySets.highTierTrack();
        trackItem.setIsPlaying(true);

        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showNowPlaying();
        verify(trackItemView).showGoLabel();
    }

    @Test
    public void shouldShowPrivateLabelAndHideOtherLabelsIfTrackIsPrivate() {
        propertySet.put(TrackProperty.IS_PRIVATE, true);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).hideInfoViewsRight();
        verify(trackItemView).showPrivateIndicator();
    }

    @Test
    public void shouldBindCreatorToView() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).setCreator("creator");
    }

    @Test
    public void shouldBindPlayCountToView() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showPlaycount(numberFormatter.format(870));
    }

    @Test
    public void shouldHighlightCurrentlyPlayingTrack() {
        trackItem.setIsPlaying(true);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showNowPlaying();
    }

    @Test
    public void shouldShowTrackGeoBlockedLabel() {
        propertySet.put(TrackProperty.BLOCKED, true);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showGeoBlocked();
    }

    @Test
    public void blockedStateShouldTakePrecedenceOverOtherAdditionalStates() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        trackItem.setIsPlaying(true);
        propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.UNAVAILABLE);

        propertySet.put(TrackProperty.BLOCKED, true);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showGeoBlocked();
    }

    @Test
    public void shouldLoadTrackArtwork() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));
        verify(imageOperations).displayInAdapterView(
                trackItem,
                ApiImageSize.getListItemImageSize(itemView.getResources()),
                imageView);
    }

    @Test
    public void shouldShowPromotedIndicator() {
        TrackItem promotedTrackItem = TestPropertySets.expectedPromotedTrackWithoutPromoter();
        renderer.bindItemView(0, itemView, singletonList(promotedTrackItem));

        verify(trackItemView).showPromotedTrack("Promoted");
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        TrackItem promotedTrackItem = TestPropertySets.expectedPromotedTrack();
        renderer.bindItemView(0, itemView, singletonList(promotedTrackItem));

        verify(trackItemView).showPromotedTrack("Promoted by SoundCloud");
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        when(itemView.getContext()).thenReturn(context());
        PromotedListItem promotedListItem = TestPropertySets.expectedPromotedTrack();
        renderer.bindItemView(0, itemView, singletonList((TrackItem) promotedListItem));

        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(trackItemView).setPromotedClickable(captor.capture());
        captor.getValue().onClick(itemView);

        verify(navigator).legacyOpenProfile(context(), Urn.forUser(193L));
        verify(eventBus).publish(eq(EventQueue.TRACKING), any(PromotedTrackingEvent.class));
    }

    @Test
    public void shouldDisableClicksForBlockedTracks() {
        propertySet.put(TrackProperty.BLOCKED, true);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(itemView).setClickable(false);
    }

    @Test
    public void shouldShowTrackPositionAndPostedTimeForTrendingChartTrackItem() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final int position = 0;
        final ChartTrackItem chartTrackItem = new ChartTrackItem(TRENDING, apiTrack, CATEGORY,
                                                            GENRE_URN, QUERY_URN);
        renderer.bindChartTrackView(chartTrackItem, itemView, position, Optional.absent());

        verify(trackItemView).showPosition(position);
        verify(trackItemView).showPostedTime(apiTrack.getCreatedAt());
    }

    @Test
    public void shouldShowTrackPositionButNotPostedTimeForTopChartTrackItem() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final int position = 0;
        final ChartTrackItem chartTrackItem = new ChartTrackItem(TOP, apiTrack, CATEGORY,
                                                            GENRE_URN, QUERY_URN);
        renderer.bindChartTrackView(chartTrackItem, itemView, position, Optional.absent());

        verify(trackItemView).showPosition(position);
        verify(trackItemView, never()).showPostedTime(apiTrack.getCreatedAt());
        verify(trackItemView).showPlaycount(anyString());
    }

}
