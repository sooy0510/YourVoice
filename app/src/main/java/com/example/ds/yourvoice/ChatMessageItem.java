package com.example.ds.yourvoice;

/**
 * Created by DS on 2018-04-09.
 */

public class ChatMessageItem {

    private String content;
    private boolean isMine;

    public ChatMessageItem(String content, boolean isMine) {
        this.content = content;
        this.isMine = isMine;
    }

    public String getContent() {
        return content;
    }

    public boolean isMine() {
        return isMine;
    }
}
