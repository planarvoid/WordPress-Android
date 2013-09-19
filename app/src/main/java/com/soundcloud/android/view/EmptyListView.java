package com.soundcloud.android.view;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EmptyListView extends RelativeLayout {
    protected View mProgressView;

    @Nullable protected ViewGroup mEmptyLayout;

    private RelativeLayout mEmptyViewHolder;
    private TextView mTxtMessage;
    private TextView mTxtLink;
    @Nullable private ImageView mImage;
    @Nullable private ErrorView mErrorView;
    protected Button mBtnAction;

    private int     mMessageResource, mImageResource;
    private String  mMessage, mSecondaryText, mActionText;

    private ActionListener mButtonActionListener;
    private ActionListener mImageActionListener;
    protected int mMode;
    private RetryListener mRetryListener;

    public interface Status extends HttpStatus {
        int WAITING = -1;
        int ERROR = -2; //generic error
        int CONNECTION_ERROR = -3;
        int SERVER_ERROR = -4;
        int OK = SC_OK; //generic OK
    }

    public EmptyListView(final Context context) {
        super(context);
        init(R.layout.empty_list);
    }

    public EmptyListView(final Context context, int layoutId) {
        super(context);
        init(layoutId);
    }

    public EmptyListView(final Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(R.layout.empty_list);
    }

    public EmptyListView setButtonActions(@Nullable final Intent primaryAction, @Nullable final Intent secondaryAction) {
        setActionListener(new ActionListener() {
            @Override
            public void onAction() {
                if (primaryAction != null) {
                    getContext().startActivity(primaryAction);
                }
            }

            @Override
            public void onSecondaryAction() {
                if (secondaryAction != null) {
                    getContext().startActivity(secondaryAction);
                }
            }
        });
        return this;
    }

    private void init(int layoutId){
        ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(layoutId, this);

        final Animation animationIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_med);
        setLayoutAnimation(new LayoutAnimationController(animationIn));

        mEmptyViewHolder = ((RelativeLayout) findViewById(R.id.empty_view_holder));
        mProgressView = findViewById(R.id.loading);
    }

    /**
     * Configure display based on response code
     * @param code
     * @return whether the code was handled here
     */
    public boolean setStatus(int code) {
        if (mMode != code) {
            mMode = code;

            if (code == Status.WAITING) {

                // don't show empty screen, show progress
                mProgressView.setVisibility(View.VISIBLE);
                if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);
                if (mErrorView != null) mErrorView.setVisibility(View.GONE);
                return true;

            } else {
                AnimUtils.hideView(getContext(), mProgressView, false);
                if (Wrapper.isStatusCodeOk(code)) {
                    // at rest, no error
                    showEmptyLayout();
                    return true;

                } else {
                    // error,
                    showError(code);
                    return true;
                }
            }

        }
        return true;
    }

    public int getStatus() {
        return mMode;
    }

    private void showError(int responseCode){
        if (mErrorView == null) {
            mErrorView = addErrorView();
            mErrorView.setOnRetryListener(mRetryListener);
        }
        AnimUtils.showView(getContext(), mErrorView, true);

        if (mEmptyLayout != null) {
            AnimUtils.hideView(getContext(), mEmptyLayout, false);
        }

        if (Wrapper.isStatusCodeError(responseCode) || responseCode == Status.SERVER_ERROR){
            mErrorView.setUnexpectedResponseState();
        } else {
            mErrorView.setConnectionErrorState();
        }
    }

    @VisibleForTesting
    protected ErrorView addErrorView() {
        ErrorView errorView = (ErrorView) LayoutInflater.from(getContext()).inflate(R.layout.error_view, null);
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mEmptyViewHolder.addView(errorView, params);
        return errorView;
    }

    protected void showEmptyLayout() {
        if (mEmptyLayout == null){
            mEmptyLayout = (ViewGroup) View.inflate(getContext(), getEmptyViewLayoutId(), null);

            final RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            mEmptyViewHolder.addView(mEmptyLayout, params);

            mTxtMessage = (TextView) findViewById(R.id.txt_message);
            mTxtLink = (TextView) findViewById(R.id.txt_link);
            mBtnAction = (Button) findViewById(R.id.btn_action);
            mImage = (ImageView) findViewById(R.id.img_1);
            if (mImage != null) {
                mImage.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mImageActionListener != null) {
                            mImageActionListener.onAction();
                        }
                    }
                });
                setImage(mImageResource);
            }

            mBtnAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mButtonActionListener != null) {
                        mButtonActionListener.onAction();
                    }
                }
            });

            // set values
            if (TextUtils.isEmpty(mMessage)) {
                setMessageText(mMessageResource);
            } else {
                setMessageText(mMessage);
            }
            setSecondaryText(mSecondaryText);
            setActionText(mActionText);
        }

        AnimUtils.showView(getContext(), mEmptyLayout, true);

        if (mErrorView != null) {
            AnimUtils.hideView(getContext(), mErrorView, false);
        }
    }

    protected int getEmptyViewLayoutId() {
        return R.layout.empty_collection_view;
    }

    public EmptyListView setImage(int imageId){
        mImageResource = imageId;
        if (mImage != null){
            if (imageId > 0){
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(imageId);
            } else {
                mImage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setMessageText(int messageId){
        mMessageResource = messageId;
        mMessage = null;
        if (mTxtMessage != null) {
            if (messageId > 0) {
                mTxtMessage.setText(messageId);
                mTxtMessage.setVisibility(View.VISIBLE);
            } else {
                mTxtMessage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setMessageText(String s) {
        mMessage = s;
        mMessageResource = -1;
        if (mTxtMessage != null) {
            if (!TextUtils.isEmpty(s)) {
                mTxtMessage.setText(s);
                mTxtMessage.setVisibility(View.VISIBLE);
            } else {
                mTxtMessage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setSecondaryText(String secondaryText) {
        mSecondaryText = secondaryText;
        if (mTxtLink != null) {
            if (secondaryText != null) {
                mTxtLink.setText(secondaryText);
                mTxtLink.setVisibility(View.VISIBLE);
                ScTextUtils.clickify(mTxtLink, mTxtLink.getText().toString(), new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mButtonActionListener != null) {
                            mButtonActionListener.onSecondaryAction();
                        }
                    }
                }, true, false);
            } else {
                mTxtLink.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setActionText(@Nullable String actionText){
        mActionText = actionText;
        if (mBtnAction != null) {
            if (actionText != null) {
                mBtnAction.setVisibility(View.VISIBLE);
                mBtnAction.setText(actionText);
            } else {
                mBtnAction.setVisibility(View.INVISIBLE);
            }
        }
        return this;
    }

    public EmptyListView setActionListener(ActionListener listener){
        mButtonActionListener = listener;
        return this;
    }

    public EmptyListView setImageActions(@Nullable final Intent primaryAction, @Nullable final Intent secondaryAction) {
        setImageActionListener(new ActionListener() {
            @Override
            public void onAction() {
                if (primaryAction != null) {
                    getContext().startActivity(primaryAction);
                }
            }

            @Override
            public void onSecondaryAction() {
                if (secondaryAction != null) {
                    getContext().startActivity(secondaryAction);
                }
            }
        });
        return this;
    }

    public EmptyListView setImageActionListener(ActionListener listener){
        mImageActionListener = listener;
        return this;
    }

    public EmptyListView setOnRetryListener(RetryListener listener) {
        mRetryListener = listener;
        if (mErrorView != null) {
            mErrorView.setOnRetryListener(listener);
        }
        return this;
    }

    public interface RetryListener {
        void onEmptyViewRetry();
    }

    public interface ActionListener {
        void onAction();
        void onSecondaryAction();
    }

}
