package com.example.ds.yourvoice;

/**
 * Created by DS on 2018-04-13.
 */

public class Chat {

    public String user;
    public String friend;
    public String text;
    public ImageDTO image;
    //public String imageUri;

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }

    public Chat(String uid, String author, String text, ImageDTO image) {
        this.user = uid;
        this.friend = author;
        this.text = text;
        this.image = image;
    }
}
