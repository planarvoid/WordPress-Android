package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Iterables;

public final class UpdateUserListSubscriber extends DefaultSubscriber<UserChangedEvent> {
    private final RecyclerItemAdapter adapter;

    public UpdateUserListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final UserChangedEvent event) {
        final Iterable<UserItem> filtered = Iterables.filter(adapter.getItems(), UserItem.class);
        for (UserItem item : filtered) {
            final Urn urn = item.getUrn();
            if (event.changeMap().containsKey(urn)) {
                final int position = adapter.getItems().indexOf(item);
                if (adapter.getItems().size() > position) {
                    ((UserItem) adapter.getItem(position)).updateWithUser(event.changeMap().get(urn));
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
