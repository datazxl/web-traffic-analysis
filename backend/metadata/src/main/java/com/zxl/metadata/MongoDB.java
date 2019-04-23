package com.zxl.metadata;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * mongoDB的连接工具类
 */
public class MongoDB {
    // Mongo的地址
    private static String mongoAddr = System.getProperty("web.metadata.mongodbAddr", "localhost");
    // Mongo的客户端
    private static MongoClient client = new MongoClient(mongoAddr);

    /**
     * 通过指定数据库名称，获取MongoDB中的某个数据库
     * @param dbName
     * @return
     */
    public static MongoDatabase getMongoDatabase(String dbName){
        return client.getDatabase(dbName);
    }
}