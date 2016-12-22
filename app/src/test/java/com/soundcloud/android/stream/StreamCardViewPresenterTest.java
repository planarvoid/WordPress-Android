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
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.collections.PropertySet;
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

    @Mock private HeaderSpannableBuilder headerSpannableBuilder;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;
    @Mock private Navigator navigator;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamItemViewHolder itemView;
    @Mock private ImageView imageView;
    @Mock private View view;
    @Mock private FeatureOperations featureOperations;

    private Date createdAtStream = new Date();
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
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(itemView).resetCardView();
    }

    @Test
    public void shouldShowPromotedIndicator() {
        SpannableString promotedSpannedString = new SpannableString("Promoted");
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        when(headerSpannableBuilder.promotedSpannedString(promotedTrackItem.getPlayableType())).thenReturn(headerSpannableBuilder);
        when(headerSpannableBuilder.get()).thenReturn(promotedSpannedString);
        presenter.bind(itemView, promotedTrackItem, EventContextMetadata.builder());

        verify(itemView).hideUserImage();
        verify(itemView).setPromotedHeader(promotedSpannedString);
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        SpannableString promotedSpannedString = new SpannableString("Promoted");
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        when(headerSpannableBuilder.promotedSpannedString(promotedTrackItem.getPlayableType())).thenReturn(headerSpannableBuilder);
        when(headerSpannableBuilder.get()).thenReturn(promotedSpannedString);
        presenter.bind(itemView, promotedTrackItem, EventContextMetadata.builder());

        verify(itemView).setPromoterHeader("SoundCloud", promotedSpannedString);
        verify(itemView).setPromoterClickable(any(PromoterClickViewListener.class));
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        when(view.getContext()).thenReturn(context());

        PlayableItem promotedListItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        presenter.bind(itemView, promotedListItem, EventContextMetadata.builder());

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
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(headerSpannableBuilder).actionSpannedString(repostedString(), playlistItem.getPlayableType());
        verify(itemView).setRepostHeader(playlistItem.getReposter().get(), spannable);
    }

    @Test
    public void buildsPostedHeaderStringForPostedPlaylist() {
        SpannableString spannable = new SpannableString("posted a playlist");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(headerSpannableBuilder).userActionSpannedString(playlistItem.getCreatorName(), "posted", playlistItem.getPlayableType());
        verify(itemView).setHeaderText(spannable);
    }

    @Test
    public void bindsHeaderViewPropertiesToViewHolder() {
        PlaylistItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(itemView).setCreatedAt(formattedDate(playlistItem.getCreatedAt()));
        verify(itemView).togglePrivateIndicator(playlistItem.isPrivate());
    }

    @Test
    public void buildsPostedHeaderStringForPostedTrack() {
        SpannableString spannable = new SpannableString("posted a track");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        TrackItem postedTrack = postedTrack();
        presenter.bind(itemView, postedTrack, EventContextMetadata.builder());

        verify(headerSpannableBuilder).userActionSpannedString(postedTrack.getCreatorName(), "posted", postedTrack.getPlayableType());
        verify(itemView).setHeaderText(spannable);
    }

    @Test
    public void buildsRepostedHeaderStringForTrackWithReposter() {
        SpannableString spannable = new SpannableString("reposted a track");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlayableItem trackItem = repostedTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder());

        verify(headerSpannableBuilder).actionSpannedString(repostedString(), trackItem.getPlayableType());
        verify(itemView).setRepostHeader(trackItem.getReposter().get(), spannable);
    }

    @Test
    public void bindsHeaderAvatarForPlaylistWithReposter() {
        PlaylistItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(imageOperations)
                .displayCircularInAdapterView(
                        argThat(isImageResourceFor(playlistItem.getReposterUrn(), playlistItem.getAvatarUrlTemplate())),
                        any(ApiImageSize.class),
                        eq(imageView));
    }

    @Test
    public void bindsArtworkView() {
        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem),
                        any(ApiImageSize.class),
                        eq(imageView));

        verify(itemView).setTitle(playlistItem.getTitle());
        verify(itemView).setArtist(playlistItem.getCreatorName());
    }

    @Test
    public void bindsHeaderAvatarForPostedPlaylist() {
        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem, EventContextMetadata.builder());

        verify(imageOperations)
                .displayCircularInAdapterView(
                        argThat(isImageResourceFor(playlistItem.getCreatorUrn(), playlistItem.getAvatarUrlTemplate())),
                        any(ApiImageSize.class),
                        eq(imageView));
    }

    @Test
    public void bindsHeaderAvatarForPromotedItemWithPromoter() {
        PromotedPlaylistItem promotedItem = promotedPlaylistItem();
        presenter.bind(itemView, promotedItem, EventContextMetadata.builder());

        verify(imageOperations)
                .displayCircularInAdapterView(
                        argThat(isImageResourceFor(promotedItem.getPromoterUrn().get(),
                                                                   promotedItem.getAvatarUrlTemplate())),
                        any(ApiImageSize.class),
                        eq(imageView));
    }

    @Test
    public void doesNotBindHeaderAvatarForPromotedItemWithoutPromoter() {
        PromotedPlaylistItem promotedItem = promotedPlaylistWithoutPromoter();
        presenter.bind(itemView, promotedItem, EventContextMetadata.builder());

        verify(imageOperations, never())
                .displayCircularInAdapterView(
                        any(ImageResource.class),
                        any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void bindsGoLabelForSnippedTrackHighTierTrack() {
        TrackItem trackItem = upsellableTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder());

        verify(itemView).showGoIndicator();
    }

    @Test
    public void bindsGoLabelForAvailableHighTierTrack() {
        TrackItem trackItem = highTierTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder());

        verify(itemView).showGoIndicator();
    }

    @Test
    public void hidesHighTierLabelWhenOtherItem() {
        PlayableItem trackItem = repostedTrack();
        presenter.bind(itemView, trackItem, EventContextMetadata.builder());

        verify(itemView, never()).showGoIndicator();
    }

    private PromotedPlaylistItem promotedPlaylistItem() {
        return PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
    }

    private PromotedPlaylistItem promotedPlaylistWithoutPromoter() {
        return PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylistWithoutPromoter());
    }

    private PlaylistItem postedPlaylist() {
        final PropertySet playlist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        playlist.put(PlayableProperty.CREATED_AT, createdAtStream);

        return PlaylistItem.from(playlist);
    }

    private PlaylistItem repostedPlaylist() {
        final PlaylistItem playlistItem = postedPlaylist();
        playlistItem.updated(PropertySet.from(
                PostProperty.REPOSTER.bind("reposter"),
                PostProperty.REPOSTER_URN.bind(Urn.forUser(123L))
        ));
        return playlistItem;
    }

    private TrackItem postedTrack() {
        final PropertySet track = ModelFixtures.create(ApiTrack.class).toPropertySet();
        track.put(PlayableProperty.CREATED_AT, createdAtStream);

        return TrackItem.from(track);
    }

    private TrackItem repostedTrack() {
        final TrackItem trackItem = postedTrack();
        trackItem.updated(PropertySet.from(
                PostProperty.REPOSTER.bind("reposter"),
                PostProperty.REPOSTER_URN.bind(Urn.forUser(123L))
        ));
        return trackItem;
    }

    private TrackItem upsellableTrack() {
        final PropertySet track = TestPropertySets.upsellableTrack();
        track.put(PlayableProperty.CREATED_AT, createdAtStream);

        return TrackItem.from(track);
    }

    private TrackItem highTierTrack() {
        final PropertySet track = TestPropertySets.highTierTrack();
        track.put(PlayableProperty.CREATED_AT, createdAtStream);

        return TrackItem.from(track);
    }

    private String repostedString() {
        return resources().getString(R.string.stream_reposted_action);
    }

    private String formattedDate(Date createdAt) {
        return ScTextUtils.formatTimeElapsedSince(resources(), createdAt.getTime(), true);
    }

}
