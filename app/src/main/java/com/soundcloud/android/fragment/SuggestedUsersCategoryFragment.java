package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersAdapter;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import java.util.List;
import java.util.Set;

public class SuggestedUsersCategoryFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    private SuggestedUsersAdapter mAdapter;
    private Category mCategory = Category.EMPTY;
    private GridView mAdapterView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(Category.EXTRA)){
            mCategory = getArguments().getParcelable(Category.EXTRA);
        }
        setAdapter(new SuggestedUsersAdapter(mCategory.getUsers()));
    }

    public void setAdapter(SuggestedUsersAdapter adapter){
        mAdapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_user_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapterView = (GridView) view.findViewById(R.id.gridview);
        mAdapterView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mAdapterView.setSelector(R.drawable.list_selector_background);
        mAdapterView.setDrawSelectorOnTop(false);
        mAdapterView.setOnItemClickListener(this);
        mAdapterView.setAdapter(mAdapter);

        Set<Long> followingIds = FollowStatus.get().getFollowedUserIds();
        for (int i = 0; i < mAdapter.getCount(); i++){
            mAdapterView.setItemChecked(i, followingIds.contains(mAdapter.getItemId(i)));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FollowStatus.get().toggleFollowing(new User(mAdapter.getItem(position)));
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void toggleFollowings(boolean shouldFollow){
        List<SuggestedUser> followings = shouldFollow
                ? mCategory.getNotFollowedUsers(FollowStatus.get().getFollowedUserIds())
                : mCategory.getFollowedUsers(FollowStatus.get().getFollowedUserIds());

        //FollowStatus.get().toggleFollowing(followings);
        for (int i = 0; i < mAdapter.getCount(); i++){
            mAdapterView.setItemChecked(i, shouldFollow);
        }
    }
}
