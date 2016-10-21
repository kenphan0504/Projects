package editor;
import javafx.scene.text.Text;

/* Represent each Text to be displayed as a TextNode
with information of its TEXT character, previous node, 
next node, x position, y position , and its own position
in the list */
public class TextNode {
    public Text text;
    public TextNode prevNode;
    public TextNode nextNode;
    private int xPos;
    private int yPos;

    public TextNode(Text text, TextNode prev, TextNode next, 
                int x, int y) {
        this.text = text;
        prevNode = prev;
        nextNode = next;
        xPos = x;
        yPos = y;
    }

    public Text returnText() {
        return text;
    }

    public void setXPosition(int x) {
        xPos = x;
    }

    public void setYPosition(int y) {
        yPos = y;
    }

    public int getXPosition() {
        return xPos;
    }

    public int getYPosition() {
        return yPos;
    }
}
