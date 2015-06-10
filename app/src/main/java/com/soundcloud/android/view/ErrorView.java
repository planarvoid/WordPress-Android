package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ErrorView extends RelativeLayout {

    private ImageView imageView;
    private TextView serverErrorText;
    private TextView connectionErrorTitle;
    private TextView connectionErrorSubtitle;

    public ErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnRetryListener(final EmptyView.RetryListener retryListener) {
        final View btnRetry = findViewById(R.id.btn_retry);
        if (retryListener != null){
            btnRetry.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (retryListener != null) {
                        retryListener.onEmptyViewRetry();
                    }
                }
            });
        } else {
            btnRetry.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        imageView = (ImageView) findViewById(R.id.img_error);
        serverErrorText = (TextView) findViewById(R.id.server_error);
        connectionErrorTitle = (TextView) findViewById(R.id.connection_error_1);
        connectionErrorSubtitle = (TextView) findViewById(R.id.connection_error_2);
    }

    public void setServerErrorState() {
        imageView.setImageResource(R.drawable.error_message_soundcloud);
        serverErrorText.setVisibility(View.VISIBLE);
        connectionErrorTitle.setVisibility(View.GONE);
        connectionErrorSubtitle.setVisibility(View.GONE);
    }

    public void setConnectionErrorState() {
        imageView.setImageResource(R.drawable.error_message_internet);
        serverErrorText.setVisibility(View.GONE);
        connectionErrorTitle.setVisibility(View.VISIBLE);
        connectionErrorSubtitle.setVisibility(View.VISIBLE);
    }
}
