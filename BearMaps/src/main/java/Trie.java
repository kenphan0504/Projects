import java.util.Set;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

public class Trie {
    private TrieNode root;

    public Trie() {
        root = new TrieNode('*', "*");
    }

    public void insert(String word) {
        TrieNode tempRoot = root;
        String wordSoFar = "";
        String cleanedWord = GraphDB.cleanString(word);
        int wordLength = cleanedWord.length();
        int wordIndex = 0;

        while (wordIndex < wordLength) {
            int i = cleanedWord.charAt(wordIndex) - 97;
            if (i == -65) {
                i = 26;
            }
            if (tempRoot.children[i] != null) {
                wordSoFar = wordSoFar + tempRoot.children[i].letter;
                tempRoot = tempRoot.children[i];
                wordIndex += 1;
            } else {
                break;
            }
        }

        while (wordIndex < wordLength) {
            int i = cleanedWord.charAt(wordIndex) - 97;
            if (i == -65) {
                i = 26;
            }
            wordSoFar = wordSoFar + cleanedWord.charAt(wordIndex);
            tempRoot.children[i] = new TrieNode(cleanedWord.charAt(wordIndex), wordSoFar);
            tempRoot.size += 1;
            tempRoot = tempRoot.children[i];
            wordIndex += 1;
        }
        tempRoot.word = word;
    }

    public List<String> autocomplete(String prefix, HashMap<String, Long> nameToNodes) {
        Set<String> names = nameToNodes.keySet();
        LinkedList<String> foundNames = new LinkedList<>();
        TrieNode tempRoot = root;
        int prefixIndex = 0;
        int prefixLength = prefix.length();

        while (prefixIndex < prefixLength) {
            int i = prefix.charAt(prefixIndex) - 97;
            if (i == -65) {
                i = 26;
            }
            if (tempRoot.children[i] == null) {
                return null;
            }
            tempRoot = tempRoot.children[i];
            prefixIndex += 1;
        }
        traverseAndFindNames(tempRoot, foundNames, names);
        return foundNames;
    }

    public void traverseAndFindNames(TrieNode t, LinkedList<String> list, Set<String> names) {
        if (t.size == 0) {
            list.add(t.getWord());
        }
        for (TrieNode child : t.children) {
            if (child != null) {
                if (names.contains(child.getWord())) {
                    list.add(child.getWord());
                }
                traverseAndFindNames(child, list, names);
            }
        }
    }
}
