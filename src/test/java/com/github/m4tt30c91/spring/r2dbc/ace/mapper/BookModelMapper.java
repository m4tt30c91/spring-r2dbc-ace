package com.github.m4tt30c91.spring.r2dbc.ace.mapper;

import java.util.Collections;
import java.util.List;

import com.github.m4tt30c91.spring.r2dbc.ace.model.Author2BookDataGroupModel;
import com.github.m4tt30c91.spring.r2dbc.ace.model.BookDataModel;
import com.github.m4tt30c91.spring.r2dbc.ace.model.DataGroupModel;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

public class BookModelMapper implements ModelMapper<BookDataModel> {
    
    @Override
    public BookDataModel map(Row row, RowMetadata rowMetadata) {
        BookDataModel bookDataModel = new BookDataModel();
        bookDataModel.setId((Integer) row.get("bookId"));
        bookDataModel.setBookTitle(row.get("bookTitle", String.class));
        bookDataModel.setAuthorId((Integer) row.get("bookAuthorId"));
        return bookDataModel;
    }

    @Override
    public List<DataGroupModel> getDataGroupModels() {
        return Collections.singletonList(new Author2BookDataGroupModel());
    }
}