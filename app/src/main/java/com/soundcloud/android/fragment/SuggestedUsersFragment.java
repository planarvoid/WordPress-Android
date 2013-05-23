package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockListFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersAdapter;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.rx.android.SafeActivityErrorHandler;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SuggestedUsersFragment extends SherlockListFragment {

    private static final String KEY_OBSERVABLE = "buckets_observable";

    private SuggestedUsersAdapter mAdapter;
    private OnboardingOperations mOnboardingOps;
    private Subscription mGetBucketsSubscription;

    public SuggestedUsersFragment() {
        this(new OnboardingOperations().<OnboardingOperations>scheduleFromActivity(), new SuggestedUsersAdapter());
    }

    public SuggestedUsersFragment(OnboardingOperations onboardingOps, SuggestedUsersAdapter adapter) {
        mOnboardingOps = onboardingOps;
        mAdapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(mAdapter);

        StateHolderFragment savedState = StateHolderFragment.obtain(this);
        Observable<?> observable = savedState.getOrDefault(KEY_OBSERVABLE, Observable.class, mOnboardingOps.getGenreBuckets().cache());
        Log.d(this, "SUBSCRIBING, obs = " + observable.hashCode());
        mGetBucketsSubscription = observable.subscribe(
                mAdapter.onNext, new OnGenreBucketsError(getActivity()), mAdapter.onCompleted);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_users_fragment, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(this, "UNSUBSCRIBING");
        mGetBucketsSubscription.unsubscribe();
    }

    private static final class OnGenreBucketsError extends SafeActivityErrorHandler {

        public OnGenreBucketsError(Activity context) {
            super(context);
        }

        @Override
        protected void handleError(Activity activity, Exception error) {
            //TODO proper error message
            AndroidUtils.showToast(activity, "Failed obtaining suggested users");
        }
    }
}
