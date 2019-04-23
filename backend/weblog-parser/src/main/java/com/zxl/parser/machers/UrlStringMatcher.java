package com.zxl.parser.machers;

/**
 * URL字符串匹配器
 */
public class UrlStringMatcher extends  StringMatcher{

    private boolean matchWithoutQueryString; //匹配时是否忽略url的query参数

    public UrlStringMatcher(MatchType matchType, String matchPattern, Boolean matchWithoutQueryString) {
        super(matchType,matchPattern);
        this.matchWithoutQueryString = matchWithoutQueryString;
    }

    @Override
    public boolean match(String url) {
        if (matchWithoutQueryString){
            int questionIndex = url.indexOf("?");
            if (questionIndex > 0){
               return super.match(url.substring(0,questionIndex));
            }
            return super.match(url);
        } else {
            return super.match(url);
        }
    }
}