package com.soundcloud.android.fragment;

import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.soundcloud.api.Request;

import android.os.Bundle;
import android.text.TextUtils;

public class ScSearchFragment extends ScListFragment {
    private Search mCurrentSearch;

    public ScSearchFragment() {
        super();
    }

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
            reset();
        }
    }

    @Override
    protected boolean canAppend() {
        return mCurrentSearch != null && super.canAppend();
    }

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
