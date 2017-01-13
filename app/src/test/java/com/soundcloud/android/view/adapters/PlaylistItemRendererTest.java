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
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.collections.PropertySet;
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

    private PropertySet propertySet;
    private PlaylistItem playlistItem;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(123)),
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR_NAME.bind("creator"),
                PlayableProperty.LIKES_COUNT.bind(5),
                PlayableProperty.IS_USER_LIKE.bind(false),
                PlaylistProperty.TRACK_COUNT.bind(11)
        );
        playlistItem = PlaylistItem.from(propertySet);

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
        propertySet.put(PlayableProperty.LIKES_COUNT, 0);
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideAlbumLabelAndAlbumYearIfPlaylistHasLikes() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.album_title).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldNotBindLikesCountToViewIfLikesCountNotSet() {
        propertySet.put(PlayableProperty.LIKES_COUNT, Consts.NOT_SET);
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldBindLikeStatusToView() {
        renderer.bindItemView(0, itemView, singletonList(playlistItem));
        assertThat(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).isEqualTo(0);

        propertySet.put(PlayableProperty.IS_USER_LIKE, true);
        renderer.bindItemView(0, itemView, singletonList(playlistItem));
        assertThat(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).isEqualTo(1);
    }

    @Test
    public void shouldShowPrivateIndicatorIfPlaylistIsPrivate() {
        propertySet.put(PlayableProperty.IS_PRIVATE, true);
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPublic() {
        propertySet.put(PlayableProperty.IS_PRIVATE, false);
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.GONE);
        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPromoted() {
        PlaylistItem item = TestPropertySets.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideLikesCountIfPlaylistIsPromoted() {
        PlaylistItem item = TestPropertySets.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideAlbumsLabelAndYearIfPlaylistIsPromoted() {
        PlaylistItem item = TestPropertySets.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.album_title).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldShowPromotedLabelWithPromoterIfPlaylistIsPromotedByPromoter() {
        PlaylistItem item = TestPropertySets.expectedPromotedPlaylist();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.promoted_playlist).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.promoted_playlist).getText()).isEqualTo("Promoted by SoundCloud");
    }

    @Test
    public void shouldShowPromotedLabelWithoutPromoterIfPlaylistIsPromotedWithoutPromoter() {
        PlaylistItem item = TestPropertySets.expectedPromotedPlaylistWithoutPromoter();

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.promoted_playlist).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(textView(R.id.promoted_playlist).getText()).isEqualTo("Promoted");
    }

    @Test
    public void shouldHideLikesCountPromotedLabelAndPrivateIndicatorForAlbums() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        PlaylistItem item = PlaylistItem.from(propertySet);

        renderer.bindItemView(0, itemView, singletonList(item));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
        assertThat(textView(R.id.promoted_playlist).getVisibility()).isEqualTo(View.GONE);
        assertThat(textView(R.id.private_indicator).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldDisplayAlbumTitleForAlbums() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "ep");
        propertySet.put(PlaylistProperty.RELEASE_DATE, "2010-10-10");
        PlaylistItem item = PlaylistItem.from(propertySet);

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
