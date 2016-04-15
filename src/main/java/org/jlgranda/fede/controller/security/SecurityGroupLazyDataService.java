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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.model.ListDataModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.IdentityQueryBuilder;
import org.primefaces.model.SelectableDataModel;

/**
 *
 * @author cesar
 */
public class SecurityGroupLazyDataService extends ListDataModel<Group> implements SelectableDataModel<Group> {

    private List<Group> resultList;

    public SecurityGroupLazyDataService() {
        resultList = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
    }

    public List<Group> getResultList() {
        return resultList;
    }

    public void setResultList(List<Group> resultList) {
        this.resultList = resultList;
    }

    public List<Group> find(IdentityManager identityManager) {
        if (identityManager == null) {
        }
        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
        IdentityQuery<Group> query = queryBuilder.createIdentityQuery(Group.class)
                .where(queryBuilder.equal(Group.NAME, "fede"));
        return query.getResultList();
    }

    @Override
    public Object getRowKey(Group entity) {
        return entity.getName();
    }

    @Override
    public Group getRowData(String rowKey) {

        for (Group group : getResultList()) {
            if (group.getName().equals(rowKey)) {
                return group;
            }
        }
        return null;
    }
//
//    @PostConstruct
//    public void init() {
//        securityGroupService.setEntityManager(em);
//    }
}
