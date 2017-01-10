package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collection;

public class PublishUserUpdateEventCommand extends PublishUpdateEventCommand<ApiUser> {
    private final EventBus eventBus;

    @Inject
    public PublishUserUpdateEventCommand(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call(Collection<ApiUser> input) {
        if (input.size() > 0) {
            final Collection<UserItem> userItems = MoreCollections.transform(input, UserItem::from);
            eventBus.publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(userItems));
            return true;
        }
        return false;
    }
}
