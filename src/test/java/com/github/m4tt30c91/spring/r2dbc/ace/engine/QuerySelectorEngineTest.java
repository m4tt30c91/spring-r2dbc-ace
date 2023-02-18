package com.github.m4tt30c91.spring.r2dbc.ace.engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.Assert;

import com.github.m4tt30c91.spring.r2dbc.ace.mapper.AuthorModelMapper;
import com.github.m4tt30c91.spring.r2dbc.ace.mapper.BookModelMapper;
import com.github.m4tt30c91.spring.r2dbc.ace.model.AuthorDataModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

    @Test
    public void shouldSelectAuhtorWithBooks() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Mono<AuthorDataModel> authorDataModel = querySelectorEngine
                .processSql("SELECT a.id AS authorId, a.first_name AS firstName, a.last_name AS lastName, b.id AS bookId, b.book_title AS bookTitle, b.author_id AS bookAuthorId FROM author a JOIN book b ON a.id = b.author_id WHERE a.id = :authorId")
                .bind("authorId", 1)
                .applyModelMappers(new AuthorModelMapper(), new BookModelMapper())
                .selectOne(AuthorDataModel.class);
        StepVerifier.create(authorDataModel)
                .assertNext(dataModel -> {
                    Assert.isTrue(1 == dataModel.getId(), "Should be author with id 1");
                    //TODO continue testing
                })
                .expectComplete()
                .verify();
    }

    private boolean containsBook(AuthorDataModel authorDataModel, String bookTitle) {
        return authorDataModel.getBooks().stream()
                .anyMatch(book -> bookTitle.equals(book.getBookTitle()));
    }

}
