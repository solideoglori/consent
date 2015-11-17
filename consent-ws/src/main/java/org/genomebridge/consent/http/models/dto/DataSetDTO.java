package org.genomebridge.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import java.util.List;


public class DataSetDTO {

    @JsonProperty
    private String consentId;

    @JsonProperty
    private UseRestriction useRestriction;

    @JsonProperty
    private Boolean deletable;

    @JsonProperty
    private List<DataSetPropertyDTO> properties;

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

    public UseRestriction getUseRestriction() {
        return useRestriction;
    }

    public void setUseRestriction(UseRestriction useRestriction) {
        this.useRestriction = useRestriction;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public List<DataSetPropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(List<DataSetPropertyDTO> properties) {
        this.properties = properties;
    }
}