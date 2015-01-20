package assignment_mazeworld;

import java.util.ArrayList;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class MazeView extends Group {

	private int pixelsPerSquare;
	private Maze maze;
	private ArrayList<Node> pieces;
	
	private int numCurrentAnimations;
	
	private static Color[] colors = {Color.RED, Color.ORANGE, Color.BLACK, Color.BROWN,
		Color.DARKGOLDENROD, Color.GREEN, Color.BLUE, Color.VIOLET, Color.CRIMSON};

	int currentColor;
	
	public MazeView(Maze m, int pixelsPerSquare) {
		currentColor = 0;
		
		pieces = new ArrayList<Node>();
		
		maze = m;
		this.pixelsPerSquare = pixelsPerSquare;

		for (int c = 0; c < maze.width; c++) {
			for (int r = 0; r < maze.height; r++) {

				int x = c * pixelsPerSquare;
				int y = (maze.height - r - 1) * pixelsPerSquare;

				Rectangle square = new Rectangle(x, y, pixelsPerSquare,
						pixelsPerSquare);
				square.setStroke(Color.WHITE);	
				if(maze.getChar(c, r) == 'r') {
					square.setFill(Color.RED);
				} 
				if(maze.getChar(c, r) == 'g') {
					square.setFill(Color.GREEN);
				} 
				if(maze.getChar(c, r) == 'b') {
					square.setFill(Color.BLUE);
				} 
				if(maze.getChar(c, r) == 'y') {
					square.setFill(Color.YELLOW);
				} 	
				this.getChildren().add(square);
			}
		}
	}

	private int squareCenterX(int c) {
		return c * pixelsPerSquare + pixelsPerSquare / 2;
		
	}
	private int squareCenterY(int r) {
		return (maze.height - r) * pixelsPerSquare - pixelsPerSquare / 2;
	}
	
	// create a new piece on the board.
	//  return the piece as a Node for use in animations
	public Node addPiece(int c, int r) {
		
		int radius = (int)(pixelsPerSquare * .4);

		Circle piece = new Circle(squareCenterX(c), squareCenterY(r), radius);
		piece.setFill(Color.BLACK);
		currentColor++;
		
		this.getChildren().add(piece);
		return piece;
		
	}
	
	
	
	


}
