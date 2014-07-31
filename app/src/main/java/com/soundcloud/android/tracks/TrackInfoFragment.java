package com.soundcloud.android.tracks;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackInfoFragment extends DialogFragment {

    private static final String EXTRA_URN = "Urn";

    @Inject TrackOperations trackOperations;
    @Inject EventBus eventBus;
    @Inject ImageOperations imageOperations;

    private Observable<TrackDetails> loadTrack;
    private Subscription subscription;
    private TextView titleView;
    private TextView usernameView;
    private TextView description;
    private ImageView artworkView;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        titleView = ((TextView) getView().findViewById(R.id.title));
        usernameView = ((TextView) getView().findViewById(R.id.username));
        description = (TextView) getView().findViewById(R.id.description);
        artworkView = (ImageView) getView().findViewById(R.id.artwork);

        subscription = loadTrack.subscribe(new TrackSubscriber());
    }

    protected void refreshMetaData(TrackDetails trackDetails) {

        titleView.setText(trackDetails.getTitle());
        titleView.setVisibility(View.VISIBLE);

        usernameView.setText(trackDetails.getCreator());
        usernameView.setVisibility(View.VISIBLE);

        description.setText(trackDetails.getDescription());
        description.setVisibility(View.VISIBLE);

        imageOperations.displayWithPlaceholder(trackDetails.getUrn(),
                ApiImageSize.getFullImageSize(getActivity().getResources()), artworkView);
    }

    private class TrackSubscriber extends DefaultSubscriber<TrackDetails> {
        @Override
        public void onNext(TrackDetails args) {
            super.onNext(args);
            Log.i("asdf", "TRACK RETURNED " + args);
            refreshMetaData(args);
        }
    }
}
