package org.example.librarymanagement.specification;

import org.example.librarymanagement.entity.Book;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> titleContains(String title) {
        if (title == null || title.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Book> isbnEquals(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("isbn"), isbn);
    }

    public static Specification<Book> authorNameContains(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.join("author").get("fullName")), "%" + authorName.toLowerCase() + "%");
    }

    public static Specification<Book> publicationYearFrom(Integer yearFrom) {
        if (yearFrom == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("publicationYear"), yearFrom);
    }

    public static Specification<Book> publicationYearTo(Integer yearTo) {
        if (yearTo == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("publicationYear"), yearTo);
    }

    public static Specification<Book> onlyAvailable(Boolean available) {
        if (available == null || !available) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThan(root.get("availableCopies"), 0);
    }

    public static Specification<Book> categoryNameEquals(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(cb.lower(root.join("categories").get("name")), categoryName.toLowerCase());
        };
    }
}