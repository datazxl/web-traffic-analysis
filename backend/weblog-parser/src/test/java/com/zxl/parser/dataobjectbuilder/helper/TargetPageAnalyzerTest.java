package com.zxl.parser.dataobjectbuilder.helper;

import com.zxl.metadata.loader.impl.MongoProfileConfigLoader;
import com.zxl.metadata.model.TargetPage;
import com.zxl.parser.configuration.loader.impl.DefaultProfileConfigLoader;
import com.zxl.parser.dataobjectbuilder.MockProfileConfigLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TargetPageAnalyzerTest {

    @Test
    public void getMatchedTargetPages() {
        TargetPageAnalyzer analyzer = new TargetPageAnalyzer(new DefaultProfileConfigLoader(new MockProfileConfigLoader()));
        List<TargetPage> matchedTargetPages = analyzer.getMatchedTargetPages(702, "http://temp.com/checkoutLogin");
        Assert.assertEquals(1, matchedTargetPages.size());
        Assert.assertEquals("1", matchedTargetPages.get(0).getId());
        Assert.assertEquals("test target", matchedTargetPages.get(0).getName());
    }
}