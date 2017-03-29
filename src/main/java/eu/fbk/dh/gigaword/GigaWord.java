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

        final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

        File inputFile = cmd.getOptionValue("input", File.class);
        File outputFile = cmd.getOptionValue("output", File.class);

        Integer blockSize = 100;
        if (args.length > 2) {
            blockSize = Integer.parseInt(args[2]);
        }

        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit");
        System.out.println("Starting CoreNLP");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
        System.out.println("Stanford initialized");

        try {
            System.out.println("Starting reading");
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            System.out.println("Starting writing");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            String line;
            int i = 0;
            int j = 0;
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                buffer.append(line).append("\n");
                if (++i >= blockSize) {
                    j++;
                    System.out.print(".");
                    if (j % 100 == 0) {
                        System.out.println(" " + (j * blockSize));
                    }

                    Annotation annotation = new Annotation(buffer.toString());
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

                    buffer = new StringBuffer();
                    i = 0;
                }
            }

            System.out.println("Closing stuff");

            reader.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
