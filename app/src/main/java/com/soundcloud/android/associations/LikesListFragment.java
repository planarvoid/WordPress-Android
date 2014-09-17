package com.soundcloud.android.associations;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.collections.ScListView;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackUrn;
import org.jetbrains.annotations.NotNull;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class LikesListFragment extends ScListFragment {

    @Inject EventBus eventBus;
    @Inject PlaybackOperations playbackOperations;
    @Inject SoundAssociationOperations soundAssociationOperations;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private ViewGroup headerView;
    private Subscription fetchIdsSubscription = Subscriptions.empty();
    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.UI_TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };;

    public LikesListFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setArguments(createArguments(Content.ME_LIKES.uri, R.string.side_menu_likes, Screen.SIDE_MENU_LIKES));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        headerView = (ViewGroup) inflater.inflate(R.layout.likes_shuffle_header, null, false);

        final ScListView listView = getScListView();
        if (listView != null) {
            listView.addHeaderView(headerView, null, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLikeIds();
    }

    @Override
    public void onDestroyView() {
        fetchIdsSubscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        refreshLikeIds();
    }

    @Override
    protected void doneRefreshing() {
        super.doneRefreshing();
        refreshLikeIds();
    }

    private void refreshLikeIds() {
        fetchIdsSubscription = soundAssociationOperations
                .getLikedTracks()
                .observeOn(mainThread()).subscribe(new LikedIdsSubscriber());
    }

    private void updateShuffleHeader(@NotNull final List<TrackUrn> likedTracks) {
        View shuffleButton = getView().findViewById(R.id.shuffle_btn);
        if (likedTracks.size() <= 1) {
            shuffleButton.setVisibility(View.GONE);
        } else {
            shuffleButton.setVisibility(View.VISIBLE);
        }

        ((TextView) headerView.findViewById(R.id.shuffle_txt)).setText(getHeaderText(likedTracks.size()));
        headerView.findViewById(R.id.shuffle_btn).setEnabled(likedTracks.size() > 1);

        headerView.findViewById(R.id.shuffle_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playbackOperations
                                .playTracksShuffled(likedTracks, new PlaySessionSource(Screen.SIDE_MENU_LIKES))
                                .doOnCompleted(sendShuffleLikesAnalytics)
                                .subscribe(subscriberProvider.get());
                    }
                }
        );
    }

    private String getHeaderText(int trackCount) {
        if (trackCount == 0) {
            return getString(R.string.number_of_liked_tracks_you_liked_zero);
        } else {
            return getResources().getQuantityString(R.plurals.number_of_liked_tracks_you_liked, trackCount, trackCount);
        }
    }

    private class LikedIdsSubscriber extends DefaultSubscriber<List<TrackUrn>> {
        @Override
        public void onNext(List<TrackUrn> tracks) {
            if (isAdded()) {
                updateShuffleHeader(tracks);
            }
        }
    }


}
