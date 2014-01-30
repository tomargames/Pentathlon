package org.ToMar.pentathlon;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
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
	public static final String COMPILEDATE = "12/11/13";
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
    public static final Color[] bgColors = {tmColors.PALEYELLOW, tmColors.PALEPEACH, tmColors.PALECHARTREUSE, tmColors.PALECYAN, tmColors.LIGHTOLIVE};
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
    private boolean gameOver;
	private String gameName;
	private String historyFile = "tmPentathlonGames.txt";
	private String saveFile = "tmPentathlonSave.txt";
	private ArrayList<String> gameLines;
    JScrollPane jsp;

    public void setUp(int numRows, int numCols, int size, int scale)
    {
		log.debug("Pentathlon.setUp");
		piecesToFind = new ArrayList[NUMBEROFGAMES];
    	gameLines = createHistoryData();
		setHistoryList(gameLines, gameLines.size(), 200);
        frame = new JFrame("ToMar Pentathlon");
		frame.setIconImage(createImageIcon("PNT.PNG","ToMarPentathlon"));
		String saveString = getSavedGame();
        for (int i = 0; i < NUMBEROFGAMES; i++)
        {
            helpBoxes[i] = new HelpBox(i);
        }
        games[U24] = new U24(this, U24);
        games[MAZE] = new Maze(numRows, numCols, size, scale, this, MAZE);
        games[SDF] = new SDF(this, SDF);
        games[HEX] = new HEX(this, HEX);
        games[AA] = new AA(this, AA);
        gameOver = false;
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
		if (saveString.isEmpty())
		{
			newGame();
		}
		else
		{
			setSavedData(saveString);
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
	public void setSavedData(String s)
	{
		log.debug("Pentathlon.setSavedData: " + s);
		gameName = s.substring(0, 14);
		level = Integer.parseInt(s.substring(14, 16));
        try
        {
			(games[SDF].getClass().getMethod("restore", new Class<?>[]{String.class})).invoke(games[SDF], s);
			if (!gameOver)
			{
				resetCounters(SDF);
				(games[AA].getClass().getMethod("restore", new Class<?>[]{String.class})).invoke(games[AA], s);
				resetCounters(AA);
				int marker = s.indexOf("*");
				if (marker > -1)
				{
					String u24s = s.substring(marker + 1, marker + 9);
					(games[U24].getClass().getMethod("restore", new Class<?>[]{String.class})).invoke(games[U24], u24s);
				}
				else
				{
					(games[U24].getClass().getMethod("reInit", (Class<?>[]) null)).invoke(games[U24]);
				}
				resetCounters(U24);
				int[] gamesToReset = {HEX, MAZE};
				for (int g : gamesToReset)
				{
					(games[g].getClass().getMethod("reInit", (Class<?>[]) null)).invoke(games[g]);
					resetCounters(g);
				}
				String message = saveHistory();
				if (!message.isEmpty())
				{
	                (games[MAZE].getClass().getMethod("setMessage", new Class<?>[]{String.class})).invoke(games[MAZE], "Error saving files.");
				}
			}
		}
        catch(Exception e)
		{
			log.error("pentathlon.SetSavedData: " + e);
		}
	}
	public void newGame()
	{
		log.debug("Pentathlon.newGame");
		gameName = Functions.getDateTimeStamp();
		if (gameOver)
		{
			log.debug("Starting after beating the old game");
			gameOver = false;
		}
		else
		{
			saveHistory();
		}
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
                if (gameOver)
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
		if (!gameOver)
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
	public String getSavedGame()
	{
		log.debug("Pentathlon.getSavedGame");
		String s = "";
        try
        {
			BufferedReader br = new BufferedReader(new FileReader(new File(saveFile)));
			s = br.readLine();
		}
        catch   (FileNotFoundException fe)
        {
            log.display("No saved game, starting new game: " + fe);
	    }
        catch   (Exception e)
        {
            log.error("Exception reading Pentathlon saved game file: " + e);
        }
		return s;
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
                (games[gameIndex].getClass().getMethod("setMessage", new Class<?>[]{String.class})).invoke(games[gameIndex], "All pieces found! Solve it!");
            }
            else
            {
                (games[gameIndex].getClass().getMethod("setMessage", new Class<?>[]{String.class})).invoke(games[gameIndex], "" + (piecesToFind[gameIndex].size() - piecesFound[gameIndex]) + " more in Level " + level + " maze.");
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
*/	private String saveHistory()
	{
		log.debug("Pentathlon.saveHistory: current gameName is " + gameName);
		StringBuilder sb = new StringBuilder("");
		// write(or overwrite) the entry for this game to reflect new level
		try
		{
	        File file = new File(historyFile);
			try (Writer output = new BufferedWriter(new FileWriter(file)))
			{
				output.write(gameName + Functions.formatNumber(level, 2));
				if (!(gameLines.get(0).equalsIgnoreCase(NODATA)))
				{
					for (int i = 0; i < gameLines.size(); i++)
					{
						String storedName = gameLines.get(i).substring(0,14);
						if (!(storedName.equalsIgnoreCase(gameName)))
						{
							log.debug("Pentathlon.saveHistory: writing line " + gameLines.get(i));
							output.write(gameLines.get(i));
						}
						else
						{
							gameLines.set(i, gameName + Functions.formatNumber(level, 2));
						}
					}
				}
			}
	    }
		catch(Exception e)
		{
			log.error("Could not create file " + historyFile + ": " + e);
			sb.append("Access to " + historyFile + " is denied. ");
		}
		// save game data for restore
		try
		{
	        File file = new File(saveFile);
		    Writer output = new BufferedWriter(new FileWriter(file));
			output.write(gameName + Functions.formatNumber(level, 2));
			output.write(((SDF) games[SDF]).getSaveString());
			output.write(((U24) games[U24]).getSaveString());
			output.write(((AA) games[AA]).getSaveString());
	        output.close();
	    }
		catch(Exception e)
		{
			log.error("Could not create file " + saveFile + ": " + e);
			sb.append("Access to " + saveFile + " is denied. ");
		}
		return sb.toString();
	}
    public boolean isGameOver()
    {
        return gameOver;
    }
    public void setGameOver()
    {
		log.debug("Pentathlon.setGameOver");
        gameOver = true;
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
		log.debug("Pentathlon.createHistoryData");
		ArrayList<String> lines = new ArrayList<>();
        try
        {
			BufferedReader br = new BufferedReader(new FileReader(new File(historyFile)));
			String s = br.readLine();
			while (s.length() > 0)
			{
				lines.add(s.substring(0, 16));
				s = s.substring(16);
			}
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
        catch   (FileNotFoundException fe)
        {
            log.display("Creating new game file: " + fe);
			lines.add(NODATA);
        }
        catch   (Exception e)
        {
            log.error("Exception reading Pentathlon game history file: " + e);
        }
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
		wordList.setBounds(500, 20, 100, 400);
	}
	private class WordList extends JDialog
    {
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
//				log(w.get(i));
				sb.append(w.get(i).substring(4, 6) + "-" + w.get(i).substring(6, 8) + "-" + w.get(i).substring(0, 4) + "    Level " + w.get(i).substring(14) + "\n");
			}
		}
		historyList = new HistoryList(sb.toString(), rows, cols);
	}
	private class HistoryList extends JDialog
    {
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
                log.error("Exception in pentathlon.WordList: " + e);
            }
			jsp = new JScrollPane(jTextArea);
            this.getContentPane().add(jsp);
        }
    }
}
