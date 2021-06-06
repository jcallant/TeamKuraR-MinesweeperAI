

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
	private ArrayList<Tile> guaranteedSafe;
	private ArrayList<Tile> guaranteedMine;
	private ArrayList<Tile> coveredFrontier;
	private ArrayList<Tile> uncoveredFrontier;
	private HashMap<String, Integer> probability;


	// label value constants
	private final int MINE = -9;
	private final int COV_NEIGHBOR = -10;
	private final int SAFE = -11;

	private class Tile{
		public int x;
		public int y;
		public Tile(int x, int y){
			this.x = x;
			this.y = y;
		}
		public String toString(){
			return "Tile" + key(this.x, this.y);
		}
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null) return false;
			if (getClass() != o.getClass()) return false;
			Tile t = (Tile) o;
			return Objects.equals(this.x, t.x) && Objects.equals(this.y, t.y);
		}
	}

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

		// [STEP2.1]: check if all mines are flagged
		if(flagsLeft == 0){
			System.out.println("No more flags. Uncovering rest");
			for(int i=1; i<=COL_DIMENSIONS; i++){
				for(int j=1; j<=ROW_DIMENSIONS; j++){
					String key = key(i, j);
					if(!records.containsKey(key) || records.get(key)==COV_NEIGHBOR){
						records.put(key, 0);
						guaranteedSafe.add(new Tile(i, j));
					}
				}
			}
		}

		// [STEP2.2]: if any guaranteed mines or safe tiles
		Action guaranteedAction = handleGuaranteed();
		if(guaranteedAction != null) return guaranteedAction;


		// [STEP3]: use uncovered frontier to gain new knowledge
		//System.out.println("\nPicking from ucf...");
		// probability map used to record probabilities of covered tiles
		probability = new HashMap<>();
		Iterator<Tile> it = uncoveredFrontier.iterator();
		while(it.hasNext()){
			//Action a = uncoveredFrontier.remove(0);
			Tile t = it.next();
			Action a = new Action(ACTION.FLAG, t.x, t.y);
			int label = records.get(key(a.x,a.y));

			// if label has no new info, pop again
			if (label == 0) {
				it.remove();
				//System.out.printf("%s->%d cn: 0 [no new info, removed]%n", key(a.x,a.y), label);
				continue;
			}

			// get number of covered neighbors
			ArrayList<Tile> possible = countCoveredNeighbors(a.x,a.y);
			//System.out.printf("%s->%d  cn: %d%n", key(a.x,a.y), label, possible.size());

			// if number of covered neighbors == label value (remaining numbers are mines)
			if(possible.size() <= label){
				//System.out.println("--match");

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

		//System.out.println("Attempting Model Checking...");
		Action modelCheckingAction = handleModelChecking2(100000);
		if (modelCheckingAction != null) return modelCheckingAction;

		// [STEP4.2] Pick from ucf with lowest probability
		Action probabilityAction = handleProbability();
		if (probabilityAction != null) return probabilityAction;

		// [STEP 5] Leave
		return new Action(ACTION.LEAVE);
	}


	// ################### Helper Functions Go Here (optional) ##################


	private String key(int x, int y){
		return "(" + x + "," + y + ")";
	}

	private void addNeighborsToSafeTiles(int x, int y){
		ArrayList<Tile> neighbors = getNeighbors(x, y);
		for(Tile t : neighbors){
			String k = key(t.x, t.y);
			if (!records.containsKey(k) || records.get(k)==COV_NEIGHBOR) {
				//System.out.println(k + " added to safe");
				records.put(k, 0);
				guaranteedSafe.add(t);
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
				if (j==y && i==x) continue;
				String k = key(i, j);
				if (!records.containsKey(k)) {
					//System.out.println(k + " added to covered frontier");
					records.put(k, COV_NEIGHBOR); // -10 placeholder for neighbors of uncovered tiles
					coveredFrontier.add(new Tile(i, j));
				}
			}
		}
	}

	private void addSelfToUncoveredFrontier(int x, int y){
		//System.out.println(key(x,y) + " added to uncovered frontier");
		uncoveredFrontier.add(new Tile(x, y));
	}

	private ArrayList<Tile> countCoveredNeighbors(int x, int y){
		int rowMin = y-1;
		int rowMax = y+1;
		if(rowMin<1) rowMin = 1;
		if(rowMax>ROW_DIMENSIONS) rowMax = ROW_DIMENSIONS;

		int colMin = x-1;
		int colMax = x+1;
		if(colMin<1) colMin = 1;
		if(colMax>COL_DIMENSIONS) colMax = COL_DIMENSIONS;

		ArrayList<Tile> possible = new ArrayList<>();
		for(int j=rowMax; j>rowMin-1; j--){
			for(int i=colMin; i<colMax+1; i++) {
				if (j==y && i==x) continue;
				String k = key(i, j);
				if (records.get(k)==COV_NEIGHBOR) {
					possible.add(new Tile(i, j));
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

	private void flagAndUpdate(ArrayList<Tile> flags){

		// for each guaranteed mine
		for (Tile f : flags) {

			// check if not already flagged
			if(records.containsKey(key(f.x,f.y)) && records.get(key(f.x,f.y))==MINE) continue;
			//System.out.println("flag: " + key(f.x,f.y));
			records.put(key(f.x,f.y),MINE); // MINE = -9 value for mines
			guaranteedMine.add(new Tile(f.x, f.y));

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
							//System.out.println("update: " + k + " = " + (records.get(k)+1) + " -> " + records.get(k));

							// if new label == 0, uncover any remaining covered neighbors
							if (labelValue == 0) {
								addNeighborsToSafeTiles(i, j);
							}
						}
					}
//					else {
//						System.out.println(k + " added to covered frontier");
//						records.put(k, COV_NEIGHBOR); //
//						coveredFrontier.add(new Action(ACTION.FLAG, i, j));
//					}
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
			Tile t = guaranteedMine.remove(0);
			Action a = new Action(ACTION.FLAG, t.x, t.y);
			currX = a.x;
			currY = a.y;
			flagsLeft--;
			return a;
		}

		// uncover safe if any
		if (!guaranteedSafe.isEmpty()) {
			Tile t = guaranteedSafe.remove(0);
			Action a = new Action(ACTION.UNCOVER, t.x, t.y);
			currX = a.x;
			currY = a.y;
			return a;
		}

		return null;
	}

	private Action handleCase121(){
		//System.out.println("\nSearching ucf for 121...");
		ArrayList<Tile> twos = uncoveredFrontier.stream()
				.filter(a -> records.get(key(a.x,a.y))==2)
				.collect(Collectors.toCollection(ArrayList::new));
		//System.out.println("twos: " + twos);
		ArrayList<Tile> flags = new ArrayList<>();
		for (Tile tile : twos) {
			if (tile.y == 1 || tile.y == ROW_DIMENSIONS) continue;
			if (tile.x == 1 || tile.x == COL_DIMENSIONS) continue;
			int i = tile.x;
			int j = tile.y;

			//System.out.println(key(i,j));
			int t = records.get(key(i,j+1));
			int b = records.get(key(i,j-1));
			int l = records.get(key(i-1,j));
			int r = records.get(key(i+1,j));

			if(t == 1 && b == 1) {
				if (checkNeighborsForMines(i, j+1) || checkNeighborsForMines(i, j-1)) continue;
				if(l == COV_NEIGHBOR) {
					flags.add(new Tile(i-1,j+1));
					flags.add(new Tile(i-1,j-1));
				}
				else if(r == COV_NEIGHBOR) {
					flags.add(new Tile(i+1,j+1));
					flags.add(new Tile(i+1,j-1));
				}
			}
			else if(l == 1 && r == 1) {
				if (checkNeighborsForMines(i-1, j) || checkNeighborsForMines(i+1, j)) continue;
				if(t == COV_NEIGHBOR) {
					flags.add(new Tile(i-1, j + 1));
					flags.add(new Tile(i+1, j + 1));
				}
				else if(b == COV_NEIGHBOR) {
					flags.add(new Tile(i-1, j - 1));
					flags.add(new Tile(i+1, j - 1));
				}
			}
		}
		flagAndUpdate(flags);
		return handleGuaranteed();
	}

	private Action handleProbability(){
		//System.out.println("\nPicking from cf with lowest probability...");
		//System.out.println("probability: " + probability);

		// pick min probability
		Action a = coveredFrontier.stream()
				.filter(t -> probability.containsKey(key(t.x,t.y)))
				.map(uncoverAction -> new Action(ACTION.UNCOVER, uncoverAction.x, uncoverAction.y))
				.min(Comparator.comparing(t -> probability.get(key(t.x, t.y))))
				.orElse(null);

		if(a!=null) {
			coveredFrontier.remove(a);
			currX = a.x;
			currY = a.y;
			return a;
		}
		return null;
	}

	private Action handleAny(){
		//System.out.println("\nPicking random tile...");
		Random random = new Random();
		int randomX = random.nextInt(COL_DIMENSIONS)+1;
		int randomY = random.nextInt(ROW_DIMENSIONS)+1;
		String k = key(randomX,randomY);
		while(records.containsKey(k) && records.get(k) != COV_NEIGHBOR){
			randomX = random.nextInt(COL_DIMENSIONS)+1;
			randomY = random.nextInt(ROW_DIMENSIONS)+1;
			k = key(randomX,randomY);
		}
		currX = randomX;
		currY = randomY;
		return new Action(ACTION.UNCOVER, randomX, randomY);
	}

	private void outputKnowledge(){
		//System.out.println("\nrecords: " + records);
		//System.out.println("\nsafe: " + guaranteedSafe);
		//System.out.println("\nuc frontier: " + uncoveredFrontier);
		//System.out.println("\nc frontier: " + coveredFrontier);
	}


	// ################### ModelChecking Functions ##################

	private Action handleModelChecking(double timeLimit){
		if(coveredFrontier.isEmpty()) return null;
		//System.out.printf(">> cf: %s\n", coveredFrontier);

		int size = (int) Math.pow(2, coveredFrontier.size());

		// initialize to 0
		int solutionCount = 0;
		HashMap<Tile, Integer> probabilities = new HashMap<>();
		for (Tile t : coveredFrontier) {
			probabilities.put(t, 0);
		}
		boolean timedOut = false;

		// using coveredFrontier, iterate through powerset
		for(int i=1; i<size; i++){

			// if taking too long, stop
			timeLimit--;
			if(timeLimit < 0) {
				System.out.println(">>>>>>>>>>> TIME UP!!!");
				timedOut = true;
				break;
			}

			// build possible mine list for this iteration
			ArrayList<Tile> mineList = new ArrayList<>();
			for(int j=0; j<coveredFrontier.size(); j++){
				if((i & (1 << j)) > 0) {
					mineList.add(coveredFrontier.get(j));
				}
			}
//			System.out.printf("%d. mineList: %s\n",i,mineList);

			// if mine list matches the amount of flagsLeft
			if(mineList.size() <= flagsLeft) {
				HashMap<String, Integer> worldRecords = new HashMap<>();

				// hold temp list (hypoFlagAndUpdate function manipulates list)
				ArrayList<Tile> temp = new ArrayList<>(mineList);

				// if mine list is a possible solution
				if(hypoFlagAndUpdate(mineList, worldRecords)!=null) {
					++solutionCount;
//					System.out.printf("%d: %s\n",solutionCount, temp);
					for(Tile a : temp){
						int p = probabilities.get(a);
						probabilities.put(a, ++p);
					}
				}
			}
		}
//		System.out.printf(">> %d solutions found\n",solutionCount);
//		System.out.printf(">> probabilities: %s\n", probabilities);




		// if not stopped by timer, all solutions found and probabilities are valid
		if(!timedOut) {
			// idk how but this block makes it worse
			// out of all solutions n, add tiles with 0\n of being mine to guaranteedSafe
//			System.out.println(">>> ADDING SAFE FOUND FROM SOLUTIONS:");
//			for (Action action : probabilities.keySet()) {
//				if (probabilities.get(action) == 0) {
//					System.out.printf(">>> safe %s\n", key(action.x, action.y));
//					Action a = new Action(ACTION.UNCOVER, action.x, action.y);
//					records.put(key(a.x,a.y), 0);
//					guaranteedSafe.add(a);
//				}
//			}
//			System.out.println("Hit any button to Continue...");
//			Scanner in = new Scanner(System.in);
//			in.nextLine();

			// out of all solutions n, add tiles with n\n of being mine to guaranteedMine
			final int finalSolutionCount = solutionCount;
			ArrayList<Tile> mines = new ArrayList<>();
			for (Tile k : probabilities.keySet()) {
				if (probabilities.get(k) == finalSolutionCount) {
					mines.add(k);
				}
			}
			flagAndUpdate(mines);

			// get guaranteed action if any
			Action finalAction = handleGuaranteed();

			// if no guaranteed, uncover tile with min probability
			if (finalAction == null){
				finalAction = probabilities.keySet().stream()
						.min(Comparator.comparing(probabilities::get))
						.map(uncoverAction -> new Action(ACTION.UNCOVER, uncoverAction.x, uncoverAction.y))
						.orElse(null);
//				System.out.println("probabilities: " + probabilities);
//				System.out.println("min: " + finalAction);
				//doPause();
			}

			// assuming a solution was found, (if not then it's broken)
			if(finalAction != null) {
				currX = finalAction.x;
				currY = finalAction.y;
			}

			return finalAction;
		}

		// else if stopped by timer, probabilities not valid
		else{
			// calculate odds for random pick
			double allTiles = ROW_DIMENSIONS * COL_DIMENSIONS;
			double knownTiles = (int) records.keySet().stream()
					.filter(k -> records.get(k) >= 0 || records.get(k) == MINE || records.get(k) == SAFE)
					.count();
			double unknownTiles = allTiles - knownTiles;
			double ratio = flagsLeft / unknownTiles ;

			if(ratio < 0.50) {
				System.out.println("IM TAKING A RISK!!!");
				return handleAny();
			}
		}

		return null;
	}

	private HashMap<String, Integer> hypoFlagAndUpdate(ArrayList<Tile> frontier, HashMap<String, Integer> hypoRecords) {

		for(Tile a : frontier) {
			int x = a.x;
			int y = a.y;
//		//System.out.println(" <hypoFlag: " + key(x, y));

			// if already marked safe, then can't be flagged as mine
			if (hypoRecords.containsKey(key(x, y)) && hypoRecords.get(key(x, y)) == SAFE) {
				//System.out.println(" </hypoFlag>");
				return null;
			}

			// mark tile as mine in hypoRecords
			hypoRecords.put(key(x, y), MINE);

			// update labels for neighboring tiles in hypoRecords
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
					if (hypoRecords.containsKey(k) && hypoRecords.get(k) >= 0) {
						int labelValue = hypoRecords.get(k);
						labelValue--;
//					System.out.println(String.format(" -label update: %s %d -> %d",k,labelValue+1, labelValue));
						if (labelValue == -1) {
//						System.out.println(" -label conflict: " + k);
//						System.out.println(" </hypoFlag>");
							return null; // if flagging as mine causes label conflict, then not possible
						}
						hypoRecords.put(k, labelValue);

						// if new label == 0, uncover any remaining covered neighbors
						if (labelValue == 0) {
							hypoAddCoveredNeighborsToSafeTiles(i, j, hypoRecords);
						}
					} else if (records.containsKey(k) && records.get(k) >= 0) {
						int labelValue = records.get(k);
						labelValue--;
//					System.out.println(String.format(" -label update: %s %d -> %d",k,labelValue+1, labelValue));
						if (labelValue == -1) {
//						System.out.println(" -label conflict: " + k);
//						System.out.println(" </hypoFlag>");
							return null; // if flagging as mine causes label conflict, then not possible
						}
						hypoRecords.put(k, labelValue);

						// if new label == 0, uncover any remaining covered neighbors
						if (labelValue == 0) {
							hypoAddCoveredNeighborsToSafeTiles(i, j, hypoRecords);
						}
					}
				}
			}
		}


		//System.out.print(" ======= checking if valid...");
		for (Tile action : uncoveredFrontier) {
			String k = key(action.x, action.y);
			if (!hypoRecords.containsKey(k) || hypoRecords.get(k) > 0) {
				//System.out.println("N: unsatisfied label " + k);
				return null;
			}
		}
		//System.out.println("\n hypoRecord: " + hypoRecords);
		//System.out.printf(" SOLUTION ");
		return hypoRecords;
	}

	private HashMap<String, Integer> hypoAddCoveredNeighborsToSafeTiles(int x, int y, HashMap<String, Integer> hypoRecords){
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
				if (j==y && i==x) continue;
				String k = key(i, j);
				if (hypoRecords.containsKey(k)){
					if(hypoRecords.get(k)==COV_NEIGHBOR) {
						//System.out.printf(" -safe %s\n", k);
						hypoRecords.put(k, SAFE);
					}
				}
				else if (!records.containsKey(k) || records.get(k)==COV_NEIGHBOR) {
					//System.out.printf(" -safe %s\n", k);
					hypoRecords.put(k, SAFE);
				}
			}
		}
		return hypoRecords;
	}

	private ArrayList<Tile> getNeighbors(int x, int y){
		ArrayList<Tile> neighbors = new ArrayList<>();
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
				if (j==y && i==x) continue;
				neighbors.add(new Tile(i, j));
			}
		}
		return neighbors;
	}

	public static void doPause(){
		System.out.println("Hit any button to Continue...");
		Scanner in = new Scanner(System.in);
		in.nextLine();
	}


	// ############################ SECOND ATTEMPT AT MODEL CHECKING ################################

	private ArrayList<ArrayList<Tile>> getOrderedChain(ArrayList<Tile> list){
		ArrayList<Tile> copy = new ArrayList<>(list);
		if(list.isEmpty()) return null;

		ArrayList<ArrayList<Tile>> chains = new ArrayList<>();

		Tile tail = copy.remove(0);
		ArrayList<Tile> sublist = new ArrayList<>();
		sublist.add(tail);

		while(!copy.isEmpty()){
			Tile up = new Tile(tail.x, tail.y+1);
			Tile down = new Tile(tail.x, tail.y-1);
			Tile right = new Tile(tail.x+1, tail.y);
			Tile left = new Tile(tail.x-1, tail.y);

			ArrayList<Tile> neighbors = getNeighbors(tail.x, tail.y);

			Tile next = null;
			if(copy.contains(up) && neighbors.contains(up)) next = up;
			else if(copy.contains(down) && neighbors.contains(down)) next = down;
			else if(copy.contains(right) && neighbors.contains(right)) next = right;
			else if(copy.contains(left) && neighbors.contains(left)) next = left;

			if(next != null) copy.remove(next);
			else {
				chains.add(new ArrayList<>(sublist));
				next = copy.remove(0);
				sublist = new ArrayList<>();
			}
			sublist.add(next);
			tail = next;
		}
		chains.add(sublist);
		return chains;
	}

	// optimizing frontier for shorter combination calculations
	private Action handleModelChecking2(double timeLimit){
		if(coveredFrontier.isEmpty()) return null;
		//System.out.printf(">> cf: %s\n", coveredFrontier);

		ArrayList<ArrayList<Tile>> chains = getOrderedChain(coveredFrontier);
		//doPause();

		// initialize to 0
		int solutionCount = 0;
		HashMap<Tile, Integer> mineProbabilities = new HashMap<>();
		HashMap<String, Integer> safeProbabilities = new HashMap<>();
		for (Tile t : coveredFrontier) {
			mineProbabilities.put(t, -1);
			safeProbabilities.put(key(t.x,t.y), -1);
		}
		boolean timedOut = false;

		for(ArrayList<Tile> sublist : chains) {
			int size = (int) Math.pow(2, sublist.size());

			// using coveredFrontier, iterate through powerset
			for(int i=1; i<size; i++){

				// if taking too long, stop
				timeLimit--;
				if(timeLimit < 0) {
					System.out.println(">>>>>>>>>>> TIME UP!!!");
					timedOut = true;
					break;
				}

				// build possible mine list for this iteration
				ArrayList<Tile> mineList = new ArrayList<>();
				for(int j=0; j<sublist.size(); j++){
					if((i & (1 << j)) > 0) {
						mineList.add(sublist.get(j));
					}
				}

//				System.out.printf("%d. mineList: %s\n", i, mineList);

				// if mine list matches the amount of flagsLeft
				if (mineList.size() <= flagsLeft) {
					HashMap<String, Integer> worldRecords = new HashMap<>();

					// hold temp list (hypoFlagAndUpdate function manipulates list)
					ArrayList<Tile> temp = new ArrayList<>(mineList);

					// if mine list is a possible solution
					Tile result = hypoFlagAndUpdate2(mineList, sublist, worldRecords);
					if (result != null && result.equals(new Tile(0,0))) {
						++solutionCount;
//						System.out.printf("%d: %s\n",solutionCount, temp);
						for (Tile a : temp) {
							int p = mineProbabilities.get(a);
							mineProbabilities.put(a, ++p);
						}
					}
					else if (result != null){
						for(String k : worldRecords.keySet()){
							if (worldRecords.get(k) == SAFE) {
								int p = safeProbabilities.get(k);
								safeProbabilities.put(k, ++p);
							}
						}
					}
				}
			}
		}
//		System.out.printf(">> %d solutions found\n",solutionCount);
//		System.out.printf(">> probabilities: %s\n", probabilities);


		// if not stopped by timer, all solutions found and probabilities are valid
		if(!timedOut) {
			// out of all solutions n, add tiles with n\n of being mine to guaranteedMine
			final int finalSolutionCount = solutionCount;
			ArrayList<Tile> mines = new ArrayList<>();
			for (Tile k : mineProbabilities.keySet()) {
				if (mineProbabilities.get(k) == finalSolutionCount) {
					mines.add(k);
				}
			}
			flagAndUpdate(mines);

			// get guaranteed action if any
			Action finalAction = handleGuaranteed();

			// if no guaranteed, uncover tile with min probability
			if (finalAction == null){
				finalAction = mineProbabilities.keySet().stream()
						.min(Comparator.comparing(mineProbabilities::get))
						.map(uncoverAction -> new Action(ACTION.UNCOVER, uncoverAction.x, uncoverAction.y))
						.orElse(null);
				System.out.println("probabilities: " + mineProbabilities);
				System.out.println("min: " + finalAction);
			}

			// assuming a solution was found, (if not then it's broken)
			if(finalAction != null) {
				currX = finalAction.x;
				currY = finalAction.y;
			}

			return finalAction;
		}

		// else if stopped by timer, probabilities not valid
		else{
			// calculate odds for random pick
			double allTiles = ROW_DIMENSIONS * COL_DIMENSIONS;
			double knownTiles = (int) records.keySet().stream()
					.filter(k -> records.get(k) >= 0 || records.get(k) == MINE || records.get(k) == SAFE)
					.count();
			double unknownTiles = allTiles - knownTiles;
			double ratio = flagsLeft / unknownTiles ;

			if(ratio < 0.50) {
				System.out.println("IM TAKING A RISK!!!");
				return handleAny();
			}
		}

		return null;
	}
	private Tile hypoFlagAndUpdate2(ArrayList<Tile> frontier, ArrayList<Tile> cfSegment, HashMap<String, Integer> hypoRecords) {

		for(Tile a : frontier) {
			int x = a.x;
			int y = a.y;
//		//System.out.println(" <hypoFlag: " + key(x, y));

			// if already marked safe, then can't be flagged as mine
			if (hypoRecords.containsKey(key(x, y)) && hypoRecords.get(key(x, y)) == SAFE) {
				//System.out.println(" </hypoFlag>");
				return null;
			}

			// mark tile as mine in hypoRecords
			hypoRecords.put(key(x, y), MINE);

			// update labels for neighboring tiles in hypoRecords
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
					if (hypoRecords.containsKey(k) && hypoRecords.get(k) >= 0) {
						int labelValue = hypoRecords.get(k);
						labelValue--;
//					System.out.println(String.format(" -label update: %s %d -> %d",k,labelValue+1, labelValue));
						if (labelValue == -1) {
//						System.out.println(" -label conflict: " + k);
//						System.out.println(" </hypoFlag>");
							return null; // if flagging as mine causes label conflict, then not possible
						}
						hypoRecords.put(k, labelValue);

						// if new label == 0, uncover any remaining covered neighbors
						if (labelValue == 0) {
							hypoAddCoveredNeighborsToSafeTiles(i, j, hypoRecords);
						}
					} else if (records.containsKey(k) && records.get(k) >= 0) {
						int labelValue = records.get(k);
						labelValue--;
//					System.out.println(String.format(" -label update: %s %d -> %d",k,labelValue+1, labelValue));
						if (labelValue == -1) {
//						System.out.println(" -label conflict: " + k);
//						System.out.println(" </hypoFlag>");
							return null; // if flagging as mine causes label conflict, then not possible
						}
						hypoRecords.put(k, labelValue);

						// if new label == 0, uncover any remaining covered neighbors
						if (labelValue == 0) {
							hypoAddCoveredNeighborsToSafeTiles(i, j, hypoRecords);
						}
					}
				}
			}
		}

		ArrayList<Tile> ucfSegment = new ArrayList<>();
		for(Tile tile : cfSegment){
			ArrayList<Tile> neighbors = getNeighbors(tile.x, tile.y);
			for(Tile neighbor : neighbors){
				if(uncoveredFrontier.contains(neighbor)){
					System.out.println("CONTAINED IN UCF: " + neighbor);
					ucfSegment.add(neighbor);
				}
			}
		}


		//System.out.print(" ======= checking if valid...");
		for (Tile tile : ucfSegment) {
			String k = key(tile.x, tile.y);
			if (!hypoRecords.containsKey(k) || hypoRecords.get(k) > 0) {
				//System.out.println("N: unsatisfied label " + k);
				return tile;
			}
		}
		//System.out.println("\n hypoRecord: " + hypoRecords);
		//System.out.printf(" SOLUTION ");
		return new Tile(0,0);
	}

	private int getX(String key){
		String first = key.split(",")[0];
		return Integer.parseInt(first.substring(1));
	}
	private int getY(String key){
		String second = key.split(",")[1];
		return Integer.parseInt(second.substring(0,second.length()-1));
	}
}
