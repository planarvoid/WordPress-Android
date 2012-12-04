package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EmptyListView extends RelativeLayout {
    protected ProgressBar mProgressBar;

    @Nullable protected ViewGroup mEmptyLayout;

    private TextView mTxtMessage;
    private TextView mTxtLink;
    private ImageView mImage;
    protected Button mBtnAction;

    private int     mMessageResource, mLinkResource, mImageResource, mActionTextResource;
    private String  mMessage;

    private ActionListener mButtonActionListener;
    private ActionListener mImageActionListener;
    private int mMode;

    public interface Mode {
        int WAITING_FOR_DATA = 1;
        int IDLE = 2;
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

        mProgressBar = (ProgressBar) findViewById(R.id.list_loading);
    }

    public boolean setMode(int mode) {
        if (mMode != mode) {
            mMode = mode;
            switch (mode) {
                case Mode.WAITING_FOR_DATA:
                    mProgressBar.setVisibility(View.VISIBLE);
                    if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);
                    return true;

                case Mode.IDLE:
                    mProgressBar.setVisibility(View.GONE);
                    showEmptyLayout();
                    return true;
            }
            return false;
        }
        return true;
    }

    protected void showEmptyLayout() {
        if (mEmptyLayout == null){
            mEmptyLayout = (ViewGroup) ((ViewStub) findViewById(R.id.empty_collection_stub)).inflate();

            mTxtMessage = (TextView) findViewById(R.id.txt_message);
            if (TextUtils.isEmpty(mMessage)){
                setMessageText(mMessageResource);
            } else {
                setMessageText(mMessage);
            }

            mTxtLink = (TextView) findViewById(R.id.txt_link);
            setSecondaryText(mLinkResource);

            mBtnAction = (Button) findViewById(R.id.btn_action);
            setActionText(mActionTextResource);

            mImage = (ImageView) findViewById(R.id.img_1);
            mImage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mImageActionListener!= null) {
                        mImageActionListener.onAction();
                    }
                }
            });
            setImage(mImageResource);

            mBtnAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mButtonActionListener != null) {
                        mButtonActionListener.onAction();
                    }
                }
            });
            setMode(Mode.WAITING_FOR_DATA);
        } else {
            mEmptyLayout.setVisibility(View.VISIBLE);
        }
    }

    public EmptyListView setImage(int imageId){
        if (mImage != null){
            if (imageId > 0){
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(imageId);
            } else {
                mImage.setVisibility(View.GONE);
            }
        } else {
            mImageResource = imageId;
        }

        return this;
    }

    public EmptyListView setMessageText(int messageId){
        if (mTxtMessage != null) {
            if (messageId > 0) {
                mTxtMessage.setText(messageId);
                mTxtMessage.setVisibility(View.VISIBLE);
            } else {
                mTxtMessage.setVisibility(View.GONE);
            }
        } else {
            mMessageResource = messageId;
            mMessage = null;
        }
        return this;
    }

    public EmptyListView setMessageText(String s) {
        if (mTxtMessage != null) {
            if (!TextUtils.isEmpty(s)) {
                mTxtMessage.setText(s);
                mTxtMessage.setVisibility(View.VISIBLE);
            } else {
                mTxtMessage.setVisibility(View.GONE);
            }
        } else {
            mMessage = s;
            mMessageResource = -1;
        }
        return this;
    }

    public EmptyListView setSecondaryText(int secondaryTextId) {
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
        } else {
            mLinkResource = secondaryTextId;
        }
        return this;
    }

    public EmptyListView setActionText(int textId){
        if (mBtnAction != null){
            if (textId > 0){
                mBtnAction.setVisibility(View.VISIBLE);
                mBtnAction.setText(textId);
            } else {
                mBtnAction.setVisibility(View.INVISIBLE);
            }
        } else {
            mActionTextResource = textId;
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
                                                context.startActivity(new Intent(Actions.FRIEND_FINDER));
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
                return new FriendFinderEmptyCollection(context).setMessageText(R.string.list_empty_user_following_message)
                        .setMessageText(R.string.list_no_facebook_friends)
                        .setActionText(R.string.connect_to_facebook)
                        .setImage(R.drawable.empty_follow_3row);
            default:
                return new EmptyListView(context);
        }
    }

    private static void goTo101s(Context context){
        context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

}
