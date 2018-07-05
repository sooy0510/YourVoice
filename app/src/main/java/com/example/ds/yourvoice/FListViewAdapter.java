package com.example.ds.yourvoice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by DS on 2018-03-15.
 */

public class FListViewAdapter extends ArrayAdapter implements View.OnClickListener, View.OnLongClickListener {

    // 버튼 클릭 이벤트를 위한 Listener 인터페이스 정의.
    public interface ListBtnClickListener {
        void onListBtnClick1(View v, int position) ;
    }

    public interface ListBtnLongClickListener {
        boolean onListBtnLongClick1(View v, int position) ;
    }

    // 생성자로부터 전달된 resource id 값을 저장.
    int resourceId ;
    // 생성자로부터 전달된 ListBtnClickListener  저장.
    private ListBtnClickListener listBtnClickListener ;
    private ListBtnLongClickListener listBtnLongClickListener ;

    // ListViewBtnAdapter 생성자. 마지막에 ListBtnClickListener 추가.
    FListViewAdapter(Context context, int resource, ArrayList<FListViewItem> list, ListBtnClickListener clickListener, ListBtnLongClickListener longClickListener) {
        super(context, resource, list) ;

        // resource id 값 복사. (super로 전달된 resource를 참조할 방법이 없음.)
        this.resourceId = resource ;

        this.listBtnClickListener = clickListener ;

        this.listBtnLongClickListener = longClickListener ;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position ;
        final Context context = parent.getContext();

        // 생성자로부터 저장된 resourceId(listview_btn_item)에 해당하는 Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(this.resourceId/*R.layout.listview_btn_item*/, parent, false);
        }

        // 화면에 표시될 View(Layout이 inflate된)로부터 위젯에 대한 참조 획득
        final ImageView iconImageView = (ImageView) convertView.findViewById(R.id.imageView1);
        final TextView textTextView1 = (TextView) convertView.findViewById(R.id.textView1);
        final TextView textTextView2 = (TextView) convertView.findViewById(R.id.textView2);

        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        final FListViewItem listViewItem = (FListViewItem) getItem(position);
        

        // 아이템 내 각 위젯에 데이터 반영
        iconImageView.setImageDrawable(listViewItem.getIcon());
        textTextView1.setText(listViewItem.getName());
        textTextView2.setText(listViewItem.getId());

        // button1의 TAG에 position값 지정. Adapter를 click listener로 지정.
        Button fphone = (Button) convertView.findViewById(R.id.fphone);
        fphone.setTag(position);
        fphone.setOnClickListener(this);

        // button2의 TAG에 position값 지정. Adapter를 click listener로 지정.
        /*Button button2 = (Button) convertView.findViewById(R.id.button2);
        button2.setTag(position);
        button2.setOnClickListener(this);*/


        //길게 누르면 삭제
        LinearLayout fitem = (LinearLayout) convertView.findViewById(R.id.fitem);
        fitem.setTag(position);
        fitem.setOnLongClickListener(this);

        return convertView;
    }


    // button2가 눌려졌을 때 실행되는 onClick함수.
    public void onClick(View v) {
        if (this.listBtnClickListener != null) {
            this.listBtnClickListener.onListBtnClick1(v, (int)v.getTag()) ;
        }
    }


    public boolean onLongClick(View v) {
        if (this.listBtnLongClickListener != null) {
            boolean b = this.listBtnLongClickListener.onListBtnLongClick1(v, (int) v.getTag());
        }
        return true;
    }



}