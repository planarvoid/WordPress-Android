package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StreamPlaylistItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistItemMenuPresenter menuPresenter;
    @Mock private StreamItemHeaderViewPresenter headerViewPresenter;
    @Mock private StreamItemEngagementsPresenter engagementsPresenter;
    @Mock private StreamPlaylistItemRenderer.StreamPlaylistViewHolder viewHolder;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());
    private StreamPlaylistItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.stream_playlist_item, new FrameLayout(context()), false);
        itemView.setTag(viewHolder);


        renderer = new StreamPlaylistItemRenderer(
                imageOperations, menuPresenter, headerViewPresenter, engagementsPresenter, resources());
    }

    @Test
    public void bindsHeaderPresenter() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(headerViewPresenter).setupHeaderView(viewHolder, playlistItem);
    }

    @Test
    public void bindsArtworkView() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(playlistItem.getEntityUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));

        verify(viewHolder).setTitle(playlistItem.getTitle());
        verify(viewHolder).setCreator(playlistItem.getCreatorName());
        verify(viewHolder).setTrackCount(
                String.valueOf(playlistItem.getTrackCount()),
                tracksString(playlistItem.getTrackCount()));
    }

    @Test
    public void bindsEngagementsPresenter() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        engagementsPresenter.bind(viewHolder, playlistItem);
    }

    @Test
    public void bindsDurationAndOverflow() {
        PlaylistItem playlistItem = postedPlaylist();
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        verify(viewHolder).showDuration(formattedTime(playlistItem.getDuration()));
        verify(viewHolder).setOverflowListener(any(StreamItemViewHolder.OverflowListener.class));
    }

    private PlaylistItem postedPlaylist() {
        return PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    }

    private String tracksString(int trackCount) {
        return resources().getQuantityString(R.plurals.number_of_tracks, trackCount);
    }

    private String formattedTime(long time) {
        return ScTextUtils.formatTimestamp(time, TimeUnit.MILLISECONDS);
    }

}
