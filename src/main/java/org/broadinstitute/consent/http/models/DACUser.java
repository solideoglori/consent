package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Date;
import java.util.List;

public class DACUser {

    @JsonProperty
    private Integer dacUserId;

    @JsonProperty
    private String email;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private String additionalEmail;

    @JsonProperty
    private List<UserRole> roles;

    @JsonProperty
    private Boolean emailPreference;

    @JsonProperty
    private String status;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private Boolean profileCompleted;

    public DACUser() {
    }

    public DACUser(Integer dacUserId, String email, String displayName, Date createDate) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
    }

    public DACUser(Integer dacUserId, String email, String displayName, Date createDate, String additionalEmail) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.additionalEmail = additionalEmail;
    }

    public DACUser(Integer dacUserId, String email, String displayName,
                   String isApproved, Date createDate, List<UserRole> roles, String additionalEmail) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.roles = roles;
        this.additionalEmail = additionalEmail;
    }

    /**
     * Convenience method for backwards compatibility support for older clients.
     *
     * @param json A json string that may or may not be correctly structured as a DACUser
     */
    public DACUser(String json) {
        GsonBuilder builder = new GsonBuilder();
        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>)
                (json1, typeOfT, context) -> new Date(json1.getAsJsonPrimitive().getAsLong()));
        Gson gson = builder.create();
        DACUser u = gson.fromJson(json, DACUser.class);
        if (u.getDacUserId() != null) {
            this.setDacUserId(u.getDacUserId());
        }
        if (!StringUtils.isEmpty(u.getEmail())) {
            this.setEmail(u.getEmail());
        }
        if (!StringUtils.isEmpty(u.getDisplayName())) {
            this.setDisplayName(u.getDisplayName());
        }
        if (u.getCreateDate() != null) {
            this.setCreateDate(u.getCreateDate());
        }
        if (!StringUtils.isEmpty(u.getAdditionalEmail())) {
            this.setAdditionalEmail(u.getAdditionalEmail());
        }
        if (u.getEmailPreference() != null) {
            this.setEmailPreference(u.getEmailPreference());
        }
        if (u.getRoles() != null && !u.getRoles().isEmpty()) {
            this.setRoles(u.getRoles());
        }
        if (!StringUtils.isEmpty(u.getStatus())) {
            this.setStatus(u.getStatus());
        }
        if (!StringUtils.isEmpty(u.getRationale())) {
            this.setRationale(u.getRationale());
        }
    }

    public Integer getDacUserId() {
        return dacUserId;
    }

    public void setDacUserId(Integer dacUserId) {
        this.dacUserId = dacUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRole> roles) {
        this.roles = roles;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getAdditionalEmail() {
        return additionalEmail;
    }

    public void setAdditionalEmail(String additionalEmail) {
        this.additionalEmail = additionalEmail;
    }

    public Boolean getEmailPreference() {
        return emailPreference;
    }

    public void setEmailPreference(Boolean emailPreference) {
        this.emailPreference = emailPreference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    @Override
    public int hashCode(){
        return  dacUserId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DACUser other = (DACUser) obj;
        return new EqualsBuilder().append(dacUserId, other.dacUserId).isEquals();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
