package com.zxl.parser.dataobjectbuilder.helper;

import com.zxl.metadata.model.TargetPage;
import com.zxl.parser.configuration.loader.ProfileConfigLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TargetPageAnalyzer {
    private Map<Integer, List<TargetPage>> profileId2TargetPages;

    public TargetPageAnalyzer(ProfileConfigLoader profileConfigLoader) {
        profileId2TargetPages = profileConfigLoader.loadAllTargetPagesConfig();
    }

    /**
     * pvurl和TargetPage匹配，返回匹配上的TargetPage(可能是多个)
     * @param profileId
     * @param pvUrl
     * @return
     */
    public List<TargetPage> getMatchedTargetPages(int profileId, String pvUrl){
        List<TargetPage> targetPages = profileId2TargetPages.getOrDefault(profileId, new ArrayList<TargetPage>());
        ArrayList<TargetPage> matchedTargetPages = new ArrayList<>();

        // 如果目标页面和来源url匹配的上，就将该目标页面TargetPage放入list
        for (TargetPage targetPage : targetPages){
            if (targetPage.match(pvUrl) == true) {
                matchedTargetPages.add(targetPage);
            }
        }
        return matchedTargetPages;
    }

}