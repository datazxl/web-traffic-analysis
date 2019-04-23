package com.zxl.parser.configuration.loader.impl;

import com.zxl.metadata.loader.ProfileConfigLoader;
import com.zxl.metadata.model.TargetPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 从metadata模块中的ProfileConfigLoader中加载和Profile相关的配置信息
 */
public class DefaultProfileConfigLoader implements com.zxl.parser.configuration.loader.ProfileConfigLoader {
    private Map<Integer, List<TargetPage>> profileId2TargetPages = new HashMap<>();

    /**
     * @param profileConfigLoader
     */
    public DefaultProfileConfigLoader(ProfileConfigLoader profileConfigLoader) {
        //1.加载所有的目标页面信息
        List<TargetPage> targetPages = profileConfigLoader.loadAllTargetPagesConfig();
        //2.将相同ProfileID的所有TargetPage组成Map
        targetPages.forEach(new Consumer<TargetPage>() {
            @Override
            public void accept(TargetPage targetPage) {
                List<TargetPage> targetPageList = profileId2TargetPages.getOrDefault(targetPage.getProfileId(), new ArrayList<TargetPage>());
                targetPageList.add(targetPage);
                profileId2TargetPages.put(targetPage.getProfileId(), targetPageList);
            }
        });
    }

    /**
     * 加载所有的目标页面配置信息
     * @return Map<Integer, List<TargetPage>> key是ProfileId，value是该ProfileId对应的所有目标页面配置数据
     */
    @Override
    public Map<Integer, List<TargetPage>> loadAllTargetPagesConfig() {
        return profileId2TargetPages;
    }
}