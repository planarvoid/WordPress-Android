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
    final String STRING_RESOURCE_PREFIX = "rectip_";

    private String[] mRecordSuggestionKeys;
    private String[] mRecordSuggestionKeysPrivate;
    private String mCurrentSuggestionKey;
    private int mCurrentSuggestionIndex;

    @SuppressWarnings("UnusedDeclaration")
    public RecordMessageView(Context context) {
        super(context);
        init();
    }

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

    public void setMessage(String message){
        super.setText(message);
        mCurrentSuggestionIndex = -1;
    }

    private void init(){
        mRecordSuggestionKeys = getResources().getStringArray(R.array.record_suggestion_keys);
        mRecordSuggestionKeysPrivate = getResources().getStringArray(R.array.record_suggestion_keys_private);
    }

    private String getCurrentSuggestionKey(){
        return mCurrentSuggestionKey;
    }

    public void loadSuggestion(String privateUserName){

        int resId;
        int tries = 0;
        final String[] suggestionKeyArray = (TextUtils.isEmpty(privateUserName)) ? mRecordSuggestionKeys : mRecordSuggestionKeysPrivate;
        do {
            mCurrentSuggestionKey = suggestionKeyArray[((int) Math.floor(Math.random() * suggestionKeyArray.length))];
            resId = getResources().getIdentifier(STRING_RESOURCE_PREFIX + mCurrentSuggestionKey,"string",getContext().getPackageName());
            tries++;
        } while (resId == 0 && tries < MAX_TRIES);

        if (resId == 0){
            mCurrentSuggestionKey = "";
            setText(getContext().getString(R.string.rectip_default));
        } else if (TextUtils.isEmpty(privateUserName)){
            setText(getContext().getString(resId));
        } else {
            setText(getContext().getString(resId, privateUserName));
        }
    }


}
