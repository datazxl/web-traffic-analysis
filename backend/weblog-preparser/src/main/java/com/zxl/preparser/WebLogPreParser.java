package com.zxl.preparser;

/**
 * 原始数据进行预解析，生成对应rawdata.web表数据的实体类
 */
public class WebLogPreParser {
    public static PreParsedLog parse(String line){
        if (line.startsWith("#")){
            return null;
        } else {
            PreParsedLog preParsedLog = new PreParsedLog();
            String[] fields = line.split(" ");
            preParsedLog.setServerTime(fields[0] + " " + fields[1]);
            preParsedLog.setServerIp(fields[2]);
            preParsedLog.setMethod(fields[3]);
            preParsedLog.setUriStem(fields[4]);
            preParsedLog.setQueryString(fields[5]);
            preParsedLog.setServerPort(Integer.parseInt(fields[6]));
            preParsedLog.setClientIp(fields[8]);
            preParsedLog.setUserAgent(fields[9].replace("+", " "));
            String[] queryFields = fields[5].split("&");
            preParsedLog.setProfileId(Integer.valueOf(queryFields[2].substring(queryFields[2].indexOf("-") + 1)));
            preParsedLog.setCommand(queryFields[1].split("=")[1]);
            String tempTime = preParsedLog.getServerTime().replace("-", "");
            preParsedLog.setYear(Integer.parseInt(tempTime.substring(0,4)));
            preParsedLog.setMonth(Integer.parseInt(tempTime.substring(0,6)));
            preParsedLog.setDay(Integer.parseInt(tempTime.substring(0,8)));
            return preParsedLog;
        }
    }
}
