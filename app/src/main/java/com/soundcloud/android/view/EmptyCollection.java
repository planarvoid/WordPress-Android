package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class EmptyCollection extends FrameLayout {
    protected ViewGroup mSyncLayout, mEmptyLayout;

    private TextView mTxtMessage;
    private TextView mTxtLink;
    private ImageView mImage;
    protected Button mBtnAction;

    private ActionListener mButtonActionListener;
    private ActionListener mImageActionListener;
    private int mMode;

    public interface Mode {
        int WAITING_FOR_DATA = 1;
        int IDLE = 2;
    }

    public EmptyCollection(final Context context, final Intent... intents) {
        super(context);
        setActionListener(context, intents);
        init();
    }

    public EmptyCollection setActionListener(final Context context, final Intent... intents) {
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
                .inflate(R.layout.empty_collection, this);

        mSyncLayout = (ViewGroup) findViewById(R.id.sync_layout);
        mEmptyLayout = (ViewGroup) findViewById(R.id.empty_layout);

        mTxtMessage = (TextView) findViewById(R.id.txt_message);
        mTxtLink = (TextView) findViewById(R.id.txt_link);
        mBtnAction = (Button) findViewById(R.id.btn_action);
        mImage = (ImageView) findViewById(R.id.img_1);

        mImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mImageActionListener!= null) {
                    mImageActionListener.onAction();
                }
            }
        });

        mTxtMessage.setVisibility(View.GONE);
        mTxtLink.setVisibility(View.GONE);

        mBtnAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mButtonActionListener != null) {
                    mButtonActionListener.onAction();
                }
            }
        });
        setMode(Mode.WAITING_FOR_DATA);
    }

    public boolean setMode(int mode) {
        if (mMode != mode) {
            mMode = mode;
            switch (mode) {
                case Mode.WAITING_FOR_DATA:
                    mEmptyLayout.setVisibility(View.GONE);
                    mSyncLayout.setVisibility(View.VISIBLE);
                    findViewById(R.id.txt_sync).setVisibility(View.GONE);
                    return true;

                case Mode.IDLE:
                    mEmptyLayout.setVisibility(View.VISIBLE);
                    mSyncLayout.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
        return true;
    }

    public EmptyCollection setImage(int imageId){
        mImage.setImageResource(imageId);
        return this;
    }

    public EmptyCollection setMessageText(int messageId){
        if(messageId == -1){
            mTxtMessage.setVisibility(View.GONE);
        } else {
            mTxtMessage.setText(messageId);
            mTxtMessage.setVisibility(View.VISIBLE);
        }
        return this;
    }

    public EmptyCollection setMessageText(String s) {
        mTxtMessage.setText(s);
        mTxtMessage.setVisibility(View.VISIBLE);
        return this;
    }

    public EmptyCollection setSecondaryText(int secondaryTextId){
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
        return this;
    }

    public EmptyCollection setActionText(int textId){
        if (textId > 0){
            mBtnAction.setVisibility(View.VISIBLE);
            mBtnAction.setText(textId);
        } else {
            mBtnAction.setVisibility(View.INVISIBLE);
        }
        return this;
    }

    public EmptyCollection setButtonActionListener(ActionListener listener){
        mButtonActionListener = listener;
        return this;
    }

    public EmptyCollection setImageActionListener(ActionListener listener){
        mImageActionListener = listener;
        return this;
    }

    public void setImageVisibility(boolean visible) {
        mImage.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public interface ActionListener {
        void onAction();
        void onSecondaryAction();
    }

    public static EmptyCollection fromContent(final Context context, final Content content) {
        switch (content) {
            case ME_SOUND_STREAM:
                return new EmptyCollection(context).setMessageText(R.string.list_empty_stream_message)
                                        .setImage(R.drawable.empty_follow)
                                        .setActionText(R.string.list_empty_stream_action)
                                        .setSecondaryText(R.string.list_empty_stream_secondary)
                                        .setButtonActionListener(new EmptyCollection.ActionListener() {
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
                                   return new EmptyCollection(context).setMessageText(R.string.list_empty_activity_message)
                                           .setImage(R.drawable.empty_share)
                                           .setActionText(R.string.list_empty_activity_action)
                                           .setSecondaryText(R.string.list_empty_activity_secondary)
                                           .setButtonActionListener(new EmptyCollection.ActionListener() {
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
                                   final EmptyCollection.ActionListener record = new EmptyCollection.ActionListener() {
                                       @Override
                                       public void onAction() {
                                           context.startActivity(new Intent(Actions.RECORD));
                                       }

                                       @Override
                                       public void onSecondaryAction() {
                                           goTo101s(context);
                                       }
                                   };

                                   return new EmptyCollection(context).setMessageText(R.string.list_empty_activity_nosounds_message)
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
                return new EmptyCollection(context);
        }
    }

    private static void goTo101s(Context context){
        context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

}
