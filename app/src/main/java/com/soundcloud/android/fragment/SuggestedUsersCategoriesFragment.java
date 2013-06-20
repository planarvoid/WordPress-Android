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
import com.soundcloud.android.view.EmptyListView;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.content.Intent;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class SuggestedUsersCategoriesFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private enum DisplayMode {
        LOADING, ERROR, CONTENT
    }
    private DisplayMode mMode = DisplayMode.LOADING;

    private static final String KEY_OBSERVABLE = "buckets_observable";
    private static final String FRAGMENT_TAG = "suggested_users_fragment";
    private static final String LOG_TAG = "suggested_users_frag";

    private SuggestedUsersCategoriesAdapter mAdapter;
    private SuggestedUsersOperations mSuggestions;
    private Subscription mSubscription;
    private Observer<CategoryGroup> mObserver;

    private EmptyListView mEmptyListView;

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

        mEmptyListView = (EmptyListView) getView().findViewById(android.R.id.empty);
        mEmptyListView.setMessageText(R.string.problem_connecting_to_SoundCloud);
        mEmptyListView.setActionText(getResources().getString(R.string.try_again));
        mEmptyListView.setActionListener(new EmptyListView.ActionListener() {
            @Override
            public void onAction() {
                refresh();
            }

            @Override
            public void onSecondaryAction() {}
        });

        final ListView listView = getListView();
        listView.setDrawSelectorOnTop(false);
        listView.setSelector(new StateListDrawable());
        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_category_list_header, null), null, false);
        listView.setOnItemClickListener(this);
        listView.setAdapter(mAdapter);

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG);
        Observable<?> observable = savedState.getOrPut(KEY_OBSERVABLE, mSuggestions.getCategoryGroups().cache().observeOn(ScSchedulers.UI_SCHEDULER));
        Log.d(LOG_TAG, "SUBSCRIBING, obs = " + observable.hashCode());
        refresh(observable);
    }

    private void refresh() {
        refresh((Observable<?>) StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG).get(KEY_OBSERVABLE));
    }

    private void refresh(Observable<?> observable) {
        if (mObserver == null) mObserver = new CategoryGroupsObserver(this);
        mSubscription = observable.subscribe(mObserver);
        setDisplayMode(DisplayMode.LOADING);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "UNSUBSCRIBING");
        mSubscription.unsubscribe();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Category item = mAdapter.getItem(position - getListView().getHeaderViewsCount());
        if (item.isErrorOrEmptyCategory()){
            refresh();
        } else {
            final Intent intent = new Intent(getActivity(), SuggestedUsersCategoryActivity.class);
            intent.putExtra(Category.EXTRA, item);
            startActivity(intent);
        }

    }

    @VisibleForTesting
    ListView getListView() {
        final View view = getView();
        return view != null ? (ListView) view.findViewById(android.R.id.list) : null;
    }

    private void setDisplayMode(DisplayMode mode){
        mMode = mode;
        switch (mMode){
            case LOADING:
                mEmptyListView.setStatus(EmptyListView.Status.WAITING);
                mEmptyListView.setVisibility(View.VISIBLE);
                getListView().setVisibility(View.GONE);
                break;

            case CONTENT:
                getListView().setVisibility(View.VISIBLE);
                mEmptyListView.setVisibility(View.GONE);
                break;

            case ERROR:
                mEmptyListView.setStatus(EmptyListView.Status.OK);
                mEmptyListView.setVisibility(View.VISIBLE);
                getListView().setVisibility(View.GONE);
                break;
        }
    }

    private static final class CategoryGroupsObserver extends RxFragmentObserver<SuggestedUsersCategoriesFragment, CategoryGroup> {

        public CategoryGroupsObserver(SuggestedUsersCategoriesFragment fragment) {
            super(fragment);
        }

        @Override
        public void onNext(SuggestedUsersCategoriesFragment fragment, CategoryGroup categoryGroup) {
            Log.d(LOG_TAG, "got category group: " + categoryGroup);
            fragment.mAdapter.addItem(categoryGroup);
            if (!categoryGroup.isFacebook()){
                fragment.setDisplayMode(DisplayMode.CONTENT);
            }
        }

        @Override
        public void onCompleted(SuggestedUsersCategoriesFragment fragment) {
            Log.d(LOG_TAG, "fragment: onCompleted");
        }

        @Override
        public void onError(SuggestedUsersCategoriesFragment fragment, Exception error) {
            fragment.setDisplayMode(DisplayMode.ERROR);
            error.printStackTrace();
            AndroidUtils.showToast(fragment.getActivity(), R.string.suggested_users_error_get_genre_buckets);
        }
    }
}
