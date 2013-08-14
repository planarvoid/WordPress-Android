package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ErrorView extends LinearLayout {

    private ImageView mImageView;
    private TextView mServerErrorText;
    private TextView mClientErrorText1;
    private TextView mClientErrorText2;

    public ErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ErrorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        mClientErrorText1 = (TextView) findViewById(R.id.client_error_1);
        mClientErrorText2 = (TextView) findViewById(R.id.client_error_2);
    }

    public void setServerErrorState() {
        mImageView.setImageResource(R.drawable.error_message_soundcloud);
        mServerErrorText.setVisibility(View.VISIBLE);
        mClientErrorText1.setVisibility(View.GONE);
        mClientErrorText2.setVisibility(View.GONE);
    }

    public void setClientErrorState() {
        mImageView.setImageResource(R.drawable.error_message_internet);
        mServerErrorText.setVisibility(View.GONE);
        mClientErrorText1.setVisibility(View.VISIBLE);
        mClientErrorText2.setVisibility(View.VISIBLE);
    }
}
