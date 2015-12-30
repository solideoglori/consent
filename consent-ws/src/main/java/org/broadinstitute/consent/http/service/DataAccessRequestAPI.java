package org.broadinstitute.consent.http.service;

import com.mongodb.MongoException;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.bson.Document;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.util.List;



public interface DataAccessRequestAPI {

    List<Document> createDataAccessRequest(Document dataAccessRequest) throws MongoException;

    Document describeDataAccessRequestById(String id) throws NotFoundException;

    List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds);

    Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException;

    List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId);

    List<Document> describeDataAccessRequests();

    UseRestriction createStructuredResearchPurpose(Document document);

    void deleteDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException;

    void deleteDataAccessRequestById(String id) throws IllegalArgumentException;

    Document updateDataAccessRequest(Document dar, String id);

    Integer getTotalUnReviewedDAR();

    // Partial Data Access Requests
    Document createPartialDataAccessRequest(Document dataAccessRequest) throws MongoException;

    List<Document> describePartialDataAccessRequests();

    Document describePartialDataAccessRequestById(String id) throws NotFoundException;

    void deletePartialDataAccessRequestById(String id) throws IllegalArgumentException;

    Document updatePartialDataAccessRequest(Document partialDar);

    List<Document> describePartialDataAccessRequestManage(Integer userId);

    Object getField(String requestId , String field);

    void setMongoDBInstance(MongoConsentDB mongo);

}

