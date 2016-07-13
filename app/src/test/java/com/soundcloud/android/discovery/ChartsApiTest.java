package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Collections;

public class ChartsApiTest extends AndroidUnitTest {

    @Mock
    private ApiClientRx apiClientRx;

    private ChartsApi chartsApi;
    private final TestSubscriber<ApiChart> chartSubscriber = TestSubscriber.create();
    private final ChartType chartType = ChartType.TOP;
    private final String genre = "all-music";

    @Before
    public void setup() {
        chartsApi = new ChartsApi(apiClientRx);
    }

    @Test
    public void returnsChartTracksForQueryParams() {
        final ApiChart expectedChart = createApiChart();
        when(apiClientRx.mappedResponse(
                argThat(isApiRequestTo("GET", "/charts").withQueryParam("type", chartType.value())
                                                        .withQueryParam("genre", genre)
                ), eq(ApiChart.class)))
                .thenReturn(Observable.just(expectedChart));

        chartsApi.chartTracks(chartType, genre).subscribe(chartSubscriber);

        assertThat(chartSubscriber.getOnNextEvents()).containsExactly(expectedChart);
    }

    @Test
    public void returnsChartTracksForNextLink() {
        String path = "/page2";
        String nextHref = "http://next-page" + path;
        final ApiChart expectedChart = createApiChart();
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", path)), eq(ApiChart.class)))
                .thenReturn(Observable.just(expectedChart));


        final Observable<ApiChart> apiChartObservable = chartsApi.chartTracks(nextHref);
        apiChartObservable.subscribe(chartSubscriber);

        assertThat(chartSubscriber.getOnNextEvents()).containsExactly(expectedChart);
    }

    private ApiChart createApiChart() {
        ApiTrack chartTrack = ModelFixtures.create(ApiTrack.class);
        return new ApiChart("title",
                            new Urn("soundcloud:chart:"+ this.genre),
                            chartType,
                            ChartCategory.MUSIC,
                            12345L,
                            new ModelCollection<>(Collections.singletonList(chartTrack)));
    }
}
