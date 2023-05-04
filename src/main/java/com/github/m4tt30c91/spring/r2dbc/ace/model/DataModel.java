package com.github.m4tt30c91.spring.r2dbc.ace.model;

/**
 * The interface to be implemented by entities in orther to be processed by a
 * QuerySelectorEngine
 */
public interface DataModel {

    /**
     * <strong>uniqueIdentifier</strong> retrieve the instance' unique identifier
     * <p>
     * Plese note that this method should be kept as simple as possible, otherwise
     * you may have negative impacts on performance
     * </P>
     * 
     * @return The unique identifier
     */
    String uniqueIdentifier();
}
