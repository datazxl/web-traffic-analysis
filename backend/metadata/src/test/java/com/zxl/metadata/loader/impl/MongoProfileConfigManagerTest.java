package com.zxl.metadata.loader.impl;

import com.zxl.metadata.model.TargetPage;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class MongoProfileConfigManagerTest {

    @Test
    public void loadAllTargetPagesConfig() {
        List<TargetPage> targetPages = new MongoProfileConfigLoader().loadAllTargetPagesConfig();

        List<TargetPage> specTargetPages = targetPages.stream().filter(targetPage -> targetPage.getId().equals("57f8df278a4bf51e2d834be9")
        ).collect(Collectors.toList());

        Assert.assertEquals(2, targetPages.size());
        TargetPage targetPage = specTargetPages.get(0);
        Assert.assertEquals("57f8df278a4bf51e2d834be9", targetPage.getId());
        Assert.assertEquals("test target", targetPage.getName());
        Assert.assertEquals("CONTAINS", targetPage.getMatchType());
        Assert.assertEquals(702, targetPage.getProfileId());
        Assert.assertEquals(true, targetPage.isEnable());
        Assert.assertEquals(false, targetPage.isMatchWithoutQueryString());
    }
}