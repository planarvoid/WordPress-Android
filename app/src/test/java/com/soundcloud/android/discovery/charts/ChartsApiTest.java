package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

public class ChartsApiTest extends AndroidUnitTest {

    private final TypeToken<ApiChart<ApiTrack>> typeToken = new TypeToken<ApiChart<ApiTrack>>() {
    };
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
        final ApiChart<ApiTrack> expectedChart = ChartsFixtures.createApiChart(genre, chartType);
        when(apiClientRx.mappedResponse(
                argThat(isApiRequestTo("GET", "/charts").withQueryParam("type", chartType.value())
                                                                .withQueryParam("genre", genre)
                ), eq(typeToken)))
                .thenReturn(Observable.just(expectedChart));

        chartsApi.chartTracks(chartType, genre).subscribe(chartSubscriber);

        assertThat(chartSubscriber.getOnNextEvents()).containsExactly(expectedChart);
    }
}
