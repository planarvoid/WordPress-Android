package com.soundcloud.android.accounts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class FetchMeCommandTest extends AndroidUnitTest {

    private FetchMeCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchMeCommand(apiClient);
    }

    @Test
    public void fetchesMeViaApi() throws Exception {
        final AutoValue_Me me = new AutoValue_Me(ModelFixtures.create(ApiUser.class));

        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.ME.path())), isA(TypeToken.class)))
                .thenReturn(me);

        assertThat(command.call(null)).isEqualTo(me);
    }

    @Test
    public void returnsNullOnException() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.ME.path())), isA(TypeToken.class)))
                .thenThrow(new IOException());

        assertThat(command.call(null)).isNull();
    }

}
