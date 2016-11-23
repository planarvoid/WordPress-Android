package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
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
import java.util.List;

public class FetchUsersCommandTest extends AndroidUnitTest {

    private FetchUsersCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        command = new FetchUsersCommand(apiClient, 2);
    }

    @Test
    public void shouldResolveUrnsToFullUsersViaApiMobile() throws Exception {
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        final List<Urn> urns = Arrays.asList(users.get(0).getUrn(), users.get(1).getUrn());

        setupRequest(urns, users);

        Collection<PublicApiUser> result = command.with(urns).call();
        assertThat(result).isEqualTo(users);
    }

    @Test
    public void shouldResolveUrnsToFullUsersViaApiMobileInPages() throws Exception {
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 3);
        final List<Urn> urns = Arrays.asList(users.get(0).getUrn(), users.get(1).getUrn(), users.get(2).getUrn());

        setupRequest(urns.subList(0, 2), users.subList(0, 2));
        setupRequest(urns.subList(2, 3), users.subList(2, 3));

        Collection<PublicApiUser> result = command.with(urns).call();
        assertThat(result).isEqualTo(users);
    }

    @Test
    public void shouldIgnoreUrnsWithNegativeId() throws Exception {
        final List<PublicApiUser> users = ModelFixtures.create(PublicApiUser.class, 2);
        final List<Urn> urns = Collections.singletonList(users.get(0).getUrn());

        setupRequest(urns, users);

        Collection<PublicApiUser> result = command
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

    private void setupRequest(List<Urn> urns, List<PublicApiUser> users) throws Exception {
        final String joinedIds = Urns.toJoinedIds(urns, ",");
        when(apiClient.fetchMappedResponse(argThat(
                isPublicApiRequestTo("GET", ApiEndpoints.LEGACY_USERS.path())
                        .withQueryParam("ids", joinedIds)
                        .withQueryParam("linked_partitioning", "1")), isA(TypeToken.class)))
                .thenReturn(new ModelCollection<>(users));
    }
}
