package com.soundcloud.android.streaming;

import android.text.TextUtils;
import com.soundcloud.android.utils.CloudUtils;

public class ScStreamItem {

    public static String SCStreamItemDidResetNotification = "SCStreamItemDidResetNotification";

    public String URL;
    public String redirectedURL;
    public boolean enabled;

    private String mURLHash;
    private long mContentLength;


    public void setContentLength(long value){
        if (mContentLength == value) return;

        boolean reset = false;
        if (mContentLength != 0){
            reset = true;
        }
        mContentLength = value;

        /*
        notify(new Intent("SCStreamItemDidResetNotification"));
        */
    }

    public long getContentLength(){
        return mContentLength;
    }

    public String getURLHash(){
        if (TextUtils.isEmpty(mURLHash) && !TextUtils.isEmpty(URL)){
            mURLHash = CloudUtils.md5(URL);
        }
        return mURLHash;
    }

    @Override
    public String toString() {
            return "ScStreamItem{url: " + URL +
                    ", redirectedURL:" + redirectedURL +
                    ", URLHash:" + mURLHash +
                    ", contentLength:" + mContentLength +
                    ", enabled:" + enabled +
                    "}";
        }

}
