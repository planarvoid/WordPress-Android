package com.soundcloud.android.creators.upload;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.ScTextUtils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ShareUserHeaderLayout extends RelativeLayout {

    public ShareUserHeaderLayout(final Activity activity, PublicApiUser user, ImageOperations imageOperations) {
        super(activity);
        View.inflate(getContext(), R.layout.share_user_header, this);

        ScTextUtils.clickify((TextView) findViewById(R.id.share_header_logout_txt), null,
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (!activity.isFinishing()) {
                            showLogoutDialog(activity);
                        }
                    }
                }, true, false);

        ((TextView) findViewById(R.id.share_header_username)).setText(user.username);

        if (user.shouldLoadIcon()) {
            final ImageView icon = (ImageView) findViewById(R.id.icon);
            imageOperations.displayWithPlaceholder(user.getUrn(), ApiImageSize.LARGE, icon);
        }
    }

    private void showLogoutDialog(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.menu_clear_user_title)
                .setMessage(R.string.menu_clear_user_desc)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutActivity.start(activity);
                    }
                }).show();
    }

    /** @noinspection UnusedDeclaration*/
    public ShareUserHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** @noinspection UnusedDeclaration*/
    public ShareUserHeaderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
