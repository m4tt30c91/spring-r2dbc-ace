package com.github.m4tt30c91.spring.r2dbc.ace.mapper;

import java.util.Collections;
import java.util.List;

import com.github.m4tt30c91.spring.r2dbc.ace.model.AuthorDataModel;
import com.github.m4tt30c91.spring.r2dbc.ace.model.DataGroupModel;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

public class AuthorModelMapper implements ModelMapper<AuthorDataModel> {

    @Override
    public AuthorDataModel map(Row row, RowMetadata rowMetadata) {
        Integer id = row.get("authorId", Integer.class);
        if (id == null) {
            return null;
        }
        AuthorDataModel authorDataModel = new AuthorDataModel();
        authorDataModel.setId(row.get("authorId", Integer.class));
        authorDataModel.setFirstName((String) row.get("firstName", String.class));
        authorDataModel.setLastName(row.get("lastName", String.class));
        return authorDataModel;
    }

    @Override
    public List<DataGroupModel> getDataGroupModels() {
        return Collections.emptyList();
    }
}
