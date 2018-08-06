package com.example.ds.yourvoice;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
        setContentView(R.layout.logout_dialog);

        Button logout = findViewById(R.id.dialog_logout);
        logout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((MainActivity)context).dialogLogout();
                dismiss();
            }
        });

        Button cancle = findViewById(R.id.dialog_cancle);
        cancle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });
    }
}

//http://mixup.tistory.com/36