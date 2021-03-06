package com.we.es.highlevel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author we
 * @date 2021-09-15 17:21
 **/
public final class EsUtil {

    private static Logger logger = LoggerFactory.getLogger(EsHighLevelRestSearchTest.class);

    private EsUtil() { }

    private static String[] elasticIps;
    private static int elasticPort;
    private static HttpHost[] httpHosts;
    private static volatile RestHighLevelClient client = null;
    /**
     * ????????????????????????
     */
    private static boolean isAutoClose = true;

    private static final String COMMA_SIGN = ",";

    public static boolean isIsAutoClose() {
        return isAutoClose;
    }

    public static void setIsAutoClose(boolean isAutoClose) {
        EsUtil.isAutoClose = isAutoClose;
    }

    /**
     * ?????????ES??????????????????
     */
    private static void init() {
        if (client == null) {
            synchronized (EsUtil.class) {
                if (client == null) {
                    RestClientBuilder restClientBuilder = RestClient.builder(httpHosts);
                    client = new RestHighLevelClient(restClientBuilder);
                }
            }
        }
    }

    /**
     * ??????ES??????????????????
     * @throws IOException
     */
    private static void close() throws IOException {
        if (client != null) {
            try {
                client.close();
                setIsAutoClose(true);
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
     * ??????????????????
     * @throws IOException
     */
    private static void createIndexTest() throws IOException {
        // setting??????
        Map<String, Object> settings = new HashMap<>();
        // ?????????
        settings.put("number_of_shards", 12);
        // ???????????????
        settings.put("number_of_routing_shards", 24);
        // ?????????
        settings.put("number_of_replicas", 1);
        // ??????????????????
        settings.put("refresh_interval", "5s");

        String index = "test5";
        String type = "test5";
        String alias = "test";

        Map<String, Object> jsonMap2 = new HashMap<>(1);
        Map<String, Object> message = new HashMap<>(1);
        // ????????????
        message.put("type", "text");
        Map<String, Object> properties = new HashMap<>(1);
        // ????????????message??????
        properties.put("msg", message);
        Map<String, Object> mapping = new HashMap<>(1);
        mapping.put("properties", properties);
        jsonMap2.put(type, mapping);

        String mappings = jsonMap2.toString();

        EsBasicModelConfig esBasicModelConfig = new EsBasicModelConfig();
        esBasicModelConfig.setIndex(index);
        esBasicModelConfig.setType(type);
        esBasicModelConfig.setMappings(mappings);
        esBasicModelConfig.setSettings(settings);
        esBasicModelConfig.setAlias(alias);

        EsUtil.createIndex(esBasicModelConfig);
    }


    /**
     * ????????????
     * @param nodes
     * @return
     */
    public static void build(String... nodes) {
        Objects.requireNonNull(nodes, "hosts can not null");
        List<HttpHost> hosts = new ArrayList<>();
        for (String host : nodes) {
            IpHandler address = new IpHandler();
            address.IpPortFromUrl(host);
            hosts.add(new HttpHost(address.getIp(), address.getPort()));
        }
        httpHosts = hosts.toArray(new HttpHost[0]);
        init();
    }


    /**
     * ???????????????(??????Mapping??????)
     * @param esBasicModelConfig
     * @return
     * @throws IOException
     */
    public static boolean createIndex(EsBasicModelConfig esBasicModelConfig) throws IOException {
        boolean flag = true;
        Objects.requireNonNull(esBasicModelConfig, "esBasicModelConfig is not null");
        String type = Objects.requireNonNull(esBasicModelConfig.getType(), "type is not null");
        String index = Objects.requireNonNull(esBasicModelConfig.getIndex(), "index is not null");
        if (exitsIndex(index)) {
            logger.warn("?????????{}????????????!?????????????????????!", index);
            return true;
        }
        String mapping = esBasicModelConfig.getMappings();
        Map<String, Object> setting = esBasicModelConfig.getSettings();
        String alias = esBasicModelConfig.getAlias();
        // ???????????????
        CreateIndexRequest request = new CreateIndexRequest(index);
        try {
            if (Objects.nonNull(mapping)) {
                // ??????????????????
                request.mapping(type, mapping);
            }
            if (Objects.nonNull(setting)) {
                // ?????????
                request.settings(setting);
            }
            if (Objects.nonNull(alias)) {
                // ??????
                request.alias(new Alias(alias));
            }

            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            flag = createIndexResponse.isAcknowledged();
        } catch (IOException e) {
            throw e;
        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return flag;

    }


    /**
     * ???????????????????????????
     * @param index
     * @return
     * @throws IOException
     */
    public static boolean exitsIndex(String index) throws IOException {
        try {
            GetIndexRequest getRequest = new GetIndexRequest();
            getRequest.indices(index);
            getRequest.local(false);
            getRequest.humanReadable(true);
            return client.indices().exists(getRequest, RequestOptions.DEFAULT);
        } finally {
            if (isAutoClose) {
                close();
            }
        }
    }


    /**
     * ????????????/????????????
     * @param map
     * @param index
     * @param type
     * @return
     * @throws IOException
     */
    public static boolean save(Map<String, Object> map, String index, String type) throws IOException {
        List<Map<String, Object>> mapList = new ArrayList<>();
        mapList.add(map);
        return saveBulk(mapList, index, type, null);
    }

    /**
     * ????????????/????????????
     * @param mapList
     * @param index
     * @param type
     * @return
     * @throws IOException
     */
    public static boolean saveBulk(List<Map<String, Object>> mapList, String index, String type) throws IOException {
        return saveBulk(mapList, index, type, null);
    }

    /**
     * ????????????/????????????
     * @param mapList ????????????
     * @param index ????????????
     * @param type ???????????????
     * @param key ????????????????????????????????????ES??????
     * @return
     * @throws IOException
     */
    public static boolean saveBulk(List<Map<String, Object>> mapList, String index, String type, String key) throws IOException {

        if (mapList == null || mapList.size() == 0) {
            return true;
        }
        if (index == null || index.trim().length() == 0 || type == null || type.trim().length() == 0) {
            return false;
        }
        try {
            BulkRequest request = new BulkRequest();
            mapList.forEach(map -> {
                if (key != null) {
                    String id = map.get(key) + "";
                    if (id == null || id.trim().length() == 0) {
                        request.add(new IndexRequest(index, type).source(map, XContentType.JSON));
                    } else {
                        request.add(new IndexRequest(index, type, id).source(map, XContentType.JSON));
                    }
                } else {
                    request.add(new IndexRequest(index, type).source(map, XContentType.JSON));
                }
            });

            BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
            // ??????????????????????????????????????????????????????false
            if (bulkResponse.hasFailures()) {
                return false;
            }

            return true;
        } finally {
            if (isAutoClose) {
                close();
            }
        }
    }

    /**
     * ????????????,??????ID??????????????????
     * @param index
     * @param type
     * @param id
     * @return
     * @throws IOException
     */
    public static boolean deleteById(String index, String type, String id) throws IOException {
        if (index == null || type == null || id == null) {
            return true;
        }
        try {
            DeleteRequest deleteRequest = new DeleteRequest();
            deleteRequest.id(id);
            deleteRequest.index(index);
            deleteRequest.type(type);
            // ????????????
            client.delete(deleteRequest, RequestOptions.DEFAULT);
        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return true;
    }

    /**
     * ??????????????????,??????ID??????????????????
     * @param index
     * @param type
     * @param ids
     * @return
     * @throws IOException
     */
    public static boolean deleteByIds(String index, String type, Set<String> ids) throws IOException {
        if (index == null || type == null || ids == null) {
            return true;
        }
        try {
            BulkRequest requestBulk = new BulkRequest();
            ids.forEach(id -> {
                DeleteRequest deleteRequest = new DeleteRequest(index, type, id);
                requestBulk.add(deleteRequest);
            });
            // ????????????
            client.bulk(requestBulk, RequestOptions.DEFAULT);
        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return false;
    }

    /**
     * ??????id??????
     * @param index
     * @param type
     * @param id
     * @return
     * @throws IOException
     */
    public static Map<String, Object> queryById(String index, String type, String id) throws IOException {
        if (index == null || type == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        try {
            GetRequest request = new GetRequest();
            request.index(index);
            request.type(type);
            request.id(id);
            GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);
            // ?????????????????????????????????????????????
            if (getResponse.isExists()) {
                map = getResponse.getSourceAsMap();
            }
        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return map;
    }


    public static List<Map<String, Object>> query(String index, String type, QueryBuilder... queryBuilders) throws IOException {
        return query(index, type, null , queryBuilders);
    }

    /**
     * ????????????
     * @param index
     * @param type
     * @param esQueryCondition
     * @param queryBuilders
     * @return
     * @throws IOException
     */
    public static List<Map<String, Object>> query(String index, String type, EsQueryCondition esQueryCondition, QueryBuilder... queryBuilders) throws IOException {
        if (index == null || type == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            // ????????????????????????
            SearchRequest searchRequest = new SearchRequest(index);
            searchRequest.types(type);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            if (esQueryCondition != null) {
                Integer form = esQueryCondition.getIndex();
                Integer pagesize = esQueryCondition.getPagesize();
                if (form != null && form > 0 && pagesize != null && pagesize > 0) {
                    form = (form - 1) * pagesize;
                    pagesize = form + pagesize;
                    // ?????????????????????
                    sourceBuilder.from(form);
                    sourceBuilder.size(pagesize);
                }
                String routing = esQueryCondition.getRouting();
                if (routing != null && routing.length() > 0) {
                    // ????????????
                    searchRequest.routing(routing);
                }

                // ????????????????????????
                searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());

                //????????????
                String order = esQueryCondition.getOrder();
                if (order != null) {
                    String[] orderField = esQueryCondition.getOrderField();
                    SortOrder order2 = order.equals(SortOrder.DESC) ? SortOrder.DESC : SortOrder.ASC;
                    //????????????????????????????????????????????????????????????????????????????????????
                    if (orderField != null) {
                        for (String field : orderField) {
                            sourceBuilder.sort(new FieldSortBuilder(field).order(order2));
                        }
                    } else {
                        sourceBuilder.sort(new ScoreSortBuilder().order(order2));
                    }
                }
                String[] includeFields = esQueryCondition.getIncludeFields();
                String[] excludeFields = esQueryCondition.getExcludeFields();
                if (includeFields != null && includeFields.length > 0 && excludeFields != null && excludeFields.length > 0) {
                    sourceBuilder.fetchSource(includeFields, excludeFields);
                }
                sourceBuilder.fetchSource(esQueryCondition.isCloseSource());
            }
            //????????????
            if (queryBuilders != null) {
                for (QueryBuilder queryBuilder : queryBuilders) {
                    sourceBuilder.query(queryBuilder);
                }
            }

            searchRequest.source(sourceBuilder);
            // ????????????
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            if(queryBuilders != null|| (esQueryCondition != null && esQueryCondition.isQueryData())){
                // ??????
                searchResponse.getHits().forEach(hit -> {
                    Map<String, Object> map = hit.getSourceAsMap();
                    list.add(map);
                });
            }

            if(esQueryCondition != null && esQueryCondition.isNeedTotal()){
                Map<String, Object> mapTotal = new HashMap<>();
                mapTotal.put("total", searchResponse.getHits().getTotalHits());
                list.add(mapTotal);
            }

        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return list;
    }

    /**
     * ??????????????????
     * @param index
     * @param type
     * @param queryBuilders
     * @return
     * @throws IOException
     */
    public static Map<String, Object> updateByQuery(String index, String type, QueryBuilder... queryBuilders) throws IOException {
        if (index == null || type == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        try {
            UpdateByQueryRequest request = new UpdateByQueryRequest();
            request.indices(index);
            request.setDocTypes(type);

            if (queryBuilders != null) {
                for (QueryBuilder queryBuilder : queryBuilders) {
                    request.setQuery(queryBuilder);
                }
            }
            // ????????????
            BulkByScrollResponse bulkResponse = client.updateByQuery(request, RequestOptions.DEFAULT);

            // ??????????????????
            map.put("time", bulkResponse.getTook().getMillis());
            map.put("total", bulkResponse.getTotal());

        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return map;
    }

    /**
     * ????????????????????????
     * @param index
     * @param type
     * @param queryBuilders
     * @return
     * @throws IOException
     */
    public static Map<String, Object> deleteByQuery(String index, String type, QueryBuilder[] queryBuilders) throws IOException {
        if (index == null || type == null || queryBuilders == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest(index, type);
            if (queryBuilders != null) {
                for (QueryBuilder queryBuilder : queryBuilders) {
                    request.setQuery(queryBuilder);
                }
            }
            // ????????????
            BulkByScrollResponse bulkResponse = client.deleteByQuery(request, RequestOptions.DEFAULT);
            // ??????????????????
            map.put("time", bulkResponse.getTook().getMillis());
            map.put("total", bulkResponse.getTotal());

        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return map;
    }


    /**
     * ?????????
     * @param index
     * @param destIndex
     * @param queryBuilders
     * @return
     * @throws IOException
     */
    public static Map<String, Object> reindexByQuery(String index, String destIndex, QueryBuilder[] queryBuilders) throws IOException {
        if (index == null || destIndex == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        try {
            // ?????????????????????????????????????????????
            ReindexRequest request = new ReindexRequest();
            // ?????????????????????
            request.setSourceIndices(index);
            // ?????????????????????
            request.setDestIndex(destIndex);
            if (queryBuilders != null) {
                for (QueryBuilder queryBuilder : queryBuilders) {
                    request.setSourceQuery(queryBuilder);
                }
            }
            // ???????????????????????????????????????????????????????????????????????????,?????????index
            request.setDestOpType("create");
            // ???????????????????????????????????????????????????????????????????????????
            request.setConflicts("proceed");


            // ???????????????????????????
            // request.setSize(10);
            // ?????????????????????????????????????????????1000
            //   request.setSourceBatchSize(10000);

            // ??????????????????
            request.setTimeout(TimeValue.timeValueMinutes(2));
            // ????????????
            BulkByScrollResponse bulkResponse = client.reindex(request, RequestOptions.DEFAULT);

            // ??????????????????
            map.put("time", bulkResponse.getTook().getMillis());
            map.put("total", bulkResponse.getTotal());
            map.put("createdDocs", bulkResponse.getCreated());
            map.put("updatedDocs", bulkResponse.getUpdated());

        } finally {
            if (isAutoClose) {
                close();
            }
        }
        return map;
    }


    public static void main(String[] args) {

        try {
            EsUtil.build("127.0.0.1:9200");
            System.out.println("ES?????????????????????!");
            // createIndexTest();
            // System.out.println("ES????????????????????????");
            String index = "student";
            String type = "_doc";
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", i);
                map.put("name", "??????" + i);
                map.put("age", 10 + i);
                list.add(map);
            }
            EsUtil.setIsAutoClose(false);
            saveBulk(list, index, type, "id");
            System.out.println("??????????????????!");
            System.out.println("???????????????1:" + queryById(index, type, "1"));
            QueryBuilder queryBuilder = new TermQueryBuilder("name", "xuwujing");
            System.out.println("???????????????:" + updateByQuery(index, type, queryBuilder));
            System.out.println("???????????????2:" + queryById(index, type, "1"));
            QueryBuilder queryBuilder3 = QueryBuilders.matchAllQuery();
            System.out.println("???????????????3:" + query(index, type, queryBuilder3));
            QueryBuilder queryBuilder4 = QueryBuilders.rangeQuery("age").from(15);
            QueryBuilder queryBuilder5 = QueryBuilders.rangeQuery("id").from(5);
            System.out.println("???????????????4:" + query(index, type, queryBuilder4,queryBuilder5));
            EsQueryCondition esQueryCondition = new EsQueryCondition();
            esQueryCondition.setCloseSource(true);
            esQueryCondition.setIndex(1);
            esQueryCondition.setPagesize(3);
            esQueryCondition.setOrder("desc");
            esQueryCondition.setOrderField(new String[]{"age"});
            String [] incStrings = new String[]{"age","name"};
            esQueryCondition.setIncludeFields(incStrings);
            esQueryCondition.setExcludeFields(new String[]{"id"});
            System.out.println("???????????????5:" + query(index, type,esQueryCondition, queryBuilder4));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // TODO: handle finally clause
            try {
                close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}


/**
 * ??????????????????
 */
class EsQueryCondition implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ???????????????  ??????null?????????0???????????????
     */
    private Integer index;
    private Integer pagesize;

    /**
     * ???????????? asc:?????????desc:??????????????????????????????
     */
    private String order;
    /**
     * ????????????
     */
    private String[] orderField;

    /**
     * ?????? ?????????????????????
     */
    private String routing;

    /**
     * ???????????????
     */
    private String[] includeFields;
    /**
     * ???????????????
     */
    private String[] excludeFields;

    /**
     * ????????????source??????
     */
    private boolean isCloseSource;

    /**  ???????????????????????? */
    private boolean isQueryData = true;

    /** ???????????? ???????????? */
    private boolean isNeedTotal = true;


    public boolean isQueryData() {
        return isQueryData;
    }

    public void setQueryData(boolean queryData) {
        isQueryData = queryData;
    }

    public boolean isNeedTotal() {
        return isNeedTotal;
    }

    public void setNeedTotal(boolean needTotal) {
        isNeedTotal = needTotal;
    }

    public boolean isCloseSource() {
        return isCloseSource;
    }

    public void setCloseSource(boolean closeSource) {
        isCloseSource = closeSource;
    }

    public String[] getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(String[] includeFields) {
        this.includeFields = includeFields;
    }

    public String[] getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(String[] excludeFields) {
        this.excludeFields = excludeFields;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getPagesize() {
        return pagesize;
    }

    public void setPagesize(Integer pagesize) {
        this.pagesize = pagesize;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getRouting() {
        return routing;
    }

    public void setRouting(String routing) {
        this.routing = routing;
    }

    public String[] getOrderField() {
        return orderField;
    }

    public void setOrderField(String[] orderField) {
        this.orderField = orderField;
    }
}


/**
 * ES???mapping??????????????????
 */
class EsBasicModelConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    /*** ????????? ***/
    private String index;
    private String type;
    private Map<String, Object> settings;
    private String mappings;
    private String alias;

    public EsBasicModelConfig() {
    }

    public EsBasicModelConfig(String index, String type) {
        this.index = index;
        this.type = type;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public void setSettings(SettingEntity settings) {
        this.settings = Objects.requireNonNull(settings, "setting can not null").toDSL();
    }

    public String getMappings() {
        return mappings;
    }

    public void setMappings(String mappings) {
        this.mappings = mappings;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return "EsBasicModelConfig [index=" + index + ", type=" + type + ", settings=" + settings + ", mappings="
                + mappings + "]";
    }

}

/**
 * setting ??????????????????
 */
class SettingEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    // ???????????????
    private int numberOfShards = 5;
    // ???????????????
    private int number_of_routing_shards = 30;
    // ?????????
    private int numberOfReplicas = 1;
    /***** ???????????? ??????:??? *********/
    private int refreshInterval = 5;
    /**
     * ???????????????????????????
     */
    private int maxResultWindow = 10000;

    public SettingEntity(int numberOfShards, int numberOfReplicas, int refreshInterval) {
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
        this.refreshInterval = refreshInterval;
    }

    public SettingEntity(int numberOfShards, int numberOfReplicas, int refreshInterval, int number_of_routing_shards,
                         int maxResultWindow, String alias) {
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
        this.refreshInterval = refreshInterval;
        this.number_of_routing_shards = number_of_routing_shards;
        this.maxResultWindow = maxResultWindow;
    }

    public SettingEntity() {

    }

    public int getNumberOfShards() {
        return numberOfShards;
    }

    /**
     * ?????????
     *
     * @param numberOfShards ??????5
     */
    public void setNumberOfShards(int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    /**
     * ?????????
     *
     * @param numberOfReplicas ??????1
     */
    public void setNumberOfReplicas(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public int getNumber_of_routing_shards() {
        return number_of_routing_shards;
    }

    public void setNumber_of_routing_shards(int number_of_routing_shards) {
        this.number_of_routing_shards = number_of_routing_shards;
    }

    public int getMaxResultWindow() {
        return maxResultWindow;
    }

    public void setMaxResultWindow(int maxResultWindow) {
        this.maxResultWindow = maxResultWindow;
    }

    /**
     * ???????????? ??????:???
     *
     * @param refreshInterval ??????5??? ?????????-1???????????????
     */
    public void setRefreshInterval(int refreshInterval) {
        if (refreshInterval < -1) {
            refreshInterval = -1;
        }
        this.refreshInterval = refreshInterval;
    }

    public Map<String, Object> toDSL() {
        Map<String, Object> json = new HashMap<>();
        json.put("number_of_shards", numberOfShards);
        json.put("number_of_routing_shards", number_of_routing_shards);
        json.put("number_of_replicas", numberOfReplicas);
        json.put("refresh_interval", refreshInterval + "s");
        json.put("max_result_window", maxResultWindow);
        return json;
    }

    @Override
    public String toString() {
        return "SettingEntity [numberOfShards=" + numberOfShards + ", numberOfReplicas=" + numberOfReplicas
                + ", refreshInterval=" + refreshInterval + ", maxResultWindow=" + maxResultWindow + "]";
    }

}

/**
 * IP?????????
 */
class IpHandler {
    private String ip;
    private Integer port;
    private static Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+(:\\d{0,5})?");
    /**
     * ??????
     */
    private static final String COMMA_COLON = ":";

    /**
     * ???url????????????hostIP:PORT
     * @param url
     */
    public void IpPortFromUrl(String url) {
        String host = "";
        Matcher matcher = p.matcher(url);
        if (matcher.find()) {
            host = matcher.group();
        }
        if (host.contains(COMMA_COLON) == false) {
            this.ip = host;
            this.port = 80;
        } else {
            String[] ipPortArr = host.split(COMMA_COLON);
            this.ip = ipPortArr[0];
            this.port = Integer.valueOf(ipPortArr[1].trim());
        }
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

}
