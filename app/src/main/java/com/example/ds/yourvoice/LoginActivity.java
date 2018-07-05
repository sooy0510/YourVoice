package com.example.ds.yourvoice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.StrictMode;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;


public class LoginActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    //private String userId;

    //자동로그인
    private SharedPreferences loginData;
    private String autoId, autoPw, autoPhone, autoName;

    // 첫로그인
    private String inputId, inputPw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        loginData = getSharedPreferences("auto", Activity.MODE_PRIVATE);
        autoId = loginData.getString("autoId", null);
        autoPw = loginData.getString("autoPw", null);
        autoPhone = loginData.getString("autoPhone", null);
        autoName = loginData.getString("autoName", null);

        if(autoId !=null && autoPw != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("userId", autoId);
            intent.putExtra("userPhone", autoPhone);
            intent.putExtra("userName", autoName);
            startActivity(intent);
            finish();
        }
    }
//
//    public String getUserId(){
//        return userId;
//    }
//
//    public void setUserId(String userId) {
//        this.userId = userId;
//    }

    public void login(View v) {

        EditText editTextId = (EditText)findViewById(R.id.userID);
        EditText editTextPw = (EditText)findViewById(R.id.userPW);
        inputId = editTextId.getText().toString();
        inputPw = editTextPw.getText().toString();

        loginCheck(inputId, inputPw);
    }

    private void loginCheck(final String Id, String Pw){
        class InsertData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(LoginActivity.this, "login...", null, true, true);
                //Toast.makeText(getApplicationContext(), "외않되", Toast.LENGTH_SHORT). show();
            }

            @Override
            protected void  onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

                String usermsg = "";
                String userphone = "";
                String username = "";

                try {
                    // PHP에서 받아온 JSON 데이터를 JSON오브젝트로 변환
                    JSONObject jObject = new JSONObject(s);
                    // results라는 key는 JSON배열로 되어있다.
                    JSONArray results = jObject.getJSONArray("result");


                    for ( int i = 0; i < results.length(); ++i ) {
                        JSONObject temp = results.getJSONObject(i);
                        usermsg =temp.get("usermsg").toString();
                        userphone = temp.get("userphone").toString();
                        username = temp.get("username").toString();

                    }
                    //Log.e("ssssssssssssssss",flagId.toString());
                    Toast.makeText(getApplicationContext(), usermsg, Toast.LENGTH_SHORT). show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if(usermsg.equals("User Found")) {

                    SharedPreferences.Editor autoLogin = loginData.edit();
                    autoLogin.putString("autoId", inputId);
                    autoLogin.putString("autoPw", inputPw);
                    autoLogin.putString("autoPhone", userphone);
                    autoLogin.putString("autoName", username);
                    //commit 으로 값 저장
                    autoLogin.commit();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("userId", Id);
                    intent.putExtra("userPhone", userphone);
                    intent.putExtra("userName", username);
                    startActivity(intent);
                    finish();
                }

                else
                    Toast.makeText(getApplicationContext(), "아이디와 비밀번호를 확인해주세요", Toast.LENGTH_SHORT). show();
            }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String Id = (String) params[0];
                    String Pw = (String) params[1];

                    String link = "http://13.124.94.107/login.php";
                    String data = URLEncoder.encode("Id", "UTF-8") + "=" + URLEncoder.encode(Id, "UTF-8");
                    data += "&" + URLEncoder.encode("Pw", "UTF-8") + "=" + URLEncoder.encode(Pw, "UTF-8");

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
                } catch (Exception e) {
                    return new String("Exception: " + e.getMessage());
                }
            }
        }
        InsertData task = new InsertData();
        task.execute(Id, Pw);
    }

    public void join(View v) {
        Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
        startActivity(intent);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
