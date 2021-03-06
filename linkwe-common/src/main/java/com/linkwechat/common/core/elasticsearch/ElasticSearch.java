package com.linkwechat.common.core.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.linkwechat.common.core.domain.elastic.ElasticSearchEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author danmo
 * @description es?????????
 * @date 2020/12/9 14:02
 **/
@Slf4j
@Component
public class ElasticSearch {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * @param idxName ????????????
     * @param idxSQL  ????????????
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:30
     * @since
     */
    public void createIndex(String idxName, String idxSQL) {
        try {
            if (!this.indexExist(idxName)) {
                //log.error(" idxName={} ????????????,idxSql={}", idxName, idxSQL);
                return;
            }
            CreateIndexRequest request = new CreateIndexRequest(idxName);
            buildSetting(request);
            request.mapping(idxSQL, XContentType.JSON);
//            request.settings() ????????????Setting
            CreateIndexResponse res = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            if (!res.isAcknowledged()) {
                throw new RuntimeException("???????????????");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param idxName ????????????
     * @param builder ????????????
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:30
     * @since
     */
    public void createIndex2(String idxName, XContentBuilder builder) {
        try {
            if (!this.indexExist(idxName)) {
                //log.error(" idxName={} ????????????,idxSql={}", idxName, builder);
                return;
            }
            CreateIndexRequest request = new CreateIndexRequest(idxName);
            GetIndexRequest getIndexRequest = new GetIndexRequest(idxName);
            buildSetting(request);
            request.mapping(builder);
//            request.settings() ????????????Setting
            boolean exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if (!exists) {
                CreateIndexResponse res = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
                if (!res.isAcknowledged()) {
                    log.info("???????????????");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????index????????????
     *
     * @param idxName index???
     * @return boolean
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:27
     * @since
     */
    public boolean indexExist(String idxName) throws Exception {
        GetIndexRequest request = new GetIndexRequest(idxName);
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.lenientExpandOpen());
        return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * ?????????index????????????
     *
     * @param idxName index???
     * @return boolean
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:27
     * @since
     */
    public boolean isExistsIndex(String idxName) throws Exception {
        return restHighLevelClient.indices().exists(new GetIndexRequest(idxName), RequestOptions.DEFAULT);
    }

    /**
     * ????????????
     *
     * @param request
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 19:27
     * @since
     */
    public void buildSetting(CreateIndexRequest request) {
        request.settings(Settings.builder().put("index.number_of_shards", 3)
                .put("index.number_of_replicas", 2));
    }

    /**
     * @param idxName index
     * @param entity  ??????
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:27
     * @since
     */
    public void insertOrUpdateOne(String idxName, ElasticSearchEntity entity) {
        IndexRequest request = new IndexRequest(idxName, "_doc");
        log.info("Data : id={},entity={}", entity.getId(), JSON.toJSONString(entity.getData()));
        request.id(entity.getId());
        request.source(entity.getData(), XContentType.JSON);
//        request.source(JSON.toJSONString(entity.getData()), XContentType.JSON);
        try {
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * ??????????????????
     *
     * @param idxName index
     * @param list    ???????????????
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:26
     * @since
     */
    public void insertBatch(String idxName, List<ElasticSearchEntity> list) {
        BulkRequest request = new BulkRequest();
        list.forEach(item -> request.add(new IndexRequest(idxName, "_doc").id(item.getId())
                .source(item.getData(), XContentType.JSON)));
        try {
            if (!CollectionUtils.isEmpty(request.requests())) {
                restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param idxName
     * @param list
     * @param consumer
     */
    public void insertBatchAsync(String idxName, List<ElasticSearchEntity> list, BiConsumer consumer, Object param) {
        BulkRequest request = new BulkRequest();
        list.parallelStream().forEach(item -> request.add(new IndexRequest(idxName, "_doc").id(item.getId())
                .source(item.getData(), XContentType.JSON)));
        try {
            if (!CollectionUtils.isEmpty(request.requests())) {
                restHighLevelClient.bulkAsync(request, RequestOptions.DEFAULT, getActionListener(consumer, list, param));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insertBatchAsync(String idxName, List<JSONObject> list, Consumer<List<JSONObject>> consumer) {
        BulkRequest request = new BulkRequest();
        list.forEach(item -> request.add(new IndexRequest(idxName, "_doc").id(item.getString("msgid"))
                .source(item, XContentType.JSON)));
        try {
            if (!CollectionUtils.isEmpty(request.requests())) {
                restHighLevelClient.bulkAsync(request, RequestOptions.DEFAULT, getActionListener(consumer, list));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateBatch(String idxName, List<ElasticSearchEntity> list) {
        BulkRequest request = new BulkRequest();
        list.forEach(item -> request.add(new UpdateRequest(idxName, item.getId()).upsert(item.getData(), XContentType.JSON)));
        try {
            if (!CollectionUtils.isEmpty(request.requests())) {
                restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ????????????
     *
     * @param idxName index
     * @param idList  ???????????????
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:14
     * @since
     */
    public <T> void deleteBatch(String idxName, Collection<T> idList) {
        BulkRequest request = new BulkRequest();
        idList.forEach(item -> request.add(new DeleteRequest(idxName, "_doc", item.toString())));
        try {
            if (!CollectionUtils.isEmpty(request.requests())) {
                restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param idxName index
     * @param builder ????????????
     * @param c       ???????????????
     * @return java.util.List<T>
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:14
     * @since
     */
    public <T> List<T> search(String idxName, SearchSourceBuilder builder, Class<T> c) {
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            long totalHits = response.getHits().getTotalHits().value;
            SearchHit[] hits = response.getHits().getHits();
            List<T> res = new ArrayList<>(hits.length);
            for (SearchHit hit : hits) {
                res.add(JSON.parseObject(hit.getSourceAsString(), c));
            }
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> PageInfo<T> searchPage(String idxName, SearchSourceBuilder builder, int pageNum, int pageSize, Class<T> c) {
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            int totalHits = (int) response.getHits().getTotalHits().value;
            SearchHit[] hits = response.getHits().getHits();
            List<T> res = new ArrayList<>(hits.length);
            for (SearchHit hit : hits) {
                //??????????????????
                //?????????????????????????????????????????????
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField hghlightContent = highlightFields.get("text.content");
                String newName = "";
                if (hghlightContent != null) {
                    //????????????????????????????????????
                    Text[] fragments = hghlightContent.getFragments();
                    //??????????????????????????????????????????
                    for (Text fragment : fragments) {
                        newName += fragment;
                    }
                }
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                sourceAsMap.put("content", newName);
                res.add(JSON.parseObject(JSONObject.toJSONString(sourceAsMap), c));
            }
            // ????????????
            PageInfo<T> page = new PageInfo<>();
            page.setList(res);
            page.setPageNum(pageNum);
            page.setPageSize(pageSize);
            page.setTotal(totalHits);
            page.setPages(totalHits == 0 ? 0 : (totalHits % pageNum == 0 ? totalHits / pageNum : (totalHits / pageNum) + 1));
            page.setHasNextPage(page.getPageNum() < page.getPages());
            return page;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ??????index
     *
     * @param idxName
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:13
     * @since
     */
    public void deleteIndex(String idxName) {
        try {
            if (!this.indexExist(idxName)) {
                //log.error(" idxName={} ????????????", idxName);
                return;
            }
            restHighLevelClient.indices().delete(new DeleteIndexRequest(idxName), RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param idxName
     * @param builder
     * @return void
     * @throws
     * @author danmo
     * @See
     * @date 2019/10/17 17:13
     * @since
     */
    public void deleteByQuery(String idxName, QueryBuilder builder) {
        DeleteByQueryRequest request = new DeleteByQueryRequest(idxName);
        request.setQuery(builder);
        //????????????????????????,?????????10000
        request.setBatchSize(10000);
        request.setConflicts("proceed");
        try {
            restHighLevelClient.deleteByQuery(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ActionListener getActionListener(Consumer consumer, List<JSONObject> list) {
        return new ActionListener() {
            @Override
            public void onResponse(Object o) {
                consumer.accept(list);
            }

            @Override
            public void onFailure(Exception e) {
                log.warn("work with es failed, exception={}", ExceptionUtils.getStackTrace(e));
            }
        };
    }

    public ActionListener getActionListener(BiConsumer consumer, List<ElasticSearchEntity> list, Object param) {
        return new ActionListener() {
            @Override
            public void onResponse(Object o) {
                consumer.accept(list, param);
            }

            @Override
            public void onFailure(Exception e) {
                log.warn("work with es failed, exception={}", ExceptionUtils.getStackTrace(e));
            }
        };
    }

    public XContentBuilder getFinanceMapping() throws IOException {
        // ?????? ????????????Mapping
        XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("properties")
                .startObject("msgid")
                .field("type", "keyword")
                .endObject()
                .startObject("seq")
                .field("type", "long")
                .endObject()
                .startObject("action")
                .field("type", "keyword")
                .endObject()
                .startObject("from")
                .field("type", "keyword")
                .endObject()
                .startObject("roomid")
                .field("type", "keyword")
                .endObject()
                .startObject("msgtime")
                .field("type", "long")
                .endObject()
                .startObject("msgtype")
                .field("type", "keyword")
                .endObject()
                .endObject()
                .endObject();
        return xContentBuilder;
    }
}