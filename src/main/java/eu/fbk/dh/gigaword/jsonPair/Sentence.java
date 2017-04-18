package eu.fbk.dh.gigaword.jsonPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by alessio on 18/04/17.
 */

public class Sentence {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sentence.class);

    Integer id, sentiment;
    List<Token> tokens;
    String parsing;

    public List<Token> getTokens() {
        return tokens;
    }
}
