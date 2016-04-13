/*
 * Copyright (C) 2016 Jorge
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
package org.jlgranda.fede.ui.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.IdentityQueryBuilder;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 *
 * @author Jorge
 */
public class LazyGroupDataModel extends LazyDataModel<Group> implements Serializable {

    private List<Group> resultList;
    private String name;
    @Inject
    private PartitionManager partitionManager;
    @Resource
    private UserTransaction userTransaction;
    IdentityManager identityManager = null;
    private int firstResult = 0;
    private String filterValue;

    public LazyGroupDataModel() {
        resultList = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
    }

    public List<Group> getResultList() {
        if (resultList.isEmpty()) {
            identityManager = partitionManager.createIdentityManager();
//            resultList = find(this.name, this.identityManager);
        }
        return resultList;
    }

//    public QueryData<E> find(String name, IdentityManager identityManager) {
//        if (name == null) {
//            throw new IllegalArgumentException("Invalid login name.");
//        }
////         QueryData<E> queryData = new QueryData<>();
//        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
//        IdentityQuery<Group> query = queryBuilder.createIdentityQuery(Group.class);
//
//        query.where(queryBuilder.equal(Group.NAME, name));
//// queryData.setResult(query.getResultList());
////        Long totalResultCount = countquery.getSingleResult();
////        queryData.setTotalResultCount(totalResultCount);
//
//        return queryData;
////        return query.getResultList();
//    }

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

    @Override
//    public List<Group> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
//
//        int end = first + pageSize;
//
//        QuerySortOrder order = QuerySortOrder.DESC;
//        if (sortOrder == SortOrder.ASCENDING) {
//            order = QuerySortOrder.ASC;
//        }
//        Map<String, Object> _filters = new HashMap<>();
//        identityManager = partitionManager.createIdentityManager();
////        QueryData<Group> qData = find(name, identityManager);
//        this.setRowCount(qData.getTotalResultCount().intValue());
//        return qData.getResult();
//    }
}
