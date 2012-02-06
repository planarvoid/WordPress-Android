package com.soundcloud.android.adapter;

import android.util.Log;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.api.Request;

import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SectionedEndlessAdapter extends RemoteCollectionAdapter {
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
    public Uri getContentUri(boolean refresh) {
        if (mSectionIndex > getWrappedAdapter().sections.size()) return null;
        return getWrappedAdapter().sections.get(refresh ? 0 : mSectionIndex).content;
    }

    @Override
    protected void setNextHref(String nextHref) {
        if (!TextUtils.isEmpty(nextHref)) {
            getWrappedAdapter().sections.get(mSectionIndex).nextHref = nextHref;
        }
    }

    @Override
    protected Request getRequest(boolean refresh) {
        if (mSectionIndex > getWrappedAdapter().sections.size()) return null;
        return getWrappedAdapter().sections.get(refresh ? 0 : mSectionIndex).getRequest(refresh);
    }

    @Override
    public int getPageIndex(boolean refresh) {
        if (mSectionIndex > getWrappedAdapter().sections.size()) return 0;
        return refresh ? 0 : getWrappedAdapter().sections.get(mSectionIndex).pageIndex;
    }

    @Override
    protected void increasePageIndex() {
        getWrappedAdapter().sections.get(mSectionIndex).pageIndex++;
    }

    @Override
    public Class<?> getLoadModel(boolean refresh) {
        return getWrappedAdapter().getLoadModel(refresh ? 0 : mSectionIndex);
    }

    public Class<?> getRefreshModel() {
        return getLoadModel(false);
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
        if (state[1] != null) restoreAppendTask((RemoteCollectionTask) state[1]);
        if (state[2] != null) restorePagingData((int[]) state[2]);
        if (state[3] != null) restoreExtraData((Object[]) state[3]);
    }

    @Override
    public SectionedAdapter getWrappedAdapter() {
        return (SectionedAdapter) super.getWrappedAdapter();
    }

    @Override
    public boolean onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing, boolean wasRefresh) {
        boolean success = super.onPostTaskExecute(newItems,nextHref,responseCode,keepGoing,wasRefresh);
        if (success && !mKeepGoing){
            nextAdapterSection(false);
        }
        return success;
    }

    @Override
    public void applyEmptyView() {
        // only let the empty view be applied if we are at the end of the sections
        if (getWrappedAdapter().sections.size() - 1 == mSectionIndex){
            super.applyEmptyView();
        }
    }

    @Override
    protected void addNewItems(List<Parcelable> newItems){
        if (newItems == null || newItems.size() == 0)  return;
        for (Parcelable newItem : newItems) {
            getWrappedAdapter().addItem(mSectionIndex,newItem);
        }
        checkForStaleItems(newItems);
    }

    private void nextAdapterSection(boolean wasRefresh) {
        // end of this section
        if (!wasRefresh) {
            for (WeakReference<SectionListener> listenerRef : mListeners) {
                SectionListener listener = listenerRef.get();
                if (listener != null && mSectionIndex < getWrappedAdapter().sections.size()) {
                    listener.onSectionLoaded(getWrappedAdapter().sections.get(mSectionIndex));
                }
            }
        }

        // executeAppendTask next section as necessary
        if (getWrappedAdapter().sections.size() - 1 > mSectionIndex) {
            mSectionIndex++;
            getWrappedAdapter().sections.get(mSectionIndex).pageIndex = 0;
            getWrappedAdapter().sections.get(mSectionIndex).nextHref = null;
            mKeepGoing = true;
        } else {
            mKeepGoing = false;
        }
        mState = IDLE;
        notifyDataSetChanged();
    }

    @Override
    protected void onEmptyRefresh(){
       if (super.getCount() == 0) {
           if (mSectionIndex >= getWrappedAdapter().sections.size() - 1) {
               mKeepGoing = false;
               mState = IDLE;
           } else {
               nextAdapterSection(true);
           }
        }
    }

    public interface SectionListener {
        void onSectionLoaded(SectionedAdapter.Section seection);
    }
}
