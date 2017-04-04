package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Locale;

public class StreamTrackItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private StreamItemViewHolder viewHolder;
    @Mock private CardEngagementsPresenter engagementsPresenter;
    @Mock private StreamCardViewPresenter headerViewPresenter;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private final TrackItem postedTrack = ModelFixtures.trackItem();
    private final TrackStreamItem postedTrackStreamItem = TrackStreamItem.create(postedTrack, postedTrack.getCreatedAt(), Optional.absent());

    private StreamTrackItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.stream_track_card, new FrameLayout(context()), false);
        itemView.setTag(viewHolder);

        renderer = new StreamTrackItemRenderer(numberFormatter, null, engagementsPresenter, headerViewPresenter);
    }

    @Test
    public void bindsHeaderViewPresenter() {
        renderer.bindItemView(0, itemView, singletonList(postedTrackStreamItem));

        verify(headerViewPresenter).bind(eq(viewHolder), eq(postedTrack), any(EventContextMetadata.Builder.class), eq(postedTrackStreamItem.createdAt()), eq(Optional.absent()));
    }

    @Test
    public void bindsPlayCountAndOverflow() {
        renderer.bindItemView(0, itemView, singletonList(postedTrackStreamItem));

        verify(viewHolder).showPlayCount(formattedStats(postedTrack.playCount()));
        verify(viewHolder).setOverflowListener(any(StreamItemViewHolder.OverflowListener.class));
    }

    @Test
    public void bindsEngagementsPresenter() {
        renderer.bindItemView(0, itemView, singletonList(postedTrackStreamItem));

        verify(engagementsPresenter).bind(eq(viewHolder), eq(postedTrack), any(EventContextMetadata.class));
    }

    @Test
    public void createsBaseContextMetadata() {
        final Integer position = 0;
        final EventContextMetadata context = renderer.getEventContextMetadataBuilder(postedTrack, position).build();

        assertThat(context.pageName()).isEqualTo(Screen.STREAM.get());
        assertThat(context.module()).isEqualTo(Module.create(Module.STREAM, position));
        assertThat(context.attributingActivity()).isEqualTo(AttributingActivity.fromPlayableItem(postedTrack));
    }

    @Test
    public void bindsNowPlaying() {
        final TrackItem updatedPostedTrack = postedTrack.withPlayingState(true);
        final TrackStreamItem updatedPostedTrackStreamItem = TrackStreamItem.create(updatedPostedTrack, updatedPostedTrack.getCreatedAt(), Optional.absent());

        renderer.bindItemView(0, itemView, singletonList(updatedPostedTrackStreamItem));

        verify(viewHolder).showNowPlaying();
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }
}
