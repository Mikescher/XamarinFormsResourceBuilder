package com.mikescher.xamarinforms.resourcebuilder;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static com.mikescher.xamarinforms.resourcebuilder.FileIO.cs;
import static com.mikescher.xamarinforms.resourcebuilder.FileIO.writeTextFile;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.*;
import static com.mikescher.xamarinforms.resourcebuilder.SVGUtil.*;

public class Main {
    public static final String LOCK_VERSION = "1.1.0.0";

    public static String vdt;

    public static HashMap<Tuple2<String, String>, String> lockdata;
    public static HashMap<Tuple2<String, String>, String> lockdata_new = new HashMap<>();

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

        String dir = fXmlFile.getAbsoluteFile().getParent();
        String outroot = root.getAttribute("outputroot");

        vdt = System.getProperty("os.name").toLowerCase().contains("win")
                ? Paths.get(dir, root.getAttribute("vd-tool-win")).toAbsolutePath().toString()
                : Paths.get(dir, root.getAttribute("vd-tool-nix")).toAbsolutePath().toString();

        File f_lock = Paths.get(dir, root.getAttribute("lockfile")).toFile();

        lockdata = new HashMap<>();
        if (f_lock.exists())
        {
            for (String s : FileIO.readUTF8TextFileLines(f_lock.getAbsolutePath()))
            {
                String[] arr = s.split("\t");
                lockdata.put(Tuple2.Create(arr[0], arr[1]), arr[2]);
            }
        }

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

                run(filepath, outputNode, outputpath, fileNode.getAttribute("path"), outputNode.getAttribute("path"));
            }

            System.out.println();
        }

        System.out.println("[WRITING LOCK]");
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Tuple2<String, String>, String> e : lockdata_new.entrySet())
            b.append(e.getKey().Item1).append("\t").append(e.getKey().Item2).append("\t").append(e.getValue()).append("\n");
        writeTextFile(f_lock, b.toString());
        System.out.println("[FINISHED]");
    }

    private static void run(String filepath, Element outputNode, String outputpath, String rawinput, String rawoutput) throws Exception
    {
        if (filepath.endsWith(".svg"))
            runFromSVG(filepath, outputNode, outputpath, rawinput, rawoutput);
        else if (filepath.endsWith(".png") || outputpath.endsWith(".jpg") || outputpath.endsWith(".jpeg") || outputpath.endsWith(".bmp"))
            runFromRasterImage(filepath, outputNode, outputpath, rawinput, rawoutput);
        else
            throw new Exception("Unsupported file extension for " + filepath);
    }

    private static void runFromRasterImage(String filepath, Element outputNode, String outputpath, String rawinput, String rawoutput) throws Exception
    {
        outputpath = outputpath.replace("{filename}", FilenameUtils.getBaseName(filepath));
        outputpath = outputpath.replace("{originalwidth}",  "" + getWidthFromPNG(filepath));
        outputpath = outputpath.replace("{originalheight}", "" + getHeightFromPNG(filepath));

        String strww = outputNode.getAttribute("width");
        String strhh = outputNode.getAttribute("height");
        if (strww.equalsIgnoreCase("auto") && strhh.equalsIgnoreCase("auto")) {
            strww = "" + getWidthFromPNG(filepath);
            strhh = "" + getHeightFromPNG(filepath);
        } else if (strww.equalsIgnoreCase("auto")) {
            strww = "" + calcAutoWidthFromPNG(filepath, Integer.parseInt(strhh));
        } else if (strhh.equalsIgnoreCase("auto")) {
            strhh = "" + calcAutoHeightFromPNG(filepath, Integer.parseInt(strww));
        }
        int ww = Integer.parseInt(strww);
        int hh = Integer.parseInt(strhh);

        outputpath = outputpath.replace("{width}", ""+ww);
        outputpath = outputpath.replace("{height}", ""+hh);

        HashMap<String, String> args = new HashMap<>();
        args.put("width", Integer.toString(ww));
        args.put("height", Integer.toString(hh));

        if (outputpath.endsWith(".png"))
        {
            output(filepath, outputpath, args, Converter::convertRasterToPNG, rawinput, rawoutput);
        }
        else if (outputpath.endsWith(".jpg") || outputpath.endsWith(".jpeg"))
        {
            output(filepath, outputpath, args, Converter::convertRasterToJPEG, rawinput, rawoutput);
        }
        else
        {
            throw new Exception("Unsupported file extension for " + outputpath);
        }
    }

    private static void runFromSVG(String filepath, Element outputNode, String outputpath, String rawinput, String rawoutput) throws Exception
    {
        File f_in = new File(filepath);

        outputpath = outputpath.replace("{filename}", FilenameUtils.getBaseName(filepath));
        outputpath = outputpath.replace("{originalwidth}",  "" + getRoundedWidthFromSVG(f_in));
        outputpath = outputpath.replace("{originalheight}", "" + getRoundedHeightFromSVG(f_in));


        if (outputpath.endsWith(".png")) {
            String strww = outputNode.getAttribute("width");
            String strhh = outputNode.getAttribute("height");
            if (strww.equalsIgnoreCase("auto") && strhh.equalsIgnoreCase("auto")) {
                strww = "" + getRoundedWidthFromSVG(f_in);
                strhh = "" + getRoundedHeightFromSVG(f_in);
            } else if (strww.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoWidthFromSVG(f_in, Integer.parseInt(strhh));
            } else if (strhh.equalsIgnoreCase("auto")) {
                strhh = "" + calcAutoHeightFromSVG(f_in, Integer.parseInt(strww));
            }
            int ww = Integer.parseInt(strww);
            int hh = Integer.parseInt(strhh);

            outputpath = outputpath.replace("{width}", ""+ww);
            outputpath = outputpath.replace("{height}", ""+hh);

            HashMap<String, String> args = new HashMap<>();
            args.put("width", Integer.toString(ww));
            args.put("height", Integer.toString(hh));

            output(filepath, outputpath, args, Converter::convertSVGToPNG, rawinput, rawoutput);

        } else if (outputpath.endsWith(".xml")) {
            String strww = outputNode.getAttribute("vector_width");
            String strhh = outputNode.getAttribute("vector_height");
            if (strww.equalsIgnoreCase("auto") && strhh.equalsIgnoreCase("auto")) {
                strww = getRoundedWidthFromSVG(f_in) + "dp";
                strhh = getRoundedHeightFromSVG(f_in) + "dp";
            } else if (strww.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoWidthFromSVG(f_in, Integer.parseInt(un_dp(strhh))) + "dp";
            } else if (strhh.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoHeightFromSVG(f_in, Integer.parseInt(un_dp(strhh))) + "dp";
            }

            HashMap<String, String> args = new HashMap<>();
            args.put("width",  strww);
            args.put("height", strhh);

            output(filepath, outputpath, args, Converter::convertSVGToVector, rawinput, rawoutput);

        } else if (outputpath.endsWith(".pdf")) {

            output(filepath, outputpath, new HashMap<>(), Converter::convertSVGToPDF, rawinput, rawoutput);

        } else {

            throw new Exception("Unsupported file extension for " + outputpath);

        }
    }

    private static void output(String input, String output, HashMap<String, String> parameter, IConverter conv, String rawinput, String rawoutput) throws Exception
    {
        File f_in  = new File(input);
        File f_out = new File(output);

        if (!f_in.exists()) throw new Exception("File '" + f_in.getAbsolutePath() + "' does not exist");

        Tuple2<File, String> result = conv.run(f_in, parameter);

        if (lockcheck(input, output, parameter, rawinput, rawoutput))
        {
            result.Item1.delete();
            System.out.println("[ ] " + result.Item2 + "  --  Not needed");
            setLockdata(input, output, parameter, rawinput, rawoutput);
            return;
        }

        String xold = f_out.exists() ? cs(f_out) : "";
        String xnew = cs(result.Item1);

        if (xnew.length() < 8) {
            throw new Exception("Conversion resulted in empty file");
        } else if (xold.isEmpty()) {
            new File(output).delete();
            result.Item1.renameTo(new File(output));
            System.out.println("[#] " + result.Item2 + "  --  File created");
            setLockdata(input, output, parameter, rawinput, rawoutput);
        } else if (xold.equals(xnew)) {
            result.Item1.delete();
            System.out.println("[/] " + result.Item2 + "  --  No changes");
            setLockdata(input, output, parameter, rawinput, rawoutput);
        } else {
            new File(output).delete();
            result.Item1.renameTo(new File(output));
            System.out.println("[#] " + result.Item2 + "  --  File changed");
            setLockdata(input, output, parameter, rawinput, rawoutput);
        }
    }

    private static void setLockdata(String input, String output, HashMap<String, String> parameter, String rawinput, String rawoutput) throws Exception
    {
        String cs = calculatelockcheck(input, output, parameter);
        lockdata_new.put(Tuple2.Create(rawinput, rawoutput), cs);
    }

    private static boolean lockcheck(String input, String output, HashMap<String, String> parameter, String rawinput, String rawoutput) throws Exception
    {
        File f1 = new File(input);
        File f2 = new File(output);

        if (!f1.exists()) return false;
        if (!f2.exists()) return false;

        String dat = lockdata.get(Tuple2.Create(rawinput, rawoutput));
        if (dat == null) return false;

        String cs = calculatelockcheck(input, output, parameter);

        if (cs.equals(dat)) return true;

        return false;
    }

    private static String calculatelockcheck(String input, String output, HashMap<String, String> parameter) throws Exception
    {
        File f1 = new File(input);
        File f2 = new File(output);

        if (!f1.exists()) return "";
        if (!f2.exists()) return "";

        String cs1 = cs(f1);
        String cs2 = cs(f2);

        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> s : parameter.entrySet()) b.append(s.getKey()).append("\t").append(s.getValue()).append("\n");

        String cs3 = cs(b.toString());

        File f = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        BasicFileAttributes a = Files.readAttributes(f.toPath(), BasicFileAttributes.class);

        String fi = LOCK_VERSION;

        return cs(cs1 + cs2 + cs3 + fi); // [ INPUT, OUTPUT, PARAM, BINARY ]
    }

}
