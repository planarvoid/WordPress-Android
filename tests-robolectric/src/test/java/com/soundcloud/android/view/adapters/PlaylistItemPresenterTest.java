package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
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
public class PlaylistItemPresenterTest {

    private PlaylistItemPresenter presenter;

    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistItemMenuPresenter playlistItemMenuPresenter;
    @Mock private FeatureFlags featureFlags;

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
        presenter = new PlaylistItemPresenter(context.getResources(), imageOperations, playlistItemMenuPresenter, featureFlags);
    }

    @Test
    public void shouldBindTitleToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_subheader).getText()).toEqual("title");
    }

    @Test
    public void shouldBindCreatorToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_header).getText()).toEqual("creator");
    }

    @Test
    public void shouldBindTrackCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_right_info).getText()).toEqual("11 tracks");
    }

    @Test
    public void shouldShowLikesCountToViewIfAny() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.list_item_counter).getText()).toEqual("5");
    }

    @Test
    public void shouldHideLikesCountToViewIfPlaylistHasZeroLikes() {
        propertySet.put(PlayableProperty.LIKES_COUNT, 0);
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldNotBindLikesCountToViewIfLikesCountNotSet() {
        propertySet.put(PlayableProperty.LIKES_COUNT, Consts.NOT_SET);
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldBindLikeStatusToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));
        expect(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).toEqual(0);

        propertySet.put(PlayableProperty.IS_LIKED, true);
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));
        expect(textView(R.id.list_item_counter).getCompoundDrawables()[0].getLevel()).toEqual(1);
    }

    @Test
    public void shouldBindReposterIfAny() {
        propertySet.put(PlayableProperty.REPOSTER, "reposter");
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.reposter).getVisibility()).toBe(View.VISIBLE);
        expect(textView(R.id.reposter).getText()).toEqual("reposter");
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.reposter).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldShowPrivateIndicatorIfPlaylistIsPrivate() {
        propertySet.put(PlayableProperty.IS_PRIVATE, true);
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfPlaylistIsPublic() {
        propertySet.put(PlayableProperty.IS_PRIVATE, false);
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.GONE);
        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldLoadIcon() {
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));
        verify(imageOperations).displayInAdapterView(
                Urn.forPlaylist(123),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}