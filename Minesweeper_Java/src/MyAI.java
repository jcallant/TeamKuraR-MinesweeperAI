

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
import java.util.HashSet;
import java.util.Set;

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
	private HashMap<String,Integer> records;
	private ArrayList<Action> guaranteedSafe;
	private Set<String> guaranteedSafeByFlag;
	private ArrayList<Action> guaranteedMine;
	private ArrayList<Action> coveredFrontier;
	private ArrayList<Action> uncoveredFrontier;
	private double elapsedTime;


	// ################### Implement Constructor (required) ####################	
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.ROW_DIMENSIONS = rowDimension;
		this.COL_DIMENSIONS = colDimension;
		this.TOTAL_MINES = this.flagsLeft = totalMines;
		this.currX = startX;
		this.currY = startY;
		this.records = new HashMap<>();
		this.guaranteedSafe = new ArrayList<>();
		this.guaranteedSafeByFlag = new HashSet<>();
		this.guaranteedMine = new ArrayList<>();
		this.coveredFrontier = new ArrayList<>();
		this.uncoveredFrontier = new ArrayList<>();
	}

	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {

		// store valid value in records
		if(number >= 0) {
			String k = key(currX, currY);
			records.put(k, number);
			records.put(k, refreshLabel(currX,currY));
			number = records.get(k);
			System.out.println(k + ": " + number);

			// add neighbors to frontier
			if (number == 0)
				addNeighborsToSafeTiles(currX, currY);
			else {
				addNeighborsToCoveredFrontier(currX, currY);
				addSelfToUncoveredFrontier(currX, currY);
			}
		}

		System.out.println("\n records: " + records);
		System.out.println("\n safe: " + guaranteedSafe);
		System.out.println("\n ucsafeset: " + guaranteedSafeByFlag);
		System.out.println("\n ucfrontier: " + uncoveredFrontier);

		// while mines to flag...
		if(!guaranteedMine.isEmpty()){
			Action a = guaranteedMine.remove(0);
			currX = a.x;
			currY = a.y;
			return a;
		}

		// while safe neighbors to uncover...
		if(!guaranteedSafe.isEmpty()){
			Action a = guaranteedSafe.remove(0);
			currX = a.x;
			currY = a.y;
			return a;
		}

		// while uncovered frontier has tiles
		while(!uncoveredFrontier.isEmpty()){
			Action a = uncoveredFrontier.remove(0);
			int label = records.get(key(a.x,a.y));

			// if no adjacent mines or is mine itself, pick another
			while((label == -3)) {
				if(uncoveredFrontier.isEmpty()) return new Action(ACTION.LEAVE);
				a = uncoveredFrontier.remove(0);
				label = records.get(key(a.x,a.y));
			}

			// if label matches the number of adjacent covered tiles
			ArrayList<Action> possible = countCoveredNeighbors(a.x,a.y);
			System.out.println(key(a.x,a.y) + " ucn: " + possible.size());
			if(possible.size() <= records.get(key(a.x,a.y))){
				System.out.println("--match");

				// flag each tile as a mine and update labels of adjacent tiles for each mine
				flagAndUpdate(possible, a.x, a.y);

				// flag mines if found
				if (!guaranteedMine.isEmpty()) {
					a = guaranteedMine.remove(0);
					currX = a.x;
					currY = a.y;
					return a;
				}

				// uncover safe if found
				else if (!guaranteedSafe.isEmpty()) {
					a = guaranteedSafe.remove(0);
					currX = a.x;
					currY = a.y;
					return a;
				}
			}
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
				if (!records.containsKey(k) || records.get(k)==-1) {
					System.out.println(k + " added to safe");
					records.put(k, 0);
					guaranteedSafe.add(new Action(ACTION.UNCOVER, i, j));
				}
			}
		}
	}

	private void addNeighborsToCoveredFrontier(int x, int y){
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
				if (!records.containsKey(k)) {
					System.out.println(k + " added to covered frontier");
					records.put(k, -1);
					coveredFrontier.add(new Action(ACTION.FLAG, i, j));
				}
			}
		}
	}

	private void addSelfToUncoveredFrontier(int x, int y){
		System.out.println(key(x,y) + " added to uncovered frontier");
		uncoveredFrontier.add(new Action(ACTION.FLAG, x, y));
	}

	private ArrayList<Action> countCoveredNeighbors(int x, int y){
		int rowMin = y-1;
		int rowMax = y+1;
		if(rowMin<1) rowMin = 1;
		if(rowMax>ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

		int colMin = x-1;
		int colMax = x+1;
		if(colMin<1) colMin = 1;
		if(colMax>COL_DIMENSIONS) colMax = COL_DIMENSIONS;

		ArrayList<Action> possible = new ArrayList<>();
		for(int j=rowMax; j>rowMin-1; j--){
			for(int i=colMin; i<colMax+1; i++) {
				if (j==currY && i==currX) continue;
				String k = key(i, j);
				if (records.get(k)==-1)
					possible.add(new Action(ACTION.FLAG, i, j));
			}
		}
		return possible;
	}

	private void flagAndUpdate(ArrayList<Action> flags, int x, int y){

		// for each guaranteed mine
		for (Action f : flags) {
//			Set<String> updated = new HashSet<>();

			// flag the mine
			System.out.println("flag: " + key(f.x,f.y));
			records.put(key(f.x,f.y),-3);
			guaranteedMine.add(f);

			// update labels for neighboring tiles
			int rowMin = f.y - 1;
			int rowMax = f.y + 1;
			if (rowMin < 1) rowMin = 1;
			if (rowMax > ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

			int colMin = f.x - 1;
			int colMax = f.x + 1;
			if (colMin < 1) colMin = 1;
			if (colMax > COL_DIMENSIONS) colMax = COL_DIMENSIONS;

			for (int j = rowMax; j > rowMin - 1; j--) {
				for (int i = colMin; i < colMax + 1; i++) {
					String k = key(i, j);
					if (records.containsKey(k) /*&& !updated.contains(k)*/) {
						if (records.get(k)>0){
							int labelValue = records.get(k);
							labelValue--;
							records.put(k, labelValue);
							System.out.println("update: " + k + " = " + (records.get(k)+1) + " -> " + records.get(k));

							// if new label == 0, uncover any remaining covered neighbors
							if (labelValue == 0) {
								//System.out.println(" neighbors added");
								addNeighborsToSafeTiles(i, j); // FIX THIS PATH; UPDATE LABEL AFTER UNCOVER
							}
						}
					}
				}
			}
		}
	}

	private int refreshLabel(int x, int y){
		int rowMin = y - 1;
		int rowMax = y + 1;
		if (rowMin < 1) rowMin = 1;
		if (rowMax > ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

		int colMin = x - 1;
		int colMax = x + 1;
		if (colMin < 1) colMin = 1;
		if (colMax > COL_DIMENSIONS) colMax = COL_DIMENSIONS;

		int label = records.get(key(x,y));
		for (int j = rowMax; j > rowMin - 1; j--) {
			for (int i = colMin; i < colMax + 1; i++) {
				String k = key(i, j);
				if (records.containsKey(k) /*&& !updated.contains(k)*/) {
					if (records.get(k) == -3)
						label--;
				}
			}
		}
		return label;
	}

	private Action findBestAction(int x, int y){
		return new Action(ACTION.FLAG, x, y);
	}
}
