#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "books.h"

void printBook(int ID);
double avgRating(int ID);
void sortTitle(int a[], int s);
void sortAuthorLast(int a[], int s);
void setSimilar(int ID);
void sortSimilar(int a[]);

struct book{
	char author[40];
	char title[80];
};
struct book sbooks[100];

struct bookRead{
	char everything[121];
};
struct bookRead books[100];

struct user{
	char name[20];
	int ratings[100];
	int nonZero;
	int similar;
};
struct user users[200];

int numBooks=0;
int numUsers=0;

int readBooks(){
	FILE * in;
	int i=0;
	int j=0;
	char *p1;
	in = fopen("books.txt", "r");
	if(in==NULL) return 1;
	while(fgets(books[i].everything, 121, in)!=NULL){
		i++;
	}
	numBooks=i;
	
	for(i=0; i<numBooks; i++){
		p1=&books[i].everything[0];
		j=0;
		while(*p1!=','){
			sbooks[i].author[j]=*p1;
			p1++;
			j++;
		}
		sbooks[i].author[j+1]='\0';
		p1+=2;
		j=0;
		while(*p1!='\0'){
			sbooks[i].title[j]=*p1;
			p1++;
			j++;
		}
		sbooks[i].title[j+1]='\0';
	}
	return 0;
}

int readUsers(){
	FILE * in;
	in = fopen("ratings.txt", "r");
	if(in==NULL) return 1;
	int i=0;
	int j;
	while(fscanf(in, "%s", users[i].name)!=EOF){
		for(j=0; j<numBooks; j++){
			fscanf(in, "%d ", &users[i].ratings[j]);
		}
		i++;
	}
	numUsers=i;
	return 0;
}

void showBooks(){
	int i;
	for(i=0; i<numBooks; i++){
		printBook(i);
	}
	return;
}

void findBook(const char * findThis){
	int IDs[numBooks];
	int i, j=0, k=0;
	int IDsl=0;
	for(i=0; i<numBooks; i++){
		for(j=k; j<numBooks; j++){
			if(strstr(sbooks[j].title, findThis)!= NULL){
				IDs[i]=j;
				IDsl++;
				k=j+1;
				break;
			}
		}
	}
	sortTitle(IDs, IDsl);
	for(i=0; i<IDsl; i++){
		printBook(IDs[i]);
	}
	return;
}

void findAuthor(const char * name){
	int IDs[numBooks];
	int i, j=0, k=0;
	int IDsl=0;
	for(i=0; i<numBooks; i++){
		for(j=k; j<numBooks; j++){
			if(strstr(sbooks[j].author, name)!=NULL){
				IDs[i]=j;
				IDsl++;
				k=j+1;
				break;
			}
		}
	}
	sortAuthorLast(IDs, IDsl);
	for(i=0; i<IDsl; i++){
		printBook(IDs[i]);
	}
	return;
}

int getRec(const char * userName){
	int i, j;
	int userID=-1;
	int count=0;
	int similarUsers[numUsers];
	for(i=0; i<numUsers; i++){
		if(strstr(users[i].name, userName)!=NULL){
			userID=i;
			break;
		}
	}
	if(userID==-1) return 1;
	setSimilar(userID);
	sortSimilar(similarUsers);
	for(i=0; i<numUsers; i++){
		for(j=0; j<numBooks; j++){
			if(users[similarUsers[i]].ratings[j]==5 && users[userID].ratings[j]==0){
				printBook(j);
				count++;
				if(count==5) return 0;
			}
		}
		for(j=0; j<numBooks; j++){
			if(users[similarUsers[i]].ratings[j]==3 && users[userID].ratings[j]==0){
				printBook(j);
				count++;
				if(count==5) return 0;
			}
		}
	}
	return 0;
}

void printBook(int ID){
	printf("ID: %d\nAuthor: %s\nTitle: %s\nAverage Rating: %f\n", ID, sbooks[ID].author, sbooks[ID].title, avgRating(ID));
	return;
}

double avgRating(int ID){
	double total;
	int i;
	for(i=0; i<numUsers; i++){
		total+=users[i].ratings[ID];
	}
	return (double)total/numUsers;
}

void sortTitle(int IDs[], int s){
	int i, j;
	int min;
	int temp;
	for(i=0; i<s-1; i++){
		min=i;
		for(j=i+1; j<s; j++){
			if(strcmp(sbooks[IDs[j]].title, sbooks[IDs[min]].title)<0){
				min=j;
			}
		}
		temp=IDs[i];
		IDs[i]=IDs[min];
		IDs[min]=temp;
	}
	return;
}

void sortAuthorLast(int sortThis[], int s){
	int i, j;
	int min;
	int temp;
	char * authorLast;
	char * authorLast2;
	for(i=0; i<s-1; i++){
		min=i;
		for(j=i+1; j<s; j++){
			authorLast = strrchr(sbooks[sortThis[j]].author, ' ');
			authorLast2 = strrchr(sbooks[sortThis[min]].author, ' ');
			if(strcmp(authorLast, authorLast2)<0){
				min=j;
			}
		}
		temp=sortThis[i];
		sortThis[i]=sortThis[min];
		sortThis[min]=temp;
	}
	return;
}

void setSimilar(int userID){
	int i, j;
	for(i=0; i<numUsers; i++){
		users[i].similar=0;
		users[i].nonZero=0;
		for(j=0; j<numBooks; j++){
			users[i].similar+=(users[i].ratings[j]*users[userID].ratings[j]);
			if(users[i].ratings[j]!=0) users[i].nonZero++;
		}
	}
}

void sortSimilar(int similarUsers[]){
	int i, j;
	int max, temp;
	for(i=0; i<numUsers-1; i++){
		max=i;
		for(j=i+1; j<numUsers; j++){
			if(users[j].similar>users[max].similar){
				if(users[j].similar==users[max].similar){
					if(strcmp(users[j].name, users[max].name)>0) break;
				}
				max=j;
			}
		}
		similarUsers[i]=max;
	}
	return;
}