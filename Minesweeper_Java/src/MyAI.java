

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
	
	// ################### Implement Constructor (required) ####################	
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.ROW_DIMENSIONS = rowDimension;
		this.COL_DIMENSIONS = colDimension;
		this.TOTAL_MINES = this.flagsLeft = totalMines;
	}
	
	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {
		System.out.println(number);


		return new Action(ACTION.LEAVE);
	}

	// ################### Helper Functions Go Here (optional) ##################
	// ...
}
