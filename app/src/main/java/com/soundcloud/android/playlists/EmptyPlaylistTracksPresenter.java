package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EmptyPlaylistTracksPresenter implements CellPresenter<PropertySet>, EmptyViewAware {

    private int emptyViewStatus = EmptyView.Status.WAITING;

    @Inject
    EmptyPlaylistTracksPresenter() {
        // for Dagger.
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        Context context = parent.getContext();
        EmptyView emptyView = new EmptyViewBuilder()
                .withImage(R.drawable.empty_playlists)
                .withMessageText(context.getString(R.string.empty_playlist_title))
                .withSecondaryText(context.getString(R.string.empty_playlist_description))
                .build(context);
        emptyView.setPadding(0, ViewUtils.dpToPx(context, 48), 0, ViewUtils.dpToPx(context, 48));
        return emptyView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> items) {
        ((EmptyView) itemView).setStatus(emptyViewStatus);
    }

    public void setEmptyViewStatus(int emptyViewStatus) {
        this.emptyViewStatus = emptyViewStatus;
    }

}
