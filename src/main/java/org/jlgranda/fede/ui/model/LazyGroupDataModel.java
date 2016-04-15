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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jpapi.util.QuerySortOrder;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.api.UnsupportedCriterium;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.IdentityQueryBuilder;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
public class LazyGroupDataModel extends LazyDataModel<Group> implements Serializable {

    private static final int MAX_RESULTS = 5;
    Logger logger = LoggerFactory.getLogger(LazyGroupDataModel.class);
    IdentityManager identityManager = null;
    @Resource
    private UserTransaction userTransaction;
    private int firstResult = 0;
    private List<Group> resultList;
    private String filterValue;
    private String tags;

    public LazyGroupDataModel(IdentityManager identityManager) {
        setPageSize(MAX_RESULTS);
        this.identityManager = identityManager;
        resultList = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
    }

    public List<Group> getResultList() {
        logger.info("load BussinesEntitys");

        if (resultList.isEmpty()/* && getSelectedBussinesEntity() != null*/) {
            IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
            IdentityQuery<Group> query = queryBuilder.createIdentityQuery(Group.class);
            return query.getResultList();
        }
        return resultList;
    }

    public int getNextFirstResult() {
        return firstResult + this.getPageSize();
    }

    public int getPreviousFirstResult() {
        return this.getPageSize() >= firstResult ? 0 : firstResult - this.getPageSize();
    }

    public Integer getFirstResult() {
        return firstResult;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    public void setFirstResult(Integer firstResult) {
        logger.info("set first result + firstResult");
        this.firstResult = firstResult;
        this.resultList = null;
    }

    public boolean isPreviousExists() {
        return firstResult > 0;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isNextExists() {
        IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
        IdentityQuery<Group> query = queryBuilder.createIdentityQuery(Group.class)
                .where(queryBuilder.equal(Group.NAME, "fede"));
        return query.getResultCount() > this.getPageSize() + firstResult;

    }

    @Override
    public Group getRowData(String rowKey) {
        return BasicModel.getGroup(this.identityManager, rowKey);
    }

    @Override
    public Object getRowKey(Group entity) {
        System.err.println("//--> getRowKey:entity" + entity);
        return entity.getName();
    }

    public List<Group> find(int first, int end, String sortField, QuerySortOrder order, Map<String, Object> _filters)
            throws UnsupportedCriterium, IdentityException {
        try {
            IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
            IdentityQuery<org.picketlink.idm.model.basic.Group> query = queryBuilder.createIdentityQuery(org.picketlink.idm.model.basic.Group.class);
            return query.getResultList();
        } catch (IdentityManagementException |
                SecurityException | IllegalStateException e) {
            try {
                this.userTransaction.rollback();
            } catch (SystemException ignore) {
            }
            throw new RuntimeException("Could not create default security entities.", e);
        }
    }

    @Override
    public List<Group> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {

        try {
            return find(first, first, sortField, QuerySortOrder.ASC, filters);
        } catch (UnsupportedCriterium ex) {
            java.util.logging.Logger.getLogger(LazyGroupDataModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IdentityException ex) {
            java.util.logging.Logger.getLogger(LazyGroupDataModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

}
