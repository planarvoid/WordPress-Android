package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;

@RunWith(MockitoJUnitRunner.class   )
public class TrackPageHolderTest {

    @Mock private FeatureFlags featureFlags;
    @Mock private View playQueueButton;
    private TrackPagePresenter.TrackPageHolder holder;

    @Before
    public void setUp() {
        holder = new TrackPagePresenter.TrackPageHolder();
        holder.playQueueButton = playQueueButton;
    }

    @Test
    public void viewSetsDoNotContainPQButtonWhenFeatureFlagIsOff() {
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(false);

        holder.populateViewSets(featureFlags);

        assertThat(holder.fullScreenViews).doesNotContain(playQueueButton);
        assertThat(holder.fullScreenAdViews).doesNotContain(playQueueButton);
        assertThat(holder.fullScreenErrorViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnScrubViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnErrorViews).doesNotContain(playQueueButton);
        assertThat(holder.onClickViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnAdViews).doesNotContain(playQueueButton);
    }

    @Test
    public void viewSetsDoNotContainPQButtonWhenFeatureFlagIsOn() {
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(true);

        holder.populateViewSets(featureFlags);

        assertThat(holder.fullScreenViews).doesNotContain(playQueueButton);
        assertThat(holder.fullScreenAdViews).doesNotContain(playQueueButton);
        assertThat(holder.fullScreenErrorViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnErrorViews).doesNotContain(playQueueButton);
        assertThat(holder.hideOnScrubViews).contains(playQueueButton);
        assertThat(holder.onClickViews).contains(playQueueButton);
        assertThat(holder.hideOnAdViews).contains(playQueueButton);
    }

}