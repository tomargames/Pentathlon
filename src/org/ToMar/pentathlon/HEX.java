package org.ToMar.pentathlon;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import org.ToMar.Utils.Functions;
import org.ToMar.Utils.tmButton;
import org.ToMar.Utils.tmColors;
import org.ToMar.Utils.tmFonts;


/**
 * Integrated into ToMarPentathlon using NetBeans: August, 2013
 */
public class HEX extends Canvas implements MouseListener
{
	private static final long serialVersionUID = -2609361116532956384L;
	public static final int TOPMARGIN = 30;
    private static final String stringValues = "ABCDEFGHIJKLMNOPQRSTUVWXYZ*";
	private static final int READY = 0;		// waiting to be active
	private static final int VIEWING = 1;	// active, "ready to play" button
	private static final int GUESSING = 2;	// no button, empty puzzle
    private static final int size = 45;
    private static final int margin = 10;
    private Polygon[] slots;
    private Card[] puzzle;
    private Color[] colors = {tmColors.DARKMAGENTA, tmColors.RED, tmColors.BLACK};
    private int stage;
    private int clickedIndex;
    private int foundPieces;
	String message;
	tmButton controlButton;
    private tmButton helpButton;
    Pentathlon pentathlon;
	private int GAMEINDEX;

    public HEX(Pentathlon pentathlon, int idx)
    {
		pentathlon.log.debug("HEX.constructor");
		this.GAMEINDEX = idx;
        this.pentathlon = pentathlon;
        this.addMouseListener(this);
		this.setSize(Pentathlon.widths[GAMEINDEX], Pentathlon.height);
        slots = new Polygon[Pentathlon.MAXLEVEL];
        // layout slots are polygons
        // 4 rows of slots, numbered vertically
        for (int i = 0; i < Pentathlon.MAXLEVEL; i++)
        {
            int x = i/4 * (size + margin) + margin;
            int[] xs = {x, x + size, x + size, x};
            int y = i%4 * (size + margin) + margin + TOPMARGIN;
            int[] ys = {y, y, y + size, y + size};
            slots[i] = new Polygon(xs, ys, 4);
        }
        controlButton = new tmButton(300, 265, 80, "READY");
        controlButton.setHeight(30);
        controlButton.setFontSize(16);
        controlButton.setFgColor(tmColors.BLACK);
        controlButton.setBgColor(tmColors.ORANGE);
        controlButton.setXLabel(15);
        controlButton.setYLabel(20);
		helpButton = pentathlon.getHelpButton(GAMEINDEX);
		message = "Welcome!";
	}
    public void reInit()
    {
		try
		{
		pentathlon.log.debug("HEX.reInit");
        //each level's puzzle will be the size of the level
        //slots from the 27 will be chosen randomly
        //letters will be used alphabetically with no skips
        puzzle = new Card[pentathlon.getLevel()];
        ArrayList<String> al = new ArrayList<>();
        int[] picks = Functions.randomPicks(Pentathlon.MAXLEVEL, pentathlon.getLevel());
        for (int i = 0; i < pentathlon.getLevel(); i++)
        {
            puzzle[i] = new Card(i, picks[i]);
            al.add(stringValues.substring(i, i + 1));
        }
        pentathlon.setPiecesToFind(GAMEINDEX, al);
        pentathlon.setActive(GAMEINDEX, false);
        message = "Find " + al.size() + " letters.";
        repaint();
        stage = READY;
        clickedIndex = -1;
        foundPieces = 0;
		} catch (Exception e)
		{
			pentathlon.log.error("Pentathlon.HEX.Exception in reInit: " + e);
		}
    }
    public void paint(Graphics og)
    {
        og.setColor(tmColors.BLACK);
        og.setFont(tmFonts.PLAIN24);
        og.drawString(Pentathlon.titles[GAMEINDEX], 55, Pentathlon.yTITLE);
        og.setFont(tmFonts.PLAIN16);
        og.drawString(message, 5, 280);
        og.setFont(tmFonts.PLAIN24);
        og.setColor(tmColors.DARKBLUE);
		if (!pentathlon.isGameOver())
		{
			for (int i = 0; i < puzzle.length; i++)
			{
				puzzle[i].draw(og);
			}
			if (stage == VIEWING)
			{
				controlButton.draw(og);
			}
		}
        helpButton.draw(this.getGraphics());
    }
    public void foundPiece(int index)
    {
		pentathlon.log.debug("HEX.foundPiece: " + index);
        puzzle[index].setStatus(Card.SHOWING);
        if (++foundPieces == puzzle.length)
        {
            stage = VIEWING;
        }
        repaint();
    }
    public void setMessage(String message)
    {
        this.message = message;
        repaint();
    }
    public void mouseClicked(MouseEvent e)
    {
 		if (pentathlon.isActive()[GAMEINDEX] && !pentathlon.isSolved()[GAMEINDEX])
        {
            message = "";
            if (controlButton.clicked(e.getX(), e.getY()) && stage == VIEWING)
            {
                for (int i = 0; i < puzzle.length; i++)
                {
                    puzzle[i].setStatus(Card.HIDING);
                }
           		stage = GUESSING;
                message = "Click on the boxes in alphabetical order.";
            }
            else if (stage == GUESSING)
            {
                for (int i = 0; i < puzzle.length; i++)
                {
    //                pentathlon.log("Checking tile " + i);
                    if (puzzle[i].clicked(e))
                    {
						if (puzzle[i].getStatus() == Card.SHOWING)
						{
							message = "Click on an empty box.";
						}
 //                       pentathlon.log("Tile " + i + " clicked, index is " + puzzle[i].getIndex());
						else if (++clickedIndex == puzzle[i].getIndex())
                        {
                            puzzle[i].setStatus(Card.SHOWING);
                            message = "So far, so good...";
//                            pentathlon.log("Card " + i + " set to showing");
                            if (clickedIndex == puzzle.length - 1)    // puzzle is solved
                            {
                                message = "You got it!";
                                repaint();
                                pentathlon.setSolved(GAMEINDEX, true);
                            }
                        }
                        else if (puzzle[i].getStatus() == Card.HIDING)
                        {
                            puzzle[i].setStatus(Card.ERROR);
  //                          pentathlon.log("Card " + i + " set to error");
                            setMessage("Click to try again...");
                            stage = READY;
                            clickedIndex = -1;
                        }
                        break;
                    }
                }
            }
            else if (stage == READY)
            {
                for (int i = 0; i < puzzle.length; i++)
                {
                    puzzle[i].setStatus(Card.SHOWING);
//                    pentathlon.log("Card " + i + " set to show");
                }
                stage = VIEWING;
                message = "Click READY to try again.";
            }
        }
        if (helpButton.clicked(e.getX(), e.getY()))
        {
            pentathlon.getHelp(GAMEINDEX);
        }
        repaint();
    }
    public void update(Graphics g)
	{
		pentathlon.setBgColor(g, GAMEINDEX);
		g.fillRect(0, 0, Pentathlon.widths[GAMEINDEX], Pentathlon.height);
		paint(g);
	}
    public void mouseReleased(MouseEvent e) {   }
    public void mouseEntered(MouseEvent e) {    }
    public void mouseExited(MouseEvent e) {    }
    public void mousePressed(MouseEvent e) {    }

    private class Card
    {
        public static final int NOTFOUND = 0;       // piece has not been found in maze yet
        public static final int SHOWING = 2;        // found, showing outline and characater
        public static final int HIDING = 1;         // showing outline only
        public static final int ERROR = 3;          // showing outline, character in red
        private int slot;
        private int status;
        private int index;

        public Card(int index, int slot)
        {
			pentathlon.log.debug("HEX.Card.constructor");
            this.index = index;
            this.slot = slot;
        }
        public boolean clicked(MouseEvent me)
        {
            if (slots[slot].contains(me.getPoint()))
            {
                return true;
            }
            return false;
        }
        public void draw(Graphics og)
        {
            if (status > NOTFOUND)
            {
                og.setColor(tmColors.PALECYAN);
                og.fillPolygon(slots[slot]);
                og.setColor(tmColors.BLACK);
                og.drawPolygon(slots[slot]);
                if (status > HIDING)
                {
                    if (status == ERROR)
                    {
                        og.setColor(tmColors.RED);
                    }
                    og.setFont(tmFonts.PLAIN32);
                    int x = slots[slot].getBounds().x + size/4;
                    int y = slots[slot].getBounds().y + size*3/4 + 1;
                    String label = stringValues.substring(index, index + 1);
                    x += getXFactor(label);
                    og.drawString(label, x, y);
                }
            }
        }
		private int getXFactor(String l)
		{
			if ("W".indexOf(l) > -1)
			{
				return -3;
			}
			else if ("M".indexOf(l) > -1)
			{
				return -2;
			}
			else if ("I".indexOf(l) > -1)
			{
				return 6;
			}
			else if ("J?*".indexOf(l) > -1)
			{
				return 5;
			}
			else if ("AELYTP".indexOf(l) > -1)
			{
				return 2;
			}
			else if ("R".indexOf(l) > -1)
			{
				return 1;
			}
			return 0;
		}
        public int getStatus()
        {
            return status;
        }
        public void setStatus(int status)
        {
            this.status = status;
        }
        public int getSlot()
        {
            return slot;
        }
        public void setSlot(int slot)
        {
            this.slot = slot;
        }
        public int getIndex()
        {
            return index;
        }
        public void setIndex(int index)
        {
            this.index = index;
        }
    }
}
