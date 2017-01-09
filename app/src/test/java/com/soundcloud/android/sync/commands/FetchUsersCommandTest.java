package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchUsersCommandTest extends AndroidUnitTest {

    private FetchUsersCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        command = new FetchUsersCommand(apiClient, 2);
    }

    @Test
    public void shouldResolveUrnsToFullUsersViaApiMobile() throws Exception {
        final List<ApiUser> users = ModelFixtures.create(ApiUser.class, 2);
        final List<Urn> urns = Arrays.asList(users.get(0).getUrn(), users.get(1).getUrn());

        setupRequest(urns, users);

        Collection<ApiUser> result = command.with(urns).call();
        assertThat(result).isEqualTo(users);
    }

    @Test
    public void shouldResolveUrnsToFullUsersViaApiMobileInPages() throws Exception {
        final List<ApiUser> users = ModelFixtures.create(ApiUser.class, 3);
        final List<Urn> urns = Arrays.asList(users.get(0).getUrn(), users.get(1).getUrn(), users.get(2).getUrn());

        setupRequest(urns.subList(0, 2), users.subList(0, 2));
        setupRequest(urns.subList(2, 3), users.subList(2, 3));

        Collection<ApiUser> result = command.with(urns).call();
        assertThat(result).isEqualTo(users);
    }

    @Test
    public void shouldIgnoreUrnsWithNegativeId() throws Exception {
        final List<ApiUser> users = ModelFixtures.create(ApiUser.class, 2);
        final List<Urn> urns = Collections.singletonList(users.get(0).getUrn());

        setupRequest(urns, users);

        Collection<ApiUser> result = command
                .with(Arrays.asList(users.get(0).getUrn(), Urn.forUser(-10)))
                .call();
        assertThat(result).isEqualTo(users);
    }

    @Test
    public void shouldAvoidApiCallWhenAllUrnsAreWithNegativeId() throws Exception {
        final List<Urn> urns = Arrays.asList(Urn.forUser(-100), Urn.forUser(-200));

        command.with(urns).call();

        verifyZeroInteractions(apiClient);
    }

    private void setupRequest(List<Urn> urns, List<ApiUser> users) throws Exception {
        Map<String, List<String>> body = new HashMap<>();
        body.put("urns", Urns.toString(urns));

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.USERS_FETCH.path()).withContent(body)), isA(TypeToken.class)))
                .thenReturn(new ModelCollection<>(users));
    }
}
