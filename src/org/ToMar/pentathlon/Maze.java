package org.ToMar.pentathlon;

import java.awt.Graphics;
import java.util.ArrayList;
import org.ToMar.Utils.*;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 *
 * @author marie
 * This is branching from the Maze class in the Maze package
 */

public class Maze extends Canvas implements MouseListener, KeyListener
{
    private static final long serialVersionUID = -3575578768327824256L;
    private Maze.Cell[] maze;
    private int maxRows;
    private int maxCols;
    private int size;
    private int scale;
    private int entranceIdx;
    private int exitIdx;
    private int youIdx;
	int yPad;
    private int xStart;
    private int yMessage = 292;
    private Maze.Direction youDoor;
    private Maze.Door westDoor;
    private Maze.Door eastDoor;
    private Maze.Door northDoor;
    private Maze.Door southDoor;
    private Polygon frontPanel;
    private Polygon backPanel;
    private Polygon floorPanel;
    private Polygon ceilingPanel;
    private boolean showWalls;
    private Pentathlon pentathlon;
    private String message = "";
    private boolean levelSolved;
    private tmButton helpButton;
    private tmButton newGameButton;
	private tmButton historyButton;
	private int GAMEINDEX;

    public Maze() {}
    public Maze(int maxRows, int maxCols, int size, int scale, Pentathlon pentathlon, int idx)
    {
		pentathlon.log.debug("Maze.constructor");
		this.GAMEINDEX = idx;
        this.addMouseListener(this);
        this.addKeyListener(this);
        this.pentathlon = pentathlon;
        this.scale = scale;					// scale is 14
        this.size = size;
        this.maxRows = maxRows;
        this.maxCols = maxCols;
        this.setSize(Pentathlon.widths[Pentathlon.MAZE], Pentathlon.height);
        this.entranceIdx = (maxRows % 2 == 1) ? maxCols * maxRows/2 - maxCols/2 : maxRows/2 * maxCols;
        this.exitIdx = entranceIdx - 1;
        repaint();
        xStart = (maxCols + 6) * size;
		yPad = 15;
        int[] xFront = {xStart, xStart + (20*scale), xStart + (20*scale), xStart};
        int[] yFront = {5*scale + yPad, 5*scale + yPad, 17*scale + yPad, 17*scale + yPad};
        frontPanel = new Polygon(xFront, yFront, 4);
        int[] xBack = {xStart + 4*scale, xStart + 16*scale, xStart + 16*scale, xStart + 4*scale};
        int[] yBack = {4*scale + yPad, 4*scale + yPad, 11*scale + yPad, 11*scale + yPad};
        backPanel = new Polygon(xBack, yBack, 4);
        int[] xFloor = {xStart, xStart + 4*scale, xStart + 16*scale, xStart + 20*scale};
        int[] yFloor = {17*scale + yPad, 11*scale + yPad, 11*scale + yPad, 17*scale + yPad};
        floorPanel = new Polygon(xFloor, yFloor, 4);
        int[] yCeiling = {5*scale + yPad, 4*scale + yPad, 4*scale + yPad, 5*scale + yPad};
        ceilingPanel = new Polygon(xFloor, yCeiling, 4);
        int[] xSouth = {xStart + 8*scale, xStart + 12*scale, xStart + 12*scale, xStart + 8*scale};
        int[] ySouth = {14*scale + yPad, 14*scale + yPad, 17*scale + yPad, 17*scale + yPad};
        southDoor = new Maze.Door(xSouth, ySouth, tmColors.DARKGREEN);
        int[] xNorth = {xStart + 8*scale, xStart + 12*scale, xStart + 12*scale, xStart + 8*scale};
        int[] yNorth = {8*scale + yPad, 8*scale + yPad, 11*scale + yPad, 11*scale + yPad};
        northDoor = new Maze.Door(xNorth, yNorth, tmColors.DARKBLUE);
        // east door should be along the line from 16,12 to 20,18
        int[] xEast = {xStart + 17*scale, xStart + 19*scale, xStart + 19*scale, xStart + 17*scale};
        int[] yEast = {10*scale + yPad, 12*scale + yPad, 15*scale + yPad + (scale/2), 12*scale + yPad + (scale/2)};
        eastDoor = new Maze.Door(xEast, yEast, tmColors.LIGHTPURPLE);
        // west door should be along the line from 0,18 to 4,12
        int[] xWest = {xStart + 1*scale, xStart + 3*scale, xStart + 3*scale, xStart + 1*scale};
        int[] yWest = {12*scale + yPad, 10*scale + yPad, 12*scale + yPad + (scale/2), 15*scale + yPad + (scale/2)};
        westDoor = new Maze.Door(xWest, yWest, tmColors.ORANGE);
		helpButton = pentathlon.getHelpButton(Pentathlon.MAZE);
        helpButton.setX(Pentathlon.widths[Pentathlon.MAZE] - 50);
        historyButton = new tmButton(Pentathlon.widths[Pentathlon.MAZE] - 145, 5, 85, "GAME HISTORY");
        historyButton.setHeight(20);
        historyButton.setFontSize(10);
        historyButton.setFgColor(Pentathlon.bgColors[Pentathlon.MAZE]);
        historyButton.setBgColor(tmColors.DARKGREEN);
        historyButton.setXLabel(5);
        historyButton.setYLabel(14);
        newGameButton = new tmButton(Pentathlon.widths[Pentathlon.MAZE] - 220, 5, 65, "NEW GAME");
        newGameButton.setHeight(20);
        newGameButton.setFontSize(10);
        newGameButton.setFgColor(Pentathlon.bgColors[Pentathlon.MAZE]);
        newGameButton.setBgColor(tmColors.DARKBLUE);
        newGameButton.setXLabel(5);
        newGameButton.setYLabel(14);
    }
    public void reInit()
    {
		try
		{
		pentathlon.log.debug("Maze.reInit");
        message = "Solve the Level " + pentathlon.getLevel() + " maze.";
        maze = new Maze.Cell[maxRows * maxCols];
        Maze.Direction.setMaxes(maxRows, maxCols);
        for (int i = 0; i < maxRows * maxCols; i++)
        {
            maze[i] = new Maze.Cell(i);
        }
        ArrayList<Integer> trail = new ArrayList<>();
        int pick = Functions.getRnd(maze.length);
        int newPick;
        int pickRnd = 0;
        int possRnd;
        maze[pick].setVisited(true);
        trail.add(pick);
        while(!trail.isEmpty())
        {
            ArrayList<Integer> possibles = new ArrayList<>();
            ArrayList<Maze.Direction> dirs = new ArrayList<>();
            for (Maze.Direction dir : Maze.Direction.values())
            {
                newPick = Maze.Direction.getNewIndex(pick, dir);
                if (newPick > -1 && !maze[newPick].isVisited())
                {
                    possibles.add(newPick);
                    dirs.add(dir);
                }
            }
            // if there are no possibles, remove it from the trail
            if (possibles.isEmpty())
            {
                trail.remove(pickRnd);
                if (trail.isEmpty())
                {
                    break;
                }
            }
            else
            {
                possRnd = Functions.getRnd(possibles.size());
                newPick = possibles.get(possRnd);
                maze[newPick].setVisited(true);
                trail.add(newPick);
                switch (dirs.get(possRnd))
                {
                    case NORTH:
                        maze[newPick].setSouthWall(false);
                        break;
                    case SOUTH:
                        maze[pick].setSouthWall(false);
                        break;
                    case EAST:
                        maze[pick].setEastWall(false);
                        break;
                    case WEST:
                        maze[newPick].setEastWall(false);
                }
            }
            pickRnd = Functions.getRnd(trail.size());
            pick = trail.get(pickRnd);
        }
        for (int i = 0; i < maze.length; i++)
        {
            maze[i].setVisited(false);
        }
        this.youIdx = this.entranceIdx;
        maze[exitIdx].setExit();
        maze[youIdx].setYou(true);
        youDoor = Maze.Direction.WEST;
        showWalls = false;                      // CHANGE THIS!!!!!
        int numberOfPieces = 0;
        for (int gameIndex = 1; gameIndex < Pentathlon.NUMBEROFGAMES; gameIndex++)
        {
            numberOfPieces += pentathlon.getPiecesToFind()[gameIndex].size();
        }
        int[] picks = Functions.randomPicks(maze.length, numberOfPieces);
        numberOfPieces = -1;
        for (int gameIndex = 1; gameIndex < Pentathlon.NUMBEROFGAMES; gameIndex++)
        {
            for (int pieceIndex = 0; pieceIndex < pentathlon.getPiecesToFind()[gameIndex].size(); pieceIndex++)
            {
                maze[picks[++numberOfPieces]].setJewelGame(gameIndex);
                maze[picks[numberOfPieces]].setJewelIndex(pieceIndex);
            }
        }
        levelSolved = false;
        pentathlon.setActive(Pentathlon.MAZE, true);
        repaint();
		} catch (Exception e)
		{
			pentathlon.log.error("Pentathlon.Maze.Exception in reInit: " + e);
		}
    }
    public void paint(Graphics g)
    {
        if (pentathlon.isGameOver())
        {
            int lev = pentathlon.getLevel() - 1;
            g.setFont(tmFonts.PLAIN24);
            g.setColor(tmColors.RED);
            g.drawString("YOU WIN!!!!!!", 10, 30);
            g.setColor(tmColors.DARKBLUE);
            g.drawString("There are no more sets to find, so there are no more levels!", 10, 60);
            g.drawString("You completed level " + lev + "!", 10, 90);
            g.setColor(tmColors.DARKGREEN);
            g.drawString("Please email me at tomargames@gmail.com.", 10, 120);
            g.drawString("Include your name and what level you reached.", 10, 150);
            g.drawString("Then look for your name at in the Maze Help window!", 10, 180);
            newGameButton.draw(this.getGraphics());
        }
        else
        {
            for (int i = 0; i < maze.length; i++)
            {
                maze[i].draw(this.getGraphics(), showWalls);
            }
            Maze.Cell north = (Maze.Direction.getNewIndex(youIdx, Maze.Direction.NORTH) > -1) ? maze[Maze.Direction.getNewIndex(youIdx, Maze.Direction.NORTH)] : null;
            Maze.Cell west = (Maze.Direction.getNewIndex(youIdx, Maze.Direction.WEST) > -1) ? maze[Maze.Direction.getNewIndex(youIdx, Maze.Direction.WEST)] : null;
            maze[youIdx].drawCell(this.getGraphics(), north, west);
            helpButton.draw(this.getGraphics());
            historyButton.draw(this.getGraphics());
			if (pentathlon.getLevel() > 1)
			{
				newGameButton.draw(this.getGraphics());
			}
			g.setFont(tmFonts.PLAIN10);
			g.setColor(tmColors.BLACK);
			g.drawString(Pentathlon.COMPILEDATE, 2, 295);
        }
    }
    public void update(Graphics g)
	{
		g.setColor(Pentathlon.bgColors[Pentathlon.MAZE]);
		g.fillRect(0, 0, Pentathlon.widths[Pentathlon.MAZE], Pentathlon.height);
		paint(g);
	}
    public void mouseClicked(MouseEvent e)
    {
		if (pentathlon.isGameOver())
		{
            if (newGameButton.clicked(e.getX(), e.getY()))
            {
				pentathlon.newGame();
			}
		}
		if (pentathlon.isActive()[Pentathlon.MAZE])
        {
            message = "";
            if (newGameButton.clicked(e.getX(), e.getY()))
            {
				if ("CONFIRM".equalsIgnoreCase(newGameButton.getLabel()))
				{
					pentathlon.newGame();
					newGameButton.setLabel("NEW GAME");
				}
				else
				{
					newGameButton.setLabel("CONFIRM");
				}
            }
			else
			{
				newGameButton.setLabel("NEW GAME");
				if (northDoor.clicked(e))
				{
				   processDoor(Maze.Direction.NORTH);
				}
				else if (southDoor.clicked(e))
				{
					processDoor(Maze.Direction.SOUTH);
				}
				else if (eastDoor.clicked(e))
				{
					if (levelSolved)
					{
						pentathlon.newLevel();
					}
					else
					{
						processDoor(Maze.Direction.EAST);
					}
				}
				else if (westDoor.clicked(e))
				{
					processDoor(Maze.Direction.WEST);
				}
				else if (helpButton.clicked(e.getX(), e.getY()))
				{
					pentathlon.getHelp(Pentathlon.MAZE);
				}
				else if (historyButton.clicked(e.getX(), e.getY()))
				{
					pentathlon.seeHistory(true);
				}
			}
            checkExit();
            repaint();
        }
    }
    public void checkExit()
    {
        if (maze[youIdx].isExit())
        {
            if (!(pentathlon.isSolved()[Pentathlon.MAZE]))
            {
                pentathlon.setSolved(Pentathlon.MAZE, true);
            }
            else
            {
                checkSolved(false);
            }
        }
    }
    public void checkSolved(boolean newSolve)
    {
        // this method determines if there are unfound pieces and unsolved puzzles
        // it gets called whenever a game is set to solved (true)
		// it also gets called when you're sitting at the exit (false)
        // first it looks at piecesFound, and the first one it finds that is false, it will show those pieces
        boolean allFound = true;
        if (!(pentathlon.isHelped()[Pentathlon.MAZE]))
        {
//			pentathlon.log("Showing the walls.");
            showWalls = true;
            pentathlon.setHelped(Pentathlon.MAZE, true);
            allFound = false;
        }
        else
        {
            for (int gameIndex = 1; gameIndex < Pentathlon.NUMBEROFGAMES; gameIndex++)
            {
//				pentathlon.log("Looking at " + Pentathlon.titles[gameIndex]);
                if (pentathlon.allPiecesFound(gameIndex))
                {
//					pentathlon.log("All its pieces have been found.");
					pentathlon.setHelped(gameIndex, true);
                }
                else
                {
	                allFound = false;
	                if (newSolve)
					{
//						pentathlon.log("newSolve is " + newSolve + ", helped is " + pentathlon.isHelped()[gameIndex]);
						if (!(pentathlon.isHelped()[gameIndex]))
						{
//							pentathlon.log("Showing pieces for it and breaking.");
							message = "Here's some help!";
							pentathlon.setHelped(gameIndex, true);
							break;
						}
                    }
                }
            }
        }
        if (allFound)
        {
            for (int gameIndex = 0; gameIndex < Pentathlon.NUMBEROFGAMES; gameIndex++)
            {
                if (!pentathlon.isSolved()[gameIndex])
                {
                    message = "Solve " + Pentathlon.titles[gameIndex];
                    allFound = false;
                    break;
                }
            }
        }
		else if (!(newSolve))
		{
			message = "Find more puzzle pieces!";
		}
		if (allFound)
        {
            if (maze[youIdx].isExit())
            {
                maze[youIdx].setEastWall(false);
                message = "Yay! Exit to Level " + (pentathlon.getLevel() + 1) + "!";
                levelSolved = true;
            }
            else
            {
                message = "Go to the exit!";
            }
        }
        repaint();
    }
    public String getMessage()
    {
        return message;
    }
    public void setMessage(String message)
    {
        this.message = message;
        repaint();

    }
    private void processDoor(Maze.Direction dir)
    {
        maze[youIdx].setYou(false);
        youIdx = Maze.Direction.getNewIndex(youIdx, dir);
        youDoor = Maze.Direction.getInverse(dir);
        maze[youIdx].setYou(true);
    }
    public void keyPressed(KeyEvent key)
    {
        message = "";
        switch (key.getKeyCode())
        {
            case 37:
                if (westDoor.isActive())
                {
                    processDoor(Maze.Direction.WEST);
                }
                break;
            case 39:
                if (eastDoor.isActive())
                {
                    if (levelSolved)
                    {
                        pentathlon.newLevel();
                    }
                    else
                    {
                       processDoor(Maze.Direction.EAST);
                    }
                }
                break;
            case 38:
                if (northDoor.isActive())
                {
                    processDoor(Maze.Direction.NORTH);
                }
                break;
            case 40:
                if (southDoor.isActive())
                {
                    processDoor(Maze.Direction.SOUTH);
                }
        }
        checkExit();
        repaint();
    }
    public void mousePressed(MouseEvent e)    {    }
    public void mouseReleased(MouseEvent e)    {    }
    public void mouseEntered(MouseEvent e)  {    }
    public void mouseExited(MouseEvent e) {    }
    public void keyTyped(KeyEvent e)    {    }
    public void keyReleased(KeyEvent e)    {    }

    private class Cell
    {
        private boolean eastWall = true;        // this controls the east wall
        private boolean southWall = true;      // this controls the south wall
        private boolean visited = false;
        private boolean exit = false;
        private boolean you = false;
        private int idx, col, row;
        private int jewelGame = 0;
        private int jewelIndex = 0;

        public Cell(int idx)
        {
            this.idx = idx;
            this.col = (idx % maxCols) + 1;
            this.row = idx / maxCols + 1;
        }
        public void drawCell(Graphics og, Maze.Cell north, Maze.Cell west)
        {
            og.setColor(tmColors.PALEPINK);
       		og.fillPolygon(frontPanel);
            og.fillPolygon(floorPanel);
       		og.fillPolygon(backPanel);
            og.fillPolygon(ceilingPanel);
			// this is the level bar, except for the outer black rectangle boundary
			// there will be 7 possible colors: ROYGBIV -- 4 levels per color
			Color[] spectrum = {tmColors.RED, tmColors.ORANGE, tmColors.YELLOW, tmColors.GREEN, tmColors.BLUE, tmColors.INDIGO, tmColors.VIOLET};
			for (int i = 0; i < pentathlon.getLevel(); i++)
			{
				int idx = i/4;
				og.setColor(spectrum[idx]);
				og.fillRect(xStart + i * 11, 35, 11, 15);
			}
            og.setColor(tmColors.BLACK);
            og.setFont(tmFonts.PLAIN24);
            og.drawString("Level " + pentathlon.getLevel(), xStart, 25);
			og.drawRect(xStart, 35, 28 * 11, 15);
            og.drawString(message, xStart, yMessage);
       		og.drawPolygon(frontPanel);
            og.drawPolygon(floorPanel);
       		og.drawPolygon(backPanel);
            og.drawPolygon(ceilingPanel);
            southDoor.process(this.isSouthWall(), og);
            eastDoor.process(this.isEastWall(), og);
            if (west == null)
            {
                westDoor.process(true, og);
            }
            else
            {
                westDoor.process(west.isEastWall(), og);
            }
            if (north == null)
            {
                northDoor.process(true, og);
            }
            else
            {
                northDoor.process(north.isSouthWall(), og);
            }
            og.setColor(tmColors.YELLOW);
            switch (youDoor)
            {
                case NORTH:
               		og.fillRoundRect(xStart + 9*scale + (scale/2), 10*scale + yPad, scale, scale, 20, 20);
                    break;
                case SOUTH:
               		og.fillRoundRect(xStart + 9*scale + (scale/2), 16*scale + yPad, scale, scale, 20, 20);
                    break;
                case EAST:
               		og.fillRoundRect(xStart + 17*scale, 13*scale + yPad, scale, scale, 20, 20);
                    break;
                case WEST:
               		og.fillRoundRect(xStart + 2*scale, 13*scale + yPad, scale, scale, 20, 20);
                    break;
                default:
                    pentathlon.log.error("ERROR!!! Fallthrough in Maze.drawCell");
            }
            if (jewelGame > 0)
            {
                Font[] jewelFonts = {null, tmFonts.PLAIN24, tmFonts.PLAIN24, tmFonts.PLAIN24, tmFonts.BOLD12};
                og.setColor(Pentathlon.bgColors[jewelGame]);
                og.fillRoundRect(xStart + 9*scale + (scale/2), 11*scale + (scale/2) + yPad, 3*scale, 2*scale, 3, 3);
                og.setColor(tmColors.RED);
                og.setFont(jewelFonts[jewelGame]);
                og.drawString("" + (pentathlon.getPiecesToFind()[jewelGame]).get(jewelIndex), xStart + 9*scale + (scale/2) + 5, 13*scale + yPad);
                pentathlon.setPiecesFound(jewelGame, jewelIndex);
                jewelGame = 0;
            }
        }
        public void draw(Graphics og, boolean walls)
        {
            int x = (col + 1) * size;
            int y = (row + 1) * size;
            og.setColor(tmColors.BLACK);
            if (this.isSouthWall())
            {
                if (walls || row == maxRows || visited)
                {
                    og.drawLine(x, y + size, x + size, y + size);
                }
            }
            if (this.isEastWall())
            {
                if (walls || col == maxCols || visited)
                {
                    og.drawLine(x + size, y, x + size, y + size);
                }
            }
            if (col == 1)
            {
                og.drawLine(x, y, x, y + size);
            }
            if (row == 1)
            {
                og.drawLine(x, y, x + size, y);
            }
            if (visited)
            {
                og.setColor(tmColors.PALECHARTREUSE);
           		og.fillRoundRect(x+1, y+1, size-1, size-1, 0, 0);
            }
            if (you)
            {
                og.setColor(tmColors.RED);
           		og.fillRoundRect(x + 2, y + 2, size - 2, size - 2,20,20);
            }
            og.setColor(tmColors.RED);
            og.setFont(tmFonts.BOLD16);
//            pentathlon.log("Looking for " + jewelGame);
            og.drawString(pentathlon.getJewels()[jewelGame], x + 5, y + size-1);
            if (exit)
            {
                og.drawString("Exit", x + size + 5, y + size - 2);
            }
        }
        public int getJewelGame()
        {
            return jewelGame;
        }

        public void setJewelGame(int jewelGame)
        {
            this.jewelGame = jewelGame;
//            this.label = "" + jewelLabel;
        }
        public int getJewelIndex()
        {
            return jewelIndex;
        }

        public void setJewelIndex(int jewelIndex)
        {
            this.jewelIndex = jewelIndex;
        }
        public boolean isEastWall()
        {
            return eastWall;
        }
        public void setEastWall(boolean eastWall)
        {
            this.eastWall = eastWall;
        }
        public boolean isSouthWall()
        {
            return southWall;
        }
        public void setSouthWall(boolean southWall)
        {
            this.southWall = southWall;
        }
        public boolean isVisited()
        {
            return visited;
        }
        public void setVisited(boolean visited)
        {
            this.visited = visited;
        }
        public int getIdx()
        {
            return idx;
        }
        public void setIdx(int idx)
        {
            this.idx = idx;
        }
        public boolean isExit()
        {
            return exit;
        }
        public void setExit()
        {
            this.exit = true;
        }
        public boolean isYou()
        {
            return you;
        }
        public void setYou(boolean you)
        {
            this.you = you;
            this.visited = true;
        }
    }
    private class Door
    {
        private Polygon door;
        private Color bgColor;
        private boolean active;

        public Door(int[] xs, int[] ys, Color bgColor)
        {
            door = new Polygon(xs, ys, xs.length);
            this.bgColor = bgColor;
            active = false;
        }
        public void process(boolean wall, Graphics og)
        {
            if (wall)
            {
                this.setActive(false);
            }
            else
            {
                this.draw(og);
            }
        }
        public void draw(Graphics og)
        {
            og.setColor(bgColor);
            og.fillPolygon(door);
            og.setColor(tmColors.BLACK);
            og.drawPolygon(door);
            setActive(true);
        }
        public boolean isActive()
        {
            return active;
        }
        public void setActive(boolean active)
        {
            this.active = active;
        }
        public boolean clicked(MouseEvent me)
        {
            if (active)
            {
                if (door.contains(me.getPoint()))
                {
                    return true;
                }
            }
            return false;
        }
    }
    public static enum Direction
    {
        NORTH,
        SOUTH,
        EAST,
        WEST;

        private static int rows = 1;
        private static int cols = 1;

        public static void setMaxes(int rows, int cols)
        {
            Maze.Direction.rows = rows;
            Maze.Direction.cols = cols;
        }
        public static Maze.Direction getInverse(Maze.Direction dir)
        {
            switch (dir)
            {
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case EAST:
                    return WEST;
                case WEST:
                    return EAST;
            }
            return NORTH;
        }
        public static int getNewIndex(int currentIndex, Maze.Direction dir)
        {
            switch (dir)
            {
                case NORTH:
                    return (currentIndex - cols < 0) ? -1 : currentIndex - cols;
                case SOUTH:
                    return (currentIndex + cols) < cols * rows ? currentIndex + cols : -1;
                case EAST:
                    return ((currentIndex + 1) % cols == 0) ? -1 : currentIndex + 1;
                case WEST:
                    return (currentIndex % cols == 0) ? -1 : currentIndex - 1;
            }
            return -1;
        }
    }
}