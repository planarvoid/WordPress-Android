package com.soundcloud.android.stream;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Locale;

public class StreamTrackItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private StreamItemViewHolder viewHolder;
    @Mock private StreamItemHeaderViewPresenter headerViewPresenter;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());
    private StreamTrackItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.stream_track_item, new FrameLayout(context()), false);
        itemView.setTag(viewHolder);

        renderer = new StreamTrackItemRenderer(
                imageOperations, numberFormatter, null, resources(), headerViewPresenter);
    }

    @Test
    public void bindsHeaderViewPresenter() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(headerViewPresenter).setupHeaderView(viewHolder, postedTrack);
    }

    @Test
    public void bindsArtworkView() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(imageOperations)
                .displayInAdapterView(
                        eq(postedTrack.getEntityUrn()),
                        any(ApiImageSize.class),
                        any(ImageView.class));

        verify(viewHolder).setTitle(postedTrack.getTitle());
        verify(viewHolder).setCreator(postedTrack.getCreatorName());
    }

    @Test
    public void resetsEngagementsBar() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).resetAdditionalInformation();
    }

    @Test
    public void bindsEngagementsBar() {
        TrackItem postedTrack = postedTrack();
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).showLikeStats(formattedStats(postedTrack.getLikesCount()), postedTrack.isLiked());
        verify(viewHolder).showRepostStats(formattedStats(postedTrack.getRepostCount()), postedTrack.isReposted());
        verify(viewHolder).showPlayCount(formattedStats(postedTrack.getPlayCount()));
    }

    @Test
    public void bindsNowPlaying() {
        TrackItem postedTrack = postedTrack();
        postedTrack.setIsPlaying(true);
        renderer.bindItemView(0, itemView, singletonList(postedTrack));

        verify(viewHolder).showNowPlaying();
    }

    private TrackItem postedTrack() {
        return TrackItem.from(ModelFixtures.create(ApiTrack.class));
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }
}
