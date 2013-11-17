package com.soundcloud.android.onboarding.suggestions;

import static rx.android.AndroidObservables.fromFragment;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.SuggestedUsersAdapter;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.view.GridViewCompat;
import rx.Observable;
import rx.android.RxFragmentObserver;
import rx.subscriptions.CompositeSubscription;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import java.util.Set;

public class SuggestedUsersCategoryFragment extends Fragment implements AdapterView.OnItemClickListener {

    private SuggestedUsersAdapter mAdapter;
    private Category mCategory;
    private GridViewCompat mAdapterView;
    private FollowingOperations mFollowingOperations;
    private final CompositeSubscription mSubscription = new CompositeSubscription();

    public SuggestedUsersCategoryFragment() {
        this(new FollowingOperations());
    }

    public SuggestedUsersCategoryFragment(FollowingOperations followingOperations) {
        mFollowingOperations = followingOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(Category.EXTRA)) {
            mCategory = getArguments().getParcelable(Category.EXTRA);
        } else {
            mCategory = Category.empty();
        }
        setAdapter(new SuggestedUsersAdapter(mCategory.getUsers()));

    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    public void setAdapter(SuggestedUsersAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_user_grid, container, false);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // for gridviewcompat setChoiceMode and setItemChecked
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapterView = (GridViewCompat) view.findViewById(R.id.suggested_users_grid);
        mAdapterView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mAdapterView.setOnItemClickListener(this);
        mAdapterView.setAdapter(mAdapter);

        for (int i = 0; i < mAdapter.getCount(); i++) {
            mAdapterView.setItemChecked(i, mFollowingOperations.getFollowedUserIds().contains(mAdapter.getItemId(i)));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSubscription.add(fromFragment(this, mFollowingOperations.toggleFollowingBySuggestedUser(mAdapter.getItem(position)))
                .subscribe(new ToggleFollowingObserver(this)));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // for gridview setItemChecked
    public void toggleFollowings(final boolean shouldFollow) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mAdapterView.setItemChecked(i, shouldFollow);
        }

        final Set<Long> followedUserIds = mFollowingOperations.getFollowedUserIds();
        Observable<UserAssociation> toggleFollowings;
        if (shouldFollow) {
            toggleFollowings = mFollowingOperations.addFollowingsBySuggestedUsers(mCategory.getNotFollowedUsers(followedUserIds));
        } else {
            toggleFollowings = mFollowingOperations.removeFollowingsBySuggestedUsers(mCategory.getFollowedUsers(followedUserIds));
        }
        mSubscription.add(fromFragment(this, toggleFollowings).subscribe(new ToggleAllObserver(this)));
    }

    private static final class ToggleFollowingObserver extends RxFragmentObserver<SuggestedUsersCategoryFragment, UserAssociation> {
        public ToggleFollowingObserver(SuggestedUsersCategoryFragment fragment) {
            super(fragment);
        }

        @Override
        public void onCompleted(SuggestedUsersCategoryFragment fragment) {
            fragment.getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        public void onError(SuggestedUsersCategoryFragment fragment, Throwable error) {
            error.printStackTrace();
            fragment.mAdapter.notifyDataSetChanged();
        }
    }

    private static final class ToggleAllObserver extends RxFragmentObserver<SuggestedUsersCategoryFragment, UserAssociation> {
        public ToggleAllObserver(SuggestedUsersCategoryFragment fragment) {
            super(fragment);
        }

        @Override
        public void onError(SuggestedUsersCategoryFragment fragment, Throwable error) {
            error.printStackTrace();
            fragment.mAdapter.notifyDataSetChanged();
        }
    }
}
