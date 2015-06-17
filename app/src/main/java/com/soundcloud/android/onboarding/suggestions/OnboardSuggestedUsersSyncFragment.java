package com.soundcloud.android.onboarding.suggestions;


import static rx.android.observables.AndroidObservable.bindFragment;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncInitiator;
import rx.Subscription;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class OnboardSuggestedUsersSyncFragment extends Fragment {

    @Inject SyncInitiator syncInitiator;
    @Inject FollowingOperations followingOperations;
    private Subscription subscription;

    public OnboardSuggestedUsersSyncFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OnboardSuggestedUsersSyncFragment(FollowingOperations followingOperations, SyncInitiator syncInitiator) {
        this.followingOperations = followingOperations;
        this.syncInitiator = syncInitiator;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        subscription = bindFragment(this, followingOperations.waitForActivities(getActivity()))
                .subscribe(new FollowingsSyncSubscriber());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout relativeLayout = new RelativeLayout(getActivity());
        relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final View view = inflater.inflate(R.layout.ak_list_loading_item, null);

        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(view, params);
        return relativeLayout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subscription.unsubscribe();
    }

    private void finish(boolean success) {
        final Intent intent = new Intent(Actions.STREAM);
        intent.putExtra(MainActivity.EXTRA_ONBOARDING_USERS_RESULT, success);
        startActivity(intent);
        getActivity().finish();
    }

    class FollowingsSyncSubscriber extends DefaultSubscriber<Boolean> {

        @Override
        public void onNext(Boolean success) {
            finish(success);
        }

        @Override
        public void onCompleted() {
            // onNext might have already finished it
            if (!getActivity().isFinishing()) {
                finish(true);
            }
        }

        @Override
        public void onError(Throwable error) {
            super.onError(error);
            // send sync adapter request for followings so retry logic will kick in
            syncInitiator.pushFollowingsToApi();
            finish(false);
        }
    }

}
