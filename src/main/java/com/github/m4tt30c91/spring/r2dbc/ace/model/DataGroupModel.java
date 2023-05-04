package com.github.m4tt30c91.spring.r2dbc.ace.model;

import java.util.List;

/**
 * The interface to be implemented by entities that define grouping association
 * between DataModels
 * 
 * @param <B> the base class
 * @param <C> the collectable class
 */
public interface DataGroupModel<B extends DataModel, C extends DataModel> {

    /**
     * <strong>base</strong> retrieve the base class of the grouping association
     * 
     * @return the base class
     */
    Class<B> base();

    /**
     * <strong>collecatable</strong> retrieve the collectable class of the grouping
     * association
     * 
     * @return the collectable class
     */
    Class<C> collectable();

    /**
     * <strong>getCollectables</strong> retriece the list of collectables from the
     * base
     * 
     * @param base the base from which to retrieve the list of collectables
     * @return the list of collectables for the base
     */
    List<C> getCollectables(B base);

    /**
     * <strong>setCollectables</strong> set the given collectables to the list of
     * collectables in base
     * 
     * @param base         the base
     * @param collectables the collectables
     */
    void setCollectables(B base, List<C> collectables);
}
