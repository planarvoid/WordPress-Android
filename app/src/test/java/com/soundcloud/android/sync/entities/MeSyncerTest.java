package com.soundcloud.android.sync.entities;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.accounts.MeStorage;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.sync.me.MeSyncer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class MeSyncerTest extends AndroidUnitTest {

    private MeSyncer meSyncer;

    @Mock private ApiClient apiClient;
    @Mock private MeStorage meStorage;

    private TestEventBusV2 eventBus = new TestEventBusV2();
    private Me me;

    @Before
    public void setup() {
        me = Me.create(UserFixtures.apiUser(), ModelFixtures.create(Configuration.class), false);

        meSyncer = new MeSyncer(apiClient, eventBus, meStorage);
    }

    @Test
    public void returnsTrueOnSuccess() throws Exception {
        setupSuccessfulFetch();

        assertThat(meSyncer.call()).isTrue();
    }

    @Test
    public void storesMeUserInStorage() throws Exception {
        setupSuccessfulFetch();

        meSyncer.call();

        verify(meStorage).store(me);
    }

    @Test
    public void sendsMeUserEntityStateChangeEvent() throws Exception {
        setupSuccessfulFetch();

        meSyncer.call();

        assertThat(eventBus.lastEventOn(EventQueue.USER_CHANGED)).isEqualTo(UserChangedEvent.forUpdate(User.fromApiUser(me.getUser())));
    }

    private void setupSuccessfulFetch() throws ApiRequestException, IOException, ApiMapperException {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.ME.path())), isA(TypeToken.class)))
                .thenReturn(me);
    }

}
