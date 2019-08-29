package com.mikescher.xamarinforms.resourcebuilder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mikescher.xamarinforms.resourcebuilder.FileIO.cs;
import static com.mikescher.xamarinforms.resourcebuilder.FileIO.writeTextFile;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.*;
import static com.mikescher.xamarinforms.resourcebuilder.SVGUtil.*;

@SuppressWarnings("DuplicatedCode")
public class Main {
    private static final String LOCK_VERSION = "1.1.0.0";

    static String vdt;

    private static HashMap<Tuple2<String, String>, String> lockdata;
    private static HashMap<Tuple2<String, String>, String> lockdata_new = new HashMap<>();

    private static int count_NotNeeded = 0;
    private static int count_Changed   = 0;
    private static int count_Created   = 0;
    private static int count_NoChanges = 0;

    public static void main(String[] args) throws Exception
    {
        long stt = System.currentTimeMillis();

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

        File f_lock = root.hasAttribute("lockfile") ? Paths.get(dir, root.getAttribute("lockfile")).toFile() : null;

        lockdata = new HashMap<>();
        if (f_lock != null && f_lock.exists())
        {
            for (String s : FileIO.readUTF8TextFileLines(f_lock.getAbsolutePath()))
            {
                String[] arr = s.split("\t");
                lockdata.put(Tuple2.Create(arr[0].trim(), arr[1].trim()), arr[2].trim());
            }
        }

        NodeList fileNodes = root.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++)
        {
            Element fileNode = (Element)fileNodes.item(i);

            String filepath = Paths.get(dir, fileNode.getAttribute("path")).toAbsolutePath().toString();

            System.out.println("[CONVERT] " + new File(filepath).getName());

            NodeList outputNodes = fileNode.getElementsByTagName("output");
            for (int j = 0; j < outputNodes.getLength(); j++)
            {
                Element outputNode = (Element)outputNodes.item(j);
                String outputpath = Paths.get(dir, outroot, outputNode.getAttribute("path")).toAbsolutePath().toString() ;

                run(filepath, outputNode, outputpath, fileNode.getAttribute("path"), outputNode.getAttribute("path"));
            }

            System.out.println();
            ThreadUtils.safeSleep(150);
        }

        NodeList wildcardNodes = root.getElementsByTagName("wildcard");
        for (int i = 0; i < wildcardNodes.getLength(); i++)
        {
            Element wildcardNode = (Element)wildcardNodes.item(i);

            String wildpath = wildcardNode.getAttribute("path");
            if (!wildpath.startsWith("**/")) wildpath = "**/" + wildpath;

            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setIncludes(new String[]{ wildpath });
            scanner.setBasedir(dir);
            scanner.setCaseSensitive(false);
            scanner.scan();
            String[] files = scanner.getIncludedFiles();

            for (String realfilename : files)
            {
                String realfilepath = Paths.get(dir, realfilename).toAbsolutePath().toString();

                System.out.println("[CONVERT] " + new File(realfilepath).getName());

                NodeList outputNodes = wildcardNode.getElementsByTagName("output");
                for (int j = 0; j < outputNodes.getLength(); j++)
                {
                    Element outputNode = (Element)outputNodes.item(j);
                    String outputpath = Paths.get(dir, outroot, outputNode.getAttribute("path")).toAbsolutePath().toString() ;

                    run(realfilepath, outputNode, outputpath, wildcardNode.getAttribute("path"), outputNode.getAttribute("path"));
                }

                System.out.println();
                ThreadUtils.safeSleep(50);
            }
        }

        System.out.println("[WRITING LOCK]");
        {
            int p1 = 3 + lockdata_new.keySet().stream().map(s -> s.Item1.length()).max(Integer::compareTo).orElse(0);
            int p2 = 3 + lockdata_new.keySet().stream().map(s -> s.Item2.length()).max(Integer::compareTo).orElse(0);
            StringBuilder lockdatabuilder = new StringBuilder();
            for (Map.Entry<Tuple2<String, String>, String> e : lockdata_new.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().Item1)).collect(Collectors.toList()))
                lockdatabuilder
                        .append(StringUtils.rightPad(e.getKey().Item1, p1))
                        .append("\t")
                        .append(StringUtils.rightPad(e.getKey().Item2, p2))
                        .append("\t")
                        .append(e.getValue()).append("\n");
            if (f_lock != null) writeTextFile(f_lock, lockdatabuilder.toString());
        }
        System.out.println("[FINISHED]");

        System.out.println();
        System.out.println("[RESULT]");
        System.out.println();

        System.out.println("Update not needed (by lockfile): " + count_NotNeeded);
        System.out.println("File changed:                    " + count_Changed);
        System.out.println("New file created (first run):    " + count_Created);
        System.out.println("File unchanged (checksum match): " + count_NoChanges);
        System.out.println();
        System.out.println("Duration: " + ((System.currentTimeMillis() - stt)/1000) + " sec");
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
            output(filepath, outputpath, args, Converter.RASTER_TO_PNG, rawinput, rawoutput);
        }
        else if (outputpath.endsWith(".jpg") || outputpath.endsWith(".jpeg"))
        {
            output(filepath, outputpath, args, Converter.RASTER_TO_JPEG, rawinput, rawoutput);
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

            output(filepath, outputpath, args, Converter.SVG_TO_PNG, rawinput, rawoutput);

        } else if (outputpath.endsWith(".xml")) {
            String strww = outputNode.getAttribute("vector_width");
            String strhh = outputNode.getAttribute("vector_height");
            if (strww.equalsIgnoreCase("auto") && strhh.equalsIgnoreCase("auto")) {
                strww = getRoundedWidthFromSVG(f_in) + "dp";
                strhh = getRoundedHeightFromSVG(f_in) + "dp";
            } else if (strww.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoWidthFromSVG(f_in, Integer.parseInt(un_dp(strhh))) + "dp";
            } else if (strhh.equalsIgnoreCase("auto")) {
                strhh = "" + calcAutoHeightFromSVG(f_in, Integer.parseInt(un_dp(strww))) + "dp";
            }

            HashMap<String, String> args = new HashMap<>();
            args.put("width",  strww);
            args.put("height", strhh);

            output(filepath, outputpath, args, Converter.SVG_TO_VECTOR, rawinput, rawoutput);

        } else if (outputpath.endsWith(".pdf")) {

            output(filepath, outputpath, new HashMap<>(), Converter.SVG_TO_PDF, rawinput, rawoutput);

        } else {

            throw new Exception("Unsupported file extension for " + outputpath);

        }
    }

    private static void output(String input, String output, HashMap<String, String> parameter, Tuple2<IConverter, IConverterTypeStr> conv, String rawinput, String rawoutput) throws Exception
    {
        File f_in  = new File(input);
        File f_out = new File(output);

        if (!f_in.exists()) throw new Exception("File '" + f_in.getAbsolutePath() + "' does not exist");

        String resultName = conv.Item2.run(parameter);

        if (lockcheck(input, output, parameter, rawinput, rawoutput))
        {
            System.out.println("[ ] " + resultName + "  --  Not needed");
            setLockdata(input, output, parameter, rawinput, rawoutput);
            count_NotNeeded++;
            return;
        }

        File resultFile = conv.Item1.run(f_in, parameter);

        String xold = f_out.exists() ? cs(f_out) : "";
        String xnew = cs(resultFile);

        if (xnew.length() < 8) {
            throw new Exception("Conversion resulted in empty file");
        } else if (xold.isEmpty()) {
            if (new File(output).exists() && !new File(output).delete()) throw new Exception("File delete failed");
            if (!resultFile.renameTo(new File(output))) throw new Exception("File renameTo failed");
            System.out.println("[#] " + resultName + "  --  File created");
            setLockdata(input, output, parameter, rawinput, rawoutput);
            count_Created++;
        } else if (xold.equals(xnew)) {
            if (!resultFile.delete()) throw new Exception("File delete failed");
            System.out.println("[/] " + resultName + "  --  No changes");
            setLockdata(input, output, parameter, rawinput, rawoutput);
            count_NoChanges++;
        } else {
            if (!new File(output).delete()) throw new Exception("File delete failed");
            if (! resultFile.renameTo(new File(output))) throw new Exception("File renameTo failed");
            System.out.println("[#] " + resultName + "  --  File changed");
            setLockdata(input, output, parameter, rawinput, rawoutput);
            count_Changed++;
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

        return cs.equals(dat);
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

        return cs(cs1 + cs2 + cs3 + LOCK_VERSION); // [ INPUT, OUTPUT, PARAM, BINARY ]
    }

}
