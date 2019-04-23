package com.zxl.parser.dataobject;

import com.zxl.parser.dataobject.dim.TargetPageInfo;

import java.util.List;

/**
 * 目标页面实体
 */
public class TargetPageDataObject extends BaseDataObject {

    private List<TargetPageInfo> targetPageInfos; //目标页面信息
    private PvDataObject pvDataObject;//该pv信息

    public List<TargetPageInfo> getTargetPageInfos() {
        return targetPageInfos;
    }

    public void setTargetPageInfos(List<TargetPageInfo> targetPageInfos) {
        this.targetPageInfos = targetPageInfos;
    }

    public PvDataObject getPvDataObject() {
        return pvDataObject;
    }

    public void setPvDataObject(PvDataObject pvDataObject) {
        this.pvDataObject = pvDataObject;
    }
}
