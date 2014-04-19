package org.ToMar.pentathlon;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.ToMar.Utils.*;

public class U24 extends Canvas implements MouseListener
{
	private static final long serialVersionUID = -3575578768327824256L;
	public static final String[] OPERATORS = {"+","-","*","/","(",")"};
	private Puzzle[] puzzles = new Puzzle[Pentathlon.MAXLEVEL];
    private int margin = 10;
	private int yOper = 40;
	private int yPuzzle = 100;
	private int yText = 180;
	private int yButton = 210;
	private int yMessage = 280;
	private Operator[] operator = new Operator[6];
	private tmButton clearGuessButton;
	private tmButton backSpaceButton;
	private tmButton submitButton;
	private ArrayList<Object> elementList;
	private String guessText = "Game over.";
	private String message = "Message goes here.";
	private Puzzle puzzle;
    private tmButton helpButton;
    private Pentathlon pentathlon;
	private int GAMEINDEX;

	public U24(Pentathlon pentathlon, int idx)
	{
		pentathlon.log.debug("U24.constructor");
		this.GAMEINDEX = idx;
        this.pentathlon = pentathlon;
        this.addMouseListener(this);
		this.setSize(Pentathlon.widths[GAMEINDEX], Pentathlon.height);
        String s;
        ArrayList<String> al = new ArrayList<>();
        try
        {
            InputStream is = this.getClass().getResourceAsStream("u24.txt");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is)))
            {
                while   (true)
                {
                    s = br.readLine();
                    if  (s == null)
                    {
                        break;
                    }
                    al.add(s);
                }
            }
        }
        catch   (Exception e)
        {
            pentathlon.log.error("Exception reading U24 input file: " + e);
        }
        int[] picks = Functions.randomPicks(al.size(), Pentathlon.MAXLEVEL);
		for (int i = 0; i < Pentathlon.MAXLEVEL; i++)
		{
			puzzles[i] = new Puzzle(al.get(picks[i]));
		}
		for (int i = 0; i < OPERATORS.length; i++)
		{
			operator[i] = new Operator(margin + i*45, yOper, 40, 40, OPERATORS[i], tmColors.LIGHTBLUE, tmFonts.PLAIN24);
		}
		// x is 10, y is 25 by default -- adjust x for +, and y for *
		operator[0].setXLabel(14);
		operator[0].setYLabel(27);
		operator[2].setYLabel(32);
		operator[3].setYLabel(27);
		submitButton = new tmButton(margin, yButton, 100, 40, "Submit", tmColors.ORANGE, tmFonts.BOLD16);
		clearGuessButton = new tmButton(margin + 105, yButton, 85, 40, "Clear", tmColors.PALEORCHID, tmFonts.BOLD16);
		backSpaceButton = new tmButton(margin + 195, yButton, 85, 40, "BackUp", tmColors.PALEBLUE, tmFonts.BOLD16);
		helpButton = pentathlon.getHelpButton(GAMEINDEX);
        helpButton.setX(Pentathlon.widths[GAMEINDEX] - 50);
	}
	public void restore(String s)
	{
		pentathlon.log.debug("U24.restore: " + s);
		puzzle = new Puzzle(s);
		reReInit();
	}
	public void reInit()
	{
		pentathlon.log.debug("U24.reInit");
		puzzle = puzzles[pentathlon.getLevel() - 1];
		reReInit();
	}
	private void reReInit()
	{
		try
		{
		guessText = "";
		elementList = new ArrayList<>();
        ArrayList<String> al = new ArrayList<>();
		for (int i = 0; i < Puzzle.NUMBERS; i++)
		{
            al.add("" + puzzle.getArgument(i).getNum());
		}
        pentathlon.setPiecesToFind(GAMEINDEX, al);
        pentathlon.setActive(GAMEINDEX, false);
        message = "Find " + al.size() + " numbers.";
        repaint();
		} catch (Exception e)
		{
			pentathlon.log.error("Pentathlon.U24.Exception in reReInit: " + e);
		}
    }
	public String getSaveString()
	{
		return puzzle.getSaveString();
	}
    public void foundPiece(int index)
    {
		pentathlon.log.debug("U24.foundPiece: " + index);
        puzzle.getArgument(index).setLabel("" + puzzle.getArgument(index).getNum());
        repaint();
    }
    public void setMessage(String message)
    {
        this.message = message;
        repaint();
    }
    public void paint(Graphics g)
    {
		if (!pentathlon.isGameOver())
		{
			for (int i = 0; i < operator.length; i++)
			{
				operator[i].draw(this.getGraphics());
			}
			for (int i = 0; i < Puzzle.NUMBERS; i++)
			{
				puzzle.getArgument(i).draw(this.getGraphics());
			}
			clearGuessButton.draw(this.getGraphics());
			backSpaceButton.draw(this.getGraphics());
			submitButton.draw(this.getGraphics());
		}
        g.setColor(tmColors.BLACK);
        g.setFont(tmFonts.PLAIN24);
        g.drawString(Pentathlon.titles[GAMEINDEX], margin + 5, Pentathlon.yTITLE);
        g.drawString(guessText, margin, yText);
        g.setFont(tmFonts.PLAIN16);
        g.drawString(message, margin, yMessage);
        helpButton.draw(this.getGraphics());
    }
    public void update(Graphics g)
	{
		pentathlon.setBgColor(g, GAMEINDEX);
		g.fillRect(0, 0, Pentathlon.widths[GAMEINDEX], Pentathlon.height);
		paint(g);
	}
    public void mouseClicked(MouseEvent e)
	{
		if (pentathlon.isActive()[GAMEINDEX] && !pentathlon.isSolved()[GAMEINDEX])
		{
			message = "";
			if (submitButton.clicked(e.getX(), e.getY()))
			{
				double value = puzzle.getValue(guessText);
				if (value == 24)
				{
                    message = "You got it!";
                    repaint();
                    pentathlon.setSolved(GAMEINDEX, true);
				}
				else
				{
					if (puzzle.getMessage().equals("OK"))
					{
                        String val = "" + Functions.formatDecimals(value, 3);
						message = "No, that equals " + val + ". Try again!";
					}
					else
					{
						message = puzzle.getMessage();
					}
				}
			}
 			else if (clearGuessButton.clicked(e.getX(), e.getY()))
			{
				while (elementList.size() > 0)
				{
					backSpace();
				}
			}
			else if (backSpaceButton.clicked(e.getX(), e.getY()))
			{
				backSpace();
			}
			else
			{
				boolean found = false;
				for (int i = 0; i < OPERATORS.length; i++)
				{
					if (operator[i].clicked(e.getX(), e.getY()))
					{
						elementList.add(operator[i]);
						guessText += operator[i].getText();
						found = true;
						break;
					}
				}
				if (!found)
				{
					for (int i = 0; i < Puzzle.NUMBERS; i++)
					{
						if (puzzle.getArgument(i).clicked(e.getX(), e.getY()))
						{
							if (puzzle.getArgument(i).isUsed())
							{
								message = "Already used.";
							}
							else
							{
								elementList.add(puzzle.getArgument(i));
								guessText += "" + puzzle.getArgument(i).getNum();
								puzzle.getArgument(i).setUsed(true);
							}
							break;
						}
					}
				}
			}
		}
        if (helpButton.clicked(e.getX(), e.getY()))
        {
            pentathlon.getHelp(GAMEINDEX);
        }
        repaint();
	}
	private void backSpace()
	{
		if (!(elementList.isEmpty()))
		{
			if (elementList.get(elementList.size() - 1).getClass().getName().equals("org.ToMar.pentathlon.U24$Argument"))
			{
				((Argument) elementList.get(elementList.size() - 1)).setUsed(false);
			}
			elementList.remove(elementList.size() - 1);
			StringBuilder sb = new StringBuilder("");
			try
			{
				for (int i = 0; i < elementList.size(); i++)
				{
					sb.append(elementList.get(i).getClass().getMethod("getText").invoke(elementList.get(i)));
				}
			}
			catch (Exception e)
			{
				pentathlon.log.error("ERROR in U24 backSpace: " + e);
			}
			guessText = sb.toString();
			repaint();
		}
	}
    public void mousePressed(MouseEvent e)    {    }
    public void mouseReleased(MouseEvent e)    {    }
    public void mouseEntered(MouseEvent e)    {    }
    public void mouseExited(MouseEvent e)    {    }

    private class Puzzle
    {
        private Argument[] argument;
        private GuessEvaluator guessEvaluator = new GuessEvaluator();
		private String saveString = "";
        public static final int NUMBERS = 4;
		public static final String COMMA = ",";

        public Puzzle(String puzzleString)
        {
			String[] holder = new String[NUMBERS];
			if (puzzleString.indexOf(COMMA) > -1)
			{
				holder = puzzleString.split(COMMA);
			}
			else
			{
				for (int i = 0; i < NUMBERS; i++)
				{
					holder[i] = puzzleString.substring(2 * i, 2 * i + 2);
				}
			}
            argument = new Argument[NUMBERS];
			StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < NUMBERS; i++)
            {
                argument[i] = new Argument(holder[i].trim(), i);
				sb.append(Functions.formatNumber(argument[i].getNum(), 2));
            }
			saveString = sb.toString();
        }
		public void resetUsed()
        {
            for (int i = 0; i < NUMBERS; i++)
            {
                argument[i].setUsed(false);
            }
        }
        public Argument getArgument(int i)
        {
            return argument[i];
        }
		public String getSaveString()
		{
			return saveString;
		}
        public double getValue(String expr)
        {
            double value = guessEvaluator.evaluate(expr, this);
            message = guessEvaluator.getMessage();
            return value;
        }
        public String getMessage()
        {
            return message;
        }
    }
	private class Operator extends tmButton
	{
		private String text;

		public Operator(int x, int y, int w, int h, String l, Color c, Font f)
		{
			super(x, y, w, h, l, c, f);
			this.setXLabel(17);
            this.setYLabel(26);
			this.text = " " + l + " ";
		}
		public String getText()
		{
			return this.text;
		}
	}
    private class Argument extends tmButton
    {
        private int num;
        private boolean used;

        public Argument(String s, int index)
        {
			super(margin + index*70, yPuzzle, 65, 40, "", tmColors.GREEN, tmFonts.PLAIN32);
            this.num = Integer.parseInt(s);
            this.used = false;
			if (num > 9)
			{
				this.setXLabel(13);
			}
			else
			{
				this.setXLabel(21);
			}
            this.setYLabel(31);
        }
		public void draw(Graphics og)
		{
			if (this.isUsed())
			{
//				og.setColor(Pentathlon.bgColors[GAMEINDEX]);
				og.setColor(tmColors.DARKGRAY);
			}
			else
			{
				og.setColor(bgColor);
			}
			og.fillRoundRect(x, y, width, height, 5, 5);
			og.setColor(fgColor);
			og.drawRoundRect(x, y, width, height, 5, 5);
			og.setFont(font);
			og.drawString(label, x + xLabel, y + yLabel);
		}
		public String getText()
		{
			return "" + num;
		}
    	public int getNum()
        {
            return this.num;
        }
    	public void setUsed(boolean used)
        {
            this.used = used;
        }
        public boolean isUsed()
        {
            return this.used;
        }
    }
    private class GuessEvaluator extends tmEvaluator
    {
    	boolean extraNumbers;

        public double evaluate(String str, Puzzle puzzle)
        {
    		puzzle.resetUsed();
        	extraNumbers = false;
            // create an array of Strings containing the elements of the equation
            numberReceived = false;
            args = new ArrayList<>();
            parenCounts[0] = parenCounts[1] = 0;
            multiplier = 1;
            StringBuffer numberInProgress = null;
            boolean operFlag = false;
            setMessage("OK");
            for (int i = 0; i < str.length(); i++)
            {
                String currentChar = str.substring(i, i+1);
    			if (" ".equals(currentChar))
        		{
            		numberInProgress = dumpNumber(numberInProgress, args, puzzle);
                    continue;
                }
                if (PARENS.indexOf(currentChar) > -1)
                {
                    numberInProgress = dumpNumber(numberInProgress, args, puzzle);
                    parenCounts[PARENS.indexOf(currentChar)] += 1;
                    args.add(currentChar);
                }
                else if (OPERS.indexOf(currentChar) > -1)
                {
                    if (operFlag)
                    {
                        if (!("-".equals(currentChar)))
                        {
                            setMessage("Operator not followed by operand");
                            return ERRORFLAG;
                        }
                        else
                        {
                            multiplier *= -1;
                            continue;
                        }
                    }
                    operFlag = true;
                    numberInProgress = dumpNumber(numberInProgress, args, puzzle);
                    if (!numberReceived)
                    {
                        if (!("-".equals(currentChar)))
                        {
                            setMessage("Operator not followed by operand");
                            return ERRORFLAG;
                        }
                        else
                        {
                            multiplier *= -1;
                            continue;
                        }
                    }
                    args.add(currentChar);
                }
                else if (NUMS.indexOf(currentChar) > -1)
                {
                    operFlag = false;
                    if (numberInProgress != null)
                    {
                        numberInProgress.append(currentChar);
                    }
                    else
                    {
                        numberInProgress = new StringBuffer(currentChar);
                    }
                }
                else
                {
                    setMessage("Non-numeric character " + currentChar + ".");
                    return ERRORFLAG;
                }
            }
            numberInProgress = dumpNumber(numberInProgress, args, puzzle);
            if (operFlag)
            {
                setMessage("Operator not followed by operand");
                return ERRORFLAG;
            }
            // check for balanced parens
            if (parenCounts[0] != parenCounts[1])
            {
                setMessage("Out of balance - " + parenCounts[0] +
					" left parens and " + parenCounts[1] +
                    " right parens.");
    			return ERRORFLAG;
        	}
    		// process stuff inside parentheses
        	if(parenCounts[0] > 0)
            {
                for (int lp = args.size() - 1; lp >= 0; lp--) //work back from the end looking for left paren
                {
                    if (!message.equals(OK))
                    {
                        break;
                    }
                    if ((args.get(lp)).equals("("))
                    {
                        for (int rp = lp + 1; rp < args.size(); rp++)
                        {
                            if ((args.get(rp)).equals(")"))
                            {
                                args = processElements(lp, rp);
                                break;
                            }
                        }
                    }
                }
            }
            // process what's left - result should be the only element left
            while(args.size() > 1 && message.equals(OK))
            {
                args = processElements(0, args.size() - 1);
    		}
        	for (int i = 0; i < Puzzle.NUMBERS; i++)
            {
                if (puzzle.getArgument(i).isUsed() == false)
                {
                    setMessage("Failed to use the number " + puzzle.getArgument(i).getNum() + ".");
                    return ERRORFLAG;
                }
            }
            if (extraNumbers)
            {
                setMessage("Extra numbers used that are not in the puzzle.");
                return ERRORFLAG;
            }
            if (!message.equals(OK))
            {
                return ERRORFLAG;
            }
            return makeDouble(args.get(0)) * multiplier;
        }
        protected StringBuffer dumpNumber(StringBuffer numberInProgress, ArrayList<String> args, Puzzle puzzle)
        {
            if (numberInProgress != null)
            {
//			log("dumping " + numberInProgress);
                args.add(numberInProgress.toString());
                int arg = (new Integer(numberInProgress.toString()).intValue());
                numberInProgress = null;
                numberReceived = true;
                if (puzzle != null)
                {
                    boolean found = false;
                    for (int j = 0; j < Puzzle.NUMBERS; j++)
                    {
                        if (puzzle.getArgument(j).isUsed() == false && puzzle.getArgument(j).getNum() == arg)
                        {
                            puzzle.getArgument(j).setUsed(true);
                            found = true;
                            break;
                        }
                    }
                    if (found == false)
                    {
                        extraNumbers = true;
                    }
                }
            }
            return numberInProgress;
        }
    }
}
