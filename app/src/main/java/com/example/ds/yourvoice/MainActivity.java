package com.example.ds.yourvoice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import com.example.ds.yourvoice.FListViewAdapter.ListBtnClickListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements FListViewAdapter.ListBtnClickListener, FListViewAdapter.ListBtnLongClickListener, RListViewAdapter.ListBtnClickListener, RListViewAdapter.ListBtnLongClickListener {

    Toolbar myToolbar;
    EditText addId;
    Intent intent;
    public static String userId, userName;

    /* Firebase */
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    FirebaseDatabase database;

    /* 친구목록 리스트뷰 변수 */
    ArrayList<HashMap<String, String>> fArrayList;
    String fJsonString;
    ListView f_listview;  //MainActivity에서 리스트뷰 위치
    FListViewAdapter f_adapter;
    ArrayList<FListViewItem> f_list = new ArrayList<FListViewItem>(); //실질적인 listview

    /* 최근통화목록 리스트뷰 변수 */
    ArrayList<HashMap<String, String>> rArrayList;
    String rJsonString;
    ListView r_listview;  //MainActivity에서 리스트뷰 위치
    RListViewAdapter r_adapter;
    ArrayList<RListViewItem> r_list = new ArrayList<RListViewItem>(); //실질적인 listview

    String checkvis;

    public static Context context;
    public Intent cIntent;

    /* 최근통화목록 삭제 변수 */
    String rUserId;
    String rFriendId;

    //친구목록
    private static String FTAG = "FRIENDLIST";

    private static final String FTAG_JSON = "friendList";
    private static final String FTAG_NAME = "friendname";
    private static final String FTAG_ID = "friendid";

    //최근통화
    private static String RTAG = "RECENTCALL";

    private static final String RTAG_JSON = "recentCallList";
    private static final String RTAG_USERID = "userid";
    private static final String RTAG_NAME = "friendname";
    private static final String RTAG_ID = "friendid";
    private static final String RTAG_DATE = "calldate";
    private static final String RTAG_CHATCNT = "chatcnt";
    private static final String RTAG_CALLER = "caller";
    private static final String RTAG_RECEIVER = "receiver";
    private static final String RTAG_VISIBLE = "visible";

    private RestartService restartService;

    private static PowerManager.WakeLock sCpuWakeLock;

    enum CallStatus {
        Default,
        Caller,
        Receiver
    }

    public CallStatus callStatus = CallStatus.Default;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        context = this;

        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        userName = intent.getStringExtra("userName");

        //firebase
        database = FirebaseDatabase.getInstance();

        cIntent = new Intent(this, CallService.class);
        cIntent.putExtra("user", userId);
        startService(cIntent);

        // 추가된 소스, Toolbar를 생성한다.
        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true); //커스터마이징 하기 위해 필요
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true); // 뒤로가기 버튼, 디폴트로 true만 해도 백버튼이 생김

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        //TabHost tabHost1 = getTabHost();

        TabHost.TabSpec ts1 = tabHost.newTabSpec("Tab1");
        ts1.setContent(R.id.tab_friendList);
        ts1.setIndicator("친구목록");
        tabHost.addTab(ts1);

        TabHost.TabSpec ts2 = tabHost.newTabSpec("Tab2");
        ts2.setContent(R.id.tab_recentCall);
        ts2.setIndicator("최근통화기록");
        tabHost.addTab(ts2);

        TabHost.TabSpec ts3 = tabHost.newTabSpec("Tab3");
        //ts3.setContent(new Intent(MainActivity.this, AddFriendActivity.class));
        ts3.setContent(R.id.tab_addFriend);
        // ts3.setContent(new Intent(MainActivity.this, AddFriendActivity.class));
        ts3.setIndicator("친구추가");
        tabHost.addTab(ts3);

        //로그인 했을때 처음 화면 설정
        tabHost.setCurrentTab(0);

        //친구목록 가져오기
        fArrayList = new ArrayList<>();
        friendData task1 = new friendData();
        task1.execute("http://13.124.94.107/getFriendList.php");

        //최근통화리스트 가져오기
        rArrayList = new ArrayList<>();
        recentCallData task2 = new recentCallData();
        task2.execute("http://13.124.94.107/getRecentCallList.php");


        //탭 변경
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                // TODO Auto-generated method stub
                String strMsg;
                strMsg = tabId;
                //Toast.makeText(getApplicationContext(), strMsg, Toast.LENGTH_SHORT). show();

                if (strMsg.equals("Tab1")) {
                    /* 기존에 있던 어댑터 삭제*/
                    f_adapter.clear();
                    /* 아이템 추가 및 어댑터 등록 */
                    fArrayList = new ArrayList<>();

                    friendData task = new friendData();
                    task.execute("http://13.124.94.107/getFriendList.php");
                }

                if (strMsg.equals("Tab2")) {
                    /* 기존에 있던 어댑터 삭제*/
                    r_adapter.clear();
                    /* 아이템 추가 및 어댑터 등록 */
                    rArrayList = new ArrayList<>();

                    recentCallData task = new recentCallData();
                    task.execute("http://13.124.94.107/getRecentCallList.php");
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startService(cIntent);
    }

    /* ---------------------------------------------- 설정 버튼 ----------------------------------------------------------- */
    //추가된 소스, ToolBar에 menu.xml을 인플레이트함
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    //추가된 소스, ToolBar에 추가된 항목의 select 이벤트를 처리하는 함수
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_settings1:
                // User chose the "Settings" item, show the app settings UI...
                Toast.makeText(getApplicationContext(), "환경설정 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return true;
            case R.id.logout:
                new AlertDialog.Builder(this)
                        .setTitle("로그아웃").setMessage("로그아웃 하시겠습니까?")
                        .setPositiveButton("로그아웃", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                logout(userId);
                                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                                //i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(i);
                                SharedPreferences auto = getSharedPreferences("auto", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = auto.edit();
                                editor.clear();
                                editor.commit();
                                Toast.makeText(MainActivity.this, "로그아웃", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        })
                        .show();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                Toast.makeText(getApplicationContext(), "나머지 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return super.onOptionsItemSelected(item);
        }
    }
    /* ---------------------------------------------- 설정 버튼 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- TAB1의 버튼 클릭 ----------------------------------------------------------- */
    @Override
    public void onListBtnClick1(View v, final int position) {
        //전화버튼 눌렀을때
        if (v.getId() == R.id.fphone) {
            FListViewItem friend = new FListViewItem();
            friend = (FListViewItem) f_adapter.getItem(position);
            String id = friend.getId();
            String fname =  friend.getName();
            //Toast.makeText(getApplicationContext(), id, Toast.LENGTH_LONG).show();
            //call(id, v);
            startCall(id, userId, fname);
        }/* else {
            new AlertDialog.Builder(this)
                    .setTitle("친구 삭제")
                    .setMessage("삭제하시겠습니까?")
                    .setIcon(R.drawable.deletefriend)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // 확인시 처리 로직
                            FListViewItem s = new FListViewItem();
                            s = (FListViewItem) f_adapter.getItem(position);
                            String id = s.getId();

                            deleteFriend(position, id);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // 취소시 처리 로직
                        }
                    })
                    .show();
        }*/
    }


    //@Override
    public boolean onListBtnLongClick1(View v, final int position) {
        new AlertDialog.Builder(this)
                .setTitle("친구 삭제")
                .setMessage("삭제하시겠습니까?")
                .setIcon(R.drawable.deletefriend)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 확인시 처리 로직
                        FListViewItem s = new FListViewItem();
                        s = (FListViewItem) f_adapter.getItem(position);
                        String id = s.getId();

                        deleteFriend(position, id);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 취소시 처리 로직
                    }
                })
                .show();
        return true;
    }

    /* ---------------------------------------------- TAB1의 버튼 클릭 끝----------------------------------------------------------- */


    /* ---------------------------------------------- TAB2의 버튼 클릭 ----------------------------------------------------------- */
    @Override
    public void onListBtnClick2(View v, final int position) {
        //전화버튼 눌렀을때
        if (v.getId() == R.id.rphone) {
            RListViewItem friend = new RListViewItem();
            friend = (RListViewItem) r_adapter.getItem(position);
            String id = friend.getId();
            String fname =  friend.getName();
            startCall(id, userId, fname);
        } else {
            Intent intent = new Intent(this, TextPopupActivity.class);
            intent.putExtra("data", "Test Popup");
            startActivityForResult(intent, 1);

        }
    }

    public boolean onListBtnLongClick2(View v, final int position) {
        new AlertDialog.Builder(this)
                .setTitle("대화기록 삭제")
                .setMessage("삭제하시겠습니까?")
                .setIcon(R.drawable.deletefriend)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 확인시 처리 로직
                        RListViewItem s = new RListViewItem();
                        s = (RListViewItem) r_adapter.getItem(position);
                        String id = s.getId();
                        String chatcnt = s.getChatCnt();
                        String caller = s.getCaller();
                        String receiver = s.getReceiver();

                        deleteRecentCall(position, id, chatcnt, caller, receiver);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 취소시 처리 로직
                    }
                })
                .show();
        return true;
    }

    /* ---------------------------------------------- TAB2의 버튼 클릭 끝----------------------------------------------------------- */


    /* ---------------------------------------------- DB에서 친구데이터 가져오기 ----------------------------------------------------------- */
    private class friendData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(FTAG, "response  - " + result);

            if (result == null) {
            } else {
                fJsonString = result;
                fShowResult();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            try {
                //String link = "http://13.124.94.107/addFriend.php";
                String data = URLEncoder.encode("userId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");

                URL url = new URL(serverURL);
                URLConnection conn = url.openConnection();

                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write(data);
                wr.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                reader.close();
                return sb.toString().trim();

            } catch (Exception e) {
                Log.d(FTAG, "InsertData: Error ", e);
                errorString = e.toString();
                return null;
            }

        }
    }

    /* ---------------------------------------------- DB에서 친구데이터 가져오기 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 친구목록결과 LISTVIEW로 생성해서 보여주기 ----------------------------------------------------------- */
    private void fShowResult() {
        try {
            JSONObject jsonObject = new JSONObject(fJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(FTAG_JSON);

            //아이템 하나씩 만들어서 arraylist 형태로 저장
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                String friendname = item.getString(FTAG_NAME);
                String friendid = item.getString(FTAG_ID);

                HashMap<String, String> hashMap = new HashMap<>();

                hashMap.put(FTAG_NAME, friendname);
                hashMap.put(FTAG_ID, friendid);

                fArrayList.add(hashMap);
            }


            FListViewItem fitem;

            //fArrayList 있는 아이템들을 FListViewItem 형태로 저장해서 리스트뷰에 추가
            for (int i = 0; i < fArrayList.size(); i++) {
                HashMap<String, String> ffitem = fArrayList.get(i);
                fitem = new FListViewItem();
                fitem.setIcon(ContextCompat.getDrawable(this, R.drawable.icon));
                fitem.setName(ffitem.get(FTAG_NAME));
                fitem.setId(ffitem.get(FTAG_ID));
                f_list.add(fitem);
            }

            // Adapter 생성
            f_adapter = new FListViewAdapter(this, R.layout.f_listview_item, f_list, this, this);

            // 리스트뷰 참조 및 Adapter달기
            f_listview = (ListView) findViewById(R.id.friend_list_view);
            f_listview.setAdapter(f_adapter);

        } catch (JSONException e) {
            Log.d(FTAG, "fShowResult : ", e);
        }

    }

    /* ---------------------------------------------- 친구목록결과 LISTVIEW로 생성해서 보여주기 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- DB에서 최근통화데이터 가져오기 ----------------------------------------------------------- */
    private class recentCallData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(RTAG, "response  - " + result);

            if (result == null) {
            } else {
                rJsonString = result;
                rShowResult();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            try {
                //String link = "http://13.124.94.107/addFriend.php";
                String data = URLEncoder.encode("userId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");

                URL url = new URL(serverURL);
                URLConnection conn = url.openConnection();

                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write(data);
                wr.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                reader.close();
                return sb.toString().trim();

            } catch (Exception e) {
                Log.d(RTAG, "InsertData: Error ", e);
                errorString = e.toString();
                return null;
            }

        }
    }

    /* ---------------------------------------------- DB에서 최근통화데이터 가져오기 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 최근통화목록결과 LISTVIEW로 생성해서 보여주기 ----------------------------------------------------------- */
    private void rShowResult() {
        try {
            String friendname;
            String friendid;


            String caller, receiver;

            JSONObject jsonObject = new JSONObject(rJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(RTAG_JSON);

            //아이템 하나씩 만들어서 arraylist 형태로 저장
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                final HashMap<String, String> hashMap = new HashMap<>();

                if(item.getString(RTAG_ID).equals(userId)){  //사용자가 수신자
                    friendname = userName;
                    friendid = item.getString(RTAG_USERID);

                    hashMap.put(RTAG_CALLER, friendid);
                    hashMap.put(RTAG_RECEIVER, userId);

                    caller = friendid;
                    receiver = userId;
                }else{ //사용자가 발신자
                    friendname = item.getString(RTAG_NAME);
                    friendid = item.getString(RTAG_ID);

                    hashMap.put(RTAG_CALLER, userId);
                    hashMap.put(RTAG_RECEIVER, friendid);

                    caller = userId;
                    receiver = friendid;
                }

                String calldate = item.getString(RTAG_DATE);
                final String chatcnt = item.getString(RTAG_CHATCNT);

                String chatroom = caller+receiver;
                DatabaseReference checkRef = database.getReference("chats").child(chatroom).child(chatcnt);


/*                checkRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        long ss = dataSnapshot.getChildrenCount();
                        int vv = (int)ss;
                        //Log.d("zzzzzzzz",vv+"");
                        if(vv == 0){
                            //hashMap.put(RTAG_VISIBLE, "N");
                            //Log.d("zzzzzzzz",vv+"N");
                            //checkvis = "N";
                            changeVisibility("N");
                            //Log.d("gggggggg",checkvis);
                        }else{
                            //hashMap.put(RTAG_VISIBLE, "Y");
                            //Log.d("zzzzzzzz",vv+"Y");
                            //checkvis = "Y";
                            //Log.d("gggggggg",checkvis);
                            changeVisibility("Y");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });*/

                //Log.d("gggggggg",checkvis);

                hashMap.put(RTAG_NAME, friendname);
                hashMap.put(RTAG_ID, friendid);
                hashMap.put(RTAG_DATE, calldate);
                hashMap.put(RTAG_CHATCNT, chatcnt);

                rArrayList.add(hashMap);
            }


            RListViewItem ritem;

            //rArrayList 있는 아이템들을 RListViewItem 형태로 저장해서 리스트뷰에 추가
            for (int i = 0; i < rArrayList.size(); i++) {
                HashMap<String, String> rritem = rArrayList.get(i);
                ritem = new RListViewItem();
                if(rritem.get(RTAG_RECEIVER).equals(userId)){  //사용자가 수신자
                    ritem.setIcon(ContextCompat.getDrawable(this, R.drawable.receiver));
                }else{  //사용자가 발신자
                    ritem.setIcon(ContextCompat.getDrawable(this, R.drawable.caller));
                }
                //ritem.setIcon(ContextCompat.getDrawable(this, R.drawable.icon));
                ritem.setName(rritem.get(RTAG_NAME));
                ritem.setId(rritem.get(RTAG_ID));
                ritem.setDate(rritem.get(RTAG_DATE));
                ritem.setChatCnt(rritem.get(RTAG_CHATCNT));
                ritem.setCaller(rritem.get(RTAG_CALLER));
                ritem.setReceiver(rritem.get(RTAG_RECEIVER));
                r_list.add(ritem);
            }

            // Adapter 생성
            r_adapter = new RListViewAdapter(this, R.layout.r_listview_item, r_list, this, this);

            // 리스트뷰 참조 및 Adapter달기
            r_listview = (ListView) findViewById(R.id.recent_call_list_view);
            r_listview.setAdapter(r_adapter);

        } catch (JSONException e) {
            Log.d(RTAG, "rShowResult : ", e);
        }

    }

    /* ---------------------------------------------- 최근통화목록결과 LISTVIEW로 생성해서 보여주기 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 친구 추가 ----------------------------------------------------------- */
    public void addFriend(View v) {
        addId = (EditText) findViewById(R.id.addFriendId);
        String friendId = addId.getText().toString();

        insertToDatabase(userId, friendId);
    }

    /* ---------------------------------------------- 친구 추가 끝----------------------------------------------------------- */


    /* ---------------------------------------------- 친구 삭제 ----------------------------------------------------------- */
    public void deleteFriend(int pos, String id) {
        //String userPhone = intent.getStringExtra("userId");
        deleteFriendFromDatabase(pos, userId, id);
    }

    /* ---------------------------------------------- 친구 삭제 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- 최근대화기록 삭제 ----------------------------------------------------------- */
    public void deleteRecentCall(int pos, String id, String chatcnt, String caller, String receiver) {
        deleteRecentCallFromDatabase(pos, userId, id, chatcnt, caller, receiver);
    }

    /* ---------------------------------------------- 최근대화기록 삭제 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- DB에 친구 번호 추가 ----------------------------------------------------------- */
    private void insertToDatabase(final String userId, String friendId) {
        class InsertData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();

                addId.setText("");
                //addPhone.requestFocus();
            }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String userId = (String) params[0];
                    String friendId = (String) params[1];


                    String link = "http://13.124.94.107/addFriend.php";
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
                    return sb.toString();
                } catch (Exception e) {
                    return new String("Exception: " + e.getMessage());
                }
            }
        }
        InsertData task = new InsertData();
        task.execute(userId, friendId);
    }

    /* ---------------------------------------------- DB에 친구 번호 추가 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- DB에 친구 번호 삭제 ----------------------------------------------------------- */
    private void deleteFriendFromDatabase(final int pos, String userId, String friendId) {
        class DeleteFriendData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                //리스트에서 삭제후 새로고침
                f_list.remove(pos);
                f_adapter.notifyDataSetChanged();
            }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String userId = (String) params[0];
                    String friendId = (String) params[1];


                    String link = "http://13.124.94.107/deleteFriend.php";
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
                    return sb.toString();
                } catch (Exception e) {
                    return new String("Exception: " + e.getMessage());
                }
            }
        }
        DeleteFriendData task = new DeleteFriendData();
        task.execute(userId, friendId);
    }


    /* ---------------------------------------------- DB에 친구 번호 삭제 끝 ----------------------------------------------------------- */

    /* ---------------------------------------------- DB에 최근통화기록 삭제 ----------------------------------------------------------- */
    private void deleteRecentCallFromDatabase(final int pos, String userId, String friendId, final String chatCnt, String caller, String receiver) {
        class DeleteRecentCallData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                //리스트에서 삭제후 새로고침
                r_list.remove(pos);
                r_adapter.notifyDataSetChanged();
            }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String userId = (String) params[0];
                    String friendId = (String) params[1];


                    String link = "http://13.124.94.107/deleteRecentCall.php";
                    String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");
                    data += "&" + URLEncoder.encode("FriendId", "UTF-8") + "=" + URLEncoder.encode(friendId, "UTF-8");
                    data += "&" + URLEncoder.encode("ChatCnt", "UTF-8") + "=" + URLEncoder.encode(chatCnt, "UTF-8");


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
        DeleteRecentCallData task = new DeleteRecentCallData();
        task.execute(userId, friendId, chatCnt);
        String chatRoom = caller+receiver;
        database.getReference("chats").child(chatRoom).child(chatCnt).setValue(null);
    }


    /* ---------------------------------------------- DB에 최근통화기록 삭제 끝 ----------------------------------------------------------- */


    /* ---------------------------------------------- 전화걸기 ----------------------------------------------------------- */
    public void call(String friendId, String friendName) {
        //String friendPhone = friendphone;
        Intent intent = new Intent(MainActivity.this, CallActivity.class);
        //intent.putExtra("Tag", v.getTag().toString());
        intent.putExtra("userId", userId);
        intent.putExtra("friendId", friendId);
        intent.putExtra("friendName", friendName);

        //startActivity(intent);
        //stopService(cIntent);
        //unregisterReceiver(restartService);
        startActivityForResult(intent, 0);
    }

    /* ---------------------------------------------- 전화걸기 끝 ----------------------------------------------------------- */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 0) {

            Log.d("CallActivitt Finish", "finish call");
            //recreate();

            startService(cIntent);

            //RelativeLayout r = (RelativeLayout)findViewById(R.id.callReceiveLayout);
            //r.setVisibility(View.INVISIBLE);

            //Intent cIntent = new Intent(this, CallService.class);
            //startService(cIntent);
        }
    }

    private void startCall(final String connectId, String Id, final String fname) {

        class InsertData extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                if (s.toString().equals("Try")) {
                    call(connectId, fname);
                } else if (s.toString().equals("Calling")) {
                    Toast.makeText(getApplicationContext(), "상대방이 이미 통화중입니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "로그인된 사용자가 아닙니다", Toast.LENGTH_SHORT).show();
                }
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


    private void logout(final String Id) {

        class InsertData extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String Id = (String) params[0];

                    String link = "http://13.124.94.107/logout.php";
                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");

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
        task.execute(Id);
    }

}