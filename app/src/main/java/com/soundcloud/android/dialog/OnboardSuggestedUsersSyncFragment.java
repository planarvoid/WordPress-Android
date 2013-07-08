package com.soundcloud.android.dialog;


import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.service.sync.SyncInitiator;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class OnboardSuggestedUsersSyncFragment extends SherlockFragment {

    private FollowingOperations mFollowingOperations;
    private Subscription mSubscription;

    public OnboardSuggestedUsersSyncFragment() {
    }

    public OnboardSuggestedUsersSyncFragment(@Nullable FollowingOperations followingOperations) {
        mFollowingOperations = followingOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (mFollowingOperations == null) {
            mFollowingOperations = new FollowingOperations();
        }

        mSubscription = mFollowingOperations.waitForActivities(getActivity()).subscribe(new FollowingsSyncObserver(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_onboarding_progress, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    private void finish(boolean success) {
        final Intent intent = new Intent(Actions.STREAM);
        intent.putExtra(Home.EXTRA_ONBOARDING_USERS_RESULT, success);
        startActivity(intent);
        getActivity().finish();
    }

    public static class FollowingsSyncObserver extends RxFragmentObserver<OnboardSuggestedUsersSyncFragment, Boolean> {

        public FollowingsSyncObserver(OnboardSuggestedUsersSyncFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(OnboardSuggestedUsersSyncFragment fragment, Boolean success) {
            fragment.finish(success);
        }

        @Override
        public void onCompleted(OnboardSuggestedUsersSyncFragment fragment) {
            // onNext might have already finished it
            if (!fragment.getActivity().isFinishing()) {
                fragment.finish(true);
            }
        }

        @Override
        public void onError(OnboardSuggestedUsersSyncFragment fragment, Exception error) {
            error.printStackTrace();
            // send sync adapter request for followings so retry logic will kick in
            SyncInitiator.pushFollowingsToApi(new AccountOperations(fragment.getActivity()).getSoundCloudAccount());
            fragment.finish(false);
        }
    }

}
