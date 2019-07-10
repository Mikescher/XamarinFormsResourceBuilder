package com.mikescher.xamarinforms.resourcebuilder;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileIO {
    public final static Charset CHARSET_UTF8 = StandardCharsets.UTF_8; //$NON-NLS-1$

    public final static String LINE_END = System.getProperty("line.separator"); //$NON-NLS-1$


    public static String readUTF8TextFile(File file) throws IOException {
        FileInputStream stream;
        String result = readUTF8TextFile(stream = new FileInputStream(file));
        stream.close();
        return result;
    }

    public static List<String> readUTF8TextFileLines(String file) throws IOException {
        String str = readUTF8TextFile(new File(file));
        String[] arr = str.split("\\r?\\n");
        List<String> ls = new ArrayList<>();

        for (int i = 0; i < arr.length; i++)
        {
            if (arr[i].isEmpty()) continue;
            ls.add(arr[i]);
        }
        return ls;
    }

    public static String readUTF8TextFile(FileInputStream file) throws IOException {
        return readTextFile(new InputStreamReader(file, CHARSET_UTF8));
    }

    public static String readTextFile(InputStreamReader reader) throws IOException {
        return readTextFile(new BufferedReader(reader));
    }

    public static String readTextFile(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        boolean first = true;

        try {
            String s;

            while ((s = reader.readLine()) != null) {
                if (!first) {
                    content.append(LINE_END);
                }
                content.append(s);
                first = false;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return content.toString();
    }

    public static void writeTextFile(String filename, String text) throws IOException {
        writeTextFile(new File(filename), text);
    }

    public static void writeTextFile(File file, String text) throws IOException {
        FileOutputStream fos;
        OutputStreamWriter osw;
        BufferedWriter bw = null;

        try {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, CHARSET_UTF8);
            bw = new BufferedWriter(osw);

            bw.write(text);

            bw.close();
        } finally {
            if (bw != null) bw.close();
        }
    }

    public static String cs(File f) throws IOException {
        String checksum;
        try (FileInputStream fis = new FileInputStream(f)) { checksum = DigestUtils.sha256Hex(fis).toUpperCase(); }
        return checksum;
    }

    public static String cs(String s) throws IOException {
        return DigestUtils.sha256Hex(s).toUpperCase();
    }

}
