package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.EmptyListView;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

class SplitScreenController implements PlaylistDetailsController {

    private final PlaylistTracksAdapter mAdapter;
    private EmptyListView mEmptyView;
    private View mListViewContainer;

    SplitScreenController(ImageOperations imageOperations) {
        mAdapter = new PlaylistTracksAdapter(imageOperations);
    }

    @Override
    public ItemAdapter<Track> getAdapter() {
        return mAdapter;
    }

    @Override
    public void onViewCreated(View layout, Resources resources) {
        mEmptyView  = (EmptyListView) layout.findViewById(android.R.id.empty);
        mEmptyView.setMessageText(resources.getString(R.string.empty_playlist));

        mListViewContainer = layout.findViewById(R.id.container);
        ListView listView = (ListView) layout.findViewById(android.R.id.list);
        listView.setEmptyView(mEmptyView);
    }

    @Override
    public void setListShown(boolean show) {
        mListViewContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyView.setStatus(status);
    }
}
