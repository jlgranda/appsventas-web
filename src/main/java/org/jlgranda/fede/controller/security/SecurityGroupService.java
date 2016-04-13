/*
 * Copyright 2013 cesar.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jlgranda.fede.controller.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;
import org.jpapi.util.QuerySortOrder;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.api.Group;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.UnsupportedCriterium;
import org.picketlink.idm.common.exception.IdentityException;

/**
 *
 * @author cesar
 */
public class SecurityGroupService implements Serializable {

    private static final long serialVersionUID = -8856264241192917839L;
    @Inject
    private PartitionManager partitionManager;
    @Resource
    private UserTransaction userTransaction; //https://issues.jboss.org/browse/PLINK-332

    IdentityManager identityManager = null;
//

    public SecurityGroupService() {
    }

   
//
//    //metodo count
//    public long count() {
//        try {
//            return (long) security.getPersistenceManager().getGroupTypeCount("GROUP");
//        } catch (IdentityException ex) {
//            Logger.getLogger(SecurityGroupService.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return 0;
//    }
//

//    public Group getGroupById(final Long id) throws IdentityException {
////        Group g = security.getPersistenceManager().findGroupByKey(String.valueOf(id));
////
////        return g;
//
//    }
//
//    public Group findByName(final String name) throws IdentityException {
//
//        return (Group) security.getPersistenceManager().findGroup(name, "GROUP");
//    }
//
//    public Group findByKey(final String key) throws IdentityException {
//        return security.getPersistenceManager().findGroupByKey(key);
//    }
//

    public List<Group> find(int first, int end, String sortField, QuerySortOrder order, Map<String, Object> _filters) throws UnsupportedCriterium, IdentityException, RuntimeException {
        List<Group> groups = new ArrayList<>();
//        try {
        //        try {
//            identityManager = partitionManager.createIdentityManager();
//            this.userTransaction.begin();
//
//            Group group = BasicModel.getGroup(identityManager, "fede");
//            groups.add(group);
//        query.setParameter(User.ID, id);
//            
//            IdentitySearchCriteriaImpl identitySearchCriteria = new IdentitySearchCriteriaImpl();
//            identitySearchCriteria.sort(SortOrder.ASCENDING);
//            if (QuerySortOrder.DESC.equals(order)) {
//                identitySearchCriteria.sort(SortOrder.DESCENDING);
//            }
//            identitySearchCriteria.sortAttributeName(sortField);
//            identitySearchCriteria.setPaged(true);
//            identitySearchCriteria.page(first, end);
//            String[] values = new String[1];
//            for (Map.Entry entry : _filters.entrySet()) {
//                values[1] = (String) entry.getValue();
//                identitySearchCriteria.attributeValuesFilter((String) entry.getKey(), values);
//            }
//
//        IdentityQuery<Group> query = partitionManager.cr
//        List<Group> tem = new ArrayList<Group>(security.getPersistenceManager().findGroup("GROUP", identitySearchCriteria));
//        return tem;
//        } catch (NotSupportedException ex) {
//            Logger.getLogger(SecurityGroupService.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (SystemException ex) {
//            Logger.getLogger(SecurityGroupService.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return groups;
    }

//    public void removeGroup(Group g) throws IdentityException {
//        security.getPersistenceManager().removeGroup(g, true);
//    }
//
//    public void associate(Group g, User u) throws IdentityException {
//        security.getRelationshipManager().associateUser(g, u);
//    }
//
//    public void disassociate(Group g, User u) throws IdentityException {
//        Collection<User> listUser = new ArrayList<User>();
//        listUser.add(u);
//        security.getRelationshipManager().disassociateUsers(g, listUser);
//    }
//
//    public User findUser(String usr) throws IdentityException {
//        return security.getPersistenceManager().findUser(usr);
//    }
//
//    Collection<Group> find(User user) throws IdentityException {
//        return security.getRelationshipManager().findAssociatedGroups(user);
//    }
//
//    boolean isAssociated(Group group, User user) throws IdentityException {
//        return security.getRelationshipManager().isAssociated(group, user);
//    }
//
//    boolean isAssociatedUser(Group group) throws IdentityException {
//        boolean b = security.getRelationshipManager().findAssociatedUsers(group, true).isEmpty();
//        return b;
//    }
}
