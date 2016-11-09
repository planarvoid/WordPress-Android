package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.createStationInfoTrack;
import static com.soundcloud.android.stations.StationFixtures.getStationWithTracks;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleBlurredImageLoader;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.IdRes;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class StationInfoHeaderRendererTest extends AndroidUnitTest {

    @Mock private ImageOperations imageOperations;
    @Mock private StationInfoAdapter.StationInfoClickListener listener;
    @Mock private SimpleBlurredImageLoader simpleBlurredImageLoader;

    private View itemView;
    private StationInfoHeaderRenderer renderer;
    private Urn station = Urn.forTrackStation(123L);

    @Before
    public void setUp() throws Exception {
        itemView = LayoutInflater.from(context()).inflate(
                R.layout.station_info_view, new FrameLayout(context()), false);
        renderer = new StationInfoHeaderRenderer(listener, simpleBlurredImageLoader, resources(), imageOperations);
    }

    @Test
    public void bindsStationInfo() {
        StationInfoHeader stationInfoHeader = StationInfoHeader.from(getStationWithTracks(station));

        renderer.bindItemView(0, itemView, singletonList(stationInfoHeader));

        assertThat(textView(R.id.station_title)).hasText(stationInfoHeader.getTitle());
        assertThat(textView(R.id.station_type)).hasText("Track station based on");
    }

    @Test
    public void bindArtwork() {
        StationInfoHeader stationInfoHeader = StationInfoHeader.from(getStationWithTracks(station));

        renderer.bindItemView(0, itemView, singletonList(stationInfoHeader));

        verify(imageOperations)
                .displayWithPlaceholder(eq(stationInfoHeader), any(ApiImageSize.class), eq(imageView(R.id.artwork)));
        verify(simpleBlurredImageLoader).displayBlurredArtwork(stationInfoHeader, imageView(R.id.blurred_background));
    }

    @Test
    public void bindDescriptionWithTwoArtists() {
        final List<StationInfoTrack> tracks = Arrays.asList(createStationInfoTrack(2, "Artist1"),
                                                            createStationInfoTrack(1, "Artist2"));
        StationInfoHeader stationInfoHeader = StationInfoHeader.from(getStationWithTracks(station, tracks));

        renderer.bindItemView(0, itemView, singletonList(stationInfoHeader));

        final SpannedString actual = (SpannedString) textView(R.id.station_desc).getText();
        assertEquals(actual.toString(), resources().getString(R.string.stations_home_description, "Artist1, Artist2"));
    }

    @Test
    public void bindDescriptionWithOneArtists() {
        final StationWithTracks stationWithTracks = getStationWithTracks(station);
        final StationInfoHeader stationInfoHeader = StationInfoHeader.from(stationWithTracks);
        final CharSequence expectedDesc = resources().getString(R.string.stations_home_description,
                                                                stationWithTracks.getMostPlayedArtists().get(0));

        renderer.bindItemView(0, itemView, singletonList(stationInfoHeader));

        final SpannedString actual = (SpannedString) textView(R.id.station_desc).getText();
        assertEquals(actual.toString(), expectedDesc);
    }

    private TextView textView(@IdRes int id) {
        return (TextView) itemView.findViewById(id);
    }

    private ImageView imageView(@IdRes int id) {
        return (ImageView) itemView.findViewById(id);
    }
}
