package com.emcsthai.mobile.webrtc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public final static String KEY_ROOM_ID = "KEY_ROOM_ID";

    private AppCompatEditText edtRoomId;

    private AppCompatButton btnJoin;

    private AppCompatButton btnEnable;
    private AppCompatButton btnDisable;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWidgets();

        btnJoin.setOnClickListener(onClickListener);
        btnEnable.setOnClickListener(onClickListener);
        btnDisable.setOnClickListener(onClickListener);
        edtRoomId.setOnEditorActionListener(onEditorActionListener);
    }

    private void initWidgets() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("EMCSWebRTC");
        setSupportActionBar(toolbar);

        edtRoomId = findViewById(R.id.edtRoomId);

        btnJoin = findViewById(R.id.btnJoin);
        btnEnable = findViewById(R.id.btnEnable);
        btnDisable = findViewById(R.id.btnDisable);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("");
        progressDialog.setMessage("Connecting, Please wait...");
    }

    private void checkJoinRoom() {
        String roomId = edtRoomId.getText().toString();
        if (!roomId.equals("")) {
            progressDialog.show();
            new Handler().postDelayed(() -> {
                progressDialog.dismiss();
                Intent intent = new Intent(getApplicationContext(), VideoCallActivity.class);
                intent.putExtra(KEY_ROOM_ID, roomId);
                startActivity(intent);
            }, 500);

        } else {
            Toast.makeText(this, "Please Input Room ID", Toast.LENGTH_SHORT).show();
        }
    }

    private View.OnClickListener onClickListener = v -> {
        if (v == btnJoin) {
            checkJoinRoom();
        }

        if (v == btnEnable) {
            btnJoin.setEnabled(true);
            btnJoin.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.btn_active_selector));
        }

        if (v == btnDisable) {
            btnJoin.setEnabled(false);
            btnJoin.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.btn_active_disable));
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