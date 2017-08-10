package com.soundcloud.android.sync.commands;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class PublishUserUpdateEventCommandTest {

    @Mock private EventBus eventBus;
    @Captor private ArgumentCaptor<UserChangedEvent> userChangedEventArgumentCaptor;

    private PublishUserUpdateEventCommand publishUserUpdateEventCommand;

    @Before
    public void setUp() throws Exception {
        publishUserUpdateEventCommand = new PublishUserUpdateEventCommand(eventBus);
    }

    @Test
    public void sendsConvertedPlaylistItem() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);

        publishUserUpdateEventCommand.call(Collections.singletonList(apiUser));

        verify(eventBus).publish(eq(EventQueue.USER_CHANGED), userChangedEventArgumentCaptor.capture());
        final UserChangedEvent changedEvent = userChangedEventArgumentCaptor.getValue();
        assertThat(changedEvent.changeMap().values()).containsExactly(User.fromApiUser(apiUser));
    }

    @Test
    public void doesNotSendEmptyEvent() throws Exception {
        publishUserUpdateEventCommand.call(Collections.emptyList());

        verify(eventBus, never()).publish(eq(EventQueue.USER_CHANGED), any(UserChangedEvent.class));
    }
}
