package org.ToMar.pentathlon;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import org.ToMar.Utils.*;


/*
 * Updated March, 2012 for PHP interface using Google authentication
 * Integrated into ToMarPentathlon using NetBeans: August, 2013
 */
public class SDF extends Canvas implements MouseListener
{
	private static final long serialVersionUID = -2609361118653956384L;
	public static final int TOPMARGIN = 35;
	private static final Color colors[][] = {{tmColors.RED, tmColors.OLIVE, tmColors.DARKCYAN},
											 {tmColors.DARKRED, tmColors.DARKGREEN, tmColors.PURPLE},
											 {tmColors.RED, tmColors.GREEN, tmColors.BLUE}};
	private int colorIndex = 0;
	private static final Color selectColor = tmColors.LIGHTGRAY;
	private static final Color normalColor = tmColors.CREAM;
	private final static int TOTALCARDS = 81;
    private static final int margin = 3;
    private static final int cardWidth = 60;
    private static final int cardHeight = 80;
    private static final int maxSlots = 21;
	private ArrayList<Card> layout;
    private ArrayList<String> cardsInMaze;
	private Card[] cards;				// shuffled deck
    private Card[] selectedCards = new Card[3];
    private Polygon[] slots = new Polygon[maxSlots];
	private int numberSelected;
	private int cardPointer;
    private int cardsInLayout;
	private String message = "";
    private tmButton helpButton;
    private tmButton colorButton;
    private Pentathlon pentathlon;
	private int GAMEINDEX;

    public SDF(Pentathlon pentathlon, int idx)
    {
		pentathlon.log.debug("SDF.constructor");
		this.GAMEINDEX = idx;
        this.pentathlon = pentathlon;
        this.addMouseListener(this);
		this.setSize(Pentathlon.widths[GAMEINDEX], Pentathlon.height);
        // layout slots are polygons
        // 3 rows of slots, numbered vertically
        for (int i = 0; i < maxSlots; i++)
        {
            int x = i/3 * (cardWidth + margin) + margin;
            int[] xs = {x, x + cardWidth, x + cardWidth, x};
            int y = i%3 * (cardHeight + margin) + margin + margin + TOPMARGIN;
            int[] ys = {y, y, y + cardHeight, y + cardHeight};
            slots[i] = new Polygon(xs, ys, 4);
        }
		helpButton = pentathlon.getHelpButton(GAMEINDEX);
		helpButton.setX(1);
        colorButton = new tmButton(55, 5, 45, "COLOR");
        colorButton.setHeight(20);
        colorButton.setFontSize(10);
        colorButton.setFgColor(Pentathlon.bgColors[GAMEINDEX]);
        colorButton.setBgColor(tmColors.DARKBLUE);
        colorButton.setXLabel(5);
        colorButton.setYLabel(14);
		cards = new Card[TOTALCARDS];
		layout = new ArrayList<>();
	}
	public void newGame()
	{
		pentathlon.log.debug("SDF.newGame");
        // layout gets seeded with first 6 cards
		cards = createDeck();
		layout = createLayout();
    }
	public String getSaveString()
	{
		pentathlon.log.debug("SDF.getSaveString");
		StringBuilder sb = new StringBuilder("");
		// first is the deck (81 cards times 4 characters each)
		for (int i = 0; i < TOTALCARDS; i++)
		{
			sb.append(cards[i]).toString();
		}
		// next is the cardPointer - 2 characters
		sb.append(Functions.formatNumber(cardPointer - cardsInMaze.size(), 2));
		// next is the layout size - 2 characters
		sb.append(Functions.formatNumber(cardsInLayout, 2));
		// and finally the layout cards
		for (int i = 0; i < cardsInLayout; i++)
		{
			sb.append(layout.get(i).toString());
		}
		return sb.toString();
	}
//1SRT3SPT2ERT1SPT3DPC2DRS2SRT3ERS1ERC2SRC3SGS3EPC1SGC1SPS1DPT2SPC3DRC2EPS1ERS3SPC2ERS1SPC3DGC2EPC2SPS3DGT1SRS1ERT1EPT1EPS2SGC1EGC3SRS1DGS2DGS3SRC1DGC2DPS3SGC1DRT3EGT2DPC2SRS2SGT3EPS3SRT1SRC2EPT2SPT1EGS3DGS1EPC2DRT2EGS1DGT3ERC2DGC1DPC3SPS1EGT1DRC2EGT3DRS3DPS3EGS2DGT3EGC3ERT1SGT2ERC2DPT1SGS3SGT2SGS3EPT3DPT1DPS2EGC3DRT1DRS2DRC05061SRT3SPT2ERT1SPT3DPC2DRS
	public void restore(String s)
	{
		pentathlon.log.debug("SDF.restore: " + s);
		// first, replace all 81 cards in deck with cards from saveString
		for (int i = 0; i < TOTALCARDS; i++)
		{
			cards[i] = new Card(s.substring(4 * i, 4 * (i+1)));
		}
		cardPointer = Integer.parseInt(s.substring(324, 326));
		int cardsInLayout = Integer.parseInt(s.substring(326, 328));
		layout = new ArrayList<>();
		int pointer = 328;
		for (int i = 0; i < cardsInLayout; i++)
		{
			layout.add(new Card(s.substring(pointer, pointer + 4)));
            layout.get(i).setPosition(i);
            layout.get(i).setActive(true);
			pointer += 4;
		}
		reInit();
	}
	public void reInit()
    {
		try
		{
		pentathlon.log.debug("SDF.reInit");
        // layout holds all the cards currently in play
        // layout will always be at least 9 cards unless there are none left in the deck
        // cards will be added to the layout in sets of 3 until there is a set to find
        // new cards will be added to both the layout and cardsToFind
        // in the layout, they will be inactive until found in the maze
        // game is over when there are no more sets, irrespective of number of cards in layout
        cardsInMaze = new ArrayList<>();
        cardsInLayout = layout.size();
        while (cardPointer < TOTALCARDS - 1 && (!(hasSet()) || cardsInMaze.size() < 3))
        {
            for (int i = 0; i < 3; i++)
            {
                cardPointer += 1;
                layout.add(cards[cardPointer]);
                cards[cardPointer].setPosition(layout.size() - 1);
                cardsInMaze.add(cards[cardPointer].toString());
            }
        }
        if (!hasSet())
        {
			pentathlon.log.debug("There are no more sets; setting gameOver.");
            pentathlon.setGameOver();
        }
        else
        {
            numberSelected = 0;
            pentathlon.setPiecesToFind(GAMEINDEX, cardsInMaze);
            if (cardsInMaze.isEmpty())
            {
                pentathlon.setActive(GAMEINDEX, true);
                message = "Solve the puzzle!";
            }
            else
            {
                pentathlon.setActive(GAMEINDEX, false);
                message = "Find " + cardsInMaze.size() + " cards.";
            }
            repaint();
        }
		} catch (Exception e)
		{
			pentathlon.log.error("Pentathlon.SDF.Exception in reInit: " + e);
		}
    }
    public void paint(Graphics og)
    {
        og.setColor(tmColors.BLACK);
        og.setFont(tmFonts.PLAIN24);
        og.drawString(Pentathlon.titles[GAMEINDEX], 102, Pentathlon.yTITLE);
        og.setFont(tmFonts.PLAIN16);
        og.drawString(message, 278, Pentathlon.yTITLE);
        // cards will be visible if they are active (have been found in maze)
        for (int i = 0; i < layout.size(); i++)
        {
			layout.get(i).draw(this.getGraphics());
        }
        helpButton.draw(this.getGraphics());
        colorButton.draw(this.getGraphics());
	}
    public void update(Graphics g)
	{
		pentathlon.setBgColor(g, GAMEINDEX);
		g.fillRect(0, 0, Pentathlon.widths[GAMEINDEX], Pentathlon.height);
		paint(g);
	}
    private boolean hasSet()
    {
		pentathlon.log.debug("SDF.hasSet");
        for (int i = 0; i < layout.size(); i++)
        {
            selectedCards[0] = layout.get(i);
            for (int j = i + 1; j < layout.size(); j++)
            {
                selectedCards[1] = layout.get(j);
                for (int k = j + 1; k < layout.size(); k++)
                {
                    selectedCards[2] = layout.get(k);
                    if (goodSet())
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private boolean goodSet()
    {
//		pentathlon.log.debug("SDF.goodSet");
        if(evaluate("getShading"))
        {
            if (evaluate("getColor"))
            {
                if (evaluate("getNumber"))
                {
                    if (evaluate("getShape"))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private boolean evaluate(String methodName)
    {
//		pentathlon.log.debug("SDF.evaluate");
        try
        {
            int value1 = ((int) selectedCards[0].getClass().getMethod(methodName, (Class<?>[]) null).invoke(selectedCards[0]));
            int value2 = ((int) selectedCards[1].getClass().getMethod(methodName, (Class<?>[]) null).invoke(selectedCards[1]));
            int value3 = ((int) selectedCards[2].getClass().getMethod(methodName, (Class<?>[]) null).invoke(selectedCards[2]));
            if (value1 == value2 & value1 == value3)
            {
                return true;
            }
            if (value1 != value2 & value1 != value3 & value2 != value3)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            System.out.println("SDF.evaluate: ERROR!!: " + e);
        }
        return false;
    }
	private ArrayList<Card> createLayout()
	{
		pentathlon.log.debug("SDF.createLayout");
		ArrayList<Card> al = new ArrayList<>();
		cardPointer = -1;
        for (int i = 0; i < 6; i++)
        {
            cardPointer += 1;
            al.add(cards[cardPointer]);
            cards[cardPointer].setPosition(al.size() - 1);
            cards[cardPointer].setActive(true);
        }
		return al;
	}
	public Card[] createDeck()
	{
		pentathlon.log.debug("SDF.createDeck");
		cardPointer = 0;
		Card[] deck = new Card[TOTALCARDS];
		Card[] shuffledDeck = new Card[TOTALCARDS];
		// create the string of 81 cards to be shuffled
		for (int color = 0; color < 3; color++)
		{
			for (int shape = 0; shape < 3; shape++)
			{
				for (int shading = 0; shading < 3; shading++)
				{
					for (int number = 0; number < 3; number++)
					{
						deck[cardPointer] = new Card();
						deck[cardPointer].setColor(color);
						deck[cardPointer].setShape(shape);
						deck[cardPointer].setShading(shading);
						deck[cardPointer].setNumber(number);
						cardPointer += 1;
					}
				}
			}
		}
		String shuffler = "00010203040506070809101112131415161718192021222324252627282930" +
							"313233343536373839404142434445464748495051525354555657585960" +
							"6162636465666768697071727374757677787980";
		// shuffle the cards and put them in the cards array
		for (int i = 0; i < TOTALCARDS; i++)
		{
			int rnd = Functions.getRnd(TOTALCARDS - i) * 2;
			String cardString = shuffler.substring(rnd, rnd + 2);
			shuffler = shuffler.substring(0, rnd) + shuffler.substring(rnd + 2, shuffler.length());
			shuffledDeck[i] = deck[Integer.parseInt(cardString)];
			shuffledDeck[i].setSelected(false);
		}
		return shuffledDeck;
	}
    public void mouseClicked(MouseEvent e)
    {
		if (pentathlon.isActive()[GAMEINDEX] && !pentathlon.isSolved()[GAMEINDEX])
        {
            for (int i = 0; i < layout.size(); i++)
            {
                if (layout.get(i).clicked(e))
                {
                    if (numberSelected == 3)
                    {
                        int num = 3;
                        for (int s = 0; s < layout.size(); s++)
                        {
                            if (layout.get(s).isSelected())
                            {
                                num -= 1;
                                selectedCards[num] = layout.get(s);
                            }
                        }
                        if (goodSet())
                        {
                            message = "You got it!";
                            repaint();
                            pentathlon.setSolved(GAMEINDEX, true);
                            // need to remove each card in the set from the layout
                            // need to relabel the positions of the cards that are left
                            ArrayList<Card> newLayout = new ArrayList<>();
                            int layoutPointer = 0;
                            for (int j = 0; j < layout.size(); j++)
                            {
                                if (layout.get(j).isSelected() == false)
                                {
                                    layout.get(j).setPosition(layoutPointer++);
                                    newLayout.add(layout.get(j));
                                }
                            }
                            layout = newLayout;
                            numberSelected = 0;
                        }
                        else
                        {
                            message = "Invalid set";
                        }
                    }
                    else
                    {
                        message = "Selected: " + numberSelected;
                    }
                    break;
                }
            }
        }
        if (helpButton.clicked(e.getX(), e.getY()))
        {
            pentathlon.getHelp(GAMEINDEX);
        }
		else if (colorButton.clicked(e.getX(), e.getY()))
		{
			colorIndex += 1;
			if (colorIndex == colors.length)
			{
				colorIndex = 0;
			}
		}
        repaint();
    }
    public void foundPiece(int index)
    {
		pentathlon.log.debug("SDF.foundPiece: " + index);
        int position = cardsInLayout + index;
        layout.get(position).setActive(true);
        repaint();
    }
    public void setMessage(String message)
    {
        this.message = message;
        repaint();
    }
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }

    private class Card
    {
        private int number;
        private int color;
        private int shading;
        private int shape;
        private int position = 0;   // numbered vertically across 3 rows
        private boolean selected;
        private boolean active;

        public Card()
        {
//			pentathlon.log.debug("SDF.Card.constructor");
            selected = false;
            active = false;
        }
		public Card(String s)
		{
//			pentathlon.log.debug("SDF.Card.constructor: " + s);
			this.number = "123".indexOf(s.substring(0,1));
			this.shading = "ESD".indexOf(s.substring(1,2));
			this.color = "RGP".indexOf(s.substring(2,3));
			this.shape = "SCT".indexOf(s.substring(3,4));
            selected = false;
            active = false;
		}
        public String toString()
        {
            String shd = "ESD";
            String col = "RGP";
            String num = "123";
            String shp = "SCT";
            return num.substring(number, number + 1)
                 +  shd.substring(shading, shading + 1)
                 +  col.substring(color, color + 1)
                 +  shp.substring(shape, shape + 1);

/*            return "Shade: " + this.getShading()
                + " Color: " + this.getColor()
                + " Number: " + this.getNumber()
                + " Shape: " + this.getShape(); */
        }
        public boolean clicked(MouseEvent me)
        {
            if (slots[position].contains(me.getPoint()))
            {
                selected = (selected == false) ? true : false;
                if (selected)
                {
                    numberSelected += 1;
                    if (numberSelected > 3)
                    {
                        selected = false;
                        numberSelected = 3;
                    }
                }
                else
                {
                    numberSelected -= 1;
                }
                return true;
            }
            return false;
        }
        public void draw(Graphics g)
        {
            if (this.isActive())
            {
                if (selected)
                {
                    g.setColor(selectColor);
                }
                else
                {
                    g.setColor(normalColor);
                }
                g.fillPolygon(slots[position]);
                g.setColor(Color.black);
                g.drawPolygon(slots[position]);
                g.setColor(colors[colorIndex][color]);
                int x = slots[position].getBounds().x;
                int y = slots[position].getBounds().y;
                int marg = 5;
                int shapeSize = (cardHeight - (4 * marg))/3;
                int dotSize = 5;
                int xDot = x + cardWidth/2 - dotSize/2;
                int xShape = x + cardWidth/3;
                // y will have 5 positions, staggered by half of cardHeight
                // if one, use position 3
                // if two, use positions 2 and 4
                // if three, use 1, 3, and 5
                // shape size will be cardHeight
                int[] yShape = {y + marg, y + marg + shapeSize/2, y + 2 * marg + shapeSize, y + 2 * marg + shapeSize * 3/2, y + 3 * marg + 2 * shapeSize};
                int[] yDot = {y + marg + shapeSize/2 - dotSize/2, y + marg + shapeSize/2 + shapeSize/2 - dotSize/2, y + 2 * marg + shapeSize + shapeSize/2 - dotSize/2, y + 2 * marg + shapeSize * 3/2 + shapeSize/2 - dotSize/2, y + 3 * marg + 2 * shapeSize + shapeSize/2 - dotSize/2};
                int[][] patterns = {{2, 0, 0}, {1, 3, 0}, {0, 2, 4}};
                for (int i = 0; i < number + 1; i++)
                {
                    if (shape == 0)
                    {
                        if (shading == 0)
                        {
                            g.drawRect(xShape, yShape[patterns[number][i]], shapeSize, shapeSize);
                            g.drawRect(xShape - 1, yShape[patterns[number][i]] - 1, shapeSize + 2, shapeSize + 2);
                        }
                        else if (shading == 1)
                        {
                            g.fillRect(xShape, yShape[patterns[number][i]], shapeSize, shapeSize);
                        }
                        else
                        {
                            g.fillRect(xDot, yDot[patterns[number][i]], dotSize, dotSize);
                            g.drawRect(xShape, yShape[patterns[number][i]], shapeSize, shapeSize);
                            g.drawRect(xShape - 1, yShape[patterns[number][i]] - 1, shapeSize + 2, shapeSize + 2);
                        }
                    }
                    else if (shape == 1)
                    {
                        if (shading == 0)
                        {
                            g.drawRoundRect(xShape, yShape[patterns[number][i]], shapeSize, shapeSize, shapeSize, shapeSize);
                            g.drawRoundRect(xShape - 1, yShape[patterns[number][i]] - 1, shapeSize + 2, shapeSize + 2, shapeSize + 2, shapeSize + 2);
                        }
                        else if (shading == 1)
                        {
                            g.fillRoundRect(xShape, yShape[patterns[number][i]], shapeSize, shapeSize, shapeSize, shapeSize);
                        }
                        else
                        {
                            g.fillRect(xDot, yDot[patterns[number][i]], dotSize, dotSize);
                            g.drawRoundRect(xShape, yShape[patterns[number][i]], shapeSize, shapeSize, shapeSize, shapeSize);
                            g.drawRoundRect(xShape - 1, yShape[patterns[number][i]] - 1, shapeSize + 2, shapeSize + 2, shapeSize + 2, shapeSize + 2);
                        }
                    }
                    else
                    {
                        int[] xs = {xShape + shapeSize/2, xShape + shapeSize * 4/3, xShape - shapeSize/3};
                        int[] x2 = {xShape + shapeSize/2, xShape + shapeSize * 4/3 - 1, xShape - shapeSize/3 + 1};
                        int[] ys = {yShape[patterns[number][i]], yShape[patterns[number][i]] + shapeSize, yShape[patterns[number][i]] + shapeSize};
                        int[] y2 = {yShape[patterns[number][i]] + 1, yShape[patterns[number][i]] + shapeSize - 1, yShape[patterns[number][i]] + shapeSize - 1};
                        if (shading == 0)
                        {
                            g.drawPolygon(xs, ys, 3);
                            g.drawPolygon(x2, y2, 3);
                        }
                        else if (shading == 1)
                        {
                            g.fillPolygon(xs, ys, 3);
                        }
                        else
                        {
                            g.fillRect(xDot, yDot[patterns[number][i]] + 2, dotSize, dotSize);
                            g.drawPolygon(xs, ys, 3);
                            g.drawPolygon(x2, y2, 3);
                        }
                    }
                }
            }
        }
        public int getPosition()
        {
            return position;
        }
        public void setPosition(int position)
        {
            this.position = position;
        }
        public void setSelected(boolean selected)
        {
            this.selected = selected;
        }
        public boolean isSelected()
        {
            return this.selected;
        }
        public void setNumber(int number)
        {
            this.number = number;
        }
        public void setColor(int color)
        {
            this.color = color;
        }
        public void setShading(int shading)
        {
            this.shading = shading;
        }
        public void setShape(int shape)
        {
            this.shape = shape;
        }
        public int getNumber()
        {
            return this.number;
        }
        public int getColor()
        {
            return this.color;
        }
        public int getShading()
        {
            return this.shading;
        }
        public int getShape()
        {
            return this.shape;
        }
        public boolean isActive()
        {
            return active;
        }
        public void setActive(boolean active)
        {
            this.active = active;
        }
    }
}
