package com.soundcloud.android.discovery.newforyou;

import static com.soundcloud.android.discovery.newforyou.NewForYouItem.Kind.HEADER;
import static com.soundcloud.android.discovery.newforyou.NewForYouItem.Kind.TRACK;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
        for (NewForYouItem newForYouItem : getItems()) {
            if (newForYouItem.isTrack()) {
                final TrackItem track = ((NewForYouItem.NewForYouTrackItem) newForYouItem).track();

                if (track.getUrn() == currentlyPlayingUrn) {
                    track.setIsPlaying(true);
                } else {
                    track.setIsPlaying(false);
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }
}
