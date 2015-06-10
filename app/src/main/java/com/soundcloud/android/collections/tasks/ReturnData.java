package com.soundcloud.android.collections.tasks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.ScModel;
import org.apache.http.HttpStatus;

import java.util.List;

public class ReturnData<T extends ScModel> {

    public final List<T> newItems;
    public final String nextHref;
    public final int responseCode;
    public final boolean keepGoing;
    public final boolean success;
    public final boolean wasRefresh;

    public ReturnData(CollectionParams<T> params) {
        this(null, params, null, HttpStatus.SC_OK, false, false);
    }

    public ReturnData(List<T> newItems,
                      CollectionParams<T> params,
                      String nextHref,
                      int responseCode,
                      boolean keepGoing,
                      boolean success) {

        this.wasRefresh = params.isRefresh();
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


    public static class Error<T extends ScModel> extends ReturnData<T> {
        public Error(CollectionParams<T> parameters) {
            this(parameters, Consts.NOT_SET);
        }

        public Error(CollectionParams<T> parameters, int statusCode) {
            super(null, parameters, null, statusCode, false, false);
        }
    }
}
