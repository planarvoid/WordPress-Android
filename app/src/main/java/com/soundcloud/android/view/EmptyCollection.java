package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class EmptyCollection extends FrameLayout {

    private ViewGroup mSyncLayout;
    private ViewGroup mEmptyLayout;

    private TextView mTxtMessage;
    private TextView mTxtLink;
    private ImageView mImage;
    private Button mBtnAction;
    private ActionListener mActionListener;

    public EmptyCollection(Context context) {
        super(context);
        init();
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

        mTxtMessage.setVisibility(View.GONE);
        mTxtLink.setVisibility(View.GONE);

        mBtnAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.onAction();
                }
            }
        });
    }

    public void setHasSyncedBefore(boolean hasSyncedBefore) {
        if (hasSyncedBefore) {
            mSyncLayout.setVisibility(View.GONE);
            mEmptyLayout.setVisibility(View.VISIBLE);
        } else {
            mEmptyLayout.setVisibility(View.GONE);
            mSyncLayout.setVisibility(View.VISIBLE);
        }
    }

    public EmptyCollection setImage(int imageId){
        mImage.setImageResource(imageId);
        return this;
    }

    public EmptyCollection setMessageText(int messageId){
        mTxtMessage.setText(messageId);
        mTxtMessage.setVisibility(View.VISIBLE);
        return this;
    }

    public void setMessageText(String s) {
        mTxtMessage.setText(s);
        mTxtMessage.setVisibility(View.VISIBLE);
    }

    public EmptyCollection setSecondaryText(int secondaryTextId){
        mTxtLink.setText(secondaryTextId);
        mTxtLink.setVisibility(View.VISIBLE);
        ScTextUtils.clickify(mTxtLink, mTxtLink.getText().toString(), new ScTextUtils.ClickSpan.OnClickListener() {
            @Override
            public void onClick() {
                if (mActionListener != null) {
                    mActionListener.onSecondaryAction();
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

    public EmptyCollection setActionListener(ActionListener listener){
        mActionListener = listener;
        return this;
    }

    public void setImageVisibility(boolean visible) {
        mImage.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public interface ActionListener {
        void onAction();
        void onSecondaryAction();
    }

    public static EmptyCollection fromContent(Context context, Content content) {
        switch (content) {
            case ME_FAVORITES:
                return new EmptyCollection(context).setMessageText(R.string.list_empty_user_following_message)
                        .setActionText(R.string.list_empty_user_following_action)
                        .setImage(R.drawable.empty_follow_3row)
                        .setActionListener(new EmptyCollection.ActionListener() {
                            @Override
                            public void onAction() {
                                // go to friend finder
                            }

                            @Override
                            public void onSecondaryAction() {
                            }
                        });
            default:
                return new EmptyCollection(context);
        }
    }

}
