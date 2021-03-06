package com.we.es.highlevel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.bucketselector.BucketSelectorPipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES??????????????????
 * @author we
 * @date 2021-09-15 21:12
 **/
public class EsAggregationSearchTest {
    private static String elasticIp = "127.0.0.1";
    private static int elasticPort = 9200;
    private static Logger logger = LoggerFactory.getLogger(EsHighLevelRestSearchTest.class);

    private static RestHighLevelClient client = null;

    public static void main(String[] args) {
        try {
            init();
            createIndex();
            bulk();
            groupbySearch();
            avgSearch();
            maxSearch();
            sumSearch();
            avgGroupSearch();
            maxGroupSearch();
            sumGroupSearch();
            topSearch();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            close();
        }

    }

    /**
     * ?????????ES???????????????
     */
    private static void init() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(elasticIp, elasticPort));
        client = new RestHighLevelClient(restClientBuilder);

    }

    /**
     * ??????ES???????????????
     */
    private static void close() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                client = null;
            }
        }
    }

    /**
     * ????????????
     * @throws IOException
     */
    private static void createIndex() throws IOException {
        // ??????
        String type = "_doc";
        String index = "student";
        // setting ??????
        Map<String, Object> settings = new HashMap<>();
        // ??????????????????????????????????????????
        settings.put("number_of_shards", 10);
        settings.put("number_of_replicas", 1);
        settings.put("refresh_interval", "5s");
        Map<String, Object> keyword = new HashMap<>(1);
        //????????????
        keyword.put("type", "keyword");
        Map<String, Object> number = new HashMap<>(1);
        //????????????
        number.put("type", "long");
        Map<String, Object> date1 = new HashMap<>(2);
        //????????????
        date1.put("type", "date");
        date1.put("format", "yyyy-MM-dd");

        Map<String, Object> date2 = new HashMap<>(2);
        //????????????
        date2.put("type", "date");
        date2.put("format", "yyyy-MM-dd HH:mm:ss.SSS");
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        //????????????message??????
        properties.put("uid", number);
        properties.put("grade", number);
        properties.put("class", number);
        properties.put("age", number);
        properties.put("name", keyword);
        properties.put("createtm", date1);
        properties.put("updatetm", date2);
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        jsonMap.put(type, mapping);

        GetIndexRequest getRequest = new GetIndexRequest();
        getRequest.indices(index);
        getRequest.types(type);
        getRequest.local(false);
        getRequest.humanReadable(true);
        boolean exists2 = client.indices().exists(getRequest, RequestOptions.DEFAULT);
        //???????????????????????????
        if(exists2) {
            System.out.println(index+"?????????????????????!");
            return;
        }
        // ???????????????
        CreateIndexRequest request = new CreateIndexRequest(index);
        try {
            // ??????????????????
            request.settings(settings);
            //??????mapping??????
            request.mapping(type, jsonMap);
            //????????????
            request.alias(new Alias("pancm_alias"));
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            boolean falg = createIndexResponse.isAcknowledged();
            if(falg){
                System.out.println("???????????????:"+index+"?????????" );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * ??????????????????
     * @throws InterruptedException
     */
    private static void bulk() throws IOException{
        // ??????
        String type = "_doc";
        String index = "student";

        BulkRequest request = new BulkRequest();
        int k =10;
        List<Map<String,Object>> mapList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.now();
        for (int i = 1; i <=k ; i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("uid",i);
            map.put("age",i);
            map.put("name","WangEn"+(i%3));
            map.put("class",i%10);
            map.put("grade",400+i);
            map.put("createtm",ldt.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            map.put("updatetm",ldt.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            if(i==5){
                map.put("updatetm","2019-11-31 21:04:55.268");
            }
            mapList.add(map);
        }


        for (int i = 0; i <mapList.size() ; i++) {
            Map<String,Object> map = mapList.get(i);
            String id = map.get("uid").toString();
            // ??????????????????/??????/?????? ??????
            //docAsUpsert ???true??????????????????????????????????????????false?????????????????????????????????
            request.add(new UpdateRequest(index, type, id).doc(map, XContentType.JSON).docAsUpsert(true).retryOnConflict(5));
        }

        client.bulk(request, RequestOptions.DEFAULT);
        System.out.println("?????????????????????");
    }

    /**
     * ????????????????????????
     * SQL: select age, name, count(*) as count1 from student group by age, name;
     * @throws IOException
     */
    private static void groupbySearch() throws IOException{
        String buk="group";
        AggregationBuilder aggregation = AggregationBuilders.terms("age").field("age");
        AggregationBuilder aggregation2 = AggregationBuilders.terms("name").field("name");
        //??????????????????????????????
        AggregationBuilder aggregation3 = AggregationBuilders.dateHistogram("createtm")
                .field("createtm")
                .format("yyyy-MM-dd")
                .dateHistogramInterval(DateHistogramInterval.DAY);

        aggregation2.subAggregation(aggregation3);
        aggregation.subAggregation(aggregation2);
        agg(aggregation,buk);
    }

    /**
     * ????????????????????????
     * @throws IOException
     */
    private static  void avgSearch() throws IOException {

        String buk="t_grade_avg";
        // ??????????????????
        AggregationBuilder aggregation = AggregationBuilders.avg(buk).field("grade");
        logger.info("????????????????????????:");
        agg(aggregation,buk);

    }

    private static  void maxSearch() throws  IOException{
        String buk="t_grade";
        AggregationBuilder aggregation = AggregationBuilders.max(buk).field("grade");
        logger.info("????????????????????????:");
        agg(aggregation,buk);
    }

    private static  void sumSearch() throws  IOException{
        String buk="t_grade";
        AggregationBuilder aggregation = AggregationBuilders.sum(buk).field("grade");
        logger.info("?????????????????????:");
        agg(aggregation,buk);
    }

    /**
     * ??????????????????????????????
     * @throws IOException
     */
    private static  void avgGroupSearch() throws IOException {
        String agg="t_class_avg";
        String buk="t_grade";
        //terms ?????????????????? ??????student???grade?????????????????????????????????????????????
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(agg).field("class");
        aggregation.subAggregation(AggregationBuilders.avg(buk).field("grade"));

        logger.info("???????????????????????????:");
        agg(aggregation,agg,buk);

    }

    private static  void maxGroupSearch() throws  IOException{

        String agg="t_class_max";
        String buk="t_grade";
        //terms ?????????????????? ??????student???grade?????????????????????????????????????????????
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(agg).field("class");
        aggregation.subAggregation(AggregationBuilders.max(buk).field("grade"));
        logger.info("???????????????????????????:");
        agg(aggregation,agg,buk);
    }

    private static  void sumGroupSearch() throws  IOException{
        String agg="t_class_sum";
        String buk="t_grade";
        //terms ?????????????????? ??????student???grade?????????????????????????????????????????????
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(agg).field("class");
        aggregation.subAggregation(AggregationBuilders.sum(buk).field("grade"));

        logger.info("?????????????????????:");
        agg(aggregation,agg,buk);
    }

    protected  static  void agg(AggregationBuilder aggregation, String buk) throws  IOException{
        SearchResponse searchResponse = search(aggregation);
        if(RestStatus.OK.equals(searchResponse.status())) {
            // ??????????????????
            Aggregations aggregations = searchResponse.getAggregations();

            if(buk.contains("avg")){
                //????????????
                Avg ba = aggregations.get(buk);
                logger.info(buk+":" + ba.getValue());
                logger.info("------------------------------------");
            }else if(buk.contains("max")){
                //????????????
                Max ba = aggregations.get(buk);
                logger.info(buk+":" + ba.getValue());
                logger.info("------------------------------------");

            }else if(buk.contains("min")){
                //????????????
                Min ba = aggregations.get(buk);
                logger.info(buk+":" + ba.getValue());
                logger.info("------------------------------------");
            }else if(buk.contains("sum")){
                //????????????
                Sum ba = aggregations.get(buk);
                logger.info(buk+":" + ba.getValue());
                logger.info("------------------------------------");
            }else if(buk.contains("top")){
                //????????????TopHits
                TopHits ba = aggregations.get(buk);
                logger.info(buk+":" + ba.getHits().totalHits);
                logger.info("------------------------------------");
            }else if (buk.contains("group")){
                Map<String,Object> map =  new HashMap<>();
                List<Map<String,Object>> list = new ArrayList<>();
                agg(map,list,aggregations);
                logger.info("??????????????????:"+list);
                logger.info("------------------------------------");
            }

        }
    }

    private static void agg(Map<String,Object> map, List<Map<String,Object>> list, Aggregations aggregations) {
        aggregations.forEach(aggregation -> {
            String name = aggregation.getName();
            Terms genders = aggregations.get(name);
            for (Terms.Bucket entry : genders.getBuckets()) {
                String key = entry.getKey().toString();
                long t = entry.getDocCount();
                map.put(name,key);
                map.put(name+"_"+"count",t);

                //???????????????????????????????????????
                List<Aggregation> list2 = entry.getAggregations().asList();
                if (list2.isEmpty()) {
                    Map<String,Object> map2 = new HashMap<>();
                    BeanUtils.copyProperties(map,map2);
                    list.add(map2);
                }else{
                    agg(map, list, entry.getAggregations());
                }
            }
        });
    }

    private static void agg(List<Map<String, Object>> list, Aggregations aggregations) {
        aggregations.forEach(aggregation -> {
            String name = aggregation.getName();
            Terms genders = aggregations.get(name);
            for (Terms.Bucket entry : genders.getBuckets()) {
                String key = entry.getKey().toString();
                long t = entry.getDocCount();
                Map<String,Object> map =new HashMap<>();
                map.put(name,key);
                map.put(name+"_"+"count",t);
                //???????????????????????????????????????
                List<Aggregation> list2 = entry.getAggregations().asList();
                if (list2.isEmpty()) {
                    list.add(map);
                }else{
                    agg(list, entry.getAggregations());
                }
            }
        });
        System.out.println(list);
    }

    private static SearchResponse search(AggregationBuilder aggregation) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("student");
        searchRequest.types("_doc");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //???????????????
        searchSourceBuilder.explain(false);
        //?????????????????????
        searchSourceBuilder.fetchSource(false);
        //??????????????????
        searchSourceBuilder.version(false);
        searchSourceBuilder.aggregation(aggregation);
        logger.info("???????????????:"+searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);
        // ????????????
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        return  searchResponse;
    }

    /**
     * ????????????
     * @param aggregation
     * @param agg
     * @param buk
     * @throws IOException
     */
    protected  static  void agg(AggregationBuilder aggregation, String agg, String buk) throws  IOException{
        // ????????????
        SearchResponse searchResponse = search(aggregation);

        //4???????????????
        //????????????????????????
        if(RestStatus.OK.equals(searchResponse.status())) {
            // ??????????????????
            Aggregations aggregations = searchResponse.getAggregations();


            //??????
            Terms byAgeAggregation = aggregations.get(agg);
            logger.info(agg+" ??????");
            logger.info("name: " + byAgeAggregation.getName());
            logger.info("type: " + byAgeAggregation.getType());
            logger.info("sumOfOtherDocCounts: " + byAgeAggregation.getSumOfOtherDocCounts());

            logger.info("------------------------------------");
            for(Terms.Bucket buck : byAgeAggregation.getBuckets()) {
                logger.info("key: " + buck.getKeyAsNumber());
                logger.info("docCount: " + buck.getDocCount());
                logger.info("docCountError: " + buck.getDocCountError());


                if(agg.contains("avg")){
                    //????????????
                    Avg ba = buck.getAggregations().get(buk);
                    logger.info(buk+":" + ba.getValue());
                    logger.info("------------------------------------");
                }else if(agg.contains("max")){
                    //????????????
                    Max ba = buck.getAggregations().get(buk);
                    logger.info(buk+":" + ba.getValue());
                    logger.info("------------------------------------");

                }else if(agg.contains("min")){
                    //????????????
                    Min ba = buck.getAggregations().get(buk);
                    logger.info(buk+":" + ba.getValue());
                    logger.info("------------------------------------");
                }else if(agg.contains("sum")){
                    //????????????
                    Sum ba = buck.getAggregations().get(buk);
                    logger.info(buk+":" + ba.getValue());
                    logger.info("------------------------------------");
                }
            }
        }
    }

    /**
     * having
     * @throws IOException
     */
    private static void havingSearch() throws IOException{
        String index="";
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.indices(index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        String alias_name = "nas_ip_address_group";
        String group_name = "nas_ip_address";
        String query_name = "acct_start_time";
        String query_type = "gte,lte";
        String query_name_value="2020-08-05 13:25:55,2020-08-20 13:26:55";
        String[] query_types= query_type.split(",");
        String[] query_name_values= query_name_value.split(",");

        for (int i = 0; i < query_types.length; i++) {
            if("gte".equals(query_types[i])){
                boolQueryBuilder.must(QueryBuilders.rangeQuery(query_name).gte(query_name_values[i]));
            }
            if("lte".equals(query_types[i])){
                boolQueryBuilder.must(QueryBuilders.rangeQuery(query_name).lte(query_name_values[i]));
            }
        }

        AggregationBuilder aggregationBuilder = AggregationBuilders.terms(alias_name).field(group_name).size(Integer.MAX_VALUE);

        //??????BucketPath??????????????????bucket??????
        Map<String, String> bucketsPathsMap = new HashMap<>(8);
        bucketsPathsMap.put("groupCount", "_count");
        //????????????
        Script script = new Script("params.groupCount >= 1000");
        //??????bucket?????????
        BucketSelectorPipelineAggregationBuilder bs =
                PipelineAggregatorBuilders.bucketSelector("having", bucketsPathsMap, script);
        aggregationBuilder.subAggregation(bs);
        sourceBuilder.aggregation(aggregationBuilder);
        //???????????????
        sourceBuilder.explain(false);
        //?????????????????????
        sourceBuilder.fetchSource(false);
        //??????????????????
        sourceBuilder.version(false);
        sourceBuilder.query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);


        System.out.println(sourceBuilder);
        // ????????????
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        // ????????????
        long count = searchResponse.getHits().getHits().length;
        Aggregations aggregations = searchResponse.getAggregations();
//        agg(aggregations);
        Map<String,Object> map =new HashMap<>();
        List<Map<String,Object>> list =new ArrayList<>();
        agg(list,aggregations);
//        System.out.println(map);
        System.out.println(list);
    }

    /**
     * ??????
     * @throws IOException
     */
    private static void distinctSearch() throws IOException{
        String buk="group";
        String distinctName="name";
        AggregationBuilder aggregation = AggregationBuilders.terms("age").field("age");
        CardinalityAggregationBuilder cardinalityBuilder  = AggregationBuilders.cardinality(distinctName).field(distinctName);
        //??????????????????????????????
//        AggregationBuilder aggregation3 = AggregationBuilders.dateHistogram("createtm")
//                .field("createtm")
//                .format("yyyy-MM-dd")
//                .dateHistogramInterval(DateHistogramInterval.DAY);
//
//        aggregation2.subAggregation(aggregation3);
        aggregation.subAggregation(cardinalityBuilder);
        agg(aggregation,buk);
    }

    private static  void topSearch() throws  IOException{

    }




}
