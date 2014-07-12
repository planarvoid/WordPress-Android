package com.soundcloud.android.view;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.utils.AnimUtils;
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

public class EmptyView extends RelativeLayout {

    protected View progressView;

    @Nullable
    protected ViewGroup emptyLayout;

    private RelativeLayout emptyViewHolder;
    private TextView textMessage;
    private TextView textLink;
    @Nullable
    private ImageView image;
    @Nullable
    private ErrorView errorView;
    protected Button buttonAction;

    private int messageResource, imageResource;
    private String message, secondaryText, actionText;

    private ActionListener buttonActionListener;
    protected int status;
    private RetryListener retryListener;

    public interface Status extends HttpStatus {
        int WAITING = -1;
        int ERROR = -2; //generic error
        int CONNECTION_ERROR = -3;
        int SERVER_ERROR = -4;
        int OK = SC_OK; //generic OK
    }

    public EmptyView(final Context context) {
        super(context);
        init(R.layout.empty_list);
    }

    public EmptyView(final Context context, int layoutId) {
        super(context);
        init(layoutId);
    }

    public EmptyView(final Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(R.layout.empty_list);
    }

    public EmptyView setButtonActions(@Nullable final Intent action) {
        setActionListener(new ActionListener() {
            @Override
            public void onAction() {
                if (action != null) {
                    getContext().startActivity(action);
                }
            }
        });
        return this;
    }

    private void init(int layoutId) {
        ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(layoutId, this);

        final Animation animationIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_med);
        setLayoutAnimation(new LayoutAnimationController(animationIn));

        emptyViewHolder = ((RelativeLayout) findViewById(R.id.empty_view_holder));
        progressView = findViewById(R.id.empty_view_progress);
    }

    public boolean setStatus(int status) {
        if (this.status != status) {
            this.status = status;

            if (status == Status.WAITING) {

                // don't show empty screen, show progress
                progressView.setVisibility(View.VISIBLE);
                if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
                if (errorView != null) errorView.setVisibility(View.GONE);
                return true;

            } else {
                AnimUtils.hideView(getContext(), progressView, false);
                if (PublicApiWrapper.isStatusCodeOk(status)) {
                    // at rest, no error
                    showEmptyLayout();
                    return true;

                } else {
                    // error,
                    showError(status);
                    return true;
                }
            }

        }
        return true;
    }

    public int getStatus() {
        return status;
    }

    private void showError(int responseCode) {
        if (errorView == null) {
            errorView = addErrorView();
            errorView.setOnRetryListener(retryListener);
        }
        AnimUtils.showView(getContext(), errorView, true);

        if (emptyLayout != null) {
            AnimUtils.hideView(getContext(), emptyLayout, false);
        }

        if (PublicApiWrapper.isStatusCodeError(responseCode) || responseCode == Status.SERVER_ERROR) {
            errorView.setUnexpectedResponseState();
        } else {
            errorView.setConnectionErrorState();
        }
    }

    @VisibleForTesting
    protected ErrorView addErrorView() {
        ErrorView errorView = (ErrorView) LayoutInflater.from(getContext()).inflate(R.layout.error_view, null);
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        emptyViewHolder.addView(errorView, params);
        return errorView;
    }

    protected void showEmptyLayout() {
        if (emptyLayout == null) {
            emptyLayout = (ViewGroup) View.inflate(getContext(), getEmptyViewLayoutId(), null);

            final RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            emptyViewHolder.addView(emptyLayout, params);

            textMessage = (TextView) findViewById(R.id.txt_message);
            textLink = (TextView) findViewById(R.id.txt_link);
            buttonAction = (Button) findViewById(R.id.btn_action);
            image = (ImageView) findViewById(R.id.empty_state_image);
            if (image != null) {
                setImage(imageResource);
            }

            buttonAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (buttonActionListener != null) {
                        buttonActionListener.onAction();
                    }
                }
            });

            // set values
            if (TextUtils.isEmpty(message)) {
                setMessageText(messageResource);
            } else {
                setMessageText(message);
            }
            setSecondaryText(secondaryText);
            setActionText(actionText);
        }

        AnimUtils.showView(getContext(), emptyLayout, true);

        if (errorView != null) {
            AnimUtils.hideView(getContext(), errorView, false);
        }
    }

    protected int getEmptyViewLayoutId() {
        return R.layout.empty_collection_view;
    }

    public EmptyView setImage(int imageId) {
        imageResource = imageId;
        if (image != null) {
            if (imageId > 0) {
                image.setVisibility(View.VISIBLE);
                image.setImageResource(imageId);
            } else {
                image.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyView setMessageText(int messageId) {
        messageResource = messageId;
        message = null;
        if (textMessage != null) {
            if (messageId > 0) {
                textMessage.setText(messageId);
                textMessage.setVisibility(View.VISIBLE);
            } else {
                textMessage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyView setMessageText(String s) {
        message = s;
        messageResource = -1;
        if (textMessage != null) {
            if (!TextUtils.isEmpty(s)) {
                textMessage.setText(s);
                textMessage.setVisibility(View.VISIBLE);
            } else {
                textMessage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyView setSecondaryText(int secondaryTextId) {
        return setSecondaryText(getResources().getString(secondaryTextId));
    }

    public EmptyView setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
        if (textLink != null) {
            if (secondaryText != null) {
                textLink.setText(secondaryText);
                textLink.setVisibility(View.VISIBLE);
            } else {
                textLink.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyView setActionText(@Nullable String actionText) {
        this.actionText = actionText;
        if (buttonAction != null) {
            if (actionText != null) {
                buttonAction.setVisibility(View.VISIBLE);
                buttonAction.setText(actionText);
            } else {
                buttonAction.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyView setActionText(int actionTextResId) {
        return setActionText(getResources().getString(actionTextResId));
    }

    public EmptyView setActionListener(ActionListener listener) {
        buttonActionListener = listener;
        return this;
    }

    public EmptyView setOnRetryListener(RetryListener listener) {
        retryListener = listener;
        if (errorView != null) {
            errorView.setOnRetryListener(listener);
        }
        return this;
    }

    public interface RetryListener {
        void onEmptyViewRetry();
    }

    public interface ActionListener {
        void onAction();
    }

}
