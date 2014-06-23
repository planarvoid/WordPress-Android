package com.soundcloud.android.associations;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.collections.ScListView;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.NotNull;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class LikesListFragment extends ScListFragment {

    @Inject EventBus eventBus;
    @Inject PlaybackOperations playbackOperations;
    @Inject SoundAssociationOperations soundAssociationOperations;

    private ViewGroup headerView;
    private Subscription fetchIdsSubscription = Subscriptions.empty();

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
        fetchIdsSubscription = soundAssociationOperations.getLikedTracksIds()
                .observeOn(mainThread()).subscribe(new LikedIdsSubscriber());
    }

    private void updateShuffleHeader(@NotNull final List<Long> likedTrackIds) {
        View shuffleButton = getView().findViewById(R.id.shuffle_btn);
        if (likedTrackIds.size() <= 1) {
            shuffleButton.setVisibility(View.GONE);
        } else {
            shuffleButton.setVisibility(View.VISIBLE);
        }

        ((TextView) headerView.findViewById(R.id.shuffle_txt)).setText(getHeaderText(likedTrackIds.size()));
        headerView.findViewById(R.id.shuffle_btn).setEnabled(likedTrackIds.size() > 1);

        headerView.findViewById(R.id.shuffle_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playbackOperations.playFromIdListShuffled(getActivity(), likedTrackIds, Screen.SIDE_MENU_LIKES);
                eventBus.publish(EventQueue.UI, UIEvent.fromShuffleMyLikes());
            }
        });
    }

    private String getHeaderText(int trackCount) {
        if (trackCount == 0) {
            return getString(R.string.number_of_liked_tracks_you_liked_zero);
        } else {
            return getResources().getQuantityString(R.plurals.number_of_liked_tracks_you_liked, trackCount, trackCount);
        }
    }

    private class LikedIdsSubscriber extends DefaultSubscriber<List<Long>> {
        @Override
        public void onNext(List<Long> trackIds) {
            if (isAdded()) {
                updateShuffleHeader(trackIds);
            }
        }
    }

}
