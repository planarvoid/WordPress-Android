package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

public final class LikeEntityListObserver extends DefaultObserver<LikesStatusEvent> {
    private final RecyclerItemAdapter adapter;

    public LikeEntityListObserver(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final LikesStatusEvent event) {
        final Iterable<LikeableItem> filtered = Iterables.filter(adapter.getItems(), LikeableItem.class);
        for (LikeableItem item : filtered) {
            final Optional<LikesStatusEvent.LikeStatus> likeStatus = event.likeStatusForUrn(item.getUrn());
            if (likeStatus.isPresent()) {
                final int position = adapter.getItems().indexOf(item);
                adapter.getItems().set(position, item.updatedWithLike(likeStatus.get()));
                adapter.notifyItemChanged(position);
            }
        }
    }
}
