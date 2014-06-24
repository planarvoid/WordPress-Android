package com.soundcloud.android.playlists;

import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.view.View;

public interface PlaylistDetailsController extends EmptyViewAware {

    ItemAdapter<PropertySet> getAdapter();

    boolean hasContent();

    void onViewCreated(View layout, Resources resources);

    void setListShown(boolean show);

}
