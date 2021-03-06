package com.spring.library.controller;

import com.spring.library.domain.Book;
import com.spring.library.domain.Genre;
import com.spring.library.domain.Writer;
import com.spring.library.service.BookService;
import com.spring.library.service.WriterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return "book/bookAddPage";
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

        model.addAttribute("genres", Genre.values());
        model.addAttribute("writers", writerService.getWriterList());
        return "book/bookAddPage";
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
        if (!isWriterSelected) {
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
        model.addAttribute("currentBook", book);
        model.addAttribute("editedBook", book);
        return "book/bookEditPage";
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

        boolean isCorrectBookForm = isCorrectBookForm(selectedGenres, writer,
                editedBook.getPublicationDate(), bindingResult, model);
        if (isCorrectBookForm) {
            /* you don't need to change the poster, but if you did, it must be correct */
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
        model.addAttribute("currentBook", currentBook);
        model.addAttribute("editedBook", editedBook);
        return "book/bookEditPage";
    }

    @DeleteMapping("/books/{book:[\\d]+}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String deleteBook(@PathVariable Book book) {
        ControllerUtils.isBookExists(book);

        bookService.deleteBook(book);

        return "redirect:/books";
    }


}
