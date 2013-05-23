package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockListFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersAdapter;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.rx.android.RxFragmentCompletionHandler;
import com.soundcloud.android.rx.android.RxFragmentErrorHandler;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class SuggestedUsersFragment extends SherlockListFragment {

    private static final String KEY_OBSERVABLE = "buckets_observable";

    private SuggestedUsersAdapter mAdapter;
    private OnboardingOperations mOnboardingOps;
    private Subscription mGetBucketsSubscription;
    private ProgressBar mProgressSpinner;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_users_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgressSpinner = (ProgressBar) view.findViewById(android.R.id.progress);

        StateHolderFragment savedState = StateHolderFragment.obtain(this);
        Observable<?> observable = savedState.getOrDefault(KEY_OBSERVABLE, Observable.class, mOnboardingOps.getGenreBuckets().cache());
        Log.d(this, "SUBSCRIBING, obs = " + observable.hashCode());
        mGetBucketsSubscription = observable.subscribe(
                mAdapter.onNextGenreBucket(), new OnGenreBucketsError(this), new OnGenreBucketsCompleted(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(this, "UNSUBSCRIBING");
        mGetBucketsSubscription.unsubscribe();
    }

    private static final class OnGenreBucketsCompleted extends RxFragmentCompletionHandler<SuggestedUsersFragment> {

        public OnGenreBucketsCompleted(SuggestedUsersFragment fragment) {
            super(fragment);
        }

        @Override
        protected void onCompleted(SuggestedUsersFragment fragment) {
            Log.d(fragment, "fragment: onCompleted");
            fragment.mAdapter.notifyDataSetChanged();
            fragment.mProgressSpinner.setVisibility(View.GONE);
        }
    }

    private static final class OnGenreBucketsError extends RxFragmentErrorHandler<SuggestedUsersFragment> {

        public OnGenreBucketsError(SuggestedUsersFragment fragment) {
            super(fragment);
        }

        @Override
        protected void onError(SuggestedUsersFragment fragment, Exception error) {
            //TODO proper error message
            AndroidUtils.showToast(fragment.getActivity(), "Failed obtaining suggested users");
        }
    }
}
