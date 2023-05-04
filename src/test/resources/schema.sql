CREATE TABLE author (id INTEGER PRIMARY KEY, first_name VARCHAR(128), last_name VARCHAR(128));
CREATE TABLE book (id INTEGER PRIMARY KEY, book_title VARCHAR(255), author_id INTEGER);
ALTER TABLE book ADD FOREIGN KEY (author_id) REFERENCES author(id);

INSERT INTO author(id, first_name, last_name) VALUES(1, 'J. R. R.', 'Tolkien');
INSERT INTO book(id, book_title, author_id) VALUES(1, 'The Fellowship of the Ring', 1);
INSERT INTO book(id, book_title, author_id) VALUES(2, 'The Two Towers', 1);
INSERT INTO book(id, book_title, author_id) VALUES(3, 'The Return of the King', 1);

INSERT INTO author(id, first_name, last_name) VALUES(2, 'J. K.', 'Rowling');
INSERT INTO book(id, book_title, author_id) VALUES(4, 'Harry Potter and the Philosopher''s Stone', 2);
INSERT INTO book(id, book_title, author_id) VALUES(5, 'Harry Potter and the Chamber of Secrets', 2);
INSERT INTO book(id, book_title, author_id) VALUES(6, 'Harry Potter and the Prisoner of Azkaban', 2);
INSERT INTO book(id, book_title, author_id) VALUES(7, 'Harry Potter and the Goblet of Fire', 2);
INSERT INTO book(id, book_title, author_id) VALUES(8, 'Harry Potter and the Order of the Phoenix', 2);
INSERT INTO book(id, book_title, author_id) VALUES(9, 'Harry Potter and the Half-Blood Prince', 2);
INSERT INTO book(id, book_title, author_id) VALUES(10, 'Harry Potter and the Deathly Hallows', 2);

INSERT INTO author(id, first_name, last_name) VALUES(3, 'George R. R.', 'Martin');