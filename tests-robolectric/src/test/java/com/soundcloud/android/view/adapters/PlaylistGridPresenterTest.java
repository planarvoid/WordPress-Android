package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.view.adapters.PlaylistGridPresenter.ItemViewHolder;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistGridPresenterTest {

    private PlaylistGridPresenter presenter;

    @Mock
    private ImageOperations imageOperations;

    @Before
    public void setUp() throws Exception {
        presenter = new PlaylistGridPresenter(imageOperations);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = presenter.createItemView(new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(itemView.getTag()).not.toBeNull(); // contains the private ViewHolder instance
        expect(itemView.findViewById(R.id.image)).not.toBeNull();
        expect(itemView.findViewById(R.id.username)).not.toBeNull();
        expect(itemView.findViewById(R.id.title)).not.toBeNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        PlaylistItem playlistItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));

        View itemView = presenter.createItemView(new FrameLayout(Robolectric.application));
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        expect(viewHolder.title.getText()).toEqual(playlistItem.getTitle());
        expect(viewHolder.username.getText()).toEqual(playlistItem.getCreatorName());
        expect(viewHolder.tagList.getText()).toEqual("#tag1, #tag2");
        expect(viewHolder.trackCount.getText()).toEqual("2 tracks");
    }

    @Test
    public void shouldShowJustTheTagIfPlaylistHasSingleTag() throws CreateModelException {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTags(Arrays.asList("tag1"));
        PlaylistItem playlistItem = PlaylistItem.from(playlist);

        View itemView = presenter.createItemView(new FrameLayout(Robolectric.application));
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        expect(viewHolder.tagList.getText()).toEqual("#tag1");
    }

    @Test
    public void shouldShowBlankTagIfPlaylistHasNoTags() throws CreateModelException {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTags(Collections.<String>emptyList());
        PlaylistItem playlistItem = PlaylistItem.from(playlist);

        View itemView = presenter.createItemView(new FrameLayout(Robolectric.application));
        presenter.bindItemView(0, itemView, Arrays.asList(playlistItem));

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        expect(viewHolder.tagList.getText()).toEqual("");
    }
}
