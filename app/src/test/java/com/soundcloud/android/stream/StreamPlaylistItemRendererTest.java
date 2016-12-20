package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.concurrent.TimeUnit;

public class StreamPlaylistItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistItemMenuPresenter menuPresenter;
    @Mock private StreamCardViewPresenter cardViewPresenter;
    @Mock private CardEngagementsPresenter engagementsPresenter;
    @Mock private StreamPlaylistItemRenderer.StreamPlaylistViewHolder viewHolder;

    private final PlaylistItem playlistItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    private final PlaylistStreamItem playlistStreamItem = PlaylistStreamItem.create(playlistItem,
                                                                                    playlistItem.getCreatedAt());

    private StreamPlaylistItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.default_playlist_card, new FrameLayout(context()), false);
        itemView.setTag(viewHolder);

        renderer = new StreamPlaylistItemRenderer(
                menuPresenter, cardViewPresenter, engagementsPresenter, resources());
    }

    @Test
    public void bindsCardViewPresenter() {
        renderer.bindItemView(0, itemView, singletonList(playlistStreamItem));

        verify(cardViewPresenter).bind(eq(viewHolder), eq(playlistItem), any(EventContextMetadata.Builder.class));
    }

    @Test
    public void createsBaseContextMetadata() {
        final int position = 0;
        final EventContextMetadata context = renderer.getEventContextMetadataBuilder(playlistItem, position).build();

        assertThat(context.invokerScreen()).isEqualTo(ScreenElement.LIST.get());
        assertThat(context.contextScreen()).isEqualTo(Screen.STREAM.get());
        assertThat(context.pageName()).isEqualTo(Screen.STREAM.get());
        assertThat(context.module()).isEqualTo(Module.create(Module.STREAM, position));
        assertThat(context.attributingActivity()).isEqualTo(AttributingActivity.fromPlayableItem(playlistItem));
    }

    @Test
    public void bindsTrackCount() {
        renderer.bindItemView(0, itemView, singletonList(playlistStreamItem));

        verify(viewHolder).setTrackCount(
                String.valueOf(playlistItem.getTrackCount()),
                tracksString(playlistItem.getTrackCount()));
    }

    @Test
    public void bindsEngagementsPresenter() {
        renderer.bindItemView(0, itemView, singletonList(playlistStreamItem));

        ArgumentCaptor<EventContextMetadata> eventContextCaptor = ArgumentCaptor.forClass(EventContextMetadata.class);
        verify(engagementsPresenter).bind(eq(viewHolder), eq(playlistItem), eventContextCaptor.capture());

        EventContextMetadata context = eventContextCaptor.getValue();
        assertThat(context.invokerScreen()).isEqualTo(ScreenElement.LIST.get());
        assertThat(context.contextScreen()).isEqualTo(Screen.STREAM.get());
        assertThat(context.pageName()).isEqualTo(Screen.STREAM.get());
    }

    @Test
    public void bindsDurationAndOverflow() {
        renderer.bindItemView(0, itemView, singletonList(playlistStreamItem));

        verify(viewHolder).setOverflowListener(any(StreamItemViewHolder.OverflowListener.class));
        verify(cardViewPresenter).bind(eq(viewHolder), eq(playlistItem), any(EventContextMetadata.Builder.class));
    }

    private String tracksString(int trackCount) {
        return resources().getQuantityString(R.plurals.number_of_tracks, trackCount);
    }

    private String formattedTime(long time) {
        return ScTextUtils.formatTimestamp(time, TimeUnit.MILLISECONDS);
    }

}
