package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Action1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class LikesListFragment extends ScListFragment {

    public static ScListFragment newInstance() {
        LikesListFragment likesListFragment = new LikesListFragment();
        likesListFragment.setArguments(createArguments(Content.ME_LIKES.uri, R.string.side_menu_likes, Screen.SIDE_MENU_LIKES));
        return likesListFragment;
    }

    private ViewGroup mHeaderView;
    private List<Long> mLikes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshLikeIds();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        mHeaderView = (ViewGroup) inflater.inflate(R.layout.likes_shuffle_header, null, false);
        mHeaderView.findViewById(R.id.shuffle_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PlaybackOperations().playFromIdListShuffled(getActivity(), mLikes, Screen.SIDE_MENU_LIKES);
            }
        });

        getScListView().getRefreshableView().addHeaderView(mHeaderView);
        updateShuffleHeader();
        return view;
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        refreshLikeIds();
    }

    private void refreshLikeIds() {
        new SoundAssociationStorage().getTrackLikesAsIdsAsync()
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<Long>>() {
            @Override
            public void call(List<Long> longs) {
                mLikes = longs;
                updateShuffleHeader();
            }
        });
    }

    private void updateShuffleHeader(){
        if (mLikes != null){
            final String likeMessage = mLikes.isEmpty() ? getString(R.string.number_of_liked_tracks_you_liked_zero) :
                    getResources().getQuantityString(R.plurals.number_of_liked_tracks_you_liked, mLikes.size(), mLikes.size());

            ((TextView) mHeaderView.findViewById(R.id.shuffle_txt)).setText(likeMessage);
            mHeaderView.findViewById(R.id.shuffle_btn).setEnabled(mLikes.size() > 1);
        } else {

        }
    }
}
