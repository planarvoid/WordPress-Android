package com.soundcloud.android.olddiscovery.newforyou;

import static com.soundcloud.android.olddiscovery.newforyou.NewForYouItem.Kind.HEADER;
import static com.soundcloud.android.olddiscovery.newforyou.NewForYouItem.Kind.TRACK;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouItem.NewForYouTrackItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class NewForYouAdapter extends RecyclerItemAdapter<NewForYouItem, RecyclerView.ViewHolder> implements PlayingTrackAware {

    NewForYouAdapter(NewForYouHeaderRenderer.Listener headerItemListener,
                     TrackItemRenderer.Listener trackItemListener,
                     @Provided NewForYouHeaderRendererFactory headerRendererFactory,
                     @Provided NewForYouTrackRendererFactory trackRendererFactory) {
        super(new CellRendererBinding<>(HEADER.ordinal(), headerRendererFactory.create(headerItemListener)),
              new CellRendererBinding<>(TRACK.ordinal(), trackRendererFactory.create(trackItemListener))
        );
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < items.size(); i++) {
            NewForYouItem newForYouItem = items.get(i);
            if (newForYouItem.isTrack()) {
                final TrackItem track = ((NewForYouTrackItem) newForYouItem).track();
                final boolean isPlaying = track.getUrn().equals(currentlyPlayingUrn);
                final TrackItem trackItem = track.withPlayingState(isPlaying);
                items.set(i, NewForYouTrackItem.create(newForYouItem.newForYou(), trackItem));
            }
        }

        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }
}