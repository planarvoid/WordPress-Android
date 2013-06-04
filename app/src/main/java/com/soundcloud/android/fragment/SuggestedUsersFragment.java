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
import android.widget.ListView;
import android.widget.ProgressBar;

public class SuggestedUsersFragment extends SherlockListFragment {

    private static final String KEY_OBSERVABLE = "buckets_observable";
    private static final String TAG = "suggested_users_fragment";

    private SuggestedUsersAdapter mAdapter;
    private OnboardingOperations mOnboardingOps;
    private Subscription mSubscription;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_users_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgressSpinner = (ProgressBar) view.findViewById(android.R.id.progress);

        final ListView listView = getListView();
        listView.setDrawSelectorOnTop(false);
        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_list_header, null));
        setListAdapter(mAdapter);

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), TAG);
        Observable<?> observable = savedState.getOrDefault(KEY_OBSERVABLE, Observable.class, mOnboardingOps.getCategoryGroups().cache());
        Log.d(this, "SUBSCRIBING, obs = " + observable.hashCode());
        mSubscription = observable.subscribe(
                mAdapter.onNextCategoryGroup(), new OnGenreBucketsError(this), new OnGenreBucketsCompleted(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(this, "UNSUBSCRIBING");
        mSubscription.unsubscribe();
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
            AndroidUtils.showToast(fragment.getActivity(), R.string.error_get_genre_buckets);
        }
    }
}
