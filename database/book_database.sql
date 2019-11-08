CREATE TABLE author (
  id SERIAL,
  name VARCHAR(30) NOT NULL,
  note TEXT,
  create_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP NOT NULL,
  CONSTRAINT pk_author PRIMARY KEY (id)
);

CREATE TABLE book (
  id SERIAL,
  name VARCHAR(255) NOT NULL,
  page INTEGER,
  publisher VARCHAR(255),
  sale_date DATE,
  isbn VARCHAR(13),
  note TEXT,
  author_id INTEGER REFERENCES author,
  create_date TIMESTAMP NOT NULL,
  update_date TIMESTAMP NOT NULL,
  CONSTRAINT pk_book PRIMARY KEY (id)
);

CREATE VIEW book_info
AS
SELECT
  book.id as book_id,
  book.name as book_name,
  CASE WHEN book.page IS NULL THEN '' ELSE TRIM(TO_CHAR(book.page, '9999')) END as page,
  COALESCE(book.publisher, '') as publisher,
  CASE WHEN book.sale_date IS NULL THEN '' ELSE TO_CHAR(book.sale_date, 'YYYY/MM/DD') END as sale_date,
  COALESCE(book.isbn, '') as isbn,
  COALESCE(book.note, '') AS book_note,
  CASE WHEN author.id IS NULL THEN '' ELSE TRIM(TO_CHAR(author.id, '999999')) END AS author_id,
  COALESCE(author.name, '') AS author_name,
  COALESCE(author.note, '') AS author_note,
  TO_CHAR(book.update_date, 'YYYY-MM-DD HH24:MI:SS.MSUS') AS book_update_date,
  CASE WHEN author.update_date IS NULL THEN '' ELSE TO_CHAR(author.update_date, 'YYYY-MM-DD HH24:MI:SS.MSUS') END AS author_update_date
FROM book
LEFT JOIN author
ON book.author_id = author.id;
