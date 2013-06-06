package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersAdapter;
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

public class SuggestedUsersFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private static final String KEY_OBSERVABLE = "buckets_observable";
    private static final String TAG = "suggested_users_fragment";

    private SuggestedUsersAdapter mAdapter;
    private OnboardingOperations mOnboardingOps;
    private Subscription mSubscription;
    private WeakReference<SuggestedUsersFragmentListener> mListenerRef;

    public SuggestedUsersFragment() {
        this(new OnboardingOperations().<OnboardingOperations>scheduleFromActivity(), new SuggestedUsersAdapter(SuggestedUsersAdapter.Section.ALL_SECTIONS));
    }

    @VisibleForTesting
    protected SuggestedUsersFragment(OnboardingOperations onboardingOps, SuggestedUsersAdapter adapter) {
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

        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setDrawSelectorOnTop(false);
        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_list_header, null));
        listView.setOnItemClickListener(this);
        listView.setAdapter(mAdapter);

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), TAG);
        Observable<?> observable = savedState.getOrPut(KEY_OBSERVABLE, mOnboardingOps.getCategoryGroups().cache());
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

    private static final class OnGenreBucketsCompleted extends RxFragmentCompletionHandler<SuggestedUsersFragment> {

        public OnGenreBucketsCompleted(SuggestedUsersFragment fragment) {
            super(fragment);
        }

        @Override
        protected void onCompleted(SuggestedUsersFragment fragment) {
            Log.d(fragment, "fragment: onCompleted");
            fragment.mAdapter.notifyDataSetChanged();
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
