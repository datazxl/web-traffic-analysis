package com.zxl.parser.dataobjectbuilder;

import com.zxl.metadata.model.TargetPage;
import com.zxl.parser.dataobject.BaseDataObject;
import com.zxl.parser.dataobject.PvDataObject;
import com.zxl.parser.dataobject.TargetPageDataObject;
import com.zxl.parser.dataobject.dim.*;
import com.zxl.parser.dataobjectbuilder.helper.SearchEngineNameUtil;
import com.zxl.parser.dataobjectbuilder.helper.TargetPageAnalyzer;
import com.zxl.parser.utils.ColumnReader;
import com.zxl.parser.utils.ParseUtils;
import com.zxl.parser.utils.UrlInfo;
import com.zxl.parser.utils.UrlParseUtils;
import com.zxl.preparser.PreParsedLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zxl.parser.utils.ParseUtils.isNullOrEmptyOrDash;
import static com.zxl.parser.utils.UrlParseUtils.getInfoFromUrl;

public class PvDataObjectBuilder extends AbstractDataObjectBuilder{

    private TargetPageAnalyzer targetPageAnalyzer;
    public PvDataObjectBuilder(TargetPageAnalyzer targetPageAnalyzer) {
        this.targetPageAnalyzer = targetPageAnalyzer;
    }

    @Override
    public String getCommand() {
        return "pv";
    }

    @Override
    public List<BaseDataObject> doBuildDataObjects(PreParsedLog preParsedLog) {
        ArrayList<BaseDataObject> baseDataObjects = new ArrayList<>();
        PvDataObject pvDataObject = new PvDataObject();
        ColumnReader columnReader = new ColumnReader(preParsedLog.getQueryString());
        // 1.解析并填充公共字段
        fillCommonBaseDataObjectValue(pvDataObject, preParsedLog, columnReader);
        // 2.解析并填充特有字段

        //2.1网站信息的解析
        pvDataObject.setSiteResourceInfo(createSiteResourceInfo(columnReader));
        //2.2广告信息的解析
        pvDataObject.setAdInfo(createAdInfo(pvDataObject.getSiteResourceInfo().getQuery()));
        //2.3浏览器信息的解析
        pvDataObject.setBrowserInfo(createBrowserInfo(columnReader));
        //2.4来源信息的解析
        pvDataObject.setReferrerInfo(createReferrerInfo(columnReader));
        //2.4.1来源类型和来源渠道的解析
        resolveReferrerDerivedColumns(pvDataObject.getReferrerInfo(), pvDataObject.getAdInfo());

        //2.5 目标页面的解析
        TargetPageDataObject targetPageDataObject = populateTargetPage(pvDataObject, preParsedLog, columnReader);

        baseDataObjects.add(pvDataObject);
        if (targetPageDataObject != null) {
            baseDataObjects.add(targetPageDataObject);
        }
        return baseDataObjects;
    }

private TargetPageDataObject populateTargetPage(PvDataObject pvDataObject,
                                                PreParsedLog preParsedLog, ColumnReader columnReader) {
    //得到指定的profileId所有已经匹配到的目标页面对象
    List<TargetPage> targetPages = targetPageAnalyzer.getMatchedTargetPages(pvDataObject.getProfileId(),
            pvDataObject.getSiteResourceInfo().getUrl());
    //2.填充特有字段
    //2.组成TargetPageDataObject
    if (targetPages != null && !targetPages.isEmpty()){
        TargetPageDataObject targetPageDataObject = new TargetPageDataObject();
        List<TargetPageInfo> targetPageInfos = targetPages.stream().map(targetPage ->
                new TargetPageInfo(targetPage.getId(), targetPage.getName(), targetPage.isEnable())).collect(Collectors.toList());
        fillCommonBaseDataObjectValue(targetPageDataObject, preParsedLog, columnReader);
        targetPageDataObject.setTargetPageInfos(targetPageInfos);
        targetPageDataObject.setPvDataObject(pvDataObject);
        return  targetPageDataObject;
    }

    return null;
}

    private void resolveReferrerDerivedColumns(ReferrerInfo referrerInfo, AdInfo adInfo) {
        //1来源渠道的计算逻辑：
        //先赋值为广告系列渠道，如果没有的话则赋值为搜索引擎，如果还没有的话则赋值为来源域名
        String adChannel = adInfo.getUtmChannel();
        if (!isNullOrEmptyOrDash(adChannel)) {
            referrerInfo.setChannel(adChannel);
        } else if (!isNullOrEmptyOrDash(referrerInfo.getSearchEngineName())) {
            referrerInfo.setChannel(referrerInfo.getSearchEngineName());
        } else {
            referrerInfo.setChannel(referrerInfo.getDomain());
        }

        //2来源类型计算逻辑
        if (!isNullOrEmptyOrDash(referrerInfo.getSearchEngineName())) {
            if (adInfo.isPaid()) {
                //从搜索引擎中过来且是付费流量
                referrerInfo.setReferType("paid search"); //付费搜索
            } else {
                //从搜索引擎中过来但不是付费流量
                referrerInfo.setReferType("organic search"); //自然搜索
            }
        } else if (!isNullOrEmptyOrDash(referrerInfo.getDomain())) {
            //从非搜索引擎的网站中过来
            referrerInfo.setReferType("referral"); //引荐，其实就是外部链接
        } else {
            //直接访问
            referrerInfo.setReferType("direct"); //直接访问
        }
    }

    private ReferrerInfo createReferrerInfo(ColumnReader columnReader) {
        ReferrerInfo referrerInfo = new ReferrerInfo();
        String referUrl = columnReader.getStringValue("gsref");
        if (referUrl == "-") {
            referrerInfo.setChannel("-");
            referrerInfo.setDomain("-");
            referrerInfo.setEqId("-");
            referrerInfo.setSearchEngineName("-");
            referrerInfo.setUrl("-");
            referrerInfo.setQuery("-");
            referrerInfo.setUrlWithoutQuery("-");
            referrerInfo.setKeyword("-");
        } else {
            UrlInfo urlInfo = UrlParseUtils.getInfoFromUrl(referUrl);
            referrerInfo.setDomain(urlInfo.getDomain());
            referrerInfo.setUrl(urlInfo.getFullUrl());
            referrerInfo.setQuery(urlInfo.getQuery());
            referrerInfo.setUrlWithoutQuery(urlInfo.getUrlWithoutQuery());

            //5、搜索引擎和关键词的解析
            SearchEngineNameUtil.populateSearchEngineInfoFromRefUrl(referrerInfo);
        }
        return referrerInfo;
    }


    private BrowserInfo createBrowserInfo(ColumnReader columnReader) {
        BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAlexaToolBar(ParseUtils.parseBoolean(columnReader.getStringValue("gsalexaver")));
        browserInfo.setBrowserLanguage(columnReader.getStringValue("gsbrlang"));
        browserInfo.setColorDepth(columnReader.getStringValue("gsclr"));
        browserInfo.setCookieEnable(ParseUtils.parseBoolean(columnReader.getStringValue("gsce")));
        browserInfo.setDeviceName(columnReader.getStringValue("dvn"));
        browserInfo.setDeviceType(columnReader.getStringValue("dvt"));
        browserInfo.setFlashVersion(columnReader.getStringValue("gsflver"));
        browserInfo.setJavaEnable(ParseUtils.parseBoolean(columnReader.getStringValue("gsje")));
        browserInfo.setOsLanguage(columnReader.getStringValue("gsoslang"));
        browserInfo.setResolution(columnReader.getStringValue("gsscr"));
        browserInfo.setSilverlightVersion(columnReader.getStringValue("gssil"));
        return browserInfo;
    }

    private AdInfo createAdInfo(String query) {
        AdInfo adInfo = new AdInfo();
        Map<String, String> landingParams = UrlParseUtils.getQueryParams(query);
        adInfo.setUtmCampaign(landingParams.getOrDefault("utm_campaign", "-"));
        adInfo.setUtmMedium(landingParams.getOrDefault("utm_medium", "-"));
        adInfo.setUtmContent(landingParams.getOrDefault("utm_content", "-"));
        adInfo.setUtmChannel(landingParams.getOrDefault("utm_channel", "-"));
        adInfo.setUtmTerm(landingParams.getOrDefault("utm_term", "-"));
        adInfo.setUtmSource(landingParams.getOrDefault("utm_source", "-"));
        adInfo.setUtmAdGroup(landingParams.getOrDefault("utm_adgroup", "-"));
        return adInfo;
    }

    private SiteResourceInfo createSiteResourceInfo(ColumnReader columnReader) {
        SiteResourceInfo siteResourceInfo = new SiteResourceInfo();
        UrlInfo urlInfo = getInfoFromUrl(columnReader.getStringValue("gsurl"));
        siteResourceInfo.setDomain(urlInfo.getDomain());
        siteResourceInfo.setQuery(urlInfo.getQuery());
        siteResourceInfo.setUrl(urlInfo.getFullUrl());
        siteResourceInfo.setPageTitle(columnReader.getStringValue("gstl"));
        siteResourceInfo.setOriginalUrl(columnReader.getStringValue("gsorurl"));
        return siteResourceInfo;
    }
}