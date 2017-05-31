package com.soundcloud.android.view.adapters;

import static java.util.Collections.singletonList;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.adapters.PlaylistCardRenderer.PlaylistViewHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.View;
import android.widget.FrameLayout;

public class PlaylistCardRendererTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock NavigationExecutor navigationExecutor;
    @Mock PlaylistItemMenuPresenter playlistMenuPresenter;
    @Mock CardEngagementsPresenter cardEngagementsPresenter;
    @Mock ScreenProvider screenProvider;
    @Mock ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    private PlaylistCardRenderer renderer;

    @Before
    public void setUp() throws Exception {
        renderer = new PlaylistCardRenderer(resources(),
                                            navigationExecutor,
                                            imageOperations,
                                            playlistMenuPresenter,
                                            cardEngagementsPresenter,
                                            screenProvider,
                                            changeLikeToSaveExperiment);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = renderer.createItemView(new FrameLayout(context()));

        assertThat(itemView).isNotNull();
        assertThat(itemView.getTag()).isNotNull(); // contains the private ViewHolder instance
        assertThat(itemView.findViewById(R.id.image)).isNotNull();
        assertThat(itemView.findViewById(R.id.creator)).isNotNull();
        assertThat(itemView.findViewById(R.id.title)).isNotNull();
    }

    @Test
    public void shouldBindItemView() {
        PlaylistItem playlistItem = ModelFixtures.playlistItem().toBuilder().trackCount(2).build();

        View itemView = renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, itemView, singletonList(playlistItem));

        PlaylistViewHolder viewHolder = (PlaylistViewHolder) itemView.getTag();
        assertThat(viewHolder.title.getText()).isEqualTo(playlistItem.title());
        assertThat(viewHolder.creator.getText()).isEqualTo(playlistItem.creatorName());
        assertThat(viewHolder.trackCount.getText()).isEqualTo("2");
        assertThat(viewHolder.tracksView.getText()).isEqualTo("TRACKS");

        verify(cardEngagementsPresenter).bind(eq(viewHolder), eq(playlistItem), Mockito.any(EventContextMetadata.class));
    }
}
