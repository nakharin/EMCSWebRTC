package com.emcsthai.mobile.webrtc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class TestConstraintLayout extends AppCompatActivity {

    private ConstraintLayout rootLayout;

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
        setContentView(R.layout.activity_remote_small_constraint_layout);

        initWidgets();

        initConstraintSet();

        imgScreenType.setOnClickListener(onClickListener);
    }

    private void initWidgets() {

        rootLayout = findViewById(R.id.rootLayout);

        imgScreenType = findViewById(R.id.imgScreenType);

        localSurfaceView = findViewById(R.id.localSurfaceView);
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
    }

    private void initConstraintSet() {
        consSetRemote.clone(rootLayout);
        consSetLocal.load(this, R.layout.activity_local_small_constraint_layout);
        consSetHalf.load(this, R.layout.activity_half_constraint_layout);
    }

    private void sendViewToBack(@NonNull View child) {
        final ViewGroup parent = (ViewGroup) child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (v == imgScreenType) {

                /**
                 * Enable DelayedTransition
                 */
//                TransitionManager.beginDelayedTransition(rootConstraintLayout);

                switch (type) {
                    case 0:
                        consSetHalf.applyTo(rootLayout);
                        type = 1;
                        break;
                    case 1:
                        sendViewToBack(remoteSurfaceView);
                        consSetLocal.applyTo(rootLayout);
                        type = 2;
                        break;
                    case 2:
                        sendViewToBack(localSurfaceView);
                        consSetRemote.applyTo(rootLayout);
                        type = 0;
                        break;
                }
            }
        }
    };
}