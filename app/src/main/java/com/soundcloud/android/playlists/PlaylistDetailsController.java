package com.soundcloud.android.playlists;

import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.EmptyViewAware;

import android.content.res.Resources;
import android.view.View;

public interface PlaylistDetailsController extends EmptyViewAware {

    ItemAdapter<Track, ? extends View> getAdapter();

    boolean hasContent();

    void onViewCreated(View layout, Resources resources);

    void setListShown(boolean show);

}
