package com.soundcloud.android.stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
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
    @Mock private View view;
    @Mock private FeatureOperations featureOperations;

    private Date createdAtStream = new Date();
    private StreamCardViewPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new StreamCardViewPresenter(headerSpannableBuilder, eventBus, screenProvider,
                navigator, resources(), imageOperations);
    }

    @Test
    public void resetsHeaderViewBeforeSettingUp() throws Exception {
        PlayableItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(itemView).resetCardView();
    }

    @Test
    public void shouldShowPromotedIndicator() {
        SpannableString promotedSpannedString = new SpannableString("Promoted");
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        when(headerSpannableBuilder.promotedSpannedString(true)).thenReturn(headerSpannableBuilder);
        when(headerSpannableBuilder.get()).thenReturn(promotedSpannedString);
        presenter.bind(itemView, promotedTrackItem);

        verify(itemView).hideUserImage();
        verify(itemView).setPromotedHeader(promotedSpannedString);
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        SpannableString promotedSpannedString = new SpannableString("Promoted");
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        when(headerSpannableBuilder.promotedSpannedString(true)).thenReturn(headerSpannableBuilder);
        when(headerSpannableBuilder.get()).thenReturn(promotedSpannedString);
        presenter.bind(itemView, promotedTrackItem);

        verify(itemView).setPromoterHeader("SoundCloud", promotedSpannedString);
        verify(itemView).setPromoterClickable(any(PromoterClickViewListener.class));
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        when(view.getContext()).thenReturn(context());

        PlayableItem promotedListItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        presenter.bind(itemView, promotedListItem);

        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(itemView).setPromoterClickable(captor.capture());
        captor.getValue().onClick(view);

        verify(navigator).openProfile(any(Context.class), eq(Urn.forUser(193L)));
        verify(eventBus).publish(eq(EventQueue.TRACKING), any(PromotedTrackingEvent.class));
    }

    @Test
    public void buildsRepostedHeaderStringForPlaylistWithReposter() {
        SpannableString spannable = new SpannableString("reposted a playlist");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlayableItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(headerSpannableBuilder).actionSpannedString(repostedString(), false);
        verify(itemView).setRepostHeader(playlistItem.getReposter().get(), spannable);
    }

    @Test
    public void buildsPostedHeaderStringForPostedPlaylist() {
        SpannableString spannable = new SpannableString("posted a playlist");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(headerSpannableBuilder).userActionSpannedString(playlistItem.getCreatorName(), "posted", false);
        verify(itemView).setHeaderText(spannable);
    }

    @Test
    public void bindsHeaderViewPropertiesToViewHolder() {
        PlaylistItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(itemView).setCreatedAt(formattedDate(playlistItem.getCreatedAt()));
        verify(itemView).togglePrivateIndicator(playlistItem.isPrivate());
    }

    @Test
    public void buildsPostedHeaderStringForPostedTrack() {
        SpannableString spannable = new SpannableString("posted a track");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        TrackItem postedTrack = postedTrack();
        presenter.bind(itemView, postedTrack);

        verify(headerSpannableBuilder).userActionSpannedString(postedTrack.getCreatorName(), "posted", true);
        verify(itemView).setHeaderText(spannable);
    }

    @Test
    public void buildsRepostedHeaderStringForTrackWithReposter() {
        SpannableString spannable = new SpannableString("reposted a track");
        when(headerSpannableBuilder.get()).thenReturn(spannable);

        PlayableItem trackItem = repostedTrack();
        presenter.bind(itemView, trackItem);

        verify(headerSpannableBuilder).actionSpannedString(repostedString(), true);
        verify(itemView).setRepostHeader(trackItem.getReposter().get(), spannable);
    }

    @Test
    public void bindsHeaderAvatarForPlaylistWithReposter() {
        PlaylistItem playlistItem = repostedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(imageOperations)
                .displayCircularInAdapterView(eq(playlistItem.getReposterUrn()), any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void bindsArtworkView() {
        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem.getEntityUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));

        verify(itemView).setTitle(playlistItem.getTitle());
        verify(itemView).setArtist(playlistItem.getCreatorName());
    }

    @Test
    public void bindsHeaderAvatarForPostedPlaylist() {
        PlaylistItem playlistItem = postedPlaylist();
        presenter.bind(itemView, playlistItem);

        verify(imageOperations)
                .displayCircularInAdapterView(eq(playlistItem.getCreatorUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));
    }

    @Test
    public void bindsPreviewIndicatorForSnippedForMidTierUpsell() {
        TrackItem trackItem = upsellableTrack();
        presenter.bind(itemView, trackItem);

        verify(itemView).togglePreviewIndicator(true);
    }

    @Test
    public void doesNotBindPreviewIndicatorWhenShouldNotUpsellMidTier() {
        PlayableItem trackItem = repostedTrack();
        presenter.bind(itemView, trackItem);

        verify(itemView).togglePreviewIndicator(false);
    }

    private PlaylistItem postedPlaylist() {
        final PropertySet playlist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        playlist.put(SoundStreamProperty.CREATED_AT, createdAtStream);

        return PlaylistItem.from(playlist);
    }

    private PlaylistItem repostedPlaylist() {
        final PlaylistItem playlistItem = postedPlaylist();
        playlistItem.update(PropertySet.from(
                PostProperty.REPOSTER.bind("reposter"),
                PostProperty.REPOSTER_URN.bind(Urn.forUser(123L))
        ));
        return playlistItem;
    }

    private TrackItem postedTrack() {
        final PropertySet track = ModelFixtures.create(ApiTrack.class).toPropertySet();
        track.put(SoundStreamProperty.CREATED_AT, createdAtStream);

        return TrackItem.from(track);
    }

    private TrackItem repostedTrack() {
        final TrackItem trackItem = postedTrack();
        trackItem.update(PropertySet.from(
                PostProperty.REPOSTER.bind("reposter"),
                PostProperty.REPOSTER_URN.bind(Urn.forUser(123L))
        ));
        return trackItem;
    }

    private TrackItem upsellableTrack() {
        final PropertySet track = TestPropertySets.upsellableTrack();
        track.put(SoundStreamProperty.CREATED_AT, createdAtStream);

        return TrackItem.from(track);
    }

    private String repostedString() {
        return resources().getString(R.string.stream_reposted_action);
    }

    private String formattedDate(Date createdAt) {
        return ScTextUtils.formatTimeElapsedSince(resources(), createdAt.getTime(), true);
    }

}
