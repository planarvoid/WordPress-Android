package com.soundcloud.android.view;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
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
    }

    public void setImage(int imageId){
        mImage.setImageResource(imageId);
    }

    public void setMessageText(int messageId){
        mTxtMessage.setText(messageId);
    }

    public void setSecondary(int secondaryTextId, ClickSpan.OnClickListener listener){
        mTxtLink.setText(secondaryTextId);
        CloudUtils.clickify(mTxtLink, mTxtLink.getText().toString(), listener,true);
    }

    public void setAction(int textId, OnClickListener listener){
        mBtnAction.setText(textId);
        mBtnAction.setOnClickListener(listener);
    }

}
