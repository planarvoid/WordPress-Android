package com.soundcloud.android.stream;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StreamHighlightsOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRx apiClientRx;
    @Mock private FeatureFlags featureFlags;
    private StreamHighlightsOperations streamHighlightsOperations;

    @Before
    public void setUp() throws Exception {
        streamHighlightsOperations = new StreamHighlightsOperations(apiClientRx,
                                                                    featureFlags,
                                                                    Schedulers.immediate(),
                                                                    ModelFixtures.entityItemCreator());
        when(featureFlags.isEnabled(Flag.STREAM_HIGHLIGHTS)).thenReturn(true);
    }

    @Test
    public void returnsHighlightsWithFiveOrMoreResults() {
        List<ApiStreamItem> apiTracks = getApiTracks(5);
        ModelCollection<ApiStreamItem> value = new ModelCollection<>(apiTracks);

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.STREAM_HIGHLIGHTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.just(value));

        streamHighlightsOperations.highlights().test().assertValues(
                createHighlights(apiTracks)
        ).assertCompleted();
    }

    public StreamItem.StreamHighlights createHighlights(List<ApiStreamItem> suggestedTracks) {
        final List<TrackStreamItem> suggestedTrackItems = new ArrayList<>(suggestedTracks.size());
        for (ApiStreamItem apiStreamItem : suggestedTracks) {
            suggestedTrackItems.add(TrackStreamItem.create(ModelFixtures.trackItem(apiStreamItem.getTrack().get()),
                                                           new Date(apiStreamItem.getCreatedAtTime())));
        }
        return StreamItem.StreamHighlights.create(suggestedTrackItems);
    }

    @Test
    public void returnsNoHighlightsWithLessThanFiveResults() {
        List<ApiStreamItem> apiTracks = getApiTracks(4);
        ModelCollection<ApiStreamItem> value = new ModelCollection<>(apiTracks);

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.STREAM_HIGHLIGHTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.just(value));

        streamHighlightsOperations.highlights().test().assertNoValues().assertCompleted();
    }

    @NonNull
    private List<ApiStreamItem> getApiTracks(int count) {
        List<ApiStreamItem> apiStreamItems = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            apiStreamItems.add(ApiStreamItemFixtures.trackPost());
        }
        return apiStreamItems;
    }
}
