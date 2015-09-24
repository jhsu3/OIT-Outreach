#include <stdint.h>
#include <stdio.h>
#include <RTL.h>
#include <MKL25Z4.h>

#include "TFT_LCD.h"
#include "font.h"
#include "tasks.h"
#include "MMA8451.h"
#include "sound.h"
#include "DMA.h"
#include "gpio_defs.h"
#include "game.h"
#include "delay.h"


U64 RA_Stack[64];

OS_TID t_Read_TS, t_Read_Accelerometer, t_Sound_Manager, t_US, t_Refill_Sound_Buffer,t_Update_Game, t_Spawn_Block;
OS_MUT LCD_mutex;
OS_MUT TS_mutex;

int runFlag=0;
int clearFlag=0;

void Init_Debug_Signals(void) {
	// Enable clock to port B
	SIM->SCGC5 |= SIM_SCGC5_PORTB_MASK;
	
	// Make pins GPIO
	PORTB->PCR[DEBUG_T0_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[DEBUG_T0_POS] |= PORT_PCR_MUX(1);          
	PORTB->PCR[DEBUG_T1_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[DEBUG_T1_POS] |= PORT_PCR_MUX(1);          
	PORTB->PCR[DEBUG_T2_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[DEBUG_T2_POS] |= PORT_PCR_MUX(1);          
	PORTB->PCR[DEBUG_T3_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[DEBUG_T3_POS] |= PORT_PCR_MUX(1);          

	PORTB->PCR[DEBUG_I0_POS] &= ~PORT_PCR_MUX_MASK;          
	PORTB->PCR[DEBUG_I0_POS] |= PORT_PCR_MUX(1);          

	
	// Set ports to outputs
	PTB->PDDR |= MASK(DEBUG_T0_POS);
	PTB->PDDR |= MASK(DEBUG_T1_POS);
	PTB->PDDR |= MASK(DEBUG_T2_POS);
	PTB->PDDR |= MASK(DEBUG_T3_POS);
	PTB->PDDR |= MASK(DEBUG_I0_POS);
	
	// Initial values are 0
	PTB->PCOR = MASK(DEBUG_T0_POS);
	PTB->PCOR = MASK(DEBUG_T1_POS);
	PTB->PCOR = MASK(DEBUG_T2_POS);
	PTB->PCOR = MASK(DEBUG_T3_POS);
	PTB->PCOR = MASK(DEBUG_I0_POS);

}	

__task void Task_Init(void) {
	os_mut_init(&LCD_mutex);

	t_Read_TS = os_tsk_create(Task_Read_TS, 4);
	t_Read_Accelerometer = os_tsk_create_user(Task_Read_Accelerometer, 3, RA_Stack, 512);
	t_Sound_Manager = os_tsk_create(Task_Sound_Manager, 2); // Should be lower priority than Refill Sound Buffer
	t_Update_Game = os_tsk_create(Task_Update_Game, 1);
	t_Spawn_Block = os_tsk_create(Task_Spawn_Block, 1);
	//t_US = os_tsk_create(Task_Update_Screen, 5);
	t_Refill_Sound_Buffer = os_tsk_create(Task_Refill_Sound_Buffer, 1);

  os_tsk_delete_self ();
}

__task void Task_Read_TS(void) {
	PT_T p;
//	COLOR_T c;
	
	os_itv_set(TASK_READ_TS_PERIOD_TICKS);
	
	while (1) {
		os_itv_wait();
		PTB->PSOR = MASK(DEBUG_T1_POS);
		if (TFT_TS_Read(&p)) {
			if(!runFlag)
				{			
				runFlag = 1;
				clearFlag = 0;
				clearScreen();
				init_game();
			}			
			else
			{
				if(!clearFlag)
				{
					clearScreen();
					TFT_Text_PrintStr_RC(8,7, "Clear!");
					Delay(100);
					clearScreen();
				}
				clearFlag = 1;
			}

		PTB->PCOR = MASK(DEBUG_T1_POS);
		}
	}
}

__task void Task_Read_Accelerometer(void) {	
	os_itv_set(TASK_READ_ACCELEROMETER_PERIOD_TICKS);
	while (1) {
os_itv_wait();
		PTB->PSOR = MASK(DEBUG_T0_POS);
		read_full_xyz();
		convert_xyz_to_roll_pitch();
#if 0
		sprintf(buffer, "X: %6d", acc_X);
		TFT_Text_PrintStr_RC(2, 0, buffer);
		sprintf(buffer, "Y: %6d", acc_Y);
		TFT_Text_PrintStr_RC(3, 0, buffer);
		sprintf(buffer, "Z: %6d", acc_Z);
		TFT_Text_PrintStr_RC(4, 0, buffer);
#endif
		PTB->PCOR = MASK(DEBUG_T0_POS);
	}
}
__task void Task_Update_Game(void)
{
	os_itv_set(TASK_UPDATE_GAME_TICKS);
	while(1)
	{
		os_itv_wait();
		if(runFlag)
		{
			read_full_xyz();
			convert_xyz_to_roll_pitch();
			os_mut_wait(&LCD_mutex, WAIT_FOREVER);
			moveChar();
			os_mut_release(&LCD_mutex);
			os_mut_wait(&LCD_mutex, WAIT_FOREVER);
			moveBlock();
			os_mut_release(&LCD_mutex);
			updateScore();
			
			if(checkCollision())
			{
				runFlag = 0;
				Sound_Disable_Amp();
			}
		}
	}
}

__task void Task_Spawn_Block(void)
{
	os_itv_set(TASK_SPAWN_BLOCK);
	while(1)
	{
		os_itv_wait();
		if(runFlag)
		{
			os_mut_wait(&LCD_mutex, WAIT_FOREVER);
			spawnBlock();
			os_mut_release(&LCD_mutex);
		}
	}
}





__task void Task_Update_Screen(void) {
	int16_t paddle_pos=TFT_WIDTH/2;
	PT_T p1, p2;
	COLOR_T paddle_color, black;
	
	paddle_color.R = 100;
	paddle_color.G = 200;
	paddle_color.B = 50;

	black.R = 0;
	black.G = 0;
	black.B = 0;
	
	os_itv_set(TASK_UPDATE_SCREEN_PERIOD_TICKS);

	while (1) {
		os_itv_wait();
		PTB->PSOR = MASK(DEBUG_T3_POS);
		
		if ((roll < -2.0) || (roll > 2.0)) {
			p1.X = paddle_pos;
			p1.Y = PADDLE_Y_POS;
			p2.X = p1.X + PADDLE_WIDTH;
			p2.Y = p1.Y + PADDLE_HEIGHT;
			TFT_Fill_Rectangle(&p1, &p2, &black); 		
			
			paddle_pos += roll;
			paddle_pos = MAX(0, paddle_pos);
			paddle_pos = MIN(paddle_pos, TFT_WIDTH-1);
			
			p1.X = paddle_pos;
			p1.Y = PADDLE_Y_POS;
			p2.X = p1.X + PADDLE_WIDTH;
			p2.Y = p1.Y + PADDLE_HEIGHT;
			TFT_Fill_Rectangle(&p1, &p2, &paddle_color); 		
		}
		
		PTB->PCOR = MASK(DEBUG_T3_POS);
	}
}
