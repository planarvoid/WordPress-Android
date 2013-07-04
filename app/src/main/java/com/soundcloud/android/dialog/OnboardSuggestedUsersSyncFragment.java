package com.soundcloud.android.dialog;


import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.service.sync.SyncStateManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class OnboardSuggestedUsersSyncFragment extends SherlockFragment {

    private SyncStateManager mSyncStateManager;
    private FollowingOperations mFollowingOperations;

    public OnboardSuggestedUsersSyncFragment() {
        this(new SyncStateManager(), null);
    }

    public OnboardSuggestedUsersSyncFragment(SyncStateManager syncStateManager, FollowingOperations followingOperations) {
        mSyncStateManager = syncStateManager;
        mFollowingOperations = followingOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (mFollowingOperations == null){
            mFollowingOperations = new FollowingOperations();
        }
        sendFollowingsPush();
    }

    private void sendFollowingsPush() {
        mFollowingOperations.pushFollowings(getActivity()).subscribe(new FollowingsSyncObserver(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_loading_item, container, false);
    }

    private void finish(boolean success) {
        mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM);
        final Intent intent = new Intent(Actions.STREAM);
        intent.putExtra(Home.EXTRA_ONBOARDING_USERS_RESULT, success);
        startActivity(intent);
        getSherlockActivity().finish();
    }

    public static class FollowingsSyncObserver extends RxFragmentObserver<OnboardSuggestedUsersSyncFragment, Void> {

        public FollowingsSyncObserver(OnboardSuggestedUsersSyncFragment fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(OnboardSuggestedUsersSyncFragment fragment) {
            fragment.finish(true);
        }

        @Override
        public void onError(OnboardSuggestedUsersSyncFragment fragment, Exception error) {
            fragment.finish(false);
        }
    }

}
