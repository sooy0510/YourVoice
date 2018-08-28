package com.example.ds.yourvoice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.ds.yourvoice.utils.AudioWriterPCM;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Device.LocalMicrophone;
import com.vidyo.VidyoClient.Device.LocalSpeaker;
import com.vidyo.VidyoClient.Device.RemoteCamera;
import com.vidyo.VidyoClient.Endpoint.ChatMessage;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import static com.vidyo.VidyoClient.Connector.Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default;

public class CallActivity extends AppCompatActivity
        implements Connector.IConnect, Connector.IRegisterParticipantEventListener, Connector.IRegisterMessageEventListener, Connector.IRegisterLocalCameraEventListener,
        Connector.IRegisterRemoteCameraEventListener, Connector.IRegisterLocalMicrophoneEventListener, Connector.IRegisterLocalSpeakerEventListener {

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
    private boolean mVidyoClientInitialized = false;
    private String userId;
    private String friendId;
    private int chatnum; //채팅방 번호
    private String chatRoom;
    private String chatCntStr; //채팅방 디렉토리 이름
    private String cflag = "N";
    private String result;

    //Layout, UI
    private EditText sendText;
    private LinearLayout videoFrame;
    private LinearLayout localFrame;
    private LinearLayout chatFrame;


    //clova
    private static final String TAG = MainActivity.class.getSimpleName();
    private static String CLIENT_ID;

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    private String mResult;
    private AudioWriterPCM writer;


    //chat
    ListView m_ListView;
    MessageAdapter m_Adapter;

    //firebase
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    //사진 전송
    int flag;
    private ImageView showImage;
    private ImageButton closeImage;
    private ImageButton gallery;
    private String imagePath;
    //private Uri imgUri, photoURI, albumURI;
    private String mCurrentPhotoPath;
    private static final int FROM_CAMERA = 0;
    private static final int FROM_ALBUM = 1;
    private Uri photoUrl;
    private Uri file;
    private String urlLastPath;
    private ChildEventListener iChildEventListener;


    enum CallStatus {
        Default,
        Caller,
        Receiver
    }

    enum VidyoConnectorState {
        VidyoConnectorStateConnected,
        VidyoConnectorStateDisconnected,
        VidyoConnectorStateDisconnectedUnexpected,
        VidyoConnectorStateFailure
    }


    public CallStatus callStatus = CallStatus.Default;
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
        mIntentFilter.addAction("CALL_STOP_CHECK");
        registerReceiver(mReceiver, mIntentFilter);

        ConnectorPkg.setApplicationUIContext(this);
        mVidyoClientInitialized = ConnectorPkg.initialize();

        /* UI 초기화 */
        localFrame = findViewById(R.id.localFrame);
        videoFrame = findViewById(R.id.videoFrame);
        //chat
        chatFrame = findViewById(R.id.chatFrame);
        sendText = findViewById(R.id.sendText);
        //사진
        gallery = findViewById(R.id.gallery);
        showImage = findViewById(R.id.showimage);
        closeImage = findViewById(R.id.close);

        sendText.setClickable(false);
        sendText.setFocusable(false);
        gallery.setVisibility(View.INVISIBLE);
        closeImage.setVisibility(View.INVISIBLE);

        // 자막 어댑터 생성
        m_Adapter = new MessageAdapter();
        // Xml에서 추가한 ListView 연결
        m_ListView = findViewById(R.id.listView);
        // ListView에 어댑터 연결
        m_ListView.setAdapter(m_Adapter);

        //키보드
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        calllayout = findViewById(R.id.activity_keyboard);
        InputMethodManager controlManager = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
        softKeyboard = new SoftKeyboard(calllayout, controlManager);

        //firebase
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        intent = getIntent();

        //userid, friendid 받기
        userId = intent.getStringExtra("userId");
        friendId = intent.getStringExtra("friendId");

        handler = new RecognitionHandler(this);



        if (intent.getStringExtra("Caller") != null && intent.getStringExtra("Receiver") != null) {
            CLIENT_ID = "Us8JNMyTCu8dGWq1HCqh";
            naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
            user = intent.getStringExtra("Caller");
            connectUser = intent.getStringExtra("Receiver");
            callStatus = CallStatus.Receiver;

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
            Thread getChatCnt = new getChatCnt();
            getChatCnt.start();
        } else {
            CLIENT_ID = "PGBXBwUedxYBZ2tHbjB6";
            naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
            callStatus = CallStatus.Caller;

            user = userId;
            connectUser = friendId;

            Thread startCall = new startCall();
            startCall.start();

            Log.d("콜액티비티실행", "발신자");
            Thread getChatCnt1 = new getChatCnt1();
            getChatCnt1.start();
        }

        //앨범선택, 사진촬영, 취소 다이얼로그 생성
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeDialog();
            }
        });


        //사진닫기 버튼
        closeImage.setOnClickListener(new View.OnClickListener(){ //닫기버튼
            @Override
            public void onClick(View view) {
                showImage.setVisibility(View.INVISIBLE);
                database.getReference("chats").child(chatRoom).child(chatCntStr).child("image").setValue(null);
                storageRef.child("images").child(urlLastPath).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(CallActivity.this, "삭제 완료",Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CallActivity.this, "삭제 실패",Toast.LENGTH_SHORT).show();
                    }
                });
                videoFrame.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));;
            }
        });



        //키보드
        softKeyboard.setSoftKeyboardCallback(new SoftKeyboard.SoftKeyboardChanged() {
            @Override
            public void onSoftKeyboardHide() {
                new Handler(Looper.getMainLooper())
                        .post(new Runnable() {
                            @Override
                            public void run() {
                                // 키보드 내려왔을때
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
                                                                 EditText editText = (EditText) findViewById(R.id.sendText);
                                                                 String inputValue = editText.getText().toString();
                                                                 editText.setText("");
                                                                 addUserChat(inputValue, 0);
                                                             }
                                                         }
        );

    }

    @Override public void onBackPressed() {
        //super.onBackPressed();
    }
    // 출처: http://migom.tistory.com/14 [devlog.gitlab.io]

    private void makeDialog(){

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(CallActivity.this,R.style.Theme_AppCompat_Dialog);

        alt_bld.setTitle("사진 업로드").setIcon(R.drawable.caller).setCancelable(

                false).setNeutralButton("앨범선택",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        Log.v("알림", "다이얼로그 > 앨범선택 선택");
                        flag = 1;
                        //앨범에서 선택
                        selectAlbum();
                    }
                }).setNegativeButton("취소   ",

                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.v("알림", "다이얼로그 > 취소 선택");
                        // 취소 클릭. dialog 닫기.
                        dialog.cancel();
                    }
                });

        AlertDialog alert = alt_bld.create();
        alert.show();
    }


    public File createImageFile() throws IOException {
        String imgFileName = System.currentTimeMillis() + ".jpg";
        File imageFile= null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "ireh");

        if(!storageDir.exists()){
            //없으면 만들기
            Log.v("알림","storageDir 존재 x " + storageDir.toString());
            storageDir.mkdirs();
        }

        Log.v("알림","storageDir 존재함 " + storageDir.toString());
        imageFile = new File(storageDir,imgFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;

    }


    //앨범 선택 클릭
    public void selectAlbum(){

        //앨범에서 이미지 가져옴
        //앨범 열기
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setType("image/*");

        startActivityForResult(intent, FROM_ALBUM);
    }


    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK){
            return;
        }

        switch (requestCode){
            case FROM_ALBUM : {
                //앨범에서 가져오기
                if(data.getData()!=null){
                    try{
                        imagePath = getPath(data.getData());
                        //File f = new File(imagePath);
                        upload(imagePath);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
            }


            case FROM_CAMERA : {
                //카메라 촬영
                try{
                    Log.v("알림", "FROM_CAMERA 처리");
                    //galleryAddPic();
                    //img1.setImageURI(imgUri);
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /* ---------------------------------------------- firebase refresh ----------------------------------------------------------- */
    public void firebaseRefresh(){
        DatabaseReference databaseReference1 = database.getReference("chats").child(chatRoom).child(chatCntStr);
        iChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.hasChild("image")){
                    Log.d("gggiii",database.getReference("image").toString());
                    Log.d("gggiii","사진추가");
                    Glide.with(CallActivity.context).load(dataSnapshot.child("image").getValue(ImageDTO.class).imageUrl).into(showImage);
                    closeImage.setVisibility(View.VISIBLE);
                    Log.d("gggiii","맨앞으로");
                    showImage.setVisibility(View.VISIBLE);
                    videoFrame.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));;
                }else{
                    // 데이터를 읽어올 때 모든 데이터를 읽어오기때문에 List 를 초기화해주는 작업이 필요하다.
                    m_Adapter.clean();
                    for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                        //String msg = messageData.getValue().toString();
                        Log.d("gggttt",messageData.child("text").getValue().toString());
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
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //if(!dataSnapshot.getValue(ImageDTO.class).imageUrl.equals("")){  //imageurl 잇으면
                if(dataSnapshot.hasChild("image")){
                    Log.d("gggiii",database.getReference("image").toString());
                    Log.d("gggiii","사진추가");
                    Glide.with(CallActivity.context).load(dataSnapshot.child("image").getValue(ImageDTO.class).imageUrl).into(showImage);
                    closeImage.setVisibility(View.VISIBLE);
                    Log.d("gggiii","맨앞으로");
                    showImage.setVisibility(View.VISIBLE);
                }else{
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
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                showImage.setVisibility(View.INVISIBLE);
                closeImage.setVisibility(View.INVISIBLE);
                videoFrame.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 0.0f));;
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference1.addChildEventListener(iChildEventListener);
    }
    /* ---------------------------------------------- firebase refresh 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- 사진 storage에 업로드 ----------------------------------------------------------- */
    public void upload(String uri){
        //Log.d("gggggg",uri);  //getpath까지 한 주소
        storageRef = storage.getReferenceFromUrl("gs://yourvoice-577c9.appspot.com");
        final String chatRoom = user + connectUser;
        file = Uri.fromFile(new File(uri));

        urlLastPath = file.getLastPathSegment();
        StorageReference riversRef = storageRef.child("images/"+urlLastPath);
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d("ggggggg","실패");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.

                photoUrl = taskSnapshot.getDownloadUrl();

                ImageDTO imageDTO = new ImageDTO();
                imageDTO.imageUrl = photoUrl.toString();


                //database.getReference("chats").child(chatRoom).child(chatCntStr).child(formattedDate);
                DatabaseReference myRef = database.getReference("chats").child(chatRoom).child(chatCntStr).child("image");
                Hashtable<String, ImageDTO> chatText = new Hashtable<String, ImageDTO>();
                chatText.put("image", imageDTO);
                myRef.setValue(chatText);
            }
        });
    }

    /* ---------------------------------------------- 사진 storage에 업로드 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- imagePath 구하기 ----------------------------------------------------------- */
    public String getPath(Uri uri){
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(this, uri, proj, null, null, null);

        Cursor cursor = cursorLoader.loadInBackground();
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();
        return cursor.getString(index);
    }
    /* ---------------------------------------------- imagePath 구하기 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- CLOVA ----------------------------------------------------------- */
    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
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
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();
                strBuf.append(results.get(0));

                if(cflag.equals("Y")){
                    mResult = strBuf.toString();
                    if (!mResult.equals("")) {
                        addUserChat(mResult,0);
                    }
                }
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                //addUserChat(mResult);
                //txtResult.setText(mResult);
                m_Adapter.add("error code:" + mResult, 2);
                break;

            case R.id.clientInactive:
                //음성인식 다시 시작
                naverRecognizer.recognize();
                if (writer != null) {
                    writer.close();
                }
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
            m_Adapter.add(mResult, 2);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.

        if(naverRecognizer!=null)
            naverRecognizer.getSpeechRecognizer().release();

        if(callStatus != CallStatus.Default) {
            Disconnect(findViewById(R.id.disconnect));
        }

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


    /* ---------------------------------------------- 채팅방 번호 구하기(수신자) ----------------------------------------------------------- */

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
                Log.d("hhhhh","getchatcnt");
                chatCntStr = sb.toString();
                chatRoom = user+connectUser;
            } catch (Exception e) {
                Log.d("getChateCnt Exception: ", e.getMessage().toString());
            }

            firebaseRefresh();
        }
    }

    /* ---------------------------------------------- 채팅방 번호 구하기(수신자) 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 채팅방 번호 구하기(발신자)/ DB에 CNT+1----------------------------------------------------------- */

    private class getChatCnt1 extends Thread {
        @Override
        public void run() {
            try {
                String link = "http://13.124.94.107/getChatCnt1.php";
                String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");
                data += "&" + URLEncoder.encode("FriendId", "UTF-8") + "=" + URLEncoder.encode(friendId, "UTF-8");

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
                chatRoom = userId+friendId;
            } catch (Exception e) {
                Log.d("getChatCnt1 Exception: ", e.getMessage().toString());
            }
            firebaseRefresh();
        }
    }

  /* ---------------------------------------------- 채팅방 번호 구하기(발신자)/ DB에 CNT+1 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 사용자 채팅 DB에 추가 ----------------------------------------------------------- */
    public void addUserChat(final String chat, int flag) {

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String formattedDate = df.format(c.getTime());

        Log.d("ddddddddddd", userId);//sooy1
        Log.d("ddddddddddd", user); //inseon
        Log.d("ddddddddddd", friendId); //inseon
        Log.d("ddddddddddd", connectUser); //sooy1

        final String chatRoom = user + connectUser;
        final DatabaseReference myRef = database.getReference("chats").child(chatRoom).child(chatCntStr).child("text").child(formattedDate);

        if(flag == 0){  //텍스트 보내기
            Hashtable<String, String> chatText = new Hashtable<String, String>();
            chatText.put("text", chat);

            if (userId.equals(user)) { //실사용자 = 발신자
                chatText.put("user", userId);
                chatText.put("friend", friendId);
            } else { //실사용자 = 수신자
                chatText.put("user", connectUser);
                chatText.put("friend", user);
            }
            myRef.setValue(chatText);

        }
    }



  /* ---------------------------------------------- 사용자 채팅 DB에 추가 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- VIDYO ----------------------------------------------------------- */
    public void Connect() {

        Log.d("connecttt", "연결");
        token = "cHJvdmlzaW9uAFlvdXJWb2ljZUAxNmNlOTMudmlkeW8uaW8AMTAwMDAwMDA2MzcwMjU2MzEyMAAANDAwNzIzZjQzZDI0YWE3MjI4NDUzNmYzNjc2MzE3YmU4MjY1ZGJmNGQwMzA5ZjZhMDFjZjBkNTY2NTdmZGVjMGIxYmFlZGYyMmI4MWRlNWFkNjEzOTVmODkxYzJjNDQw";
        // 전화 받을 떄
        if (callStatus.name().equals("Receiver")) {
            displayName = user + "-" + connectUser;
            userId = connectUser;
            friendId = user;
            Log.d("전화수신", user + "->" + connectUser);

            byte num = (byte) 00;
            vc = new Connector(null, VIDYO_CONNECTORVIEWSTYLE_Default, 1, "warning info@VidyoClient info@VidyoConnector", "", 0);
            vc. setCpuTradeOffProfile (Connector.ConnectorTradeOffProfile.VIDYO_CONNECTORTRADEOFFPROFILE_High);
//            vc.showPreview(false);
//            vc.showViewLabel(videoFrame, false);
//            vc.setViewBackgroundColor(videoFrame, num, num, num);
//            vc_preview = new Connector(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0, "warning info@VidyoClient info@VidyoConnector", "", 0);
//            vc_preview.showViewLabel(localFrame, false);
//            vc_preview.setViewBackgroundColor(localFrame, num, num, num);
            RegisterForVidyoEvents();

            //clova 음성인식 시작
            if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
                // Start button is pushed when SpeechRecognizer's state is inactive.
                // Run SpeechRecongizer by calling recognize().
                mResult = "";
                naverRecognizer.recognize();
            } else {
                Log.d(TAG, "stop and wait Final Result");
                naverRecognizer.getSpeechRecognizer().stop();
            }

            vc.connect("prod.vidyo.io", token, "call", displayName, this);
        }
        // 전화 걸때
        else {
            callStatus = CallStatus.Caller;
            displayName = user + "-" + connectUser;
            Log.d("전화발신", user + "->" + connectUser);

            callStatus = CallStatus.Caller;

            byte num = (byte) 00;
            vc = new Connector(null, VIDYO_CONNECTORVIEWSTYLE_Default, 1, "warning info@VidyoClient info@VidyoConnector", "", 0);
            vc. setCpuTradeOffProfile (Connector.ConnectorTradeOffProfile.VIDYO_CONNECTORTRADEOFFPROFILE_High);

//            vc.showPreview(false);
//            vc.showViewLabel(videoFrame, false);
//            vc.setViewBackgroundColor(videoFrame, num, num, num);
//            vc_preview = new Connector(localFrame, VIDYO_CONNECTORVIEWSTYLE_Default, 0, "warning info@VidyoClient info@VidyoConnector", "", 0);
//            vc_preview.showViewLabel(localFrame, false);
//            vc_preview.setViewBackgroundColor(localFrame, num, num, num);
            RegisterForVidyoEvents();

            Log.d("connecttt", "vidyo 연결");

            //clova 음성인식 시작
            if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
                // Start button is pushed when SpeechRecognizer's state is inactive.
                // Run SpeechRecongizer by calling recognize().
                mResult = "";
                naverRecognizer.recognize();
            } else {
                Log.d(TAG, "stop and wait Final Result");
                //btnStart.setEnabled(false);
                naverRecognizer.getSpeechRecognizer().stop();
            }

            Log.d("connecttt", "clova 시작");

            //vc_preview.selectDefaultCamera();
            //vc_preview.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());
            //vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());
            vc.connect("prod.vidyo.io", token, "call", displayName, this);
            Log.d("connecttt", "connect  시작");
        }
    }

    public void Disconnect(View v) {

        Log.d("connecttt", "연결종료");
        if(vc!=null) {
            vc.disconnect();
        }

        Thread stopCallThread = new stopCall();
        stopCallThread.start();

        //clova
        callStatus = CallStatus.Default;
        naverRecognizer.getSpeechRecognizer().stop();
        Log.d(TAG, "clova finish");
        cflag = "N";
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
        vc.registerLocalMicrophoneEventListener(this);
        vc.registerLocalSpeakerEventListener(this);

     /* Register for local window share and local monitor events */
        //vc.registerLocalMonitorEventListener(this);
    }

    /* custom local preview */
    public void onLocalCameraAdded(LocalCamera localCamera) {
        Log.d("connecttt", "onLocalCameraAdded");
        vc.assignViewToLocalCamera(localFrame, localCamera, true, false);
        localCamera.setAspectRatioConstraint(1, 1);
        vc.showViewLabel(localFrame, false);
//        vc.setViewBackgroundColor(videoFrame, num, num, num);
        vc.showViewAt(localFrame, 0, 0, localFrame.getWidth(), localFrame.getHeight());

        localCamera.setFramerateTradeOffProfile (LocalCamera.LocalCameraTradeOffProfile.VIDYO_LOCALCAMERATRADEOFFPROFILE_High);
        localCamera.setResolutionTradeOffProfile (LocalCamera.LocalCameraTradeOffProfile.VIDYO_LOCALCAMERATRADEOFFPROFILE_High);
    }

    public void onLocalCameraRemoved(LocalCamera localCamera) {
        Log.d("connecttt", "onLocalCameraRemoved");
    }

    public void onLocalCameraSelected(final LocalCamera localCamera) { /* Camera was selected by user or automatically */
        Log.d("connecttt", "onLocalCameraSelected");
    }
    public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState state) {
        Log.d("connecttt", "onLocalCameraStateUpdated");
        Log.d("connecttt", state.toString());
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
    /* Microphone event listener */
    public void onLocalMicrophoneAdded(LocalMicrophone localMicrophone)    {
        Log.d("connecttt", "onLocalMicrophoneAdded");
        //vc.selectLocalMicrophone(localMicrophone);
    }
    public void onLocalMicrophoneRemoved(LocalMicrophone localMicrophone)  {
        Log.d("connecttt", "onLocalMicrophoneRemoved");
    }
    public void onLocalMicrophoneSelected(LocalMicrophone localMicrophone) {
        Log.d("connecttt", "onLocalMicrophoneSelected");
    }
    public void onLocalMicrophoneStateUpdated(LocalMicrophone localMicrophone, Device.DeviceState state) {
        Log.d("connecttt", "onLocalMicrophoneStateUpdated");
        Log.d("connecttt", state.toString());
    }

    /* Speaker event listener */
    public void onLocalSpeakerAdded(LocalSpeaker localSpeaker)    {
        Log.d("connecttt", "onLocalSpeakerAdded");
        //vc.selectLocalSpeaker(localSpeaker);
    }
    public void onLocalSpeakerRemoved(LocalSpeaker localSpeaker)  {
        Log.d("connecttt", "onLocalSpeakerRemoved");
    }
    public void onLocalSpeakerSelected(LocalSpeaker localSpeaker) {
        Log.d("connecttt", "onLocalSpeakerSelected");
    }
    public void onLocalSpeakerStateUpdated(LocalSpeaker localSpeaker, Device.DeviceState state) {
        Log.d("connecttt", "onLocalSpeakerStateUpdated");
        Log.d("connecttt", state.toString());
    }

    /******************************************************************************/

    // Participant Joined
    public void onParticipantJoined(Participant participant) {
        Log.d("connecttt", "ParticipainJoined");
        cflag = "Y"; //음성인식 시작 flag

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton b = findViewById(R.id.sendButton);
                b.setEnabled(true);
                gallery.setVisibility(View.VISIBLE);
            }
        });

        //키보드 보이게
        sendText.setFocusableInTouchMode(true);
        sendText.setClickable(true);
        sendText.setFocusable(true);
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

    //상대방이 강제 종료 될 경우
    private class stopCheck extends Thread {

        @Override
        public void run() {
            //super.run();

            //통화 연결 될때까지 DB 체크
            while (cflag.equals("N")) {
                try {
                    String num = "3";

                    String link = "http://13.124.94.107/callingUpdate.php";
                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(friendId, "UTF-8");
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
                        break;
                    }

                    if (result.equals("denial")) {
                        Disconnect(findViewById(R.id.disconnect));
                        Log.d("connecttt", "stopcheck");
                        break;
                    }
                } catch (Exception e) {
                    Log.d("발신자전화서비스스레드 Exception", e.toString());
                }
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent mintent) {
            if (mintent.getAction().equals("CALL_DENIAL")) {
                Log.d("connecttt", "call_denial");
                Disconnect(findViewById(R.id.disconnect));
            }
            if (mintent.getAction().equals("CALL_STOP_CHECK")) {
                Log.d("connecttt", "call_stop_check");
                Thread stopCheckThread = new stopCheck();
                stopCheckThread.start();
            }
        }
    };

}