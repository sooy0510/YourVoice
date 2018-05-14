package com.example.ds.yourvoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RelativeLayout;

/**
 * Created by DS on 2018-05-11.
 */

public class CallBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("ACTION.CallReceive")) {
            Log.d("CallBroadcastReceiver", "ACTION.CallReceive");

            Intent cIntent = new Intent(context, CallReceiveActivity.class);
            cIntent.putExtra("callerID", intent.getStringExtra("callerID"));
            cIntent.putExtra("receiverID", intent.getStringExtra("receiverID"));
            cIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(cIntent);
        }

//        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
//
//        }
    }
}
