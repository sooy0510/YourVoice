package com.example.ds.yourvoice;

/**
 * Created by DS on 2018-05-01.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class RestartService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("서비스재시작", "서비스재시작브로드캐스트받음");


        /* 서비스 죽일때 알람으로 다시 서비스 등록 */
        if (intent.getAction().equals("ACTION.RESTART.CallService")) {

            Log.d("서비스재시작", "서비스");

            Intent i = new Intent(context, CallService.class);
            context.startService(i);
        }

        /* 폰 재부팅할때 서비스 등록 */
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            Log.d("서비스재시작", "ACTION_BOOT_COMPLETED");

            Intent i = new Intent(context, CallService.class);
            context.startService(i);
        }

        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            Log.d("서비스재시작", "ACTION_SCREEN_ON");

            Intent i = new Intent(context, CallService.class);
            context.startService(i);
        }
    }
}
