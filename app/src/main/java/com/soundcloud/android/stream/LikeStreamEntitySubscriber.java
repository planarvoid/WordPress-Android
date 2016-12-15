package com.soundcloud.android.stream;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;

class LikeStreamEntitySubscriber extends DefaultSubscriber<LikesStatusEvent> {
    private final StreamAdapter adapter;

    LikeStreamEntitySubscriber(StreamAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final LikesStatusEvent event) {
        for (int position = 0; position < adapter.getItems().size(); position++) {
            final StreamItem item = adapter.getItem(position);
            final Optional<ListItem> listItem = item.getListItem();
            if (listItem.isPresent() && event.likes().containsKey(listItem.get().getUrn())) {
                final StreamItem updatedStreamItem = item.copyWith(event.likes().get(listItem.get().getUrn()));
                adapter.setItem(position, updatedStreamItem);
            }
        }
    }
}
