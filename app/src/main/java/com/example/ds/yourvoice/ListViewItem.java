package com.example.ds.yourvoice;

import android.graphics.drawable.Drawable;

/**
 * Created by DS on 2018-03-15.
 */

public class ListViewItem {
    private Drawable iconDrawable ;
    private String nameStr ;
    private String phoneStr ;
    private String textStr ;

    public void setIcon(Drawable icon) {
        iconDrawable = icon ;
    }
    public void setName(String name) {
        nameStr = name ;
    }
    public void setPhone(String phone) {
        phoneStr = phone ;
    }
    public void setText(String text) {
        textStr = text ;
    }

    public Drawable getIcon() {
        return this.iconDrawable ;
    }
    public String getName() { return this.nameStr ; }
    public String getPhone() {
        return this.phoneStr ;
    }
    public String getText() {
        return this.textStr ;
    }

}
