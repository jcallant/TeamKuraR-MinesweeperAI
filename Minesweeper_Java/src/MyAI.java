

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

import java.util.*;
import java.util.stream.Collectors;

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
	private ArrayList<Action> guaranteedMine;
	private ArrayList<Action> coveredFrontier;
	private ArrayList<Action> uncoveredFrontier;
	private HashMap<String, Integer> probability;
	private double elapsedTime;

	// label value constants
	private final int MINE = -9;
	private final int COV_NEIGHBOR = -10;


	// ################### Implement Constructor (required) ####################	
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.ROW_DIMENSIONS = rowDimension;
		this.COL_DIMENSIONS = colDimension;
		this.TOTAL_MINES = this.flagsLeft = totalMines;
		this.currX = startX;
		this.currY = startY;
		this.records = new HashMap<>();
		this.guaranteedSafe = new ArrayList<>();
		this.guaranteedMine = new ArrayList<>();
		this.coveredFrontier = new ArrayList<>();
		this.uncoveredFrontier = new ArrayList<>();
		this.probability = new HashMap<>();
	}

	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {

		// [STEP1]: store uncovered value in records
		if(number >= 0) {
			String k = key(currX, currY);

			// put value in records
			records.put(k, number);

			// refresh value if any neighbors are flagged mines
			records.put(k, refreshLabel(currX,currY));

			// assign fresh value
			number = records.get(k);
			//System.out.println(k + ": " + number);

			// if label = 0, all neighbors are safe
			if (number == 0)
				addNeighborsToSafeTiles(currX, currY);

			else {
				addNeighborsToCoveredFrontier(currX, currY);
				addSelfToUncoveredFrontier(currX, currY);
			}
		}


		// output all details
		//outputKnowledge();


		// [STEP2]: if any guaranteed mines or safe tiles
		Action guaranteedAction = handleGuaranteed();
		if(guaranteedAction != null) return guaranteedAction;


		// [STEP3]: use uncovered frontier to gain new knowledge
		//System.out.println("\nPicking from ucf...");
		// probability map used to record probabilities of covered tiles
		probability = new HashMap<>();
		Iterator<Action> it = uncoveredFrontier.iterator();
		while(it.hasNext()){
			//Action a = uncoveredFrontier.remove(0);
			Action a = it.next();
			int label = records.get(key(a.x,a.y));

			// if label has no new info, pop again
			if (label == 0) {
				it.remove();
				System.out.printf("%s->%d cn: 0 [no new info, removed]%n", key(a.x,a.y), label);
				continue;
			}

			// get number of covered neighbors
			ArrayList<Action> possible = countCoveredNeighbors(a.x,a.y);
			System.out.printf("%s->%d  cn: %d%n", key(a.x,a.y), label, possible.size());

			// if number of covered neighbors == label value (remaining numbers are mines)
			if(possible.size() <= label){
				System.out.println("--match");

				// flag each tile as a mine and update labels of adjacent tiles for each mine
				flagAndUpdate(possible);
				a = handleGuaranteed();
				if (a != null) return a;
			}
		}

		// [STEP4.1] Check for 121 cases
		Action case121Action = handleCase121();
		if (case121Action != null) return case121Action;


		/* At this point, making guesses */

		// update coveredFrontier with new knowledge
		coveredFrontier = coveredFrontier.stream()
				.filter(a -> records.get(key(a.x,a.y))==COV_NEIGHBOR)
				.collect(Collectors.toCollection(ArrayList::new));

		// output updated details
		outputKnowledge();

		Action modelCheckingAction = handleModelChecking();
		if(modelCheckingAction != null) return modelCheckingAction;

		// [STEP4.2] Pick from ucf with lowest probability
		//Action probabilityAction = handleProbability();
		//if (probabilityAction != null) return probabilityAction;

		// [STEP4.3] Pick any from cf
		if(flagsLeft != 0) return handleAny();

		// [STEP 5] Leave
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
				if (!records.containsKey(k) || records.get(k)==COV_NEIGHBOR) {
					//System.out.println(k + " added to safe");
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
					//System.out.println(k + " added to covered frontier");
					records.put(k, COV_NEIGHBOR); // -10 placeholder for neighbors of uncovered tiles
					coveredFrontier.add(new Action(ACTION.UNCOVER, i, j));
				}
			}
		}
	}

	private void addSelfToUncoveredFrontier(int x, int y){
		//System.out.println(key(x,y) + " added to uncovered frontier");
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
				if (records.get(k)==COV_NEIGHBOR) {
					possible.add(new Action(ACTION.FLAG, i, j));
					if (!probability.containsKey(k))
						probability.put(k, 1);
					else{
						int p = probability.get(k);
						probability.put(k, ++p);
					}
				}
			}
		}
		return possible;
	}

	private void flagAndUpdate(ArrayList<Action> flags){

		// for each guaranteed mine
		for (Action f : flags) {

			// flag the mine
			System.out.println("flag: " + key(f.x,f.y));
			records.put(key(f.x,f.y),MINE); // MINE = -9 value for mines
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
					if (records.containsKey(k)) {
						if (records.get(k)>0){
							int labelValue = records.get(k);
							labelValue--;
							records.put(k, labelValue);
							System.out.println("update: " + k + " = " + (records.get(k)+1) + " -> " + records.get(k));

							// if new label == 0, uncover any remaining covered neighbors
							if (labelValue == 0) {
								addNeighborsToSafeTiles(i, j); // FIX THIS PATH; UPDATE LABEL AFTER UNCOVER
							}
						}
					}
					else {
						System.out.println(k + " added to covered frontier");
						records.put(k, COV_NEIGHBOR); // -10 placeholder for neighbors of uncovered tiles
						coveredFrontier.add(new Action(ACTION.UNCOVER, i, j));
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
				if (records.containsKey(k)) {
					if (records.get(k) == MINE)
						label--;
				}
			}
		}
		return label;
	}

	private boolean checkNeighborsForMines(int x, int y){
		int rowMin = y - 1;
		int rowMax = y + 1;
		if (rowMin < 1) rowMin = 1;
		if (rowMax > ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

		int colMin = x - 1;
		int colMax = x + 1;
		if (colMin < 1) colMin = 1;
		if (colMax > COL_DIMENSIONS) colMax = COL_DIMENSIONS;

		for (int j = rowMax; j > rowMin - 1; j--) {
			for (int i = colMin; i < colMax + 1; i++) {
				String k = key(i, j);
				//System.out.printf("Checking %s neighbors for mines...", k);
				if (records.containsKey(k)) {
					if (records.get(k) == MINE)
						return true;
				}
			}
		}
		return false;
	}

	private Action handleGuaranteed(){
		// flag mines if any
		if (!guaranteedMine.isEmpty()) {
			Action a = guaranteedMine.remove(0);
			currX = a.x;
			currY = a.y;
			flagsLeft--;
			return a;
		}

		// uncover safe if any
		if (!guaranteedSafe.isEmpty()) {
			Action a = guaranteedSafe.remove(0);
			currX = a.x;
			currY = a.y;
			return a;
		}

		return null;
	}

	private Action handleCase121(){
		//System.out.println("\nSearching ucf for 121...");
		ArrayList<Action> twos = uncoveredFrontier.stream()
				.filter(a -> records.get(key(a.x,a.y))==2)
				.collect(Collectors.toCollection(ArrayList::new));
		//System.out.println("twos: " + twos);
		ArrayList<Action> flags = new ArrayList<>();
		for (Action a : twos) {
			if (a.y == 1 || a.y == ROW_DIMENSIONS) continue;
			if (a.x == 1 || a.x == COL_DIMENSIONS) continue;
			int i = a.x;
			int j = a.y;

			//System.out.println(key(i,j));
			int t = records.get(key(i,j+1));
			int b = records.get(key(i,j-1));
			int l = records.get(key(i-1,j));
			int r = records.get(key(i+1,j));

			if(t == 1 && b == 1) {
				if (checkNeighborsForMines(i, j+1) || checkNeighborsForMines(i, j-1)) continue;
				if(l == COV_NEIGHBOR) {
					flags.add(new Action(ACTION.FLAG,i-1,j+1));
					flags.add(new Action(ACTION.FLAG,i-1,j-1));
				}
				else if(r == COV_NEIGHBOR) {
					flags.add(new Action(ACTION.FLAG,i+1,j+1));
					flags.add(new Action(ACTION.FLAG,i+1,j-1));
				}
			}
			else if(l == 1 && r == 1) {
				if (checkNeighborsForMines(i-1, j) || checkNeighborsForMines(i+1, j)) continue;
				if(t == COV_NEIGHBOR) {
					flags.add(new Action(ACTION.FLAG, i-1, j + 1));
					flags.add(new Action(ACTION.FLAG, i+1, j + 1));
				}
				else if(b == COV_NEIGHBOR) {
					flags.add(new Action(ACTION.FLAG, i-1, j - 1));
					flags.add(new Action(ACTION.FLAG, i+1, j - 1));
				}
			}
		}
		flagAndUpdate(flags);
		return handleGuaranteed();
	}

	private Action handleModelChecking(){
		return null;
	}

	private Action handleProbability(){
		System.out.println("\nPicking from cf with lowest probability...");
		System.out.println("probability: " + probability);

		// pick min probability
		Action a = coveredFrontier.stream()
				.filter(t -> probability.containsKey(key(t.x,t.y)))
				.min(Comparator.comparing(t -> probability.get(key(t.x, t.y))))
				.orElse(null);

		// pick min probability that isn't diagonal from corner
//		Action a = coveredFrontier.stream()
//				.filter(t -> probability.containsKey(key(t.x,t.y)))
//				.filter(t -> {
//					boolean right = (t.x != COL_DIMENSIONS && records.containsKey(key(t.x+1, t.y)) && records.get(key(t.x+1, t.y)) == -1);
//					boolean left = (t.x != 1 && records.containsKey(key(t.x-1, t.y)) &&records.get(key(t.x-1, t.y)) == -1);
//					boolean top = (t.y != ROW_DIMENSIONS && records.containsKey(key(t.x, t.y+1)) && records.get(key(t.x, t.y+1)) == -1);
//					boolean bottom = (t.y != 1 && records.containsKey(key(t.x,t.y-1)) && records.get(key(t.x,t.y-1)) == -1);
//					return (top && right) || (top && left) || (bottom && right) || (bottom && left);
//				})
//				.min(Comparator.comparing(t -> probability.get(key(t.x, t.y))))
//				.orElse(null);

		if(a!=null) {
			coveredFrontier.remove(a);
			currX = a.x;
			currY = a.y;
			return a;
		}
		return null;
	}

	private Action handleAny(){
		System.out.println("\nPicking random tile...");
		Random random = new Random();
		int randomX = random.nextInt(COL_DIMENSIONS)+1;
		int randomY = random.nextInt(ROW_DIMENSIONS)+1;
		String k = key(randomX,randomY);
		while(records.containsKey(k) && records.get(k) != COV_NEIGHBOR){
			randomX = random.nextInt(COL_DIMENSIONS)+1;
			randomY = random.nextInt(ROW_DIMENSIONS)+1;
			k = key(randomX,randomY);
		}
		return new Action(ACTION.UNCOVER, randomX, randomY);
	}

	private void outputKnowledge(){
		System.out.println("\nrecords: " + records);
		System.out.println("\nsafe: " + guaranteedSafe);
		System.out.println("\nuc frontier: " + uncoveredFrontier);
		System.out.println("\nc frontier: " + coveredFrontier);
	}
}
