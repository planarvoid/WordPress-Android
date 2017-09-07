package com.soundcloud.android.tracks;

import static com.soundcloud.android.api.model.ChartType.TOP;
import static com.soundcloud.android.api.model.ChartType.TRENDING;
import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.GoOnboardingTooltipEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlay;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.olddiscovery.charts.ChartTrackItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;
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
    @Mock private View goIndicator;
    @Mock private ImageView imageView;
    @Mock private TrackItemView trackItemView;
    @Mock private TrackItemView.Factory trackItemViewFactory;
    @Mock private OfflineSettingsOperations offlineSettingsOperations;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;
    @Mock private IntroductoryOverlayPresenter introductoryOverlayPresenter;
    @Mock private TrackStatsDisplayPolicy trackStatsDisplayPolicy;

    private TrackItem trackItem;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private Track.Builder trackBuilder;

    @Before
    public void setUp() throws Exception {
        renderer = new TrackItemRenderer(imageOperations,
                                         numberFormatter,
                                         null,
                                         eventBus,
                                         screenProvider,
                                         navigator,
                                         featureOperations,
                                         trackItemViewFactory,
                                         offlineSettingsOperations,
                                         connectionHelper,
                                         goOnboardingTooltipExperiment,
                                         lazyOf(introductoryOverlayPresenter),
                                         trackStatsDisplayPolicy);

        trackBuilder = ModelFixtures.baseTrackBuilder()
                                    .urn(Urn.forTrack(123))
                                    .title("title")
                                    .creatorName("creator")
                                    .creatorUrn(Urn.forUser(456L))
                                    .snippetDuration(227000L)
                                    .fullDuration(237000L)
                                    .snipped(true)
                                    .userLike(false)
                                    .userRepost(false)
                                    .likesCount(0)
                                    .permalinkUrl(Strings.EMPTY)
                                    .isPrivate(false)
                                    .displayStatsEnabled(true)
                                    .playCount(870);

        trackItem = ModelFixtures.trackItem(trackBuilder.build());

        when(trackItemViewFactory.getPrimaryTitleColor()).thenReturn(R.color.list_primary);
        when(trackItemView.getImage()).thenReturn(imageView);
        when(trackItemView.getContext()).thenReturn(context());
        when(trackItemView.getResources()).thenReturn(resources());
        when(trackItemView.getGoIndicator()).thenReturn(goIndicator);
        when(imageView.getContext()).thenReturn(context());
        when(itemView.getResources()).thenReturn(resources());
        when(itemView.getTag()).thenReturn(trackItemView);
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).setTitle("title", R.color.list_primary);
    }

    @Test
    public void shouldBindDurationToViewAndHideOtherLabelsIfTrackIsNeitherSnippedNorPrivate() {
        final TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.snipped(false).isPrivate(false).build());
        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(trackItemView).hideInfoViewsRight();
        verify(trackItemView).showDuration("3:57");
    }

    @Test
    public void shouldShowGoLabelIfTrackIsHighTierPreview() {
        trackItem = PlayableFixtures.upsellableTrack();
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).hideInfoViewsRight();
        verify(trackItemView).showPreviewLabel();
        verify(trackItemView, never()).showDuration(anyString());
        verify(trackItemView).showGoLabel();
    }

    @Test
    public void shouldShowGoLabelIfTrackIsFullHighTierTrack() {
        trackItem = PlayableFixtures.highTierTrack();
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showGoLabel();
    }

    @Test
    public void shouldShowGoLabelForNowPlayingTrack() {
        trackItem = PlayableFixtures.highTierTrack();
        trackItem = trackItem.withPlayingState(true);

        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showNowPlaying();
        verify(trackItemView).showGoLabel();
    }

    @Test
    public void shouldShowPrivateLabelAndHideOtherLabelsIfTrackIsPrivate() {
        final TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.isPrivate(true).build());
        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(trackItemView).hideInfoViewsRight();
        verify(trackItemView).showPrivateIndicator();
    }

    @Test
    public void shouldBindCreatorToView() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).setCreator("creator");
    }

    @Test
    public void shouldNotBindPlayCountToViewWhenPolicyReturnsFalse() {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(false);

        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView, never()).showPlaycount(anyString());
    }

    @Test
    public void shouldBindPlayCountToViewWhenPolicyReturnsTrue() {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);

        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showPlaycount(numberFormatter.format(870));
    }

    @Test
    public void shouldHighlightCurrentlyPlayingTrack() {
        trackItem = trackItem.withPlayingState(true);
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).showNowPlaying();
    }

    @Test
    public void shouldShowNotAvailableOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.offlineState(OfflineState.UNAVAILABLE).build());

        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(trackItemView).showNotAvailableOffline();
    }

    @Test
    public void shouldShowNoWifiForRequestedTrack() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(false);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.offlineState(OfflineState.REQUESTED).build());

        renderer.bindOfflineTrackView(updatedTrackItem, itemView, 0, Optional.absent(), Optional.absent());

        verify(trackItemView).showNoWifi();
    }

    @Test
    public void shouldShowNoConnectionForRequestedTrack() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(true);
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.offlineState(OfflineState.REQUESTED).build());

        renderer.bindOfflineTrackView(updatedTrackItem, itemView, 0, Optional.absent(), Optional.absent());

        verify(trackItemView).showNoConnection();
    }

    @Test
    public void shouldShowRequestedTrack() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsOperations.isWifiOnlyEnabled()).thenReturn(false);
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.offlineState(OfflineState.REQUESTED).build());

        renderer.bindOfflineTrackView(updatedTrackItem, itemView, 0, Optional.absent(), Optional.absent());

        verify(trackItemView).showRequested();
    }

    @Test
    public void shouldShowDownloadingTrack() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.offlineState(OfflineState.DOWNLOADING).build());

        renderer.bindOfflineTrackView(updatedTrackItem, itemView, 0, Optional.absent(), Optional.absent());

        verify(trackItemView).showDownloading();
    }

    @Test
    public void shouldShowDownloadedTrack() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.offlineState(OfflineState.DOWNLOADED).build());

        renderer.bindOfflineTrackView(updatedTrackItem, itemView, 0, Optional.absent(), Optional.absent());

        verify(trackItemView).showDownloaded();
    }

    @Test
    public void shouldShowTrackGeoBlockedLabel() {
        final TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.blocked(true).build());
        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(trackItemView).showGeoBlocked();
    }

    @Test
    public void blockedStateShouldTakePrecedenceOverOtherAdditionalStates() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.blocked(true).offlineState(OfflineState.UNAVAILABLE).build());
        updatedTrackItem = updatedTrackItem.withPlayingState(true);

        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(trackItemView).showGeoBlocked();
    }

    @Test
    public void shouldLoadTrackArtwork() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));
        verify(imageOperations).displayInAdapterView(trackItem.getUrn(),
                                                     trackItem.getImageUrlTemplate(),
                                                     ApiImageSize.getListItemImageSize(itemView.getResources()),
                                                     imageView,
                                                     ImageOperations.DisplayType.DEFAULT);
    }

    @Test
    public void shouldShowPromotedIndicator() {
        TrackItem promotedTrackItem = PlayableFixtures.expectedPromotedTrackWithoutPromoter();
        renderer.bindItemView(0, itemView, singletonList(promotedTrackItem));

        verify(trackItemView).showPromotedTrack("Promoted");
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        TrackItem promotedTrackItem = PlayableFixtures.expectedPromotedTrack();
        renderer.bindItemView(0, itemView, singletonList(promotedTrackItem));

        verify(trackItemView).showPromotedTrack("Promoted by SoundCloud");
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        AppCompatActivity activity = activity();
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        when(itemView.getContext()).thenReturn(activity);
        PlayableItem promotedListItem = PlayableFixtures.expectedPromotedTrack();
        renderer.bindItemView(0, itemView, singletonList((TrackItem) promotedListItem));

        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(trackItemView).setPromotedClickable(captor.capture());
        captor.getValue().onClick(itemView);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forProfile(Urn.forUser(193L)))));
        verify(eventBus).publish(eq(EventQueue.TRACKING), any(PromotedTrackingEvent.class));
    }

    @Test
    public void shouldDisableClicksForBlockedTracks() {
        final TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.blocked(true).build());
        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(itemView).setClickable(false);
    }

    @Test
    public void shouldEnableClicksForNonBlockedTracks() {
        final TrackItem updatedTrackItem = ModelFixtures.trackItem(trackBuilder.blocked(false).build());
        renderer.bindItemView(0, itemView, singletonList(updatedTrackItem));

        verify(itemView).setClickable(true);
    }

    @Test
    public void shouldShowTrackPositionAndPostedTimeForTrendingChartTrackItem() {
        final TrackItem trackItem = ModelFixtures.trackItem();
        final int position = 0;
        final ChartTrackItem chartTrackItem = new ChartTrackItem(TRENDING, trackItem, CATEGORY,
                                                                 GENRE_URN, QUERY_URN);
        renderer.bindChartTrackView(chartTrackItem, itemView, position, Optional.absent());

        verify(trackItemView).showPosition(position);
        verify(trackItemView).showPostedTime(trackItem.getCreatedAt());
    }

    @Test
    public void shouldShowTrackPositionButNotPostedTimeForTopChartTrackItem() {
        final TrackItem trackItem = ModelFixtures.trackItem();
        final int position = 0;
        final ChartTrackItem chartTrackItem = new ChartTrackItem(TOP, trackItem, CATEGORY,
                                                                 GENRE_URN, QUERY_URN);
        renderer.bindChartTrackView(chartTrackItem, itemView, position, Optional.absent());

        verify(trackItemView).showPosition(position);
        verify(trackItemView, never()).showPostedTime(trackItem.getCreatedAt());
        verify(trackItemView).showPlaycount(anyString());
    }

    @Test
    public void shouldNotShowGoPlusIntroductoryOverlayIfTrackIsNotFullHighTier() {
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        TrackItem snippedHighTierTrack = ModelFixtures.trackItem(trackBuilder.snipped(true).subHighTier(true).build());

        renderer.bindSearchTrackView(snippedHighTierTrack, itemView, 0, Optional.absent(), Optional.absent());

        verifyZeroInteractions(introductoryOverlayPresenter);
    }

    @Test
    public void shouldNotShowGoPlusIntroductoryOverlayIfExperimentIsNotEnabled() {
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        TrackItem snippedHighTierTrack = ModelFixtures.trackItem(trackBuilder.snipped(false).subHighTier(true).build());

        renderer.bindSearchTrackView(snippedHighTierTrack, itemView, 0, Optional.absent(), Optional.absent());

        verifyZeroInteractions(introductoryOverlayPresenter);
    }

    @Test
    public void shouldNotShowGoPlusIntroductoryOverlayIfNotHighTier() {
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        TrackItem snippedHighTierTrack = ModelFixtures.trackItem(trackBuilder.snipped(false).subHighTier(true).build());

        renderer.bindSearchTrackView(snippedHighTierTrack, itemView, 0, Optional.absent(), Optional.absent());

        verifyZeroInteractions(introductoryOverlayPresenter);
    }

    @Test
    public void shouldShowGoPlusIntroductoryOverlay() {
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        TrackItem snippedHighTierTrack = ModelFixtures.trackItem(trackBuilder.snipped(false).subHighTier(true).build());

        renderer.bindSearchTrackView(snippedHighTierTrack, itemView, 0, Optional.absent(), Optional.absent());

        ArgumentCaptor<IntroductoryOverlay> introductoryOverlayCaptor = ArgumentCaptor.forClass(IntroductoryOverlay.class);
        verify(introductoryOverlayPresenter).showIfNeeded(introductoryOverlayCaptor.capture());

        IntroductoryOverlay introductoryOverlay = introductoryOverlayCaptor.getValue();
        assertThat(introductoryOverlay.overlayKey()).isEqualTo(IntroductoryOverlayKey.SEARCH_GO_PLUS);
        assertThat(introductoryOverlay.targetView()).isEqualTo(trackItemView.getGoIndicator());
        assertThat(introductoryOverlay.title()).isEqualTo(R.string.overlay_search_go_plus_title);
        assertThat(introductoryOverlay.description()).isEqualTo(R.string.overlay_search_go_plus_description);
        assertThat(introductoryOverlay.icon()).isEqualTo(Optional.of(trackItemView.getResources().getDrawable(R.drawable.go_indicator_tooltip)));
        assertThat(introductoryOverlay.event()).isEqualTo(Optional.of(GoOnboardingTooltipEvent.forSearchGoPlus()));
    }

}
