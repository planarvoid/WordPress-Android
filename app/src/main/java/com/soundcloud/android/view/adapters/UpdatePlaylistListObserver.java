package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.java.collections.Iterables;

public final class UpdatePlaylistListObserver extends DefaultObserver<PlaylistChangedEvent> {
    private final RecyclerItemAdapter adapter;

    public UpdatePlaylistListObserver(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final PlaylistChangedEvent event) {
        final Iterable<UpdatablePlaylistItem> filtered = Iterables.filter(adapter.getItems(), UpdatablePlaylistItem.class);
        for (UpdatablePlaylistItem item : filtered) {
            final Urn urn = item.getUrn();
            if (event.changeMap().containsKey(urn)) {
                final int position = adapter.getItems().indexOf(item);
                if (adapter.getItems().size() > position) {
                    adapter.getItems().set(position, event.apply(item));
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
