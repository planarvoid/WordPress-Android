package com.soundcloud.android.task.collection;

import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;

public class ReturnData<T extends ScModel> {

    public CollectionHolder<T> newItems;
    public String nextHref;
    public int responseCode;
    public boolean keepGoing;
    public boolean success;
    public boolean wasRefresh;

    public ReturnData(CollectionParams params) {
        this.wasRefresh = params.isRefresh;
    }

    public ReturnData(CollectionHolder<T> newItems,
                      CollectionParams<T> params,
                      String nextHref,
                      int responseCode,
                      boolean keepGoing,
                      boolean success) {

        this(params);
        this.newItems = newItems;
        this.nextHref = nextHref;
        this.responseCode = responseCode;
        this.keepGoing = keepGoing;
        this.success = success;
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
}
