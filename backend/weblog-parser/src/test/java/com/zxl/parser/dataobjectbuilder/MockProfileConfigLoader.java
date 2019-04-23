package com.zxl.parser.dataobjectbuilder;

import com.zxl.metadata.loader.ProfileConfigLoader;
import com.zxl.metadata.model.TargetPage;

import java.util.ArrayList;
import java.util.List;

public class MockProfileConfigLoader implements ProfileConfigLoader{

    @Override
    public List<TargetPage> loadAllTargetPagesConfig() {
        List<TargetPage> targetPages = new ArrayList<>();
        TargetPage targetPage = new TargetPage("1",702,"test target",null,"/checkoutLogin",
                "CONTAINS",true,true);
        targetPages.add(targetPage);
        return targetPages;
    }
}
