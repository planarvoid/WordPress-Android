package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Event;

import android.os.Parcelable;
import android.text.TextUtils;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class DashboardQueryTask extends QueryTask {

    private int prependIndex = 0;
    private String mNextCursor;

    public DashboardQueryTask(SoundCloudApplication app) {
        super(app);
    }

    @Override
    protected void onPostExecute(Boolean keepGoing) {
        LazyBaseAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            if (adapter.getCount() == prependIndex && !TextUtils.isEmpty(mNextCursor)){
                ((EventsAdapter)mAdapterReference.get())
                        .onNextEventsCursor(mNextCursor);
            }
            adapter.onPostQueryExecute();
        }
    }


    @Override
    protected void onProgressUpdate(Parcelable...updates) {
        LazyBaseAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            if (updates != null && updates.length > 0 && keepMoving.get()) {
                for (Parcelable newitem : updates) {
                    if (prependIndex < adapter.getData().size() && ((Event) newitem).track.id == ((Event)adapter.getData().get(prependIndex)).track.id){
                        keepMoving.set(false);
                        break;
                    }
                    adapter.getData().add(prependIndex,newitem);
                    mNextCursor = ((Event) newitem).next_cursor;
                    prependIndex++;
                }
            }
        }
    }

}
