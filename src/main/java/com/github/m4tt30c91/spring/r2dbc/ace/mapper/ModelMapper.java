package com.github.m4tt30c91.spring.r2dbc.ace.mapper;

import java.util.List;
import com.github.m4tt30c91.spring.r2dbc.ace.model.DataGroupModel;
import com.github.m4tt30c91.spring.r2dbc.ace.model.DataModel;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

/**
 * The interface to be implemented in order to define mappers for specific
 * DataModels
 * 
 * @param <T> the DataModel
 */
public interface ModelMapper<T extends DataModel> {

    /**
     * <strong>map</strong> perform a mapping between each entry in a row and the
     * DataModel
     * 
     * @param row         a single record
     * @param rowMetadata the metadata for the record
     * @return the DataModel
     */
    T map(Row row, RowMetadata rowMetadata);

    /**
     * <strong>getDataGroupModels</strong> retrieve the list of DataGroupModels
     * <p>
     * If the specific implementation does not require any DataGroupModel, the
     * implementation may return null or an empty list
     * </P>
     * 
     * @return the list of DataGroupModels
     */
    List<DataGroupModel> getDataGroupModels();
}
