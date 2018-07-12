package com.example.ds.yourvoice;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by DS on 2018-04-06.
 */

public class CallService extends Service {

    // 서비스 종료시 재부팅 딜레이 시간, activity의 활성 시간을 벌어야 한다.
    private static final int REBOOT_DELAY_TIMER = 2 * 1000;

    private static String user;
    private String connectUser = null;

    private Thread thread;
    private Thread callerThread;
    //private boolean call = false;

    private String result;

    @Override
    public IBinder onBind(Intent intent) {
// Wont be called as service is not bound

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 등록된 알람은 제거
        Log.d("PersistentService", "onCreate()");
        unregisterRestartAlarm();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // 서비스가 죽었을때 알람 등록
        Log.d("서비스종료", "onDestroy()");
        //stopSelf();
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
    public int onStartCommand(final Intent intent, int flags, final int startId) {

        startForeground(1, new Notification());

        Log.d("서비스실행", "" + startId);

        //connectUser = null;

        if(thread==null || !thread.isAlive()) {
            Log.d("서비스", "새로운스레드");
            thread = new Thread("Service Thread") {

                @Override
                public void run() {

                    if (user == null) {
                        //stopSelf();
                        user = intent.getStringExtra("user");
                        Log.d("서비스 유저아이디", user);
                    }

                    if (connectUser != null)
                        connectUser = null;

                    while (connectUser == null && user != null) {

                        try {
                            thread.sleep(1000 * 2);
                        } catch (Exception e) {
                            Log.d("서비스스레드 Exception", e.toString());
                        }

                        try {
                            Log.d("전화", "서비스스레드 While");
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
                                thread.sleep(1 * 1000);
                            } else if (result.equals("caller")) {
                                Log.d("전화", "caller");
                                callerThread = new callerCheck();
                                callerThread.start();
                                Thread.currentThread().interrupt();
                                //stopSelf();
                                break;
                            } else {
                                connectUser = result;

                                Intent broadcastIntent = new Intent();
                                broadcastIntent.setAction("ACTION.CallReceive");
                                broadcastIntent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING); //중복된 브로드캐스트를 하나로
                                broadcastIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES); //프로세스가 존재하지 않아도 리시버에게 전달
                                broadcastIntent.putExtra("callerID", connectUser);
                                broadcastIntent.putExtra("receiverID", user);
                                sendBroadcast(broadcastIntent);

                                Log.d("서비스" + connectUser, user);

                                //((CallActivity)CallActivity.context).setConnectUser(connectUser);
                                //((CallActivity)CallActivity.context).Connect();
                                Log.d("전화왔다", result);
                                callerThread = new callerCheck();
                                callerThread.start();
                                Thread.currentThread().interrupt();
                                //stopSelf();
                                break;
                            }
                        } catch (Exception e) {
                            Log.d("서비스스레드 Exception", e.toString());
                        }
                    }
                }
            };
            thread.start();
        } else {
            thread.interrupt();
            Log.d("서비스", "스레드 이미 있음");
        }
        //return super.onStartCommand(intent, flags, 1);
        return START_STICKY;

    }

    private class callerCheck extends Thread {

        private boolean denial = false;

        @Override
        public void run() {
            //super.run();
            Log.d("전화", "타이머태스크");
            long now = System.currentTimeMillis();

            //60초동안 수신을 기다림
            while (System.currentTimeMillis() - now < 1000 * 60) {
                try {
                    String Id;
                    String num = "2";

                    if(connectUser!=null)
                        Id = connectUser;
                    else
                        Id = user;

                    String link = "http://13.124.94.107/callingUpdate.php";
                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
                    data += "&" + URLEncoder.encode("num", "UTF-8") + "=" + URLEncoder.encode(num, "UTF-8");

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
                        //Log.d("서비스스레드결과", result);
                        break;
                    }

                    if (result.equals("wait")) {
                        Log.d("전화", "전화수신대기중..");
                        thread.sleep(1 * 1000);
                    } else if (result.equals("denial")) {
                        Log.d("전화", "전화수신거부");
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("CALL_DENIAL");
                        sendBroadcast(broadcastIntent);
                        connectUser = null;
                        denial = true;
                        //this.interrupt();
                        break;
                    }
                } catch (Exception e) {
                    Log.d("발신자전화서비스스레드 Exception", e.toString());
                    registerRestartAlarm();
                    break;
                }
            }
            if(!denial) {
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("CALL_DENIAL");
                sendBroadcast(broadcastIntent);
                connectUser = null;
                //this.interrupt();
            }
        }
    }


    //서비스가 실행중인지 확인
    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.example.ds.yourvoice".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}