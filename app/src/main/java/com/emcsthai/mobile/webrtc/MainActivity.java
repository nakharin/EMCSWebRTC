package com.emcsthai.mobile.webrtc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public final static String KEY_ROOM_ID = "KEY_ROOM_ID";

    private Toolbar toolbar;

    private AppCompatEditText edtRoomId;
    private AppCompatButton btnJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWidgets();

        btnJoin.setOnClickListener(onClickListener);
    }

    private void initWidgets() {

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("EMCSWebRTC");
        setSupportActionBar(toolbar);

        edtRoomId = findViewById(R.id.edtRoomId);
        btnJoin = findViewById(R.id.btnJoin);
    }

    private View.OnClickListener onClickListener = v -> {
        if (v == btnJoin) {

            String roomId = edtRoomId.getText().toString();
            if (!roomId.equals("")) {
                Intent intent = new Intent(getApplicationContext(), VideoCallActivity.class);
                intent.putExtra(KEY_ROOM_ID, roomId);
                startActivity(intent);

            } else {
                Toast.makeText(this, "Please Input Room ID", Toast.LENGTH_SHORT).show();
            }
        }
    };
}