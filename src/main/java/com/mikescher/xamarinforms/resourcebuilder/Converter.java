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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import static com.mikescher.xamarinforms.resourcebuilder.FileIO.readUTF8TextFile;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.getHeightFromPNG;
import static com.mikescher.xamarinforms.resourcebuilder.PNGUtil.getWidthFromPNG;
import static com.mikescher.xamarinforms.resourcebuilder.SVGUtil.*;

@SuppressWarnings("DuplicatedCode")
class Converter
{
    static Tuple2<IConverter, IConverterTypeStr> RASTER_TO_PNG  = Tuple2.Create(Converter::convertRasterToPNG,  Converter::getTypeStrRasterToPNG);
    static Tuple2<IConverter, IConverterTypeStr> RASTER_TO_JPEG = Tuple2.Create(Converter::convertRasterToJPEG, Converter::getTypeStrRasterToJPEG);
    static Tuple2<IConverter, IConverterTypeStr> SVG_TO_PNG     = Tuple2.Create(Converter::convertSVGToPNG,     Converter::getTypeStrSVGToPNG);
    static Tuple2<IConverter, IConverterTypeStr> SVG_TO_PDF     = Tuple2.Create(Converter::convertSVGToPDF,     Converter::getTypeStrSVGToPDF);
    static Tuple2<IConverter, IConverterTypeStr> SVG_TO_VECTOR  = Tuple2.Create(Converter::convertSVGToVector,  Converter::getTypeStrSVGToVector);

    private static String getTypeStrRasterToPNG(HashMap<String, String> parameter)
    {
        int ww = Integer.parseInt(parameter.get("width"));
        int hh = Integer.parseInt(parameter.get("height"));
        return StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24);
    }

    private static File convertRasterToPNG(File input, HashMap<String, String> parameter) throws Exception
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

        return f_tmp;
    }

    private static String getTypeStrRasterToJPEG(HashMap<String, String> parameter)
    {
        int ww = Integer.parseInt(parameter.get("width"));
        int hh = Integer.parseInt(parameter.get("height"));
        return StringUtils.rightPad("JPEG @ "+ww+"x"+hh+"", 24);
    }

    private static File convertRasterToJPEG(File input, HashMap<String, String> parameter) throws Exception
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

        return f_tmp;
    }

    private static String getTypeStrSVGToPNG(HashMap<String, String> parameter)
    {
        int ww = Integer.parseInt(parameter.get("width"));
        int hh = Integer.parseInt(parameter.get("height"));
        return StringUtils.rightPad("PNG @ "+ww+"x"+hh+"", 24);
    }

    private static File convertSVGToPNG(File input, HashMap<String, String> parameter) throws Exception
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

        return f_tmp;
    }

    private static String getTypeStrSVGToVector(HashMap<String, String> parameter)
    {
        String ww = parameter.get("width");
        String hh = parameter.get("height");
        return StringUtils.rightPad("XML @ "+ww+"x"+hh+"", 24);
    }

    private static File convertSVGToVector(File input, HashMap<String, String> parameter) throws Exception
    {
        String ww = parameter.get("width");
        String hh = parameter.get("height");

        File f_tmp_dir = Paths.get(System.getProperty("java.io.tmpdir"), "xfrb_" + UUID.randomUUID()).toFile();
        if (!f_tmp_dir.mkdirs()) throw new Exception("Could not mkdir");
        f_tmp_dir.deleteOnExit();
        File f_tmp_file = Paths.get(f_tmp_dir.getAbsolutePath(), input.getName().replace(".svg", ".xml")).toFile();
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

        return f_tmp_file;
    }

    private static String getTypeStrSVGToPDF(HashMap<String, String> parameter)
    {
        return StringUtils.rightPad("PDF", 24);
    }

    private static File convertSVGToPDF(File input, HashMap<String, String> parameter) throws Exception
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

        return f_tmp;
    }
}
