package com.github.m4tt30c91.spring.r2dbc.ace.model;

public class BookDataModel implements DataModel {

    private int id;
    private String bookTitle;
    private int authorId;

    @Override
    public String uniqueIdentifier() {
        return this.id + "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }
    
}
