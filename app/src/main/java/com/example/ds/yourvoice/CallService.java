package com.example.ds.yourvoice;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Created by DS on 2018-04-06.
 */

public class CallService extends Service {

    // 서비스 종료시 재부팅 딜레이 시간, activity의 활성 시간을 벌어야 한다.
    private static final int REBOOT_DELAY_TIMER = 1 * 1000;

    private String user = null;
    private String connectUser = null;

    private Thread thread = null;
    //private boolean call = false;

    private String result = null;

    //default = 0;
    //caller = 1;
    //receiver = 2;
    private String callStatus = "Default";

    @Override
    public IBinder onBind(Intent intent) {
// Wont be called as service is not bound

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //user = MainActivity.userId;
        user = "inseon";
        //getUserId();

        // 등록된 알람은 제거
        Log.d("PersistentService", "onCreate()");
        unregisterRestartAlarm();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 서비스가 죽었을때 알람 등록
        Log.d("PersistentService", "onDestroy()");
        registerRestartAlarm();
    }


    private void registerRestartAlarm() {

        Log.d("PersistentService", "registerRestartAlarm()");

        Intent intent = new Intent(CallService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.CallService");
        PendingIntent sender = PendingIntent.getBroadcast(CallService.this, 0, intent, 0);

        long firstTime = SystemClock.elapsedRealtime();
        firstTime += REBOOT_DELAY_TIMER; // 1초 후에 알람이벤트 발생

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, REBOOT_DELAY_TIMER, sender);
    }


    /**
     * 기존 등록되어있는 알람을 해제한다.
     */
    private void unregisterRestartAlarm() {

        Log.d("PersistentService", "unregisterRestartAlarm()");
        Intent intent = new Intent(CallService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.CallService");
        PendingIntent sender = PendingIntent.getBroadcast(CallService.this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("서비스실행", "서비스실행");
        startForeground(1, new Notification());

        thread = new Thread("Service Thread") {
            @Override
            public void run() {

                while (connectUser == null && callStatus.equals("Default")) {
                    //callStatus = mCallback.getCallStatus();
                    Log.d("상태", callStatus);
                    try {
                        String Id = user;

                        String link = "http://13.124.94.107/callCheck.php";
                        String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");

                        URL url = new URL(link);
                        URLConnection conn = url.openConnection();

                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                        wr.write(data);
                        wr.flush();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        StringBuilder sb = new StringBuilder();
                        String line;

                        // Read Server Response
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                            result = sb.toString();
                            Log.d("서비스스레드결과", result);
                            break;
                        }

                        if (result.equals("false")) {
                            Log.d("전화체크", user + " 전화없음");
                        } else {
                            connectUser = result;
                            callStatus = "Receiver";

                            Intent broadcastIntent = new Intent();
                            broadcastIntent.setAction(MainActivity.CALL_RECEIVER);
                            broadcastIntent.putExtra("callerID", connectUser);
                            broadcastIntent.putExtra("receiverID", user);
                            sendBroadcast(broadcastIntent);

                            Log.d("서비스" + connectUser, user);

                            //((CallActivity)CallActivity.context).setConnectUser(connectUser);
                            //((CallActivity)CallActivity.context).Connect();
                            Log.d("전화왔다", result);
                            break;
                        }
                    } catch (Exception e) {
                        Log.d("서비스스레드 Exception", e.toString());
                    }
                }
            }
        };
        thread.start();
        return super.onStartCommand(intent, flags, startId);
       // return START_STICKY;
    }
}

