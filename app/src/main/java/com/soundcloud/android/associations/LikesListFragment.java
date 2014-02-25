package com.soundcloud.android.associations;

import static rx.android.observables.AndroidObservable.fromFragment;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.collections.ScListView;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultObserver;
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

    @Inject
    EventBus mEventBus;
    @Inject
    PlaybackOperations mPlaybackOperations;
    @Inject
    SoundAssociationOperations mSoundAssociationOperations;

    private ViewGroup mHeaderView;
    private Subscription mFetchIdsSubscription = Subscriptions.empty();

    public LikesListFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setArguments(createArguments(Content.ME_LIKES.uri, R.string.side_menu_likes, Screen.SIDE_MENU_LIKES));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        mHeaderView = (ViewGroup) inflater.inflate(R.layout.likes_shuffle_header, null, false);

        final ScListView listView = getScListView();
        if (listView != null) {
            listView.getRefreshableView().addHeaderView(mHeaderView);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshLikeIds();
    }

    @Override
    public void onDestroy() {
        mFetchIdsSubscription.unsubscribe();
        super.onDestroy();
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
        mFetchIdsSubscription = fromFragment(this, mSoundAssociationOperations.getLikedTracksIds()).subscribe(mLikedTrackIdsObserver);
    }

    private void updateShuffleHeader(@NotNull final List<Long> likedTrackIds) {
        final String likeMessage;
        if (likedTrackIds.isEmpty()) {
            likeMessage = getString(R.string.number_of_liked_tracks_you_liked_zero);
        } else {
            likeMessage = getResources().getQuantityString(R.plurals.number_of_liked_tracks_you_liked, likedTrackIds.size(), likedTrackIds.size());
        }

        ((TextView) mHeaderView.findViewById(R.id.shuffle_txt)).setText(likeMessage);
        mHeaderView.findViewById(R.id.shuffle_btn).setEnabled(likedTrackIds.size() > 1);

        mHeaderView.findViewById(R.id.shuffle_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaybackOperations.playFromIdListShuffled(getActivity(), likedTrackIds, Screen.SIDE_MENU_LIKES);
                mEventBus.publish(EventQueue.UI, UIEvent.fromShuffleMyLikes());
            }
        });
    }

    private final DefaultObserver<List<Long>> mLikedTrackIdsObserver = new DefaultObserver<List<Long>>() {
        @Override
        public void onNext(List<Long> trackIds) {
            updateShuffleHeader(trackIds);
        }
    };

}
