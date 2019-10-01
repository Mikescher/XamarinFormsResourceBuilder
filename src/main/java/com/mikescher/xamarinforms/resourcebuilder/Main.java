package com.mikescher.xamarinforms.resourcebuilder;

import com.mikescher.xamarinforms.resourcebuilder.converter.*;
import com.mikescher.xamarinforms.resourcebuilder.env.RunEnvironment;
import com.mikescher.xamarinforms.resourcebuilder.util.FileIO;
import com.mikescher.xamarinforms.resourcebuilder.util.ThreadUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

@SuppressWarnings("DuplicatedCode")
public class Main {
    public static final String LOCK_VERSION = "1.2.0.0";

    public static void main(String[] args) throws Exception
    {
        RunEnvironment env = new RunEnvironment();
        env.Start();

        if (args.length < 1)
        {
            System.out.println("Not enough parameter");
            return;
        }

        File fXmlFile = new File(args[0]);
        Element root = getXMLRoot(fXmlFile);

        env.Init(fXmlFile.getAbsoluteFile().getParent(), root);

        NodeList fileNodes = root.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++)
        {
            Element fileNode = (Element)fileNodes.item(i);

            String sourcepath = env.getPathInRunDirectory(fileNode.getAttribute("path"));
            System.out.println("[CONVERT] " + new File(sourcepath).getName());

            NodeList outputNodes = fileNode.getElementsByTagName("output");
            for (int j = 0; j < outputNodes.getLength(); j++)
            {
                Element outputNode = (Element)outputNodes.item(j);
                String outputpath = env.getPathInOutputDirectory(outputNode.getAttribute("path"));

                getConverter(sourcepath, outputpath).run(env, sourcepath, fileNode.getAttribute("path"), outputNode);
            }

            System.out.println();
            if (env.SleepTime>0) ThreadUtils.safeSleep(env.SleepTime);
        }

        NodeList wildcardNodes = root.getElementsByTagName("wildcard");
        for (int i = 0; i < wildcardNodes.getLength(); i++)
        {
            Element wildcardNode = (Element)wildcardNodes.item(i);
            for (String realfilepath : FileIO.listWildcardFiles(env, wildcardNode.getAttribute("path")))
            {
                System.out.println("[CONVERT] " + new File(realfilepath).getName());

                NodeList outputNodes = wildcardNode.getElementsByTagName("output");
                for (int j = 0; j < outputNodes.getLength(); j++)
                {
                    Element outputNode = (Element)outputNodes.item(j);
                    String outputpath  = env.getPathInOutputDirectory(outputNode.getAttribute("path"));

                    getConverter(realfilepath, outputpath).run(env, realfilepath, wildcardNode.getAttribute("path"), outputNode);
                }

                System.out.println();
                if (env.SleepTime>0) ThreadUtils.safeSleep(env.SleepTime);
            }
        }

        env.writeLockFile();

        System.out.println("[FINISHED]");

        env.writeResult();
    }

    private static AbstractConverter getConverter(String sourcePath, String destPath) throws Exception
    {
        sourcePath = sourcePath.toLowerCase();
        destPath   = destPath.toLowerCase();

        if (sourcePath.endsWith(".png")  && destPath.endsWith(".png")) return new RasterToPNGConverter();
        if (sourcePath.endsWith(".bmp")  && destPath.endsWith(".png")) return new RasterToPNGConverter();
        if (sourcePath.endsWith(".jpg")  && destPath.endsWith(".png")) return new RasterToPNGConverter();
        if (sourcePath.endsWith(".jpeg") && destPath.endsWith(".png")) return new RasterToPNGConverter();

        if (sourcePath.endsWith(".png")  && destPath.endsWith(".jpg")) return new RasterToJPEGConverter();
        if (sourcePath.endsWith(".bmp")  && destPath.endsWith(".jpg")) return new RasterToJPEGConverter();
        if (sourcePath.endsWith(".jpg")  && destPath.endsWith(".jpg")) return new RasterToJPEGConverter();
        if (sourcePath.endsWith(".jpeg") && destPath.endsWith(".jpg")) return new RasterToJPEGConverter();

        if (sourcePath.endsWith(".png")  && destPath.endsWith(".jpeg")) return new RasterToJPEGConverter();
        if (sourcePath.endsWith(".bmp")  && destPath.endsWith(".jpeg")) return new RasterToJPEGConverter();
        if (sourcePath.endsWith(".jpg")  && destPath.endsWith(".jpeg")) return new RasterToJPEGConverter();
        if (sourcePath.endsWith(".jpeg") && destPath.endsWith(".jpeg")) return new RasterToJPEGConverter();

        if (sourcePath.endsWith(".png")  && destPath.endsWith(".bmp"))  return new RasterToBMPConverter();
        if (sourcePath.endsWith(".bmp")  && destPath.endsWith(".bmp"))  return new RasterToBMPConverter();
        if (sourcePath.endsWith(".jpg")  && destPath.endsWith(".bmp"))  return new RasterToBMPConverter();
        if (sourcePath.endsWith(".jpeg") && destPath.endsWith(".bmp"))  return new RasterToBMPConverter();

        if (sourcePath.endsWith(".svg")  && destPath.endsWith(".xml"))  return new VectorToAndroidConverter();

        if (sourcePath.endsWith(".svg")  && destPath.endsWith(".pdf"))  return new VectorToPDFConverter();

        if (sourcePath.endsWith(".svg")  && destPath.endsWith(".png"))  return new VectorToPNGConverter();

        if (sourcePath.endsWith(".svg")  && destPath.endsWith(".jpg"))  return new VectorToJPEGConverter();
        if (sourcePath.endsWith(".svg")  && destPath.endsWith(".jpeg")) return new VectorToJPEGConverter();

        if (sourcePath.endsWith(".svg")  && destPath.endsWith(".bmp"))  return new VectorToBMPConverter();

        throw new Exception("Cannot convert from ["+FileIO.getFileExtension(sourcePath)+"] to ["+FileIO.getFileExtension(destPath)+"]");
    }

    private static Element getXMLRoot(File fXmlFile) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        return doc.getDocumentElement();
    }
}
