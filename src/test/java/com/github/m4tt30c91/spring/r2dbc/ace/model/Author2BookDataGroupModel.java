package com.github.m4tt30c91.spring.r2dbc.ace.model;

import java.util.List;

public class Author2BookDataGroupModel implements DataGroupModel<AuthorDataModel, BookDataModel> {

    @Override
    public Class<AuthorDataModel> base() {
        return AuthorDataModel.class;
    }

    @Override
    public Class<BookDataModel> collectable() {
        return BookDataModel.class;
    }

    @Override
    public List<BookDataModel> getCollectables(AuthorDataModel base) {
        return base.getBooks();
    }

    @Override
    public void setCollectables(AuthorDataModel base, List<BookDataModel> collectables) {
        base.setBooks(collectables);
    }
    
}
