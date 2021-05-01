

/*

AUTHOR:      John Lu

DESCRIPTION: This file contains your agent class, which you will
             implement.

NOTES:       - If you are having trouble understanding how the shell
               works, look at the other parts of the code, as well as
               the documentation.

             - You are only allowed to make changes to this portion of
               the code. Any changes to other portions of the code will
               be lost when the tournament runs your code.
*/

package src;
import src.Action.ACTION;
import  java.util.LinkedList;

public class MyAI extends AI {
	// ########################## INSTRUCTIONS ##########################
	// 1) The Minesweeper Shell will pass in the board size, number of mines
	// 	  and first move coordinates to your agent. Create any instance variables
	//    necessary to store these variables.
	//
	// 2) You MUST implement the getAction() method which has a single parameter,
	// 	  number. If your most recent move is an Action.UNCOVER action, this value will
	//	  be the number of the tile just uncovered. If your most recent move is
	//    not Action.UNCOVER, then the value will be -1.
	// 
	// 3) Feel free to implement any helper functions.
	//
	// ###################### END OF INSTURCTIONS #######################
	
	// This line is to remove compiler warnings related to using Java generics
	// if you decide to do so in your implementation.
	@SuppressWarnings("unchecked")

	/* dev in LOCAL -> test in OPENLAB
	 *
	 * LOCAL
	 * 1. make changes to MyAI.java
	 * 2. git add MyAI.java
	 * 3. git commit -m
	 * 4. git push
	 *
	 * in OPENLAB
	 * 1. git branch/checkout (make sure in correct branch)
	 * 1. git pull
	 * 2. check changes
	 * 3. test
	 *
	 */

	private final int ROW_DIMENSIONS;
	private final int COL_DIMENSIONS;
	private final int TOTAL_MINES;
	private int flagsLeft;
	private int x;
	private int y;
	private LinkedList<int[]> coords;

	
	// ################### Implement Constructor (required) ####################	
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.ROW_DIMENSIONS = rowDimension;
		this.COL_DIMENSIONS = colDimension;
		this.TOTAL_MINES = this.flagsLeft = totalMines;
		x = startX;
		y = startY;
		coords = new LinkedList<>();
	}
	
	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {
		if (number == 0) {
			int tLocation = determineBorder(x, y);
			addNeighborsZero(tLocation);
		}
		for (int[] a : coords) {
			System.out.print(a[0]);
			System.out.print(" ");
			System.out.println(a[1]);
		}


		return new Action(ACTION.LEAVE);
	}

	// ################### Helper Functions Go Here (optional) ##################
	private int determineBorder(int x, int y) {
		if (y == ROW_DIMENSIONS) {
			if (x == 1) 							{ return 0; } // top left corner
			else if (x > 1 && x < COL_DIMENSIONS) 	{ return 1; } // middle top border
			else if (x == COL_DIMENSIONS) 			{ return 2; } // top right corner
		}
		else if (y > 1 && y < ROW_DIMENSIONS) {
			if (x == 1) 							{ return 3; } // middle left border
			else if (x > 1 && x < COL_DIMENSIONS) 	{ return 4; } // not a border piece
			else if (x == COL_DIMENSIONS) 			{ return 5; } // middle right border
		}
		else if (y == 1) {
			if (x == 1) 							{ return 6; } // bot left corner
			else if (x > 1 && x < COL_DIMENSIONS) 	{ return 7; } // middle bot border
			else if (x == COL_DIMENSIONS) 			{ return 8; } // bot right corner
		}
		return 9; // should never be reached
	}

	private void addNeighborsZero(int tLocation) {
		switch (tLocation) {
			case 0:
				coords.add(new int[]{x+1,y});	// right
				coords.add(new int[]{x,y-1});	// bot
				coords.add(new int[]{x+1,y-1});	// bot right
				break;
			case 1:
				coords.add(new int[]{x-1,y});	// left
				coords.add(new int[]{x+1,y});	// right
				coords.add(new int[]{x-1,y-1});	// bot left
				coords.add(new int[]{x,y-1});	// bot
				coords.add(new int[]{x+1,y-1});	// bot right
				break;
			case 2:
				coords.add(new int[]{x-1,y});	// left
				coords.add(new int[]{x,y-1});	// bot
				coords.add(new int[]{x-1,y-1});	// bot left
				break;
			case 3:
				coords.add(new int[]{x, y+1});	// top
				coords.add(new int[]{x+1,y+1});	// top right
				coords.add(new int[]{x+1,y});	// right
				coords.add(new int[]{x,y-1});	// bot
				coords.add(new int[]{x+1,y-1});	// bot right
				break;
			case 4:
				coords.add(new int[]{x-1,y+1}); // top left
				coords.add(new int[]{x, y+1});	// top
				coords.add(new int[]{x+1,y+1});	// top right
				coords.add(new int[]{x-1,y});	// left
				coords.add(new int[]{x+1,y});	// right
				coords.add(new int[]{x-1,y-1});	// bot left
				coords.add(new int[]{x,y-1});	// bot
				coords.add(new int[]{x+1,y-1});	// bot right
				break;
			case 5:
				coords.add(new int[]{x-1,y+1}); // top left
				coords.add(new int[]{x, y+1});	// top
				coords.add(new int[]{x-1,y});	// left
				coords.add(new int[]{x-1,y-1});	// bot left
				coords.add(new int[]{x,y-1});	// bot
				break;
			case 6:
				coords.add(new int[]{x, y+1});	// top
				coords.add(new int[]{x+1,y+1});	// top right
				coords.add(new int[]{x+1,y});	// right
				break;
			case 7:
				coords.add(new int[]{x-1,y+1}); // top left
				coords.add(new int[]{x, y+1});	// top
				coords.add(new int[]{x+1,y+1});	// top right
				coords.add(new int[]{x-1,y});	// left
				coords.add(new int[]{x+1,y});	// right
				break;
			case 8:
				coords.add(new int[]{x-1,y+1}); // top left
				coords.add(new int[]{x, y+1});	// top
				coords.add(new int[]{x-1,y});	// left
				break;
			default:
				break;
		}
	}
}
