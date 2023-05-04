package com.github.m4tt30c91.spring.r2dbc.ace.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.data.util.Pair;
import org.springframework.r2dbc.core.DatabaseClient;
import com.github.m4tt30c91.spring.r2dbc.ace.mapper.ModelMapper;
import com.github.m4tt30c91.spring.r2dbc.ace.model.DataGroupModel;
import com.github.m4tt30c91.spring.r2dbc.ace.model.DataModel;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Mono;

/**
 * A select processor to make up for the lack of relationship processors on the
 * jpa' R2DBC implementation
 */
public class QuerySelectorEngine {

    /**
     * THe submitted DatabaseClient
     */
    private final DatabaseClient databaseClient;

    /**
     * Instantiate the QuerySelectorEngine with a DatabaseClient
     *
     * @param databaseClient the DatabaseClient
     */
    public QuerySelectorEngine(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    /**
     * <strong>processSql</strong> submit a select sql statement to process its
     * result
     *
     * @param sql the select sql statement
     * @return a QueryProcessor to process the result
     */
    public QueryProcessor processSql(String sql) {
        DatabaseClient.GenericExecuteSpec genericExecuteSpec = this.databaseClient.sql(sql);
        return new QueryProcessor(genericExecuteSpec);
    }

    /**
     * The class definition to create the QueryResultProcessor to process records
     */
    public static class QueryProcessor {

        /**
         * a DatabaseClient.GenericExecuteSpec instance
         */
        private DatabaseClient.GenericExecuteSpec genericExecuteSpec;

        /**
         * Private constructor to prevent external code to create an instance of the
         * class
         *
         * @param genericExecuteSpec a DatabaseClient.GenericExecuteSpec instance
         */
        private QueryProcessor(DatabaseClient.GenericExecuteSpec genericExecuteSpec) {
            this.genericExecuteSpec = genericExecuteSpec;
        }

        /**
         * <strong>bind</strong> wrap the DatabaseClient.GenericExecuteSpec.bind method
         *
         * @param name  the bind variable
         * @param value the value to replace
         * @return the same QueryProcessor to implement fluent programming
         */
        public QueryProcessor bind(String name, Object value) {
            this.genericExecuteSpec = this.genericExecuteSpec.bind(name, value);
            return this;
        }

        /**
         * <strong>applyModelMappers</strong> collect the list of ModelMappers and
         * submit each record to each instance
         *
         * @param modelMappers the array of ModelMappers
         * @return a QueryResulProcessor to perform the required select operation
         */
        public QueryResultProcessor applyModelMappers(ModelMapper... modelMappers) {
            Mono<List<Map<Class<? extends DataModel>, DataModel>>> resultPublisher = this.genericExecuteSpec
                    .map((row, rowMetadata) -> this.applyModelMappers(row,
                            rowMetadata, modelMappers))
                    .all().collectList();
            return new QueryResultProcessor(resultPublisher, modelMappers);
        }

        /**
         * <strong>applyModelMappers</strong> collect the list of ModelMappers and
         * submit the record to each instance
         *
         * @param row          the record
         * @param rowMetadata  the record metadata
         * @param modelMappers the array of ModelMappers
         * @return a mapping between DataModel classes and their instances, based on the
         *         record
         */
        private Map<Class<? extends DataModel>, DataModel> applyModelMappers(Row row,
                RowMetadata rowMetadata, ModelMapper... modelMappers) {
            return Arrays.stream(modelMappers).map(modelMapper -> modelMapper.map(row, rowMetadata))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(DataModel::getClass, dataModel -> dataModel));
        }

        /**
         * The class definition to perform the processing of records
         */
        public static class QueryResultProcessor {

            /**
             * The result processed from some select statement
             */
            private final Mono<List<Map<Class<? extends DataModel>, DataModel>>> resultPublisher;
            /**
             * The list of DataGroupModels derived by the set of ModelMappers
             */
            private final List<DataGroupModel> dataGroupModels;

            /**
             * The mapping between Bases and Collectables
             */
            private final Bases2Collectables bases2Collectables;
            /**
             * The association between each DataGroupModel and its own Base
             */
            private final List<Pair<DataGroupModel, DataModel>> group2Base;
            /**
             * The association between each DataModel and its list of unique ids
             */
            private final Map<Class<? extends DataModel>, Set<String>> distinctDataModels;

            /**
             * Private constructor to prevent external code to create an instance of the
             * class
             *
             * @param resultPublisher the result processed from some select statement
             * @param modelMappers    the array of ModelMappers to map each record to a set
             *                        of
             *                        DataModel
             */
            private QueryResultProcessor(
                    Mono<List<Map<Class<? extends DataModel>, DataModel>>> resultPublisher,
                    ModelMapper... modelMappers) {
                this.resultPublisher = resultPublisher;
                this.dataGroupModels = new LinkedList<>();
                this.bases2Collectables = new Bases2Collectables();
                this.group2Base = new LinkedList<>();
                this.distinctDataModels = new HashMap<>();
                Arrays.stream(modelMappers).map(ModelMapper::getDataGroupModels)
                        .filter(Objects::nonNull).filter(list -> !list.isEmpty())
                        .collect(Collectors.toList()).forEach(this.dataGroupModels::addAll);
            }

            /**
             * <strong>selectOne</strong> select the first entry for tClass in the records
             * list
             * <p>
             * Please note that the method will return one element even if more than one
             * result is found, so that if your result set contains more than one element
             * such that DataModel::uniqueIdentifier is not equal, only one of them will be
             * return and no error will be thrown. This coise has been made for the sake of
             * the engine performances
             * </p>
             *
             * @param tClass the target class to be returned from the result processing
             * @param <T>    the type of the target class
             * @return the first data model that meet tClass or empty if no element is
             *         present
             */
            public <T extends DataModel> Mono<T> selectOne(Class<T> tClass) {
                return this.resultPublisher
                        .filter(list -> !list.isEmpty())
                        .map(list -> {
                            this.collectAndGroup(list);
                            return (T) list.get(0).get(tClass);
                        });
            }

            /**
             * <strong>selectMany</strong> select all the entries for tClass in the records
             * list
             *
             * @param tClass the target class to be returned from the result processing
             * @param <T>    the type of the target class
             * @return the list of data models that meet tClass
             */
            public <T extends DataModel> Mono<List<T>> selectMany(Class<T> tClass) {
                return this.resultPublisher
                        .filter(list -> !list.isEmpty())
                        .map(list -> {
                            this.collectAndGroup(list);
                            return this.selectDistinct(list, tClass);
                        })
                        .switchIfEmpty(Mono.just(Collections.emptyList()));
            }

            /**
             * <strong>selectDistinct</strong> select all the distinct data models for
             * tClass in the records list
             *
             * @param list   the records processed by a statement
             * @param tClass the target class to be returned from the result processing
             * @param <T>    the type of the target class
             * @return the list of distinct data models
             */
            private <T extends DataModel> List<T> selectDistinct(
                    List<Map<Class<? extends DataModel>, DataModel>> list, Class<T> tClass) {
                Set<String> ids = new HashSet<>();
                List<T> dataModels = new LinkedList<>();
                for (Map<Class<? extends DataModel>, DataModel> map : list) {
                    T entry = (T) map.get(tClass);
                    String id = entry.uniqueIdentifier();
                    if (!ids.contains(id)) {
                        dataModels.add(entry);
                        ids.add(id);
                    }
                }
                return dataModels;
            }

            /**
             * <strong>collectAndGroup</strong> collect all the records and apply each group
             * function
             *
             * @param list the records processed by a statement
             */
            private void collectAndGroup(List<Map<Class<? extends DataModel>, DataModel>> list) {

                // Collect and group each record for each model grouping
                for (Map<Class<? extends DataModel>, DataModel> record : list) {
                    for (DataGroupModel dataGroupModel : this.dataGroupModels) {
                        this.collectRecord(dataGroupModel, record);
                    }
                }

                // Distinct collections
                for (Pair<DataGroupModel, DataModel> pair : this.group2Base) {
                    this.distinctCollection(pair);
                }
            }

            /**
             * <strong>collectRecord</strong> collect, if any, the association between the
             * base and the collectable for that record
             *
             * @param dataGroupModel the association between a Base and a Collectable
             * @param record         a representation of a single record in terms of
             *                       DataModels
             */
            private void collectRecord(DataGroupModel dataGroupModel,
                    Map<Class<? extends DataModel>, DataModel> record) {

                // Collect base and collectable classes
                Class<? extends DataModel> baseClass = dataGroupModel.base();

                // Guard point: no record to collect
                boolean isBaseInRecord = record.containsKey(dataGroupModel.base());
                boolean isCollectableInRecord = isBaseInRecord && record.containsKey(dataGroupModel.collectable());
                if (!isCollectableInRecord)
                    return;

                // Collect data models
                DataModel base = record.get(dataGroupModel.base());
                DataModel collectable = record.get(dataGroupModel.collectable());
                String id = base.uniqueIdentifier();

                // Add the collectable and, if the process generates a new collection, collect
                // the association
                Pair<Boolean, List<DataModel>> collectables = this.bases2Collectables.addCollectable(baseClass, id,
                        collectable);
                if (!collectables.getFirst()) {
                    dataGroupModel.setCollectables(base, collectables.getSecond());
                    this.group2Base.add(Pair.of(dataGroupModel, base));
                }

            }

            /**
             * <strong>distinctCollection</strong> distinct all data in all collectables and
             * rewrite on base
             *
             * @param pair the association between a ModelGrouping and a DataModel
             */
            private void distinctCollection(Pair<DataGroupModel, DataModel> pair) {

                // Get data from pair
                DataGroupModel dataGroupModel = pair.getFirst();
                DataModel base = pair.getSecond();
                Class<? extends DataModel> baseClass = base.getClass();
                String id = base.uniqueIdentifier();

                // Guard point: data already processed
                boolean containsClass = this.distinctDataModels.containsKey(baseClass);
                boolean skipRecord = containsClass && this.distinctDataModels.get(baseClass).contains(id);
                if (skipRecord)
                    return;

                // Get collectable and distinct
                List<DataModel> collectables = dataGroupModel.getCollectables(base);
                List<DataModel> distinctCollectables = collectables.stream()
                        .collect(Collectors.collectingAndThen(Collectors.toCollection(
                                () -> new TreeSet<>(Comparator.comparing(DataModel::uniqueIdentifier))),
                                LinkedList::new));

                // Set collectable for base
                dataGroupModel.setCollectables(base, distinctCollectables);

                if (containsClass) { // Some data model for the same base class has already been
                                     // collected
                    this.distinctDataModels.get(baseClass).add(id);
                } else { // No data model for the same base class has been collected yet
                    Set<String> set = new HashSet<>();
                    set.add(id);
                    this.distinctDataModels.put(baseClass, set);
                }
            }

            /**
             * The class definition to map Bases to Collectables
             */
            private static class Bases2Collectables {

                /**
                 * The internal association between a base class and its ids
                 */
                private final Map<Class<? extends DataModel>, BasesId2Collectables> map;

                /**
                 * Empty constructor to initialize the map
                 */
                public Bases2Collectables() {
                    this.map = new HashMap<>();
                }

                /**
                 * <strong>addCollectable</strong> associate a Base to a Collectable
                 * <p>
                 * If the baseClass is not yet present, a new entry in the map will be stored,
                 * and it will begin the delegation process to create the list of collectables
                 * starting from collectable.
                 * </p>
                 * <p>
                 * If the baseClass is present, the entry will be resumed and the delegation
                 * process will try to add collectable to an already existing collection if
                 * present, or create a new one if no collection has already been created.
                 * </p>
                 *
                 * @param baseClass   the base class
                 * @param id          the base id
                 * @param collectable the collectable element
                 * @return A pair of elements where:
                 *         <ul>
                 *         <li>pair.left is a boolean that takes true if the list of
                 *         collectables was already build, false instead</li>
                 *         <li>pair.right is the resulting list of collectables</li>
                 *         </ul>
                 */
                public Pair<Boolean, List<DataModel>> addCollectable(
                        Class<? extends DataModel> baseClass, String id, DataModel collectable) {
                    BasesId2Collectables basesId2Collectables;
                    if (this.map.containsKey(baseClass)) {
                        basesId2Collectables = this.map.get(baseClass);
                    } else {
                        basesId2Collectables = new BasesId2Collectables();
                        this.map.put(baseClass, basesId2Collectables);
                    }
                    return basesId2Collectables.addCollectable(id, collectable);
                }

                /**
                 * The class definition to map Base Ids to Collectables
                 */
                private static final class BasesId2Collectables {

                    /**
                     * The internal association between a base id and its collectables
                     */
                    private final Map<String, Collectable2Collection> map;

                    /**
                     * Empty constructor to initialize the map
                     */
                    public BasesId2Collectables() {
                        this.map = new HashMap<>();
                    }

                    /**
                     * <strong>addCollectable</strong> associate the base id to a collectable
                     * <p>
                     * If the id is not yet present, a new entry in the map will be stored, and it
                     * will begin the delegation process to create the list of collectables starting
                     * from collectable.
                     * </p>
                     * <p>
                     * If the id is present, the entry will be resumed and the delegation process
                     * will try to add collectable to an already existing collection if present, or
                     * create a new one if no collection has already been created.
                     * </p>
                     *
                     * @param id          the base id
                     * @param collectable the collectable
                     * @return A pair of elements where:
                     *         <ul>
                     *         <li>pair.left is a boolean that takes true if the list of
                     *         collectables was already build, false instead</li>
                     *         <li>pair.right is the resulting list of collectables</li>
                     *         </ul>
                     */
                    public Pair<Boolean, List<DataModel>> addCollectable(String id,
                            DataModel collectable) {
                        Collectable2Collection collectable2Collection;
                        if (this.map.containsKey(id)) {
                            collectable2Collection = this.map.get(id);
                        } else {
                            collectable2Collection = new Collectable2Collection();
                            this.map.put(id, collectable2Collection);
                        }
                        return collectable2Collection.addCollectable(collectable);
                    }

                    /**
                     * The class definition to map Collectable classes to Collectables
                     */
                    private static final class Collectable2Collection {

                        /**
                         * The internal association between a collectable class and its collectables
                         */
                        private final Map<Class<? extends DataModel>, List<DataModel>> map;

                        /**
                         * Empty constructor to initialize the map
                         */
                        public Collectable2Collection() {
                            this.map = new HashMap<>();
                        }

                        /**
                         * <strong>addCollectable</strong> associate collectable to a collection of
                         * collectables of the same type
                         * <p>
                         * If the class is not yet present, a new entry in the map will be stored, a
                         * new list will be created and collectable will be stored inside the
                         * collection
                         * </p>
                         * <p>
                         * if the class is present, the entry will be resumed and the collectable
                         * will be add to the collection
                         * </p>
                         *
                         * @param collectable the collectable
                         * @return A pair of elements where:
                         *         <ul>
                         *         <li>pair.left is a boolean that takes true if the list of
                         *         collectables was already build, false instead</li>
                         *         <li>pair.right is the resulting list of collectables</li>
                         *         </ul>
                         */
                        public Pair<Boolean, List<DataModel>> addCollectable(
                                DataModel collectable) {
                            boolean wasListAlreadyPresent;
                            List<DataModel> dataModels;
                            Class<? extends DataModel> aClass = collectable.getClass();
                            if (this.map.containsKey(aClass)) {
                                // A collection for the same class has already been created
                                wasListAlreadyPresent = true;
                                dataModels = this.map.get(aClass);
                                dataModels.add(collectable);
                            } else {
                                // No collection for the same class has already been created
                                wasListAlreadyPresent = false;
                                dataModels = new LinkedList<>();
                                dataModels.add(collectable);
                                this.map.put(aClass, dataModels);
                            }
                            return Pair.of(wasListAlreadyPresent, dataModels);
                        }
                    }
                }
            }
        }
    }

}
