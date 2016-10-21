package editor;
import javafx.scene.text.Text;

public class TextBuffer {

    public TextNode sentinel;
    public int size;

    public TextBuffer() {
        sentinel = new TextNode(null, null, null, 0, 0);
        sentinel.prevNode = sentinel;
        sentinel.nextNode = sentinel;
        size = 0;
    }

    /*Add a new Text node to the end in constant time given
    x position and y position. Update size and current position
    and return the new Node*/
    public TextNode addLast(Text text, int x, int y) {
        TextNode newNode = new TextNode(text, sentinel.prevNode, sentinel, x, y);
        TextNode p = sentinel.prevNode;
        p.nextNode = newNode;
        sentinel.prevNode = newNode;
        size += 1;
        return newNode;
    }

    /*Remove last Text node and update size and curPosition*/
    public TextNode removeLast() {
        TextNode lastNode = sentinel.prevNode;
        sentinel.prevNode = lastNode.prevNode;
        sentinel.prevNode.nextNode = sentinel;
        lastNode.prevNode = null;
        lastNode.prevNode = null;
        size -= 1;
        return lastNode;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public TextNode getFirst() {
        if (size == 0) {
            return null;
        }
        return sentinel.nextNode;
    }

    public TextNode getLast() {
        if (size == 0) {
            return null;
        }
        return sentinel.prevNode;
    }
}
