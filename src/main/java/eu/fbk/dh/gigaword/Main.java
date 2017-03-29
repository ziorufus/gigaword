package eu.fbk.dh.gigaword;


import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by giovannimoretti on 27/03/17.
 */
public class Main {
    public static void main(String[] args) {



        /// SBLOCCA PER LEGGERE
/*
        DB db_read = DBMaker.fileDB("file.db").fileMmapEnable()
                .closeOnJvmShutdown().readOnly()
                .make();
        ConcurrentMap<byte [], Integer> mappa = db_read.hashMap("map").keySerializer(Serializer.BYTE_ARRAY).valueSerializer(Serializer.INTEGER).open();


        byte[] ba = args[0].getBytes();




            Integer i = mappa.get(ba);
            System.out.println(new String(ba) + "\t" + i.toString());



        db_read.close();
        System.exit(0);
*/

        try {
            Files.deleteIfExists(Paths.get("file.db"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        DB db = DBMaker.fileDB("file.db").fileMmapEnable()
                .closeOnJvmShutdown()
                .make();

        ConcurrentMap<byte[], Integer> map = db.hashMap("map").keySerializer(Serializer.BYTE_ARRAY).valueSerializer(Serializer.INTEGER)
                .createOrOpen();


        File folder = new File(args[0]);
        try {
            java.nio.file.Files.walk(folder.toPath()).collect(Collectors.toList()).parallelStream()
                    .forEach(filePath -> {
                        try {
                            if (java.nio.file.Files.isRegularFile(filePath)) {

                                AtomicInteger iter = new AtomicInteger(0);

                                AtomicLong readed_size = new AtomicLong(0);
                                Long fsize = Files.size(filePath);


                                System.out.println(filePath.getFileName() + "\t" + Files.size(filePath));


                                try (Stream<String> stream = Files.lines(filePath.toAbsolutePath())) {
                                    stream.forEach(s -> {
                                                String[] items = s.split("\t");
                                                try {
                                                    map.putIfAbsent(items[0].getBytes(), 0);
                                                    map.put(items[0].getBytes() , map.get(items[0].getBytes()) + Integer.parseInt(items[1])  );
                                                    readed_size.addAndGet(s.getBytes().length);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    System.err.println("Something wrong with line splitting... line:" + iter.toString() + " " + filePath.getFileName());
                                                }
                                                if (iter.incrementAndGet() % 150000 == 0) {
                                                    System.out.println(filePath.getFileName() + "\t" + (readed_size.get() / (float) fsize) * 100);
                                                    //System.gc();
                                                }
                                            }
                                    );

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }


        db.close();

    }

}
