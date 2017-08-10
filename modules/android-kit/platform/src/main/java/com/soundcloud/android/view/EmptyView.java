package com.soundcloud.android.view;

import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.androidkit.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
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

    @Nullable private ViewGroup emptyLayout;
    @Nullable private ImageView image;
    @Nullable private ErrorView errorView;

    private View progressView;
    private Button buttonAction;
    private TextView textMessage;
    private TextView textLink;

    private int messageResource, imageResource;
    private String message, secondaryText, actionText;

    private ActionListener buttonActionListener;
    private Status status;

    public enum Status {
        WAITING,
        ERROR,
        CONNECTION_ERROR,
        SERVER_ERROR,
        OK
    }

    public EmptyView(final Context context) {
        super(context);
        init(R.layout.ak_empty_view);
    }

    public EmptyView(final Context context, int layoutId) {
        super(context);
        init(layoutId);
    }

    public EmptyView(final Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(R.layout.ak_empty_view);
    }

    @SuppressWarnings("unused")
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

        final Animation animationIn = AnimationUtils.loadAnimation(getContext(), R.anim.ak_fade_in_med);
        setLayoutAnimation(new LayoutAnimationController(animationIn));

        progressView = findViewById(R.id.ak_emptyview_progress);

        setClickable(true); // needs to be clickable for swipe to refresh gesture
        setBackgroundFromAttribute();
    }

    private void setBackgroundFromAttribute() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();

        final boolean resolved = theme.resolveAttribute(R.attr.emptyViewBackgroundColor, typedValue, true);
        if (resolved) {
            setBackgroundColor(typedValue.data);
        }

    }

    public boolean setStatus(Status status) {
        if (this.status != status) {
            this.status = status;

            if (status == Status.WAITING) {
                // don't show empty screen, show progress
                progressView.setVisibility(View.VISIBLE);
                if (emptyLayout != null) {
                    emptyLayout.setVisibility(View.GONE);
                }
                if (errorView != null) {
                    errorView.setVisibility(View.GONE);
                }
                return true;

            } else {
                AnimUtils.hideView(progressView, false);
                if (status == Status.OK) {
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

    private void showError(Status status) {
        if (errorView == null) {
            errorView = addErrorView();
        }
        AnimUtils.showView(errorView, true);

        if (emptyLayout != null) {
            AnimUtils.hideView(emptyLayout, false);
        }

        switch (status) {
            case SERVER_ERROR:
                errorView.setServerErrorState();
                break;
            case CONNECTION_ERROR:
                errorView.setConnectionErrorState();
                break;
            default:
                //TODO: might consider handling this separately
                errorView.setServerErrorState();
        }
    }

    protected ErrorView addErrorView() {
        @SuppressLint("InflateParams") ErrorView errorView = (ErrorView) LayoutInflater.from(getContext()).inflate(R.layout.ak_error_view, null);
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(errorView, params);
        return errorView;
    }

    protected void showEmptyLayout() {
        if (emptyLayout == null) {
            setupViews();
            // set values
            if (TextUtils.isEmpty(message)) {
                setMessageText(messageResource);
            } else {
                setMessageText(message);
            }
            setSecondaryText(secondaryText);
            setActionText(actionText);
        }

        AnimUtils.showView(emptyLayout, true);

        if (errorView != null) {
            AnimUtils.hideView(errorView, false);
        }
    }

    private void setupViews() {
        emptyLayout = (ViewGroup) View.inflate(getContext(), getEmptyViewLayoutId(), null);

        final LayoutParams params =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        addView(emptyLayout, params);

        textMessage = (TextView) findViewById(R.id.ak_emptyview_message);
        textLink = (TextView) findViewById(R.id.ak_emptyview_link);
        buttonAction = (Button) findViewById(R.id.ak_emptyview_btn_action);
        image = (ImageView) findViewById(R.id.ak_emptyview_image);

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
    }

    protected int getEmptyViewLayoutId() {
        return R.layout.ak_empty_collection_view;
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

    public EmptyView setActionText(String actionText) {
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

    public interface ActionListener {
        void onAction();
    }

}
