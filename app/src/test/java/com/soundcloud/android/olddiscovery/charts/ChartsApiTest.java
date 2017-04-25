package com.soundcloud.android.olddiscovery.charts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ChartsApiTest extends AndroidUnitTest {

    private ChartsApi chartsApi;

    private final TestObserver<ApiChart> chartSubscriber = TestObserver.create();
    private final ChartType chartType = ChartType.TOP;
    private final TypeToken<ApiChart<ApiTrack>> typeToken = new TypeToken<ApiChart<ApiTrack>>() {};

    @Mock
    private ApiClientRxV2 apiClientRx;

    @Before
    public void setup() {
        chartsApi = new ChartsApi(apiClientRx);
    }

    @Test
    public void returnsChartTracksForQueryParams() {
        final String genre = "all-music";
        final ApiChart<ApiTrack> expectedChart = ChartsFixtures.createApiChart(genre, chartType);

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/charts")
                .withQueryParam("type", chartType.value())
                .withQueryParam("genre", genre)), eq(typeToken)))
                .thenReturn(Single.just(expectedChart));

        chartsApi.chartTracks(chartType, genre).subscribe(chartSubscriber);

        chartSubscriber.assertResult(expectedChart);
    }
}
