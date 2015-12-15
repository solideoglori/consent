package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.IntStream;

import static com.mongodb.client.model.Filters.eq;


/**
 * Implementation class for DatabaseDataAccessRequestAPI.
 */
public class DatabaseDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private final MongoConsentDB mongo;

    private final UseRestrictionConverter converter;

    private final String UN_REVIEWED = "un-reviewed";

    private final ElectionDAO electionDAO;

    private final ConsentDAO consentDAO;

    private final String DATA_SET_ID = "datasetId";

    private final String SUFFIX = "-A-";

    private final Logger logger = Logger.getLogger("DatabaseDataAccessRequestAPI");



    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param mongo     The Data Access Object instance that the API should use to
     *                  read/write data.
     * @param converter
     */
    public static void initInstance(MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(mongo, converter, electionDAO, consentDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param mongo The Data Access Object used to read/write data.
     */
    private DatabaseDataAccessRequestAPI(MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO) {
        this.mongo = mongo;
        this.converter = converter;
        this.electionDAO = electionDAO;
        this.consentDAO = consentDAO;
    }


    @Override
    public List<Document> createDataAccessRequest(Document dataAccessRequest) throws MongoException {
        List<Document> dataAccessList = new ArrayList<>();
        List<String> dataSets =  dataAccessRequest.get(DATA_SET_ID, List.class);
        dataAccessRequest.remove(DATA_SET_ID);
        if (CollectionUtils.isNotEmpty(dataSets)) {
            Set<ConsentDataSet> consentDataSets = consentDAO.getConsentIdAndDataSets(dataSets);
            consentDataSets.forEach((consentDataSet) -> {
                Document dataAccess = processDataSet(dataAccessRequest, consentDataSet);
                dataAccessList.add(dataAccess);
            });
        }
        insertDataAccess(dataAccessList);
        return dataAccessList;
    }


    @Override
    public Document describeDataAccessRequestById(String id) throws NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        return mongo.getDataAccessRequestCollection().find(query).first();
    }


    @Override
    public void deleteDataAccessRequestById(String id) {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);

    }


    @Override
    public Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        Document dar = mongo.getDataAccessRequestCollection().find(query).first();
        Document result = new Document();
        for (String field : fields) {
            if(field.equals("datasetId")){
                List<String> dataSets = dar.get(field, List.class);
                result.append(field, dataSets);
            }else{
                String content = (String) dar.getOrDefault(field.replaceAll("\\s", ""), "Not found");
                result.append(field, content);
            }
        }
        return result;
    }

    @Override
    public List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds) {
        List<Document> response = new ArrayList<>();
        for (String datasetId : dataSetIds) {
            response.addAll(mongo.getDataAccessRequestCollection().find(eq("datasetId", datasetId)).into(new ArrayList<>()));
        }
        return response;
    }

    @Override
    public List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId) {
        FindIterable<Document> accessList = userId == null ? mongo.getDataAccessRequestCollection().find().sort(new BasicDBObject("sortDate", -1))
                : mongo.getDataAccessRequestCollection().find(new BasicDBObject("userId", userId)).sort(new BasicDBObject("sortDate", -1));
        List<DataAccessRequestManage> darManage = new ArrayList<>();
        List<String> accessRequestIds = getRequestIds(accessList);
        if (CollectionUtils.isNotEmpty(accessRequestIds)) {
            List<Election> electionList = new ArrayList<>();
            electionList.addAll(electionDAO.findLastElectionsByReferenceIdsAndType(accessRequestIds, 1));
            HashMap electionAccessMap = createAccessRequestElectionMap(electionList);
            darManage.addAll(createAccessRequestManage(accessList, electionAccessMap));
        }
        return darManage;
    }

    @Override
    public List<Document> describeDataAccessRequests() {
        return mongo.getDataAccessRequestCollection().find().into(new ArrayList<>());
    }

    @Override
    public UseRestriction createStructuredResearchPurpose(Document document) {
        return converter.parseJsonFormulary(document.toJson());
    }

    @Override
    public void deleteDataAccessRequest(Document document) {
        BasicDBObject query = new BasicDBObject("_id", document.get("_id"));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }

    @Override
    public Document updateDataAccessRequest(Document dataAccessRequest, String id) throws MongoException {
        BasicDBObject query = new BasicDBObject("dar_code", id);
        dataAccessRequest.remove("_id");
        dataAccessRequest.put("sortDate", new Date());
        if (mongo.getDataAccessRequestCollection().findOneAndReplace(query, dataAccessRequest) == null) {
            throw new NotFoundException("Data access for the specified id does not exist");
        }
        return mongo.getDataAccessRequestCollection().find(query).first();
    }

    @Override
    public Integer getTotalUnReviewedDAR() {
        FindIterable<Document> accessList = mongo.getDataAccessRequestCollection().find();
        Integer unReviewedDAR = 0;
        List<String> accessRequestIds = getRequestIds(accessList);
        if (CollectionUtils.isNotEmpty(accessRequestIds)) {
            List<Election> electionList = new ArrayList<>();
            electionList.addAll(electionDAO.findLastElectionsByReferenceIdsAndType(accessRequestIds, 1));
            HashMap<String, Election> electionAccessMap = createAccessRequestElectionMap(electionList);
            for (Document dar : accessList) {
                ObjectId id = dar.get("_id", ObjectId.class);
                Election election = electionAccessMap.get(id.toString());
                if (election == null) ++unReviewedDAR;
            }
        }
        return unReviewedDAR;
    }

    private void insertDataAccess(List<Document> dataAccessRequestList) {
        if(CollectionUtils.isNotEmpty(dataAccessRequestList)){
            String seq = mongo.getNextSequence("dar_code_counter");
            if (dataAccessRequestList.size() > 1) {
                IntStream.range(0, dataAccessRequestList.size())
                        .forEach(idx -> {
                                    dataAccessRequestList.get(idx).append("dar_code", "DAR-" + seq + SUFFIX + idx);
                                    dataAccessRequestList.get(idx).remove("_id");;
                                }

                        );
                mongo.getDataAccessRequestCollection().insertMany(dataAccessRequestList);
            }else{
                dataAccessRequestList.get(0).append("dar_code", "DAR-" + seq);
                mongo.getDataAccessRequestCollection().insertMany(dataAccessRequestList);
            }

        }

    }

    private List<DataAccessRequestManage> createAccessRequestManage(FindIterable<Document> documents, Map<String, Election> electionList) {
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        documents.forEach((Block<Document>) dar -> {
            DataAccessRequestManage darManage = new DataAccessRequestManage();
            ObjectId id = dar.get("_id", ObjectId.class);
            Election election = electionList.get(id.toString());
            darManage.setCreateDate(new Timestamp((long) id.getTimestamp() * 1000));
            darManage.setRus(dar.getString("rus"));
            darManage.setProjectTitle(dar.getString("projectTitle"));
            darManage.setDataRequestId(id.toString());
            darManage.setFrontEndId(dar.get("dar_code").toString());
            darManage.setSortDate(dar.getDate("sortDate"));
            if (election == null) {
                darManage.setElectionStatus(UN_REVIEWED);
            } else {
                darManage.setElectionId(election.getElectionId());
                darManage.setElectionStatus(election.getStatus());
                darManage.setElectionVote(election.getFinalVote());
            }
            requestsManage.add(darManage);
        });
        return requestsManage;
    }

    private List getRequestIds(FindIterable<Document> access) {
        List<String> accessIds = new ArrayList<>();
        if (access != null) {
            access.forEach((Block<Document>) document -> {
                accessIds.add(document.get("_id").toString());
            });
        }
        return accessIds;
    }

    private HashMap createAccessRequestElectionMap(List<Election> elections) {
        HashMap electionMap = new HashMap<>();
        elections.forEach(election -> {
            electionMap.put(election.getReferenceId(), election);
        });
        return electionMap;
    }

    private Document processDataSet(Document dataAccessRequest, ConsentDataSet consentDataSet) {
        List<Document> dataSetList = new ArrayList<>();
        List<String> datasetId = new ArrayList<>();
        Document dataAccess = new Document(dataAccessRequest);
        consentDataSet.getDataSets().forEach((k,v) -> {
            Document document = new Document();
            document.put(DATA_SET_ID,k);
            datasetId.add(k);
            document.put("name", v);
            dataSetList.add(document);
        });
        dataAccess.put("datasetId", datasetId);
        dataAccess.put("datasetDetail",dataSetList);
        return dataAccess;
    }
}

