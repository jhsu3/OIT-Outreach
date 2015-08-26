/* 
 * File:   books.h
 * Author: gbyrd
 * 
 * These are the functions that MUST be implemented in books.c.
 * See the specification document for detailed information.
 *
 * Created on October 26, 2013, 2:38 PM
 */



int readBooks(); /* read books.txt file, return 0 if success, 1 if error */
int readUsers(); /* read ratings.txt file, return 0 if success, 1 if error */

void showBooks();  /* print all books, ordered by id number */
/* note: id number is determined by the order of books in the books.txt file */

/* print all books that contain nameStr in title, sorted by title */
void findBook(const char* nameStr);

/* print all books that contain authStr in author (either name), 
 * sorted by author last name */
void findAuthor(const char* authStr);

/* print top five recommended books for specified user */
/* return 0 if success, 1 if error (e.g., no such user) */
int getRec(const char* userName);

