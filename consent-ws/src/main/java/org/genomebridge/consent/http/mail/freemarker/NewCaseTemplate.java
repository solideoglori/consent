package org.genomebridge.consent.http.mail.freemarker;

public class NewCaseTemplate {

    private String electionType;

    private String entityId;

    private String serverUrl;

    public NewCaseTemplate(String election, String entityId, String serverUrl) {
        this.electionType = election;
        this.entityId = entityId;
        this.serverUrl = serverUrl;
    }

    public String getElectionType() {
        return electionType;
    }

    public void setElectionType(String electionType) {
        this.electionType = electionType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}