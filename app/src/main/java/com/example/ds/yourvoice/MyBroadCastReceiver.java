package com.example.ds.yourvoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by DS on 2018-04-02.
 */

public class MyBroadCastReceiver extends BroadcastReceiver{

    private String receiveUser;
    private String id;

    @Override
    public void onReceive(Context context, Intent intent) {
        receiveUser = ((CallActivity)CallActivity.context).getConnectUser();
        id = ((MainActivity)MainActivity.context).getUserId();

        if(receiveUser.equals(id)) {
            Toast.makeText(context, "전화왔따", Toast.LENGTH_LONG).show();
        }
    }
}
