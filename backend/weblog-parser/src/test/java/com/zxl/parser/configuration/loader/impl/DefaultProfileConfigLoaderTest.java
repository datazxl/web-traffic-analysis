package com.zxl.parser.configuration.loader.impl;

import com.zxl.metadata.loader.impl.MongoProfileConfigLoader;
import com.zxl.metadata.model.TargetPage;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultProfileConfigLoaderTest {

    @Test
    public void loadAllTargetPagesConfig() {
        DefaultProfileConfigLoader loader = new DefaultProfileConfigLoader(new MongoProfileConfigLoader());
        Map<Integer, List<TargetPage>> map = loader.loadAllTargetPagesConfig();
        for (Map.Entry e : map.entrySet()){
            System.out.println(e.getKey() +" " + e.getValue());
        }
    }
}