package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.LocaleFormatter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;

import java.util.Collections;
import java.util.Locale;

public class DiscoveryCardSyncerTest extends AndroidUnitTest {

    @Mock private ApiClient apiClient;
    @Mock private DiscoveryWritableStorage discoveryWritableStorage;

    private DiscoveryCardSyncer syncer;

    @Before
    public void setUp() throws Exception {
        syncer = new DiscoveryCardSyncer(apiClient, discoveryWritableStorage, new LocaleFormatter(Locale.CANADA));
    }

    @Test
    public void syncReturnsFalseToForceBackoff() throws Exception {
        when(apiClient.fetchMappedResponse(MockitoHamcrest.argThat(isApiRequestTo("GET", ApiEndpoints.DISCOVERY_CARDS.path())), eq(ModelCollection.class)))
                .thenReturn(new ModelCollection<ApiDiscoveryCard>(Collections.emptyList()));

        assertThat(syncer.call()).isFalse();
    }
}
