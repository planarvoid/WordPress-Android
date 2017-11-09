package com.soundcloud.android.sync.commands;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collection;

public class PublishUserUpdateEventCommand extends PublishUpdateEventCommand<UserRecord> {
    private final EventBus eventBus;

    @Inject
    public PublishUserUpdateEventCommand(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call(Collection<UserRecord> input) {
        if (input.size() > 0) {
            final Collection<User> userItems = MoreCollections.transform(input, User::fromUserRecord);
            eventBus.publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(userItems));
            return true;
        }
        return false;
    }
}
