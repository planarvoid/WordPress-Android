package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class SuggestedUsersCategoryFragment extends SherlockFragment implements AdapterView.OnItemClickListener {

    public static String KEY_CATEGORY = "category";

    private ArrayAdapter<SuggestedUser> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final List<SuggestedUser> suggestedUsers = ((Category) getArguments().getParcelable(KEY_CATEGORY)).getUsers();
        final ArrayAdapter<SuggestedUser> adapter = new ArrayAdapter<SuggestedUser>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                suggestedUsers);

        setAdapter(adapter);
    }

    public void setAdapter(ArrayAdapter<SuggestedUser> adapter){
        mAdapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setDrawSelectorOnTop(false);
        listView.setHeaderDividersEnabled(false);
        listView.setOnItemClickListener(this);
        listView.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FollowStatus.get().toggleFollowing(new User(mAdapter.getItem(position)));
    }
}
