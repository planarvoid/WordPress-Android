package com.soundcloud.android.tracks;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.content.Intent;
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

    @Inject TrackRepository trackRepository;
    @Inject EventBus eventBus;
    @Inject ImageOperations imageOperations;
    @Inject TrackInfoPresenter presenter;

    private Observable<PropertySet> loadTrack;
    private Subscription subscription;

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
        loadTrack = trackRepository.fullTrackWithUpdate(getArguments().<Urn>getParcelable(EXTRA_URN))
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
        public void onNext(final PropertySet propertySet) {
            final View view = getView();
            presenter.bind(view, propertySet, new TrackInfoCommentClickListener(TrackInfoFragment.this, eventBus, propertySet));

            if (propertySet.contains(TrackProperty.DESCRIPTION)){
                presenter.bindDescription(view, propertySet);
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
        private final EventBus eventBus;
        private final PropertySet track;

        private final Handler collapseDelayHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_COMMENTS_OPEN));
            }
        };

        public TrackInfoCommentClickListener(TrackInfoFragment fragment, EventBus eventBus, PropertySet track) {
            trackInfoFragmentRef = new WeakReference<>(fragment);
            this.eventBus = eventBus;
            this.track = track;
        }

        @Override
        public void onCommentsClicked() {
            final TrackInfoFragment trackInfoFragment = trackInfoFragmentRef.get();
            if (trackInfoFragment != null && !trackInfoFragment.isDetached()){
                collapsePlayerOnDelay(trackInfoFragment.getActivity());
                trackInfoFragment.dismiss();
            }
        }

        private void collapsePlayerOnDelay(Context context) {
            subscribeToCollapsedEvent(context);
            collapseDelayHandler.sendEmptyMessageDelayed(0, COLLAPSE_DELAY_MILLIS);
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_COMMENTS_OPEN));
        }

        private void subscribeToCollapsedEvent(Context context) {
            eventBus.queue(EventQueue.PLAYER_UI)
                    .first(PlayerUIEvent.PLAYER_IS_COLLAPSED)
                    .subscribe(goToCommentsPage(context));
        }

        private DefaultSubscriber<PlayerUIEvent> goToCommentsPage(final Context context) {
            return new DefaultSubscriber<PlayerUIEvent>() {
                @Override
                public void onNext(PlayerUIEvent args) {
                    context.startActivity(new Intent(context, TrackCommentsActivity.class)
                            .putExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK, track));
                }
            };
        }
    }


}
