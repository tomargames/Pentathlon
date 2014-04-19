package org.ToMar.pentathlon;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import org.ToMar.Utils.Functions;
import org.ToMar.Utils.tmButton;
import org.ToMar.Utils.tmColors;
import org.ToMar.Utils.tmLog;

/**
 *
 * @author marie
 */
public class Pentathlon implements Runnable
{
    private Thread thread;
	public static final String COMPILEDATE = "04/15/14";
    public static final int NUMBEROFGAMES = 5;          //change to 5
    public static final int MAXLEVEL = 27;
    public static final int MAZE = 0;
    public static final int U24 = 1;
    public static final int HEX = 2;
    public static final int AA = 3;           // comment out
    public static final int SDF = 4;        //should always be NUMBEROFGAMES - 1
    public static final int[] widths = {750, 300, 400, 450, 400};
    public static final int height = 300;
	public static final int yTITLE = 25;
	public static final int NEWGAME = 0;
	public static final int GAMESAVED = 1;
	public static final int GAMEOVER = 2;
	private int gameStage = 0;
    public static final Color[] bgColors = {tmColors.PALEYELLOW, tmColors.PALEPEACH, tmColors.PALECHARTREUSE, tmColors.PALECYAN, tmColors.PALEORCHID};
    public static final String[] helps = {"MAZE.html", "U24.html", "HEX.html", "AA.html", "SDF.html"};
    public static final String[] titles = {"Maze", "Twenty-Four", "AlphabetSoup", "AnchorsAway", "SameDifference"};
	public static final String NODATA = "No data available";
	public tmLog log = new tmLog(tmLog.PROD);
    private Canvas[] games = new Canvas[NUMBEROFGAMES];      // instances of games
    private int level;           // levels of games
    private ArrayList<String>[] piecesToFind;
    private int[] piecesFound = new int[NUMBEROFGAMES];
    private boolean[] solved = new boolean[NUMBEROFGAMES];
    private boolean[] active = new boolean[NUMBEROFGAMES];
    private boolean[] helped = new boolean[NUMBEROFGAMES];
    private HelpBox[] helpBoxes = new HelpBox[NUMBEROFGAMES];
	private WordList wordList;
	private HistoryList historyList;
    private String[] jewels = new String[NUMBEROFGAMES];
    private JFrame frame;
    private JPanel container = new JPanel();
    private JPanel panel1 = new JPanel();
    private JPanel panel2 = new JPanel();
	private String gameName;
	private String historyFile = "gameHistory.txt";
	private String saveFile = "gameInProgress.txt";
	private ArrayList<String> gameLines;
    JScrollPane jsp;

	@SuppressWarnings("unchecked")
    public void setUp(int numRows, int numCols, int size, int scale)
    {
		log.debug("Pentathlon.setUp");
		piecesToFind = new ArrayList[NUMBEROFGAMES];
        frame = new JFrame("ToMar Pentathlon");
		frame.setIconImage(createImageIcon("PNT.PNG","ToMarPentathlon"));
        for (int i = 0; i < NUMBEROFGAMES; i++)
        {
            helpBoxes[i] = new HelpBox(i);
        }
        games[U24] = new U24(this, U24);
        games[MAZE] = new Maze(numRows, numCols, size, scale, this, MAZE);
        games[SDF] = new SDF(this, SDF);
        games[HEX] = new HEX(this, HEX);
        games[AA] = new AA(this, AA);
        gameStage = NEWGAME;
        container.setBackground(tmColors.BLACK);
        panel1.setBackground(tmColors.DARKGRAY);
        panel2.setBackground(tmColors.DARKGRAY);
        container.setLayout(new BorderLayout());
        panel1.add(games[MAZE]);
        panel1.add(games[SDF]);
        panel2.add(games[AA]);
        panel2.add(games[U24]);
        panel2.add(games[HEX]);
        container.add(panel1, BorderLayout.NORTH);
        container.add(panel2, BorderLayout.SOUTH);
        jsp = new JScrollPane(container);
        frame.add(jsp);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        thread = new Thread(this);
        thread.start();
		ArrayList<String> saveLines = Functions.textFileToArrayList(saveFile);
		log.display("Save file has " + saveLines.size() + " lines");
		if (saveLines.isEmpty())
		{
			newGame();
		}
		else
		{
			// restore saved game
			gameName = saveLines.get(0).substring(0, 14);
			level = Integer.parseInt(saveLines.get(0).substring(14, 16));
			try
			{
				log.debug("Restoring SDF with " + saveLines.get(1));
				(games[SDF].getClass().getMethod("restore", new Class<?>[]{String.class})).invoke(games[SDF], saveLines.get(1));
				resetCounters(SDF);
				log.debug("Restoring AA with " + saveLines.get(3));
				(games[AA].getClass().getMethod("restore", new Class<?>[]{String.class})).invoke(games[AA], saveLines.get(3));
				resetCounters(AA);
				log.debug("Restoring U24 with " + saveLines.get(2));
				(games[U24].getClass().getMethod("restore", new Class<?>[]{String.class})).invoke(games[U24], saveLines.get(2));
				resetCounters(U24);
				int[] gamesToReset = {HEX, MAZE};
				for (int g : gamesToReset)
				{
					(games[g].getClass().getMethod("reInit", (Class<?>[]) null)).invoke(games[g]);
					resetCounters(g);
				}
			}
			catch (Exception e)
			{
				log.error("Error retrieving saved game: " + e);
				newGame();
			}
	    	gameLines = createHistoryData();
		}
    }
	private void resetCounters(int gameIndex)
	{
		log.debug("Pentathlon.resetCounters: " + gameIndex);
		this.setSolved(gameIndex, false);
		this.piecesFound[gameIndex] = 0;
		this.setHelped(gameIndex, false);
		this.setJewels(gameIndex, "");
	}
	public void newGame()
	{
		log.debug("Pentathlon.newGame");
		gameName = Functions.getDateTimeStamp();
    	gameLines = createHistoryData();
		setHistoryList(gameLines, gameLines.size(), 200);
		gameStage = NEWGAME;
		((SDF) games[SDF]).newGame();
        level = 0;						// back door -- set to ((level you want) - 1)
        newLevel();
	}
    public void newLevel()
    {
		log.debug("Pentathlon.newLevel");
		level += 1;
        for (int gameIndex = NUMBEROFGAMES - 1; gameIndex > -1; gameIndex--)
        {
            try
            {
                (games[gameIndex].getClass().getMethod("reInit", (Class<?>[]) null)).invoke(games[gameIndex]);
                if (gameStage == GAMEOVER)
                {
					log.debug("Pentathlon.newLevel: game is over.");
                    break;
                }
                this.piecesFound[gameIndex] = 0;
                this.setSolved(gameIndex, false);
                this.setHelped(gameIndex, false);
                this.setJewels(gameIndex, "");
            }
            catch(Exception e)
            {
                log.error("pentathlon.newLevel.gameIndex = " + gameIndex + " : " + e);
            }
        }
		if (gameStage < GAMEOVER && level > 1)
		{
			saveHistory();
		}
    }
    /*
     * Level shown onscreen will always be level+1
     * For each game, each level
     * 1. init the level, create an arrayList of pieces to be found
     * 2. use this method to set them
     * 3. in the game itself, set active to false
     * 4. in the maze, get the pieces, set numberFound to 0, set solved to false
     * 5. as the pieces are found, use setPiecesFound, with index
     * 6. this method will increment the piecesFound counter for the game
     * 6. when piecesFound counter = size of the arrayList, set active to true for that game
     * 7. when the puzzle is solved in the game, set solved = true
     */
	public void setPiecesToFind(int gameIndex, ArrayList<String> al)
    {
        piecesToFind[gameIndex] = al;
    }
    public boolean allPiecesFound(int gameIndex)
    {
        if (piecesToFind[gameIndex].size() == piecesFound[gameIndex])
        {
            return true;
        }
        return false;
    }
	public tmButton getHelpButton(int gameIndex)
	{
        tmButton helpButton = new tmButton(5, 5, 45, "HELP");
        helpButton.setHeight(20);
        helpButton.setFontSize(14);
        helpButton.setFgColor(Pentathlon.bgColors[gameIndex]);
        helpButton.setBgColor(tmColors.DARKMAGENTA);
        helpButton.setXLabel(5);
        helpButton.setYLabel(15);
		return helpButton;
	}
    public void setPiecesFound(int gameIndex, int pieceIndex)
    {
		log.debug("Pentathlon.setPiecesFound: " + gameIndex + ": " + pieceIndex);
        piecesFound[gameIndex] += 1;
        try
        {
            (games[gameIndex].getClass().getMethod("foundPiece", new Class<?>[]{int.class})).invoke(games[gameIndex], pieceIndex);
            if (allPiecesFound(gameIndex))
            {
                active[gameIndex] = true;
                (games[gameIndex].getClass().getMethod("setMessage", new Class<?>[]{String.class})).invoke(games[gameIndex], "Solve it!");
            }
            else
            {
                (games[gameIndex].getClass().getMethod("setMessage", new Class<?>[]{String.class})).invoke(games[gameIndex], "" + (piecesToFind[gameIndex].size() - piecesFound[gameIndex]) + " more.");
            }
        }
        catch(Exception e)
        {
            log.error("pentathlon.setPiecesFound for gameIndex " + gameIndex +": " + e);
        }
    }
/*	private void displayGameLines(String tag)
	{
		for (int i = 0; i < gameLines.size(); i++)
		{
			log.debug(tag + " gameLine" + i + ": " + gameLines.get(i));
		}
	}
*/	private void saveHistory()
	{
		// at the beginning of each level after 1, write game history and save current game
		// if you're about to start a new game, having won the old one, just write game history
		log.debug("Pentathlon.saveHistory: current gameName is " + gameName);
		ArrayList<String> al = new ArrayList<>();
		// write(or overwrite) the entry for this game to reflect new level
		// each line will be gameName (14) + level (2) + currentDate (8) + finishedIndicator (1)
		// first, write line for current game
		al.add(gameName + Functions.formatNumber(level, 2) + Functions.getDateTimeStamp().substring(0,8) + "N");
		if (!(gameLines.get(0).equalsIgnoreCase(NODATA)))
		{
			for (int i = 0; i < gameLines.size(); i++)
			{
				String storedName = gameLines.get(i).substring(0,14);
				if (!(storedName.equalsIgnoreCase(gameName)))
				{
					log.debug("Pentathlon.saveHistory: writing line " + gameLines.get(i));
					al.add(gameLines.get(i));
				}
			}
	    }
		Functions.arrayListToTextFile(historyFile, al);
		// save game data for restore
		al = new ArrayList<>();
		al.add(gameName + Functions.formatNumber(level, 2));
		al.add(((SDF) games[SDF]).getSaveString());
		al.add(((U24) games[U24]).getSaveString());
		al.add(((AA) games[AA]).getSaveString());
		Functions.arrayListToTextFile(saveFile, al);
		gameStage = GAMESAVED;
	}
    public boolean isGameOver()
    {
        return (gameStage == GAMEOVER) ? true : false;
    }
	public int getGameStage()
	{
		return gameStage;
	}
    public void setGameOver()
    {
		log.debug("Pentathlon.setGameOver");
        gameStage = GAMEOVER;
        try
        {
            for (int i = 0; i < NUMBEROFGAMES; i++)
            {
                (games[i].getClass().getMethod("setMessage", new Class<?>[]{String.class})).invoke(games[i],"Game over.");
                active[i] = false;
            }
            (games[MAZE].getClass().getMethod("repaint", (Class<?>[]) null)).invoke(games[MAZE]);
        }
        catch(Exception ef)
        {
            log.error("pentathlon.endOfGame: " + ef);
        }
		log.debug("Game is over, looking for line to mark as finished. Game name is " + gameName);
		for (int i = 0; i < gameLines.size(); i++)
		{
			log.debug("Looking at " + gameLines.get(i).substring(0,14));
			if (gameName.equalsIgnoreCase(gameLines.get(i).substring(0, 14)))
			{
				log.debug("Found the line: " + gameLines.get(i));
				String s = gameLines.get(i).substring(0,24) + "Y";
				gameLines.set(i, s);
				log.debug("After fixing: " + gameLines.get(i));
				Functions.arrayListToTextFile(historyFile, gameLines);
				break;
			}
		}
		// wipe out gameInProgress file
		Functions.arrayListToTextFile(saveFile, new ArrayList<String>());
    }
    public int getLevel()
    {
        return level;
    }
	public void seeWordList(boolean visible)
	{
		wordList.setVisible(visible);
	}
	public void seeHistory(boolean visible)
	{
		historyList.setVisible(visible);
	}
    public void getHelp(int gameIndex)
    {
        helpBoxes[gameIndex].setVisible(true);
    }
    public ArrayList[] getPiecesToFind()
    {
        return piecesToFind;
    }
    public int[] getPiecesFound()
    {
        return piecesFound;
    }
    public String[] getJewels()
    {
        return jewels;
    }
    public void setJewels(int gameIndex, String jewel)
    {
        this.jewels[gameIndex] = jewel;
    }
    public boolean[] isSolved()
    {
        return solved;
    }
    public void setSolved(int index, boolean solved)
    {
        this.solved[index] = solved;
        if (solved)
        {
            ((Maze) games[MAZE]).checkSolved(true);
        }
    }
    public boolean[] isHelped()
    {
        return helped;
    }
    public void setHelped(int index, boolean helped)
    {
        this.helped[index] = helped;
        if (index > MAZE && helped)
        {
            jewels[index] = "•";
        }
    }
	/** Returns an ImageIcon, or null if the path was invalid. */
	protected Image createImageIcon(String path, String description)
	{
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null)
		{
			return new ImageIcon(imgURL, description).getImage();
		}
		else
		{
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}
	private ArrayList<String> createHistoryData()
	{
		// read in file of game data, and set historyFile for this one
		// each line will be gameName (14) + level (2) + currentDate (8) + finishedIndicator (1)
		log.debug("Pentathlon.createHistoryData");
		ArrayList<String> lines = Functions.textFileToArrayList(historyFile);
		// sort lines
		if (lines.isEmpty())
		{
			lines.add(NODATA);
		}
		else
		{
			boolean flips = true;
			while (flips == true)
			{
				flips = false;
				for (int i = 0; i < lines.size() - 1; i++)
				{
					if (lines.get(i).substring(14).compareTo(lines.get(i+1).substring(14)) < 0)
					{
						String temp = lines.get(i);
						String temp1 = lines.get(i + 1);
						lines.set(i, temp1);
						lines.set(i + 1, temp);
						flips = true;
					}
				}
			}
		}
		setHistoryList(lines, lines.size(), 200);
        return lines;
	}
	public boolean[] isActive()
    {
        return active;
    }
    public void setActive(int index, boolean active)
    {
        this.active[index] = active;
    }
    public JFrame getFrame()
    {
        return frame;
    }
	public void setBgColor(Graphics g, int idx)
	{
	  	if (isActive()[idx])			// this means all pieces have been found
		{
			if (isSolved()[idx])
			{
				g.setColor(tmColors.GRAY);
			}
			else
			{
				g.setColor(bgColors[idx]);
			}
		}
		else if (piecesFound[idx] > 0)
		{
			g.setColor(tmColors.PALEGRAY);
		}
		else
		{
			g.setColor(tmColors.DARKGRAY);
		}
	}
    public static void main(String[] args)
    {
        new Pentathlon().setUp(11, 15, 20, 14);
    }
    public void run()
    {
        while (true)
        {
        }
    }
	private class HelpBox extends JDialog
    {
		private static final long serialVersionUID = 1L;
        int gameIndex;
		JScrollPane jsp;
		JEditorPane jEditorPane;


        public HelpBox(int gameIndex)
        {
            super(frame, "ToMar Pentathlon Help");
            this.gameIndex = gameIndex;
            this.setSize(600, 400);
            try
            {
				jEditorPane = new JEditorPane(new URL("http://www.tomargames.com/ToMar2012/PNT/" + helps[gameIndex]));
            }
            catch   (Exception e)
            {
                log.error("Exception in pentathlon.helpBox, gameIndex " + gameIndex + ": " + e);
            }
			jsp = new JScrollPane(jEditorPane);
            this.getContentPane().add(jsp);
        }
    }
	public void setWordList(ArrayList<String> w, int rows, int cols)
	{
		StringBuilder sb = new StringBuilder(w.size());
		for (int i = 0; i < w.size(); i++)
		{
			sb.append(w.get(i) + "\n");
		}
		wordList = new WordList(sb.toString(), rows, cols);
		wordList.setBounds(600, 20, 100, 400);
	}
	private class WordList extends JDialog
    {
		private static final long serialVersionUID = 1L;
		JScrollPane jsp;
		JTextArea jTextArea;

        public WordList(String wordList, int rows, int cols)
        {
            super(frame, "AnchorsAway Word List");
            this.setSize(20, 400);
            try
            {
				jTextArea = new JTextArea(wordList, rows, cols);
            }
            catch   (Exception e)
            {
                log.error("Exception in pentathlon.WordList: " + e);
            }
			jsp = new JScrollPane(jTextArea);
            this.getContentPane().add(jsp);
        }
    }
	public void setHistoryList(ArrayList<String> w, int rows, int cols)
	{
		StringBuilder sb = new StringBuilder(w.size());
		if (w.get(0).equalsIgnoreCase(NODATA))
		{
			sb.append(NODATA);
		}
		else
		{
			for (int i = 0; i < w.size(); i++)
			{
//				each line will be gameName (14) + level (2) + currentDate (8) + finishedIndicator (1)
				sb.append("Started: " +
						w.get(i).substring(4, 6) + "-" +
						w.get(i).substring(6, 8) + "-" +
						w.get(i).substring(0, 4) + "   Played: " +
						w.get(i).substring(20, 22) + "-" +
						w.get(i).substring(22, 24) + "-" +
						w.get(i).substring(16, 20) + "   Level: " +
						w.get(i).substring(14, 16));
				if ("Y".equalsIgnoreCase(w.get(i).substring(24)))
				{
					sb.append(" WINNER!!!");
				}
				else if (gameName.equalsIgnoreCase(w.get(i).substring(0, 14)))
				{
					sb.append(" current game");
				}
				sb.append("\n");
			}
		}
		historyList = new HistoryList(sb.toString(), rows, cols);
	}
	private class HistoryList extends JDialog
    {
		private static final long serialVersionUID = 1L;
		JScrollPane jsp;
		JTextArea jTextArea;

        public HistoryList(String historyList, int rows, int cols)
        {
            super(frame, "Pentathlon Game History");
            this.setSize(400, 200);
            try
            {
				jTextArea = new JTextArea(historyList, rows, cols);
            }
            catch   (Exception e)
            {
                log.error("Exception in pentathlon.HistoryList: " + e);
            }
			jsp = new JScrollPane(jTextArea);
            this.getContentPane().add(jsp);
        }
    }
}
