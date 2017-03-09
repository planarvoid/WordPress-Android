package com.soundcloud.android.stream;

import static com.soundcloud.android.testsupport.matchers.ImageResourceMatcher.isImageResourceFor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;

import java.util.Date;

public class StreamCardViewPresenterTest extends AndroidUnitTest {

    private static final Date ITEM_CREATED_AT = new Date();
    private static final Date CREATED_AT_STREAM = new Date(ITEM_CREATED_AT.getTime() + 100);

    @Mock private HeaderSpannableBuilder headerSpannableBuilder;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;
    @Mock private Navigator navigator;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamItemViewHolder itemView;
    @Mock private ImageView imageView;
    @Mock private View view;
    @Mock private FeatureOperations featureOperations;

    @Mock private FeatureFlags flags;

    private StreamCardViewPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new StreamCardViewPresenter(headerSpannableBuilder, eventBus, screenProvider,
                                                navigator, resources(), imageOperations);

        when(itemView.getImage()).thenReturn(imageView);
        when(itemView.getUserImage()).thenReturn(imageView);
    }

    @Test
    public void resetsHeaderViewBeforeSettingUp() throws Exception {
        PlayableItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView).resetCardView();
    }

    @Test
    public void shouldShowPromotedIndicator() {
        SpannableString promotedSpannedString = new SpannableString("Promoted");
        TrackItem promotedTrackItem = PlayableFixtures.expectedPromotedTrackWithoutPromoter();
        when(headerSpannableBuilder.promotedSpannedString(promotedTrackItem.getPlayableType())).thenReturn(headerSpannableBuilder);
        when(headerSpannableBuilder.get()).thenReturn(promotedSpannedString);
        presenter.bind(itemView, promotedTrackItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView).hideUserImage();
        verify(itemView).setPromotedHeader(promotedSpannedString);
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        SpannableString promotedSpannedString = new SpannableString("Promoted");
        TrackItem promotedTrackItem = PlayableFixtures.expectedPromotedTrack();
        when(headerSpannableBuilder.promotedSpannedString(promotedTrackItem.getPlayableType())).thenReturn(headerSpannableBuilder);
        when(headerSpannableBuilder.get()).thenReturn(promotedSpannedString);
        presenter.bind(itemView, promotedTrackItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView).setPromoterHeader("SoundCloud", promotedSpannedString);
        verify(itemView).setPromoterClickable(any(PromoterClickViewListener.class));
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        when(view.getContext()).thenReturn(context());

        PlayableItem promotedListItem = PlayableFixtures.expectedPromotedTrack();
        presenter.bind(itemView, promotedListItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(itemView).setPromoterClickable(captor.capture());
        captor.getValue().onClick(view);

        verify(navigator).legacyOpenProfile(any(Context.class), eq(Urn.forUser(193L)));
        verify(eventBus).publish(eq(EventQueue.TRACKING), any(PromotedTrackingEvent.class));
    }

    @Test
    public void buildsRepostedHeaderStringForPlaylistWithReposter() {
        SpannableString spannable = new SpannableString("reposted a playlist");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlayableItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(headerSpannableBuilder).actionSpannedString(repostedString(), playlistItem.getPlayableType());
        verify(itemView).setRepostHeader(playlistItem.reposter().get(), spannable);
    }

    @Test
    public void buildsPostedHeaderStringForPostedPlaylist() {
        SpannableString spannable = new SpannableString("posted a playlist");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(headerSpannableBuilder).userActionSpannedString(playlistItem.creatorName(), "posted", playlistItem.getPlayableType());
        verify(itemView).setHeaderText(spannable);
    }

    @Test
    public void bindsHeaderViewPropertiesToViewHolder() {
        PlaylistItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView).setCreatedAt(formattedDate(CREATED_AT_STREAM));
        verify(itemView).togglePrivateIndicator(playlistItem.isPrivate());
    }

    @Test
    public void buildsPostedHeaderStringForPostedTrack() {
        SpannableString spannable = new SpannableString("posted a track");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        TrackItem postedTrack = postedTrack();
        presenter.bind(itemView, postedTrack, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(headerSpannableBuilder).userActionSpannedString(postedTrack.creatorName(), "posted", postedTrack.getPlayableType());
        verify(itemView).setHeaderText(spannable);
    }

    @Test
    public void buildsRepostedHeaderStringForTrackWithReposter() {
        SpannableString spannable = new SpannableString("reposted a track");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlayableItem trackItem = repostedTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(headerSpannableBuilder).actionSpannedString(repostedString(), trackItem.getPlayableType());
        verify(itemView).setRepostHeader(trackItem.reposter().get(), spannable);
    }

    @Test
    public void bindsHeaderAvatarForPlaylistWithReposter() {
        PlaylistItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(imageOperations)
                .displayCircularInAdapterView(
                        argThat(isImageResourceFor(playlistItem.reposterUrn().get(), playlistItem.avatarUrlTemplate())),
                        any(ApiImageSize.class),
                        eq(imageView));
    }

    @Test
    public void bindsArtworkView() {
        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem),
                        any(ApiImageSize.class),
                        eq(imageView));

        verify(itemView).setTitle(playlistItem.title());
        verify(itemView).setArtist(playlistItem.creatorName());
    }

    @Test
    public void bindsHeaderAvatarForPostedPlaylist() {
        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(imageOperations)
                .displayCircularInAdapterView(
                        argThat(isImageResourceFor(playlistItem.creatorUrn(), playlistItem.avatarUrlTemplate())),
                        any(ApiImageSize.class),
                        eq(imageView));
    }

    @Test
    public void bindsHeaderAvatarForPromotedItemWithPromoter() {
        PlaylistItem promotedItem = promotedPlaylistItem();
        presenter.bind(itemView, promotedItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(imageOperations)
                .displayCircularInAdapterView(
                        argThat(isImageResourceFor(promotedItem.promoterUrn().get(),
                                                   promotedItem.avatarUrlTemplate())),
                        any(ApiImageSize.class),
                        eq(imageView));
    }

    @Test
    public void doesNotBindHeaderAvatarForPromotedItemWithoutPromoter() {
        PlaylistItem promotedItem = promotedPlaylistWithoutPromoter();
        presenter.bind(itemView, promotedItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(imageOperations, never())
                .displayCircularInAdapterView(
                        any(ImageResource.class),
                        any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void bindsGoLabelForSnippedTrackHighTierTrack() {
        TrackItem trackItem = upsellableTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView).showGoIndicator();
    }

    @Test
    public void bindsGoLabelForAvailableHighTierTrack() {
        TrackItem trackItem = highTierTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView).showGoIndicator();
    }

    @Test
    public void hidesHighTierLabelWhenOtherItem() {
        PlayableItem trackItem = repostedTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder(), CREATED_AT_STREAM);

        verify(itemView, never()).showGoIndicator();
    }

    private PlaylistItem promotedPlaylistItem() {
        return PlayableFixtures.expectedPromotedPlaylist();
    }

    private PlaylistItem promotedPlaylistWithoutPromoter() {
        return PlayableFixtures.expectedPromotedPlaylistWithoutPromoter();
    }

    private PlaylistItem postedPlaylist() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setCreatedAt(ITEM_CREATED_AT);

        return PlaylistItem.from(playlist);
    }

    private PlaylistItem repostedPlaylist() {
        return postedPlaylist().updateWithReposter("reposter", Urn.forUser(123L));
    }

    private TrackItem postedTrack() {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setCreatedAt(ITEM_CREATED_AT);

        return TrackItem.from(track);
    }

    private TrackItem repostedTrack() {
        return postedTrack().updateWithReposter("reposter", Urn.forUser(123L));
    }

    private TrackItem upsellableTrack() {
        return TrackItem.from(PlayableFixtures.upsellableTrackBuilder().createdAt(ITEM_CREATED_AT).build());
    }

    private TrackItem highTierTrack() {
        return TrackItem.from(PlayableFixtures.highTierTrackBuilder().createdAt(ITEM_CREATED_AT).build());
    }

    private String repostedString() {
        return resources().getString(R.string.stream_reposted_action);
    }

    private String formattedDate(Date createdAt) {
        return ScTextUtils.formatTimeElapsedSince(resources(), createdAt.getTime(), true);
    }

}
