package com.soundcloud.android.view.adapters;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

public class PlaylistItemRendererTest extends AndroidUnitTest {

    private PlaylistItemRenderer renderer;

    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistItemMenuPresenter playlistItemMenuPresenter;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;
    @Mock private Navigator navigator;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());

    private View itemView;

    private PlaylistItem playlistItem;
    private PlaylistItem.Default.Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = ModelFixtures.playlistItemBuilder()
                               .getUrn(Urn.forPlaylist(123))
                               .title("title")
                               .creatorName("creator")
                               .likesCount(5)
                               .isUserLike(false)
                               .trackCount(11);
        playlistItem = builder.build();

        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.playlist_list_item, new FrameLayout(context()), false);
        renderer = new PlaylistItemRenderer(resources(), imageOperations, numberFormatter,
                                            playlistItemMenuPresenter, eventBus, screenProvider, navigator);
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.list_item_subheader).getText()).isEqualTo("title");
    }

    @Test
    public void shouldBindCreatorToView() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.list_item_header).getText()).isEqualTo("creator");
    }

    @Test
    public void shouldBindTrackCountToView() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.list_item_right_info).getText()).isEqualTo("11 tracks");
    }

    @Test
    public void shouldShowLikesCountToViewIfAny() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.list_item_counter).getText()).isEqualTo("5");
    }

    @Test
    public void shouldHideLikesCountToViewIfPlaylistHasZeroLikes() {
        renderer.bindItemView(0, itemView, singletonList(builder.likesCount(0).build()));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideAlbumLabelAndAlbumYearIfPlaylistHasLikes() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.album_title).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldNotBindLikesCountToViewIfLikesCountNotSet() {
        renderer.bindItemView(0, itemView, singletonList(builder.likesCount(Consts.NOT_SET).build()));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldBindLikeStatusToView() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));
        assertThat(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).isEqualTo(0);

        renderer.bindItemView(0, itemView, singletonList(builder.isUserLike(true).build()));
        assertThat(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).isEqualTo(1);
    }

    @Test
    public void shouldShowPrivateIndicatorIfPlaylistIsPrivate() {
        renderer.bindItemView(0, itemView, singletonList(builder.isPrivate(true).build()));

        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPublic() {
        renderer.bindItemView(0, itemView, singletonList(builder.isPrivate(false).build()));

        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.GONE);
        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPromoted() {
        PlaylistItem item = PlayableFixtures.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideLikesCountIfPlaylistIsPromoted() {
        PlaylistItem item = PlayableFixtures.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideAlbumsLabelAndYearIfPlaylistIsPromoted() {
        PlaylistItem item = PlayableFixtures.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.album_title).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldShowPromotedLabelWithPromoterIfPlaylistIsPromotedByPromoter() {
        PlaylistItem item = PlayableFixtures.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.promoted_playlist).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.promoted_playlist).getText()).isEqualTo("Promoted by SoundCloud");
    }

    @Test
    public void shouldShowPromotedLabelWithoutPromoterIfPlaylistIsPromotedWithoutPromoter() {
        PlaylistItem item = PlayableFixtures.expectedPromotedPlaylistWithoutPromoter();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.promoted_playlist).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.promoted_playlist).getText()).isEqualTo("Promoted");
    }

    @Test
    public void shouldHideLikesCountPromotedLabelAndPrivateIndicatorForAlbums() {
        PlaylistItem item = PlaylistItem.builder(playlistItem).isAlbum(true).build();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
        assertThat(textView(R.id.promoted_playlist).getVisibility()).isEqualTo(View.GONE);
        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldDisplayAlbumTitleForAlbums() {
        PlaylistItem item = PlaylistItem.builder(playlistItem).isAlbum(true).setType(Optional.of("ep")).releaseDate("2010-10-10").build();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.album_title).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.album_title).getText()).isEqualTo("EP · 2010");
    }

    @Test
    public void shouldLoadIcon() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));
        verify(imageOperations).displayInAdapterView(
                playlistItem,
                ApiImageSize.getListItemImageSize(itemView.getResources()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
