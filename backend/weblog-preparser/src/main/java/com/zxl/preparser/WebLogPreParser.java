package com.zxl.preparser;

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
            String serverTime = preParsedLog.getServerTime();
            preParsedLog.setYear(Integer.parseInt(serverTime.substring(0,4)));
            preParsedLog.setMonth(Integer.parseInt(serverTime.substring(5,7)));
            preParsedLog.setDay(Integer.parseInt(serverTime.substring(8,10)));
            return preParsedLog;
        }
    }
}
