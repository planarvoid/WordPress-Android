package com.soundcloud.android.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

public class EmptyCollection extends FrameLayout {

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
        CloudUtils.clickify(mTxtLink, mTxtLink.getText().toString(), new ClickSpan.OnClickListener() {
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

}
