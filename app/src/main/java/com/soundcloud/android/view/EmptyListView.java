package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EmptyListView extends RelativeLayout {
    protected ProgressBar mProgressBar;

    @Nullable protected ViewGroup mEmptyLayout;

    private RelativeLayout mEmptyViewHolder;
    private TextView mTxtMessage;
    private TextView mTxtLink;
    @Nullable private ImageView mImage;
    @Nullable private LinearLayout mError;
    protected Button mBtnAction;

    private int     mMessageResource, mLinkResource, mImageResource, mActionTextResource;
    private String  mMessage;

    private ActionListener mButtonActionListener;
    private ActionListener mImageActionListener;
    protected int mMode;

    public interface Mode {
        int WAITING_FOR_DATA = 1;
        int IDLE = 2;
        int ERROR = 3;
    }

    public EmptyListView(final Context context, final Intent... intents) {
        super(context);
        setActionListener(context, intents);
        init();
    }

    public EmptyListView setActionListener(final Context context, final Intent... intents) {
        if (intents.length > 0) {
            setButtonActionListener(new ActionListener() {
                @Override
                public void onAction() {
                    context.startActivity(intents[0]);
                }
                @Override
                public void onSecondaryAction() {
                    if (intents.length > 1) {
                        context.startActivity(intents[1]);
                    }
                }
            });
        }
        return this;
    }

    private void init(){
        ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.empty_list, this);

        mEmptyViewHolder = ((RelativeLayout) findViewById(R.id.empty_view_holder));
        mProgressBar = (ProgressBar) findViewById(R.id.list_loading);
    }

    public boolean setMode(int mode) {
        if (mMode != mode) {
            mMode = mode;
            switch (mode) {
                case Mode.WAITING_FOR_DATA:
                    mProgressBar.setVisibility(View.VISIBLE);
                    if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);
                    if (mError != null) mError.setVisibility(View.GONE);
                    return true;

                case Mode.IDLE:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    return true;

                case Mode.ERROR:
                    mProgressBar.setVisibility(View.GONE);
                    showError();
                    return true;
            }
            return false;
        }
        return true;
    }

    private void showError(){
        if (mError == null) {
            mError = (LinearLayout) View.inflate(getContext(), R.layout.empty_list_error, null);
            mEmptyViewHolder.addView(mError);
        } else {
            mError.setVisibility(View.VISIBLE);
        }
        if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);
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
            setSecondaryText(mLinkResource);
            setActionText(mActionTextResource);


        } else {
            mEmptyLayout.setVisibility(View.VISIBLE);
        }


        if (mError != null) mError.setVisibility(View.GONE);
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

    public EmptyListView setSecondaryText(int secondaryTextId) {
        mLinkResource = secondaryTextId;
        if (mTxtLink != null) {
            if (secondaryTextId > 0) {
                mTxtLink.setText(secondaryTextId);
                mTxtLink.setVisibility(View.VISIBLE);
                ScTextUtils.clickify(mTxtLink, mTxtLink.getText().toString(), new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mButtonActionListener != null) {
                            mButtonActionListener.onSecondaryAction();
                        }
                    }
                }, true);
            } else {
                mTxtLink.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setActionText(int textId){
        mActionTextResource = textId;
        if (mBtnAction != null){
            if (textId > 0){
                mBtnAction.setVisibility(View.VISIBLE);
                mBtnAction.setText(textId);
            } else {
                mBtnAction.setVisibility(View.INVISIBLE);
            }
        }
        return this;
    }

    public EmptyListView setButtonActionListener(ActionListener listener){
        mButtonActionListener = listener;
        return this;
    }

    public EmptyListView setImageActionListener(ActionListener listener){
        mImageActionListener = listener;
        return this;
    }

    public interface ActionListener {
        void onAction();
        void onSecondaryAction();
    }

    public static EmptyListView fromContent(final Context context, final Content content) {
        switch (content) {
            case ME_SOUND_STREAM:
                return new EmptyListView(context).setMessageText(R.string.list_empty_stream_message)
                                        .setImage(R.drawable.empty_follow)
                                        .setActionText(R.string.list_empty_stream_action)
                                        .setSecondaryText(R.string.list_empty_stream_secondary)
                                        .setButtonActionListener(new EmptyListView.ActionListener() {
                                            @Override
                                            public void onAction() {
                                                context.startActivity(new Intent(Actions.WHO_TO_FOLLOW));
                                            }

                                            @Override
                                            public void onSecondaryAction() {
                                                context.startActivity(new Intent(Actions.FRIEND_FINDER));
                                            }
                                        });
            case ME_ACTIVITIES:
                User loggedInUser = SoundCloudApplication.fromContext(context).getLoggedInUser();
                if (loggedInUser == null || loggedInUser.track_count > 0) {
                                   return new EmptyListView(context).setMessageText(R.string.list_empty_activity_message)
                                           .setImage(R.drawable.empty_share)
                                           .setActionText(R.string.list_empty_activity_action)
                                           .setSecondaryText(R.string.list_empty_activity_secondary)
                                           .setButtonActionListener(new EmptyListView.ActionListener() {
                                               @Override
                                               public void onAction() {
                                                   context.startActivity(new Intent(Actions.YOUR_SOUNDS));
                                               }

                                               @Override
                                               public void onSecondaryAction() {
                                                   goTo101s(context);
                                               }
                                           });
                               } else {
                                   final EmptyListView.ActionListener record = new EmptyListView.ActionListener() {
                                       @Override
                                       public void onAction() {
                                           context.startActivity(new Intent(Actions.RECORD));
                                       }

                                       @Override
                                       public void onSecondaryAction() {
                                           goTo101s(context);
                                       }
                                   };

                                   return new EmptyListView(context).setMessageText(R.string.list_empty_activity_nosounds_message)
                                           .setImage(R.drawable.empty_rec)
                                           .setActionText(R.string.list_empty_activity_nosounds_action)
                                           .setSecondaryText(R.string.list_empty_activity_nosounds_secondary)
                                           .setButtonActionListener(record)
                                           .setImageActionListener(record);
                               }

            case ME_FRIENDS:
                return new FriendFinderEmptyCollection(context);

            default:
                return new EmptyListView(context);
        }
    }

    private static void goTo101s(Context context){
        context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

}
