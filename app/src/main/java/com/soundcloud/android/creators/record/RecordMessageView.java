package com.soundcloud.android.creators.record;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * TextView that handles recording suggestion functionality
 */

public class RecordMessageView extends TextView {

    private String[] recordSuggestions;
    private String currentSuggestion;

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
        super.setText(message, BufferType.NORMAL);
        currentSuggestion = "";
    }

    private void init(){
        recordSuggestions = getResources().getStringArray(R.array.record_suggestions);
    }

    public String getCurrentSuggestion(){
        return currentSuggestion;
    }

    public void loadSuggestion(){
        currentSuggestion = recordSuggestions[((int) Math.floor(Math.random() * recordSuggestions.length))];
        setText(currentSuggestion);
    }


}
