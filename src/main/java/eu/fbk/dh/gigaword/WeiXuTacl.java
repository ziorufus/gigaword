package eu.fbk.dh.gigaword;

import com.google.common.collect.HashMultimap;
import eu.fbk.utils.core.CommandLine;
import eu.fbk.utils.core.diff_match_patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 07/03/17.
 */

public class WeiXuTacl {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeiXuTacl.class);
    private static diff_match_patch diff_match_patch = new diff_match_patch();
    private static Pattern parenthesisPattern = Pattern.compile("-[a-z]{3}-");

    public static void main(String[] args) {

        try {
            final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                    .withOption("i", "input", "Input folder", "FOLDER", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            FileOutputStream stream = new FileOutputStream(outputFile);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

            List<String> simpList = new ArrayList<>();
            HashMultimap<String, String> done = HashMultimap.create();

            List<String> types = new ArrayList<>();
            types.add("tune");
            types.add("test");

            for (String type : types) {
                List<String> normLines = new ArrayList<>();
                normLines.addAll(readFile(inputFile, type + ".8turkers.tok.norm"));
                simpList.add(type + ".8turkers.tok.simp");
                for (int i = 0; i < 8; i++) {
                    simpList.add(type + ".8turkers.tok.turk." + i);
                }

                for (String s : simpList) {
                    printDiff(normLines, readFile(inputFile, s), writer, done);
                }
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }

    private static void printDiff(List<String> normLines, List<String> simpLines, Writer writer,
            HashMultimap<String, String> done) throws IOException {
        for (int i = 0; i < normLines.size(); i++) {

            String n = normLines.get(i);
            String s = simpLines.get(i);

            LinkedList<eu.fbk.utils.core.diff_match_patch.Diff> diffs = diff_match_patch.diff_main(n, s);
            diff_match_patch.diff_cleanupSemantic(diffs);

            int equalMinSize = 10;
            int insDelMinSize = 5;

            for (int j = 0; j < diffs.size() - 4; j++) {
                ArrayList<eu.fbk.utils.core.diff_match_patch.Diff> theseDiffs = new ArrayList<>();
                for (int k = 0; k < 4; k++) {
                    theseDiffs.add(diffs.get(j + k));
                }
                if (theseDiffs.get(0).operation != eu.fbk.utils.core.diff_match_patch.Operation.EQUAL ||
                        theseDiffs.get(1).operation != eu.fbk.utils.core.diff_match_patch.Operation.DELETE ||
                        theseDiffs.get(2).operation != eu.fbk.utils.core.diff_match_patch.Operation.INSERT ||
                        theseDiffs.get(3).operation != eu.fbk.utils.core.diff_match_patch.Operation.EQUAL) {
                    continue;
                }

                if (theseDiffs.get(0).text.length() < equalMinSize) {
                    continue;
                }
                if (theseDiffs.get(3).text.length() < equalMinSize) {
                    continue;
                }

                if (theseDiffs.get(1).text.length() < insDelMinSize) {
                    continue;
                }
                if (theseDiffs.get(2).text.length() < insDelMinSize) {
                    continue;
                }

                Matcher matcher;
                matcher = parenthesisPattern.matcher(theseDiffs.get(1).text);
                if (matcher.find()) {
                    continue;
                }
                matcher = parenthesisPattern.matcher(theseDiffs.get(2).text);
                if (matcher.find()) {
                    continue;
                }

                int lastSpace = theseDiffs.get(0).text.lastIndexOf(" ");
                int len = theseDiffs.get(0).text.length();

//                System.out.println(c);
//                System.out.println(lastSpace);
//                System.out.println(len);

                if (lastSpace + 1 != len) {
                    String toAdd = theseDiffs.get(0).text.substring(lastSpace + 1);
                    String oldText = theseDiffs.get(0).text;
                    theseDiffs.get(0).text = oldText.substring(0, oldText.length() - toAdd.length());
                    theseDiffs.get(1).text = toAdd + theseDiffs.get(1).text;
                    theseDiffs.get(2).text = toAdd + theseDiffs.get(2).text;
//                    System.out.println(toAdd);
                }

                int firstSpace = theseDiffs.get(3).text.indexOf(" ");
                if (firstSpace > 0) {
                    String toAdd = theseDiffs.get(3).text.substring(0, firstSpace);
                    String oldText = theseDiffs.get(3).text;
                    theseDiffs.get(3).text = oldText.substring(firstSpace);
                    theseDiffs.get(1).text = theseDiffs.get(1).text + toAdd;
                    theseDiffs.get(2).text = theseDiffs.get(2).text + toAdd;

                }

                if (done.containsKey(theseDiffs.get(1).text)) {
                    if (done.get(theseDiffs.get(1).text).contains(theseDiffs.get(2).text)) {
                        continue;
                    }
                }

                done.put(theseDiffs.get(1).text, theseDiffs.get(2).text);

                writer.append(theseDiffs.toString()).append("\n");
            }

//            for (eu.fbk.utils.core.diff_match_patch.Diff diff : diffs) {
//                System.out.println(diff);
//            }

        }
    }

    private static List<String> readFile(File dir, String fileName) throws IOException {
        File thisFile = new File(dir.getAbsolutePath() + File.separator + fileName);
        return Files.readAllLines(thisFile.toPath());
    }
}
