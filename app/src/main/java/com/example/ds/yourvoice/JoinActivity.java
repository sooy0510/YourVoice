package com.example.ds.yourvoice;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

/**
 * Created by DS on 2018-02-19.
 */

public class JoinActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText editTextId;
    private EditText editTextPw;
    private EditText editTextName;
    private EditText editTextPhone;
    private String flagId = "idnokay"; //중복체크안한걸로 초기화
    private String flagPhone = "phonenokay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_keyboard_arrow_left_orange_24);

        editTextId = (EditText)findViewById(R.id.newID);
        editTextPw = (EditText)findViewById(R.id.newPW);
        editTextName = (EditText)findViewById(R.id.newName);
        editTextPhone = (EditText)findViewById(R.id.newPhone);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:{ //toolbar의 back키 눌렀을 때 동작
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // custom toast 만들기
    public void makeToast(String s, Drawable d) {
        View view = View.inflate(JoinActivity.this, R.layout.custom_toast, null);
        ImageView iv = view.findViewById(R.id.toast_image);
        iv.setImageDrawable(d);
        TextView tv = view.findViewById(R.id.toast_text);
        tv.setText(s);
        Toast toast = new Toast(JoinActivity.this);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 210);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }


    /* 아이디 중복체크버튼 클릭 */
    public void checkID(View v){
        String Id = editTextId.getText().toString();
        if(Id == null || Id.equals("") == true){
            makeToast("아이디를 입력해주세요", getResources().getDrawable(R.drawable.baseline_clear_white_24));
        }else{
            isExistID(Id);
        }
    }

    /* 아이디 중복체크 */
    private boolean isExistID(String Id){
        class checkIdData extends AsyncTask<String, Void, String>{

            @Override
            protected void  onPostExecute(String s) {
                super.onPostExecute(s);

                try {
                    // PHP에서 받아온 JSON 데이터를 JSON오브젝트로 변환
                    JSONObject jObject = new JSONObject(s);
                    // results라는 key는 JSON배열로 되어있다.
                    JSONArray results = jObject.getJSONArray("result");
                    String idmsg = "";


                    for ( int i = 0; i < results.length(); ++i ) {
                        JSONObject temp = results.getJSONObject(i);
                        idmsg =temp.get("idmsg").toString();
                        flagId = temp.get("idflag").toString();

                    }
                    //Log.e("ssssssssssssssss",flagId.toString());
                    if(flagId.equals("idokay"))
                        makeToast(idmsg, getResources().getDrawable(R.drawable.baseline_check_white_24));
                    else
                        makeToast(idmsg, getResources().getDrawable(R.drawable.baseline_clear_white_24));
                    //Toast.makeText(getApplicationContext(), idmsg, Toast.LENGTH_SHORT). show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(String... params){
                try{
                    String Id = (String) params[0];
                    String CheckId = "ID";
                    String link = "http://13.124.94.107/checkJoinCondition.php";
                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
                    data += "&" + URLEncoder.encode("Check", "UTF-8") + "=" + URLEncoder.encode(CheckId, "UTF-8");
                    data += "&" + URLEncoder.encode("Phone", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8");

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
                        sb.append(line + "\n");
                        break;
                    }
                    return sb.toString();
                }catch (Exception e) {
                    return new String("Exception: " + e.getMessage());
                }
            }
        }
        checkIdData task = new checkIdData();
        task.execute(Id);

        return true;
    }

    /* 번호 중복체크버튼 클릭 */
    public void checkPhone(View v){
        String Phone = editTextPhone.getText().toString();
        if(Phone == null || Phone.equals("") == true){
            makeToast("전화번호를 입력해주세요", getResources().getDrawable(R.drawable.baseline_clear_white_24));
        }else{
            isExistPhone(Phone);
        }
    }

    /* 번호 중복체크 */
    private void isExistPhone(String phone){
        class checkPhoneData extends AsyncTask<String, Void, String>{

            @Override
            protected void  onPostExecute(String s) {
                super.onPostExecute(s);

                try {
                    // PHP에서 받아온 JSON 데이터를 JSON오브젝트로 변환
                    JSONObject jObject = new JSONObject(s);
                    // results라는 key는 JSON배열로 되어있다.
                    JSONArray results = jObject.getJSONArray("result");
                    String phonemsg = "";


                    for ( int i = 0; i < results.length(); ++i ) {
                        JSONObject temp = results.getJSONObject(i);
                        phonemsg =temp.get("phonemsg").toString();
                        flagPhone = temp.get("phoneflag").toString();

                    }
                    //Log.e("ssssssssssssssss",flagPhone.toString());
                    if(flagPhone.equals("phoneokay"))
                        makeToast(phonemsg, getResources().getDrawable(R.drawable.baseline_check_white_24));
                    else
                        makeToast(phonemsg, getResources().getDrawable(R.drawable.baseline_clear_white_24));
                    //Toast.makeText(getApplicationContext(), phonemsg, Toast.LENGTH_SHORT). show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(String... params){
                try{
                    String phone = (String) params[0];
                    String CheckPhone = "PHONE";
                    String link = "http://13.124.94.107/checkJoinCondition.php";
                    String data = URLEncoder.encode("Phone", "UTF-8") + "=" + URLEncoder.encode(phone, "UTF-8");
                    data += "&" + URLEncoder.encode("Check", "UTF-8") + "=" + URLEncoder.encode(CheckPhone, "UTF-8");
                    data += "&" + URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8");

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
                        sb.append(line + "\n");
                        break;
                    }
                    return sb.toString();
                }catch (Exception e) {
                    return new String("Exception: " + e.getMessage());
                }
            }
        }
        checkPhoneData task = new checkPhoneData();
        task.execute(phone);
    }


    public void insert(View v) {

        String Id = editTextId.getText().toString();
        String Pw = editTextPw.getText().toString();
        String Name = editTextName.getText().toString();
        String Phone = editTextPhone.getText().toString();

        /*if(Id != null || !Id.equals(""))
            Toast.makeText(getApplicationContext(), "아이디를 입력해주세요", Toast.LENGTH_SHORT). show();
        if(Pw != null || !Pw.equals(""))
            Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요", Toast.LENGTH_SHORT). show();
        if(Name != null || !Name.equals(""))
            Toast.makeText(getApplicationContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT). show();
        if(Phone != null || !Phone.equals(""))
            Toast.makeText(getApplicationContext(), "휴대폰번호를 입력해주세요", Toast.LENGTH_SHORT). show();*/

        if(Id != null && !Id.equals("") && Pw != null && !Pw.equals("") && Name != null && !Name.equals("") && Phone != null && !Phone.equals("")){
            if(flagId.equals("idokay") && flagPhone.equals("phoneokay")){
                insertToDatabase(Id, Pw, Name, Phone);
                startActivity(new Intent(JoinActivity.this, LoginActivity.class));
            }else{
                makeToast("중복체크버튼을 눌러주세요", getResources().getDrawable(R.drawable.baseline_clear_white_24));
                //Toast.makeText(getApplicationContext(), "아이디와 번호를 확인 후 중복체크버튼을 눌러주세요", Toast.LENGTH_SHORT). show();
            }
        }else makeToast("모든 항목을 입력해주세요", getResources().getDrawable(R.drawable.baseline_clear_white_24));
            //Toast.makeText(getApplicationContext(), "모든 항목을 입력해주세요", Toast.LENGTH_SHORT). show();


    }



    private void insertToDatabase(String Id, String Pw, String Name, String Phone) {
        class InsertData extends AsyncTask<String, Void, String> {
        //ProgressDialog loading;
        @Override
            protected void onPreExecute() {
            super.onPreExecute();
            //loading = ProgressDialog.show(JoinActivity.this, "Please Wait", null, true, true);
        }

        @Override
            protected void  onPostExecute(String s) {
            super.onPostExecute(s);
            //loading.dismiss();
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT). show();
        }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String Id = (String) params[0];
                    String Pw = (String) params[1];
                    String Name = (String) params[2];
                    String Phone = (String) params[3];


                    String link = "http://13.124.94.107/join.php";
                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
                    data += "&" + URLEncoder.encode("Pw", "UTF-8") + "=" + URLEncoder.encode(Pw, "UTF-8");
                    data += "&" + URLEncoder.encode("Name", "UTF-8") + "=" + URLEncoder.encode(Name, "UTF-8");
                    data += "&" + URLEncoder.encode("Phone", "UTF-8") + "=" + URLEncoder.encode(Phone, "UTF-8");


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
        task.execute(Id, Pw, Name, Phone);
    }

}
