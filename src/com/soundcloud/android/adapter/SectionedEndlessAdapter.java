package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.view.FriendFinderView;
import com.soundcloud.api.Request;

import java.util.List;

public class SectionedEndlessAdapter extends LazyEndlessAdapter{

    private FriendFinderView mFriendFinderView;

    private boolean mSuggestedMode;

    private int mSectionIndex = 0;

    public SectionedEndlessAdapter(ScActivity activity, SectionedAdapter wrapped) {
        super(activity, wrapped, null);
    }

    @Override
    public List<Parcelable> getData() {
        return ((SectionedAdapter) this.getWrappedAdapter()).getData(mSectionIndex);
    }

    @Override
    protected Request getRequest() {
        return ((SectionedAdapter) this.getWrappedAdapter()).getRequest(mSectionIndex);
    }

    @Override
    public Class<?> getLoadModel() {
        return ((SectionedAdapter) this.getWrappedAdapter()).getLoadModel(mSectionIndex);
    }

    public void clear(){
           ((SectionedAdapter) getWrappedAdapter()).sections.clear();
    }

    public Object saveState(){
        return new Object[] {
                ((SectionedAdapter) getWrappedAdapter()).sections,
                getTask(),
                savePagingData(),
                saveExtraData()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(Object[] state){
        if (state[0] != null) ((SectionedAdapter) getWrappedAdapter()).sections = (List<SectionedAdapter.Section>) state[0];
        if (state[1] != null) this.restoreTask((AppendTask) state[1]);
        if (state[2] != null) this.restorePagingData((int[]) state[2]);
        if (state[3] != null) this.restoreExtraData((String) state[3]);
    }

    @Override
    public void onPostTaskExecute(Boolean keepgoing) {
        super.onPostTaskExecute(keepgoing);
        if (keepgoing != null && !keepgoing && ((SectionedAdapter) this.getWrappedAdapter()).sections.size() - 1 > mSectionIndex) {
            mCurrentPage = 0;
            mSectionIndex++;
            keepOnAppending.set(true);
        }
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        ViewGroup row = (ViewGroup) super.getPendingView(parent);
        row.findViewById(R.id.listHeader).setVisibility(View.GONE);
        return row;
    }
}
