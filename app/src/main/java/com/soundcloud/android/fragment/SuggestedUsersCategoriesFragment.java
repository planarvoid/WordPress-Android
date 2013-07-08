package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.SuggestedUsersCategoryActivity;
import com.soundcloud.android.adapter.SuggestedUsersCategoriesAdapter;
import com.soundcloud.android.api.SuggestedUsersOperations;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.android.RxFragmentObserver;
import com.soundcloud.android.rx.observers.ScObserver;
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

    public final static String SHOW_FACEBOOK = "SHOW_FACEBOOK";

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

    private ListView mListView;
    private EmptyListView mEmptyListView;

    public SuggestedUsersCategoriesFragment() {
        this(new SuggestedUsersOperations(), null,
                new SuggestedUsersCategoriesAdapter());
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter.setActiveSections(shouldShowFacebook() ?
                SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS :
                SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK);
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

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setMessageText(R.string.problem_connecting_to_SoundCloud);
        mEmptyListView.setActionText(getResources().getString(R.string.try_again));
        mEmptyListView.setActionListener(new EmptyListView.ActionListener() {
            @Override
            public void onAction() {
                refresh();
            }

            @Override
            public void onSecondaryAction() {
            }
        });

        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setDrawSelectorOnTop(false);
        mListView.setSelector(new StateListDrawable());
        mListView.setHeaderDividersEnabled(false);
        mListView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_category_list_header, null), null, false);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);

        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG);
        Observable<?> observable;
        if (savedState.has(KEY_OBSERVABLE)){
            observable = savedState.get(KEY_OBSERVABLE);
        } else {
            observable = createCategoriesObservable();
            savedState.put(KEY_OBSERVABLE, observable);
        }
        loadCategories(observable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
        mEmptyListView = null;
    }

    private Observable<CategoryGroup> createCategoriesObservable() {
        final Observable<CategoryGroup> categoryGroups = shouldShowFacebook() ?
                mSuggestions.getCategoryGroups() : mSuggestions.getMusicAndSoundsSuggestions();
        return categoryGroups.cache().observeOn(ScSchedulers.UI_SCHEDULER);
    }

    private void refresh() {
        final Observable<CategoryGroup> categoriesObservable = createCategoriesObservable();
        StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG).put(KEY_OBSERVABLE, categoriesObservable);
        loadCategories(categoriesObservable);
    }

    private void loadCategories(Observable<?> observable) {
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
        final Category item = mAdapter.getItem(position - mListView.getHeaderViewsCount());
        if (item.isError()){
            refresh();
        } else {
            final Intent intent = new Intent(getActivity(), SuggestedUsersCategoryActivity.class);
            intent.putExtra(Category.EXTRA, item);
            startActivity(intent);
        }
    }

    private boolean shouldShowFacebook(){
        return getArguments() != null && getArguments().getBoolean(SHOW_FACEBOOK, false);
    }

    private void setDisplayMode(DisplayMode mode){
        mMode = mode;
        switch (mMode){
            case LOADING:
                mEmptyListView.setStatus(EmptyListView.Status.WAITING);
                mEmptyListView.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
                break;

            case CONTENT:
                mListView.setVisibility(View.VISIBLE);
                mEmptyListView.setVisibility(View.GONE);
                break;

            case ERROR:
                mEmptyListView.setStatus(EmptyListView.Status.OK);
                mEmptyListView.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
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
            fragment.mAdapter.notifyDataSetChanged();

            if (!categoryGroup.isFacebook()){
                fragment.setDisplayMode(DisplayMode.CONTENT);
            } else if (fragment.shouldShowFacebook()) {
                new FollowingOperations().addFollowingsBySuggestedUsers(categoryGroup.getAllSuggestedUsers())
                        .subscribe(new ScObserver<Void>() {});
            }
        }

        @Override
        public void onCompleted(SuggestedUsersCategoriesFragment fragment) {
            Log.d(LOG_TAG, "fragment: onCompleted");
        }

        @Override
        public void onError(SuggestedUsersCategoriesFragment fragment, Exception error) {
            error.printStackTrace();
            fragment.setDisplayMode(DisplayMode.ERROR);
        }
    }
}
