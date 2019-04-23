package com.zxl.metadata.loader;

import com.zxl.metadata.model.TargetPage;

import java.util.List;

/**
 * 和Profile相关的配置信息管理的接口
 */
public interface ProfileConfigLoader {
    /**
     * 从数据源（MongoDB或其他）加载所有的目标页面配置。
     * @return Map<Integer, List<TargetPage>> key是ProfileId，value是该ProfileId对应的所有目标页面配置数据
     */
    public List<TargetPage> loadAllTargetPagesConfig();
}
