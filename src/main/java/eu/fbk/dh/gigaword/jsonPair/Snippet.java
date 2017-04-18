package eu.fbk.dh.gigaword.jsonPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by alessio on 18/04/17.
 */

public class Snippet {

    private static final Logger LOGGER = LoggerFactory.getLogger(Snippet.class);

    String speaker;
    List<Sentence> sentences;

    public List<Sentence> getSentences() {
        return sentences;
    }
}
