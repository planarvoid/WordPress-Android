package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

public final class RepostEntityListSubscriber extends DefaultSubscriber<RepostsStatusEvent> {
    private final RecyclerItemAdapter adapter;

    public RepostEntityListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final RepostsStatusEvent event) {
        final Iterable<RepostableItem> filtered = Iterables.filter(adapter.getItems(), RepostableItem.class);
        for (RepostableItem item : filtered) {
            final Optional<RepostsStatusEvent.RepostStatus> repostStatus = event.repostStatusForUrn(item.getUrn());
            if (repostStatus.isPresent()) {
                final int position = adapter.getItems().indexOf(item);
                adapter.getItems().set(position, item.updatedWithRepost(repostStatus.get()));
                adapter.notifyItemChanged(position);
            }
        }
    }
}
