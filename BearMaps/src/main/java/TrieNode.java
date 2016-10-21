public class TrieNode {
    char letter;
    int size;
    String word;
    TrieNode[] children;

    public TrieNode(char letter, String word) {
        this.letter = letter;
        this.word = word;
        children = new TrieNode[27];
        size = 0;
    }

    public String getWord() {
        return this.word;
    }
}
