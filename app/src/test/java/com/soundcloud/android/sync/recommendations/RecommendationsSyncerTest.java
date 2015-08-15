package com.soundcloud.android.sync.recommendations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class RecommendationsSyncerTest extends AndroidUnitTest { // because of Uri class

    private RecommendationsSyncer recommendationsSyncer;

    @Mock private ApiClient apiClient;
    @Mock private StoreRecommendationsCommand storeRecommendationsCommand;

    @Before
    public void setUp() throws Exception {
        recommendationsSyncer = new RecommendationsSyncer(apiClient, storeRecommendationsCommand);
    }

    @Test
    public void writeFromApiToRecommendationsStorage() throws Exception {
        final ModelCollection<ApiRecommendation> recommendations = new ModelCollection<>(Collections.<ApiRecommendation>emptyList());
        final WriteResult writeResult = mock(WriteResult.class);

        when(apiClient.fetchMappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.RECOMMENDATIONS.path())), isA(TypeToken.class))).thenReturn(recommendations);
        when(storeRecommendationsCommand.call(recommendations)).thenReturn(writeResult);

        recommendationsSyncer.call();

        verify(storeRecommendationsCommand).call(recommendations);
        verify(writeResult).success();
        verifyNoMoreInteractions(storeRecommendationsCommand);
    }
}