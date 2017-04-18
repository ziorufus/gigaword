package eu.fbk.dh.gigaword;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Created by alessio on 07/03/17.
 */

public class GigaWord {

    private static final Logger LOGGER = LoggerFactory.getLogger(GigaWord.class);

    public static void main(String[] args) {
//        String inputFileName = args[0];
//        String outputFileName = args[1];

        try {
            final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("p", "properties", "Properties file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);
            File propertiesFile = cmd.getOptionValue("properties", File.class);

            Properties properties = new Properties();
            properties.setProperty("annotators", "tokenize, ssplit");
            properties.setProperty("ssplit.newlineIsSentenceBreak", "always");

            if (propertiesFile != null) {
                properties.load(new FileReader(propertiesFile));
            }

            System.out.println("Starting CoreNLP");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
            System.out.println("Stanford initialized");

            System.out.println("Starting reading");
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            System.out.println("Starting writing");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            String line;
            int j = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                Annotation annotation = new Annotation(line);
                pipeline.annotate(annotation);

                StringBuffer sentenceBuffer = new StringBuffer();
                for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                    for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                        sentenceBuffer.append(token.originalText()).append(" ");
                    }
                    writer.append(sentenceBuffer.toString().trim()).append("\n");
                    sentenceBuffer = new StringBuffer();
                    writer.flush();
                }
            }

            System.out.println("Closing stuff");

            reader.close();
            writer.close();
        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
