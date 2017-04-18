package eu.fbk.dh.gigaword;

import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 29/03/17.
 */

public class NGramIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NGramIndexer.class);
    private static Pattern removePos = Pattern.compile("^(.*)_[A-Z0-9]+");

    public static void main(String[] args) {

        try {
            final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)

                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            String inputFileName = cmd.getOptionValue("input", String.class);
            String outputFileName = cmd.getOptionValue("output", String.class);

            File file = new File(outputFileName);
            if (file.exists()) {
                LOGGER.info("File {} exists, exiting", outputFileName);
                System.exit(1);
            }

            LOGGER.info("Processing {}", inputFileName);

            InputStream inputStream = IO.read(inputFileName);
            OutputStream outputStream = IO.write(outputFileName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            String line;
            String lastWord = null;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 4) {
                    continue;
                }

                String word = parts[0];
                Integer num = Integer.parseInt(parts[2]);

                // Strategy?
                if (!word.contains("_")) {
                    continue;
                }
//                Matcher matcher = removePos.matcher(word);
//                if (matcher.find()) {
//                    word = matcher.group(1);
//                }

                if (lastWord == null || !lastWord.equals(word)) {
                    if (lastWord != null) {
                        writer.append(lastWord).append("\t").append(Integer.toString(count)).append("\n");
                    }
                    lastWord = word;
                    count = 0;
                }

                count += num;
            }
            writer.append(lastWord).append("\t").append(Integer.toString(count)).append("\n");

            reader.close();
            writer.close();
            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
