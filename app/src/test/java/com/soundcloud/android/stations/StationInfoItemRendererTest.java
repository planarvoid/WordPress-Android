package com.soundcloud.android.stations;

import static java.util.Collections.singletonList;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleBlurredImageLoader;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;

import android.graphics.Bitmap;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class StationInfoItemRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private StationInfoAdapter.StationInfoClickListener listener;
    @Mock private SimpleBlurredImageLoader simpleBlurredImageLoader;

    private View itemView;
    private StationInfoItemRenderer renderer;

    @Before
    public void setUp() throws Exception {
        itemView = LayoutInflater.from(context()).inflate(
                R.layout.station_info_view, new FrameLayout(context()), false);
        renderer = new StationInfoItemRenderer(listener, simpleBlurredImageLoader, resources(), imageOperations);
        when(imageOperations.blurredPlayerArtwork(eq(resources()),
                                                  any(StationInfo.class),
                                                  any(Scheduler.class),
                                                  any(Scheduler.class)))
                .thenReturn(Observable.<Bitmap>empty());
    }

    @Test
    public void bindsStationInfo() {
        StationInfo stationInfo = StationInfo.from(StationFixtures.getStation(Urn.forTrackStation(123L)));

        renderer.bindItemView(0, itemView, singletonList(stationInfo));

        assertThat(textView(R.id.station_title)).hasText(stationInfo.getTitle());
        assertThat(textView(R.id.station_type)).hasText("Track station based on");
    }

    @Test
    public void bindArtwork() {
        StationInfo stationInfo = StationInfo.from(StationFixtures.getStation(Urn.forTrackStation(123L)));

        renderer.bindItemView(0, itemView, singletonList(stationInfo));

        verify(imageOperations)
                .displayWithPlaceholder(eq(stationInfo), any(ApiImageSize.class), eq(imageView(R.id.artwork)));
        verify(simpleBlurredImageLoader).displayBlurredArtwork(stationInfo, imageView(R.id.blurred_background));
    }

    private TextView textView(@IdRes int id) {
        return (TextView) itemView.findViewById(id);
    }

    private ImageView imageView(@IdRes int id) {
        return (ImageView) itemView.findViewById(id);
    }
}
