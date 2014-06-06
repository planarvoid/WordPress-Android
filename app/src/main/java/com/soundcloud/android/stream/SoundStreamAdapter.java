package com.soundcloud.android.stream;

import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;

import javax.inject.Inject;

class SoundStreamAdapter extends PagingItemAdapter<PropertySet> {

    static final int TRACK_ITEM_TYPE = 0;
    static final int PLAYLIST_ITEM_TYPE = 1;

    private TrackItemPresenter trackPresenter;

    @Inject
    SoundStreamAdapter(TrackItemPresenter trackPresenter, PlaylistItemPresenter playlistPresenter) {
        super(new CellPresenterEntity<PropertySet>(TRACK_ITEM_TYPE, trackPresenter),
                new CellPresenterEntity<PropertySet>(PLAYLIST_ITEM_TYPE, playlistPresenter));
        init(trackPresenter);
    }

    private void init(TrackItemPresenter trackPresenter) {
        this.trackPresenter = trackPresenter;
    }

    @Override
    public int getItemViewType(int position) {
        final int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else if (getItem(position).get(PlayableProperty.URN) instanceof TrackUrn) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    final class PlayQueueEventSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.getKind() == PlayQueueEvent.NEW_QUEUE || event.getKind() == PlayQueueEvent.TRACK_CHANGE) {
                trackPresenter.setPlayingTrack(event.getCurrentTrackUrn());
                notifyDataSetChanged();
            }
        }
    }
}
