package com.github.m4tt30c91.spring.r2dbc.ace.model;

import java.util.List;

public class AuthorDataModel implements DataModel {

    private int id;
    private String firstName;
    private String lastName;
    private List<BookDataModel> books;

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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<BookDataModel> getBooks() {
        return books;
    }

    public void setBooks(List<BookDataModel> books) {
        this.books = books;
    }
}
