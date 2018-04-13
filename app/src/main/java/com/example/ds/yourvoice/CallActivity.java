package com.example.ds.yourvoice;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ds.yourvoice.utils.AudioWriterPCM;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Endpoint.ChatMessage;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import static com.vidyo.VidyoClient.Connector.Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default;

public class CallActivity extends AppCompatActivity
        implements Connector.IConnect, Connector.IRegisterParticipantEventListener, Connector.IRegisterMessageEventListener {

    private Intent intent;

    public static Context context;

    private Connector vc;
    private String token;
    private String displayName, resourceId;
    private String connectUser;
    private String user;
    private FrameLayout videoFrame;
    private LinearLayout chatFrame;
    private LinearLayout sendEdit;
    private boolean mVidyoClientInitialized = false;
    private boolean tryCall;

    enum CallStatus {
        Default,
        Caller,
        Receiver
    }

    public CallStatus callStatus = CallStatus.Default;


    //clova
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CLIENT_ID = "PGBXBwUedxYBZ2tHbjB6";

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    //private TextView txtResult;
    //private Button btnStart;
    private String mResult;

    private AudioWriterPCM writer;

    //chat
    ListView m_ListView;
    MessageAdapter m_Adapter;


    enum VidyoConnectorState {
        VidyoConnectorStateConnected,
        VidyoConnectorStateDisconnected,
        VidyoConnectorStateDisconnectedUnexpected,
        VidyoConnectorStateFailure
    }

    private VidyoConnectorState mVidyoConnectorState = VidyoConnectorState.VidyoConnectorStateDisconnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call);
        Log.d("콜액티비티실행", "실행");

        context = this;

        ConnectorPkg.setApplicationUIContext(this);
        mVidyoClientInitialized = ConnectorPkg.initialize();
        videoFrame = (FrameLayout)findViewById(R.id.videoFrame);


        intent = getIntent();
        //clova
        //txtResult = (TextView) findViewById(R.id.txt_result);
        //btnStart = (Button) findViewById(R.id.btn_start);

        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

        //chat
        chatFrame = (LinearLayout) findViewById(R.id.im1);
        sendEdit = (LinearLayout) findViewById(R.id.send_edit);

        // 커스텀 어댑터 생성
        m_Adapter = new MessageAdapter();

        // Xml에서 추가한 ListView 연결
        m_ListView = (ListView) findViewById(R.id.listView1);

        // ListView에 어댑터 연결
        m_ListView.setAdapter(m_Adapter);

        if(intent.getStringExtra("Caller") != null && intent.getStringExtra("Receiver") != null) {
            user = intent.getStringExtra("Caller");
            connectUser = intent.getStringExtra("Receiver");
            //Connect 함수 매개변수값 오또카지;;
            callStatus = CallStatus.Receiver;
            //Connect();
            Log.d("콜액티비티실행", "수신자");
        } else {
            callStatus = CallStatus.Caller;
            //Connect();
            Log.d("콜액티비티실행", "발신자");
        }

/*        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    // Start button is pushed when SpeechRecognizer's state is inactive.
                    // Run SpeechRecongizer by calling recognize().
                    mResult = "";
                    *//*if(txtResult.getText().toString().equals("")){
                        txtResult.setText("Connecting...");
                    }else{
                        txtResult.append("\n Connecting...");
                    }*//*
                    if(chatFrame.getVisibility() == View.VISIBLE){
                        m_Adapter.add("Connecting...",2);
                    }
                    //btnStart.setText(R.string.str_stop);
                    naverRecognizer.recognize();
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    btnStart.setEnabled(false);

                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });*/

       /* m_Adapter.add("이건 뭐지",1);
        m_Adapter.add("쿨쿨",1);
        m_Adapter.add("쿨쿨쿨쿨",0);
        m_Adapter.add("재미있게",1);
        m_Adapter.add("놀자라구나힐힐 감사합니다. 동해물과 백두산이 마르고 닳도록 놀자 놀자 우리 놀자",1);
        m_Adapter.add("재미있게",1);
        m_Adapter.add("재미있게",0);
        m_Adapter.add("2015/11/20",2);
        m_Adapter.add("재미있게",1);
        m_Adapter.add("재미있게",1);*/

        findViewById(R.id.button1).setOnClickListener(new Button.OnClickListener()
              {
                  @Override
                  public void onClick(View v) {
                      //Toast.makeText(getApplicationContext(), "외않되", Toast.LENGTH_SHORT). show();
                      EditText editText = (EditText) findViewById(R.id.editText1) ;
                      String inputValue = editText.getText().toString() ;
                      editText.setText("");
                      refresh(inputValue,0);
                  }
              }
        );
    }


    private void refresh(String inputValue, int _str) {
        m_Adapter.add(inputValue,_str) ;
        m_Adapter.notifyDataSetChanged();
    }



    /* ---------------------------------------------- CLOVA ----------------------------------------------------------- */
    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.

                //txtResult.append("\n Connected");
                m_Adapter.add("Connected",2);
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                //txtResult.setText(mResult);
                //txtResult.append(mResult);
                //m_Adapter.add("mResult",1);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();
                /*for(String result : results) {
                    strBuf.append(result);
                    strBuf.append("\n");
                }*/
                //첫번째 항목 출력
                strBuf.append(results.get(0));

                //strBuf.append("\n");
                mResult = strBuf.toString();
                addUserChat(mResult);
                m_Adapter.add(mResult,1);
                m_Adapter.notifyDataSetChanged();
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                addUserChat(mResult);
                //txtResult.setText(mResult);
                m_Adapter.add("error code:"+mResult,2);
                //btnStart.setText(R.string.str_start);
                //btnStart.setEnabled(true);
                break;

            case R.id.clientInactive:
                //음성인식 다시 시작
                //btnStart.setText(R.string.str_stop);
                naverRecognizer.recognize();
                if (writer != null) {
                    writer.close();
                }

                //btnStart.setText(R.string.str_start);
               // btnStart.setEnabled(true);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();

    }

    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        //txtResult.setText("");
        m_Adapter.add(mResult,2);
        //btnStart.setText(R.string.str_start);
        //btnStart.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<CallActivity> mActivity;

        RecognitionHandler(CallActivity activity) {
            mActivity = new WeakReference<CallActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CallActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    /* ---------------------------------------------- CLOVA 끝 ----------------------------------------------------------- */
    public void addUserChat(String chat){
        //String userChat = chat;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("chats");


        Hashtable<String,String> chatText = new Hashtable<String,String>();
        user = intent.getStringExtra("userId");
        chatText.put("user",user);
        chatText.put("friend",connectUser);
        myRef.setValue(chatText);

        /*addPhone = (EditText)findViewById(R.id.addFriendPhone);
        String friendPhone = addPhone.getText().toString();

        //LoginActivity loginActivity = (LoginActivity) getApplication();

        String userId = intent.getStringExtra("userId");
        //Log.e("aaaaaaaaaaaaaaaaaaaa",userId);

        insertToDatabase(userId, friendPhone);*/

        //friendPhone.setText("");
        //friendPhone.requestFocus();
    }



















    /* ---------------------------------------------- VIDYO ----------------------------------------------------------- */
    public void Connect(View v) {

        Log.d("connecttt", "연결");
        token = "cHJvdmlzaW9uAGluc2VvbkA0OTNmN2UudmlkeW8uaW8AMTYzNjg5NjA5MDM5AAAwMjA1NGY0YTIxOTRkYzQ1NTc1ZGM5NGVmZThjMTI3MGI1Yjk2Y2FmN2ZmYzAxYjJiOTZiYTY1ZGJiYjYwYmI2MTBmNGQ1MTcyNWI1NTQxMzY2NWNkZTFhNGViYzY1NWU=";

//        user = intent.getStringExtra("userId");
//        user = ((MainActivity)MainActivity.context).getUserId();
//        connectUser = v.getTag().toString();s
//        displayName = user + "-" + connectUser;

        // Log.d("전화를", conn);

        if (callStatus.name().equals("Receiver")) {
            displayName = user + "-" + connectUser;
            Log.d("전화수신", user + "->" + connectUser);

            vc = new Connector(videoFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 2, "warning info@VidyoClient info@VidyoConnector", "", 0);

            vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());

            vc.selectDefaultCamera();
            vc.selectDefaultMicrophone();
            vc.selectDefaultSpeaker();
            vc.selectDefaultNetworkInterfaceForSignaling();
            vc.selectDefaultNetworkInterfaceForMedia();

            vc.connect("prod.vidyo.io", token, "call", displayName, this);
        } else {
            callStatus = CallStatus.Caller;
            user = ((MainActivity) MainActivity.context).getUserId();
            connectUser = intent.getStringExtra("Tag");
            displayName = user + "-" + connectUser;
            Log.d("전화발신", user + "->" + connectUser);

            //startCall(connectUser, user);
            if (true) {
                Log.d("connecttt", "함수트루");

                callStatus = CallStatus.Caller;

                vc = new Connector(videoFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 2, "warning info@VidyoClient info@VidyoConnector", "", 0);

                //채팅창 보이도록
                if(chatFrame.getVisibility() == View.GONE){
                    chatFrame.setVisibility(View.VISIBLE);
                }else{
                    chatFrame.setVisibility(View.GONE);
                }

                if(sendEdit.getVisibility() == View.GONE){
                    sendEdit.setVisibility(View.VISIBLE);
                }else{
                    sendEdit.setVisibility(View.GONE);
                }

                //clova 음성인식 시작
                if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    // Start button is pushed when SpeechRecognizer's state is inactive.
                    // Run SpeechRecongizer by calling recognize().
                    mResult = "";
                    if(chatFrame.getVisibility() == View.VISIBLE){
                        m_Adapter.add("Connecting...",2);
                    }
                    naverRecognizer.recognize();
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    //btnStart.setEnabled(false);
                    naverRecognizer.getSpeechRecognizer().stop();
                }
                vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());

                vc.selectDefaultCamera();
                vc.selectDefaultMicrophone();
                vc.selectDefaultSpeaker();
                vc.selectDefaultNetworkInterfaceForSignaling();
                vc.selectDefaultNetworkInterfaceForMedia();

                //mVidyoConnector.Connect(host, token, displayName, resourceId, this);
                vc.connect("prod.vidyo.io", token, "call", displayName, this);
                //sendBroadcast(new Intent("action_call"));
            }
        }
    }

//
//        if(mVidyoClientInitialized) {Log.d("connecttt", "초기화");
//
//            startCall(connectUser, user);
//            if(tryCall) { Log.d("connecttt", "함수트루");
//
//                callStatus = CallStatus.Caller;
//
//                vc = new Connector(videoFrame, Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, 2, "warning info@VidyoClient info@VidyoConnector", "", 0);
//
//                vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
//
//                vc.selectDefaultCamera();
//                vc.selectDefaultMicrophone();
//                vc.selectDefaultSpeaker();
//                vc.selectDefaultNetworkInterfaceForSignaling();
//                vc.selectDefaultNetworkInterfaceForMedia();
//
//                //mVidyoConnector.Connect(host, token, displayName, resourceId, this);
//                vc.connect("prod.vidyo.io", token, "call", displayName, this);
//
//                //sendBroadcast(new Intent("action_call"));
//            }
//        } else {
//            Log.d("Initialize failed", "not constructing VidyoConnector");
//        }


    public void Disconnect(View v) {
        vc.disconnect();
        finish();
        //clova
        callStatus = CallStatus.Default;
        Log.d("disconnect", "연결종료");
        naverRecognizer.getSpeechRecognizer().stop();
        Log.d(TAG, "clova finish");


    }

    public void onSuccess() {
        Log.d("onSuccess", "successfully connected.");
        //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateConnected, "Connected");



    }

    public void onFailure(Connector.ConnectorFailReason reason) {
        Log.d("onFailure", ": connection attempt failed, reason = " + reason.toString());

        // Update UI to reflect connection failed
        //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateFailure, "Connection failed");
    }

    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {
        if (reason == Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            // Release device resources
            vc.disable();
            vc = null;

            // Uninitialize the VidyoClient library
            ConnectorPkg.uninitialize();

            Log.d("onDisconnected", "successfully disconnected, reason = " + reason.toString());
            //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateDisconnected, "Disconnected");
        } else {
            Log.d("onDisconnected", "unexpected disconnection, reason = " + reason.toString());
            //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateDisconnectedUnexpected, "Unexpected disconnection");
        }
    }

    // Register for VidyoConnector event listeners. Note: this is an arbitrary function name.
    public void RegisterForVidyoEvents() {
        // Register for Participant callbacks
        vc.registerParticipantEventListener(this);

        // Register to receive chat messages
        vc.registerMessageEventListener(this);

       /* Register for local window share and local monitor events */
//        vc.registerLocalWindowShareEventListener(this);
//        vc.registerLocalMonitorEventListener(this);
    }
    // Participant Joined
    public void onParticipantJoined(Participant participant) {
        Log.d("ParticipainJoined", "ture");
    }
    // Participant Left
    public void onParticipantLeft(Participant participant) {}
    // Ordered array of participants according to rank
    public void onDynamicParticipantChanged(ArrayList participants, ArrayList cameras) {}
    // Current loudest speaker
    public void onLoudestParticipantChanged(Participant participant, boolean audioOnly) {}

    // Register for VidyoConnector event listeners. Note: this is an arbitrary function name.
//    public void RegisterForVidyoEvents() {
//        // Register to receive chat messages
//        vc.registerMessageEventListener(this);
//    }

    private void SendChatMessage(String message) {
        vc.sendChatMessage(message);
    }
    // Message received from other participants
    public void onChatMessageReceived(Participant participant, ChatMessage chatMessage) {}

//    /* Register for VidyoConnector event listeners. Note: this is an arbitrary function name. */
//    public void RegisterForVidyoEvents() {
//        /* Register for local window share and local monitor events */
//        vc.registerLocalWindowShareEventListener(this);
//        vc.registerLocalMonitorEventListener(this);
//    }

    @Override
    protected void onDestroy() {
//        // Release device resources
//        vc.disable();
//        vc = null;
//
//        // Uninitialize the VidyoClient library
//        ConnectorPkg.uninitialize();

        Log.d("Destroy", "true");
        super.onDestroy();
    }

    private void startCall(final String connectId, String Id) {

        class InsertData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;

            protected void onCancelled() {
                super.onCancelled();
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(CallActivity.this, "caling...", null, true, true);
                //Toast.makeText(getApplicationContext(), "외않되", Toast.LENGTH_SHORT). show();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

                if (s.toString().equals("Try")) {
                    Log.d("connecttt", "트라이");
                    tryCall = true;
                } else
                    tryCall = false;

            }

            @Override
            protected String doInBackground(String... params) {

                if (this.isCancelled()) {
                    // 비동기작업을 cancel해도 자동으로 취소해주지 않으므로,
                    // 작업중에 이런식으로 취소 체크를 해야 한다.
                    return null;
                }

                try {
                    String connectId = (String) params[0];
                    String Id = (String) params[1];
                    Log.d("connecttt", connectId + Id);

                    String link = "http://13.124.94.107/callStateCheck.php";
                    String data = URLEncoder.encode("connectId", "UTF-8") + "=" + URLEncoder.encode(connectId, "UTF-8");
                    data += "&" + URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");

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
        task.execute(connectId, Id);
    }

    public String getConnectUser(){
        return connectUser;
    }
}
