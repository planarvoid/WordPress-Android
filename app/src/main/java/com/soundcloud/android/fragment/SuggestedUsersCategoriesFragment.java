package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.SuggestedUsersCategoryActivity;
import com.soundcloud.android.adapter.SuggestedUsersCategoriesAdapter;
import com.soundcloud.android.api.SuggestedUsersOperations;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class SuggestedUsersCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private static final String KEY_OBSERVABLE = "buckets_observable";
    private static final String FRAGMENT_TAG = "suggested_users_fragment";
    private static final String LOG_TAG = "suggested_users_frag";

    private SuggestedUsersCategoriesAdapter mAdapter;
    private SuggestedUsersOperations mSuggestions;
    private Subscription mSubscription;
    private Observer<CategoryGroup> mObserver;

    private View mListContainer;
    private View mProgressSpinner;

    public SuggestedUsersCategoriesFragment() {
        this(new SuggestedUsersOperations(), null,
                new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS));
    }

    @VisibleForTesting
    protected SuggestedUsersCategoriesFragment(SuggestedUsersOperations onboardingOps,
                                               @Nullable Observer<CategoryGroup> observer,
                                               SuggestedUsersCategoriesAdapter adapter) {
        mSuggestions = onboardingOps;
        mObserver = observer;
        mAdapter = adapter;
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

        mProgressSpinner = getView().findViewById(android.R.id.progress);
        mListContainer = getView().findViewById(R.id.list_container);
        setListShown(false);

        final ListView listView = getListView();
        listView.setDrawSelectorOnTop(false);
        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_category_list_header, null), null, false);
        listView.setOnItemClickListener(this);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(getView().findViewById(android.R.id.empty));

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG);
        Observable<?> observable = savedState.getOrPut(KEY_OBSERVABLE, mSuggestions.getCategoryGroups().cache().observeOn(ScSchedulers.UI_SCHEDULER));
        Log.d(LOG_TAG, "SUBSCRIBING, obs = " + observable.hashCode());

        if (mObserver == null) mObserver = new CategoryGroupsObserver(this);
        mSubscription = observable.subscribe(mObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "UNSUBSCRIBING");
        mSubscription.unsubscribe();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(getActivity(), SuggestedUsersCategoryActivity.class);
        intent.putExtra(Category.EXTRA, mAdapter.getItem(position - getListView().getHeaderViewsCount()));
        startActivity(intent);
    }

    @VisibleForTesting
    ListView getListView() {
        final View view = getView();
        return view != null ? (ListView) view.findViewById(android.R.id.list) : null;
    }

    private void setListShown(boolean isShown){
        mProgressSpinner.setVisibility(isShown ? View.GONE : View.VISIBLE);
        mListContainer.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    private static final class CategoryGroupsObserver extends RxFragmentObserver<SuggestedUsersCategoriesFragment, CategoryGroup> {

        public CategoryGroupsObserver(SuggestedUsersCategoriesFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(SuggestedUsersCategoriesFragment fragment, CategoryGroup categoryGroup) {
            Log.d(LOG_TAG, "got category group: " + categoryGroup);
            fragment.mAdapter.addItem(categoryGroup);
        }

        @Override
        public void onCompleted(SuggestedUsersCategoriesFragment fragment) {
            Log.d(LOG_TAG, "fragment: onCompleted");
            fragment.mAdapter.notifyDataSetChanged();
            fragment.setListShown(true);
        }

        @Override
        public void onError(SuggestedUsersCategoriesFragment fragment, Exception error) {
            // TODO : populate error view
            fragment.setListShown(true);
            error.printStackTrace();
            AndroidUtils.showToast(fragment.getActivity(), R.string.suggested_users_error_get_genre_buckets);
        }
    }
}
