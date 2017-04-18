package eu.fbk.dh.gigaword.jsonPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alessio on 18/04/17.
 */

public class Pair {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pair.class);

    String id, score, debate_title, block_id, block_title, target;
    Snippet snippet1, snippet2;

    public String getId() {
        return id;
    }

    public String getTarget() {
        return target;
    }

    public Snippet getSnippet1() {
        return snippet1;
    }

    public Snippet getSnippet2() {
        return snippet2;
    }
}
