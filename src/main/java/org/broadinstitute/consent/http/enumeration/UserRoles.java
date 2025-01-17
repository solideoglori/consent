package org.broadinstitute.consent.http.enumeration;

import org.broadinstitute.consent.http.resources.Resource;

public enum UserRoles {

    MEMBER(Resource.MEMBER, 1),
    CHAIRPERSON(Resource.CHAIRPERSON, 2),
    ALUMNI(Resource.ALUMNI, 3),
    ADMIN(Resource.ADMIN, 4),
    RESEARCHER(Resource.RESEARCHER, 5),
    DATAOWNER(Resource.DATAOWNER, 6);

    private String roleName;
    private Integer roleId;

    UserRoles(String roleName, Integer roleId) {
        this.roleName = roleName;
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public static UserRoles getUserRoleFromName(String value) {
        for (UserRoles e : UserRoles.values()) {
            if (e.getRoleName().equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }

}
