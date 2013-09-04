package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ErrorView extends RelativeLayout {

    private ImageView mImageView;
    private TextView mServerErrorText;
    private TextView mConnectionErrorText1;
    private TextView mConnectionErrorText2;

    public ErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnRetryListener(final EmptyListView.RetryListener retryListener) {
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
        mImageView = (ImageView) findViewById(R.id.img_error);
        mServerErrorText = (TextView) findViewById(R.id.server_error);
        mConnectionErrorText1 = (TextView) findViewById(R.id.connection_error_1);
        mConnectionErrorText2 = (TextView) findViewById(R.id.connection_error_2);
    }

    public void setUnexpectedResponseState() {
        mImageView.setImageResource(R.drawable.error_message_soundcloud);
        mServerErrorText.setVisibility(View.VISIBLE);
        mConnectionErrorText1.setVisibility(View.GONE);
        mConnectionErrorText2.setVisibility(View.GONE);
    }

    public void setConnectionErrorState() {
        mImageView.setImageResource(R.drawable.error_message_internet);
        mServerErrorText.setVisibility(View.GONE);
        mConnectionErrorText1.setVisibility(View.VISIBLE);
        mConnectionErrorText2.setVisibility(View.VISIBLE);
    }
}
