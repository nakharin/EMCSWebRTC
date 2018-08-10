package com.emcsthai.mobile.webrtc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

public class TestConstraintLayout extends AppCompatActivity {

    private ConstraintLayout rootConstraintLayout;

    private ImageView imgScreenType;

    private View localSurfaceView;
    private View remoteSurfaceView;

    private int type = 1;

    private ConstraintSet consSetHalf = new ConstraintSet();
    private ConstraintSet consSetLocal = new ConstraintSet();
    private ConstraintSet consSetRemote = new ConstraintSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_half_constraint_layout);

        initWidgets();

        initConstraintSet();

        imgScreenType.setOnClickListener(onClickListener);
    }

    private void initWidgets() {

        rootConstraintLayout = findViewById(R.id.rootConstraintLayout);

        imgScreenType = findViewById(R.id.imgScreenType);

        localSurfaceView = findViewById(R.id.localSurfaceView);
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
    }

    private void initConstraintSet() {
        consSetHalf.clone(rootConstraintLayout);
        consSetLocal.load(this, R.layout.activity_local_small_constraint_layout);
        consSetRemote.load(this, R.layout.activity_remote_small_constraint_layout);
    }

    private void sendViewToBack(@NonNull View child) {
        final ViewGroup parent = (ViewGroup) child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    private void setHalfScreen() {
        Toast.makeText(TestConstraintLayout.this, "setHalf", Toast.LENGTH_SHORT).show();
        consSetHalf.applyTo(rootConstraintLayout);
    }

    private void setLocalSmall() {
        Toast.makeText(TestConstraintLayout.this, "setLocalSmall", Toast.LENGTH_SHORT).show();
        sendViewToBack(remoteSurfaceView);
        consSetLocal.applyTo(rootConstraintLayout);
    }

    private void setRemoteSmall() {
        Toast.makeText(TestConstraintLayout.this, "setRemoteSmall", Toast.LENGTH_SHORT).show();
        sendViewToBack(localSurfaceView);
        consSetRemote.applyTo(rootConstraintLayout);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (v == imgScreenType) {

//                TransitionManager.beginDelayedTransition(rootConstraintLayout);

                switch (type) {
                    case 0:
                        setHalfScreen();
                        type = 1;
                        break;
                    case 1:
                        setLocalSmall();
                        type = 2;
                        break;
                    case 2:
                        setRemoteSmall();
                        type = 0;
                        break;
                }
            }
        }
    };
}