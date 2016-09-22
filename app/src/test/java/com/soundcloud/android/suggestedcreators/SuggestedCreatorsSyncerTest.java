package com.soundcloud.android.suggestedcreators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreators;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SuggestedCreatorsSyncerTest extends AndroidUnitTest {

    private SuggestedCreatorsSyncer syncer;
    @Mock private ApiClient apiClient;
    @Mock private StoreSuggestedCreatorsCommand storeSuggestedCreatorsCommand;
    @Mock private WriteResult positiveWriteResult;

    @Before
    public void setup() {
        syncer = new SuggestedCreatorsSyncer(apiClient, storeSuggestedCreatorsCommand);
        when(positiveWriteResult.success()).thenReturn(true);
    }

    @Test
    public void writesApiResultToDatabase() throws Exception {
        final ApiSuggestedCreators apiSuggestedCreators = SuggestedCreatorsFixtures.createApiSuggestedCreators();
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(apiSuggestedCreators);
        when(storeSuggestedCreatorsCommand.call(apiSuggestedCreators)).thenReturn(positiveWriteResult);

        assertThat(syncer.call()).isTrue();
        verify(apiClient).fetchMappedResponse(any(ApiRequest.class), eq(TypeToken.of(ApiSuggestedCreators.class)));
        verify(storeSuggestedCreatorsCommand).call(apiSuggestedCreators);
    }
}
