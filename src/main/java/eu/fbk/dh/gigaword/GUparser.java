package eu.fbk.dh.gigaword;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dh.tint.digimorph.DigiMorph;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 12/04/17.
 */

public class GUparser {

    private static final Logger LOGGER = LoggerFactory.getLogger(GUparser.class);
    private static final Pattern accentPattern = Pattern.compile("(\\w*)([AEIOUaeiou])'([^\\w]|$)");
    private static final Set<String> upperCase = new HashSet<>();

    public static String uppercaseFirst(String message) {
        return Character.toUpperCase(message.charAt(0)) + message.substring(1);
    }

    static class ParseFile implements Runnable {

        File f;
        BufferedWriter writer;
        Set<String> eAcute;

        public ParseFile(File f, BufferedWriter writer, Set<String> eAcute) {
            this.f = f;
            this.writer = writer;
            this.eAcute = eAcute;
        }

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override public void run() {
            try {
                synchronized (System.out) {
                    LOGGER.info(f.getAbsolutePath());
                }
                
                List<String> lines = Files.readLines(f, Charsets.UTF_8);
                boolean lastIsUppercase = false;

                ArrayList<String> newLines = new ArrayList<>();
                StringBuffer buffer = new StringBuffer();
                for (String line : lines) {

                    boolean startWithSpace = line.startsWith(" ");

                    // Replacing double spaces
                    line = line.replaceAll("\\s+", " ");
                    line = line.trim();

                    if (line.length() == 0) {
                        continue;
                    }

                    // Remove uppercased rows
                    boolean isUppercase = false;
                    if (upperCase.contains(line)) {
                        line = line.toLowerCase();
                        isUppercase = true;
                    }

                    // Replacing accents
                    StringBuffer sb = new StringBuffer();
                    Matcher matcher = accentPattern.matcher(line);
                    while (matcher.find()) {
                        StringBuffer replacementText = new StringBuffer();
                        String firstPart = matcher.group(1);
                        replacementText.append(firstPart);
                        switch (matcher.group(2).charAt(0)) {
                        case 'a':
                            replacementText.append("à");
                            break;
                        case 'e':
                            if (eAcute.contains(firstPart.toLowerCase())) {
                                replacementText.append("é");
                            } else {
                                replacementText.append("è");
                            }
                            break;
                        case 'i':
                            replacementText.append("ì");
                            break;
                        case 'o':
                            replacementText.append("ò");
                            break;
                        case 'u':
                            replacementText.append("ù");
                            break;
                        case 'A':
                            replacementText.append("À");
                            break;
                        case 'E':
                            if (eAcute.contains(firstPart.toLowerCase())) {
                                replacementText.append("É");
                            } else {
                                replacementText.append("È");
                            }
                            break;
                        case 'I':
                            replacementText.append("Ì");
                            break;
                        case 'O':
                            replacementText.append("Ò");
                            break;
                        case 'U':
                            replacementText.append("Ù");
                            break;
                        }
                        replacementText.append(matcher.group(3));
                        matcher.appendReplacement(sb, replacementText.toString());
                    }
                    matcher.appendTail(sb);
                    line = sb.toString();

                    if (startWithSpace && (!lastIsUppercase || Character.isUpperCase(line.charAt(0)))) {
                        if (buffer.length() > 0) {
                            newLines.add(buffer.toString().trim());
                            buffer = new StringBuffer();
                        }
                    }

                    if (isUppercase) {
                        lastIsUppercase = true;
                    } else {
                        lastIsUppercase = false;
                    }

                    if (isUppercase && buffer.length() == 0) {
                        line = uppercaseFirst(line);
                    }
                    buffer.append(line).append(" ");
                }

                if (buffer.length() > 0) {
                    newLines.add(buffer.toString().trim());
                }

                synchronized (writer) {
                    for (String newLine : newLines) {
                        writer.append(newLine).append("\n");
                    }
                    writer.append("\n");
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                    .withOption("i", "input", "Input folder", "FILE", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("l", "limit", "Limit on number of files", "NUM", CommandLine.Type.INTEGER, true, false, false)
                    .withOption("t", "threads", "Number of threads (default 1)", "NUM", CommandLine.Type.INTEGER, true, false, false)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputDir = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);
            Integer limit = cmd.getOptionValue("limit", Integer.class);
            Integer nThreads = cmd.getOptionValue("threads", Integer.class, 1);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            // Add patterns for uppercase words
            upperCase.add("IL DIRETTORE");
            upperCase.add("IL PRESIDENTE");
            upperCase.add("IL RETTORE");

            // Loading Italian words with acute accent on "e"
            Set<String> eAcute = new HashSet<>();
            DigiMorph digiMorph = new DigiMorph();
            for (Object key : digiMorph.getMap().keySet()) {
                String ks = (String) key;
                if (ks.length() <= 1) {
                    continue;
                }
                if (ks.endsWith("é")) {
                    eAcute.add(ks.substring(0, ks.length() - 1));
                }
            }

            int fileNo = 0;

            LOGGER.info("Starting multi-thread extraction ({} threads)", nThreads);
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);

            for (File f : Files.fileTreeTraverser().preOrderTraversal(inputDir)) {
                if (!f.isFile()) {
                    continue;
                }
                if (f.getName().startsWith(".")) {
                    continue;
                }

                fileNo++;

                ParseFile parseFile = new ParseFile(f, writer, eAcute);
                executor.execute(parseFile);

                if (limit != null && fileNo > limit) {
                    break;
                }
            }

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }

    }
}
