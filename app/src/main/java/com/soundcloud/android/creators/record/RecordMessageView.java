package com.soundcloud.android.creators.record;

import com.soundcloud.android.R;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * TextView that handles recording suggestion functionality
 */

public class RecordMessageView extends TextView {

    public static final String STRING_RESOURCE_PREFIX = "rectip_";

    private String[] recordSuggestionKeys;
    private String[] recordSuggestionKeysPrivate;
    private String currentSuggestionKey;

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
        currentSuggestionKey = "";
    }

    private void init(){
        recordSuggestionKeys = getResources().getStringArray(R.array.record_suggestion_keys);
        recordSuggestionKeysPrivate = getResources().getStringArray(R.array.record_suggestion_keys_private);
    }

    public String getCurrentSuggestionKey(){
        return currentSuggestionKey;
    }

    public void loadSuggestion(String privateUserName){

        currentSuggestionKey = (TextUtils.isEmpty(privateUserName)) ?
                                recordSuggestionKeys[((int) Math.floor(Math.random() * recordSuggestionKeys.length))] :
                                recordSuggestionKeysPrivate[((int) Math.floor(Math.random() * recordSuggestionKeysPrivate.length))];

        final int resId = getResources().getIdentifier(STRING_RESOURCE_PREFIX + currentSuggestionKey, "string", getContext().getPackageName());

        if (resId == 0){
            // missing resource, should be caught by tests
            currentSuggestionKey = "";
            setText(getContext().getString(R.string.rectip_default));
        } else if (TextUtils.isEmpty(privateUserName)){
            setText(getContext().getString(resId));
        } else {
            setText(getContext().getString(resId, privateUserName));
        }
    }


}
