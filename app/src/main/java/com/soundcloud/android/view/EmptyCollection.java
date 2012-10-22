package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.util.Log;
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
    private ActionListener mButtonActionListener;

    private ActionListener mImageActionListener;
    private Mode mMode;

    public enum Mode {
        WAITING_FOR_SYNC, WAITING_FOR_DATA, IDLE
    }

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

    public void setMode(Mode mode) {
        if (mMode != mode) {
            mMode = mode;
            switch (mode) {
                case WAITING_FOR_SYNC:
                    mEmptyLayout.setVisibility(View.GONE);
                    mSyncLayout.setVisibility(View.VISIBLE);
                    findViewById(R.id.txt_sync).setVisibility(View.VISIBLE);
                    break;
                case WAITING_FOR_DATA:
                    mEmptyLayout.setVisibility(View.GONE);
                    mSyncLayout.setVisibility(View.VISIBLE);
                    findViewById(R.id.txt_sync).setVisibility(View.GONE);
                    break;
                case IDLE:

                    mEmptyLayout.setVisibility(View.VISIBLE);
                    mSyncLayout.setVisibility(View.GONE);
                    break;

            }
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

    public static EmptyCollection fromContent(Context context, Content content) {
        switch (content) {
            case ME_FAVORITES:
                return new EmptyCollection(context).setMessageText(R.string.list_empty_user_following_message)
                        .setActionText(R.string.list_empty_user_following_action)
                        .setImage(R.drawable.empty_follow_3row)
                        .setButtonActionListener(new EmptyCollection.ActionListener() {
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
