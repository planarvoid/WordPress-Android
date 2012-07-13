package com.soundcloud.android.view.create;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EmailPickerItem extends RelativeLayout {
    private TextView mName, mEmail;
    private ImageView mIcon;

    @SuppressWarnings("UnusedDeclaration")
    public EmailPickerItem(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public EmailPickerItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public EmailPickerItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mName  = (TextView) findViewById(R.id.name);
        mEmail = (TextView) findViewById(R.id.email);
        mIcon  = (ImageView) findViewById(R.id.icon);
    }

    public String getEmail() {
        String email = mEmail.getText().toString();
        return TextUtils.isEmpty(email) ? getName() : email;
    }

    public String getName() {
        return mName.getText().toString();
    }

    public void setName(CharSequence name) {
        mName.setText(name);
    }

    public void setEmail(CharSequence email) {
        mEmail.setText(email);
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getContext().getResources(),
                    R.drawable.ic_contact_list_picture);
        }
        mIcon.setImageBitmap(bitmap);
    }

    public void initializeFromCursor(Cursor c) {
        setName(c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
        setEmail(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)));
        setBitmap(ImageUtils.loadContactPhoto(getContext().getContentResolver(),
                c.getLong(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))));

    }
}
