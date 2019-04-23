package com.zxl.parser;

import com.zxl.parser.dataobject.InvalidLogObject;
import com.zxl.parser.dataobject.ParsedDataObject;
import com.zxl.parser.dataobjectbuilder.AbstractDataObjectBuilder;
import com.zxl.preparser.PreParsedLog;
/**
 *  weblog-parser这个模块对外提供服务的类
 *  该类中包含了LogParser需要的builders和cmds
 */
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LogParser {
    // LogParser支持解析的日志类型
    private Set<String> cmds;
    // LogParser中所有的日志builders
    private List<AbstractDataObjectBuilder> builders;

    public LogParser(Set<String> cmds, List<AbstractDataObjectBuilder> builders) {
        this.cmds = cmds;
        this.builders = builders;
    }

    /**
     * 日志解析的接口
     *  返回的对象中，含有正常的DataObject，也可能含有无效的DataObject，所以我们返回标识接口ParsedDataObject
     *  不管是正常的DataObject还是无效的DataObject都会实现这个标识接口ParsedDataObject
     * @param preParsedLog
     * @return 返回已经解析好的DataObject
     */
    public List<? extends ParsedDataObject> parse(PreParsedLog preParsedLog) {
        String cmd = preParsedLog.getCommand();
        // 看看是否是支持的日志类型
        if (cmds.contains(cmd)) {
            //是否有对应的DataObjectBuilder
            for (AbstractDataObjectBuilder builder : builders) {
                if (cmd.equals(builder.getCommand())){
                    return builder.doBuildDataObjects(preParsedLog);
                }
            }
            return Arrays.asList(new InvalidLogObject("dont find support builder"));
        } else {
            return Arrays.asList(new InvalidLogObject("dont support command"));
        }

    }
}