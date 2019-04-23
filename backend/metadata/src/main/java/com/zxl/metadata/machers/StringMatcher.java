package com.zxl.metadata.machers;

import java.util.regex.Pattern;

/**
 * 字符串匹配器
 */
public class StringMatcher {

    //匹配类型
    private MatchType matchType;
    //匹配字符串
    private String matchPattern;

    public StringMatcher(MatchType matchType, String matchPattern) {
        this.matchType = matchType;
        this.matchPattern = matchPattern;
    }

    public boolean match(String str){
        if (matchType == MatchType.REGEX_MATCH){
            Pattern pattern = Pattern.compile(matchPattern);
            return pattern.matcher(str).find();
        } else if (matchType == MatchType.START_WITH) {
            return str.startsWith(matchPattern);
        } else if (matchType == MatchType.END_WITH){
            return str.endsWith(matchPattern);
        } else {
            return str.contains(matchPattern);
        }
    }
}
