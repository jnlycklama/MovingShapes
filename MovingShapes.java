import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.util.ArrayList;
import java.util.Collections;


/**
 * The MovingShapes class creates a GUI window that displays a game of moving Squares.
 * The Squares get bigger and bigger as they collide, and the point of the game is to click
 * the biggest Square possible before it turns black. 
 * As extras, the Squares spawn faster and move faster as you get more and more points. The 
 * background is also customizable by colour. If you hit negative points, the Squares will turn
 * into Doges and that will continue for the rest of the game. 
 * 
 * This class was written and modified from the class MovingShapes given by Margaret Lamb. 
 * 
 */
public class MovingShapes extends JFrame implements ActionListener {

	//Constants
    private static final int GREEN_SCORE = 1;
    private static final int BLUE_SCORE = 2;
    private static final int RED_SCORE = 3;
    private static final int GRAY_SCORE = 4;
    private static final int BLACK_SCORE = -1;
    
    private final int FIRST_BACKGROUND = 1; 
    private final int LAST_BACKGROUND = 5;
    
    private final static String FILE_NAME = "scores.ser";

    // Initial dimensions of the inner panel.  The user can change the size of the frame while
    // the program is running.
    private static final int INITIAL_PANEL_WIDTH = 600;
    private static final int INITIAL_PANEL_HEIGHT = 400;

    // Sequence of colors for squares -- they start at the first color
    // and move to the next after each collision until they reach the last
    // private static final Color squareColors[] = {Color.GREEN, Color.BLUE, Color.RED, Color.GRAY, Color.BLACK};
    private static final Color squareColors[] = {Color.GREEN, Color.BLUE, Color.RED, Color.GRAY, Color.BLACK};
    private static final Color INITIAL_COLOR = squareColors[0];
    private static final Color LAST_COLOR = squareColors[squareColors.length-1];

    // Number of pixels a square grows after each collision
    private final int SIZE_INCREMENT = 10;

    //Instancewide static variables, generally static for ease of access from Square class
    private static boolean isPaused = false;
    private static boolean doge = false;
    private static int finalScore = 0;
    private static Image dogeImage;
    private static int highestScore = 0;

    // initial size for squares
    private int INITIAL_SQUARE_SIZE = 20;
    
    // Count of number of ticks until it's time to create a new square
    private int creationCountdown = 0;
    // Number of ticks between creation of new squares.  If you want fewer or more shapes,
    // you can change this number.
    private int CREATION_INTERVAL = 300; // 3 seconds
    private String name = null;


    // A timer to "tick" every 20 milliseconds (or as close to that as the system can manage).
    // If you want the program to run slower while you're debugging, just increase the 20 to
    // a larger number.
    private Timer timer = new Timer(20, this);

    // A list of the squares showing in the window.  We'll be talking about the ArrayList class soon;
    // it's like a Python list.
    private ArrayList<Square> squareList = new ArrayList<Square>();

    // Pointer to the main frame of the program (for referencing from inside inner classes)
    private JFrame thisFrame = this;
    
    // The inner area of the window -- the part that can contain squares (doesn't include
    // title, menu bar, and borders)
    private MovingSquarePanel innerPanel = new MovingSquarePanel();

    //Additional panels
    private JPanel scorePanel = new JPanel();
    private JPanel TextEntryPanel = new JPanel();
    private JPanel ActionButtons = new JPanel();
    private JPanel BackgroundPanel = new JPanel();
    
    //Additional buttons/text field
    private JLabel scoreLabel = new JLabel("Your score is 0");
    
    private NameTextField nameTextEntry = new NameTextField();

    private JButton PauseButton = new JButton("Pause");
    private JButton StopButton = new JButton("Stop");
    
    private JButton BackgroundButton = new JButton("Randomize!");

    // Constructor to set up the window for the program
    public MovingShapes() {
        // set position of window in screen (better for demo videos)
        setLocation(new Point(600,100));
        setTitle("Moving Squares"); 
        // Make sure program cleans itself up when the user closes the window
        setDefaultCloseOperation(EXIT_ON_CLOSE); 
        // Make the inner panel part of the window and specify its initial size
        getContentPane().add(innerPanel);
        // Add a "listener" to react every time the size of the window is changed.
        innerPanel.addComponentListener(new Resizer());
        innerPanel.setPreferredSize(new Dimension(INITIAL_PANEL_WIDTH, INITIAL_PANEL_HEIGHT));
        innerPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        // Specify that the actionPerformed method will be called each time the timer ticks
        timer.addActionListener(this);
        innerPanel.addMouseListener(new ClickListener());
        PauseButton.addMouseListener(new ButtonListener());
        // If the stop button is pressed, the program will exit
        StopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                System.exit(0);
            }
        });
        ActionButtons.add(StopButton);
        ActionButtons.add(PauseButton);
        ActionButtons.setBackground(Color.CYAN);

        //Sets up the panel for entering your name
        TextEntryPanel.setLayout(new GridLayout(0,3));
        TextEntryPanel.add(new JPanel(){
            {
                this.setVisible(false);
            }
        }); 
        TextEntryPanel.add(nameTextEntry);
        TextEntryPanel.setBackground(Color.MAGENTA);

        //Sets up the panel for your score
        scorePanel.add(scoreLabel);
        scorePanel.setBackground(Color.LIGHT_GRAY);
        
        BackgroundButton.setFont(new Font("Comic Sans MS", Font.PLAIN, 11));
        
        //randomizes the background
        BackgroundButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            // yellow, cyan, magenta, purple, white 
            int randomBackground = FIRST_BACKGROUND + (int)(Math.random() * LAST_BACKGROUND); 
            if (randomBackground == 1) 
                innerPanel.setBackground(Color.YELLOW); 
            else if (randomBackground == 2) 
                innerPanel.setBackground(Color.WHITE);
            else if (randomBackground == 3) 
                innerPanel.setBackground(Color.ORANGE); 
            else if (randomBackground == 4) 
                innerPanel.setBackground(Color.CYAN);
            else if (randomBackground == 5) 
                innerPanel.setBackground(Color.MAGENTA); } });
        BackgroundPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        BackgroundPanel.add(BackgroundButton); 
        BackgroundPanel.setBackground(Color.CYAN);

        thisFrame.getContentPane().add(scorePanel, BorderLayout.NORTH);
        thisFrame.getContentPane().add(TextEntryPanel, BorderLayout.SOUTH);
        thisFrame.getContentPane().add(ActionButtons, BorderLayout.EAST);
        thisFrame.getContentPane().add(BackgroundPanel, BorderLayout.WEST);

        // Now that everything's set up, show the window on the screen and start the timer
        pack(); 
        setVisible(true); 
        timer.start();
    } // end MovingShapes


    // This method is called each time the timer "ticks".  It updates the position of every
    // square, creates a new square if it's time, and handles collisions.
    public void actionPerformed(ActionEvent e) {
        /***** Make each square move.  *****/
        // The square itself will know what direction it needs to move and what to do if it 
        // hits the boundary of the panel.  All we need to do here is tell it to move.
        for (Square square: squareList) 
            square.move(); 
        // Search for collisions.  When a pair of collide, the newer one disappears and the
        // older one grows and changes color (unless it's already black)
        for (int i = 0; i < squareList.size(); i++) {
            Square squareA = squareList.get(i);
            for (int j = i+1; j < squareList.size(); j++) {
                Square squareB = squareList.get(j);
                /* see if the two squares are have collided */
                if (Square.overlap(squareA,squareB)) {
                    // Get rid of squareB
                    squareList.remove(j);
                    j--; // so that we won't skip checking the next square
                    // squareA grows & changes to next color unless it's already at the last color (black)
                    squareA.grow(SIZE_INCREMENT);
                    Color colorA = squareA.getColor();
                    boolean found = false;
                    for (int colorIndex = 0; !found && colorIndex < squareColors.length-1; colorIndex++) {
                        if (colorA == squareColors[colorIndex]) {
                            colorA = squareColors[colorIndex+1];
                            squareA.setColor(colorA);
                            found = true;
                        } // end if
                    } // end for
                    if (colorA == LAST_COLOR)
                        squareA.stop();
                } // end if
            } // end for

        } // end for
        // If it's time to create a new square, do that, but make sure it doesn't appear
        // on top of an existing square.  If the screen is so full that this can't be done
        // after the maximum number of tries, the program ends.
        final int MAX_TRIES = 100; // number of times we try to create a new square before giving up
        if (creationCountdown == 0) {
            int tries = 0; // number of times we try to create a new square in a place
            boolean success = false; // becomes true when we've successfully created a new square that doesn't overlap with an older one
            Square newSquare = null; 
            while (!success && tries < MAX_TRIES) {
                newSquare = new Square(INITIAL_SQUARE_SIZE, INITIAL_COLOR); // square constructor picks a random direction and position
                // See if the new square overlaps any of the others
                boolean hasOverlap = false; // true if the new square overlaps with an existing one
                for (int i = 0; i < squareList.size() && !hasOverlap; i++) {
                    if (Square.overlap(squareList.get(i),newSquare)) {
                        hasOverlap = true;
                    } // end if
                } // end for
                if (!hasOverlap) {
                    success = true;
                    break;
                }
                else {
                    tries++;
                } // end if
            } // end while
            if (success) {
                squareList.add(newSquare); 
                creationCountdown = CREATION_INTERVAL;
                // re-start count until time to add another shape
                if (CREATION_INTERVAL > 20){
                    CREATION_INTERVAL--;
                }
            }
            else {
                // Could not create a new square without overlapping with another: end program.
                if (doge){
                    JOptionPane.showMessageDialog(thisFrame, "WOW, SUCH LOSS.");
                    JOptionPane.showMessageDialog(thisFrame, "MANY DOGE");
                    JOptionPane.showMessageDialog(thisFrame, "MUCH POINTS");
                    JOptionPane.showMessageDialog(thisFrame, "SO SQUARE");
                    JOptionPane.showMessageDialog(thisFrame, "HIGH SCORES");
                } else {
                	JOptionPane.showMessageDialog(thisFrame, "SCREEN IS TOO FULL; DISPLAYING HISCORES");
                }
                timer.stop();
				hiScores(name, finalScore);
				this.setVisible(false);
            } // end if
        }
        else {
            creationCountdown--;
        } // end if

        // Tell the inner panel to re-display its contents according to the 
        // updated list of squares
        innerPanel.repaint();

    } // end actionPerformed
    
    
    private void hiScores(String userName, int userScore) {
		ArrayList<ScoreObject> scores = getScoresFromFile();
		scores.add(new ScoreObject(userName, userScore));
		Collections.sort(scores);
		scores.remove(scores.size()-1);
		ScoreBoard scoreBoard = new ScoreBoard(scores);
		saveToFile(scores);
	}


	private void saveToFile(ArrayList<ScoreObject> scores) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(FILE_NAME);
			out = new ObjectOutputStream(fos);
			out.writeObject(scores);
			out.close();
		} catch (Exception ex) {
			System.out.println("Saving file failed! Unexpected Error.");
		}	
	}


	private ArrayList<ScoreObject> getScoresFromFile() {
		ArrayList<ScoreObject> scores = new ArrayList<ScoreObject>();
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(FILE_NAME);
			in = new ObjectInputStream(fis);
			scores = (ArrayList<ScoreObject>) in.readObject();
			in.close();
		} catch (Exception ex) {
			System.out.println("Reading file failed! Defaulting scores.");
			scores = new ArrayList<ScoreObject>();
			scores.add(new ScoreObject("Anonymous", -1));
			scores.add(new ScoreObject("Anonymous", -1));
			scores.add(new ScoreObject("Anonymous", -1));
			scores.add(new ScoreObject("Anonymous", -1));
			scores.add(new ScoreObject("Anonymous", -1));
		}
		return scores;
	}

    //returns true if the game is paused
    public static boolean getIsPaused() {
        return isPaused;
    } // end getIsPaused

    //After you click a Square, it adds however much that Square is worth to your existing points
    public void score(Color squareColor) {
        if (isPaused == false){
            if (squareColor == Color.GREEN) {
                finalScore += GREEN_SCORE; 
            } else if (squareColor == Color.BLUE) {
                finalScore += BLUE_SCORE; 
            }else if (squareColor == Color.RED) {
                finalScore += RED_SCORE; 
            }else if (squareColor == Color. GRAY) { 
                finalScore += GRAY_SCORE; 
            }else if (squareColor == Color.BLACK) { 
                finalScore += BLACK_SCORE;
            }
            }
        //if you get negative points, you will go into "doge mode"
        if (finalScore < 0){ 
            doge = true; 
        }
        //changes the score label once you add your name
        if (name == null){
            scoreLabel.setText("Your score is "+finalScore); 
        }else{
            scoreLabel.setText(name+", your score is "+finalScore); 
        }
        if (finalScore > highestScore)
			highestScore = finalScore;
    } // end score
    
    //returns true if in "doge mode" (negative points)
    public static boolean getDogeMode(){
        return doge;
    } // end getDogeMode

    //Gets the doge image from the project folder
    public static Image getDogeImage(){
        //If you have already gotten the image, use the existing image
        if (dogeImage != null) {
            return dogeImage;
        }
        BufferedImage img = null;
        //If you haven't gotten the image yet, take it from the folder
        try {
            img = ImageIO.read(new File("doge.jpg"));
        } catch (IOException e) {

        }
        dogeImage = img;
        return dogeImage;
    } // end getDogeImage

    // This is an inner class that specifies what should happen if the window is resized.
    // Its componentResized method will be called at the start of the program and then
    // each time the user re-sizes the window.
    // It's responsible for informing the square class of its new limits.  
    private class Resizer extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            // Get the new dimensions of the inner panel
            int panelWidth = innerPanel.getWidth();
            int panelHeight = innerPanel.getHeight();

            // Tell the Square class that the size of its enclosing panel has changed 
            Square.setPanelDimensions(panelWidth, panelHeight);

            for(Square square:squareList){
                square.fixBounds();
            }

        } // end componentResized
    } // end Resizer


    // This is an inner class for the inner panel.  It adds knowledge about how to "paint" the
    // contents of the panel to the standard JPanel class
    private class MovingSquarePanel extends JPanel {
        // This method describes how to "paint" the squares inside the panel
        public void paintComponent(Graphics gc) {
            super.paintComponent(gc); // default panel drawing
            // draw each square in the panel
            for (Square square: squareList) {
                square.paint(gc);
            } // end for
        } // end paintComponent  
    } // end class MovingSquarePanel

    //Creates the field for where you enter your name in the JFrame
    private class NameTextField extends JTextField {
        //Watches the text field, so when something is entered the program will know
        public NameTextField(){
            this.addKeyListener(new EnterListener());
        } // end NameTextField
        //After the name is entered, reset the score label to include their name
        public void enterCallBack(){
            name = this.getText();
            scoreLabel.setText(name+", your score is "+finalScore); 
            this.setVisible(false);
            this.getTopLevelAncestor().repaint();
        } // end enterCallBack

    }// end NameTextField

    //Listener for when the Pause button is pressed
    public class ButtonListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (isPaused == true) {
                timer.start();
                //Makes the button say "Pause" after the game is resumed
                PauseButton.setText("Pause");
                isPaused = false;
            }
            else {
                if (isPaused == false) {
                    isPaused = true;
                    timer.stop();
                    //Makes the button say "resume" once it has been pressed
                    PauseButton.setText("Resume");
                }
            }
        } // end mouseClicked
    }// end ButtonListener

    //This class listens to the clicks made by the user
    //It is also how the speed and spawn rate changes as
    //the user gets more and more points
    public class ClickListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            int x = event.getX();
            int y = event.getY();
            for (int i = 0; i < squareList.size(); i++){
                if (isPaused == false) {
                    if (squareList.get(i).inside(x,y)){
                        score(squareList.get(i).getColor());
                        squareList.remove(i);
                        if((finalScore/3 >= 1) && timer.getDelay() > 16){
                            timer.setDelay(16); // makes the squares go faster if final score > 3
                            if (CREATION_INTERVAL > 50){
                                CREATION_INTERVAL -= 50; //decreases the time between spawns
                            }
                        } else if (finalScore/3/3 >= 1 && timer.getDelay() > 12){
                            timer.setDelay(12); // makes the squares go faster if final score > 9
                            if (CREATION_INTERVAL > 50){
                                CREATION_INTERVAL -= 50; //decreases the time between spawns
                            }
                        } else if (finalScore/3/3/3 >= 1 && timer.getDelay() > 8){
                            timer.setDelay(8); // makes the squares go faster if final score > 27
                            if (CREATION_INTERVAL > 50){
                                CREATION_INTERVAL -= 50; //decreases the time between spawns
                            }
                        } else if (finalScore/3/3/3/3 >= 1 && timer.getDelay() > 4){
                            timer.setDelay(4); // makes the squares go faster if final score > 81
                            if (CREATION_INTERVAL > 50){
                                CREATION_INTERVAL -= 50; //decreases the time between spawns
                            }
                        }
                    }
                }
            }
        } // end mousePressed
    } // end ClickListener

    //Listens for when the Enter Key is pressed
    public class EnterListener extends KeyAdapter{

        @Override
        public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_ENTER){
                //If the Enter key is pressed, change the score label
                ((NameTextField) event.getComponent()).enterCallBack();
            }
        }// end keyPressed
    } // end EnterListener


    public static void main(String args[]) {
        // create an instance of this class and let it run
    	new MovingShapes();
	} // end main

} // end class MovingShapes 