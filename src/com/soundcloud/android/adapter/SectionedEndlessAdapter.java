package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.task.LoadFollowingsTask;
import com.soundcloud.android.view.FriendFinderView;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SectionedEndlessAdapter extends LazyEndlessAdapter{

    private FriendFinderView mFriendFinderView;

    private boolean mSuggestedMode;
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

    public void clearData(){
        mSectionIndex = 0;
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

        rebindPendingView(pendingPosition, pendingView);
        pendingView = null;
        pendingPosition = -1;
        notifyDataSetChanged();

        if (keepgoing != null) {
            if (!keepgoing){
                  for (WeakReference<SectionListener> listenerRef : mListeners) {
                        if (listenerRef.get() != null) {
                            listenerRef.get().onSectionLoaded(((SectionedAdapter) this.getWrappedAdapter()).sections.get(mSectionIndex));
                        }
                    }

                    // load next section as necessary
                    if (((SectionedAdapter) this.getWrappedAdapter()).sections.size() - 1 > mSectionIndex) {
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
