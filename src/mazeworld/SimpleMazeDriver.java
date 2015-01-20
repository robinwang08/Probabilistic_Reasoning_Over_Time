package mazeworld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import mazeworld.SearchProblem.SearchNode;
import mazeworld.SimpleMazeProblem.SimpleMazeNode;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SimpleMazeDriver extends Application {

	Maze maze;

	// Filtering method
	ArrayList<String> colorPath = new ArrayList<String>();
	ArrayList<Double> filterDist = new ArrayList<Double>();
	ArrayList<Double> fbDist = new ArrayList<Double>();

	// For forward-backward smoothing implementation
	public HashMap<Integer, ArrayList<Double>> forwards = new HashMap<Integer, ArrayList<Double>>();
	public HashMap<Integer, List<Integer>> intcor;
	public HashMap<List<Integer>, Integer> intrep;

	// For viterbi implementation
	public ArrayList<Double> viterbiProbs = new ArrayList<Double>();
	public HashMap<Integer, List<Integer>> tempbestPath = new HashMap<Integer, List<Integer>>();
	public HashMap<Integer, List<Integer>> bestPath = new HashMap<Integer, List<Integer>>();

	// instance variables used for graphical display
	private static final int PIXELS_PER_SQUARE = 64;
	MazeView mazeView;
	List<AnimationPath> animationPathList;

	// some basic initialization of the graphics; needs to be done before
	// runSearches, so that the mazeView is available
	private void initMazeView() {
		maze = Maze
				.readFromFile("/Users/robin/Documents/learn/l6/src/assignment_mazeworld/simple.maz");

		animationPathList = new ArrayList<AnimationPath>();
		// build the board
		mazeView = new MazeView(maze, PIXELS_PER_SQUARE);

	}

	// assumes maze and mazeView instance variables are already available
	private void startMoves() {

		// Maze width and height
		int n = maze.width;
		int m = maze.height;

		// Initial probabilities are all 1/16
		for (int i = 0; i < n * m; i++) {
			filterDist.add((double) 1 / 16);
			viterbiProbs.add((double) 1 / 16);
		}

		// Map domain integers to their coordinates to aid in printing solution
		intcor = new HashMap<Integer, List<Integer>>();
		intrep = new HashMap<List<Integer>, Integer>();
		int key = -1;
		for (int row = 0; row < m; row++) {
			for (int col = 0; col < n; col++) {
				key++;
				intcor.put(key, Arrays.asList(row, col));
				intrep.put(Arrays.asList(row, col), key);
				bestPath.put(key, Arrays.asList(key));
				tempbestPath.put(key, new ArrayList<Integer>());
			}
		}

		// X and Y coordinates
		List<Integer> x = new ArrayList<Integer>();
		List<Integer> y = new ArrayList<Integer>();

		// Adding in the coordinates of the path of the robot
		x.add(0);
		y.add(0);

		x.add(1);
		y.add(0);

		x.add(2);
		y.add(0);

		x.add(3);
		y.add(0);

		x.add(3);
		y.add(1);
		
		x.add(3);
		y.add(2);
		
		x.add(3);
		y.add(3);
		 

		// Path; animation
		animationPathList = new ArrayList<AnimationPath>();
		animationPathList.add(new AnimationPath(mazeView, x, y));

	}

	public static void main(String[] args) {
		launch(args);

	}

	// javafx setup of main view window for mazeworld
	@Override
	public void start(Stage primaryStage) {

		initMazeView();

		primaryStage.setTitle("Maze");

		// add everything to a root stackpane, and then to the main window
		StackPane root = new StackPane();
		root.getChildren().add(mazeView);
		primaryStage.setScene(new Scene(root));

		primaryStage.show();

		// do the real work of the driver; run search tests
		startMoves();

		// sets mazeworld's game loop (a javafx Timeline)
		Timeline timeline = new Timeline(1.0);
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.getKeyFrames().add(
				new KeyFrame(Duration.seconds(.05), new GameHandler()));
		timeline.playFromStart();

	}

	// every frame, this method gets called and tries to do the next move
	// for each animationPath.
	private class GameHandler implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent e) {
			// System.out.println("timer fired");
			for (AnimationPath animationPath : animationPathList) {
				// note: animationPath.doNextMove() does nothing if the
				// previous animation is not complete. If previous is complete,
				// then a new animation of a piece is started.
				animationPath.doNextMove();
			}

		}
	}

	// each animation path needs to keep track of some information:
	// the underlying search path, the "piece" object used for animation,
	// etc.
	private class AnimationPath {
		// Robot
		private Node piece;
		// X and Y coordinates
		private List<Integer> xsearchPath;
		private List<Integer> ysearchPath;
		// Time
		private int currentMove = 0;
		// Keep track of location
		private int lastX;
		private int lastY;
		// Help with animation
		boolean animationDone = true;
		// To find out when the entire color sequence is finished
		boolean fb = false;

		// Constructor
		public AnimationPath(MazeView mazeView, List<Integer> x, List<Integer> y) {
			// Get the x,y coordinates
			xsearchPath = x;
			ysearchPath = y;
			// Put the robot down on its first location
			piece = mazeView.addPiece(x.get(0), y.get(0));
			lastX = x.get(0);
			lastY = y.get(0);
			currentMove = 1;
			// Get color
			String color = getRealColor(lastX, lastY);
			colorPath.add(color);
			// Call on the first step of the filtering algorithm
			predict(color, filterDist);
			System.out.println(color + " " + currentMove);
			printPdist(filterDist);
		}

		// Next time slice
		public void doNextMove() {
			// Check for animation finish
			if (currentMove < xsearchPath.size() && animationDone) {
				// Move the robot
				int dx = xsearchPath.get(currentMove) - lastX;
				int dy = ysearchPath.get(currentMove) - lastY;
				animateMove(piece, dx, dy);
				lastX = xsearchPath.get(currentMove);
				lastY = ysearchPath.get(currentMove);
				currentMove++;
				// Get the color of new location
				String color = getRealColor(lastX, lastY);
				colorPath.add(color);
				// Call on filtering again
				predict(color, filterDist);
				System.out.println(color);
				printPdist(filterDist);
			}
			// Finished with moves, got entire color sequence
			if (currentMove >= xsearchPath.size() && animationDone && !fb) {
				fb = true;
				// Call forward-backward smoothing
				// forwardBack(fbDist);
				// Call viterbi
			// viterbi();
			}
		}

		// move the piece n by dx, dy cells
		public void animateMove(Node n, int dx, int dy) {
			animationDone = false;
			TranslateTransition tt = new TranslateTransition(Duration.millis(300), n);
			tt.setByX(PIXELS_PER_SQUARE * dx);
			tt.setByY(-PIXELS_PER_SQUARE * dy);
			// set a callback to trigger when animation is finished
			tt.setOnFinished(new AnimationFinished());

			tt.play();

		}

		// when the animation is finished, set an instance variable flag
		// that is used to see if the path is ready for the next step in the
		// animation
		private class AnimationFinished implements EventHandler<ActionEvent> {
			@Override
			public void handle(ActionEvent event) {
				animationDone = true;

			}
		}

	}

	public void predict(String color, ArrayList<Double> pDist) {

		// For each of the 16 locations in our vector
		for (int i = 0; i < pDist.size(); i++) {
			double r1 = 0, r2 = 0, r3 = 0, r4 = 0, r5 = 0, r6 = 0;
			double blocked = 0;
			// Get x and y coordinates of the location
			int xcor, ycor;
			int tempcord;
			List<Integer> blah = intcor.get(i);
			xcor = blah.get(0);
			ycor = blah.get(1);
			// Check for walls of all possible adjacent locations
			if (maze.isLegal(xcor, ycor + 1)) {
				// If the adjacent spot is open, get the transitional probability
				tempcord = intrep.get(Arrays.asList(xcor, ycor + 1));
				r1 = pDist.get(tempcord) * .25;
			} else {
				// There is a wall
				blocked++;
			}
			if (maze.isLegal(xcor, ycor - 1)) {
				tempcord = intrep.get(Arrays.asList(xcor, ycor - 1));
				r2 = pDist.get(tempcord) * .25;
			} else {
				blocked++;
			}
			if (maze.isLegal(xcor + 1, ycor)) {
				tempcord = intrep.get(Arrays.asList(xcor + 1, ycor));
				r3 = pDist.get(tempcord) * .25;
			} else {
				blocked++;
			}
			if (maze.isLegal(xcor - 1, ycor)) {
				tempcord = intrep.get(Arrays.asList(xcor - 1, ycor));
				r4 = pDist.get(tempcord) * .25;
			} else {
				blocked++;
			}
			// Same location, robot moved into a wall
			tempcord = intrep.get(Arrays.asList(xcor, ycor));
			r5 = pDist.get(tempcord) * (blocked * .25);
			// Sum of all possible adjacent locations probabitlies for single cell
			r6 = r1 + r2 + r3 + r4 + r5;
			// Update step
			update(i, r6, color, pDist);
		}
		// Normalize the probability distribution
		normalize(pDist);
	}

	public void update(int indexx, double r6, String color,
			ArrayList<Double> pDist) {
		// Find the actual color of the location
		int xcor, ycor;
		List<Integer> blah = intcor.get(indexx);
		xcor = blah.get(0);
		ycor = blah.get(1);
		// Check if the color matches
		if (getRealColor(xcor, ycor).equals(color)) {
			// 88% chance given the sensored color matches
			pDist.set(indexx, (r6 * .88));
		} else {
			// Doesnt match
			pDist.set(indexx, (r6 * .04));
		}
	}

	public void normalize(ArrayList<Double> pDist) {
		// Add all probabilities
		double sum = 0;
		for (double x : pDist) {
			sum = sum + x;
		}
		// Divide to normalize
		for (int i = 0; i < pDist.size(); i++) {
			pDist.set(i, (pDist.get(i) / sum));
		}
	}

	public void printPdist(ArrayList<Double> pDist) {
		for (int i = 0; i < pDist.size(); i++) {
			System.out.print(intcor.get(i));
			System.out.println(pDist.get(i));
		}
	}

	public String getRealColor(int x, int y) {
		String correctColor = null;
		if (maze.isr(x, y))
			correctColor = "r";
		if (maze.isy(x, y))
			correctColor = "y";
		if (maze.isg(x, y))
			correctColor = "g";
		if (maze.isb(x, y))
			correctColor = "b";
		return correctColor;
	}

	public String getColor(int x, int y) {
		String correctColor = null;
		String wrongColor = null;
		if (maze.isr(x, y))
			correctColor = "r";
		if (maze.isy(x, y))
			correctColor = "y";
		if (maze.isg(x, y))
			correctColor = "g";
		if (maze.isb(x, y))
			correctColor = "b";
		double chance = Math.random() * 100;
		if (chance < 88) {
			return correctColor;
		} else {
			double chance2 = Math.random() * 100;
			if (chance2 < 33) {
				if (correctColor.equals("r"))
					wrongColor = "b";
				if (correctColor.equals("g"))
					wrongColor = "y";
				if (correctColor.equals("y"))
					wrongColor = "g";
				if (correctColor.equals("b"))
					wrongColor = "r";
			}
			if (chance2 >= 33 && chance2 <= 67) {
				if (correctColor.equals("r"))
					wrongColor = "y";
				if (correctColor.equals("g"))
					wrongColor = "b";
				if (correctColor.equals("y"))
					wrongColor = "r";
				if (correctColor.equals("b"))
					wrongColor = "g";
			}
			if (chance2 > 67) {
				if (correctColor.equals("r"))
					wrongColor = "g";
				if (correctColor.equals("g"))
					wrongColor = "r";
				if (correctColor.equals("y"))
					wrongColor = "b";
				if (correctColor.equals("b"))
					wrongColor = "y";
			}
		}
		return wrongColor;
	}

	public ArrayList<Double> forwardBack(ArrayList<Double> pDist) {
		// Backward message
		ArrayList<Double> b = new ArrayList<Double>();
		// Smoothed
		ArrayList<Double> sv = new ArrayList<Double>();
		// f[0]
		for (int i = 0; i < maze.width * maze.height; i++) {
			pDist.add((double) 1 / 16);
		}
		// b is filled with 1 initially
		for (int i = 0; i < maze.width * maze.height; i++) {
			b.add((double) 1);
		}
		// Get the forward message and record each time slice in a hashmap
		ArrayList<Double> temp1 = new ArrayList<Double>();
		temp1.addAll(pDist);
		forwards.put(0, temp1);
		for (int j = 0; j < colorPath.size(); j++) {
			predict(colorPath.get(j), pDist);
			ArrayList<Double> temp = new ArrayList<Double>();
			temp.addAll(pDist);
			forwards.put(j + 1, temp);
		}
		// Calculate the backward message
		for (int k = colorPath.size() - 1; k > 0; k--) {
			sv = pointMult(forwards.get(k), b);

			normalize(sv);
			
			System.out.println(k);
			printPdist(sv);
			
			b = backward(b, k);
		}
		return sv;
	}

	public ArrayList<Double> backward(ArrayList<Double> b, int time) {
		ArrayList<Double> bb = new ArrayList<Double>();
		// For each location
		for (int i = 0; i < b.size(); i++) {
			double firstarg = 0, thirdarg = 0;
			// Recursively calculate the second argument of 15.9
			firstarg = rbackward(time, i);
			ArrayList<Double> temp = new ArrayList<Double>();
			// We have the forward message at each time saved in hash map
			temp = forwards.get(time - 1);
			thirdarg = temp.get(i);
			// We are changing the backward message for each location
			bb.add(firstarg * thirdarg);
		}
		return bb;
	}

	public Double rbackward(int time, int index) {
		double firstarg = 0;
		// Base case
		if (time == colorPath.size() - 1) {
			int xcor, ycor;
			List<Integer> blah = intcor.get(index);
			xcor = blah.get(0);
			ycor = blah.get(1);
			if (getRealColor(xcor, ycor).equals(colorPath.get(time))) {
				firstarg = .88;
			} else {
				firstarg = .04;
			}
			ArrayList<Double> temp = new ArrayList<Double>();
			temp = forwards.get(time - 1);
			firstarg = firstarg * temp.get(index);
			return firstarg;
		} else {
			// Recursive case
			int xcor, ycor;
			List<Integer> blah = intcor.get(index);
			xcor = blah.get(0);
			ycor = blah.get(1);
			if (getRealColor(xcor, ycor).equals(colorPath.get(time))) {
				firstarg = .88;
			} else {
				firstarg = .04;
			}
			ArrayList<Double> temp = new ArrayList<Double>();
			temp = forwards.get(time - 1);
			firstarg = firstarg * rbackward(time + 1, index) * temp.get(index);
			return firstarg;
		}
	}

	public ArrayList<Double> pointMult(ArrayList<Double> a, ArrayList<Double> b) {
		ArrayList<Double> c = new ArrayList<Double>();
		for (int i = 0; i < a.size(); i++) {
			c.add((a.get(i) * b.get(i)));
		}
		return c;
	}

	public void viterbi() {

		// Initial Reading, t = 1
		ArrayList<Double> tempvitprobs = new ArrayList<Double>();
		for (int h = 0; h < viterbiProbs.size(); h++) {
			int x1cor, y1cor;
			List<Integer> x1 = intcor.get(h);
			x1cor = x1.get(0);
			y1cor = x1.get(1);
			if (getRealColor(x1cor, y1cor).equals(colorPath.get(0))) {
				tempvitprobs.add(.88);
			} else {
				tempvitprobs.add(.04);
			}
		}
		viterbiProbs.clear();
		viterbiProbs.addAll(tempvitprobs);

		// From t = 2
		for (int i = 1; i < colorPath.size() - 1; i++) {
			// To hold the probabilities temporarily
			ArrayList<Double> tempprobs = new ArrayList<Double>();
			// Each location
			for (int j = 0; j < viterbiProbs.size(); j++) {
				// Probability of color given location
				double firstarg = 0;
				int xcor, ycor;
				List<Integer> x1 = intcor.get(j);
				xcor = x1.get(0);
				ycor = x1.get(1);
				if (getRealColor(xcor, ycor).equals(colorPath.get(i + 1))) {
					firstarg = .88;
				} else {
					firstarg = .04;
				}

				// Check all adjacent squares
				double max = 0;
				int bestmove = 0;
				double r1 = 0, r2 = 0, r3 = 0, r4 = 0, r5 = 0;
				double blocked = 0;
				int tempcord;
				if (maze.isLegal(xcor, ycor + 1)) {
					// Get the integer representation of that square
					tempcord = intrep.get(Arrays.asList(xcor, ycor + 1));
					// Get the previous max
					r1 = .25 * viterbiProbs.get(tempcord);
					if (r1 > max) {
						// If this is best move
						max = r1;
						bestmove = tempcord;
					}
				} else {
					blocked++;
				}
				if (maze.isLegal(xcor, ycor - 1)) {
					tempcord = intrep.get(Arrays.asList(xcor, ycor - 1));
					r2 = .25 * viterbiProbs.get(tempcord);
					if (r2 > max) {
						max = r2;
						bestmove = tempcord;
					}
				} else {
					blocked++;
				}
				if (maze.isLegal(xcor + 1, ycor)) {
					tempcord = intrep.get(Arrays.asList(xcor + 1, ycor));
					r3 = .25 * viterbiProbs.get(tempcord);
					if (r3 > max) {
						max = r3;
						bestmove = tempcord;
					}
				} else {
					blocked++;
				}
				if (maze.isLegal(xcor - 1, ycor)) {
					tempcord = intrep.get(Arrays.asList(xcor - 1, ycor));
					r4 = .25 * viterbiProbs.get(tempcord);
					if (r4 > max) {
						max = r4;
						bestmove = tempcord;
					}
				} else {
					blocked++;
				}
				// Same location, hit wall
				tempcord = intrep.get(Arrays.asList(xcor, ycor));
				r5 = viterbiProbs.get(tempcord) * (blocked * .25);
				if (r5 > max) {
					max = r5;
					bestmove = tempcord;
				}
				// Updates to prevent mix up of data
				tempprobs.add(firstarg * max);
				ArrayList<Integer> tempBest = new ArrayList<Integer>();
				tempBest.addAll(bestPath.get(bestmove));
				tempBest.add(j);
				tempbestPath.put(j, tempBest);
			}
			bestPath.clear();
			for (int loc = 0; loc < viterbiProbs.size(); loc++) {
				bestPath.put(loc, tempbestPath.get(loc));
			}
			tempbestPath.clear();
			// Normalize and insert into pDist
			normalize(tempprobs);
			viterbiProbs.clear();
			viterbiProbs.addAll(tempprobs);
			tempprobs.clear();
		}
		// Append the final location and print solution
		viterbiFind();
	}

	public void viterbiFind() {

		double max = 0;
		int bestFinal = 0;
		for (int i = 0; i < viterbiProbs.size(); i++) {
			if (viterbiProbs.get(i) > max)
				max = viterbiProbs.get(i);
			bestFinal = i;
		}
		bestPath.get(bestFinal).add(bestFinal);
		System.out.print(bestPath.get(bestFinal));
	}

}