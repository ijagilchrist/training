package org.training.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class JSON {

    private static ObjectMapper mapper;
    
    static {
        mapper = new ObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ"));
        JSON.mapper.findAndRegisterModules();
    }

    public static <T> T read(File file, Class<T> class_)throws IOException {
    
        return JSON.mapper.readValue(file, class_);
            
    }

    public static <T> List<T> readList(File file, Class<T> class_)throws IOException {

        CollectionType listType = JSON.mapper.getTypeFactory().constructCollectionType(ArrayList.class,class_);
        return mapper.readValue(file,listType);
           
    }

    public static void write(Object object, File file)throws IOException {
    
        ObjectWriter writer = mapper.writer();
        writer.writeValue(file,object);
            
    }
    
}
