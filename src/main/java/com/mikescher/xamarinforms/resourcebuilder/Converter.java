package com.mikescher.xamarinforms.resourcebuilder;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.fop.svg.PDFTranscoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import static com.mikescher.xamarinforms.resourcebuilder.FileIO.cs;
import static com.mikescher.xamarinforms.resourcebuilder.FileIO.readUTF8TextFile;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.getHeightFromPNG;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.getWidthFromPNG;
import static com.mikescher.xamarinforms.resourcebuilder.SVGUtil.*;
import static com.mikescher.xamarinforms.resourcebuilder.SVGUtil.un_dp;

public class Converter
{
    public static Tuple2<File, String> convertRasterToPNG(File input, HashMap<String, String> parameter) throws Exception
    {
        int ww = Integer.parseInt(parameter.get("width"));
        int hh = Integer.parseInt(parameter.get("height"));

        File f_tmp = File.createTempFile("xfrb_2_", ".png");
        f_tmp.deleteOnExit();

        BufferedImage img = ImageIO.read(input);

        BufferedImage scaledImage;
        if (ww == getWidthFromPNG(input) && hh == getHeightFromPNG(input)) {
            scaledImage = img;
        } else {
            ResampleOp resizeOp = new ResampleOp(ww, hh);
            resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
            scaledImage = resizeOp.filter(img, null);
        }

        ImageIO.write(scaledImage, "PNG", f_tmp);

        return Tuple2.Create(f_tmp, StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24));
    }

    public static Tuple2<File, String> convertRasterToJPEG(File input, HashMap<String, String> parameter) throws Exception
    {
        int ww = Integer.parseInt(parameter.get("width"));
        int hh = Integer.parseInt(parameter.get("height"));

        File f_tmp = File.createTempFile("xfrb_2_", ".png");
        f_tmp.deleteOnExit();

        BufferedImage img = ImageIO.read(input);

        BufferedImage scaledImage;
        if (ww == getWidthFromPNG(input) && hh == getHeightFromPNG(input)) {
            scaledImage = img;
        } else {
            ResampleOp resizeOp = new ResampleOp(ww, hh);
            resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
            scaledImage = resizeOp.filter(img, null);
        }

        ImageIO.write(scaledImage, "JPG", f_tmp);

        return Tuple2.Create(f_tmp, StringUtils.rightPad("JPEG @ "+ww+"x"+hh+"", 24));
    }

    public static Tuple2<File, String> convertSVGToPNG(File input, HashMap<String, String> parameter) throws Exception
    {
        int ww = Integer.parseInt(parameter.get("width"));
        int hh = Integer.parseInt(parameter.get("height"));

        File f_tmp = File.createTempFile("xfrb_2_", ".png");
        f_tmp.deleteOnExit();

        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) ww);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) hh);
        TranscoderInput tcinput = new TranscoderInput(new FileInputStream(input));
        OutputStream ostream = new FileOutputStream(f_tmp);
        TranscoderOutput tcoutput = new TranscoderOutput(ostream);
        t.transcode(tcinput, tcoutput);
        ostream.flush();

        return Tuple2.Create(f_tmp, StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24));
    }

    public static Tuple2<File, String> convertSVGToVector(File input, HashMap<String, String> parameter) throws Exception
    {
        String ww = parameter.get("width");
        String hh = parameter.get("height");

        File f_in = input;
        File f_tmp_dir = Paths.get(System.getProperty("java.io.tmpdir"), "xfrb_" + UUID.randomUUID()).toFile();
        f_tmp_dir.mkdirs();
        f_tmp_dir.deleteOnExit();
        File f_tmp_file = Paths.get(f_tmp_dir.getAbsolutePath(), f_in.getName().replace(".svg", ".xml")).toFile();
        f_tmp_file.deleteOnExit();

        Tuple3<Integer, String, String> r;
        if (Double.parseDouble(un_dp(ww)) ==  getFloatWidthFromSVG(input) && Double.parseDouble(un_dp(hh)) == getFloatHeightFromSVG(input)) {
            r = ProcessHelper.procExec(Main.vdt, "-in", input.getAbsolutePath(), "-out", f_tmp_file.getParent(), "-c");
        } else {
            r = ProcessHelper.procExec(Main.vdt, "-in", input.getAbsolutePath(), "-out", f_tmp_file.getParent(), "-c", "-widthDp", un_dp(ww), "-heightDp", un_dp(hh));
        }

        if (r.Item1 != 0)  throw new Exception("vd-tool failed: \n\n" + r.Item1 + "\n\n" + r.Item2 + "\n\n" + r.Item3);

        String xnew = readUTF8TextFile(f_tmp_file);
        if (!xnew.contains("<vector")) throw new Exception("vd-tool resulted in invalid output:\n" + xnew);

        return Tuple2.Create(f_tmp_file, StringUtils.rightPad("XML @ "+ww+"x"+hh+"", 24));
    }

    public static Tuple2<File, String> convertSVGToPDF(File input, HashMap<String, String> parameter) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_3_", ".pdf");
        f_tmp.deleteOnExit();

        PDFTranscoder t = new PDFTranscoder();
        TranscoderInput tcinput = new TranscoderInput(new FileInputStream(input));
        OutputStream ostream = new FileOutputStream(f_tmp);
        TranscoderOutput tcoutput = new TranscoderOutput(ostream);
        t.transcode(tcinput, tcoutput);
        ostream.flush();
        ostream.close();

        return Tuple2.Create(f_tmp, StringUtils.rightPad("PDF", 24));
    }
}
