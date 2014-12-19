package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

public class TrackLikesAdapter extends ItemAdapter<PropertySet> implements ReactiveAdapter<Iterable<PropertySet>> {

    private final CallsiteToken callsiteToken = CallsiteToken.build();

    @Inject
    public TrackLikesAdapter(TrackItemPresenter trackPresenter) {
        super(trackPresenter);
    }

    @Override
    public void onNext(Iterable<PropertySet> propertySets) {
        for (PropertySet propertySet : propertySets) {
            addItem(propertySet);
        }
    }

    @Override
    public void onCompleted() {
        notifyDataSetChanged();
    }

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e, callsiteToken);
    }
}
