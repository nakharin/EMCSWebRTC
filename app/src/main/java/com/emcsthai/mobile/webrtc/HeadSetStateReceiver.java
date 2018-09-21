package com.emcsthai.mobile.webrtc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Objects;

public class HeadSetStateReceiver extends BroadcastReceiver {

    private static final String TAG = "HeadSetStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    Log.d(TAG, "Headset is unplugged");
                    Toast.makeText(context, "Speakerphone is On.", Toast.LENGTH_SHORT).show();
                    Objects.requireNonNull(audioManager).setSpeakerphoneOn(true);
                    break;
                case 1:
                    Log.d(TAG, "Headset is plugged");
                    Toast.makeText(context, "Speakerphone is Off.", Toast.LENGTH_SHORT).show();
                    Objects.requireNonNull(audioManager).setSpeakerphoneOn(false);
                    break;
                default:
                    Log.d(TAG, "I have no idea what the headset state is");
            }
        }
    }
}
