package com.github.m4tt30c91.spring.r2dbc.ace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.Assert;

import com.github.m4tt30c91.spring.r2dbc.ace.engine.QuerySelectorEngine;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
public class QuerySelectorEngineTest {

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    public void shouldDatabaseClientBeInitialized() {
        Assert.notNull(this.databaseClient, "DatabaseClient should not be null");
    }

    @Test
    public void shouldQuerySelectorProcessorBeInitialized() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Assert.notNull(querySelectorEngine, "QuerySelectorEngine should not be null");
    }

    @Test
    public void shouldH2BeInitialized() {
        Flux<String> authors = this.databaseClient
                .sql("SELECT * FROM author ORDER BY id")
                .map(row -> row.get("first_name", String.class) + " " + row.get("last_name", String.class))
                .all();
        StepVerifier.create(authors)
                .expectNext("J. R. R. Tolkien")
                .expectNext("J. K. Rowling")
                .expectComplete()
                .verify();
    }

}
