package com.example.ds.yourvoice;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by DS on 2018-08-06.
 */

public class CustomDialog extends Dialog {
    private Context context;

    public CustomDialog(Context context){
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_dialog);

        Button pos = findViewById(R.id.dialog_pos);

        if (((MainActivity) context).dialog_state.equals("logout")) {
            pos.setText("로그아웃");
        }
        else if(((MainActivity) context).dialog_state.equals("deletef")) {
            pos.setText("친구삭제");
        }
        else if(((MainActivity) context).dialog_state.equals("deleteChat")) {
            pos.setText("삭제");
        }

        pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MainActivity) context).dialog_state.equals("logout")) {
                    ((MainActivity) context).dialogLogout();
                    dismiss();
                }
                else if(((MainActivity) context).dialog_state.equals("deletef")) {
                    ((MainActivity) context).dialogDeleteFriend();
                    dismiss();
                }
                else if(((MainActivity) context).dialog_state.equals("deleteChat")) {
                    ((MainActivity) context).dialogDeleteChat();
                    dismiss();
                }
            }
        });

        Button cancle = findViewById(R.id.dialog_cancle);
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}

//http://mixup.tistory.com/36