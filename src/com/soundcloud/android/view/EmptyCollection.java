package com.soundcloud.android.view;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.soundcloud.android.R;

public class EmptyCollection extends FrameLayout {

    private TextView mTxtMessage;
    private TextView mTxtLink;
    private ImageView mImage;
    private Button mBtnAction;

    public EmptyCollection(Context context) {
        super(context);
        init();
    }

    public EmptyCollection(Context context, int imageId, int messageId, int linkId, OnClickListener listener) {
        super(context);
        init();
        setImage(imageId);
        setMessageText(messageId);
        setLinkText(linkId);
        setActionListener(listener);
    }

    private void init(){
        ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.empty_collection, this);

        mTxtMessage = (TextView) findViewById(R.id.txt_message);
        mTxtLink = (TextView) findViewById(R.id.txt_link);
        mBtnAction = (Button) findViewById(R.id.btn_action);
        mImage = (ImageView) findViewById(R.id.img_1);
    }

    protected void setImage(int imageId){
        mImage.setImageResource(imageId);
    }

    protected void setMessageText(int messageId){
        mTxtMessage.setText(messageId);
    }

    protected void setLinkText(int linkId){
        mTxtLink.setText(Html.fromHtml(getResources().getString(linkId)));
    }

    protected void setActionListener(OnClickListener listener){
        mBtnAction.setOnClickListener(listener);
    }

}
