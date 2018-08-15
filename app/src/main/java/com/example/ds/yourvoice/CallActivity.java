package com.example.ds.yourvoice;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ds.yourvoice.utils.AudioWriterPCM;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Device.RemoteCamera;
import com.vidyo.VidyoClient.Endpoint.ChatMessage;
import com.vidyo.VidyoClient.Endpoint.Participant;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Comment;

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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import static com.vidyo.VidyoClient.Connector.Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default;
import static com.vidyo.VidyoClient.Connector.Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Tiles;

public class CallActivity extends AppCompatActivity
        implements Connector.IConnect, Connector.IRegisterParticipantEventListener, Connector.IRegisterMessageEventListener, Connector.IRegisterLocalCameraEventListener,
        Connector.IRegisterRemoteCameraEventListener {

    private Intent intent;
    private IntentFilter mIntentFilter;
    public static Context context;

    //키보드
    SoftKeyboard softKeyboard;
    RelativeLayout calllayout;
    private InputMethodManager imm;

    private Connector vc;
    private String token;
    private String displayName, resourceId;
    private String connectUser;
    private String user;
    private EditText sendText;
    private LinearLayout videoFrame;
    private LinearLayout localFrame;
    private LinearLayout chatFrame;
    //private LinearLayout sendEdit;
    private boolean mVidyoClientInitialized = false;
    private String userId;
    private String friendId;
    private int chatnum; //채팅방 번호
    private int chatCnt; //해당 친구와 몇번째 채팅인지
    private String chatCntStr; //채팅방 디렉토리 이름
    private String cflag = "N";

    //firebase 데이터 가져오기
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    enum CallStatus {
        Default,
        Caller,
        Receiver
    }

    public CallStatus callStatus = CallStatus.Default;


    //clova
    private static final String TAG = MainActivity.class.getSimpleName();
    private static String CLIENT_ID;

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    //private TextView txtResult;
    //private Button btnStart;
    private String mResult;

    private AudioWriterPCM writer;

    //chat
    ListView m_ListView;
    MessageAdapter m_Adapter;

    //firebase
    FirebaseDatabase database;
    private List<Chat> mChat;


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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d("콜액티비티실행", "실행");

        context = this;

        //수신자가 전화거부하면 브로드캐스트 받음
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("CALL_DENIAL");
        registerReceiver(mReceiver, mIntentFilter);

        ConnectorPkg.setApplicationUIContext(this);
        mVidyoClientInitialized = ConnectorPkg.initialize();
        localFrame = findViewById(R.id.localFrame);
        videoFrame = findViewById(R.id.videoFrame);

        ImageButton b = findViewById(R.id.sendButton);
        b.setEnabled(false);

        //firebase
        database = FirebaseDatabase.getInstance();
        mChat = new ArrayList<>();

        intent = getIntent();

        //userid, friendid 받기
        userId = intent.getStringExtra("userId");
        friendId = intent.getStringExtra("friendId");

        handler = new RecognitionHandler(this);
        //naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

        //chat
        chatFrame = (LinearLayout) findViewById(R.id.im1);
        //sendEdit = (LinearLayout) findViewById(R.id.send_edit);

        // 커스텀 어댑터 생성
        m_Adapter = new MessageAdapter();

        // Xml에서 추가한 ListView 연결
        m_ListView = (ListView) findViewById(R.id.listView1);

        // ListView에 어댑터 연결
        m_ListView.setAdapter(m_Adapter);

        //키보드
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        //영상통화 연결되기 전까지는 sendtext 막기
        sendText = (EditText)findViewById(R.id.sendText);
        sendText.setClickable(false);
        sendText.setFocusable(false);
        //imm.showSoftInput((View) sendText.getWindowToken(),0);
        //sendText.setFocusable(false);

        if (intent.getStringExtra("Caller") != null && intent.getStringExtra("Receiver") != null) {
            CLIENT_ID = "Us8JNMyTCu8dGWq1HCqh";
            naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
            user = intent.getStringExtra("Caller");
            connectUser = intent.getStringExtra("Receiver");
            //Connect 함수 매개변수값 오또카지;;
            callStatus = CallStatus.Receiver;
            //Connect();

            new AsyncTask<Void, Void, Void>(){
                @Override
                protected void onPostExecute(Void result) {
                    Connect();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    return null;
                }
            }.execute();

            Log.d("콜액티비티실행", "수신자");
        } else {
            CLIENT_ID = "PGBXBwUedxYBZ2tHbjB6";
            naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
            callStatus = CallStatus.Caller;

            //startCall(friendId, userId);
            Thread startCall = new startCall();
            startCall.start();
            //Connect();
            Log.d("콜액티비티실행", "발신자");
        }

        calllayout = (RelativeLayout)findViewById(R.id.activity_main);

        InputMethodManager controlManager = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
        softKeyboard = new SoftKeyboard(calllayout, controlManager);



        softKeyboard.setSoftKeyboardCallback(new SoftKeyboard.SoftKeyboardChanged() {
            @Override
            public void onSoftKeyboardHide() {
                new Handler(Looper.getMainLooper())
                        .post(new Runnable() {
                            @Override
                            public void run() {
                                // 키보드 내려왔을때s
                                LinearLayout.LayoutParams plControl = (LinearLayout.LayoutParams)chatFrame.getLayoutParams();
                                plControl.topMargin = 600;
                                plControl.height = 600;
                                chatFrame.setLayoutParams(plControl);
                            }
                        });
            }

            @Override
            public void onSoftKeyboardShow() {
                new Handler(Looper.getMainLooper())
                        .post(new Runnable() {
                            @Override
                            public void run() {
                                // 키보드 올라왔을때
                                Log.d("connecttt","키보드 올라옴");
                                LinearLayout.LayoutParams plControl = (LinearLayout.LayoutParams)chatFrame.getLayoutParams();
                                plControl.topMargin = 470;
                                plControl.height = 535;
                                chatFrame.setLayoutParams(plControl);

                            }
                        });
            }
        });

       /* @Override
        public void onDestroy() {
            super.onDestroy();
            softKeyboard.unRegisterSoftKeyboardCallback();
        }*/

        findViewById(R.id.sendButton).setOnClickListener(new Button.OnClickListener() {
                                                             @Override
                                                             public void onClick(View v) {
                                                                 //Toast.makeText(getApplicationContext(), "외않되", Toast.LENGTH_SHORT). show();
                                                                 EditText editText = (EditText) findViewById(R.id.sendText);
                                                                 String inputValue = editText.getText().toString();
                                                                 editText.setText("");
                                                                 //refresh(inputValue, 0);
                                                                 addUserChat(inputValue);
                                                             }
                                                         }
        );
    }

//
//    private void refresh(String inputValue, int _str) {
//        m_Adapter.add(inputValue, _str);
//        m_Adapter.notifyDataSetChanged();
//    }

    @Override public void onBackPressed() {
        //super.onBackPressed();
    }
    // 출처: http://migom.tistory.com/14 [devlog.gitlab.io]

    /* ---------------------------------------------- CLOVA ----------------------------------------------------------- */
    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.

                //txtResult.append("\n Connected");
                //m_Adapter.add("Connected", 2);
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
                /*if(mResult.length() > 10){
                    Message msg1 = Message.obtain(handler, R.id.finalResult, mResult);
                    //msg1.sendToTarget();
                    handleMessage(msg1);
                    //naverRecognizer.onResult((SpeechRecognitionResult)msg.obj);
                }*/
                //txtResult.setText(mResult);
                //txtResult.append(mResult);
                //m_Adapter.add(mResult,1);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();
                strBuf.append(results.get(0));


                //strBuf.append("\n");
                mResult = strBuf.toString();
                if (!mResult.equals("")) {
                    addUserChat(mResult);
                    //m_Adapter.add(mResult,1);
                    //m_Adapter.notifyDataSetChanged();
                }
                //addUserChat(mResult);
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                //addUserChat(mResult);
                //txtResult.setText(mResult);
                m_Adapter.add("error code:" + mResult, 2);
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
        if(naverRecognizer!=null)
            naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(m_Adapter!=null) {
            mResult = "";
            //txtResult.setText("");
            m_Adapter.add(mResult, 2);
            //btnStart.setText(R.string.str_start);
            //btnStart.setEnabled(true);
            //Log.d("callActivity", "전화시작");
            //Connect(findViewById(R.id.button2));
            //findViewById(R.id.button2).performClick();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.

        if(naverRecognizer!=null)
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


    /* ---------------------------------------------- 채팅방 번호 구하기 ----------------------------------------------------------- */

    private class getChatCnt extends Thread {

        @Override
        public void run() {
            try {
                String link = "http://13.124.94.107/getChatCnt.php";
                String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(user, "UTF-8");
                data += "&" + URLEncoder.encode("FriendId", "UTF-8") + "=" + URLEncoder.encode(connectUser, "UTF-8");

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
                chatCntStr = sb.toString();
            } catch (Exception e) {
                Log.d("getChateCnt Exception: ", e.getMessage().toString());
            }
        }
    }
//
//    private void getChatCnt(final String userId, String friendId) {
//        class getChatRoomNum extends AsyncTask<String, Void, String> {
//            //ProgressDialog loading;
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                //loading = ProgressDialog.show(CallActivity.this, "Please Wait", null, true, true);
//            }
//
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                //loading.dismiss();
//                //int chatnum = Integer.parseInt(s);
//                //chatCntStr = Integer.toString(chatnum);
//                chatCntStr = s;
//            }
//
//            @Override
//            protected String doInBackground(String... params) {
//
//                try {
//                    String userId = (String) params[0];
//                    String friendId = (String) params[1];
//
//
//                    String link = "http://13.124.94.107/getChatCnt.php";
//                    String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");
//                    data += "&" + URLEncoder.encode("FriendId", "UTF-8") + "=" + URLEncoder.encode(friendId, "UTF-8");
//
//
//                    URL url = new URL(link);
//                    URLConnection conn = url.openConnection();
//
//
//                    conn.setDoOutput(true);
//                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//
//
//                    wr.write(data);
//                    wr.flush();
//
//
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//
//
//                    StringBuilder sb = new StringBuilder();
//                    String line = null;
//
//
//                    // Read Server Response
//                    while ((line = reader.readLine()) != null) {
//                        sb.append(line);
//                        break;
//                    }
//                    chatCntStr = sb.toString();
//                    return sb.toString();
//                } catch (Exception e) {
//                    return new String("Exception: " + e.getMessage());
//                }
//            }
//        }
//        getChatRoomNum task = new getChatRoomNum();
//        task.execute(userId, friendId);
//    }

    /* ---------------------------------------------- 채팅방 번호 구하기 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 채팅방 번호 구하기/ DB에 CNT+1----------------------------------------------------------- */

    private class getChatCnt1 extends Thread {
        @Override
        public void run() {
            try {
                String link = "http://13.124.94.107/getChatCnt1.php";
                String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(user, "UTF-8");
                data += "&" + URLEncoder.encode("FriendId", "UTF-8") + "=" + URLEncoder.encode(connectUser, "UTF-8");

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
                chatnum = Integer.parseInt(sb.toString());
                chatCntStr = Integer.toString(chatnum);
            } catch (Exception e) {
                Log.d("getChatCnt1 Exception: ", e.getMessage().toString());
            }
        }
    }

//    private void getChatCnt1(final String userId, String friendId) {
//        class getChatRoomNum1 extends AsyncTask<String, Void, String> {
//            //ProgressDialog loading;
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                //loading = ProgressDialog.show(CallActivity.this, "Please Wait", null, true, true);
//            }
//
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                //loading.dismiss();
//                chatnum = Integer.parseInt(s);
//                chatCntStr = Integer.toString(chatnum);
//                //Toast.makeText(getApplicationContext(), chatCntStr, Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            protected String doInBackground(String... params) {
//
//                try {
//                    String userId = (String) params[0];
//                    String friendId = (String) params[1];
//
//
//                    String link = "http://13.124.94.107/getChatCnt1.php";
//                    String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");
//                    data += "&" + URLEncoder.encode("FriendId", "UTF-8") + "=" + URLEncoder.encode(friendId, "UTF-8");
//
//
//                    URL url = new URL(link);
//                    URLConnection conn = url.openConnection();
//
//
//                    conn.setDoOutput(true);
//                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//
//
//                    wr.write(data);
//                    wr.flush();
//
//
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//
//
//                    StringBuilder sb = new StringBuilder();
//                    String line = null;
//
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
//        getChatRoomNum1 task = new getChatRoomNum1();
//        task.execute(userId, friendId);
//    }

    /* ---------------------------------------------- 채팅방 번호 구하기/ DB에 CNT+1 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 사용자 채팅 DB에 추가 ----------------------------------------------------------- */
    public void addUserChat(String chat) {

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = df.format(c.getTime());

        //Toast.makeText(getApplicationContext(), chatCntStr, Toast.LENGTH_SHORT).show();

        //DatabaseReference myRef = database.getReference("chats").child(formattedDate);
        String chatRoom = user + connectUser;
        Log.d("ddddddddddd", userId);//sooy1
        Log.d("ddddddddddd", user); //inseon
        Log.d("ddddddddddd", friendId); //inseon
        Log.d("ddddddddddd", connectUser); //sooy1
        if (userId.equals(user)) { //실사용자 = 발신자
            //getChatCnt(user, connectUser);
            Log.d("aaaaaaaaa", chatRoom);
            Log.d("aaaaaaaaa", chatCntStr);
        } else { //실사용자 = 수신자
            //getChatCnt(user, connectUser);
            Log.d("aaaaaaaaa", chatRoom);
            Log.d("aaaaaaaaa", chatCntStr);
        }


        DatabaseReference myRef = database.getReference("chats").child(chatRoom).child(chatCntStr).child(formattedDate);


        Hashtable<String, String> chatText = new Hashtable<String, String>();
        //user = intent.getStringExtra("userId");
        chatText.put("text", chat);

        if (userId.equals(user)) { //실사용자 = 발신자
            chatText.put("user", userId);
            chatText.put("friend", friendId);
        } else { //실사용자 = 수신자
            chatText.put("user", connectUser);
            chatText.put("friend", user);
        }
        myRef.setValue(chatText);


        //m_Adapter.add(mResult,1);
        //m_Adapter.notifyDataSetChanged();


        //채팅내용 가져오기
        DatabaseReference databaseReference = firebaseDatabase.getReference("chats").child(chatRoom).child(chatCntStr);


        databaseReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 데이터를 읽어올 때 모든 데이터를 읽어오기때문에 List 를 초기화해주는 작업이 필요하다.
                m_Adapter.clean();
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    //String msg = messageData.getValue().toString();
                    Chat chat = messageData.getValue(Chat.class);

                    if (user.equals(userId)) { //사용자 = 발신자
                        if (user.equals(chat.user)) { //사용자 = 채팅의 user
                            m_Adapter.add(chat.text, 1);
                        } else {
                            m_Adapter.add(chat.text, 0);
                        }
                    } else { //사용자 = 수신자
                        if (connectUser.equals(chat.user)) { //사용자 = 채팅의 user
                            m_Adapter.add(chat.text, 1);
                        } else {
                            m_Adapter.add(chat.text, 0);
                        }
                    }
                }
                // notifyDataSetChanged를 안해주면 ListView 갱신이 안됨
                m_Adapter.notifyDataSetChanged();
                // ListView 의 위치를 마지막으로 보내주기 위함
                m_ListView.setSelection(m_Adapter.getCount() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
/*
        // 데이터 받아오기 및 어댑터 데이터 추가 및 삭제 등..리스너 관리
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Chat chat = dataSnapshot.getValue(Chat.class);
                mChat.add(chat);
                //refresh(mChat.,0);(mChat.size()-1);



            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/
    }



    /* ---------------------------------------------- 사용자 채팅 DB에 추가 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- VIDYO ----------------------------------------------------------- */
    public void Connect() {

        Log.d("connecttt", "연결");
        token = "cHJvdmlzaW9uAHVzZXIxQGFjNjM1OC52aWR5by5pbwA2MzcwMTExMDc5NgAANjk5Yzc1ZDZhMGM1ZDA4NmJkMTJhMWRlMGIxNjViNjM4YWJjZWRmMDAzMzBjMTllZjRiY2FiMGZiMzcxMzE0ODdkYmEyMTgyYTFjZTk0NWVjOTBlZmZhYzhlMzc2ODE0";
        // 전화 받을 떄
        if (callStatus.name().equals("Receiver")) {
            displayName = user + "-" + connectUser;
            userId = connectUser;
            friendId = user;
            Log.d("전화수신", user + "->" + connectUser);

            //Connector(Object viewId, ConnectorViewStyle viewStyle, int remoteParticipants, String logFileFilter, String logFileName, long userData)
            byte num = (byte) 00;
            vc = new Connector(null, VIDYO_CONNECTORVIEWSTYLE_Default, 1, "warning info@VidyoClient info@VidyoConnector", "", 0);
//            vc.showPreview(false);
//            vc.showViewLabel(videoFrame, false);
//            vc.setViewBackgroundColor(videoFrame, num, num, num);
//            vc_preview = new Connector(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0, "warning info@VidyoClient info@VidyoConnector", "", 0);
//            vc_preview.showViewLabel(localFrame, false);
//            vc_preview.setViewBackgroundColor(localFrame, num, num, num);
            RegisterForVidyoEvents();

//            //clova 음성인식 시작
//            if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
//                // Start button is pushed when SpeechRecognizer's state is inactive.
//                // Run SpeechRecongizer by calling recognize().
//                mResult = "";
//                /*if (chatFrame.getVisibility() == View.VISIBLE) {
//                    m_Adapter.add("Connecting...", 2);
//                }*/
//                naverRecognizer.recognize();
//            } else {
//                Log.d(TAG, "stop and wait Final Result");
//                //btnStart.setEnabled(false);
//                naverRecognizer.getSpeechRecognizer().stop();
//            }

            //vc_preview.selectDefaultCamera();
            //vc_preview.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
            vc.connect("prod.vidyo.io", token, "call", displayName, this);
//            ImageButton ibtn = findViewById(R.id.disconnect);
//            ibtn.bringToFront();

            Thread getChatCnt = new getChatCnt();
            getChatCnt.start();

//            while(!cflag.equals("Y")){ }
//
//            if(cflag.equals("Y")){
//
//                //채팅창 보이도록
//                if (chatFrame.getVisibility() == View.GONE) {
//                    chatFrame.setVisibility(View.VISIBLE);
//                } else {
//                    chatFrame.setVisibility(View.GONE);
//                }
//            }

            /*getChatCnt(user, connectUser);

            //채팅창 보이도록
            if (chatFrame.getVisibility() == View.GONE) {
                chatFrame.setVisibility(View.VISIBLE);
            } else {
                chatFrame.setVisibility(View.GONE);
            }*/
        }
        // 전화 걸때
        else {
            callStatus = CallStatus.Caller;
            //user = ((MainActivity) MainActivity.context).getUserId();
            user = userId;
            connectUser = friendId;
            displayName = user + "-" + connectUser;
            Log.d("전화발신", user + "->" + connectUser);

            callStatus = CallStatus.Caller;

//            ImageButton ibtn = findViewById(R.id.disconnect);
//            ibtn.bringToFront();

            //RegisterForVidyoEvents();
            byte num = (byte) 00;
            vc = new Connector(null, VIDYO_CONNECTORVIEWSTYLE_Default, 1, "warning info@VidyoClient info@VidyoConnector", "", 0);
//            vc.showPreview(false);
//            vc.showViewLabel(videoFrame, false);
//            vc.setViewBackgroundColor(videoFrame, num, num, num);
//            vc_preview = new Connector(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0, "warning info@VidyoClient info@VidyoConnector", "", 0);
//            vc_preview.showViewLabel(localFrame, false);
//            vc_preview.setViewBackgroundColor(localFrame, num, num, num);
            RegisterForVidyoEvents();

            Log.d("connecttt", "vidyo 연결");

//            //clova 음성인식 시작
//            if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
//                // Start button is pushed when SpeechRecognizer's state is inactive.
//                // Run SpeechRecongizer by calling recognize().
//                mResult = "";
//                /*if (chatFrame.getVisibility() == View.VISIBLE) {
//                    m_Adapter.add("Connecting...", 2);
//                }*/
//                naverRecognizer.recognize();
//            } else {
//                Log.d(TAG, "stop and wait Final Result");
//                //btnStart.setEnabled(false);
//                naverRecognizer.getSpeechRecognizer().stop();
//            }

            Log.d("connecttt", "clova 시작");

            //vc_preview.selectDefaultCamera();
            //vc_preview.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
            //vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
            vc.connect("prod.vidyo.io", token, "call", displayName, this);
//            ImageButton ibtn = findViewById(R.id.disconnect);
//            ibtn.bringToFront();
            Log.d("connecttt", "connect  시작");

//            발신자만 채팅방 번호 추가  //채팅방이름은 발신자id+수신자id
            Thread getChatCnt1 = new getChatCnt1();
            getChatCnt1.start();

//            while(!cflag.equals("Y")){ }
//
//            if(cflag.equals("Y")){
//                //채팅창 보이도록
//                if (chatFrame.getVisibility() == View.GONE) {
//                    chatFrame.setVisibility(View.VISIBLE);
//                } else {
//                    chatFrame.setVisibility(View.GONE);
//                }
//            }
        }
    }

    public void Disconnect(View v) {

        Log.d("connecttt", "연결종료");
        if(vc!=null) {
            vc.disconnect();
        }

        //if (callStatus.name().equals("Caller")) {
            //stopCall(user);
            Thread stopCallThread = new stopCall();
            stopCallThread.start();
        //}
        //else
            //stopCall(connectUser);

        //clova
        callStatus = CallStatus.Default;
        naverRecognizer.getSpeechRecognizer().stop();
        Log.d(TAG, "clova finish");
        cflag = "N";

        //this.setResult(0);
        finish();
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

            Log.d("connecttt", "successfully disconnected, reason = " + reason.toString());
            //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateDisconnected, "Disconnected");
        } else {
            Log.d("connecttt", "unexpected disconnection, reason = " + reason.toString());
            //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateDisconnectedUnexpected, "Unexpected disconnection");
        }
    }

    // Register for VidyoConnector event listeners. Note: this is an arbitrary function name.
    public void RegisterForVidyoEvents() {
        // Register for Participant callbacks
        vc.registerParticipantEventListener(this);

        // Register to receive chat messages
        vc.registerMessageEventListener(this);

        vc.registerLocalCameraEventListener(this);
        vc.registerRemoteCameraEventListener(this);

       /* Register for local window share and local monitor events */
        //vc.registerLocalMonitorEventListener(this);
    }

    /* custom local preview */
    public void onLocalCameraAdded(LocalCamera localCamera)    {
        Log.d("connecttt", "onLocalCameraAdded");
        vc.assignViewToLocalCamera(localFrame, localCamera, true, false);
        vc.showViewLabel(localFrame, false);
//        vc.setViewBackgroundColor(videoFrame, num, num, num);
        vc.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());

//        if(callStatus == CallStatus.Caller) {
//            ImageButton ibtn = findViewById(R.id.disconnect);
//            ibtn.bringToFront();
//        }
    }

    public void onLocalCameraRemoved(LocalCamera localCamera)  {
        Log.d("connecttt", "onLocalCameraRemoved");
    }

    public void onLocalCameraSelected(final LocalCamera localCamera) { /* Camera was selected by user or automatically */
        Log.d("connecttt", "onLocalCameraSelected");
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (localCamera != null) {
//                    Log.d("connecttt", "localcamera");
//                    //vc_preview.assignViewToCompositeRenderer(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0);
//                    vc_preview.assignViewToLocalCamera(localFrame, localCamera, false, false);
//                    vc_preview.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
//                } else {
//                    vc.hideView(localFrame);
//                }
//            }
//        });
    }
    public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState state) {
        Log.d("connecttt", "onLocalCameraStateUpdated");
        Log.d("connecttt", state.toString());
        //        if(state == Device.DeviceState.VIDYO_DEVICESTATE_Started) {
//            vc.assignViewToLocalCamera(localFrame, localCamera, false, false);
//            vc.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
//        }
    }
    /* Local camera change initiated by user. Note: this is an arbitrary function name. */


    /******************************************************************************/
    /* custom participant's source view */
    public void onRemoteCameraAdded(final RemoteCamera remoteCamera, final Participant participant) {
        Log.d("connecttt", "onRemoteCameraAdded");
        Log.d("connecttt", participant.getId().toString());

        //        vc.assignViewToCompositeRenderer(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0);
        vc.assignViewToRemoteCamera(videoFrame, remoteCamera, true, false);
        vc.showViewLabel(videoFrame, false);
        vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());

        //키보드 보이게
        sendText.setFocusableInTouchMode(true);
        sendText.setClickable(true);
        sendText.setFocusable(true);

//        if(callStatus == CallStatus.Caller) {
//            ImageButton ibtn = findViewById(R.id.disconnect);
//            ibtn.bringToFront();
//        }

//        if (remoteCamera!=null) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.d("connecttt", "remotecamera");
////                    vc.assignViewToRemoteCamera(videoFrame, remoteCamera, false, false);
////                    vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
////                    ImageButton ibtn = findViewById(R.id.disconnect);
////                    ibtn.bringToFront();
//                    //vc.showPreview(false);
//                    //vc_preview = new Connector(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0, "warning info@VidyoClient info@VidyoConnector", "", 0);
//                    //vc_preview.showViewLabel(localFrame, false);
//                    vc.assignViewToCompositeRenderer(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0);
//                    vc.assignViewToLocalCamera(localFrame, localCam, false, false);
//                    //vc.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
//                    //vc_preview.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
//                    //assignViewToLocalCamera(Object viewId, LocalCamera localCamera, boolean displayCropped, boolean allowZoom)
//                    //vc_preview.setViewBackgroundColor(localFrame, num, num, num);
//                }
//            });
//        }
    }

    public void onRemoteCameraRemoved(RemoteCamera remoteCamera, Participant participant) {
        Log.d("connecttt", "onRemoteCameraRemoved");
        /* Existing camera became unavailable */
        vc.hideView(R.id.videoFrame);
    }

    public void onRemoteCameraStateUpdated(RemoteCamera remoteCamera, Participant participant, Device.DeviceState state) {
        Log.d("connecttt", "onRemoteCameraStateUpdated");
    }

    /******************************************************************************/


    // Participant Joined
    public void onParticipantJoined(Participant participant) {
        Log.d("connecttt", "ParticipainJoined");
        cflag = "Y";

        //clova 음성인식 시작
        if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
            // Start button is pushed when SpeechRecognizer's state is inactive.
            // Run SpeechRecongizer by calling recognize().
            mResult = "";
                /*if (chatFrame.getVisibility() == View.VISIBLE) {
                    m_Adapter.add("Connecting...", 2);
                }*/
            naverRecognizer.recognize();
        } else {
            Log.d(TAG, "stop and wait Final Result");
            //btnStart.setEnabled(false);
            naverRecognizer.getSpeechRecognizer().stop();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton b = findViewById(R.id.sendButton);
                b.setEnabled(true);
            }
        });

        //키보드 보이게
        /*sendText.setFocusableInTouchMode(true);
        sendText.setClickable(true);
        sendText.setFocusable(true);*/
        //imm.showSoftInput(sendText, 0);
    }

    // Participant Left
    public void onParticipantLeft(Participant participant) {
        Log.d("connecttt", "ParticipantLeft");
        Disconnect(findViewById(R.id.disconnect));
    }

    // Ordered array of participants according to rank
    public void onDynamicParticipantChanged(ArrayList participants, ArrayList cameras) {
    }

    // Current loudest speaker
    public void onLoudestParticipantChanged(Participant participant, boolean audioOnly) {
    }

    private void SendChatMessage(String message) {
        vc.sendChatMessage(message);
    }

    // Message received from other participants
    public void onChatMessageReceived(Participant participant, ChatMessage chatMessage) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release device resources

        Log.d("Destroy", "true");

        if(vc!=null) {
            vc.disable();
            vc = null;
        }

        if(mVidyoClientInitialized)
            ConnectorPkg.uninitialize();

        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            Log.d("callActivity", e.toString());
        }

        if(callStatus != CallStatus.Default)
            Disconnect(findViewById(R.id.disconnect));
    }

    private class startCall extends Thread {
        @Override
        public void run() {
            try {

                String link = "http://13.124.94.107/startCall.php";
                String data = URLEncoder.encode("connectId", "UTF-8") + "=" + URLEncoder.encode(friendId, "UTF-8");
                data += "&" + URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");

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

                Connect();
            } catch (Exception e) {
                Log.d("startCall Exception: ", e.getMessage().toString());
            }
        }
    }
//
//    private void startCall(final String connectId, String Id) {
//
//        class InsertData extends AsyncTask<String, Void, String> {
//
//            protected void onCancelled() {
//                //stopCall(user);
//            }
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//            }
//
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                Connect();
//            }
//
//            @Override
//            protected String doInBackground(String... params) {
//
//                if (this.isCancelled()) {
//                    // 비동기작업을 cancel해도 자동으로 취소해주지 않으므로,
//                    // 작업중에 이런식으로 취소 체크를 해야 한다.
//                    return null;
//                }
//
//                try {
//                    String connectId = (String) params[0];
//                    String Id = (String) params[1];
//                    Log.d("connecttt", connectId + Id);
//
//                    String link = "http://13.124.94.107/startCall.php";
//                    String data = URLEncoder.encode("connectId", "UTF-8") + "=" + URLEncoder.encode(connectId, "UTF-8");
//                    data += "&" + URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
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
//        task.execute(connectId, Id);
//    }

    private class stopCall extends Thread {

        @Override
        public void run() {
            try {
                String link = "http://13.124.94.107/stopCall.php";
                String data;

                data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");

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
            } catch (Exception e) {
                Log.d("stopCall exception", e.getMessage().toString());
            }
        }
    }

//    private void stopCall(final String Id) {
//
//        class InsertData extends AsyncTask<String, Void, String> {
//            //ProgressDialog loading;
//
//            protected void onCancelled() {
//                super.onCancelled();
//            }
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                //loading = ProgressDialog.show(CallActivity.this, "caling...", null, true, true);
//                //Toast.makeText(getApplicationContext(), "외않되", Toast.LENGTH_SHORT). show();
//            }
//
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                //loading.dismiss();
//
//                if (s.toString().equals("Disconnect")) {
//                    Log.d("Disconnect", "DB초기화");
//                }
//            }
//
//            @Override
//            protected String doInBackground(String... params) {
//
//                if (this.isCancelled()) {
//                    // 비동기작업을 cancel해도 자동으로 취소해주지 않으므로,
//                    // 작업중에 이런식으로 취소 체크를 해야 한다.
//                    return null;
//                }
//
//                try {
//                    String Id = (String) params[0];
//
//                    String link = "http://13.124.94.107/stopCall.php";
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent mintent) {
            if (mintent.getAction().equals("CALL_DENIAL")) {
                Log.d("connecttt", "call_denial");
                Disconnect(findViewById(R.id.disconnect));
            }
        }
    };

    public String getConnectUser() {
        return connectUser;
    }

}