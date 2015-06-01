package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.rx.observers.DefaultFragmentObserver;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class SuggestedUsersCategoriesFragment extends Fragment implements AdapterView.OnItemClickListener {

    public final static String SHOW_FACEBOOK = "SHOW_FACEBOOK";
    private static final String KEY_OBSERVABLE = "buckets_observable";
    private static final String FRAGMENT_TAG = "suggested_users_fragment";
    private static final String LOG_TAG = "suggested_users_frag";

    @Inject SuggestedUsersCategoriesAdapter adapter;
    @Inject SuggestedUsersOperations suggestedUserOps;
    @Inject FollowingOperations followingOperations;

    private Subscription subscription;
    private Observer<CategoryGroup> observer;
    private ListView listView;
    private EmptyView emptyView;

    public SuggestedUsersCategoriesFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected SuggestedUsersCategoriesFragment(SuggestedUsersOperations suggestedUserOps,
                                               @Nullable Observer<CategoryGroup> observer,
                                               SuggestedUsersCategoriesAdapter adapter) {
        this.suggestedUserOps = suggestedUserOps;
        this.observer = observer;
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter.setActiveSections(shouldShowFacebook() ?
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
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = (EmptyView) view.findViewById(android.R.id.empty);
        emptyView.setMessageText(R.string.problem_connecting_to_SoundCloud);
        emptyView.setActionText(getResources().getString(R.string.try_again));
        emptyView.setActionListener(new EmptyView.ActionListener() {
            @Override
            public void onAction() {
                refresh();
            }
        });

        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setDrawSelectorOnTop(false);
        listView.setSelector(new StateListDrawable());
        listView.setHeaderDividersEnabled(false);
        listView.addHeaderView(getLayoutInflater(null).inflate(R.layout.suggested_users_category_list_header, null), null, false);
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);

        // TODO: get rid of StateHolderFragment in favor of setRetainInstanceState or Memento
        StateHolderFragment savedState = StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG);
        Observable<CategoryGroup> observable;
        if (savedState.has(KEY_OBSERVABLE)) {
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
        listView = null;
        emptyView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "UNSUBSCRIBING");
        subscription.unsubscribe();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Category item = adapter.getItem(position - listView.getHeaderViewsCount());
        if (item.isError()) {
            refresh();
        } else {
            final Intent intent = new Intent(getActivity(), SuggestedUsersCategoryActivity.class);
            intent.putExtra(Category.EXTRA, item);
            startActivity(intent);
        }
    }

    private Observable<CategoryGroup> createCategoriesObservable() {
        final Observable<CategoryGroup> categoryGroups = shouldShowFacebook() ?
                suggestedUserOps.getCategoryGroups() : suggestedUserOps.getMusicAndSoundsSuggestions();
        return categoryGroups.cache().observeOn(AndroidSchedulers.mainThread());
    }

    private void refresh() {
        final Observable<CategoryGroup> categoriesObservable = createCategoriesObservable();
        StateHolderFragment.obtain(getFragmentManager(), FRAGMENT_TAG).put(KEY_OBSERVABLE, categoriesObservable);
        loadCategories(categoriesObservable);
    }

    private void loadCategories(Observable<CategoryGroup> observable) {
        if (observer == null) {
            observer = new CategoryGroupsObserver(this, followingOperations);
        }
        subscription = observable.subscribe(observer);
        setDisplayMode(DisplayMode.LOADING);
    }

    private boolean shouldShowFacebook() {
        return getArguments() != null && getArguments().getBoolean(SHOW_FACEBOOK, false);
    }

    private void setDisplayMode(DisplayMode mode) {
        switch (mode) {
            case LOADING:
                emptyView.setStatus(EmptyView.Status.WAITING);
                emptyView.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                break;

            case CONTENT:
                listView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                break;

            case ERROR:
                emptyView.setStatus(EmptyView.Status.OK);
                emptyView.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                break;
        }
    }

    private enum DisplayMode {
        LOADING, ERROR, CONTENT
    }

    private static final class CategoryGroupsObserver extends DefaultFragmentObserver<SuggestedUsersCategoriesFragment, CategoryGroup> {

        private final FollowingOperations followingOperations;

        public CategoryGroupsObserver(SuggestedUsersCategoriesFragment fragment, FollowingOperations followingOperations) {
            super(fragment);
            this.followingOperations = followingOperations;
        }

        @Override
        public void onNext(SuggestedUsersCategoriesFragment fragment, CategoryGroup categoryGroup) {
            Log.d(LOG_TAG, "got category group: " + categoryGroup);
            fragment.adapter.addItem(categoryGroup);
            fragment.adapter.notifyDataSetChanged();

            if (!categoryGroup.isFacebook()) {
                fragment.setDisplayMode(DisplayMode.CONTENT);
            } else if (fragment.shouldShowFacebook()) {
                fireAndForget(followingOperations.addFollowingsBySuggestedUsers(categoryGroup.getAllSuggestedUsers()));
            }
        }

        @Override
        public void onCompleted(SuggestedUsersCategoriesFragment fragment) {
            Log.d(LOG_TAG, "fragment: onCompleted");
        }

        @Override
        public void onError(SuggestedUsersCategoriesFragment fragment, Throwable error) {
            fragment.setDisplayMode(DisplayMode.ERROR);
        }
    }
}
