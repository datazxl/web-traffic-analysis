package com.zxl.parser.configuration.loader;

import com.zxl.metadata.model.TargetPage;

import java.util.List;
import java.util.Map;

/**
 * 加载Profile相关的配置数据
 */
public interface ProfileConfigLoader {

    /**
     * 加载所有的目标页面配置信息
     */
    public Map<Integer,List<TargetPage>> loadAllTargetPagesConfig();
}
