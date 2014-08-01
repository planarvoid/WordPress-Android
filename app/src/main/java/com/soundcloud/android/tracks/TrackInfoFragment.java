package com.soundcloud.android.tracks;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;

import android.app.Dialog;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class TrackInfoFragment extends DialogFragment {

    private static final String EXTRA_URN = "Urn";

    @Inject TrackOperations trackOperations;
    @Inject EventBus eventBus;
    @Inject ImageOperations imageOperations;
    @Inject TrackItemPresenter trackItemPresenter;

    private Observable<PropertySet> loadTrack;
    private Subscription subscription;
    private TextView description;

    public static Bundle createArgs(TrackUrn trackUrn) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_URN, trackUrn);
        return args;
    }

    public static TrackInfoFragment create(Bundle args) {
        TrackInfoFragment fragment = new TrackInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Inject
    public TrackInfoFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @VisibleForTesting
    TrackInfoFragment(TrackOperations trackOperations, EventBus eventBus){
        this.trackOperations = trackOperations;
        this.eventBus = eventBus;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Theme_TrackInfoDialog);
        loadTrack = trackOperations.trackDetailsWithUpdate(getArguments().<TrackUrn>getParcelable(EXTRA_URN))
                .observeOn(mainThread())
                .cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.track_details_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        description = (TextView) getView().findViewById(R.id.description);
        subscription = loadTrack.subscribe(new TrackSubscriber());
    }

    protected void refreshMetaData(PropertySet propertySet) {
        trackItemPresenter.bindItemView(getView(), propertySet);
        description.setText(propertySet.get(TrackProperty.DESCRIPTION));
        description.setVisibility(View.VISIBLE);
    }

    private class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet args) {
            refreshMetaData(args);
        }
    }
}
