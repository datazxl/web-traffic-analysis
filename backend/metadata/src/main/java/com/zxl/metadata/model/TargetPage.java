package com.zxl.metadata.model;

import com.zxl.metadata.machers.MatchType;
import com.zxl.metadata.machers.StringMatcher;

/**
 * 目标页面配置实体类
 */
public class TargetPage {
    private String id; //id
    private int profileId; //这个目标页面配置所属的profile
    private String name; //目标页面配置名称
    private String description; //描述
    private String matchPattern; //匹配URL字符串
    private String matchType; //匹配类型
    private boolean matchWithoutQueryString; //匹配时是否忽略url的query参数
    private boolean isEnable; //配置是否开启(有效)
    private StringMatcher matcher; //用于匹配url是否符合当前目标页面TargetPage

    public TargetPage(String id, int profileId, String name, String description, String matchPattern, String matchType, boolean matchWithoutQueryString, boolean isEnable) {
        this.id = id;
        this.profileId = profileId;
        this.name = name;
        this.description = description;
        this.matchPattern = matchPattern;
        this.matchType = matchType;
        this.matchWithoutQueryString = matchWithoutQueryString;
        this.isEnable = isEnable;
        this.matcher = new StringMatcher(MatchType.valueOf(matchType),matchPattern);
    }

    public String getId() {
        return id;
    }

    public int getProfileId() {
        return profileId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMatchPattern() {
        return matchPattern;
    }

    public String getMatchType() {
        return matchType;
    }

    public boolean isMatchWithoutQueryString() {
        return matchWithoutQueryString;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public StringMatcher getMatcher() {
        return matcher;
    }

    /**
     * 根据url，判断是否是目标页面
     * @param url
     * @return
     */
    public boolean match(String url) {
        if (matchWithoutQueryString){
            int questionIndex = url.indexOf("?");
            if (questionIndex > 0){
                return matcher.match(url.substring(0,questionIndex));
            }
            return matcher.match(url);
        } else {
            return matcher.match(url);
        }
    }
}
