package seven.g5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.io.*;

import org.apache.log4j.Logger;

import seven.ui.Letter;
import seven.ui.LetterGame;
import seven.ui.Player;
import seven.ui.PlayerBids;
import seven.ui.SecretState;

public class DumbPlayer implements Player {

	
	/*
	 * This player bids randomly.
	 */

	// an array of words to be used for making decisions
	private static final Word[] wordlist;

	// for logging
	private Logger logger = Logger.getLogger(this.getClass());

	// the set of letters that this player currently has
	private ArrayList<Character> currentLetters;
	
	// unique ID
	private int myID;
	
	// for generating random numbers
	private Random random = new Random();
	
	// map number of letters to set of words of that length
	private Map<Integer,Set<String>> wordsBySize;
	
	
	// frequency of words given a letter
	private Map<Character, Double> freqs;
	
	// running average winning bid value by letter
	private Map<Character, Double> averageWinningBids;
	private double averageBid = 0.0;
	private double averageWinningBid = 0.0;
	
	// set of possibilities
	Set<String> possibilites;
	
	/* This code initializes the word list */
	static {
		BufferedReader r;
		String line = null;
		ArrayList<Word> wtmp = new ArrayList<Word>(55000);
		try {
			// you can use textFiles/dictionary.txt if you want the whole list
			r = new BufferedReader(new FileReader("textFiles/super-small-wordlist.txt"));
			while (null != (line = r.readLine())) {
				wtmp.add(new Word(line.trim()));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		wordlist = wtmp.toArray(new Word[wtmp.size()]);
	}

	//chars is the chars in our hand
	// word is what we are analyizing
	public boolean isPossible(String chars, String word)
	{
		//char to how many of char in our hand
		Map<Character, Integer> m = new HashMap<Character, Integer>();
		for(int i = 0; i < chars.length(); i++)
		{
			char curr = chars.charAt(i);
			if(m.containsKey(curr))
				m.put(curr, m.get(curr)+1);
			else
				m.put(curr, 1);
		}
		
		//see if word can use all our letters
		for(char key : m.keySet())
		{
			String temp = word.toString();
			for(int i = m.get(key); i > 0; i--)
			{
				int t = temp.indexOf(key);
				if(t == -1)
					return false;
				else
					temp = temp.substring(t+1);
			}
		}
		return true;
	}
	
    /*
     * This is called once at the beginning of a Game.
     * The id is what the game considers to be your unique identifier
     * The number_of_rounds is the total number of rounds to be played in this game
     * The number_of_players is, well, the number of players.
     */
	public void newGame(int id, int number_of_rounds, int number_of_players) {
		myID = id;
		
		this.wordsBySize = new HashMap<Integer, Set<String>>();
		for(int i = 1; i < 8; i++) {
			wordsBySize.put(i, new HashSet<String>());
		}
		for(int i = 0; i < wordlist.length; i++) {
			int s = wordlist[i].word.length();
			if(s<=7) {
				wordsBySize.get(s).add(wordlist[i].word);
			}
		}	
		
		averageWinningBids = new HashMap<Character, Double>();
		possibilites = new HashSet<String>();
	}


	/*
	 * This method is called at the beginning of a new round.
	 * The secretState contains your current score and the letters that were secretly given to you in this round
	 * The current_round indicates the current round number (0-based)
	 */
	public void newRound(SecretState secretState, int current_round) {

		// be sure to reinitialize the list at the start of the round
		currentLetters = new ArrayList<Character>();
		
		// init probability and count maps
		// letter count
		// letter score
		// 
		
		
		// add any letters from the secret state
		for (Letter l : secretState.getSecretLetters()) {
			//logger.trace("myID = " + myID + " and I'm adding " + l + " from the secret state");
			currentLetters.add(l.getCharacter());
		}
		
		possibilites.addAll(wordsBySize.get(7));
	}
	
	private int calculateBid(ArrayList<Letter> rack, Letter tile) {
		int bid = tile.getValue();
		StringBuffer b = new StringBuffer(tile.toString());
		for (Letter l : rack) {
			b.append(l.toString());
		}
		int premium = calculatePremium(b.toString(), tile);
		if (premium  == Integer.MIN_VALUE) {
			return 0;
		}
		return bid + premium;
	}
	
	private void narrowPossibilities() {
		Set<String> newPoss = new HashSet<String>();
		StringBuffer b = new StringBuffer();
		for (Character c : currentLetters) {
			b.append(c);
		}
		String chars = b.toString();
		for (String word : possibilites) {
			if (isPossible(chars, word)) {
				newPoss.add(word);
			}
		}
		possibilites.clear();
		possibilites.addAll(newPoss);
	}
	
	private int calculatePremium(String chars, Letter tile) {
		Set<String> newPoss = new HashSet<String>();
		for (String word : possibilites) {
			if (isPossible(chars, word)) {
				newPoss.add(word);
			}
		}
		
		if (newPoss.isEmpty()) {
			return Integer.MIN_VALUE;
		}
		
		// get percentage
		double p = newPoss.size() * 100 / possibilites.size();
		logger.debug("percentage=" + p);
		
		int premium = 0;
		if (averageWinningBid > 0) {
			if (averageWinningBids.get(tile.getCharacter()) != null) {
				premium = (int) Math.round(averageWinningBids.get(tile.getCharacter())) - tile.getValue();
			} else {
				// no record for this letter
				premium = (int) Math.round(averageWinningBid) - tile.getValue();
			}
		} else {
			// first bid
			premium = 0;
		}
		
		logger.debug("premium based on bidding=" + premium);
		
		if (p > 30) {
			premium = (int) Math.round(premium + (p / 7));
		} 
		
		logger.debug("premium weighted by frequency=" + premium);
		
		return premium;
	}

	/*
	 * This method is called when there is a new letter available for bidding.
	 * bidLetter = the Letter that is being bid on
	 * playerBidList = the list of all previous bids from all players
	 * playerList = the class names of the different players
	 * secretState = your secret state (which includes the score)
	 */
	public int getBid(Letter bidLetter, ArrayList<PlayerBids> playerBidList, ArrayList<String> playerList, SecretState secretState) {
		//logger.trace("myID=" + myID + " and I'm bidding on " + bidLetter);
		//logger.trace("myID= " + myID + " and my score is " + secretState.getScore());

		int bid = this.calculateBid(secretState.getSecretLetters(), bidLetter);
		
		return bid;
	}

	
	/*
	 * This method is called after a bid. It indicates whether or not the player
	 * won the bid and what letter was being bid on, and also includes all the
	 * other players' bids. 
	 */
    public void bidResult(boolean won, Letter letter, PlayerBids bids) {

    	Double ab = averageWinningBids.get(letter.getCharacter());
    	if (ab == null) {
    		averageWinningBids.put(letter.getCharacter(), new Double(bids.getWinAmmount()));
    	} else {
    		ab = (ab + bids.getWinAmmount()) / 2;
    		averageWinningBids.put(letter.getCharacter(), ab);
    	}
    	if (averageWinningBid == 0.0) {
    		averageWinningBid = (double) bids.getWinAmmount();
    	} else {
    		averageWinningBid = (averageWinningBid + bids.getWinAmmount()) / 2;
    	}
    	
    	if (won) {
    		//logger.trace("My ID is " + myID + " and I won the bid for " + letter);
    		currentLetters.add(letter.getCharacter());
    		this.narrowPossibilities();
    	}
    	else {
    		//logger.trace("My ID is " + myID + " and I lost the bid for " + letter);
    	}
    }

    /*
     * This method is called after all the letters have been purchased in the round.
     * The word that you return will be scored for this round.
     */
	public String getWord() {
		char c[] = new char[currentLetters.size()];
		for (int i = 0; i < c.length; i++) {
			c[i] = currentLetters.get(i);
		}
		String s = new String(c);
		Word ourletters = new Word(s);
		Word bestword = new Word("");
		for (Word w : wordlist) {
			if (ourletters.contains(w)) {
				if (w.score > bestword.score) {
					bestword = w;
				}

			}
		}
		logger.trace("My ID is " + myID + " and my word is " + bestword.word);
		
		return bestword.word;
	}

	/*
	 * This method is called at the end of the round
	 * The ArrayList contains the scores of all the players, ordered by their ID
	 */
	public void updateScores(ArrayList<Integer> scores) {
		
	}




}
