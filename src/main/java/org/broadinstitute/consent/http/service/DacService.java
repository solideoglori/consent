package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class DacService {

    private DacDAO dacDAO;

    @Inject
    public DacService(DacDAO dacDAO) {
        this.dacDAO = dacDAO;
    }

    public List<Dac> findAll() {
        return dacDAO.findAll();
    }

    public List<DACUser> findAllDACUsersBySearchString(String term) {
        return dacDAO.findAllDACUsersBySearchString(term).stream().distinct().collect(Collectors.toList());
    }

    /**
     * Retrieve a list of Dacs that contain a Dac, the list of chairperson users for the Dac, and a
     * list of member users for the Dac.
     *
     * @return List of Dac objects
     */
    public List<Dac> findAllDacsWithMembers() {
        List<Dac> dacs = dacDAO.findAll();
        List<DACUser> allDacMembers = dacDAO.findAllDACUserMemberships().stream().distinct().collect(Collectors.toList());
        Map<Dac, List<DACUser>> dacToUserMap = groupUsersByDacs(dacs, allDacMembers);
        return dacs.stream().peek(d -> {
            List<DACUser> chairs = dacToUserMap.get(d).stream().
                    filter(u -> u.getRoles().stream().
                            anyMatch(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) && ur.getDacId().equals(d.getDacId()))).
                    collect(Collectors.toList());
            List<DACUser> members = dacToUserMap.get(d).stream().
                    filter(u -> u.getRoles().stream().
                            anyMatch(ur -> ur.getRoleId().equals(UserRoles.MEMBER.getRoleId()) && ur.getDacId().equals(d.getDacId()))).
                    collect(Collectors.toList());
            d.setChairpersons(chairs);
            d.setMembers(members);
        }).collect(Collectors.toList());
    }

    /**
     * Convenience method to group DACUsers into their associated Dacs. Users can be in more than
     * a single Dac, and a Dac can have multiple types of users, either Chairpersons or Members.
     *
     * @param dacs List of all Dacs
     * @param allDacMembers List of all DACUsers, i.e. users that are in any Dac.
     * @return Map of Dac to list of DACUser
     */
    private Map<Dac, List<DACUser>> groupUsersByDacs(List<Dac> dacs, List<DACUser> allDacMembers) {
        Map<Integer, Dac> dacMap = dacs.stream().collect(Collectors.toMap(Dac::getDacId, d -> d));
        Map<Integer, DACUser> userMap = allDacMembers.stream().collect(Collectors.toMap(DACUser::getDacUserId, u -> u));
        Map<Dac, List<DACUser>> dacToUserMap = new HashMap<>();
        dacs.forEach(d -> dacToUserMap.put(d, new ArrayList<>()));
        allDacMembers.stream().
                flatMap(u -> u.getRoles().stream()).
                filter(ur -> ur.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) ||
                                ur.getRoleId().equals(UserRoles.MEMBER.getRoleId())).
                forEach(ur -> {
                    Dac d = dacMap.get(ur.getDacId());
                    DACUser u = userMap.get(ur.getUserId());
                    if (d != null && u != null && dacToUserMap.containsKey(d)) {
                        dacToUserMap.get(d).add(u);
                    }
                });
        return dacToUserMap;
    }

    public Dac findById(Integer dacId) {
        Dac dac = dacDAO.findById(dacId);
        List<DACUser> chairs = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.CHAIRPERSON.getRoleId());
        List<DACUser> members = dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId());
        dac.setChairpersons(chairs);
        dac.setMembers(members);
        return dac;
    }

    public Integer createDac(String name, String description) {
        Date createDate = new Date();
        return dacDAO.createDac(name, description, createDate);
    }

    public void updateDac(String name, String description, Integer dacId) {
        Date updateDate = new Date();
        dacDAO.updateDac(name, description, updateDate, dacId);
    }

    public void deleteDac(Integer dacId) {
        dacDAO.deleteDacMembers(dacId);
        dacDAO.deleteDac(dacId);
    }

    public DACUser findUserById(Integer id) throws IllegalArgumentException {
        return populatedUserById(id);
    }

    public List<DACUser> findMembersByDacId(Integer dacId) {
        List<DACUser> dacUsers = dacDAO.findMembersByDacId(dacId);
        List<Integer> allUserIds = dacUsers.
                stream().
                map(DACUser::getDacUserId).
                distinct().
                collect(Collectors.toList());
        Map<Integer, List<UserRole>> userRoleMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            userRoleMap.putAll(dacDAO.findUserRolesForUsers(allUserIds).
                    stream().
                    collect(groupingBy(UserRole::getUserId)));
        }
        dacUsers.forEach(u -> {
            if (userRoleMap.containsKey(u.getDacUserId())) {
                u.setRoles(userRoleMap.get(u.getDacUserId()));
            }
        });
        return dacUsers;
    }

    public DACUser addDacMember(Role role, DACUser user, Dac dac) {
        dacDAO.addDacMember(role.getRoleId(), user.getDacUserId(), dac.getDacId());
        return populatedUserById(user.getDacUserId());
    }

    public void removeDacMember(Role role, DACUser user, Dac dac) {
        List<UserRole> dacRoles = user.
                getRoles().
                stream().
                filter(r -> r.getDacId() != null).
                filter(r -> r.getDacId().equals(dac.getDacId())).
                filter(r -> r.getRoleId().equals(role.getRoleId())).
                collect(Collectors.toList());
        dacRoles.forEach(userRole -> dacDAO.removeDacMember(userRole.getUserRoleId()));
    }

    public Role getChairpersonRole() {
        return dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
    }

    public Role getMemberRole() {
        return dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
    }

    private DACUser populatedUserById(Integer userId) {
        DACUser user = dacDAO.findUserById(userId);
        user.setRoles(dacDAO.findUserRolesForUser(userId));
        return user;
    }

}
