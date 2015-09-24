#include "game.h"
#include "tft_lcd.h"
#include "font.h"
#include "mma8451.h"
#include <stdlib.h>
#include <stdio.h>
#include "sound.h"

PT_T charPos;
PT_T charPos2;
PT_T erasePos1;
PT_T erasePos2;
COLOR_T charColor;
COLOR_T eraseColor;
COLOR_T blockColor;

BLOCK blocks[BLOCKNUM];
int blockIndex=0;
int score = 0;
int highScore = 0;

char buf[16];


//Use roll for left/right

void init_game(void)
{
	int i;
	score = 0;
	
	charPos.X = 110;
	charPos.Y = 300;
	charPos2.X = charPos.X+CHARSIZE;
	charPos2.Y = charPos.Y+CHARSIZE;
	
	erasePos1.X = 0;
	erasePos1.Y = 0;
	erasePos2.X = 240;
	erasePos2.Y = 320;
	
	charColor.R = 100;
	charColor.G = 0;
	charColor.B = 0;
	
	eraseColor.R = 0;
	eraseColor.G = 0;
	eraseColor.B = 0;
	
	blockColor.R = 0;
	blockColor.G = 0;
	blockColor.B = 100;
	
	for(i=0; i < BLOCKNUM-1; i++)
	{
		blocks[i].size = -1;
	}
	DRAWCHAR;
}

#define MOVESPEED 8
#define TILTLIMIT 10

void moveChar(void)
{
	float check;
	check = getRoll();
	if(charPos.X+MOVESPEED >= RIGHTLIMIT && check > TILTLIMIT)
	{
		ERASECHAR;
		charPos.X = 220;
		charPos2.X = 240;
	}
	else if(charPos.X-MOVESPEED > 250 && check < -TILTLIMIT)
	{
		ERASECHAR;
		charPos.X = 0;
		charPos2.X = 20;
	}
	else if(check > TILTLIMIT && charPos.X < RIGHTLIMIT)
	{
		ERASECHAR;
		charPos.X += MOVESPEED;
		charPos2.X += MOVESPEED;
	}
	else if(check < -TILTLIMIT)
	{
		ERASECHAR;
		charPos.X -= MOVESPEED;
		charPos2.X -= MOVESPEED;
	}
	DRAWCHAR;
}

void updateScore(void)
{
	char buffer[16];
	score += 1;
	sprintf(buffer, "Score: %6d", score);
	TFT_Text_PrintStr_RC(0,0, buffer);
}

void spawnBlock(void)
{
	blocks[blockIndex].column = rand()%12;
	blocks[blockIndex].pos1.X = blocks[blockIndex].column * BLOCKWIDTH;
	blocks[blockIndex].pos1.Y = 0;
	blocks[blockIndex].size = rand()%20+40;
	blocks[blockIndex].speed = rand()%10+10;
	blocks[blockIndex].color.R = rand()%90+10;
	blocks[blockIndex].color.G = rand()%90+10;
	blocks[blockIndex].color.B = rand()%90+10;
	blocks[blockIndex].pos2.X = blocks[blockIndex].pos1.X + BLOCKWIDTH;
	blocks[blockIndex].pos2.Y = blocks[blockIndex].pos1.Y + blocks[blockIndex].size;
	//TFT_Fill_Rectangle(&blocks[blockIndex].pos1, &blocks[blockIndex].pos2, &blockColor);
	blockIndex++;
	if(blockIndex >= BLOCKNUM-1) blockIndex = 0;
}

void moveBlock(void)
{
	int i;
	for(i = 0; i < BLOCKNUM-1; i++)
	{
		if(blocks[i].pos2.Y >= 320 && blocks[i].size > 0)
		{
			blocks[i].size = -1;
			TFT_Fill_Rectangle(&blocks[i].pos1, &blocks[i].pos2, &eraseColor);
		}
		if(blocks[i].size > 0)
		{
			TFT_Fill_Rectangle(&blocks[i].pos1, &blocks[i].pos2, &eraseColor);
			blocks[i].pos1.Y += blocks[i].speed;
			blocks[i].pos2.Y += blocks[i].speed;
			TFT_Fill_Rectangle(&blocks[i].pos1, &blocks[i].pos2, &blocks[i].color);
		}
	}
}

int checkCollision(void)
{
	int i;
	for(i=0; i<BLOCKNUM-1; i++)
	{
		if(abs(blocks[i].pos2.X-charPos2.X) <= 20 && blocks[i].pos2.Y >= 300 && blocks[i].size > 0)
		{
			Sound_Disable_Amp();
			if(score > highScore) highScore = score;
			sprintf(buf, "High score: %6d", highScore);
			TFT_Text_PrintStr_RC(6,1, "You lost!");
			TFT_Text_PrintStr_RC(7,1, buf);
			TFT_Text_PrintStr_RC(8,1, "Touch to play again");
			return 1;
		}
	}
	Sound_Enable_Amp();
	return 0;
}

void clearScreen(void)
{
	int i;
	for(i=0; i<BLOCKNUM-1; i++)
	{
		blocks[i].size = -1;
	}
	TFT_Fill_Rectangle(&erasePos1, &erasePos2, &eraseColor);
}
