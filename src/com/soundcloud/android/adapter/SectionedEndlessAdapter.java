package com.soundcloud.android.adapter;

import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SectionedEndlessAdapter extends LazyEndlessAdapter{
    private List<WeakReference<SectionListener>> mListeners;
    private int mSectionIndex = 0;

    public SectionedEndlessAdapter(ScActivity activity, SectionedAdapter wrapped) {
        this(activity, wrapped, false);
    }

    public SectionedEndlessAdapter(ScActivity activity, SectionedAdapter wrapped, boolean autoAppend) {
        super(activity, wrapped, null, null, autoAppend);
        mListeners = new ArrayList<WeakReference<SectionListener>>();
    }

    public void addListener(SectionListener listener){
        for (WeakReference<SectionListener> listenerRef : mListeners){
            if (listenerRef.get() != null && listenerRef.get() == listener) return;
        }
        mListeners.add(new WeakReference<SectionListener>(listener));
    }

    public void addSection(SectionedAdapter.Section newSection){
        getWrappedAdapter().sections.add(newSection);
    }

    @Override
    public List<Parcelable> getData() {
        return getWrappedAdapter().getData(mSectionIndex);
    }

    @Override
    protected Uri getContentUri(boolean refresh) {
        if (mSectionIndex > getWrappedAdapter().sections.size()) return null;
        return getWrappedAdapter().sections.get(refresh ? 0 : mSectionIndex).contentUri;
    }

    @Override
    protected Request getRequest(boolean refresh) {
        if (mSectionIndex > getWrappedAdapter().sections.size()) return null;
        return getWrappedAdapter().sections.get(refresh ? 0 : mSectionIndex).getRequest(refresh);
    }

    @Override
    public Class<?> getLoadModel(boolean isRefresh) {
        return getWrappedAdapter().getLoadModel(isRefresh ? 0 : mSectionIndex);
    }

    @Override
    public void resetData(){
        mSectionIndex = 0;
        getWrappedAdapter().clearData();
    }

    public void clearSections(){
        mSectionIndex = 0;
        getWrappedAdapter().sections.clear();
    }

    public Object saveState(){
        return new Object[] {
                getWrappedAdapter().sections,
                getAppendTask(),
                savePagingData(),
                saveExtraData()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(Object[] state){
        if (state[0] != null) getWrappedAdapter().sections = (List<SectionedAdapter.Section>) state[0];
        if (state[1] != null) restoreAppendTask((LoadCollectionTask) state[1]);
        if (state[2] != null) restorePagingData((int[]) state[2]);
        if (state[3] != null) restoreExtraData((String) state[3]);
    }

    @Override
    public SectionedAdapter getWrappedAdapter() {
        return (SectionedAdapter) super.getWrappedAdapter();
    }

    @Override
    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        mPendingView = null;
        if (responseCode == HttpStatus.SC_OK) {
            if (newItems != null && newItems.size() > 0) {
                for (Parcelable newItem : newItems) {
                    getWrappedAdapter().addItem(mSectionIndex,newItem);
                }
            }
            if (!TextUtils.isEmpty(nextHref)) {
                getWrappedAdapter().sections.get(mSectionIndex).nextHref = nextHref;
            }
            if (!keepGoing) {
                nextAdapterSection();
            } else {
                mState = keepGoing ? WAITING : DONE;
            }
        } else {
            handleResponseCode(responseCode);
            applyEmptyView();
        }
        notifyDataSetChanged();
    }

    private void nextAdapterSection() {
        // end of this section
        for (WeakReference<SectionListener> listenerRef : mListeners) {
            SectionListener listener = listenerRef.get();
            if (listener != null && mSectionIndex < getWrappedAdapter().sections.size()) {
                listener.onSectionLoaded(getWrappedAdapter().sections.get(mSectionIndex));
            }
        }

        // load next section as necessary
        if (getWrappedAdapter().sections.size() - 1 > mSectionIndex) {
            mSectionIndex++;
            mPageIndex = 0;
            mState = WAITING;
        } else {
            mState = DONE;
        }
        notifyDataSetChanged();
    }

    @Override
    protected void onEmptyRefresh(){
       if (super.getCount() == 0) {
           if (mSectionIndex >= getWrappedAdapter().sections.size() - 1) {
               mState = DONE;
           } else {
               nextAdapterSection();
           }
        }
    }

    @Override
    public void allowInitialLoading(){
        super.allowInitialLoading();
    }

    public interface SectionListener {
        void onSectionLoaded(SectionedAdapter.Section seection);
    }

    @Override public boolean needsRefresh() {
        return getWrappedAdapter().sections.size() > 0 && super.needsRefresh();
    }
}
