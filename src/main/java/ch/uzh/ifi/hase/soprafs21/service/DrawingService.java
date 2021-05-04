package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.entity.BrushStroke;
import ch.uzh.ifi.hase.soprafs21.entity.Drawing;
import ch.uzh.ifi.hase.soprafs21.entity.Round;
import ch.uzh.ifi.hase.soprafs21.helper.Standard;
import ch.uzh.ifi.hase.soprafs21.repository.BrushStrokeRepository;
import ch.uzh.ifi.hase.soprafs21.repository.DrawingRepository;
import ch.uzh.ifi.hase.soprafs21.repository.RoundRepository;
import org.hibernate.mapping.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class DrawingService {

    private final RoundRepository roundRepository;

    private final DrawingRepository drawingRepository;

    private final BrushStrokeRepository brushStrokeRepository;

    @Autowired
    public DrawingService(@Qualifier("drawingRepository")DrawingRepository drawingRepository, RoundRepository roundRepository, BrushStrokeRepository brushStrokeRepository) {
        this.drawingRepository = drawingRepository;
        this.roundRepository = roundRepository;
        this.brushStrokeRepository = brushStrokeRepository;
    }

    // get all the drawings
    public List<Drawing> getDrawing() {
        return this.drawingRepository.findAll();
    }

    // get a specific drawing
    public Drawing getDrawing(Long drawingId) {
        List<Drawing> drawings = getDrawing();

        Drawing drawingFound = null;
        for (Drawing i : drawings) {
            if (drawingId.equals(i.getId())) {
                drawingFound = i;
            }
        }

        return drawingFound;
    }

    // get all brush strokes
    public List<BrushStroke> getBrushStroke() { return this.brushStrokeRepository.findAll(); }

    // get a specific brush stroke
    public BrushStroke getBrushStroke(Long brushStrokeId) {
        List<BrushStroke> brushStrokes = getBrushStroke();

        BrushStroke brushStrokeFound = null;
        for (BrushStroke i : brushStrokes) {
            if (brushStrokeId.equals(i.getId())) {
                brushStrokeFound = i;
            }
        }

        return brushStrokeFound;
    }

    /** Get the latest brushstrokes of a drawing past a certain time
     * The method is used to update the picture users see in the front end. They send us the time at which they last
     * updated and we send them all the information they have missed so far.
     *
     * @param drawing = the drawing they would like to see
     * @param timeStamp = the time from which onward they need the information
     * @return a sorted list of all the need brush strokes starting from latest to newest
     */
    public List<BrushStroke> getDrawing(Drawing drawing, LocalDateTime timeStamp) {
        int index = 0; // initiate the index

        // setting everything up to iterate over the indexes to find the point after which the needed brush strokes are listed
        DateTimeFormatter formatter = new Standard().getDateTimeFormatter();
        BrushStroke brushStroke = drawing.getBrushStrokes().get(index);
        LocalDateTime value = LocalDateTime.parse(brushStroke.getTimeStamp(),formatter);

        // iterate over the saved brush strokes in drawing
        while(value.isBefore(timeStamp)) {
            index++;
            brushStroke = drawing.getBrushStrokes().get(index);
            value = LocalDateTime.parse(brushStroke.getTimeStamp(),formatter);
        }

        // get a sublist past the critical point
        List<BrushStroke> test = drawing.getBrushStrokes().subList(index, drawing.getBrushStrokes().size());

        // sort the list and then return it
        Collections.sort(test);
        return test;
    }

    /** Add a new brush stroke to an existing drawing while at the same time saving it in the repository
     *
     * @param drawing = the drawing we would like to add brush stroke to
     * @param brushStroke = the brush stroke we are supposed to add
     */
    public void addStroke(Drawing drawing, BrushStroke brushStroke) {
        brushStroke = brushStrokeRepository.save(brushStroke);
        brushStrokeRepository.flush();

        drawing.add(brushStroke);

        drawing = drawingRepository.save(drawing);
        drawingRepository.flush();
    }

    // get all rounds
    public List<Round> getRounds() {
        return this.roundRepository.findAll();
    }

    // get a specific round
    public Round getRound(Long roundId) {
        List<Round> rounds = getRounds();

        Round roundFound = null;
        for (Round i : rounds) {
            if (roundId.equals(i.getId())) {
                roundFound = i;
            }
        }

        return roundFound;
    }




    /*
    public Drawing getDrawing(LocalDateTime timeStamp) {
        int index = 0;
        while(brushStrokes.get(index).getTimeStamp().isBefore(timeStamp)) {
            index++;
        }
        ArrayList<BrushStroke> temp = new ArrayList<BrushStroke>(brushStrokes.subList(index,brushStrokes.size()-1));
        Drawing value = new Drawing();
        value.setBrushStrokes(temp);
        value.setDrawerId(0);
        return value;
    }*/
}
