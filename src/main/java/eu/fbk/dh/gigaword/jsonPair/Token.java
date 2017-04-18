package eu.fbk.dh.gigaword.jsonPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alessio on 18/04/17.
 */

public class Token {

    private static final Logger LOGGER = LoggerFactory.getLogger(Token.class);

    Integer id, startoffset, endoffset;
    String word, lemma, pos, ner, sentiment;
    Boolean target;

    public String getWord() {
        return word;
    }

    public String getLemma() {
        return lemma;
    }

    public String getPos() {
        return pos;
    }
}
