package com.example.ds.yourvoice;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements ListViewAdapter.ListBtnClickListener{

    Toolbar myToolbar;
    EditText addPhone;
    Intent intent;
    //private ListView mListView;
    String userPhone;
    String userId;
    ArrayList<HashMap<String, String>> mArrayList;
    ListView mListView;
    String mJsonString;
    ListView listview ;
    ListViewAdapter adapter;
    ArrayList<ListViewItem> list = new ArrayList<ListViewItem>() ;

    private static String TAG = "FRIENDLIST";

    private static final String TAG_JSON="friendList";
    private static final String TAG_NAME = "friendname";
    private static final String TAG_PHONE ="friendphone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        intent = getIntent();
        userPhone = intent.getStringExtra("userPhone");
        userId = intent.getStringExtra("userId");


        // 추가된 소스, Toolbar를 생성한다.
        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true); //커스터마이징 하기 위해 필요
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true); // 뒤로가기 버튼, 디폴트로 true만 해도 백버튼이 생김

        TabHost tabHost1 = (TabHost) findViewById(android.R.id.tabhost);
        tabHost1.setup();
        //TabHost tabHost1 = getTabHost();

        TabHost.TabSpec ts1 = tabHost1.newTabSpec("Tab1");
        ts1.setContent(R.id.tab_friendList);
        ts1.setIndicator("친구목록");
        tabHost1.addTab(ts1);

        TabHost.TabSpec ts2 = tabHost1.newTabSpec("Tab2");
        ts2.setContent(R.id.tab_recentCall);
        ts2.setIndicator("최근통화기록");
        tabHost1.addTab(ts2);

        TabHost.TabSpec ts3 = tabHost1.newTabSpec("Tab3");
        //ts3.setContent(new Intent(MainActivity.this, AddFriendActivity.class));
        ts3.setContent(R.id.tab_addFriend);
       // ts3.setContent(new Intent(MainActivity.this, AddFriendActivity.class));
        ts3.setIndicator("친구추가");
        tabHost1.addTab(ts3);

        tabHost1.setCurrentTab(0);


        mArrayList = new ArrayList<>();
        friendData task = new friendData();
        task.execute("http://13.124.94.107/getFriendList.php");
        //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        //startActivity(intent);

        // 위에서 생성한 listview에 클릭 이벤트 핸들러 정의.
      /*  listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // get item
                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position) ;

                String titleStr = item.getTitle() ;
                String descStr = item.getDesc() ;
                Drawable iconDrawable = item.getIcon() ;

                // TODO : use item data.
            }
        }) ;*/

  /*      // 위에서 생성한 listview에 클릭 이벤트 핸들러 정의.
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // TODO : item click
            }
        }) ;*/


        tabHost1.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                // TODO Auto-generated method stub
                String strMsg;
                strMsg = tabId;
                //Toast.makeText(getApplicationContext(), strMsg, Toast.LENGTH_SHORT). show();

                if(strMsg.equals("Tab1")){
                    /* 기존에 있던 어댑터 삭제*/
                    adapter.clear();
                    /* 아이템 추가 및 어댑터 등록 */
                    mArrayList = new ArrayList<>();

                    friendData task = new friendData();
                    task.execute("http://13.124.94.107/getFriendList.php");
                }
            }
        });

    }

    @Override
    public void onListBtnClick(int position) {
        Toast.makeText(this, Integer.toString(position+1) + " Item is selected..", Toast.LENGTH_SHORT).show() ;
    }

    private class friendData extends AsyncTask<String, Void, String>{
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
            Log.d(TAG, "response  - " + result);

            if (result == null){
            }
            else {

                mJsonString = result;
                showResult();
            }
        }


        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            try {
                //String link = "http://13.124.94.107/addFriend.php";
                String data = URLEncoder.encode("userPhone", "UTF-8") + "=" + URLEncoder.encode(userPhone, "UTF-8");

                URL url = new URL(serverURL);
                URLConnection conn = url.openConnection();

                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write(data);
                wr.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = reader.readLine()) != null){
                    sb.append(line);
                }

                reader.close();
                return sb.toString().trim();

            } catch (Exception e) {
                Log.d(TAG, "InsertData: Error ", e);
                errorString = e.toString();
                return null;
            }

        }
    }

    private void showResult(){
        //Log.e("llllllllllllllllllll",mJsonString);
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for(int i=0;i<jsonArray.length();i++){

                JSONObject item = jsonArray.getJSONObject(i);

                String friendname = item.getString(TAG_NAME);
                String friendphone = item.getString(TAG_PHONE);

                HashMap<String,String> hashMap = new HashMap<>();

                hashMap.put(TAG_NAME, friendname);
                hashMap.put(TAG_PHONE, friendphone);

                mArrayList.add(hashMap);
            }

            //friendListAdapter mfriendListAdapter = new friendListAdapter();

            //리스트뷰 Adapter 생성
           // adapter = new ListViewAdapter();
            ListViewItem item ;

            for(int i=0; i<mArrayList.size(); i++){
                HashMap<String, String> ritem =  mArrayList.get(i);
                //adapter.addItem(ContextCompat.getDrawable(MainActivity.this,R.drawable.icon),item.get(TAG_NAME), item.get(TAG_PHONE));
                item = new ListViewItem();
                item.setIcon(ContextCompat.getDrawable(this, R.drawable.icon));
                item.setTitle(ritem.get(TAG_NAME));
                item.setDesc(ritem.get(TAG_PHONE));
                list.add(item);
            }

            // Adapter 생성
            adapter = new ListViewAdapter(this, R.layout.listview_item, list, this) ;

            // 리스트뷰 참조 및 Adapter달기
            listview = (ListView) findViewById(R.id.listview1);
            listview.setAdapter(adapter);

           /* ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, mArrayList, R.layout.listview_item,
                    new String[]{TAG_NAME, TAG_PHONE},
                    new int[]{R.id.friendName, R.id.friendPhone}
            );

            mListView.setAdapter(adapter);*/



            //adapter.addItem(ContextCompat.getDrawable(MainActivity.this,R.drawable.icon),mArrayList.get(0).get(), TAG_PHONE);

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }


    public void addFriend(View v){
        addPhone = (EditText)findViewById(R.id.addFriendPhone);
        String friendPhone = addPhone.getText().toString();

        //LoginActivity loginActivity = (LoginActivity) getApplication();

        String userId = intent.getStringExtra("userId");
        //Log.e("aaaaaaaaaaaaaaaaaaaa",userId);

        insertToDatabase(userId, friendPhone);

        //friendPhone.setText("");
        //friendPhone.requestFocus();
    }


    /* 친구 번호 추가 */
    private void insertToDatabase(final String userId, String friendPhone) {
        class InsertData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }

            @Override
            protected void  onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG). show();

                addPhone.setText("");
                //addPhone.requestFocus();
            }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String userId = (String) params[0];
                    String friendPhone = (String) params[1];


                    String link = "http://13.124.94.107/addFriend.php";
                    String data = URLEncoder.encode("UserId", "UTF-8") + "=" + URLEncoder.encode(userId, "UTF-8");
                    data += "&" + URLEncoder.encode("FriendPhone", "UTF-8") + "=" + URLEncoder.encode(friendPhone, "UTF-8");


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
        task.execute(userId, friendPhone);
    }

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
            case R.id.action_settings2:
                // User chose the "Settings" item, show the app settings UI...
                Toast.makeText(getApplicationContext(), "환경설정 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                Toast.makeText(getApplicationContext(), "나머지 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return super.onOptionsItemSelected(item);
        }
    }

    public void call (View v) {
        Intent intent = new Intent(MainActivity.this, CallActivity.class);
        intent.putExtra("userId", "userId");
        startActivity(intent);
    }
}
