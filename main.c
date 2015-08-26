/* 
 * File:   main.c
 * Author: gbyrd
 * 
 * This file contains the main function for Program 3.  It implements
 * a simple user interface for the book recommendation system.  This version
 * only supports one user, entered when the program starts.
 * 
 * Commands are:
 *   b: print all books, sorted by ID number
 *   t: print all books with a matching string in title, sorted by title
 *   a: print all books with a matching string in author, sorted by author
 *   ?: print recommendations for user
 *   h: help -- print commands
 *   q: quit
 *
 * Created on October 26, 2013, 2:37 PM
 */

#include <stdio.h>
#include <stdlib.h>

#include "books.h"
void randomize();

int main() {
    
    int status;
    char command;
    char inString[81];
    int id, rating;
    char userName[21];
    
    printf("Loading book database...\n");
    status = readBooks();
    if (status != 0) {
        printf("ERROR reading books\n");
        return EXIT_FAILURE;
    }
    printf("\nLoading user database...\n");
    status = readUsers();
    if (status != 0) {
        printf("ERROR reading users\n");
        return EXIT_FAILURE;
    }
    
    printf("\n\nWelcome to the Book Recommendation System\n");
    
    /* User enters the username that will be used to generate recs */
    printf("\nEnter your username (no spaces): ");
    scanf("%20s%*c", userName);
    
    /* NOTE: The %*c in the format strings will consume the next  
     * character from the user's input, which should be a linefeed.
     * This is important because we are using %c to read one character 
     * in the code below.  There's not much protection against stupid
     * users, so don't be one -- type a legal input followed by ENTER.
     */
    
    /* user interface is infinite loop, will end with 'q' command */
    while (1) {
        
        printf("\nEnter a command (q to quit, h to list commands): " );
        scanf("%c%*c", &command);
        
        /* COMMAND: q = quit */
        if (command == 'q') break;  /* break out of infinite loop */
        
        /* COMMAND: h = help */
        else if (command == 'h') {
            printf("b = print all books\n");
            printf("t = find book by title\n");
            printf("a = find book by author\n");
            printf("? = get recommended books\n");
            printf("q = quit\n");
            printf("h = help\n");
        }
        
        /* COMMAND: b = print books */
        else if (command == 'b') {
            showBooks();
        }
        
        /* COMMAND: t = title search */
        else if (command == 't') {
            printf("Enter a string (no spaces) to match against title: ");
            scanf("%80s%*c", inString);
            findBook(inString);
        }
        
        /* COMMAND: a = author search */
        else if (command == 'a') {
            printf("Enter a string (no spaces) to match against author: ");
            scanf("%40s%*c", inString);
            findAuthor(inString);
        }
        
        /* COMMAND: ? = get recommendation */
        else if (command == '?') {
            if (getRec(userName) != 0) {
                printf("User %s not found.\n", userName);
            }
        }
        
        /* if no match, command is ignored */
        
    }

    return (EXIT_SUCCESS);
}

