package com.example.ds.yourvoice;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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

    private EditText editTextId;
    private EditText editTextPw;
    private EditText editTextName;
    private EditText editTextPhone;
    private String flagId;
    private String flagPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join);

        editTextId = (EditText)findViewById(R.id.newID);
        editTextPw = (EditText)findViewById(R.id.newPW);
        editTextName = (EditText)findViewById(R.id.newName);
        editTextPhone = (EditText)findViewById(R.id.newPhone);
    }

    /* 아이디 중복체크버튼 클릭 */
    public void checkID(View v){
        String Id = editTextId.getText().toString();
        if(Id == null || Id.equals("") == true){
            Toast.makeText(getApplicationContext(),"ID를 입력해주세요",Toast.LENGTH_SHORT)
                    .show();
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
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG). show();
            }

            @Override
            protected String doInBackground(String... params){
                try{
                    String Id = (String) params[0];
                    String CheckId = "ID";
                    String link = "http://203.252.219.238/checkJoinCondition.php";
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
                        sb.append(line);
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
            Toast.makeText(getApplicationContext(),"휴대폰 번호를 입력해주세요",Toast.LENGTH_SHORT)
                    .show();
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
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG). show();
            }

            @Override
            protected String doInBackground(String... params){
                try{
                    String phone = (String) params[0];
                    String CheckPhone = "PHONE";
                    String link = "http://203.252.219.238/checkJoinCondition.php";
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
                        sb.append(line);
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

        Toast.makeText(getApplicationContext(), "성고옹", Toast.LENGTH_LONG). show();

        //insertToDatabase(Id, Pw, Name, Phone);
    }



    private void insertToDatabase(String Id, String Pw, String Name, String Phone) {
        class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog loading;
        @Override
            protected void onPreExecute() {
            super.onPreExecute();
            loading = ProgressDialog.show(JoinActivity.this, "Please Wait", null, true, true);
        }

        @Override
            protected void  onPostExecute(String s) {
            super.onPostExecute(s);
            loading.dismiss();
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG). show();
        }

            @Override
            protected String doInBackground(String... params) {

                try {
                    String Id = (String) params[0];
                    String Pw = (String) params[1];
                    String Name = (String) params[2];
                    String Phone = (String) params[3];


                    String link = "http://203.252.219.238/join.php";
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
