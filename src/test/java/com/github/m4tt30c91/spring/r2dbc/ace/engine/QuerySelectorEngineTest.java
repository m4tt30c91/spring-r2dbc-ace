package com.github.m4tt30c91.spring.r2dbc.ace.engine;

import java.util.List;

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
                .expectNext("George R. R. Martin")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldSelectAuhtorWithBooks() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Mono<AuthorDataModel> authorDataModel = querySelectorEngine
                .processSql(
                        "SELECT a.id AS authorId, a.first_name AS firstName, a.last_name AS lastName, b.id AS bookId, b.book_title AS bookTitle, b.author_id AS bookAuthorId FROM author a JOIN book b ON a.id = b.author_id WHERE a.id = :authorId")
                .bind("authorId", 1)
                .applyModelMappers(new AuthorModelMapper(), new BookModelMapper())
                .selectOne(AuthorDataModel.class);
        StepVerifier.create(authorDataModel)
                .assertNext(dataModel -> {
                    Assert.isTrue(1 == dataModel.getId(), "Should be author with id 1");
                    this.assertJRRTalkienAndBooks(dataModel);
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldSelectAllAuthorsWithBooks() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Mono<List<AuthorDataModel>> authorDataModels = querySelectorEngine
                .processSql(
                        "SELECT a.id AS authorId, a.first_name AS firstName, a.last_name AS lastName, b.id AS bookId, b.book_title AS bookTitle, b.author_id AS bookAuthorId FROM author a JOIN book b ON a.id = b.author_id ORDER BY a.id")
                .applyModelMappers(new AuthorModelMapper(), new BookModelMapper())
                .selectMany(AuthorDataModel.class);
        StepVerifier.create(authorDataModels)
                .assertNext(dataModels -> {
                    Assert.isTrue(2 == dataModels.size(), "Should contain 2 authors");
                    this.assertAuthorsAndBooks(dataModels);
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReturnEmpty() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Mono<AuthorDataModel> authorDataModel = querySelectorEngine
                .processSql(
                        "SELECT a.id AS authorId, a.first_name AS firstName, a.last_name AS lastName, b.id AS bookId, b.book_title AS bookTitle, b.author_id AS bookAuthorId FROM author a JOIN book b ON a.id = b.author_id WHERE a.id = :authorId")
                .bind("authorId", 3)
                .applyModelMappers(new AuthorModelMapper(), new BookModelMapper())
                .selectOne(AuthorDataModel.class);
        StepVerifier.create(authorDataModel)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReturnEmptyList() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Mono<List<AuthorDataModel>> authorDataModels = querySelectorEngine
                .processSql(
                        "SELECT a.id AS authorId, a.first_name AS firstName, a.last_name AS lastName, b.id AS bookId, b.book_title AS bookTitle, b.author_id AS bookAuthorId FROM author a JOIN book b ON a.id = b.author_id WHERE a.id = :authorId order by a.id")
                .bind("authorId", 3)
                .applyModelMappers(new AuthorModelMapper(), new BookModelMapper())
                .selectMany(AuthorDataModel.class);
        StepVerifier.create(authorDataModels)
                .assertNext(list -> {
                    Assert.isTrue(list.isEmpty(), "Should contain no author");
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReturnAllAuthors() {
        QuerySelectorEngine querySelectorEngine = new QuerySelectorEngine(this.databaseClient);
        Mono<List<AuthorDataModel>> authorDataModels = querySelectorEngine
                .processSql(
                        "SELECT a.id AS authorId, a.first_name AS firstName, a.last_name AS lastName, b.id AS bookId, b.book_title AS bookTitle, b.author_id AS bookAuthorId FROM author a LEFT JOIN book b ON a.id = b.author_id")
                .applyModelMappers(new AuthorModelMapper(), new BookModelMapper())
                .selectMany(AuthorDataModel.class);
        StepVerifier.create(authorDataModels)
                .assertNext(list -> {
                    Assert.isTrue(3 == list.size(), "Should contain 3 authors");
                    this.assertAuthorsAndBooks(list);
                    AuthorDataModel martin = list.get(2);
                    String martinFullName = martin.getFirstName() + " " + martin.getLastName();
                    Assert.isTrue("George R. R. Martin".equals(martinFullName), "Should author with full name 'George R. R. Martin'");
                    Assert.isNull(martin.getBooks(), "Should have written no books");
                })
                .expectComplete()
                .verify();
    }

    private void assertAuthorsAndBooks(List<AuthorDataModel> dataModels) {
        AuthorDataModel tolkien = dataModels.get(0);
        AuthorDataModel rowling = dataModels.get(1);
        this.assertJRRTalkienAndBooks(tolkien);
        this.asserJKRowlingAndBooks(rowling);
    }

    private void assertJRRTalkienAndBooks(AuthorDataModel tolkien) {
        String tolkienFullName = tolkien.getFirstName() + " " + tolkien.getLastName();
        Assert.isTrue("J. R. R. Tolkien".equals(tolkienFullName),
                "Should be author with full name 'J. R. R. Tolkien'");
        Assert.isTrue(3 == tolkien.getBooks().size(), "Should have written 3 books");
        Assert.isTrue(this.containsBook(tolkien, "The Fellowship of the Ring"),
                "Should have written 'The Fellowship of the Ring'");
        Assert.isTrue(this.containsBook(tolkien, "The Two Towers"), "Should have written 'The Two Towers'");
        Assert.isTrue(this.containsBook(tolkien, "The Return of the King"),
                "Should have written 'The Return of the King'");
    }

    private void asserJKRowlingAndBooks(AuthorDataModel rowling) {
        String rowlingFullName = rowling.getFirstName() + " " + rowling.getLastName();
        Assert.isTrue("J. K. Rowling".equals(rowlingFullName),
                "Should be author with full name 'J. K. Rowling'");
        Assert.isTrue(7 == rowling.getBooks().size(), "Should have written 7 books");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Philosopher's Stone"),
                "Should have written 'Harry Potter and the Philosopher's Stone'");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Chamber of Secrets"),
                "Should have written 'Harry Potter and the Chamber of Secrets'");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Prisoner of Azkaban"),
                "Should have written 'Harry Potter and the Prisoner of Azkaban'");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Goblet of Fire"),
                "Should have written 'Harry Potter and the Goblet of Fire'");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Order of the Phoenix"),
                "Should have written 'Harry Potter and the Order of the Phoenix'");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Half-Blood Prince"),
                "Should have written 'Harry Potter and the Half-Blood Prince'");
        Assert.isTrue(this.containsBook(rowling, "Harry Potter and the Deathly Hallows"),
                "Should have written 'Harry Potter and the Deathly Hallows'");
    }

    private boolean containsBook(AuthorDataModel authorDataModel, String bookTitle) {
        return authorDataModel.getBooks().stream()
                .anyMatch(book -> bookTitle.equals(book.getBookTitle()));
    }

}
