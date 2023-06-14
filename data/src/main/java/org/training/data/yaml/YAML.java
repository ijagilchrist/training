package org.training.data.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;

public class YAML {

    private static ObjectMapper mapper;
    
    static {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(100*1024*1024);
        YAMLFactory yamlFactory = YAMLFactory.builder()
                                             .loaderOptions(loaderOptions)
                                             .build();
        mapper = new ObjectMapper(yamlFactory);
        mapper.setDateFormat(new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ"));
        YAML.mapper.findAndRegisterModules();
    }

    public static <T> T read(File file, Class<T> class_)throws IOException {
    
        return YAML.mapper.readValue(file, class_);
            
    }

    public static <T> List<T> readList(File file, Class<T> class_)throws IOException {

        CollectionType listType = YAML.mapper.getTypeFactory().constructCollectionType(ArrayList.class,class_);
        return mapper.readValue(file,listType);
           
    }

    public static void write(Object object, File file)throws IOException {
    
        ObjectWriter writer = mapper.writer();
        writer.writeValue(file,object);
            
    }
    
}
