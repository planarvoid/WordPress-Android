package com.soundcloud.android.stream;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PlaylistUrn;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import java.util.List;

public class SoundStreamFragment extends Fragment
        implements RefreshableListComponent<ConnectableObservable<Page<List<PropertySet>>>> {

    @VisibleForTesting
    static final String ONBOARDING_RESULT_EXTRA = "onboarding.result";

    @Inject SoundStreamOperations soundStreamOperations;
    @Inject SoundStreamAdapter adapter;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject PlaybackOperations playbackOperations;

    private ConnectableObservable<Page<List<PropertySet>>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public static SoundStreamFragment create(boolean onboardingSucceeded) {
        final Bundle args = new Bundle();
        args.putBoolean(ONBOARDING_RESULT_EXTRA, onboardingSucceeded);
        SoundStreamFragment fragment = new SoundStreamFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SoundStreamFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public ConnectableObservable<Page<List<PropertySet>>> buildObservable() {
        final ConnectableObservable<Page<List<PropertySet>>> observable =
                soundStreamOperations.existingStreamItems().observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<Page<List<PropertySet>>> observable) {
        this.observable = observable;
        this.connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public ConnectableObservable<Page<List<PropertySet>>> refreshObservable() {
        return soundStreamOperations.updatedStreamItems().observeOn(mainThread()).replay();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectObservable(buildObservable());
    }

    @Override
    public void onResume() {
        super.onResume();
        soundStreamOperations.updateLastSeen();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sound_stream_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listViewController.onViewCreated(this, observable, view, adapter, adapter);
        pullToRefreshController.onViewCreated(this, observable, adapter);
        adapter.onViewCreated();

        final EmptyView emptyView = listViewController.getEmptyView();
        emptyView.setImage(R.drawable.empty_stream);
        if (getArguments().getBoolean(ONBOARDING_RESULT_EXTRA)) {
            emptyView.setMessageText(R.string.list_empty_stream_message);
            emptyView.setActionText(R.string.list_empty_stream_action);
            emptyView.setButtonActions(new Intent(Actions.WHO_TO_FOLLOW));
        } else {
            emptyView.setMessageText(R.string.error_onboarding_fail);
        }
    }

    @Override
    public void onDestroyView() {
        adapter.onDestroyView();
        listViewController.onDestroyView();
        pullToRefreshController.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final PropertySet item = adapter.getItem(position);
        final Urn playableUrn = item.get(PlayableProperty.URN);
        if (playableUrn instanceof TrackUrn) {
            playbackOperations.playTracks(getActivity(), (TrackUrn) playableUrn,
                    soundStreamOperations.trackUrnsForPlayback(), position, Screen.SIDE_MENU_STREAM);
        } else if (playableUrn instanceof PlaylistUrn) {
            PlaylistDetailActivity.start(getActivity(), (PlaylistUrn) playableUrn, Screen.SIDE_MENU_STREAM);
        }
    }
}
