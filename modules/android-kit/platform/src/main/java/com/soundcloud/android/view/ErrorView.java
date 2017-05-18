package com.soundcloud.android.view;

import com.soundcloud.androidkit.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class ErrorView extends ScrollView {

    private ImageView imageView;
    private TextView serverErrorText;
    private TextView connectionErrorTitle;
    private TextView connectionErrorSubtitle;

    public ErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        imageView = (ImageView) findViewById(R.id.ak_emptyview_error_image);
        serverErrorText = (TextView) findViewById(R.id.ak_emptyview_error_message1);
        connectionErrorTitle = (TextView) findViewById(R.id.ak_emptyview_error_message2);
        connectionErrorSubtitle = (TextView) findViewById(R.id.ak_emptyview_error_message3);
    }

    public void setServerErrorState() {
        imageView.setImageResource(R.drawable.ak_emptyview_error_server);
        serverErrorText.setVisibility(View.VISIBLE);
        connectionErrorTitle.setVisibility(View.GONE);
        connectionErrorSubtitle.setVisibility(View.GONE);
    }

    public void setConnectionErrorState() {
        imageView.setImageResource(R.drawable.ak_emptyview_error_connectivity);
        serverErrorText.setVisibility(View.GONE);
        connectionErrorTitle.setVisibility(View.VISIBLE);
        connectionErrorSubtitle.setVisibility(View.VISIBLE);
    }
}
