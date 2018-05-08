package com.example.ds.yourvoice;

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
        user = "sooy1";
        //getUserId();
        //user = ((MainActivity)MainActivity.context).getUserId();
    }

    //    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d("스레드서비스실행", "스레드서비스실행");
//        if(thread == null) {
//            thread = new dbCheck(){
//                public void run(){
//                    thread.start();
//                    if(connectUser == null) {
//                        //dbCheck(user);
//                        Log.d("전화체크", user + " 전화없음");
//                    }
//                    else if(connectUser != null){
//                        Toast.makeText(getApplicationContext(), "전화왓따", Toast.LENGTH_LONG).show();
//                    }
//                }
//            };
//            //thread.start();
//        }
//        return super.onStartCommand(intent, flags, startId);
//    }
//
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("서비스실행", "서비스실행");

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
    }
}
//
//    private void dbCheck(final String Id) {
//
//        class InsertData extends AsyncTask<String, Void, String> {
//            ProgressDialog loading;
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                //loading = ProgressDialog.show(CallService.this, "caling...", null, true, true);
//                //Toast.makeText(getApplicationContext(), "외않되", Toast.LENGTH_SHORT). show();
//            }
//
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                //loading.dismiss();
//
//                if (s.toString().equals("false")) {
//                }
//                else {
//                    connectUser = s.toString();
//                }
//            }
//
//            @Override
//            protected String doInBackground(String... params) {
//
//                try {
//                    String Id = (String) params[0];
//
//                    String link = "http://13.124.94.107/callCheck.php";
//                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
//
//                    URL url = new URL(link);
//                    URLConnection conn = url.openConnection();
//
//                    conn.setDoOutput(true);
//                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//
//                    wr.write(data);
//                    wr.flush();
//
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//
//                    StringBuilder sb = new StringBuilder();
//                    String line = null;
//
//                    // Read Server Response
//                    while ((line = reader.readLine()) != null) {
//                        sb.append(line);
//                        break;
//                    }
//                    return sb.toString();
//                } catch (Exception e) {
//                    return new String("Exception: " + e.getMessage());
//                }
//            }
//        }
//        InsertData task = new InsertData();
//        task.execute(Id);
//    }


//    private class dbCheck extends Thread {
//
//        public dbCheck(){
//            //초기화
//        }
//
//        @Override
//        public void run() {
//            try {
//                String Id = user;
//
//                String link = "http://13.124.94.107/callCheck.php";
//                String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
//
//                URL url = new URL(link);
//                URLConnection conn = url.openConnection();
//
//                conn.setDoOutput(true);
//                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//
//                wr.write(data);
//                wr.flush();
//
//                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//
//                StringBuilder sb = new StringBuilder();
//                String line = null;
//
//                // Read Server Response
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                    result = sb.toString();
//                    Log.d("서비스스레드결과", result);
//                    break;
//                }
//            } catch (Exception e) {
//                 Log.d("서비스스레드 Exception", e.toString());
//            }
//        }
//    }
//}