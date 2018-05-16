package com.example.ds.yourvoice;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import static com.example.ds.yourvoice.CallActivity.context;

/**
 * Created by DS on 2018-05-11.
 */

public class CallReceiveActivity extends AppCompatActivity {

    private static PowerManager.WakeLock sCpuWakeLock;
    public String callerId;
    public String receiverId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_receive);

        //wakeLock();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        callerId = intent.getStringExtra("callerID");
        receiverId = intent.getStringExtra("receiverID");

        TextView tv = (TextView)findViewById(R.id.caller);
        tv.setText(callerId);
    }

    public void wakeLock() {
        //액티비티 깨우기
        if (sCpuWakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "hi");

        sCpuWakeLock.acquire();


        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }

        //출처: http://cofs.tistory.com/173 [CofS]
    }

    public void receive(View v){
        Log.d("전화받기", "receive");
        callingUpdate(receiverId, "1");
        call(callerId, receiverId);
    }

    public void denial(View v) {
        Log.d("전화끊기", "denial");
        Intent cIntent = new Intent(this, CallService.class);
        stopService(cIntent);
        callingUpdate(callerId, "0");
    }

    /* ---------------------------------------------- 전화 받기/끊기 ----------------------------------------------------------- */
    private void callingUpdate(final String Id, String num) {
        class InsertData extends AsyncTask<String, Void, String> {

            protected void onCancelled() {
                this.onCancelled();
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.d("calling Update", "DB 업데이트");
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                if (s.toString().equals("success")) {
                    Log.d("callingUpdate success", "DB 업데이트 성공");
                } else {
                    Log.d("callingUpdate error", "DB 업데이트 에러");
                    Log.d(s.toString(), "DB 업데이트");
                }

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("ACTION.RESTART.CallService");
                broadcastIntent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                broadcastIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                sendBroadcast(broadcastIntent);
                finish();
            }

            @Override
            protected String doInBackground(String... params) {

                if (this.isCancelled()) {
                    // 비동기작업을 cancel해도 자동으로 취소해주지 않으므로,
                    // 작업중에 이런식으로 취소 체크를 해야 한다.
                    return null;
                }

                try {
                    String Id = (String) params[0];
                    String num = (String) params[1];

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
                    String line = null;

                    // Read Server Response
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        break;
                    }
                    return sb.toString();
                } catch (Exception e) {
                    return new String("Exception: " + e.getMessage());
                }
            }
        }
        InsertData task = new InsertData();
        task.execute(Id, num);
    }


    /* ---------------------------------------------- 전화 받기/끊기 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 전화걸기 ----------------------------------------------------------- */

    public void call(String caller, String receiver) {
        Intent intent = new Intent(CallReceiveActivity.this, CallActivity.class);
        intent.putExtra("Caller", caller);
        intent.putExtra("Receiver", receiver);
        startActivityForResult(intent, 0);
//        stopService(cIntent);
    }

    /* ---------------------------------------------- 전화걸기 끝 ----------------------------------------------------------- */

}
