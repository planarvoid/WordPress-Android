package com.soundcloud.android.task.collection;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.view.EmptyListView;

import java.util.List;

public class ReturnData<T extends ScModel> {

    public final List<T> newItems;
    public final String nextHref;
    public final int responseCode;
    public final boolean keepGoing;
    public final boolean success;
    public final boolean wasRefresh;

    public ReturnData(CollectionParams<T> params) {
        this(null, params, null, EmptyListView.Status.OK, false, false);
    }

    public ReturnData(List<T> newItems,
                      CollectionParams<T> params,
                      String nextHref,
                      int responseCode,
                      boolean keepGoing,
                      boolean success) {

        this.wasRefresh = params.isRefresh;
        this.newItems = newItems;
        this.nextHref = nextHref;
        this.keepGoing = keepGoing;
        this.success = success;
        this.responseCode = responseCode;
    }

    @Override
    public String toString() {
        return "ReturnData{" +
                "keepGoing=" + keepGoing +
                ", newItems=" + newItems +
                ", nextHref='" + nextHref + '\'' +
                ", responseCode=" + responseCode +
                ", wasRefresh=" + wasRefresh +
                ", success=" + success +
                '}';
    }


    public  static class Error<T extends ScModel> extends ReturnData<T> {
        public Error() {
            super(null, null, null, EmptyListView.Status.CONNECTION_ERROR, false, false);
        }
    }
}
