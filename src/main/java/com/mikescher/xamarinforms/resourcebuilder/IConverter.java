package com.mikescher.xamarinforms.resourcebuilder;

import java.io.File;
import java.util.HashMap;

public interface IConverter {
    Tuple2<File, String> run(File input, HashMap<String, String> parameter) throws Exception;
}
