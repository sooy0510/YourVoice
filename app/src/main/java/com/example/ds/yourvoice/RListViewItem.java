package com.example.ds.yourvoice;

import android.graphics.drawable.Drawable;

/**
 * Created by DS on 2018-06-28.
 */

public class RListViewItem {
    private Drawable iconDrawable ;
    private String nameStr ;
    private String idStr ;
    private String dateStr ;
    private String textStr ;
    private String chatCnt ;
    private String callerStr ;
    private String receiverStr ;
    private String checkVisibility ;


    public void setIcon(Drawable icon) {
        iconDrawable = icon ;
    }
    public void setName(String name) {
        nameStr = name ;
    }
    public void setId(String phone) {
        idStr = phone ;
    }
    public void setDate(String date) {
        dateStr = date ;
    }
    public void setText(String text) {  textStr = text ; }
    public void setChatCnt(String chatcnt) {
        chatCnt = chatcnt ;
    }
    public void setCaller(String caller) {
        callerStr = caller ;
    }
    public void setReceiver(String receiver) { receiverStr = receiver ; }

    public Drawable getIcon() {
        return this.iconDrawable ;
    }
    public String getName() { return this.nameStr ; }
    public String getId() {
        return this.idStr ;
    }
    public String getDate() { return this.dateStr ; }
    public String getText() {
        return this.textStr ;
    }
    public String getChatCnt() {
        return this.chatCnt ;
    }
    public String getCaller() { return this.callerStr ; }
    public String getReceiver() { return this.receiverStr ;}

}