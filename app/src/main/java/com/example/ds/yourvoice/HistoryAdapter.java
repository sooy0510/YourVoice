package com.example.ds.yourvoice;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DS on 2018-07-11.
 */

public class HistoryAdapter extends BaseAdapter{
        public class ListContents{
            String msg;
            int type;
            ListContents(String _msg,int _type)
            {
                this.msg = _msg;
                this.type = _type;
            }
        }

        private List<com.example.ds.yourvoice.HistoryAdapter.ListContents> h_List = new ArrayList<com.example.ds.yourvoice.HistoryAdapter.ListContents>();
        public HistoryAdapter() {
            h_List = new ArrayList();
        }
        // 외부에서 아이템 추가 요청 시 사용
        public void add(String _msg,int _type) {

            h_List.add(new com.example.ds.yourvoice.HistoryAdapter.ListContents(_msg,_type));
        }

        // 외부에서 리스트 삭제 요청 시 사용
        public void clean() {

            h_List.clear();
        }

        // 외부에서 아이템 삭제 요청 시 사용
        public void remove(int _position) {
            h_List.remove(_position);
        }

        @Override
        public int getCount() {
            return h_List.size();
        }

        @Override
        public Object getItem(int position) {
            return h_List.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int pos = position;
            final Context context = parent.getContext();

            TextView text    = null;
            com.example.ds.yourvoice.HistoryAdapter.CustomHolder holder  = null;
            LinearLayout layout  = null;
            View            viewRight = null;
            View            viewLeft = null;



            // 리스트가 길어지면서 현재 화면에 보이지 않는 아이템은 converView가 null인 상태로 들어 옴
            if ( convertView == null ) {
                // view가 null일 경우 커스텀 레이아웃을 얻어 옴
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.history_chatitem, parent, false);

                layout    = (LinearLayout) convertView.findViewById(R.id.layout);
                text    = (TextView) convertView.findViewById(R.id.text);
                viewRight    = (View) convertView.findViewById(R.id.imageViewright);
                viewLeft    = (View) convertView.findViewById(R.id.imageViewleft);


                // 홀더 생성 및 Tag로 등록
                holder = new com.example.ds.yourvoice.HistoryAdapter.CustomHolder();
                holder.m_TextView   = text;
                holder.layout = layout;
                holder.viewRight = viewRight;
                holder.viewLeft = viewLeft;
                convertView.setTag(holder);
            }
            else {
                holder  = (com.example.ds.yourvoice.HistoryAdapter.CustomHolder) convertView.getTag();
                text    = holder.m_TextView;
                layout  = holder.layout;
                viewRight = holder.viewRight;
                viewLeft = holder.viewLeft;
            }

            String chatText = h_List.get(position).msg;

            Paint paint = text.getPaint();
            int frameWidth = 800;
            int startIndex = 0;
            int endIndex = paint.breakText(chatText, true, frameWidth, null);
            String save = chatText.substring(startIndex, endIndex);

            int lines = 1;

            while(true){
                startIndex = endIndex;
                chatText = chatText.substring(startIndex);

                if(chatText.length() == 0)break;
                else lines++;

                endIndex = paint.breakText(chatText, true, frameWidth, null);
                save += "\n"+chatText.substring(0, endIndex);
            }



            // Text 등록
            text.setText(save);

            if(h_List.get(position).type == 0 ) {
                text.setBackgroundResource(R.drawable.chat_left);
                layout.setGravity(Gravity.LEFT);
                viewRight.setVisibility(View.GONE);
                viewLeft.setVisibility(View.GONE);
            }else if(h_List.get(position).type == 1){
                text.setBackgroundResource(R.drawable.chat_right);
                layout.setGravity(Gravity.RIGHT);
                viewRight.setVisibility(View.GONE);
                viewLeft.setVisibility(View.GONE);
            }else if(h_List.get(position).type == 2){
                layout.setGravity(Gravity.CENTER);
                viewRight.setVisibility(View.GONE);
                viewLeft.setVisibility(View.GONE);
            }


            // 리스트 아이템을 터치 했을 때 이벤트 발생
            convertView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // 터치 시 해당 아이템 이름 출력
                    //Toast.makeText(context, "리스트 클릭 : "+h_List.get(pos), Toast.LENGTH_SHORT).show();
                }
            });

            return convertView;
        }

        private class CustomHolder {
            TextView    m_TextView;
            LinearLayout    layout;
            View viewRight;
            View viewLeft;
        }
    }



