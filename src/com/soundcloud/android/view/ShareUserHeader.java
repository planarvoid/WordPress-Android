package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShareUserHeader extends RelativeLayout{
    public ShareUserHeader(final ScActivity activity, User u) {
        super(activity);

         LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.share_user_header, this);

        ((TextView) findViewById(R.id.share_header_txt)).setText(Html.fromHtml(getResources().getString(R.string.share_to_soundcloud)));

        CloudUtils.clickify((TextView) findViewById(R.id.share_header_logout_txt),new ClickSpan.OnClickListener(){
            @Override
            public void onClick() {
                activity.safeShowDialog(Consts.Dialogs.DIALOG_LOGOUT);
            }
        },true);

        ((TextView) findViewById(R.id.share_header_username)).setText(u.username);

        final ImageView icon = (ImageView) findViewById(R.id.icon);
        if (CloudUtils.checkIconShouldLoad(u.avatar_url)) {
            if (ImageLoader.get(activity).bind(icon, u.avatar_url, null) != ImageLoader.BindResult.OK) {
                icon.setImageDrawable(getResources().getDrawable(R.drawable.avatar_badge));
            }
        }
    }

    public ShareUserHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShareUserHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
