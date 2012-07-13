package com.soundcloud.android.view.create;

import com.soundcloud.android.R;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

/**
 * TextView that handles recording suggestion functionality
 */

public class RecordMessageView extends TextView {

    private static final int MAX_TRIES = 5;
    public static final String STRING_RESOURCE_PREFIX = "rectip_";

    private String[] mRecordSuggestionKeys;
    private String[] mRecordSuggestionKeysPrivate;
    private String mCurrentSuggestionKey;

    @SuppressWarnings("UnusedDeclaration")
    public RecordMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RecordMessageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setMessage(int resourceId){
        setMessage(getContext().getString(resourceId));
    }

    public void setMessage(String message){
        super.setText(message);
        mCurrentSuggestionKey = "";
    }

    private void init(){
        mRecordSuggestionKeys = getResources().getStringArray(R.array.record_suggestion_keys);
        mRecordSuggestionKeysPrivate = getResources().getStringArray(R.array.record_suggestion_keys_private);
    }

    private String getCurrentSuggestionKey(){
        return mCurrentSuggestionKey;
    }

    public void loadSuggestion(String privateUserName){

        mCurrentSuggestionKey = (TextUtils.isEmpty(privateUserName)) ?
                                mRecordSuggestionKeys[((int) Math.floor(Math.random() * mRecordSuggestionKeys.length))] :
                                mRecordSuggestionKeysPrivate[((int) Math.floor(Math.random() * mRecordSuggestionKeysPrivate.length))];

        final int resId = getResources().getIdentifier(STRING_RESOURCE_PREFIX + mCurrentSuggestionKey, "string", getContext().getPackageName());

        if (resId == 0){
            // missing resource, should be caught by tests
            mCurrentSuggestionKey = "";
            setText(getContext().getString(R.string.rectip_default));
        } else if (TextUtils.isEmpty(privateUserName)){
            setText(getContext().getString(resId));
        } else {
            setText(getContext().getString(resId, privateUserName));
        }
    }


}
