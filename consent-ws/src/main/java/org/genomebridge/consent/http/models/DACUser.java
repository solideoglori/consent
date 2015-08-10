package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private List<DACUserRole> roles;

    @JsonProperty
    //will remove this after refactor
    private String memberStatus = "Admin";

    public DACUser() {
    }

    public DACUser(Integer dacUserId, String email, String displayName,
                   Date createDate) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
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

    public List<DACUserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<DACUserRole> roles) {
        this.roles = roles;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
