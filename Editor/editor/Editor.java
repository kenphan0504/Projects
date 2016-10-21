package editor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Editor extends Application {

    Group root;
    private String inputFileName;
    private Group textRoot = new Group();

    private TextBuffer textBuffer;
    private ArrayList<TextNode> linePointer; //points to the start of each line
    private BoundedStack<String> undoStack;
    private BoundedStack<String> redoStack;

    private int windowWidth = 500;
    private int windowHeight = 500;
    private int screenWidth;

    private final int MARGIN = 5;
    private final int TOP_BOTTOM_MARGIN = 0;
    private final int FONT_SIZE = 12;
    private int fontSize = FONT_SIZE;
    private final int STARTING_LINE = 0;
    private int yPosOfLastLine;

    private final int CURSOR_WIDTH = 1;
    private final int BLINK_DURATION = 1;
    private int cursorHeight = (int) Math.round(
                            getSampleLetter(FONT_SIZE).getLayoutBounds().getHeight());

    private final String ENTER_KEY = "\r";
    private final String NEW_LINE = "\n";
    private final int NEW_LINE_IN_ASCII = 10;
    private final int ESC_IN_ASCII = 27;   
    private final int BS_IN_ASCII = 8;

    private String fontName = "Verdana";

    private Rectangle cursor;
    private ScrollBar scrollBar;

    //starting x-position of each added letter before rendering
    private int xPos = MARGIN; 
    //starting y-position of each added letter before rendering
    private int yPos = TOP_BOTTOM_MARGIN;
    //point to the latest node so know where the cursor will be
    private TextNode cursorPointer;

    public Editor() {
        textBuffer = new TextBuffer();
        undoStack = new BoundedStack<String>(100);
        cursor = new Rectangle(MARGIN, TOP_BOTTOM_MARGIN, CURSOR_WIDTH, cursorHeight);
        yPosOfLastLine = 0;
    }

    /*
    - Every clicking action by the user gets handled here. 
    If user click in between letters, should direct the cursor
    to the space between two letter by getting the x and y position
    of the mouse click and round it to the closest two letters 
    location
    - Might need to create new TextBuffer to store the new texts that
    gets clicked by the user after the cursor is moved to a different 
    location of the text */
    private class MouseEventHandler implements EventHandler<MouseEvent> {

        /*In order to move cursor at constant time when clicked,
        first checks which line is the y position of the mouse
        clicked is closest to, and then search from that line by
        getting the pointer to the beginning of that line through
        ArrayList linePointer. Then go through each letter's 
        x position and see which is closest to x position of the 
        mouse clicked*/
        @Override 
        public void handle(MouseEvent mouseEvent) { 
            if (textBuffer.isEmpty()) {
                return;
            }

            int mousePressedX = (int) mouseEvent.getX();
            int mousePressedY = (int) (mouseEvent.getY() - Math.round(textRoot.getLayoutY()));

            if (mousePressedY <= 0) {
                cursor.setX(MARGIN);
                cursor.setY(TOP_BOTTOM_MARGIN);
                cursorPointer = textBuffer.getFirst();
            } 
            Text sampleText = getSampleLetter(fontSize);
            int sampleHeight = (int) Math.round(sampleText.getLayoutBounds().getHeight());

            int lineToSearch = (mousePressedY / sampleHeight);   

            /*move cursor to end if mouse click is below 
            the end of the file*/
            if (lineToSearch > linePointer.size() 
                || ((mousePressedY >= textBuffer.getLast().getYPosition())
                    && mousePressedX > textBuffer.getLast().getXPosition())) {
                Text lastLetter = textBuffer.getLast().returnText();
                int lastLetterWidth = (int) Math.round(lastLetter.getLayoutBounds().getWidth());
                cursor.setX(textBuffer.getLast().getXPosition() + lastLetterWidth);
                cursor.setY(textBuffer.getLast().getYPosition());
                cursorPointer = textBuffer.getLast();
                return;
            }

            TextNode pointerToLineSearch = linePointer.get(lineToSearch);

            /*figure out where the mouse click is closest to*/
            while (pointerToLineSearch.getYPosition() <= mousePressedY
                   && pointerToLineSearch.returnText() != null) {
                Text curLetter = pointerToLineSearch.returnText();
                int curLetterXPosition = pointerToLineSearch.getXPosition();
                int curLetterYPosition = pointerToLineSearch.getYPosition();
                int curLetterWidth = (int) Math.round(curLetter.getLayoutBounds().getWidth());
                
                /*check if user clicks at the empty space of a nonfilled line
                then move cursor to the last letter*/
                if ((pointerToLineSearch.nextNode.getYPosition() > mousePressedY)
                    && curLetterXPosition < mousePressedX) {
                    cursor.setX(pointerToLineSearch.getXPosition() + curLetterWidth);
                    cursor.setY(pointerToLineSearch.getYPosition());
                    cursorPointer = pointerToLineSearch;
                } 

                if (Math.abs(mousePressedX - curLetterXPosition) <= curLetterWidth) {
                    int closeToLeft = Math.abs(mousePressedX - curLetterXPosition);
                    int closeToRight = Math.abs(mousePressedX - curLetterXPosition - curLetterWidth);
                    if (closeToLeft < closeToRight) {
                        cursor.setX(curLetterXPosition);
                        cursorPointer = pointerToLineSearch.prevNode;
                    } else {
                        cursor.setX(curLetterXPosition + curLetterWidth);
                        cursorPointer = pointerToLineSearch;
                    }
                    cursor.setY(pointerToLineSearch.getYPosition());
                    break;
                }
                pointerToLineSearch = pointerToLineSearch.nextNode;
            }
        }
    }

    /*Takes in every letter that the user types, stores them into
    TextBuffer, deals with shortcut keys, and call render() to 
    display text appropriately*/
    private class KeyEventHandler implements EventHandler<KeyEvent> {

        private int curXPos = MARGIN;
        private int curYPos = 0;

        /*Take in each key pressed, get all the information needed
        like height, width, x, and y positions, and stored each character
        into textBuffer. Then call render() to display onto the screen
        -Undo and Redo doesn't work completely, can only undo and redo
        after users insert characters*/
        @Override 
        public void handle(KeyEvent keyEvent) {
            KeyCode code = keyEvent.getCode();
            if (keyEvent.isShortcutDown()) {
                if (code == KeyCode.Z && undoStack.size() > 0) {
                    String textToDoAction = cursorPointer.returnText().getText();
                    String action = undoStack.pop();
                    if (action.equals("delete")) {
                        redoStack.push("");
                    } else {
                        redoStack.push(textToDoAction);
                    }
                    undo(action, textToDoAction);
                } else if (code == KeyCode.Y && redoStack.size() > 0) {
                    String text = redoStack.pop();
                    redo(text);
                } else if (code == KeyCode.P) {
                    int x = (int) (cursor.getX());
                    int y = (int) (cursor.getY() + Math.round(textRoot.getLayoutY()));
                    System.out.println(x + ", " + y);
                } else if (code == KeyCode.EQUALS || code == KeyCode.PLUS) {
                    fontSize += 4;
                    if (!textBuffer.isEmpty()) {
                        render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                    } else {
                        cursor.setHeight(fontSize);
                    }
                } else if (code == KeyCode.MINUS && fontSize > 4) {
                    fontSize -= 4;
                    if (!textBuffer.isEmpty()) {
                        render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                    } else {
                        cursor.setHeight(fontSize);
                    }
                } else if (code == KeyCode.S) {
                    save(inputFileName);
                }
            } else if ((keyEvent.getEventType() == KeyEvent.KEY_TYPED) 
                        && (keyEvent.getCharacter().charAt(0) != ESC_IN_ASCII)
                        && (keyEvent.getCharacter().charAt(0) != BS_IN_ASCII)) {
                String charTyped = keyEvent.getCharacter();
                if (charTyped.length() > 0) {
                    /*If cursor is not at the end of the file*/
                    if (cursorPointer != null 
                        && cursorPointer.nextNode.returnText() != null) {
                        if (charTyped.equals(ENTER_KEY)) {
                            fillByCursorLocation(NEW_LINE);
                        } else {
                            fillByCursorLocation(charTyped);
                        }
                    } else {
                        if (charTyped.equals(ENTER_KEY)) {
                            fillTextBuffer(NEW_LINE);
                        } else {
                            fillTextBuffer(charTyped);
                        }
                    }
                }
                render(textBuffer, fontName, fontSize, windowWidth, windowHeight);

                /*Handle moving the cursor correctly to the beginning of 
                next line when the user presses ENTER in the middle of a 
                line or middle of a word*/
                if (cursorPointer.returnText().getText().equals(NEW_LINE)) {
                    Text nextText = cursorPointer.nextNode.returnText();
                    if (nextText != null) {
                        cursor.setX(MARGIN);
                        cursor.setY((int) (Math.round(nextText.getY())));
                    }
                }
                adjustScrollBar();
                keyEvent.consume();
            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                if (!textBuffer.isEmpty()) {
                    Text cursorPointsTo = cursorPointer.returnText();
                    if (code == KeyCode.LEFT) { 
                        if (cursorPointer.returnText() != null) {
                            int textWidth = ((int) Math.round(
                                        cursorPointsTo.getLayoutBounds().getWidth()));
                            /*if cursor is appears after the first letter 
                            in the file*/
                            if (cursorPointer.getXPosition() == MARGIN) {
                                cursor.setX(((int) cursor.getX()) - textWidth);
                                cursorPointer = cursorPointer.prevNode;
                            } else {
                                cursorPointer = cursorPointer.prevNode;
                                render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                            }
                        }
                    } else if (code == KeyCode.RIGHT) {
                        if (cursorPointer.nextNode.returnText() != null) {
                            cursorPointer = cursorPointer.nextNode;
                        }
                        render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                        /*Move cursor down to beginning of next line if next
                        node is a newline character*/
                        if (cursorPointer.returnText().getText().equals(NEW_LINE)) {
                            cursor.setX(cursorPointer.nextNode.getXPosition());
                            cursor.setY(cursorPointer.nextNode.getYPosition());
                        }
                    } else if (code == KeyCode.UP) {
                        if (cursorPointer.getYPosition() != TOP_BOTTOM_MARGIN) {
                            searchLine(code, windowWidth, windowHeight);
                        }
                    } else if (code == KeyCode.DOWN) {
                        if (cursorPointer.getYPosition() != yPosOfLastLine) {
                            searchLine(code, windowWidth, windowHeight);
                        }
                    } else if (code == KeyCode.BACK_SPACE && cursorPointer.returnText() != null) {
                        delete();
                    }
                }
                adjustScrollBar();
            }           
        }
    }

    /*Undo a command; e.g: if action was inserting
    text then undoing it would be to delete*/
    public void undo(String action, String text) {
        if (action.equals("insert")) {
            delete();
        } else if (action.equals("delete")) {
            if (cursorPointer.nextNode.returnText() == null) {
                fillTextBuffer(text);
            } else {
                fillByCursorLocation(text);
            }
            render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
        }
    }

    /*Redo a command; e.g: if action was inserting
    text then redoing it would be insert
    -if argument is a valid text then insert
    -else delete*/
    public void redo(String text) {
        if (!text.equals("")) {
            if (cursorPointer.returnText() == null) {
                fillTextBuffer(text);
            } else {
                fillByCursorLocation(text);
            }
            render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
        } else {
            delete();
        }
    }

    /*Called when user presses backspace*/
    public void delete() {
        Text textToRemove;
        textToRemove = cursorPointer.returnText();

        TextNode temp = cursorPointer.nextNode;
        TextNode nodeToRemove = cursorPointer;
        cursorPointer.prevNode.nextNode = temp;
        temp.prevNode = cursorPointer.prevNode;
        cursorPointer = cursorPointer.prevNode;

        nodeToRemove.prevNode = null;
        nodeToRemove.nextNode = null;

        textBuffer.size -= 1;
        textRoot.getChildren().remove(textToRemove);
        render(textBuffer, fontName, fontSize, windowWidth, windowHeight);

        /*After deleting the first character in the file, 
        set the cursor to point back to the starting point*/
        if (cursorPointer.returnText() == null) {
            cursor.setX(MARGIN);
        }
    }

    /*Search either above or below current line based
    on the direction of arrow key pressed (up/down) and
    move cursor to the closest horizonal position*/ 
    public void searchLine(KeyCode direction, int windW, int windH) {
        int moveLine;
        if (direction == KeyCode.UP) {
            moveLine = -1;
        } else {
            moveLine = 1;
        }

        int xPosition = (int) Math.round(cursor.getX());
        int yPosition = (int) Math.round(cursor.getY());

        Text t = cursorPointer.returnText();
        int letterWidth = (int) Math.round(t.getLayoutBounds().getWidth());

        Text sampleText = getSampleLetter(fontSize);
        int sampleHeight = (int) Math.round(sampleText.getLayoutBounds().getHeight());

        int lineToSearch = (yPosition / sampleHeight) + moveLine;

        if (lineToSearch >= linePointer.size()) {
            return;
        }

        TextNode positionToMeasure = linePointer.get(lineToSearch);
        int yPositionOfLineCheck = positionToMeasure.getYPosition();
        while (positionToMeasure.getYPosition() != yPosition) {
            /*In case the line above is shorter then move cursor to last
            character of line above*/
            /*To check if ever exceed the line we are checking*/
            TextNode nextPosition = positionToMeasure.nextNode;
            int yPositionOfNextLetter = nextPosition.getYPosition();
            if (((moveLine < 0) && yPositionOfNextLetter == yPosition)
                || ((moveLine > 0) && yPositionOfNextLetter > yPositionOfLineCheck)
                || nextPosition.returnText() == null) { 
                cursorPointer = positionToMeasure;
                render(textBuffer, fontName, fontSize, windW, windH);
                break;
            } 
            int curXPosition =  (int) Math.round(positionToMeasure.returnText().getX());
            if (Math.abs(curXPosition - xPosition) <= letterWidth) {
                Text curLetter = positionToMeasure.returnText();
                int curLetterWidth = (int) Math.round(curLetter.getLayoutBounds().getWidth());
                int left = Math.abs(curXPosition - xPosition);
                int right = Math.abs(curXPosition + curLetterWidth - xPosition);
                if (left < right) {
                    cursor.setX(curXPosition);
                    cursorPointer = positionToMeasure.prevNode;
                } else {
                    cursor.setX(curXPosition + curLetterWidth);
                    cursorPointer = positionToMeasure;
                }
                cursor.setY(positionToMeasure.getYPosition());
                break;
            }
            positionToMeasure = positionToMeasure.nextNode;
        }
    }

    /* Creates the cursor with colors; handle() changes the 
    color ofthe cursor; */
    private class BlinkEventHandler implements EventHandler<ActionEvent> {
        private int colorIndex = 0;
        private Color[] blinkColors = {Color.BLACK, Color.WHITE};

        BlinkEventHandler() {
            changeColor();
        }

        private void changeColor() {
            cursor.setFill(blinkColors[colorIndex]);
            colorIndex = (colorIndex + 1) % blinkColors.length;
        }

        @Override
        public void handle(ActionEvent ae) {
            changeColor();
        }
    }

    /*Main method for filling up TextBuffer; called whenever
    the user type or when reading from a file*/
    public void fillTextBuffer(String text) {
        Text curChar = new Text(text);
        curChar.setFont(Font.font(fontName, fontSize));
        cursorPointer = textBuffer.addLast(curChar, xPos, yPos);
        textRoot.getChildren().add(curChar);
        undoStack.push("insert");
        redoStack = new BoundedStack<String>(100); //new redo stack everytime user types
    }

    /*Secondary method for filling up TextBuffer; called whenever
    the cursor is not at the end but somewhere in the middle.
    Always add new text before the cursor*/
    public void fillByCursorLocation(String text) {
        Text curChar = new Text(text);
        curChar.setFont(Font.font(fontName, fontSize));
        TextNode newText = new TextNode(curChar, null, null, xPos, yPos);
        TextNode temp = cursorPointer.nextNode;
        cursorPointer.nextNode = newText;
        newText.prevNode = cursorPointer;
        newText.nextNode = temp;
        temp.prevNode = newText;
        cursorPointer = newText; 
        textBuffer.size += 1;
        textRoot.getChildren().add(curChar);
        undoStack.push("insert");
        redoStack = new BoundedStack<String>(100); //new redo stack everytime user types
    }

    /*Render the each character within TextBuffer according to its
    x and y position, and font sizeso it will display correctly, 
    update x and y position of each Text object/character by moving 
    a temporary pointer across the whole TextBuffer*/
    public void render(TextBuffer t, String fontName, int fontSize,
                        int windW, int windH) {
        /*Only to get the height of the letter
        to check if a character is a new-line*/
        Text s = getSampleLetter(fontSize);
        int letterHeight = ((int) Math.round(s.getLayoutBounds().getHeight()));

        int curXPos = MARGIN;
        int curYPos = 0;

        int numLettersInWord = 0;
        boolean mightWrap = false;

        int curWordWidth = 0;

        /*returns a pointer to the first Text object of 
        TextBuffer, used for iteration in constant time*/
        TextNode curNode = t.getFirst(); 
        TextNode wordPointer = curNode;

        /*list of pointer changes after every render because
        the position of the characters will shift*/
        linePointer = new ArrayList<TextNode>();  

        for (int i = 0; i < t.size(); i++) {
            Text curText = curNode.returnText();
            curText.setTextOrigin(VPos.TOP);
            curText.setFont(Font.font(fontName, fontSize));

            int curTextWidth = ((int) Math.round(curText.getLayoutBounds().getWidth()));
            boolean isNewLine = ((int) Math.round(curText.getLayoutBounds().getHeight()) 
                                   > letterHeight);

            /*Assume that a space is followed by the start of a word*/
            if (curText.getText().equals(" ")) {
                mightWrap = true;
                wordPointer = curNode.nextNode;
                numLettersInWord = 0;
            }

            /*If the current letter doesn't fit on the same line 
            and is currently part of a word. Then we word wrap the 
            current word to the next line*/
            if (((curXPos + curTextWidth) >= (screenWidth - MARGIN)) 
                && mightWrap && !(curNode.returnText().getText().equals(" "))) {
                curXPos = MARGIN;
                curYPos += letterHeight;
                for (int k = 0; k < numLettersInWord - 1; k++) {
                    Text letterInWord = wordPointer.returnText();
                    letterInWord.setX(curXPos);
                    letterInWord.setY(curYPos);
                    int curTWidth = ((int) Math.round(letterInWord.getLayoutBounds().getWidth()));
                    wordPointer.setXPosition(curXPos);
                    wordPointer.setYPosition(curYPos);
                    if ((wordPointer.getXPosition()) == MARGIN) {
                        linePointer.add(wordPointer);
                    }
                    curXPos += curTWidth;
                    wordPointer = wordPointer.nextNode;
                }
                mightWrap = false;
            }  

            else if ((curXPos + curTextWidth) >= (screenWidth - MARGIN)
                    && !curText.getText().equals(" ")) {
                curYPos += letterHeight;
                curXPos = MARGIN;
            }

            /*update the x and y positon of each letter within TextBuffer*/
            curNode.setXPosition(curXPos);
            curNode.setYPosition(curYPos);

            curText.setX(curNode.getXPosition());
            curText.setY(curNode.getYPosition());

            numLettersInWord += 1;

            /*Special treatment for whitespace characters at the end 
            of a line so that they never be wrapped to the next line*/
            if ((curXPos + curTextWidth) >= (screenWidth - MARGIN)
                    && curText.getText().equals(" ")) {
                cursor.setX(MARGIN);
                cursor.setY(curYPos + letterHeight);
            } else {

            /*If points to a new line character then, then shifts
            the pointer to the next line.*/
            cursor.setHeight(letterHeight);
            if (isNewLine || ((curXPos + curTextWidth) >= (screenWidth - MARGIN)
                    && curText.getText().equals(" "))) {
                curYPos += letterHeight;
                curXPos = MARGIN;
                cursor.setX(curXPos);
                cursor.setY(curYPos);
            } else {
                /*update cursor according to the text the cursor
                points to*/
                Text cursorPointsTo = cursorPointer.returnText();

                /*Check in case cursor is not pointing to any letter,
                usually caused by backspace/deleting characters*/
                if (cursorPointsTo != null) {
                    int width = ((int) Math.round(cursorPointsTo.getLayoutBounds().getWidth()));
                    cursor.setX(cursorPointer.getXPosition() + width);
                    cursor.setY(cursorPointer.getYPosition());
                }

                yPosOfLastLine = curYPos;
            }
        }
        /*add up linePointer with pointers to the start
        of each new line*/
        if ((curNode.getXPosition()) == MARGIN) {
            linePointer.add(curNode);
        }

        curXPos = curXPos + curTextWidth;
        curNode = curNode.nextNode;     
        }
        scrollBar.setMax(letterHeight*linePointer.size() - windowHeight);
    }

    /*Make the cursor blink by creating a BlinkEvent
    and set the duration */
    public void blinkCursor() {
        final Timeline t = new Timeline();
        t.setCycleCount(Timeline.INDEFINITE);
        BlinkEventHandler cursorChange = new BlinkEventHandler();
        KeyFrame kF = new KeyFrame(Duration.seconds(BLINK_DURATION), cursorChange);
        t.getKeyFrames().add(kF);
        t.play();
    }

    /*return a SIZE pt font sample letter*/
    public Text getSampleLetter(int size) {
        Text sample = new Text("s");
        sample.setFont(Font.font(fontName, size));
        return sample;
    }

    /*Snap the screen back into place if user types when the 
    cursor is off screen, or when move cursor using arrow keys
    off the screen
    -Only able to get it to work when cursor is below the screen*/
    public void adjustScrollBar() {
        Text sampleText = getSampleLetter(fontSize);
        int sampleHeight = (int) Math.round(sampleText.getLayoutBounds().getHeight());

        int yPosOfCursor = (int) Math.round(cursor.getY());
        int shiftTextBy = yPosOfCursor - windowHeight + sampleHeight;
        int cursorLocation = (int) Math.round(yPosOfCursor + textRoot.getLayoutY());

        if (cursorLocation < 0) {
            scrollBar.setValue(yPosOfCursor);
        } else if (cursorLocation >= windowHeight) {
            scrollBar.setValue(shiftTextBy);
        }
    }

    /*Takes in the TextBuffer, and File name, go through all the nodes
    and update the original document*/
    public void save(String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            TextNode textPointer = textBuffer.getFirst();
            for (int i = 0; i < textBuffer.size(); i++) {
                String curLetter = textPointer.returnText().getText();
                if (curLetter.equals(ENTER_KEY) || curLetter.equals(NEW_LINE)) {
                    writer.write("\r\n"); //because of Window 10...
                } else {
                    writer.write(curLetter);
                }
                textPointer = textPointer.nextNode;
            }
            writer.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }

    @Override
    public void start(Stage primaryStage) {

        root = new Group();
        root.getChildren().add(textRoot);

        Scene scene = new Scene(root, windowWidth, windowHeight);

        Parameters parameters = getParameters();
        List<String> arguments = parameters.getRaw();

        if (arguments.size() == 0) {
            System.out.println("No filename was provided.");
            Text error = new Text("No filename was provided");
            root.getChildren().add(error);   //the idea is for the Editor display the 
            primaryStage.setTitle("Editor"); //message and then exit. But too fast to see
            primaryStage.setScene(scene);
            primaryStage.show();
            System.exit(0);
        }

        EventHandler<KeyEvent> keyEventHandler = 
                new KeyEventHandler();

        EventHandler<MouseEvent> mouseEventHandler = 
                new MouseEventHandler();

        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnMouseClicked(mouseEventHandler);

        textRoot.getChildren().add(cursor);
        blinkCursor();

        scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);

        scrollBar.setPrefHeight(windowHeight);
        scrollBar.setMin(STARTING_LINE);
        scrollBar.setMax(yPosOfLastLine);

        int scrollBarWidth = (int) Math.round(scrollBar.getLayoutBounds().getWidth());
        screenWidth = windowWidth - scrollBarWidth;
        scrollBar.setLayoutX(screenWidth);

        root.getChildren().add(scrollBar);

        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldValue,
                    Number newValue) {
                textRoot.setLayoutY(-(newValue.intValue()));
            }
        });

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                windowWidth = newScreenWidth.intValue();
                screenWidth = windowWidth - (int) Math.round(scrollBar.getLayoutBounds().getWidth());
                render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                scrollBar.setLayoutX(screenWidth);
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                windowHeight= newScreenHeight.intValue();
                render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                scrollBar.setPrefHeight(windowHeight);
            }
        });

        primaryStage.setTitle("Editor");
        primaryStage.setScene(scene);
        inputFileName = arguments.get(0);

        try {
            File inputFile = new File(inputFileName);
            if(inputFile.exists()) {
                FileReader reader = new FileReader(inputFile);
                BufferedReader bufferedReader = new BufferedReader(reader);
                int intRead = -1;

                while ((intRead = bufferedReader.read()) != -1) {
                    char charRead = (char) intRead;
                    if (charRead == NEW_LINE_IN_ASCII) { //since \n always follow 
                        charRead = (char) bufferedReader.read(); 
                    }                                            
                    fillTextBuffer(Character.toString(charRead));
                }
                render(textBuffer, fontName, fontSize, windowWidth, windowHeight);
                bufferedReader.close();
            }
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("Unable to open " + inputFileName + " Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
