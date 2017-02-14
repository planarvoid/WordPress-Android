package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.ui.view.PlayerUpsellView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;
import android.widget.Button;

@RunWith(MockitoJUnitRunner.class)
public class TrackPageHolderTest {

    @Mock private View playQueueButton;
    @Mock private PlayerUpsellView upsellView;
    @Mock private Button upsellButton;

    private TrackPagePresenter.TrackPageHolder holder;

    @Before
    public void setUp() {
        when(upsellView.getUpsellButton()).thenReturn(upsellButton);

        holder = new TrackPagePresenter.TrackPageHolder();
        holder.upsellView = upsellView;
        holder.playQueueButton = playQueueButton;
    }

    @Test
    public void viewSetsContainPQButton() {

        holder.populateViewSets();

        assertThat(holder.fullScreenViews).doesNotContain(playQueueButton);
        assertThat(holder.fullScreenAdViews).doesNotContain(playQueueButton);
        assertThat(holder.fullScreenErrorViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnErrorViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnScrubViews).contains(playQueueButton);
        assertThat(holder.onClickViews).contains(playQueueButton);
        assertThat(holder.hideOnAdViews).contains(playQueueButton);
    }

}
