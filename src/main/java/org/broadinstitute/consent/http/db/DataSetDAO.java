package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.resources.Resource;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UseStringTemplate3StatementLocator
@RegisterMapper({DataSetMapper.class})
public interface DataSetDAO extends Transactional<DataSetDAO> {

    String CHAIRPERSON = Resource.CHAIRPERSON;

    @SqlQuery("select * from dataset where dataSetId = :dataSetId")
    DataSet findDataSetById(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select * from dataset where objectId = :objectId")
    DataSet findDataSetByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("select objectId from dataset where dataSetId = :dataSetId")
    String findObjectIdByDataSetId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select * from dataset where objectId = :objectId")
    Integer findDataSetIdByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("select * from dataset where dataSetId in (<dataSetIdList>) and needs_approval = true")
    List<DataSet> findNeedsApprovalDataSetByDataSetId(@BindIn("dataSetIdList") List<Integer> dataSetIdList);

    @SqlBatch("insert into dataset (name, createDate, objectId, active, alias) values (:name, :createDate, :objectId, :active, :alias)")
    void insertAll(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("update dataset set name = :name, createDate = :createDate, active = :active, alias = :alias where dataSetId = :dataSetId")
    void updateAll(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("update dataset set name = :name, active = :active, createDate = :createDate, alias = :alias where objectId = :objectId")
    void updateAllByObjectId(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("insert into datasetproperty (dataSetId, propertyKey, propertyValue, createDate )" +
            " values (:dataSetId, :propertyKey, :propertyValue, :createDate)")
    void insertDataSetsProperties(@BindBean List<DataSetProperty> dataSetPropertiesList);

    @SqlBatch("delete from datasetproperty where dataSetId = :dataSetId")
    void deleteDataSetsProperties(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlBatch("delete from dataset where dataSetId = :dataSetId")
    void deleteDataSets(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlUpdate("update dataset set active = null, name = null, createDate = null, needs_approval = 0 where dataSetId = :dataSetId")
    void logicalDataSetdelete(@Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("update dataset set active = :active where dataSetId = :dataSetId")
    void updateDataSetActive(@Bind("dataSetId") Integer dataSetId, @Bind("active") Boolean active);

    @SqlUpdate("update dataset set needs_approval = :needs_approval where dataSetId = :dataSetId")
    void updateDataSetNeedsApproval(@Bind("dataSetId") Integer dataSetId, @Bind("needs_approval") Boolean needs_approval);

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction " +
            "from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId and d.name is not null inner join dictionary k on k.keyId = dp.propertyKey " +
            "inner join consentassociations ca on ca.dataSetId = d.dataSetId inner join consents c on c.consentId = ca.consentId " +
            "order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSets();

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction " +
            "from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey " +
            "inner join consentassociations ca on ca.dataSetId = d.dataSetId inner join consents c on c.consentId = ca.consentId " +
            "where d.dataSetId = :dataSetId order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSetWithPropertiesByDataSetId(@Bind("dataSetId") Integer dataSetId);

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery(" select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction from dataset  d inner join datasetproperty dp on dp.dataSetId = d.dataSetId " +
            " inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.dataSetId = d.dataSetId inner join consents c on c.consentId = ca.consentId inner join election e on e.referenceId = ca.consentId " +
            " inner join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON  + "' inner join (SELECT referenceId,MAX(createDate) maxDate FROM election where status ='Closed' group by referenceId) ev on ev.maxDate = e.createDate " +
            " and ev.referenceId = e.referenceId and v.vote = true and d.active = true order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSetsForResearcher();

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction " +
            "from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey " +
            "inner join consentassociations ca on ca.dataSetId = d.dataSetId inner join consents c on c.consentId = ca.consentId " +
            "where d.dataSetId in (<dataSetIdList>) order by d.dataSetId, k.receiveOrder")
    Set<DataSetDTO> findDataSetsByReceiveOrder(@BindIn("dataSetIdList") List<Integer> dataSetIdList);


    @SqlQuery("select *  from dataset where objectId in (<objectIdList>) ")
    List<DataSet> searchDataSetsByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery("select ds.dataSetId  from dataset ds where ds.objectId in (<objectIdList>) ")
    List<Integer> searchDataSetsIdsByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @RegisterMapper({DictionaryMapper.class})
    @SqlQuery("SELECT * FROM dictionary d order by receiveOrder")
    List<Dictionary> getMappedFieldsOrderByReceiveOrder();

    @RegisterMapper({DictionaryMapper.class})
    @SqlQuery("SELECT * FROM dictionary d WHERE d.displayOrder is not null  order by displayOrder")
    List<Dictionary> getMappedFieldsOrderByDisplayOrder();

    @SqlQuery("SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>)")
    List<DataSet> getDataSetsForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery("SELECT * FROM dataset d WHERE d.name IN (<names>)")
    List<DataSet> getDataSetsForNameList(@BindIn("names") List<String> names);

    @SqlQuery("SELECT ds.* FROM consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId WHERE ca.consentId = :consentId")
    List<DataSet> getDataSetsForConsent(@Bind("consentId") String consentId);

    @RegisterMapper({AssociationMapper.class})
    @SqlQuery("SELECT * FROM consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.objectId IN (<objectIdList>)")
    List<Association> getAssociationsForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @RegisterMapper({AssociationMapper.class})
    @SqlQuery("SELECT * FROM consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.dataSetId IN (<dataSetIdList>)")
    List<Association> getAssociationsForDataSetIdList(@BindIn("dataSetIdList") List<Integer> dataSetIdList);

    @RegisterMapper({AutocompleteMapper.class})
    @SqlQuery("SELECT DISTINCT d.dataSetId as id, d.objectId as objId, CONCAT_WS(' | ', d.objectId, d.name, dsp.propertyValue, c.name) as concatenation FROM dataset d " +
            " inner join consentassociations ca on ca.dataSetId = d.dataSetId and d.active = true" +
            " inner join consents c on c.consentId = ca.consentId " +
            " inner join election e on e.referenceId = ca.consentId " +
            " inner join datasetproperty dsp on dsp.dataSetId = d.dataSetId and dsp.propertyKey IN (9) " +
            " inner join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON  +
            "'inner join (SELECT referenceId,MAX(createDate) maxDate FROM" +
            " election where status ='Closed' group by referenceId) ev on ev.maxDate = e.createDate and ev.referenceId = e.referenceId " +
            " and v.vote = true " +
            " and (d.objectId like concat('%',:partial,'%') " +
            " or d.name like concat('%',:partial,'%') " +
            " or dsp.propertyValue like concat('%',:partial,'%')" +
            " or c.name like concat('%',:partial,'%') )" +
            " order by d.dataSetId")
    List< Map<String, String>> getObjectIdsbyPartial(@Bind("partial") String partial);

    @RegisterMapper({AutocompleteMapper.class})
    @SqlQuery("SELECT DISTINCT d.dataSetId as id, d.objectId as objId, CONCAT_WS(' | ', d.objectId, d.name, dsp.propertyValue, c.name) as concatenation " +
            "FROM dataset d inner join datasetproperty dsp on dsp.dataSetId = d.dataSetId and dsp.propertyKey IN (9) " +
            "inner join consentassociations ca on ca.dataSetId = d.dataSetId and d.active = true " +
            "inner join consents c on c.consentId = ca.consentId " +
            "WHERE d.name = :name")
    List< Map<String, String>> getObjectIdsbyDataSetName(@Bind("name")String name);

    @SqlQuery("SELECT ca.associationId FROM consentassociations ca INNER JOIN dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.objectId = :objectId")
    Integer getConsentAssociationByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("SELECT ca.consentId FROM consentassociations ca INNER JOIN dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.dataSetId = :dataSetId")
    String getAssociatedConsentIdByDataSetId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("SELECT dataSetId FROM dataset WHERE name = :name")
    Integer getDataSetByName(@Bind("name") String name);

    @SqlQuery("select *  from dataset where name in (<names>) ")
    List<DataSet> searchDataSetsByNameList(@BindIn("names") List<String> names);

    @Mapper(BatchMapper.class)
    @SqlQuery("select dataSetId, name  from dataset where name in (<nameList>)")
    List<Map<String,Integer>> searchByNameIdList(@BindIn("nameList") List<String> nameList);

    @SqlQuery(" SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>) AND d.name is not null")
    List<DataSet> getDataSetsWithValidNameForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery("select *  from dataset where dataSetId in (<dataSetIds>) ")
    List<DataSet> searchDataSetsByIds(@BindIn("dataSetIds") List<Integer> dataSetIds);

    @SqlQuery("select MAX(alias) from dataset")
    Integer findLastAlias();

}
