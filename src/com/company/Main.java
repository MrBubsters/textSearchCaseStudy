package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {


    public static Connection connectDatabase() throws SQLException {
        String url = "jdbc:postgresql://192.168.122.228:5432/postgres";
        return DriverManager.getConnection(url, "postgres", "test123");
    }


    public static boolean findString(String keyword, String searchText) {
        return searchText.contains(keyword);
    }

    public static int naiveSearch(String keyword, String searchText) {
        int count = 0;

        if (keyword.contains(" ")) {
            String[] keywords = keyword.split(" ");
            String[] split = searchText.split(" ");
            for (int i = 0; i < split.length; i++) {
                if (Objects.equals(split[i], keywords[0])) {
                    boolean match = true;
                    for (int j = 1; j < keywords.length; j++) {
                        if (!split[i + j].contains(keywords[j])) {
                            match = false;
                        }
                    }
                    if (match) {
                        count++;
                    }
                }
            }
            return count;
        }
        else {
            String[] split = searchText.split(" ");
            for (String word : split) {
                if (word.contains(keyword)) {
                    count++;
                }
            }
            return count;
        }
    }

    public static long regexSearch(String keyword, String searchText) {
        /**
         * Regex search for single words using \b is 10x slower than just searching for the text in all instances
         */
//            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
        Pattern pattern = Pattern.compile(keyword);
        Matcher matcher = pattern.matcher(searchText);
        long count = matcher.results().count();
        return count;
    }

    public static Map<String, String> generateTextMap() throws IOException {
        List<String> files = new ArrayList<String>();
        files.addAll(Arrays.asList("warp_drive.txt", "hitchhikers.txt", "french_armed_forces.txt"));
        String folder = "sample_text";

        Map<String, String> fileTextMap = new HashMap<String, String>();

        for (String fileName : files) {
            String path = folder + "/" + fileName;
            String text = Files.readString(Path.of(path));

            fileTextMap.put(fileName, text);
        }
        return fileTextMap;
    }

    public static void testRun() throws IOException, SQLException {
        /**
         String text search methods (fastest in small batches)
         */
        List<String> files = new ArrayList<String>();
        files.addAll(Arrays.asList("warp_drive.txt", "hitchhikers.txt", "french_armed_forces.txt"));
        String folder = "sample_text";

        Map<String, String> fileTextMap = new HashMap<String, String>();
        Map<String, Integer> searchTextMap = new HashMap<String, Integer>();

        for (String fileName : files) {
            String path = folder + "/" + fileName;
            String text = Files.readString(Path.of(path));

            fileTextMap.put(fileName, text);
        }

//        String keyword = "the";
//        long startTime = System.currentTimeMillis();
//        System.out.println(naiveSearch(keyword, fileTextMap.get("warp_drive.txt")));
//        System.out.println(naiveSearch(keyword, fileTextMap.get("hitchhikers.txt")));
//        System.out.println(naiveSearch(keyword, fileTextMap.get("french_armed_forces.txt")));
//        long duration = (System.currentTimeMillis() - startTime);
//        System.out.println("Time of execution: " + duration + "ms");

        List<String> keywords = new ArrayList<>();
        while (keywords.size() < 20000000) {
            BufferedReader reader = new BufferedReader(new FileReader("sample_text/words_alpha.txt"));
            String line = reader.readLine();
            while (line != null) {
                keywords.add(line);
                line = reader.readLine();
            }
            reader.close();
        }
        System.out.println(keywords.size() + " keywords");

        System.out.println("Starting naive search for string");
        long startTime = System.currentTimeMillis();
        for (String keyword : keywords) {
            for (String key : fileTextMap.keySet()) {
                int count = naiveSearch(keyword, fileTextMap.get(key));
                searchTextMap.put(key, count);
            }
        }
        long duration = (System.currentTimeMillis() - startTime);
        System.out.println("Time of execution: " + duration + "ms");
        System.out.println("Average query length: " + duration/keywords.size());


        System.out.println("Starting regex search for string");
        startTime = System.currentTimeMillis();
        for (String keyword : keywords) {
            for (String key : fileTextMap.keySet()) {
                int count = (int) regexSearch(keyword, fileTextMap.get(key));
                searchTextMap.put(key, count);
            }
        }
        duration = (System.currentTimeMillis() - startTime);
        System.out.println("Time of execution: " + duration + "ms");
        System.out.println("Average query length: " + duration/keywords.size());


        /**
         * SQL methods
         */

        Connection conn = connectDatabase();

//        startTime = System.currentTimeMillis();
//        for (String keyword : keywords) {
//            String sql = String.format("select regexp_matches(sample_text, '%s', 'g') from public.text_documents td", keyword);
//            Statement stmt = conn.createStatement();
//
//            ResultSet rs = stmt.executeQuery(sql);
////            while (rs.next()) {
////                System.out.println(rs.getInt("count"));
////            }
//        }
//        duration = (System.currentTimeMillis() - startTime);
//        System.out.println("Time of execution: " + duration + "ms");

        System.out.println("Starting SQL indexed search for string");
        startTime = System.currentTimeMillis();
        for (String keyword : keywords) {
            String sql = String.format("select * from public.text_documents td where indexed_text @@ to_tsquery('%s') ", keyword);
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql);
//            while (rs.next()) {
//                System.out.println(rs.getInt("count"));
//            }
        }
        duration = (System.currentTimeMillis() - startTime);
        System.out.println("Time of execution: " + duration + "ms");
        System.out.println("Average query length: " + duration/keywords.size());
    }

    public static void runApp() {
        try {
            Map<String, String> fileTextMap = generateTextMap();
            Connection conn = connectDatabase();

            Scanner reader = new Scanner(System.in);
            System.out.println("Enter a search term:");

            String keyword = reader.next();

            System.out.println("Search Method: 1) String match 2) Regex 3) Indexed");
            int method = reader.nextInt();

            long startTime = System.currentTimeMillis();
            StringBuilder results = new StringBuilder("Search results:\n");

            if (method == 1) {
                for (String key : fileTextMap.keySet()) {
                    int count = naiveSearch(keyword, fileTextMap.get(key));
                    results.append(key).append(" - ").append(count).append(" matches\n");
                }
            }
            else if (method == 2) {
                for (String key : fileTextMap.keySet()) {
                    int count = (int) regexSearch(keyword, fileTextMap.get(key));
                    results.append(key).append(" - ").append(count).append(" matches\n");
                }
            }
            else if (method == 3) {
                String sql = String.format("select title from public.text_documents td where indexed_text @@ to_tsquery('%s')", keyword);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String title = rs.getString("title");

//                    results = results +
                }
            }

            System.out.println(results);

            long duration = (System.currentTimeMillis() - startTime);
            System.out.println("Time of execution: " + duration + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Would you like to search for a term? (y|n)");
        Scanner reader = new Scanner(System.in);
        String appFlag = reader.next();

        if (appFlag.equals("y")) {
            runApp();
        }
        else {
            System.out.println("Running benchmark mode");
            try {
                testRun();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
