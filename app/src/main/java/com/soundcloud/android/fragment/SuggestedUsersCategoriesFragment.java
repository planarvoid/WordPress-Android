package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersCategoriesAdapter;
import com.soundcloud.android.fragment.listeners.SuggestedUsersFragmentListener;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.rx.android.RxFragmentCompletionHandler;
import com.soundcloud.android.rx.android.RxFragmentErrorHandler;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.ref.WeakReference;

public class SuggestedUsersCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private static final String KEY_OBSERVABLE = "buckets_observable";
    private static final String FRAGMENT_TAG = "suggested_users_fragment";
    private static final String LOG_TAG = "suggested_users_frag";

    private SuggestedUsersCategoriesAdapter mAdapter;
    private OnboardingOperations mOnboardingOps;
    private Subscription mSubscription;
    private WeakReference<SuggestedUsersFragmentListener> mListenerRef;

    public SuggestedUsersCategoriesFragment() {
        this(new OnboardingOperations().<OnboardingOperations>scheduleFromActivity(), new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS));
    }

    @VisibleForTesting
    protected SuggestedUsersCategoriesFragment(OnboardingOperations onboardingOps, SuggestedUsersCategoriesAdapter adapter) {
        mOnboardingOps = onboardingOps;
        mAdapter = adapter;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListenerRef = new WeakReference<SuggestedUsersFragmentListener>((SuggestedUsersFragmentListener) activity);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement SuggestedUsersFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_users_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView listView = getListView();
        listView.setDrawSelectorOnTop(false);
        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_category_list_header, null), null, false);
        listView.setOnItemClickListener(this);
        listView.setAdapter(mAdapter);

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG);
        Observable<?> observable = savedState.getOrPut(KEY_OBSERVABLE, mOnboardingOps.getCategoryGroups().cache());
        Log.d(LOG_TAG, "SUBSCRIBING, obs = " + observable.hashCode());
        mSubscription = observable.subscribe(
                mAdapter.onNextCategoryGroup(), new OnGenreBucketsError(this), new OnGenreBucketsCompleted(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "UNSUBSCRIBING");
        mSubscription.unsubscribe();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final SuggestedUsersFragmentListener listener = mListenerRef.get();
        if (listener != null){
            listener.onCategorySelected(mAdapter.getItem(position - getListView().getHeaderViewsCount()));
        }
    }

    @VisibleForTesting
    ListView getListView() {
        final View view = getView();
        return view != null ? (ListView) view.findViewById(android.R.id.list) : null;
    }

    private static final class OnGenreBucketsCompleted extends RxFragmentCompletionHandler<SuggestedUsersCategoriesFragment> {

        public OnGenreBucketsCompleted(SuggestedUsersCategoriesFragment fragment) {
            super(fragment);
        }

        @Override
        protected void onCompleted(SuggestedUsersCategoriesFragment fragment) {
            Log.d(LOG_TAG, "fragment: onCompleted");
            fragment.mAdapter.notifyDataSetChanged();
        }
    }

    private static final class OnGenreBucketsError extends RxFragmentErrorHandler<SuggestedUsersCategoriesFragment> {

        public OnGenreBucketsError(SuggestedUsersCategoriesFragment fragment) {
            super(fragment);
        }

        @Override
        protected void onError(SuggestedUsersCategoriesFragment fragment, Exception error) {
            error.printStackTrace();
            //TODO proper error message
            AndroidUtils.showToast(fragment.getActivity(), R.string.suggested_users_error_get_genre_buckets);
        }
    }
}
