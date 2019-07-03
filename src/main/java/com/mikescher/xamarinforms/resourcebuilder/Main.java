package com.mikescher.xamarinforms.resourcebuilder;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;

public class Main {
    public final static Charset CHARSET_UTF8 = StandardCharsets.UTF_8; //$NON-NLS-1$

    public final static String LINE_END = System.getProperty("line.separator"); //$NON-NLS-1$

    private static String vdt;

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.out.println("Not enough parameter");
            return;
        }

        File fXmlFile = new File(args[0]);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        String dir = fXmlFile.getParent();
        String outroot = root.getAttribute("outputroot");

        vdt = System.getProperty("os.name").toLowerCase().contains("win")
                ? Paths.get(dir, root.getAttribute("vd-tool-win")).toAbsolutePath().toString()
                : Paths.get(dir, root.getAttribute("vd-tool-nix")).toAbsolutePath().toString();

        NodeList fileNodes = root.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++)
        {
            Element fileNode = (Element)fileNodes.item(i);

            String filepath = Paths.get(dir, fileNode.getAttribute("path")).toAbsolutePath().toString() ;

            System.out.println("[CONVERT] " + new File(filepath).getName());

            NodeList outputNodes = fileNode.getElementsByTagName("output");
            for (int j = 0; j < outputNodes.getLength(); j++)
            {
                Element outputNode = (Element)outputNodes.item(j);
                String outputpath = Paths.get(dir, outroot, outputNode.getAttribute("path")).toAbsolutePath().toString() ;

                if (outputpath.endsWith(".png")) {
                    int ww = Integer.parseInt(outputNode.getAttribute("width"));
                    int hh = Integer.parseInt(outputNode.getAttribute("height"));
                    outputRasterImage(filepath, outputpath, ww, hh);

                } else if (outputpath.endsWith(".xml")) {
                    String ww = outputNode.getAttribute("vector_width");
                    String hh = outputNode.getAttribute("vector_height");
                    outputAndroidVector(filepath, outputpath, ww, hh);

                } else if (outputpath.endsWith(".pdf")) {

                    outputVectorPDF(filepath, outputpath);

                } else {

                    throw new Exception("Unsupported file extension for " + outputpath);

                }
            }

            System.out.println();
        }
    }

    private static void outputRasterImage(String input, String output, int ww, int hh) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_2_", ".png");
        f_tmp.deleteOnExit();
        File f_out = Paths.get(output).toFile();

        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) ww);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) hh);
        TranscoderInput tcinput = new TranscoderInput(new FileInputStream(input));
        OutputStream ostream = new FileOutputStream(f_tmp);
        TranscoderOutput tcoutput = new TranscoderOutput(ostream);
        t.transcode(tcinput, tcoutput);
        ostream.flush();

        String xold = f_out.exists() ? cs(f_out) : "";
        String xnew = cs(f_tmp);

        if (xold.equals(xnew)) {
            f_tmp.delete();
            System.out.println("[ ] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  No changes");
        } else {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  File changed");
        }
    }

    private static void outputAndroidVector(String input, String output, String ww, String hh) throws Exception {
        File f_in = new File(input);
        File f_out = Paths.get(output).toFile();
        File f_tmp_dir = Paths.get(System.getProperty("java.io.tmpdir"), "xfrb_" + UUID.randomUUID()).toFile();
        f_tmp_dir.mkdirs();
        f_tmp_dir.deleteOnExit();
        File f_tmp_file = Paths.get(f_tmp_dir.getAbsolutePath(), f_in.getName().replace(".svg", ".xml")).toFile();
        f_tmp_file.deleteOnExit();

        Tuple3<Integer, String, String> r = ProcessHelper.procExec(vdt, "-in", input, "-out", f_tmp_file.getParent(), "-c", "-widthDp", ww.replace("dp", ""), "-heightDp", hh.replace("dp", ""));

        if (r.Item1 != 0)  throw new Exception("vd-tool failed: \n\n" + r.Item1 + "\n\n" + r.Item2 + "\n\n" + r.Item3);

        String xold = f_out.exists() ? readUTF8TextFile(f_out) : "";
        String xnew = readUTF8TextFile(f_tmp_file);

        if (xold.equals(xnew)) {
            f_tmp_file.delete();
            f_tmp_dir.delete();
            System.out.println("[ ] " + StringUtils.rightPad("XML @ ("+ww+")x("+hh+")", 24) + "  --  No changes");
        } else {
            f_out.delete();
            f_tmp_file.renameTo(f_out);
            f_tmp_dir.delete();
            System.out.println("[!] " + StringUtils.rightPad("XML @ ("+ww+")x("+hh+")", 24) + "  --  File changed");
        }
    }

    private static void outputVectorPDF(String input, String output)
    {
        File f_in = new File(input);
        File f_out = new File(output);

        System.out.println("[ ] " + StringUtils.rightPad("PDF", 24) + "  --  No changes");
    }

    public static String readUTF8TextFile(File file) throws IOException {
        FileInputStream stream;
        String result = readUTF8TextFile(stream = new FileInputStream(file));
        stream.close();
        return result;
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

    private static String cs(File f) throws IOException {
        String checksum;
        try (FileInputStream fis = new FileInputStream(f)) { checksum = DigestUtils.sha256Hex(fis).toUpperCase(); }
        return checksum;
    }
}
