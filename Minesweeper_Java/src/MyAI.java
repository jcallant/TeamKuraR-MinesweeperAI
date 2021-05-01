

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

import javax.sound.midi.SysexMessage;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.ArrayList;

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
	private LinkedList<int[]> coords;
	private LinkedList<int[]> coordsGreater;
	private ArrayList<String> visited;
	private ArrayList<String> mines;
	private int startX;
	private int startY;
	private int prevX;
	private int prevY;
	
	// ################### Implement Constructor (required) ####################	
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.ROW_DIMENSIONS = rowDimension;
		this.COL_DIMENSIONS = colDimension;
		this.TOTAL_MINES = this.flagsLeft = totalMines;
		this.startX = startX;
		this.startY = startY;
		this.prevX = -1;
		this.prevY = -1;
		coords = new LinkedList<>();
		coordsGreater = new LinkedList<>();
		visited = new ArrayList<>();
		mines = new ArrayList<>();
		coords.add(new int[]{startX, startY});
	}
	
	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {
		if (visited.size() == (ROW_DIMENSIONS*COL_DIMENSIONS)-TOTAL_MINES) {
			return new Action(ACTION.LEAVE);
		}
		if (prevX == -1 && prevY == -1) {
			prevX = startX;
			prevY = startY;
			return new Action(ACTION.UNCOVER, startX, startY);
		}
		System.out.println("mines " + number);
		System.out.println("currently " + prevX + "," + prevY);
		if (!coords.isEmpty()) {
			int[] last = coords.getLast();
			visited.add(prevX+ "," + prevY);
			coords.removeLast();
			if (number == 0) {
				int tLocation = determineBorder(prevX, prevY);
				System.out.println("added neighbors");
				addNeighborsZero(tLocation, prevX, prevY);
			}
			else if (number > 0) {
				coordsGreater.add(new int[]{prevX, prevY});
			}
			prevX = last[0];
			prevY = last[1];
			return new Action(ACTION.UNCOVER, last[0], last[1]);
		}
		// TODO: method for tiles with label > 0, logic is currently wrong
		else if (!coordsGreater.isEmpty()) {
			System.out.println();
			int[] firstGreater = coordsGreater.getFirst();
			coordsGreater.removeFirst(); // probably wrong here
			int tLocation = determineBorder(firstGreater[0], firstGreater[1]);
			ArrayList<int[]> adjacents = getAdjacents(tLocation, firstGreater[0], firstGreater[1]);
			if (adjacents.size() == number) {
				for (int[] tile : adjacents) {
					mines.add(tile[0]+","+tile[1]);
				}
			}
		}
		else {
			return new Action(ACTION.LEAVE);
		}
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

	private void addNeighborsZero(int tLocation, int x, int y) {
		String leftX = Integer.toString(x-1);
		String X = Integer.toString(x);
		String rightX = Integer.toString(x+1);
		String downY = Integer.toString(y-1);
		String Y = Integer.toString(y);
		String upY = Integer.toString(y+1);
		int[] topLeft = {x-1,y+1}; int[] top = {x,y+1}; int[] topRight = {x+1,y+1};
		int[] left = {x-1,y}; 							int[] right = {x+1,y};
		int[] botLeft = {x-1,y-1}; int[] bot = {x,y-1}; int[] botRight = {x+1,y-1};
		switch (tLocation) {
			case 0:
				if (!visited.contains(rightX+ ","+Y)) { coords.add(right); }
				if (!visited.contains(X+","+downY)) { coords.add(bot); }
				if (!visited.contains(rightX+","+downY)) { coords.add(botRight); }
				break;
			case 1:
				if (!visited.contains(leftX+","+Y)) { coords.add(left); }
				if (!visited.contains(rightX+","+Y)) { coords.add(right); }
				if (!visited.contains(leftX+","+downY)) { coords.add(botLeft); }
				if (!visited.contains(X+","+downY)) { coords.add(bot); }
				if (!visited.contains(rightX+","+downY)) { coords.add(botRight); }
				break;
			case 2:
				if (!visited.contains(leftX+","+Y)) { coords.add(left); }
				if (!visited.contains(X+","+downY)) { coords.add(bot); }
				if (!visited.contains(leftX+","+downY)) { coords.add(botLeft); }
				break;
			case 3:
				if (!visited.contains(X+","+upY)) { coords.add(top); }
				if (!visited.contains(rightX+","+upY)) { coords.add(topRight); }
				if (!visited.contains(rightX+","+Y)) { coords.add(right); }
				if (!visited.contains(X+","+downY)) { coords.add(bot); }
				if (!visited.contains(rightX+","+downY)) { coords.add(botRight); }
				break;
			case 4:
				if (!visited.contains(leftX+","+upY)) { coords.add(topLeft); }
				if (!visited.contains(X+","+upY)) { coords.add(top); }
				if (!visited.contains(rightX+","+upY)) { coords.add(topRight); }
				if (!visited.contains(leftX+","+Y)) { coords.add(left); }
				if (!visited.contains(rightX+","+Y)) { coords.add(right); }
				if (!visited.contains(leftX+","+downY)) { coords.add(botLeft); }
				if (!visited.contains(X+","+downY)) { coords.add(bot); }
				if (!visited.contains(rightX+","+downY)) { coords.add(botRight); }
				break;
			case 5:
				if (!visited.contains(leftX+","+upY)) { coords.add(topLeft); }
				if (!visited.contains(X+","+upY)) { coords.add(top); }
				if (!visited.contains(leftX+","+Y)) { coords.add(left); }
				if (!visited.contains(leftX+","+downY)) { coords.add(botLeft); }
				if (!visited.contains(X+","+downY)) { coords.add(bot); }
				break;
			case 6:
				if (!visited.contains(X+","+upY)) { coords.add(top); }
				if (!visited.contains(rightX+","+upY)) { coords.add(topRight); }
				if (!visited.contains(rightX+","+Y)) { coords.add(right); }
				break;
			case 7:
				if (!visited.contains(leftX+","+upY)) { coords.add(topLeft); }
				if (!visited.contains(X+","+upY)) { coords.add(top); }
				if (!visited.contains(rightX+","+upY)) { coords.add(topRight); }
				if (!visited.contains(leftX+","+Y)) { coords.add(left); }
				if (!visited.contains(rightX+","+Y)) { coords.add(right); }
				break;
			case 8:
				if (!visited.contains(leftX+","+upY)) { coords.add(topLeft); }
				if (!visited.contains(X+","+upY)) { coords.add(top); }
				if (!visited.contains(leftX+","+Y)) { coords.add(left); }
				break;
			default:
				break;
		}
	}

	private ArrayList<int[]> getAdjacents(int tLocation, int x, int y) {
		ArrayList<int[]> adjacents = new ArrayList<>();
		String leftX = Integer.toString(x-1);
		String X = Integer.toString(x);
		String rightX = Integer.toString(x+1);
		String downY = Integer.toString(y-1);
		String Y = Integer.toString(y);
		String upY = Integer.toString(y+1);
		int[] topLeft = {x-1,y+1}; int[] top = {x,y+1}; int[] topRight = {x+1,y+1};
		int[] left = {x-1,y}; 							int[] right = {x+1,y};
		int[] botLeft = {x-1,y-1}; int[] bot = {x,y-1}; int[] botRight = {x+1,y-1};
		switch (tLocation) {
			case 0:
				if (!visited.contains(rightX+ ","+Y)) { adjacents.add(right); }
				if (!visited.contains(X+","+downY)) { adjacents.add(bot); }
				if (!visited.contains(rightX+","+downY)) { adjacents.add(botRight); }
				break;
			case 1:
				if (!visited.contains(leftX+","+Y)) { adjacents.add(left); }
				if (!visited.contains(rightX+","+Y)) { adjacents.add(right); }
				if (!visited.contains(leftX+","+downY)) { adjacents.add(botLeft); }
				if (!visited.contains(X+","+downY)) { adjacents.add(bot); }
				if (!visited.contains(rightX+","+downY)) { adjacents.add(botRight); }
				break;
			case 2:
				if (!visited.contains(leftX+","+Y)) { adjacents.add(left); }
				if (!visited.contains(X+","+downY)) { adjacents.add(bot); }
				if (!visited.contains(leftX+","+downY)) { adjacents.add(botLeft); }
				break;
			case 3:
				if (!visited.contains(X+","+upY)) { adjacents.add(top); }
				if (!visited.contains(rightX+","+upY)) { adjacents.add(topRight); }
				if (!visited.contains(rightX+","+Y)) { adjacents.add(right); }
				if (!visited.contains(X+","+downY)) { adjacents.add(bot); }
				if (!visited.contains(rightX+","+downY)) { adjacents.add(botRight); }
				break;
			case 4:
				if (!visited.contains(leftX+","+upY)) { adjacents.add(topLeft); }
				if (!visited.contains(X+","+upY)) { adjacents.add(top); }
				if (!visited.contains(rightX+","+upY)) { adjacents.add(topRight); }
				if (!visited.contains(leftX+","+Y)) { adjacents.add(left); }
				if (!visited.contains(rightX+","+Y)) { adjacents.add(right); }
				if (!visited.contains(leftX+","+downY)) { adjacents.add(botLeft); }
				if (!visited.contains(X+","+downY)) { adjacents.add(bot); }
				if (!visited.contains(rightX+","+downY)) { adjacents.add(botRight); }
				break;
			case 5:
				if (!visited.contains(leftX+","+upY)) { adjacents.add(topLeft); }
				if (!visited.contains(X+","+upY)) { adjacents.add(top); }
				if (!visited.contains(leftX+","+Y)) { adjacents.add(left); }
				if (!visited.contains(leftX+","+downY)) { adjacents.add(botLeft); }
				if (!visited.contains(X+","+downY)) { adjacents.add(bot); }
				break;
			case 6:
				if (!visited.contains(X+","+upY)) { adjacents.add(top); }
				if (!visited.contains(rightX+","+upY)) { adjacents.add(topRight); }
				if (!visited.contains(rightX+","+Y)) { adjacents.add(right); }
				break;
			case 7:
				if (!visited.contains(leftX+","+upY)) { adjacents.add(topLeft); }
				if (!visited.contains(X+","+upY)) { adjacents.add(top); }
				if (!visited.contains(rightX+","+upY)) { adjacents.add(topRight); }
				if (!visited.contains(leftX+","+Y)) { adjacents.add(left); }
				if (!visited.contains(rightX+","+Y)) { adjacents.add(right); }
				break;
			case 8:
				if (!visited.contains(leftX+","+upY)) { adjacents.add(topLeft); }
				if (!visited.contains(X+","+upY)) { adjacents.add(top); }
				if (!visited.contains(leftX+","+Y)) { adjacents.add(left); }
				break;
			default:
				break;
		}
		return adjacents;
	}
}
