package com.mikescher.xamarinforms.resourcebuilder.converter;

import com.mikescher.xamarinforms.resourcebuilder.env.RunEnvironment;
import com.mikescher.xamarinforms.resourcebuilder.util.SVGUtil;
import com.mikescher.xamarinforms.resourcebuilder.values.FittingType;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class VectorToPDFConverter extends AbstractConverter
{
    @Override
    protected double getInputWidthDouble(File file) throws Exception {
        return SVGUtil.getFloatWidthFromSVG(file);
    }

    @Override
    protected double getInputHeightDouble(File file) throws Exception {
        return SVGUtil.getFloatHeightFromSVG(file);
    }

    @Override
    protected void setMargin(int value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected void setFitting(FittingType value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected void setBackgroundColor(Color value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected void setHeight(String value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected void setWidth(String value) throws Exception {
        throw new Exception("This attribute is not allowed here");
    }

    @Override
    protected String getDisplayString() {
        return "PDF";
    }

    @Override
    protected File convert(RunEnvironment env) throws Exception
    {
        File f_tmp = File.createTempFile("xfrb_pdf_", ".pdf");
        f_tmp.deleteOnExit();

        PDFTranscoder t = new PDFTranscoder();
        TranscoderInput tcinput = new TranscoderInput(new FileInputStream(InputFile));
        OutputStream ostream = new FileOutputStream(f_tmp);
        TranscoderOutput tcoutput = new TranscoderOutput(ostream);
        t.transcode(tcinput, tcoutput);
        ostream.flush();
        ostream.close();

        return f_tmp;
    }
}
