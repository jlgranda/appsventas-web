/*
 * Copyright (C) 2016 jlgranda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.fede.controller.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.jpapi.util.QuerySortOrder;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.api.UnsupportedCriterium;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.Condition;
import org.picketlink.idm.query.IdentityQueryBuilder;
import org.picketlink.idm.query.IdentityQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de gestión de grupos de seguridades
 * @author jlgranda
 */
public class SecurityGroupService implements Serializable {

    Logger logger = LoggerFactory.getLogger(SecurityGroupService.class);
    private static final long serialVersionUID = -8856264241192917839L;
    private EntityManager entityManager;
    
    PartitionManager partitionManager;
    IdentityManager identityManager;
    RelationshipManager relationshipManager;

    public SecurityGroupService() {
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    public void setPartitionManager(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public IdentityManager getIdentityManager() {
        return identityManager;
    }

    public void setIdentityManager(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    public RelationshipManager getRelationshipManager() {
        return relationshipManager;
    }

    public void setRelationshipManager(RelationshipManager relationshipManager) {
        this.relationshipManager = relationshipManager;
    }

    //metodo count
    public long count() {

        return 0;
    }

    public Group findByName(final String name) throws IdentityException {
        return BasicModel.getGroup(identityManager, name);
    }
    
    public Group findByKey(final String key){
        Map<String, Object> _filters = new HashMap<>();
        _filters.put(Group.ID.toString(), key);
        List<Group> result = find(1, 2, Group.ID.toString(), QuerySortOrder.DESC, _filters);
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Group> find(int first, int end, String sortField, QuerySortOrder order, Map<String, Object> _filters) {

        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
        
        IdentityQuery<Group> query  =  queryBuilder.createIdentityQuery(Group.class);
        if (!_filters.isEmpty()){
            Condition condition = null;
            for (String key : _filters.keySet()){
                if (Group.NAME.toString().equalsIgnoreCase(key)){
                    condition = queryBuilder.like(Group.NAME, (String)_filters.get(key));
                } else if (Group.ID.toString().equalsIgnoreCase(key)){
                    condition = queryBuilder.equal(Group.ID, (String)_filters.get(key));
                } else if (Group.PATH.toString().equalsIgnoreCase(key)){
                    condition = queryBuilder.like(Group.PATH, (String)_filters.get(key));
                } 
            }
            query.where(condition);
        }
        List<Group> result = query.getResultList();
        return result.isEmpty() ? new ArrayList<Group>() : result;
    }
    public void removeGroup(Group g) throws IdentityException {    
        identityManager.remove(g);
    }
    
    public void associate(Group g, User u) throws IdentityException {
        Account a = identityManager.lookupById(Account.class, u.getId());
        BasicModel.addToGroup(relationshipManager, a, g);
    }

    public void disassociate(Group g, User u) throws IdentityException {
        Account a = identityManager.lookupById(Account.class, u.getId());
        BasicModel.removeFromGroup(relationshipManager, a, g);
    }

    public User findUser(String usr) throws IdentityException {
        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
        List<User> result = queryBuilder
                .createIdentityQuery(User.class)
                .where(queryBuilder.equal(User.LOGIN_NAME, usr))
                .getResultList();
       
        return result.isEmpty() ? null : result.get(0);
    }

//    Collection<Group> find(User user) throws IdentityException {
//        return relationshipManager.findAssociatedGroups(user);
//    }

//    boolean isAssociated(Group group, User user) throws IdentityException {
//        return relationshipManager.isAssociated(group, user);
//    }
//    
//    boolean isAssociatedUser(Group group) throws IdentityException {
//        boolean b = security.getRelationshipManager().findAssociatedUsers(group, true).isEmpty();
//        logger.info("Eqaula-->  valor de asociacion "+b);
//        return b;
//    }

    public List<Group> find(int pageSize, Integer firstResult) throws IdentityException, UnsupportedCriterium {
        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
        List<Group> result = queryBuilder.createIdentityQuery(Group.class).setLimit(firstResult).setLimit(pageSize).getResultList();
        return result.isEmpty() ? new ArrayList<Group>() : result;
    }
}
