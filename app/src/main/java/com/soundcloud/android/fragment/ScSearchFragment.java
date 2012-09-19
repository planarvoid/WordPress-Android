package com.soundcloud.android.fragment;

import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.soundcloud.api.Request;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

public class ScSearchFragment extends ScListFragment {

    public static ScSearchFragment newInstance() {
        ScSearchFragment fragment = new ScSearchFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", Content.SEARCH.uri);
        fragment.setArguments(args);
        return fragment;
    }

    public void setCurrentSearch(Search currentSearch) {
        if (mCurrentSearch != currentSearch) {
            mCurrentSearch = currentSearch;
            refresh(false);
        }
    }

    private Search mCurrentSearch;

    @Override
    protected Request getRequest(boolean isRefresh) {
        if (TextUtils.isEmpty(mNextHref) || isRefresh) {
            // first page
            return mCurrentSearch.request();
        } else {
            return super.getRequest(isRefresh);
        }
    }
}
