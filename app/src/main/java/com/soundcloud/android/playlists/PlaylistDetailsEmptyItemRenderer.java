package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyStatus;
import com.soundcloud.android.view.LoadingTracksLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class PlaylistDetailsEmptyItemRenderer implements CellRenderer<PlaylistDetailEmptyItem> {

    @Inject
    PlaylistDetailsEmptyItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_details_emptyview, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailEmptyItem> items) {
        View loading = ButterKnife.findById(itemView, R.id.loading);
        View serverError = ButterKnife.findById(itemView, R.id.server_error);
        View connectionError = ButterKnife.findById(itemView, R.id.connection_error);
        View noTracks = ButterKnife.findById(itemView, R.id.no_tracks);

        EmptyStatus emptyStatus = items.get(position).getEmptyStatus();

        switch (emptyStatus) {
            case WAITING:
                ViewUtils.setGone(Arrays.asList(serverError, connectionError, noTracks));
                ViewUtils.setVisible(Collections.singleton(loading));
                ButterKnife.<LoadingTracksLayout>findById(itemView, R.id.loading).start();
                break;
            case CONNECTION_ERROR:
                ViewUtils.setGone(Arrays.asList(loading, serverError, noTracks));
                ViewUtils.setVisible(Collections.singleton(connectionError));
                break;
            case SERVER_ERROR:
                ViewUtils.setGone(Arrays.asList(loading, connectionError, noTracks));
                ViewUtils.setVisible(Collections.singleton(serverError));
                break;
            case OK:
                ViewUtils.setGone(Arrays.asList(loading, serverError, connectionError));
                ViewUtils.setVisible(Collections.singleton(noTracks));
                ButterKnife.findById(itemView, R.id.empty_playlist_owner_message)
                           .setVisibility(items.get(position).isOwner() ? View.VISIBLE : View.GONE);
                break;
        }
    }
}
