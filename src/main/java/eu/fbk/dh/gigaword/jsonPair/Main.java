package eu.fbk.dh.gigaword.jsonPair;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 18/04/17.
 */

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_REGEXP = "^(NN|JJ|VB)";

    public static void parseSnippet(Writer writer, Snippet snippet, String idSnippet, Pattern posPattern, String annotation, String target,
            String id)
            throws IOException {
        for (Sentence sentence : snippet.getSentences()) {
            for (Token token : sentence.getTokens()) {
                Matcher matcher = posPattern.matcher(token.getPos());
                if (matcher.find()) {
                    writer.append(annotation).append("\t");
                    writer.append(id).append("\t");
                    writer.append(idSnippet).append("\t");
                    writer.append(target).append("\t");
                    writer.append(token.getLemma().toLowerCase()).append("\t");
                    writer.append(token.getWord().toLowerCase()).append("\n");
                }
            }
        }

    }

    public static void main(String[] args) {
//        String jsonFile = "/Users/alessio/Desktop/Step1_pipeline/Json_task_1.json";
//        String annotationFile = "/Users/alessio/Desktop/Step1_pipeline/t1_annotations_all.txt";
//        String outputFile = "/Users/alessio/Desktop/Step1_pipeline/output.txt";

        Gson gson = new Gson();
        Map<String, String> annotations = new HashMap<>();

        try {
            final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                    .withOption("j", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("a", "annotation", "Annotation file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("r", "regexp", String.format("Regular expression, default %s", DEFAULT_REGEXP), "REGEXP", CommandLine.Type.STRING,
                            true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File jsonFile = cmd.getOptionValue("j", File.class);
            File annotationFile = cmd.getOptionValue("a", File.class);
            File outputFile = cmd.getOptionValue("o", File.class);

            String regExp = cmd.getOptionValue("r", String.class, DEFAULT_REGEXP);
            Pattern posPattern = Pattern.compile(regExp);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(jsonFile), "UTF-8"));
            List<String> lines = Files.readLines(annotationFile, Charsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }

                annotations.put(parts[0], parts[1]);
            }

            reader.beginObject();
            reader.nextName();
            reader.beginArray();
            while (reader.hasNext()) {
                Pair pair = gson.fromJson(reader, Pair.class);
                String id = pair.getId();
                if (!annotations.containsKey(id)) {
                    continue;
                }

                String annotation = annotations.get(id);
                String target = pair.getTarget().replaceAll("[]\\(\\)]", "");

                parseSnippet(writer, pair.getSnippet1(), "1", posPattern, annotation, target, id);
                parseSnippet(writer, pair.getSnippet2(), "2", posPattern, annotation, target, id);
            }

            reader.close();
            writer.close();
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
