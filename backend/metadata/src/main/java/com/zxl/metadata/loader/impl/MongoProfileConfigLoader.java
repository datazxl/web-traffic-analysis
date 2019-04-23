package com.zxl.metadata.loader.impl;

import com.mongodb.client.MongoDatabase;
import com.zxl.metadata.MongoDB;
import com.zxl.metadata.loader.ProfileConfigLoader;
import com.zxl.metadata.model.TargetPage;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MongoProfileConfigLoader implements ProfileConfigLoader {
    /**
     * 从mongo中web-analysis数据库的TargetPage Collection中加载所有的目标页面配置
     * @return
     */
    @Override
    public List<TargetPage> loadAllTargetPagesConfig() {
        ArrayList<TargetPage> targetPages = new ArrayList<>();
        MongoDatabase targetPageDB = MongoDB.getMongoDatabase("web-analysis");
        targetPageDB.getCollection("TargetPage").find().forEach(new Consumer<Document>() {
            @Override
            public void accept(Document document) {
                TargetPage targetPage = new TargetPage(document.getString("Id"),document.getInteger("ProfileId"),
                        document.getString("Name"),document.getString("Description"),document.getString("MatchPattern"),
                        document.getString("MatchType"),document.getBoolean("MatchWithoutQueryString"),!document.getBoolean("IsDisabled"));
                targetPages.add(targetPage);
            }
        });
        return targetPages;
    }
}
