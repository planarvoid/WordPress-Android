package com.soundcloud.android.tracks;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class TrackInfoFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = TrackInfoFragment.class.getSimpleName();
    private static final String EXTRA_URN = "Urn";
    private static final int COLLAPSE_DELAY_MILLIS = 300;

    @Inject TrackItemRepository trackRepository;
    @Inject EventBusV2 eventBus;
    @Inject ImageOperations imageOperations;
    @Inject TrackInfoPresenter presenter;
    @Inject NavigationExecutor navigationExecutor;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    private Observable<TrackItem> loadTrack;
    private Disposable disposable;

    public static TrackInfoFragment create(Urn trackUrn) {
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

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.PLAYER_INFO));

        setStyle(STYLE_NO_FRAME, R.style.Theme_TrackInfoDialog);
        loadTrack = trackRepository.fullTrackWithUpdate(getArguments().getParcelable(EXTRA_URN))
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
        disposable = loadTrack.subscribeWith(new TrackSubscriber());
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
        disposable.dispose();
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    private class TrackSubscriber extends DefaultObserver<TrackItem> {
        @Override
        public void onNext(final TrackItem trackItem) {
            final View view = getView();
            final Urn trackUrn = trackItem.getUrn();
            final TrackInfoCommentClickListener commentClickListener =
                    new TrackInfoCommentClickListener(TrackInfoFragment.this, eventBus, trackUrn, navigationExecutor);
            presenter.bind(view, trackItem, commentClickListener);

            if (trackItem.description().isPresent()) {
                presenter.bindDescription(view, trackItem);
            } else {
                presenter.showSpinner(view);
            }
        }

        @Override
        public void onError(Throwable e) {
            Log.e(TAG, "Error when loading track", e);
            presenter.bindNoDescription(getView());
        }
    }

    static class TrackInfoCommentClickListener implements TrackInfoPresenter.CommentClickListener {

        private final WeakReference<TrackInfoFragment> trackInfoFragmentRef;
        private final CollapseDelayHandler collapseDelayHandler;
        private final EventBusV2 eventBus;
        private final Urn trackurn;
        private final NavigationExecutor navigationExecutor;

        private static class CollapseDelayHandler extends Handler {
            private final EventBusV2 eventBus;

            private CollapseDelayHandler(EventBusV2 eventBus) {
                this.eventBus = eventBus;
            }

            @Override
            public void handleMessage(Message msg) {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
            }
        }

        public TrackInfoCommentClickListener(TrackInfoFragment fragment,
                                             EventBusV2 eventBus,
                                             Urn trackurn,
                                             NavigationExecutor navigationExecutor) {
            this.navigationExecutor = navigationExecutor;
            this.trackInfoFragmentRef = new WeakReference<>(fragment);
            this.eventBus = eventBus;
            this.trackurn = trackurn;
            this.collapseDelayHandler = new CollapseDelayHandler(eventBus);
        }

        @Override
        public void onCommentsClicked() {
            final TrackInfoFragment trackInfoFragment = trackInfoFragmentRef.get();
            if (trackInfoFragment != null && !trackInfoFragment.isDetached()) {
                collapsePlayerOnDelay(trackInfoFragment.getActivity());
                trackInfoFragment.dismiss();
            }
        }

        private void collapsePlayerOnDelay(Context context) {
            subscribeToCollapsedEvent(context);
            collapseDelayHandler.sendEmptyMessageDelayed(0, COLLAPSE_DELAY_MILLIS);
        }

        private void subscribeToCollapsedEvent(final Context context) {
            eventBus.queue(EventQueue.PLAYER_UI)
                    .filter(PlayerUIEvent.PLAYER_IS_COLLAPSED_V2)
                    .firstOrError()
                    .subscribe(LambdaSingleObserver.onNext(args -> navigationExecutor.openTrackComments(context, trackurn)));
        }

    }
}
