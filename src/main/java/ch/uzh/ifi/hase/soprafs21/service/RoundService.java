package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.entity.*;
import ch.uzh.ifi.hase.soprafs21.helper.Standard;
import ch.uzh.ifi.hase.soprafs21.repository.DrawingRepository;
import ch.uzh.ifi.hase.soprafs21.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs21.repository.RoundRepository;
import ch.uzh.ifi.hase.soprafs21.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static ch.uzh.ifi.hase.soprafs21.constant.RoundStatus.*;

@Service
@Transactional
public class RoundService {

    private final GameRepository gameRepository;

    private final RoundRepository roundRepository;

    private final DrawingRepository drawingRepository;

    private final UserRepository userRepository;

    @Autowired
    public RoundService(@Qualifier("roundRepository") RoundRepository roundRepository, GameRepository gameRepository, DrawingRepository drawingRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.roundRepository = roundRepository;
        this.drawingRepository = drawingRepository;
        this.userRepository = userRepository;
    }

    // get a specific round quality of life
    public Round getRound(Long roundId) {
        Optional<Round> potRound = roundRepository.findById(roundId);
        Round value = null;

        if (potRound.isEmpty()) { // if not found
            String nonExistingRound = "This round does not exist, has expired or has not been initialized yet.";
            new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(nonExistingRound));
        } else { // if found
            value = potRound.get();
        }

        return value;
    }


    /**
     * Here starts the important part. This are the methods that implement the functionalities needed in the back-end
     * to make the game work.
     */
    // a method to create a new round
    public Round createRound(Game game) {

        // find the game this round belongs to and create an empty object
        Round round = new Round();
        int n = game.getPlayers().size();

        // prepare multiple drawings for this round
        List<Drawing> drawings = new ArrayList<Drawing>();
        Drawing tempDrawing;
        for(int i = 0; i < n; i++) {
            tempDrawing = new Drawing();
            tempDrawing = drawingRepository.save(tempDrawing);
            drawings.add(tempDrawing);
        }
        drawingRepository.flush(); // safe the drawings in the repository
        round.setDrawings(drawings); // safe the list of drawings within the round

        // get all the players (might get updated)
        round.setPlayers(game.getPlayers());

        // generate words for this round
        ArrayList<String> words = new ArrayList<String>();
        Words wordGenerator = new Words();
        int choices = new Standard().getNumberOfChoices();
        for (int i = 0; i < n * choices; i++) {
            words.add(wordGenerator.getRandomWord());
        }
        round.setWords(words);

        // continue with setting the fields in order
        round.setIndex(0);
        round.setStatus(DRAWING);
        round.setHasGuessed( new boolean[n] );
        round.setHasDrawn( new boolean[n] );

        // last but not least safe it and push it to the repositories
        round = roundRepository.save(round);
        roundRepository.flush();
        game.setRoundId(round.getId());
        game.setRoundTracker(game.getRoundTracker() + 1);
        gameRepository.saveAndFlush(game);

        // finally return the created round
        return round;

    }

    // helper method to find a new painter while respecting who has already drawn
    public void setNewPainter(Round round) {
        // get a random player
        Random rand = new Random();

        int n = round.getPlayers().size();
        int i = rand.nextInt(n), sign = -1, distance = 0;

        // if player has already drawn systematically pick an acceptable drawer
        int value = (i + sign * distance + n) % n;
        while(round.getHasDrawn()[value]) {
            if(sign == -1) {
                distance++;
            }
            sign *= -1;
            value = (i + sign * distance + n) % n;
        }

        // set the new drawer
        String newDrawerName = round.getPlayers().get(value);
        round.setDrawerName(newDrawerName);

        // save in the array that he/she can not be picked again
        boolean[] newHasDrawn = round.getHasDrawn();
        newHasDrawn[value] = true;
        round.setHasDrawn(newHasDrawn);
    }

    // change index of the round so the API calls go to a different object in the array
    public void setRoundIndex(Round round, int h) {
        if(h < round.getPlayers().size()) {
            round.setIndex(h);
            roundRepository.saveAndFlush(round);
        }
    }

    // change the current phase of the round
    public void changePhase(Round round) {
        if(round.getStatus().equals(DRAWING)) {
            round.setStatus(SELECTING);
        } else {
            round.setStatus(DRAWING);
        }
        roundRepository.saveAndFlush(round);
    }

    // change the phase to done to show the round is already finished
    public void endRound(Round round) {
        round.setStatus(DONE);
        roundRepository.saveAndFlush(round);
    }

    // (Issue #36) the function that returns the choices a drawer can pick from
    public List<String> getChoices(Round round, String username) {
        if (round.getStatus().equals(DRAWING)) { // check if the phase of the round is correct
            String notCorrectPhase = "This round is currently in drawing. Wait for the phase to end before looking for the word choices.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notCorrectPhase));
        } else if (!username.equals(round.getDrawerName())) { // check if it is the right user asking for those infos
            String notRightUser = "This user is not the current drawer. Please wait until it is your turn";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notRightUser));
        } else { // get the current choices of words
            int index = round.getIndex();
            int choices = new Standard().getNumberOfChoices();
            return round.getWords().subList(choices * index, choices * (index + 1));
        }
    }

    // (Issue #39) the function that allows the drawer to pick the word he/she would like to draw
    public void makeChoice(Round round, String username, int choice) {
        int numberOfChoices = new Standard().getNumberOfChoices();
        if (round.getStatus().equals(DRAWING)) { // check if the phase of the round is correct
            String notCorrectPhase = "This round is currently in drawing. Wait for the phase to end before picking a word from the choices.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notCorrectPhase));
        } else if (!username.equals(round.getDrawerName())) { // check if it is the right user sending this choice
            String notRightUser = "This user is not the current drawer. Please wait until it is your turn";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notRightUser));
        } else if (choice < 0 || choice >= numberOfChoices) {
            String notRightChoice = "The choice you send is out of bounds. Please consider taking one of the options given to you.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notRightChoice));
        } else if (round.getWord() != null) {
            String noLongerYourChoice = "The drawer has already mad a choice and that choice can not be changed.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(noLongerYourChoice));
        } else { // get the current choices of words
            int index = round.getIndex();
            String wordOfChoice = round.getWords().get(index * numberOfChoices + choice);
            round.setWord(wordOfChoice);
            roundRepository.saveAndFlush(round);
        }
    }

    // (Issue #116) reset the word that was chosen
    public void resetChoice(Round round) {
        round.setWord(null);
        roundRepository.saveAndFlush(round);
    }

    // (Issue #116) make a choice for a user that has not send his wishes in time
    public void makeChoiceForUser(Round round) {
        Random rand = new Random();
        int index = round.getIndex();
        int numberOfChoices = new Standard().getNumberOfChoices();
        int choiceId = rand.nextInt(numberOfChoices);
        String choice = round.getWords().get( index * numberOfChoices + choiceId);
        round.setWord(choice);
        roundRepository.saveAndFlush(round);
    }

    // (Issue #45) API-call for requesting the letter-count
    public int getLength(Round round) {
        if (round.getStatus().equals(SELECTING)) { // check if the phase of the round is correct
            String notCorrectPhase = "This round is currently in selecting. Wait for the phase to end before getting information about the word.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notCorrectPhase));
        } else { // get the current length of the word
            return round.getWord().length();
        }
    }

    // (Issue #52 Part I) function to handle when a user has made a guess
    public boolean makeGuess(Message message, Round round) {
        if (round.getStatus().equals(SELECTING)) { // check if the phase of the round is correct
            String notCorrectPhase = "This round is not in a drawing phase right now. Chances are you missed the opportunity to get points.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(notCorrectPhase));
        } else { // continue with the checks
            boolean isPartOfGame = round.getPlayers().contains(message.getWriterName()); // check if the writer is part of the game
            boolean isNotPainter = !message.getWriterName().equals(round.getDrawerName()); // check a person other then the painter has made a guess
            boolean isRightWord = message.getMessage().equals(round.getWord()); // check if the guess is correct
            boolean hasNotGuessedYet = false; // default value

            // more precise evaluation of "hasNotGuessedYet"
            int playerIndex = 0;
            if(isPartOfGame) {
                playerIndex = round.getPlayers().indexOf(message.getWriterName());
                hasNotGuessedYet = !round.getHasGuessed()[playerIndex];
            }

            // summaries, make all the checks together
            boolean value = isNotPainter && isPartOfGame && isRightWord && hasNotGuessedYet;

            // if it is a correct guess, remember that that player has guessed
            if(value) {
                boolean[] temp = round.getHasGuessed();
                temp[playerIndex] = true;
                round.setHasGuessed(temp);
                roundRepository.saveAndFlush(round);
            }

            // either way return the value
            return value;
        }
    }

    // at end of drawing phase reset everybody's has guessed status
    public void resetHasGuessed(Round round) {
        Arrays.fill(round.getHasGuessed(), false);
        roundRepository.saveAndFlush(round);
    }

    // TODO *41 see if function handling is up to the standarts
    // drawer has drawn (automatically distinguishes between first time drawing and subsequent strokes)
    // public void addStroke(long idOfDrawer, BrushStroke brushStroke) {
    /*public void addStroke(String nameOfDrawer, BrushStroke brushStroke) {
        // safety-check if it is coming from the right person
        // if(idOfDrawer == drawerId) {
        if(nameOfDrawer == drawerName) {
            // now check if it is the first time this person is sending
            if(pictures.size() < currentWord) { // if the index is bigger the drawing record then the drawing has not been created yet
                pictures.add(new Drawing());
                stopWatch.reset(); // prepare the timer and then start it
                if(stopWatch.ready()) {
                    stopWatch.begin();
                }
            }
            // if the drawing has been set up we can pass the inputs down to the drawing
            if(pictures.size() == currentWord && !stopWatch.timeIsUp()) { // the current picture is being guest and changed
                pictures.get(currentWord - 1).addStroke(brushStroke);
            }
        }
    }

    // TODO: #43 add function to send drawing information to the guessers
    public Drawing getDrawing(LocalDateTime timeStamp) {
        return pictures.get(currentWord-1).getDrawing(timeStamp);
    }



    // round has a turning point with new roles or it finishes
    /*public void check() {
        // check if the timer indeed has run out and we have to ...
        if (stopWatch.timeIsUp()) {
            if(currentWord == hasDrawn.length + 1) { // ... finish the round and distribute the points
                long[] sumRound = new long[players.size()];
                int i = 0, h = 0;
                while(i < players.size()) {
                    h = 0;
                    while(h < players.size()) {
                        sumRound[h] += tempScore[h][i];
                        h++;
                    }
                }
                this.partOf.updatePoints(sumRound);
            } else { // ... a new painter
                setNewPainter();
                currentWord++;
            }
        }

    }*/


}
