package com.zxl.parser.utils;

import java.util.HashMap;
import java.util.Map;

/**
 *  按照一定的规则解析query_string，将所有的kv值放到内存中
 *  便于根据key获取相对应的value
 */
public class ColumnReader {
    private Map<String,String> keyvalues = new HashMap<String, String>();

    public ColumnReader(String line){
        String[] fields = line.split("&");
        for(String field :fields){
            String[] kv = field.split("=");
            if (kv.length == 2){
                keyvalues.put(kv[0], kv[1]);
            }
        }
    }

    /**
     * 对获取的value进行二次解码（如果有）并返回解码后的值
     * @param key
     * @return
     */
    public String getStringValue(String key) {
        return ParseUtils.decode(keyvalues.getOrDefault(key, "-"));
    }
}
