package com.spring.library.controller;

import com.spring.library.domain.*;
import com.spring.library.service.BookService;
import com.spring.library.service.ReviewService;
import com.spring.library.service.WriterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;


@Controller
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private WriterService writerService;

    @Autowired
    private ReviewService reviewService;


    @GetMapping("books")
    public String getBookList(Model model) {
        model.addAttribute("books", bookService.getBookList());
        return "book/bookList";
    }

    @GetMapping("books/{book:[\\d]+}")
    public String getBookPage(@PathVariable Book book, Model model) {
        ControllerUtils.isBookExists(book);

        model.addAttribute("book", book);
        return "book/bookPage";
    }

    @GetMapping("books/add")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String getBookAddPage(Model model) {
        model.addAttribute("genres", Genre.values());
        model.addAttribute("writers", writerService.getWriterList());
        return "book/bookAdd";
    }

    @PostMapping("books")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String addNewBook(
            @Valid Book book,
            BindingResult bindingResult,
            Model model,
            @RequestParam Map<String, String> form,
            @RequestParam(name = "selectedWriter", required = false) Writer writer,
            @RequestParam(name = "posterFile") MultipartFile posterFile
    ) {
        Set<Genre> selectedGenres = bookService.getSelectedGenresFromForm(form);

        book.setGenres(selectedGenres);
        book.setWriter(writer);
        model.addAttribute("book", book);

        boolean isCorrectBookForm = isCorrectBookForm(selectedGenres, writer,
                book.getPublicationDate(), bindingResult, model);
        boolean isCorrectPoster = isCorrectPoster(posterFile, model);
        if (isCorrectBookForm && isCorrectPoster) {
            try {
                String posterFilename = bookService.getPosterFilename(posterFile);
                book.setFilename(posterFilename);
                bookService.loadPosterFile(posterFile, posterFilename);

                if (bookService.addNewBook(book)) {
                    return "redirect:/books";
                } else {
                    model.addAttribute("bookError", "Book already exists");
                }
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("posterFileError", "Incorrect file");
            }
        }

        return getBookAddPage(model);
    }


    private boolean isCorrectBookForm(
            Set<Genre> genres,
            Writer writer,
            Date publicationDate,
            BindingResult bindingResult,
            Model model
    ) {
        boolean isCorrectGenres = !genres.isEmpty();
        if (!isCorrectGenres) {
            model.addAttribute("genresError", "Please, select a book genres");
        }

        boolean isWriterSelected = writer != null;
        if (isWriterSelected) {
            model.addAttribute("selectedWriter", writer);
        } else {
            model.addAttribute("selectedWriterError", "Please, select an author or create a new one");
        }

        boolean isPublicationDateSelected = publicationDate != null;
        if (!isPublicationDateSelected) {
            model.addAttribute("publicationDateError", "Please, select the publication date");
        }


        boolean isBindingResultHasErrors = ControllerUtils.mergeErrorsWithModel(bindingResult, model);

        return isCorrectGenres && isWriterSelected && isPublicationDateSelected
                && !isBindingResultHasErrors;
    }

    private boolean isCorrectPoster(MultipartFile posterFile, Model model) {
        boolean isCorrectPoster = !StringUtils.isEmpty(posterFile.getOriginalFilename()) && bookService.isImage(posterFile);
        if (!isCorrectPoster) {
            model.addAttribute("posterFileError", "There are must be correct poster file");
        }

        return isCorrectPoster;
    }

    @GetMapping("books/{book:[\\d]+}/edit")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String getBookEditPage(@PathVariable Book book, Model model) {
        ControllerUtils.isBookExists(book);

        model.addAttribute("genres", Genre.values());
        model.addAttribute("writers", writerService.getWriterList());
        model.addAttribute("book", book);
        model.addAttribute("currentBookId", book.getId());
        return "book/bookEdit";
    }

    @PutMapping("/books/{currentBook:[\\d]+}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String updateBook(
            @PathVariable("currentBook") Book currentBook,
            @Valid Book editedBook,
            BindingResult bindingResult,
            Model model,
            @RequestParam Map<String, String> form,
            @RequestParam(name = "selectedWriter", required = false) Writer writer,
            @RequestParam(name = "posterFile") MultipartFile posterFile
    ) {
        ControllerUtils.isBookExists(currentBook);

        Set<Genre> selectedGenres = bookService.getSelectedGenresFromForm(form);

        editedBook.setGenres(selectedGenres);
        editedBook.setWriter(writer);
        model.addAttribute("book", editedBook);

        boolean isCorrectBookForm = isCorrectBookForm(selectedGenres, writer,
                editedBook.getPublicationDate(), bindingResult, model);
        if (isCorrectBookForm) {
            /* you don't need to change the poster, but if you did, it must be correct*/
            boolean isCorrectPoster = isCorrectPoster(posterFile, model);
            if (isCorrectPoster) {
                String posterFilename = bookService.getPosterFilename(posterFile);
                editedBook.setFilename(posterFilename);
                try {
                    bookService.loadPosterFile(posterFile, posterFilename);
                } catch (IOException e) {
                    e.printStackTrace();
                    model.addAttribute("posterFileError", "Incorrect file");
                    isCorrectBookForm = false;
                }
            }
        }

        if (isCorrectBookForm) {
            bookService.updateBook(currentBook, editedBook);
            return "redirect:/books/" + currentBook.getId();
        }

        model.addAttribute("genres", Genre.values());
        model.addAttribute("writers", writerService.getWriterList());
        model.addAttribute("book", editedBook);
        model.addAttribute("currentBookId", currentBook.getId());
        return "book/bookEdit";
    }

    @DeleteMapping("/books/{book:[\\d]+}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String deleteBook(@PathVariable Book book) {
        ControllerUtils.isBookExists(book);

        bookService.deleteBook(book);

        return "redirect:/books";
    }


    /*Book Reviews*/

    @GetMapping("/books/{book:[\\d]+}/reviews")
    public String getBookReviewsPage(@PathVariable("book") Book reviewBook, Model model) {
        ControllerUtils.isBookExists(reviewBook);

        addAssessmentsToModel(model);
        addBookAndBookReviewsToModel(model, reviewBook);
        model.addAttribute("reviewAction", "/books/" + reviewBook.getId() + "/reviews");
        return "review/reviewList";
    }

    @PostMapping("/books/{book:[\\d]+}/reviews")
    public String addNewReview(
            @PathVariable("book") Book reviewBook,
            @AuthenticationPrincipal User currentUser,
            @Valid Review review,
            BindingResult bindingResult,
            Model model
    ) {
        ControllerUtils.isBookExists(reviewBook);

        boolean isAssessmentSelected = isAssessmentSelected(review, model);
        boolean isBindingResultHasErrors = ControllerUtils.mergeErrorsWithModel(bindingResult, model);

        if (isAssessmentSelected && !isBindingResultHasErrors) {
            review.setAuthor(currentUser);
            review.setBook(reviewBook);
            if (reviewService.addNewReview(currentUser.getId(), reviewBook.getId(), review)) {
                return "redirect:/books/" + reviewBook.getId() + "/reviews";
            } else {
                model.addAttribute("reviewError", "You have already written a review of this book");
            }
        }


//        return "forward:/books/" + reviewBook.getId() + "/reviews";
        addAssessmentsToModel(model);
        addBookAndBookReviewsToModel(model, reviewBook);
        model.addAttribute("review", review);
        model.addAttribute("reviewAction", "/books/" + reviewBook.getId() + "/reviews");
        return "review/reviewList";
    }


    @GetMapping("/books/{book:[\\d]+}/reviews/{review:[\\d]+}")
    public String getUserReviewPage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("review") Review review,
            @PathVariable("book") Book book,
            Model model
    ) {
        checkCorrectRequest(book, review, currentUser);

        model.addAttribute("review", review);
        addAssessmentsToModel(model);
        model.addAttribute("reviewAction", "/books/" + book.getId() + "/reviews/" + review.getId());
        return "review/reviewEditPage";
    }

    @PutMapping("/books/{book:[\\d]+}/reviews/{currentReview:[\\d]+}")
    public String editUserReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("book") Book book,
            @PathVariable("currentReview") Review currentReview,
            @Valid Review editedReview,
            BindingResult bindingResult,
            Model model
    ) {
        checkCorrectRequest(book, currentReview, currentUser);

        boolean isBindingResultHasErrors = ControllerUtils.mergeErrorsWithModel(bindingResult, model);
        if (!isBindingResultHasErrors) {
            reviewService.updateUserReview(currentReview, editedReview);
            return "redirect:/books/" + book.getId() + "/reviews";
        }

        addAssessmentsToModel(model);
        model.addAttribute("reviewAction", "/books/" + book.getId() + "/reviews/" + currentReview.getId());
        model.addAttribute("review", editedReview);
        return "review/reviewEditPage";
    }

    @DeleteMapping("/books/{book:[\\d]+}/reviews/{review:[\\d]+}")
    public String deleteReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("book") Book book,
            @PathVariable("review") Review review
    ) {
        checkCorrectRequest(book, review, currentUser);

        reviewService.deleteUserReview(review);

        return "redirect:/books/" + book.getId() + "/reviews";
    }


    private void checkCorrectRequest(Book book, Review review, User currentUser) {
        ControllerUtils.isBookExists(book);
        ControllerUtils.isReviewExists(review);
        reviewService.checkBookContainsReview(book, review);
        reviewService.checkCurrentUserRights(currentUser, review.getAuthor());
    }

    private boolean isAssessmentSelected(Review review, Model model) {
        boolean isAssessmentSelected = review.getAssessment() != null;
        if (!isAssessmentSelected) {
            model.addAttribute("assessmentError", "Please, select an assessment");
        }

        return isAssessmentSelected;
    }

    private void addBookAndBookReviewsToModel(Model model, Book reviewBook) {
        model.addAttribute("reviews", reviewService.getAllBookReviews(reviewBook.getId()));
        model.addAttribute("book", reviewBook);
    }

    private void addAssessmentsToModel(Model model) {
        model.addAttribute("assessments", Assessment.values());
    }

}
