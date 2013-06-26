package com.soundcloud.android.dialog;


import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.service.sync.SyncOperations;
import com.soundcloud.android.service.sync.SyncStateManager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class OnboardSuggestedUsersSyncFragment extends SherlockFragment {

    public static final int MAX_RETRIES = 3;

    private int mTriesRemaining = MAX_RETRIES;
    private SyncStateManager mSyncStateManager;
    private SyncOperations mSyncOperations;

    public OnboardSuggestedUsersSyncFragment() {
        this(new SyncStateManager(), null);
    }
    public OnboardSuggestedUsersSyncFragment(SyncStateManager syncStateManager, SyncOperations syncOperations) {
        mSyncStateManager = syncStateManager;
        mSyncOperations = syncOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (mSyncOperations == null){
            mSyncOperations = new SyncOperations(getActivity());
        }
        sendFollowingsPush();
    }

    private boolean sendFollowingsPush() {
        if (mTriesRemaining-- > 0){
            mSyncOperations.pushFollowings().subscribe(new FollowingsSyncObserver(this));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_loading_item, container, false);
    }

    private void finish() {
        mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM);
        startActivity(new Intent(Actions.STREAM));

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    public static class FollowingsSyncObserver extends RxFragmentObserver<OnboardSuggestedUsersSyncFragment, Void> {

        public FollowingsSyncObserver(OnboardSuggestedUsersSyncFragment fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(OnboardSuggestedUsersSyncFragment fragment) {
            fragment.finish();
        }

        @Override
        public void onError(OnboardSuggestedUsersSyncFragment fragment, Exception error) {
            if (fragment.sendFollowingsPush()) {
                Toast.makeText(fragment.getActivity(), "Error pushing some of your followings", Toast.LENGTH_LONG).show();
                fragment.finish();
            }
        }
    }

}
