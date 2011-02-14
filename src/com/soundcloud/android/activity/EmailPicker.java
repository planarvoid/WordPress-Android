package com.soundcloud.android.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import com.soundcloud.android.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class EmailPicker extends Activity {
    public static final int GET_EMAIL   = 0;
    public static final int PICK_EMAILS = 9002;

    public static final String BUNDLE_KEY = "emails";
    public static final String SELECTED = "selected";


    static final Pattern EMAIL_REGEXP = Pattern.compile("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
    private MultiAutoCompleteTextView mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout layout = new FrameLayout(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.email_picker, layout);

        setContentView(layout);

        mEmail = (MultiAutoCompleteTextView) findViewById(R.id.email);
        mEmail.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mEmail.setAdapter(new EmailCursorAdapter(this, null));
        mEmail.setValidator(new AutoCompleteTextView.Validator() {
            @Override
            public boolean isValid(CharSequence text) {
                return EMAIL_REGEXP.matcher(text).matches();
            }

            @Override
            public CharSequence fixText(CharSequence invalidText) {
                return null;
            }
        });

        if (getIntent().hasExtra(BUNDLE_KEY)) {
            String[] emails = getIntent().getExtras().getStringArray(BUNDLE_KEY);
            if (emails != null && emails.length > 0) {
              mEmail.setText(TextUtils.join(", ", emails));
            }

            // put cursor right after selected item
            if (getIntent().hasExtra(SELECTED)) {
                String selected = getIntent().getStringExtra(SELECTED);
                int cursorPos = mEmail.getEditableText().toString()
                        .indexOf(selected) + selected.length();

                if (cursorPos <= mEmail.length()) mEmail.setSelection(cursorPos);
            }
        }

        TextView picker = (TextView) findViewById(R.id.picker);
        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                EmailPicker.this.startActivityForResult(i, GET_EMAIL);
            }
        });

        TextView pick_done = (TextView) findViewById(R.id.pick_done);
        pick_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent();
                Uri uri = new Uri.Builder()
                        .scheme("content")
                        .authority(EmailPicker.this.getClass().getName())
                        .path(BUNDLE_KEY)
                        .build();

                List<String> emails = new ArrayList<String>();
                for (String e : mEmail.getText().toString().split("\\s*,\\s*")) {
                    e = e.trim();
                    if (!TextUtils.isEmpty(e) && EMAIL_REGEXP.matcher(e).matches()) emails.add(e);
                }
                result.putExtra(BUNDLE_KEY, emails.toArray(new String[emails.size()]));
                setResult(RESULT_OK, result.setData(uri));
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_EMAIL) {
            Log.v(TAG, "onActivityResult(" + data + ")");

            String id = data.getData().getLastPathSegment();

            if (id != null) {
                Cursor c = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Email._ID,
                                ContactsContract.CommonDataKinds.Email.DATA},

                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                );
                if (c != null && c.moveToFirst()) {
                    String email = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));

                    if (!TextUtils.isEmpty(email) &&
                        !mEmail.getText().toString().contains(email)) {

                        if (mEmail.length() == 0) {
                            mEmail.setText(email+", ");
                        } else {
                            if (!mEmail.getEditableText().toString().trim().endsWith(","))
                                 mEmail.append(", ");

                            mEmail.append(email+",  ");
                        }

                        mEmail.setSelection(mEmail.length());
                    }
                }
            }
        }
    }

    static class EmailCursorAdapter extends CursorAdapter {
        private ContentResolver mContentResolver;
        private LayoutInflater mInflater;
        private final int resource = android.R.layout.simple_dropdown_item_1line;

        public EmailCursorAdapter(Context context, Cursor c) {
            super(context, c);
            mContentResolver = context.getContentResolver();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        public CharSequence convertToString(Cursor cursor) {
            return cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return createViewFromResource(
                    convertToString(cursor),
                    null, parent, resource);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            createViewFromResource(
                    convertToString(cursor),
                    view,
                    null,
                    resource);
        }



        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            Log.v(TAG, "runQueryOnBackgrounThread(" + constraint + ")");

            if (constraint != null) {
                return mContentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Email._ID,
                                ContactsContract.CommonDataKinds.Email.DATA},
                        "UPPER(" + ContactsContract.CommonDataKinds.Email.DATA + ") GLOB ?",
                        new String[]{constraint.toString().toUpperCase() + "*"},
                        ContactsContract.CommonDataKinds.Email.DATA + " ASC");
            } else {
                return super.runQueryOnBackgroundThread(constraint);
            }

        }

        private View createViewFromResource(CharSequence s, View convertView, ViewGroup parent,
                                            int resource) {
            View view;
            TextView text;

            if (convertView == null) {
                view = mInflater.inflate(resource, parent, false);
            } else {
                view = convertView;
            }

            text = (TextView) view;
            text.setText(s);
            text.setTextColor(view.getResources().getColor(R.color.white));
            return text;
        }
    }
}
