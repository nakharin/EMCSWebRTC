package com.emcsthai.mobile.webrtc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.emcs.nakharin.webrtc.Constants;
import com.emcs.nakharin.webrtc.VideoCallActivity;

public class MainActivity extends AppCompatActivity {

    private ViewGroup transitionsContainer;

    private AppCompatEditText edtRoomId;

    private AppCompatButton btnJoin;

    private TextView textError;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWidgets();

        btnJoin.setOnClickListener(onClickListener);
        edtRoomId.setOnEditorActionListener(onEditorActionListener);
    }

    private void initWidgets() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("EMCSWebRTC");
        setSupportActionBar(toolbar);

        transitionsContainer = findViewById(R.id.transitionsContainer);

        edtRoomId = findViewById(R.id.edtRoomId);

        btnJoin = findViewById(R.id.btnJoin);

        textError = findViewById(R.id.textError);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("");
        progressDialog.setMessage("Connecting, Please wait...");
    }

    private void checkJoinRoom() {

        TransitionManager.beginDelayedTransition(transitionsContainer);

        String roomId = edtRoomId.getText().toString();
        if (!roomId.equals("")) {
            textError.setVisibility(View.GONE);
            progressDialog.show();
            new Handler().postDelayed(() -> {
                progressDialog.dismiss();
                Intent intent = new Intent(getApplicationContext(), VideoCallActivity.class);
                intent.putExtra(Constants.KEY_ROOM_ID, roomId);
                startActivity(intent);
            }, 500);

        } else {
            textError.setVisibility(View.VISIBLE);
        }
    }

    private View.OnClickListener onClickListener = v -> {
        if (v == btnJoin) {
            checkJoinRoom();
        }
    };

    private TextView.OnEditorActionListener onEditorActionListener = (v, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            checkJoinRoom();
            return true;
        }
        return false;
    };
}