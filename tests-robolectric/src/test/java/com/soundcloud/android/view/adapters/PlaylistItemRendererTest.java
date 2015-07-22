package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
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
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistItemRendererTest {

    private PlaylistItemRenderer renderer;

    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistItemMenuPresenter playlistItemMenuPresenter;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;
    @Mock private Navigator navigator;

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
                PlayableProperty.IS_LIKED.bind(false),
                PlaylistProperty.TRACK_COUNT.bind(11)
        );
        playlistItem = PlaylistItem.from(propertySet);

        final Context context = Robolectric.application;
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        itemView = layoutInflater.inflate(R.layout.playlist_list_item, new FrameLayout(context), false);
        renderer = new PlaylistItemRenderer(context.getResources(), imageOperations, playlistItemMenuPresenter,
                eventBus, screenProvider, navigator);
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_subheader).getText()).toEqual("title");
    }

    @Test
    public void shouldBindCreatorToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_header).getText()).toEqual("creator");
    }

    @Test
    public void shouldBindTrackCountToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_right_info).getText()).toEqual("11 tracks");
    }

    @Test
    public void shouldShowLikesCountToViewIfAny() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.list_item_counter).getText()).toEqual("5");
    }

    @Test
    public void shouldHideLikesCountToViewIfPlaylistHasZeroLikes() {
        propertySet.put(PlayableProperty.LIKES_COUNT, 0);
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldNotBindLikesCountToViewIfLikesCountNotSet() {
        propertySet.put(PlayableProperty.LIKES_COUNT, Consts.NOT_SET);
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldBindLikeStatusToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));
        expect(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).toEqual(0);

        propertySet.put(PlayableProperty.IS_LIKED, true);
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));
        expect(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).toEqual(1);
    }

    @Test
    public void shouldBindReposterIfAny() {
        propertySet.put(PlayableProperty.REPOSTER, "reposter");
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.reposter).getVisibility()).toBe(View.VISIBLE);
        expect(textView(R.id.reposter).getText()).toEqual("reposter");
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.reposter).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldShowPrivateIndicatorIfPlaylistIsPrivate() {
        propertySet.put(PlayableProperty.IS_PRIVATE, true);
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPublic() {
        propertySet.put(PlayableProperty.IS_PRIVATE, false);
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.GONE);
        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPromoted() {
        PlaylistItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());

        renderer.bindItemView(0, itemView, Arrays.asList(item));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldHideLikesCountIfPlaylistIsPromoted() {
        PlaylistItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());

        renderer.bindItemView(0, itemView, Arrays.asList(item));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldShowPromotedLabelWithPromoterIfPlaylistIsPromotedByPromoter() {
        PlaylistItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());

        renderer.bindItemView(0, itemView, Arrays.asList(item));

        expect(textView(R.id.promoted_playlist).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.promoted_playlist).getText()).toEqual("Promoted by SoundCloud");
    }

    @Test
    public void shouldShowPromotedLabelWithoutPromoterIfPlaylistIsPromotedWithoutPromoter() {
        PlaylistItem item = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylistWithoutPromoter());

        renderer.bindItemView(0, itemView, Arrays.asList(item));

        expect(textView(R.id.promoted_playlist).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.promoted_playlist).getText()).toEqual("Promoted");
    }

    @Test
    public void shouldLoadIcon() {
        renderer.bindItemView(0, itemView, Arrays.asList(playlistItem));
        verify(imageOperations).displayInAdapterView(
                Urn.forPlaylist(123),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
