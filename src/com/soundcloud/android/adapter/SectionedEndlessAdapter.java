package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.api.Request;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SectionedEndlessAdapter extends LazyEndlessAdapter{
    private List<WeakReference<SectionListener>> mListeners;

    private int mSectionIndex = 0;

    public SectionedEndlessAdapter(ScActivity activity, SectionedAdapter wrapped) {
        super(activity, wrapped, null);
        mListeners = new ArrayList<WeakReference<SectionListener>>();
    }

    public void addListener(SectionListener listener){
        for (WeakReference<SectionListener> listenerRef : mListeners){
            if (listenerRef.get() != null && listenerRef.get() == listener) return;
        }
        mListeners.add(new WeakReference<SectionListener>(listener));
    }


    @Override
    public List<Parcelable> getData() {
        return getWrappedAdapter().getData(mSectionIndex);
    }

    @Override
    protected Request getRequest() {
        return getWrappedAdapter().getRequest(mSectionIndex);
    }

    @Override
    public Class<?> getLoadModel() {
        return getWrappedAdapter().getLoadModel(mSectionIndex);
    }

    public void clearData(){
        mSectionIndex = 0;
        getWrappedAdapter().sections.clear();
    }

    public Object saveState(){
        return new Object[] {
                getWrappedAdapter().sections,
                getTask(),
                savePagingData(),
                saveExtraData()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(Object[] state){
        if (state[0] != null) getWrappedAdapter().sections = (List<SectionedAdapter.Section>) state[0];
        if (state[1] != null) restoreTask((AppendTask) state[1]);
        if (state[2] != null) restorePagingData((int[]) state[2]);
        if (state[3] != null) restoreExtraData((String) state[3]);
    }

    @Override
    public SectionedAdapter getWrappedAdapter() {
        return (SectionedAdapter) super.getWrappedAdapter();
    }

    @Override
    public void onPostTaskExecute(Boolean keepgoing) {

        rebindPendingView(pendingPosition, pendingView);
        pendingView = null;
        pendingPosition = -1;
        notifyDataSetChanged();

        if (keepgoing != null) {
            if (!keepgoing) {
                  for (WeakReference<SectionListener> listenerRef : mListeners) {
                        SectionListener listener = listenerRef.get();
                        if (listener != null && mSectionIndex < getWrappedAdapter().sections.size()) {
                            listener.onSectionLoaded(getWrappedAdapter().sections.get(mSectionIndex));
                        }
                    }

                    // load next section as necessary
                    if (getWrappedAdapter().sections.size() - 1 > mSectionIndex) {
                        mCurrentPage = 0;
                        mSectionIndex++;
                        keepOnAppending.set(true);
                        return;
                    }
            }
            keepOnAppending.set(keepgoing);
        } else {
            mException = true;
        }

        // configure the empty view depending on possible exceptions
        setEmptyviewText();
        mListView.setEmptyView(mEmptyView);

        mActivity.handleException();
        mActivity.handleError();
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        ViewGroup row = (ViewGroup) super.getPendingView(parent);
        row.findViewById(R.id.listHeader).setVisibility(View.GONE);
        return row;
    }

    public interface SectionListener {
        void onSectionLoaded(SectionedAdapter.Section seection);
    }
}
