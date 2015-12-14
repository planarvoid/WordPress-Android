package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.view.adapters.PlaylistGridRenderer.ItemViewHolder;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;

import java.util.Collections;

public class PlaylistGridRendererTest extends AndroidUnitTest {

    private PlaylistGridRenderer renderer;

    @Mock private ImageOperations imageOperations;

    @Before
    public void setUp() throws Exception {
        renderer = new PlaylistGridRenderer(imageOperations);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = renderer.createItemView(new FrameLayout(context()));
        assertThat(itemView).isNotNull();
        assertThat(itemView.getTag()).isNotNull(); // contains the private ViewHolder instance
        assertThat(itemView.findViewById(R.id.image)).isNotNull();
        assertThat(itemView.findViewById(R.id.username)).isNotNull();
        assertThat(itemView.findViewById(R.id.title)).isNotNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        PlaylistItem playlistItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));

        View itemView = renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        assertThat(viewHolder.title.getText()).isEqualTo(playlistItem.getTitle());
        assertThat(viewHolder.username.getText()).isEqualTo(playlistItem.getCreatorName());
        assertThat(viewHolder.tagList.getText()).isEqualTo("#tag1, #tag2");
        assertThat(viewHolder.trackCount.getText()).isEqualTo("2 tracks");
    }

    @Test
    public void shouldShowJustTheTagIfPlaylistHasSingleTag() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTags(singletonList("tag1"));
        PlaylistItem playlistItem = PlaylistItem.from(playlist);

        View itemView = renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        assertThat(viewHolder.tagList.getText()).isEqualTo("#tag1");
    }

    @Test
    public void shouldShowBlankTagIfPlaylistHasNoTags() throws CreateModelException {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTags(Collections.<String>emptyList());
        PlaylistItem playlistItem = PlaylistItem.from(playlist);

        View itemView = renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        assertThat(viewHolder.tagList.getText()).isEqualTo("");
    }
}
