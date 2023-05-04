# Spring R2DBC ACE

## Introduction

**Spring R2DBC ACE** is a utility library built on top of [Spring Data R2DBC](https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/).

It provides features to implement One-To-Many and One-To-One associations, that are currently missing in the provided in the Spring implementation.

## Installation procedure

In order to integrate **Spring R2DBC ACE** you have to add the following module to your dependencies:

```
<dependency>
    <groupId>com.github.m4tt30c91</groupId>
    <artifactId>spring-r2dbc-ace</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

Thhe module depends on the following:

```
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-commons</artifactId>
    <scope>provided</scope>
</dependency>
```

```
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-r2dbc</artifactId>
    <scope>provided</scope>
</dependency>
```

Please note that these dependencies are marked as *provided*, so you have to include them in your module to work.

The current implementation is relying on **Spring Boot 2.7.8**:

```
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.8</version>
    <relativePath/>
</parent>
```

## How to use it

We will make use of the following terms:
* **Base**: any model that may have dependencies to a set of objects
* **Collectable**: any model that is defined as a set of objects in some **Base**

For example if we take the Author-Book scenario, an Author may be a Base and a Book may be a Collectable, since each author may have written multiple books.

The whole process relies on the `com.github.m4tt30c91.spring.r2dbc.ace.engine.QuerySelectorEngine`.

The engine needs a `org.springframework.r2dbc.core.DatabaseClient` to be initialized.

The usual process is:
1. Submit the SQL statement through `QuerySelectorEngine::processSql`; this will produce a `QuerySelectorEngine.QueryProcessor` that will help further procedures
2. Provide any bind variables to the processor through `QueryProcessor::bind`
3. Apply the mapping definition to the processor through `QueryProcessor::applyModelMappers`; this will produce a `QueryProcessor.QueryResultProcessor` that will help you define the select procedure
4. Permorm the select procedure on the processor either through `QueryResultProcessor::selectOne`, if you are looking at a single result, or through `QueryResultProcessor::selectMany`, if your are looking at a result set; you'll have to provide the root entity

There are mainly three concepts you need to familiarize with:
* **ModelMapper**
* **DataGroupModel**
* **DataModel**

### ModelMapper

A **ModelMapper** is a component you have to implement to define the mapping between data in a recordset and a **Base**.

You also have to provide the set of **Collectables** (if any), that should be used to define any kind of One-To-Many relationship.

You may want to have a look at the JavaDoc for more information about the interface definition.

### DataGroupModel

A **DataGroupModel** is a component you have to implement to define the relationship between some **Base** and some **Collectable**.

You may want to have a look at the JavaDoc for more information about the interface definition.

### DataModel

A **DataModel** is a component you have to implement to define any kind of entity.

Please note that an entity has to provide a **uniqueIdentifier**, that can be anything; keep in mind that computing this identifier impacts the engine performance, so you should keep it as simple as possible.

You may want to have a look at the JavaDoc for more information about the interface definition.

## Author-Book example

Before going any further, the tests provide a set of example you can follow to implement your integration; following we will show a simple Author-Book scenario.

Let's say you have a list of Authors, where each Author wrote one or more Books.

Here is the data layer definition:

```
CREATE TABLE author (id INTEGER PRIMARY KEY, first_name VARCHAR(128), last_name VARCHAR(128));
CREATE TABLE book (id INTEGER PRIMARY KEY, book_title VARCHAR(255), author_id INTEGER);
ALTER TABLE book ADD FOREIGN KEY (author_id) REFERENCES author(id);
```

First of all we have to create are **DataModels**:

```
package com.github.m4tt30c91.spring.r2dbc.ace.model;

import java.util.List;

public class AuthorDataModel implements DataModel {

    private int id;
    private String firstName;
    private String lastName;
    private List<BookDataModel> books;

    @Override
    public String uniqueIdentifier() {
        return this.id + "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<BookDataModel> getBooks() {
        return books;
    }

    public void setBooks(List<BookDataModel> books) {
        this.books = books;
    }
}
```

```
package com.github.m4tt30c91.spring.r2dbc.ace.model;

public class BookDataModel implements DataModel {

    private int id;
    private String bookTitle;
    private int authorId;

    @Override
    public String uniqueIdentifier() {
        return this.id + "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

}
```

Note in both the scenarios the `DataModel::uniqueIdentifier` implementations refer to the *Primary Keys* of each entity.

Then we have to define our Author-To-Book mapping, through a **DataGroupModel**:

```
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
```

At the end we have to define our mappers, one for each **DataModel**; we will then proceed to implement our **ModelMappers**:

```
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
```

```
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
        Integer id = row.get("bookId", Integer.class);
        if (id == null) {
            return null;
        }
        BookDataModel bookDataModel = new BookDataModel();
        bookDataModel.setId(id);
        bookDataModel.setBookTitle(row.get("bookTitle", String.class));
        bookDataModel.setAuthorId(row.get("bookAuthorId", Integer.class));
        return bookDataModel;
    }

    @Override
    public List<DataGroupModel> getDataGroupModels() {
        return Collections.singletonList(new Author2BookDataGroupModel());
    }
}
```

Please not that the `Author2BookDataGroupModel` could have been provided either in `AuthorModelMapper` or `BookModelMapper`, the result would have been the same.

Let's say now that we want to read all the Books for a specific Author:

```
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
```

We can also retrieve all the Authors with their Books:

```
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
```

We won't goo in to the details about the validation steps over here, but you can check all the tests in the module.