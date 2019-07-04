package com.mikescher.xamarinforms.resourcebuilder;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.fop.svg.PDFTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.mikescher.xamarinforms.resourcebuilder.FileIO.*;
import static com.mikescher.xamarinforms.resourcebuilder.SVGUtil.*;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.*;

public class Main {
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

        String dir = fXmlFile.getAbsoluteFile().getParent();
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

                run(filepath, outputNode, outputpath);
            }

            System.out.println();
        }
    }

    private static void run(String filepath, Element outputNode, String outputpath) throws Exception
    {

        if (filepath.endsWith(".svg"))
            runFromSVG(filepath, outputNode, outputpath);
        else if (filepath.endsWith(".png") || outputpath.endsWith(".jpg") || outputpath.endsWith(".jpeg") || outputpath.endsWith(".bmp"))
            runFromImage(filepath, outputNode, outputpath);
        else
            throw new Exception("Unsupported file extension for " + filepath);

    }

    private static void runFromImage(String filepath, Element outputNode, String outputpath) throws Exception
    {
        outputpath = outputpath.replace("{filename}", new File(outputpath).getName());
        outputpath = outputpath.replace("{originalwidth}",  "" + getWidthFromPNG(filepath));
        outputpath = outputpath.replace("{originalheight}", "" + getHeightFromPNG(filepath));

        if (outputpath.endsWith(".png")) {
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

            outputRasterImageFromImage(filepath, outputpath, ww, hh);

        } else if (outputpath.endsWith(".jpg") || outputpath.endsWith(".jpeg")) {
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

            outputJPEGFromImage(filepath, outputpath, ww, hh);

        } else {

            throw new Exception("Unsupported file extension for " + outputpath);

        }
    }

    private static void runFromSVG(String filepath, Element outputNode, String outputpath) throws Exception {
        outputpath = outputpath.replace("{filename}", new File(outputpath).getName());
        outputpath = outputpath.replace("{originalwidth}",  "" + getRoundedWidthFromSVG(filepath));
        outputpath = outputpath.replace("{originalheight}", "" + getRoundedHeightFromSVG(filepath));

        if (outputpath.endsWith(".png")) {
            String strww = outputNode.getAttribute("width");
            String strhh = outputNode.getAttribute("height");
            if (strww.equalsIgnoreCase("auto") && strhh.equalsIgnoreCase("auto")) {
                strww = "" + getRoundedWidthFromSVG(filepath);
                strhh = "" + getRoundedHeightFromSVG(filepath);
            } else if (strww.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoWidthFromSVG(filepath, Integer.parseInt(strhh));
            } else if (strhh.equalsIgnoreCase("auto")) {
                strhh = "" + calcAutoHeightFromSVG(filepath, Integer.parseInt(strww));
            }
            int ww = Integer.parseInt(strww);
            int hh = Integer.parseInt(strhh);

            outputpath = outputpath.replace("{width}", ""+ww);
            outputpath = outputpath.replace("{height}", ""+hh);

            outputRasterImageFromSVG(filepath, outputpath, ww, hh);

        } else if (outputpath.endsWith(".xml")) {
            String strww = outputNode.getAttribute("vector_width");
            String strhh = outputNode.getAttribute("vector_height");
            if (strww.equalsIgnoreCase("auto") && strhh.equalsIgnoreCase("auto")) {
                strww = getRoundedWidthFromSVG(filepath) + "dp";
                strhh = getRoundedHeightFromSVG(filepath) + "dp";
            } else if (strww.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoWidthFromSVG(filepath, Integer.parseInt(un_dp(strhh))) + "dp";
            } else if (strhh.equalsIgnoreCase("auto")) {
                strww = "" + calcAutoHeightFromSVG(filepath, Integer.parseInt(un_dp(strhh))) + "dp";
            }
            outputAndroidVectorFromSVG(filepath, outputpath, strww, strhh);

        } else if (outputpath.endsWith(".pdf")) {

            outputVectorPDFFromSVG(filepath, outputpath);

        } else {

            throw new Exception("Unsupported file extension for " + outputpath);

        }
    }

    private static void outputRasterImageFromImage(String input, String output, int ww, int hh) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_2_", ".png");
        f_tmp.deleteOnExit();
        File f_out = Paths.get(output).toFile();

        BufferedImage img = ImageIO.read(new File(input));

        BufferedImage scaledImage;
        if (ww == getWidthFromPNG(input) && hh == getHeightFromPNG(input)) {
            scaledImage = img;
        } else {
            ResampleOp resizeOp = new ResampleOp(ww, hh);
            resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
            scaledImage = resizeOp.filter(img, null);
        }

        ImageIO.write(scaledImage, "PNG", f_tmp);

        String xold = f_out.exists() ? cs(f_out) : "";
        String xnew = cs(f_tmp);

        if (xnew.isEmpty()) {
            throw new Exception("Conversion resulted in empty file");
        } else if (xold.isEmpty()) {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  File created");
        } else if (xold.equals(xnew)) {
            f_tmp.delete();
            System.out.println("[ ] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  No changes");
        } else {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  File changed");
        }
    }

    private static void outputJPEGFromImage(String input, String output, int ww, int hh) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_2_", ".jpeg");
        f_tmp.deleteOnExit();
        File f_out = Paths.get(output).toFile();

        BufferedImage img = ImageIO.read(new File(input));

        BufferedImage scaledImage;
        if (ww == getWidthFromPNG(input) && hh == getHeightFromPNG(input)) {
            scaledImage = img;
        } else {
            ResampleOp resizeOp = new ResampleOp(ww, hh);
            resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
            scaledImage = resizeOp.filter(img, null);
        }

        ImageIO.write(scaledImage, "JPG", f_tmp);

        String xold = f_out.exists() ? cs(f_out) : "";
        String xnew = cs(f_tmp);

        if (xnew.isEmpty()) {
            throw new Exception("Conversion resulted in empty file");
        } else if (xold.isEmpty()) {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("JPEG @ "+ww+"x"+hh+"", 24) + "  --  File created");
        } else if (xold.equals(xnew)) {
            f_tmp.delete();
            System.out.println("[ ] " + StringUtils.rightPad("JPEG @ "+ww+"x"+hh+"", 24) + "  --  No changes");
        } else {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("JPEG @ "+ww+"x"+hh+"", 24) + "  --  File changed");
        }
    }

    private static void outputRasterImageFromSVG(String input, String output, int ww, int hh) throws Exception
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

        if (xnew.isEmpty()) {
            throw new Exception("Conversion resulted in empty file");
        } else if (xold.isEmpty()) {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  File created");
        } else if (xold.equals(xnew)) {
            f_tmp.delete();
            System.out.println("[ ] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  No changes");
        } else {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24) + "  --  File changed");
        }
    }

    private static void outputAndroidVectorFromSVG(String input, String output, String ww, String hh) throws Exception
    {
        File f_in = new File(input);
        File f_out = Paths.get(output).toFile();
        File f_tmp_dir = Paths.get(System.getProperty("java.io.tmpdir"), "xfrb_" + UUID.randomUUID()).toFile();
        f_tmp_dir.mkdirs();
        f_tmp_dir.deleteOnExit();
        File f_tmp_file = Paths.get(f_tmp_dir.getAbsolutePath(), f_in.getName().replace(".svg", ".xml")).toFile();
        f_tmp_file.deleteOnExit();

        Tuple3<Integer, String, String> r = ProcessHelper.procExec(vdt, "-in", input, "-out", f_tmp_file.getParent(), "-c", "-widthDp", un_dp(ww), "-heightDp", un_dp(hh));

        if (r.Item1 != 0)  throw new Exception("vd-tool failed: \n\n" + r.Item1 + "\n\n" + r.Item2 + "\n\n" + r.Item3);

        String xold = f_out.exists() ? readUTF8TextFile(f_out) : "";
        String xnew = readUTF8TextFile(f_tmp_file);

        if (!xnew.contains("<vector")) throw new Exception("vd-tool resulted in invalid output:\n" + xnew);

        if (xnew.isEmpty()) {
            throw new Exception("Conversion resulted in empty file");
        } else if (xold.isEmpty()) {
            f_out.delete();
            f_tmp_file.renameTo(f_out);
            f_tmp_dir.delete();
            System.out.println("[!] " + StringUtils.rightPad("XML @ ("+ww+")x("+hh+")", 24) + "  --  File created");
        } else if (xold.equals(xnew)) {
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

    private static void outputVectorPDFFromSVG(String input, String output) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_3_", ".pdf");
        f_tmp.deleteOnExit();
        File f_out = Paths.get(output).toFile();
        File f_in = new File(input);

        if (f_out.exists() && f_out.lastModified() >= f_in.lastModified()) return;

        PDFTranscoder t = new PDFTranscoder();
        TranscoderInput tcinput = new TranscoderInput(new FileInputStream(input));
        OutputStream ostream = new FileOutputStream(f_tmp);
        TranscoderOutput tcoutput = new TranscoderOutput(ostream);
        t.transcode(tcinput, tcoutput);
        ostream.flush();
        ostream.close();

        if (f_out.exists()) {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PDF", 24) + "  --  File changed");
        } else {
            new File(output).delete();
            f_tmp.renameTo(new File(output));
            System.out.println("[!] " + StringUtils.rightPad("PDF", 24) + "  --  File created");
        }
    }
}
