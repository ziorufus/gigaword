package eu.fbk.dh.gigaword;

import eu.fbk.utils.core.CommandLine;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 20/03/17.
 */

public class FindMultiWords {

    public static Pattern pattern = Pattern.compile("^\\w");
    public static Integer MIN_FREQ = 5;
    public static Integer NUM_THREAD = 6;
    public static Integer MAX_NGRAM = 3;
//    public static Integer STEP = 1000000;

    private static final Logger LOGGER = LoggerFactory.getLogger(FindMultiWords.class);

    static class CountStuff implements Runnable {

        private String line;
        Map<Integer, ConcurrentMap<byte[], Integer>> dbMaps;
        AtomicLong wordTot;

        public CountStuff(String line, Map<Integer, ConcurrentMap<byte[], Integer>> dbMaps, AtomicLong wordTot) {
            this.line = line;
            this.dbMaps = dbMaps;
            this.wordTot = wordTot;
        }

        @Override public void run() {

            int size = dbMaps.size();
            String[] buffer = new String[size];
            String[] parts = line.split("\\s+");
            Matcher matcher;
            StringBuffer b;
            byte[] bytes;
            ConcurrentMap<byte[], Integer> map;
            for (String part : parts) {
                matcher = pattern.matcher(part);
                if (!matcher.find()) {
                    continue;
                }

                for (int i = 1; i < buffer.length; i++) {
                    if (buffer[i] != null) {
                        buffer[i - 1] = buffer[i];
                    }
                }
                buffer[buffer.length - 1] = part;

                b = new StringBuffer();
                for (int i = buffer.length - 1; i >= 0; i--) {
                    int n = buffer.length - i;
                    b.insert(0, buffer[i]);
                    bytes = b.toString().getBytes();
                    map = dbMaps.get(n);
                    map.putIfAbsent(bytes, 0);
                    map.put(bytes, map.get(bytes) + 1);
                    b.insert(0, " ");
                }
            }

        }
    }

    public static void main(String[] args) {
//        String inputFile = args[0];
//        String outputFile = args[1];

//        ConcurrencyFrequencyHashSet<String> wordFreq = new ConcurrencyFrequencyHashSet<>();
//        ConcurrencyFrequencyHashSet<String> bigramFreq = new ConcurrencyFrequencyHashSet();
//        ConcurrencyFrequencyHashSet<String> trigramFreq = new ConcurrencyFrequencyHashSet();

        try {

            final CommandLine cmd = CommandLine.parser().withName("command-line-test")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "DB output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("f", "force", "Force file overwrite")
                    .withOption(null, "threads", "Number of threads", "NUM", CommandLine.Type.INTEGER, true, false, false)
                    .withOption(null, "ngram-max-size", String.format("Maximum number of tokens (default %d)", MAX_NGRAM), "NUM",
                            CommandLine.Type.INTEGER, true, false, false)

                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);
            Boolean forceOverwrite = cmd.hasOption("force");
            Integer nThreads = cmd.getOptionValue("threads", Integer.class, NUM_THREAD);
            Integer maxNgramSize = cmd.getOptionValue("ngram-max-size", Integer.class, MAX_NGRAM);

            LOGGER.info("Max ngram size: {}", maxNgramSize);

            if (outputFile.exists()) {
                if (forceOverwrite) {
                    outputFile.delete();
                } else {
                    LOGGER.warn("File {} exists, exiting (use the force option to overwrite)", outputFile.getName());
                    System.exit(1);
                }
            }

            DB db = DBMaker.fileDB(outputFile.getAbsolutePath())
                    .closeOnJvmShutdown()
                    .make();
            Map<Integer, ConcurrentMap<byte[], Integer>> dbMaps = new HashMap<>();
            for (int i = 0; i < maxNgramSize; i++) {
                dbMaps.put(i + 1,
                        db.hashMap("map" + (i + 1)).keySerializer(Serializer.BYTE_ARRAY).valueSerializer(Serializer.INTEGER).createOrOpen());
            }

            AtomicLong wordTot = new AtomicLong(0);
//            Long bigramTot = 0L;

            // Counting stuff
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            ExecutorService executor = Executors.newFixedThreadPool(nThreads);

            int counter = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase();
                CountStuff countStuff = new CountStuff(line, dbMaps, wordTot);
                executor.execute(countStuff);
                counter++;
                if (counter % 150000 == 0) {
                   System.gc();
                }
            }

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            Atomic.Long wordCount = db.atomicLong("wordCount").create();
            wordCount.addAndGet(wordTot.get());
            reader.close();

//            for (byte[] bytes : dbMaps.get(3).keySet()) {
//                System.out.println(new String(bytes));
//                System.out.println(dbMaps.get(3).get(bytes));
//                System.out.println();
//            }

//            System.out.println(dbMaps.get(1).toString());

//            BufferedWriter writer;

//            writer = new BufferedWriter(new FileWriter(wfFile));
//            for (String word : wordFreq.keySet()) {
//                writer.append(word).append("\t").append(wordFreq.get(word).toString()).append("\n");
//            }
//            writer.close();
//
//            writer = new BufferedWriter(new FileWriter(bfFile));
//            for (String word : bigramFreq.keySet()) {
//                writer.append(word).append("\t").append(bigramFreq.get(word).toString()).append("\n");
//            }
//            writer.close();
//
//            writer = new BufferedWriter(new FileWriter(tfFile));
//            for (String word : trigramFreq.keySet()) {
//                writer.append(word).append("\t").append(trigramFreq.get(word).toString()).append("\n");
//            }
//            writer.close();

            /*

//            Map<String, Double> out = calculatePMI(wordFreq, bigramFreq, wordTot);
            Map<String, Double> out = calculateTScore(wordFreq, bigramFreq, wordTot.get());

            System.out.println("Sorting");
            LinkedHashMap<String, Double> sorted = out.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            int i = 0;
            for (String s : sorted.keySet()) {
                i++;
                writer.append(i + " --- " + s + "\n");
//                System.out.println(i + " ---" + s);
//                System.out.println(sorted.get(s));
//                if (i++ > 50) {
//                    break;
//                }
            }

            writer.close();
                          */
        } catch (Exception e) {
            CommandLine.fail(e);
            // ignored
        }
    }

    private static Map<String, Double> calculatePMI(ConcurrencyFrequencyHashSet<String> wordFreq,
            ConcurrencyFrequencyHashSet<String> bigramFreq, Long wordTot) {
        Map<String, Double> ret = new HashMap<>();
        for (String bigram : bigramFreq.keySet()) {
            Integer freq = bigramFreq.get(bigram);
            if (freq < MIN_FREQ) {
                continue;
            }
            String[] parts = bigram.split("\\s+");

            double n = 1.0 * bigramFreq.get(bigram) / wordTot;
            double d = 1.0;
            for (String part : parts) {
                d = d * wordFreq.get(part);
            }
            d = d / wordTot;

            ret.put(bigram, Math.log(n / d));
        }
        return ret;
    }

    private static Map<String, Double> calculateTScore(ConcurrencyFrequencyHashSet<String> wordFreq,
            ConcurrencyFrequencyHashSet<String> bigramFreq, Long wordTot) {
        Map<String, Double> ret = new HashMap<>();
        for (String bigram : bigramFreq.keySet()) {
            Integer freq = bigramFreq.get(bigram);
            if (freq < MIN_FREQ) {
                continue;
            }
            String[] parts = bigram.split("\\s+");

            double n = 1.0 * bigramFreq.get(bigram) / wordTot;
            double d = 1.0;
            for (String part : parts) {
                d = d * wordFreq.get(part);
            }
            d = d / wordTot;

            ret.put(bigram, (n - d) / Math.sqrt(n));
        }
        return ret;
    }
}
