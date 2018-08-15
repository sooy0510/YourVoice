package com.example.ds.yourvoice;

/**
 * Created by DS on 2018-04-13.
 */

public class Chat {

    public String user;
    public String friend;
    public String text;

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }

    public Chat(String uid, String author, String text) {
        this.user = uid;
        this.friend = author;
        this.text = text;
    }
}
