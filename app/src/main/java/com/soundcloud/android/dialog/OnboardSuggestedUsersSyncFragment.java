package com.soundcloud.android.dialog;


import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.MainActivity;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.service.sync.SyncInitiator;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.RxFragmentObserver;
import rx.android.concurrency.AndroidSchedulers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class OnboardSuggestedUsersSyncFragment extends Fragment {

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
            mFollowingOperations = new FollowingOperations().observeOn(AndroidSchedulers.mainThread());
        }

        mSubscription = mFollowingOperations.waitForActivities(getActivity()).subscribe(new FollowingsSyncObserver(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout relativeLayout = new RelativeLayout(getActivity());
        relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final View view = inflater.inflate(R.layout.list_loading_item, null);

        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(view, params);
        return relativeLayout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    private void finish(boolean success) {
        final Intent intent = new Intent(Actions.STREAM);
        intent.putExtra(MainActivity.EXTRA_ONBOARDING_USERS_RESULT, success);
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
        public void onError(OnboardSuggestedUsersSyncFragment fragment, Throwable error) {
            error.printStackTrace();
            // send sync adapter request for followings so retry logic will kick in
            SyncInitiator.pushFollowingsToApi(new AccountOperations(fragment.getActivity()).getSoundCloudAccount());
            fragment.finish(false);
        }
    }

}
