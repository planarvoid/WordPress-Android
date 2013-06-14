package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersAdapter;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.User;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.rx.observers.ScObserver;
import com.soundcloud.android.view.GridViewCompat;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import java.util.Set;

public class SuggestedUsersCategoryFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private SuggestedUsersAdapter mAdapter;
    private Category mCategory = Category.EMPTY;
    private GridViewCompat mAdapterView;
    private FollowingOperations mFollowingOperations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(Category.EXTRA)) {
            mCategory = getArguments().getParcelable(Category.EXTRA);
        }
        setAdapter(new SuggestedUsersAdapter(mCategory.getUsers()));
        mFollowingOperations = new FollowingOperations().scheduleFromActivity();
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

        mAdapterView = (GridViewCompat) view.findViewById(R.id.gridview);
        mAdapterView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mAdapterView.setOnItemClickListener(this);
        mAdapterView.setAdapter(mAdapter);

        final Set<Long> followingIds = FollowStatus.get().getFollowedUserIds();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mAdapterView.setItemChecked(i, followingIds.contains(mAdapter.getItemId(i)));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mFollowingOperations.toggleFollowing(new User(mAdapter.getItem(position))).subscribe(mToggleFollowingObserver);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // for gridview setItemChecked
    public void toggleFollowings(final boolean shouldFollow) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mAdapterView.setItemChecked(i, shouldFollow);
        }

        final Set<Long> followedUserIds = FollowStatus.get().getFollowedUserIds();
        if (shouldFollow) {
            mFollowingOperations.addFollowingsBySuggestedUsers(mCategory.getNotFollowedUsers(followedUserIds)).subscribe(mToggleAllObserver);
        } else {
            mFollowingOperations.removeFollowingsBySuggestedUsers(mCategory.getFollowedUsers(followedUserIds)).subscribe(mToggleAllObserver);
        }
    }

    private ScObserver<Void> mToggleFollowingObserver = new ScObserver<Void>() {
        @Override
        public void onCompleted() {
            final FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.supportInvalidateOptionsMenu();
            }
        }

        @Override
        public void onError(Exception e) {
            mAdapter.notifyDataSetChanged();
        }
    };

    private ScObserver<Void> mToggleAllObserver = new ScObserver<Void>() {
        @Override
        public void onError(Exception e) {
            mAdapter.notifyDataSetChanged();
        }
    };
}
