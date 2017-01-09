package com.soundcloud.android.stream;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.List;

public class StreamHighlightsOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRx apiClientRx;
    @Mock private FeatureFlags featureFlags;
    private StreamHighlightsOperations streamHighlightsOperations;

    @Before
    public void setUp() throws Exception {
        streamHighlightsOperations = new StreamHighlightsOperations(apiClientRx, featureFlags, Schedulers.immediate());
        when(featureFlags.isEnabled(Flag.STREAM_HIGHLIGHTS)).thenReturn(true);
    }

    @Test
    public void returnsHighlightsWithFiveOrMoreResults() {
        List<ApiTrack> apiTracks = getApiTracks(5);
        ModelCollection<ApiTrack> value = new ModelCollection<>(apiTracks);

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.STREAM_HIGHLIGHTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.just(value));

        streamHighlightsOperations.highlights().test().assertValues(
                StreamItem.StreamHighlights.create(apiTracks)
        ).assertCompleted();
    }

    @Test
    public void returnsNoHighlightsWithLessThanFiveResults() {
        List<ApiTrack> apiTracks = getApiTracks(4);
        ModelCollection<ApiTrack> value = new ModelCollection<>(apiTracks);

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.STREAM_HIGHLIGHTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.just(value));

        streamHighlightsOperations.highlights().test().assertNoValues().assertCompleted();
    }

    @NonNull
    private List<ApiTrack> getApiTracks(int count) {
        return ModelFixtures.create(ApiTrack.class, count);
    }
}
