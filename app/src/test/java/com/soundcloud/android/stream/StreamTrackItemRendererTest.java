package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
    private final TrackItem postedTrack = TrackItem.from(ModelFixtures.create(ApiTrack.class));

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
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(headerViewPresenter).bind(viewHolder, postedTrack);
    }

    @Test
    public void bindsPlayCountAndOverflow() {
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).showPlayCount(formattedStats(postedTrack.getPlayCount()));
        verify(viewHolder).setOverflowListener(any(StreamItemViewHolder.OverflowListener.class));
    }

    @Test
    public void bindsEngagementsPresenter() {
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        ArgumentCaptor<EventContextMetadata> eventContextCaptor = ArgumentCaptor.forClass(EventContextMetadata.class);
        verify(engagementsPresenter).bind(eq(viewHolder), eq(postedTrack), eventContextCaptor.capture());

        EventContextMetadata context = eventContextCaptor.getValue();
        assertThat(context.invokerScreen()).isEqualTo(ScreenElement.LIST.get());
        assertThat(context.contextScreen()).isEqualTo(Screen.STREAM.get());
        assertThat(context.pageName()).isEqualTo(Screen.STREAM.get());
    }

    @Test
    public void bindsNowPlaying() {
        postedTrack.setIsPlaying(true);

        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).showNowPlaying();
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }
}