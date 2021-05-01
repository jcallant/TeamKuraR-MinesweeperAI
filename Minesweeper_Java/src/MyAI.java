

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

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
	private int currX;
	private int currY;
	private int lastX;
	private int lastY;
	private HashMap<String,Integer> records;
	private ArrayList<Action> safeTiles;
	private ArrayList<Action> frontier;


	// ################### Implement Constructor (required) ####################	
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.ROW_DIMENSIONS = rowDimension;
		this.COL_DIMENSIONS = colDimension;
		this.TOTAL_MINES = this.flagsLeft = totalMines;
		this.currX = startX;
		this.currY = startY;
		this.lastX = startX;
		this.lastY = startY;
		this.records = new HashMap<>();
		this.safeTiles = new ArrayList<>();
		this.frontier = new ArrayList<>();
	}

	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {
		// store value in records
		String s = key(currX,currY);
		records.put(s, number);
		System.out.println(s + ": " + number);

		// add neighbors to frontier
		if(number == 0)
			addNeighborsToSafeTiles(currX,currY);
		else
			addNeighborsToFrontier(currX,currY);
		System.out.println("\n" + records);
		System.out.println("\n" + safeTiles);

		// while safe neighbors to uncover...
		if(!safeTiles.isEmpty()){
			Action a = safeTiles.remove(0);
			currX = a.x;
			currY = a.y;
			return a;
		}

		// while frontier has tiles
		if(!frontier.isEmpty()){
			Action a = frontier.remove(0);
			while(records.get(key(a.x,a.y))==0)
				a = frontier.remove(0);
			currX = a.x;
			currY = a.y;
			a = findBestAction(currX, currY);
			return a;
		}



		return new Action(ACTION.LEAVE);
	}

	// ################### Helper Functions Go Here (optional) ##################
	private String key(int x, int y){
		return "(" + x + "," + y + ")";
	}
	private void addNeighborsToSafeTiles(int x, int y){
		int rowMin = y-1;
		int rowMax = y+1;
		if(rowMin<1) rowMin = 1;
		if(rowMax>ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

		int colMin = x-1;
		int colMax = x+1;
		if(colMin<1) colMin = 1;
		if(colMax>COL_DIMENSIONS) colMax = COL_DIMENSIONS;

		for(int j=rowMax; j>rowMin-1; j--){
			for(int i=colMin; i<colMax+1; i++) {
				if (j==currY && i==currX) continue;
				String k = key(i, j);
				System.out.println(k);
				if (!records.containsKey(k)) {
					records.put(k, -1);
					safeTiles.add(new Action(ACTION.UNCOVER, i, j));
				}
				else if(records.get(k)==-2) {
					records.put(k, -1);
					safeTiles.add(new Action(ACTION.UNCOVER, i, j));
				}
			}
		}
	}

	private void addNeighborsToFrontier(int x, int y){
		int rowMin = y-1;
		int rowMax = y+1;
		if(rowMin<1) rowMin = 1;
		if(rowMax>ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

		int colMin = x-1;
		int colMax = x+1;
		if(colMin<1) colMin = 1;
		if(colMax>COL_DIMENSIONS) colMax = COL_DIMENSIONS;

		for(int j=rowMax; j>rowMin-1; j--){
			for(int i=colMin; i<colMax+1; i++) {
				if (j==currY && i==currX) continue;
				String k = key(i, j);
				System.out.println(k);
				if (!records.containsKey(k)) {
					records.put(k, -2);
					frontier.add(new Action(ACTION.FLAG, i, j));
				}
			}
		}
	}

	private Action findBestAction(int x, int y){
		return new Action(ACTION.FLAG, x, y);
	}
}
