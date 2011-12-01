package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.view.CreateController;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;

public class ScCreate extends ScActivity implements CreateController.CreateListener {

    public static final int REQUEST_UPLOAD_FILE = 1;
    private CreateController mCreateController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sc_create);

        mCreateController = new CreateController(this,
                (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content),getIntent().getData());
        mCreateController.setListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCreateController.onResume();
        trackPage(Consts.Tracking.RECORD);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCreateController.onPause();
    }

    @Override
    protected void onStart(){
        super.onStart();
        mCreateController.onStart();
    }

    @Override
    protected void onStop() {
        mCreateController.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCreateController.onDestroy();
    }


    @Override
    public void onCreateServiceBound() {
        super.onCreateServiceBound();
        mCreateController.onCreateServiceBound(mCreateService);
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        mCreateController.onSaveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        mCreateController.onRestoreInstanceState(state);
        super.onRestoreInstanceState(state);
    }



    @Override
    protected Dialog onCreateDialog(int which) {
        Dialog created = null;
        if (mCreateController != null) {
            created = mCreateController.onCreateDialog(which);
        }
        return created == null ? super.onCreateDialog(which) : created;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.UPLOAD_FILE:
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("audio/*"), REQUEST_UPLOAD_FILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;
            case REQUEST_UPLOAD_FILE:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    final Intent intent = (new Intent(Actions.SHARE))
                            .putExtra(Intent.EXTRA_STREAM, uri);

                    final String file = uri.getLastPathSegment();
                    if (file != null && file.lastIndexOf(".") != -1) {
                        intent.putExtra(Actions.EXTRA_TITLE,
                                file.substring(0, file.lastIndexOf(".")));
                    }
                    startActivity(intent);
                }
        }
    }

    @Override
    public void onSave(Uri recordingUri, final Recording recording, boolean newRecording) {
        if (newRecording){
            startActivity(new Intent(this, ScUpload.class).setData(recordingUri));
            mCreateController.reset();
        } else {
            startActivityForResult(new Intent(this, ScUpload.class).setData(recordingUri), 0);
        }
    }

    @Override
    public void onCancel() {
        mCreateController.reset();
    }

    @Override
    public void onDelete() {
        finish();
    }
}
