package com.example.ds.yourvoice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

/**
 * Created by DS on 2018-07-03.
 */



public class TextPopupActivity extends Activity {

    /* Firebase */
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    FirebaseDatabase database;

    HistoryAdapter h_Adapter;
    ListView h_ListView;

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.text_popup);

        iv = findViewById(R.id.call_icon);

        //UI 객체생성
        final TextView hfriend = (TextView)findViewById(R.id.hfriend);
        final TextView hdate = (TextView)findViewById(R.id.hdate);

        h_Adapter = new HistoryAdapter();

        // Xml에서 추가한 ListView 연결
        h_ListView = (ListView) findViewById(R.id.txtText);

        // ListView에 어댑터 연결
        h_ListView.setAdapter(h_Adapter);



        //데이터 가져오기
        Intent intent = getIntent();
        final String chatroom = intent.getStringExtra("chatRoom");
        final String chatcnt = intent.getStringExtra("chatcnt");
        final String caller = intent.getStringExtra("caller");
        final String receiver = intent.getStringExtra("receiver");
        final String userId = intent.getStringExtra("userId");
        final String date = intent.getStringExtra("date");
        final String fname = intent.getStringExtra("fname");
        //txtText.setText(data);

        DatabaseReference databaseReference = firebaseDatabase.getReference("chats").child(chatroom).child(chatcnt);

        databaseReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // 데이터를 읽어올 때 모든 데이터를 읽어오기때문에 List 를 초기화해주는 작업이 필요하다.
                h_Adapter.clean();
                String nstring = date;
                String tranf="";
                //DateFormat date1 = new SimpleDateFormat("yyyyMMddhhmmss");
                try {
                    //Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
                    //nstring = new SimpleDateFormat("yyyy년 MM월 dd일").format(date1);
                    DateFormat date1 = new SimpleDateFormat("yyyyMMddHHmmss");
                    Date date2 = date1.parse(nstring);
                    SimpleDateFormat transFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss");
                    tranf = transFormat.format(date2);
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                if (userId.equals(caller)) { //사용자 = 발신자
                    hfriend.setText(fname);
                    iv.setImageDrawable(getResources().getDrawable(R.drawable.baseline_arrow_forward_green_18));
                } else { //사용자 = 수신자
                    hfriend.setText(fname);
                    iv.setImageDrawable(getResources().getDrawable(R.drawable.baseline_arrow_back_red_18));
                }

                hdate.setText(tranf);

                if(dataSnapshot.getChildrenCount() == 0){
                    h_Adapter.add("자막내역이 없음", 2);
                }
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    //String msg = messageData.getValue().toString();
                    Chat chat = messageData.getValue(Chat.class);
                    if (userId.equals(caller)) { //사용자 = 발신자
                        //hfriend.setText("수신자  "+receiver);
                        if (userId.equals(chat.user)) { //사용자 = 채팅의 user
                            h_Adapter.add(chat.text, 1);
                        } else {
                            h_Adapter.add(chat.text, 0);
                        }
                    } else { //사용자 = 수신자
                        //hfriend.setText("발신자  "+caller);
                        if (receiver.equals(chat.user)) { //사용자 = 채팅의 user
                            h_Adapter.add(chat.text, 1);
                        } else {
                            h_Adapter.add(chat.text, 0);
                        }
                    }
                }
                // notifyDataSetChanged를 안해주면 ListView 갱신이 안됨
                h_Adapter.notifyDataSetChanged();
                // ListView 의 위치를 마지막으로 보내주기 위함
                //h_ListView.setSelection(h_Adapter.getCount() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //확인 버튼 클릭
    public void mOnClose(View v){
        //데이터 전달하기
        Intent intent = new Intent();
        intent.putExtra("result", "Close Popup");
        setResult(RESULT_OK, intent);

        //액티비티(팝업) 닫기
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }
}
