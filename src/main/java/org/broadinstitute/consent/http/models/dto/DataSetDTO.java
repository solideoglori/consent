package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.util.DatasetUtil;

import java.util.List;


public class DataSetDTO {

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private String consentId;

    @JsonProperty
    private String translatedUseRestriction;

    @JsonProperty
    private Boolean deletable;

    @JsonProperty
    private List<DataSetPropertyDTO> properties;

    @JsonProperty
    private Boolean active;

    @JsonProperty
    private Boolean needsApproval;

    @JsonProperty
    private Boolean isAssociatedToDataOwners;

    @JsonProperty
    private Boolean updateAssociationToDataOwnerAllowed;

    @JsonProperty
    private String alias;

    public DataSetDTO() {
    }

    public DataSetDTO(List<DataSetPropertyDTO> properties) {
        this.properties= properties;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getTranslatedUseRestriction() {
        return translatedUseRestriction;
    }

    public void setTranslatedUseRestriction(String translatedUseRestriction) {
        this.translatedUseRestriction = translatedUseRestriction;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public String getPropertyValue(String propertyName){
        return properties.get(properties.indexOf(new DataSetPropertyDTO(propertyName, ""))).getPropertyValue();
    }

    public List<DataSetPropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(List<DataSetPropertyDTO> properties) {
        this.properties = properties;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getNeedsApproval() {
        return needsApproval;
    }

    public void setNeedsApproval(Boolean needsApproval) {
        this.needsApproval = needsApproval;
    }

    public Boolean getIsAssociatedToDataOwners() {
        return isAssociatedToDataOwners;
    }

    public void setIsAssociatedToDataOwners(Boolean isAssociatedToDataOwners) {
        this.isAssociatedToDataOwners = isAssociatedToDataOwners;
    }

    public Boolean getUpdateAssociationToDataOwnerAllowed() {
        return updateAssociationToDataOwnerAllowed;
    }

    public void setUpdateAssociationToDataOwnerAllowed(Boolean updateAssociationToDataOwnerAllowed) {
        this.updateAssociationToDataOwnerAllowed = updateAssociationToDataOwnerAllowed;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setAlias(Integer alias) {
        this.alias = DatasetUtil.parseAlias(alias);
    }

    public String getAlias(){
        return alias;
    }
}
