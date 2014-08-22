package com.soundcloud.android.tracks;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class TrackInfoFragment extends DialogFragment implements View.OnClickListener {

    private static final String EXTRA_URN = "Urn";

    @Inject TrackOperations trackOperations;
    @Inject EventBus eventBus;
    @Inject ImageOperations imageOperations;
    @Inject TrackInfoPresenter presenter;

    private Observable<PropertySet> loadTrack;
    private Subscription subscription;

    public static TrackInfoFragment create(TrackUrn trackUrn) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_URN, trackUrn);
        TrackInfoFragment fragment = new TrackInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Inject
    public TrackInfoFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYER_INFO.get());

        setStyle(STYLE_NO_FRAME, R.style.Theme_TrackInfoDialog);
        loadTrack = trackOperations.fullTrackWithUpdate(getArguments().<TrackUrn>getParcelable(EXTRA_URN))
                .observeOn(mainThread())
                .cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return presenter.create(getActivity().getLayoutInflater(), container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscription = loadTrack.subscribe(new TrackSubscriber());

        final int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.track_info_margin_horizontal);
        final int verticalPadding = getResources().getDimensionPixelSize(R.dimen.track_info_margin_vertical);
        view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        view.setBackgroundColor(getResources().getColor(R.color.artwork_overlay));
        view.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        // bug in the compatibility library : https://code.google.com/p/android/issues/detail?id=17423
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    private class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet propertySet) {
            final View view = getView();
            presenter.bind(view, propertySet);

            if (propertySet.contains(TrackProperty.DESCRIPTION)){
                presenter.bindDescription(view, propertySet);
            } else {
                presenter.showSpinner(view);
            }
        }

        @Override
        public void onError(Throwable e) {
            presenter.bindNoDescription(getView());
        }
    }
}
