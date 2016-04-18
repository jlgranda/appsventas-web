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
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.jlgranda.fede.controller.security.SecurityGroupService;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.picketlink.idm.api.UnsupportedCriterium;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.model.basic.Group;
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

    private int firstResult = 0;
    private List<Group> resultList;
    private String filterValue;

    private SecurityGroupService securityGroupService;

    public LazyGroupDataModel(SecurityGroupService securityGroupService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.securityGroupService = securityGroupService;
    }

    @PostConstruct
    public void init() {
    }

    public List<Group> getResultList() {
        logger.info("load BussinesEntitys");

        if (resultList.isEmpty()/* && getSelectedBussinesEntity() != null*/) {
            try {
                resultList = securityGroupService.find(this.getPageSize(), this.getFirstResult());
            } catch (IdentityException ex) {
                java.util.logging.Logger.getLogger(LazyGroupDataModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedCriterium ex) {
                java.util.logging.Logger.getLogger(LazyGroupDataModel.class.getName()).log(Level.SEVERE, null, ex);
            }
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

    public boolean isNextExists() {
        return securityGroupService.count() > this.getPageSize() + firstResult;

    }

    @Override
    public Group getRowData(String rowKey) {

        return (Group) securityGroupService.findByKey(rowKey);

    }

    @Override
    public Object getRowKey(Group entity) {
        System.err.println("//--> getRowKey:entity" + entity);
        return entity.getName();
    }

    @Override
    public List<Group> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        List<Group> result = new ArrayList<>();

        int end = first + pageSize;
        QuerySortOrder order = QuerySortOrder.ASC;
        if (sortOrder == SortOrder.DESCENDING) {
            order = QuerySortOrder.DESC;
        }
        Map<String, Object> _filters = new HashMap<>();
        if (!Strings.isNullOrEmpty(getFilterValue())){
            _filters.put(Group.NAME.toString(), getFilterValue());
        }
        result = securityGroupService.find(first, end, sortField, order, _filters);
        this.setRowCount(result.size());  //importante para cargar datos 

        return result;
    }

}