package editor;
import java.util.Stack;

/*A Stack that will have a limited size.
-Takes in Text objects to store undo/redo 
information
-When shortcut+Z(undo), remove a text node from 
textBuffer and push into here to store for redo later
-When shortcut+Y(redo), pop the most recent text node 
out and add it back to data structure*/
public class BoundedStack<T> extends Stack<T> {
    private int bound;

    public BoundedStack(int b) {
        super();
        bound = b;
    }

    @Override
    public T push(T t) {
        if (this.size() >= bound) {
            remove(0);
        }
        return super.push(t);
    }
}
