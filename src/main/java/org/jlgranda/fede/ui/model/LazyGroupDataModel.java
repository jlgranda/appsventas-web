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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.jpapi.util.QuerySortOrder;
import org.omnifaces.cdi.ViewScoped;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.api.UnsupportedCriterium;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.IdentityQueryBuilder;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
@Named
@ViewScoped
public class LazyGroupDataModel extends LazyDataModel<Group> {

    private static final int MAX_RESULTS = 5;
    Logger logger = LoggerFactory.getLogger(LazyGroupDataModel.class);
    IdentityManager identityManager = null;
    @Inject
    private PartitionManager partitionManager;
    @Resource
    private UserTransaction userTransaction;
    private int firstResult = 0;
    private Group[] selectedGroups;
    private Group selectedGroup;
    private List<Group> resultList;
    private String filterValue;
    private String tags;

    public LazyGroupDataModel() {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
    }

    public List<Group> find(int first, int end, String sortField, QuerySortOrder order, Map<String, Object> _filters)
            throws UnsupportedCriterium, IdentityException {
        try {
            identityManager = partitionManager.createIdentityManager();
            IdentityQueryBuilder queryBuilder = identityManager.getQueryBuilder();
            IdentityQuery<Group> query = queryBuilder.createIdentityQuery(Group.class);
            return query.getResultList();
        } catch (IdentityManagementException |
                SecurityException | IllegalStateException e) {
            throw new RuntimeException("Could not create default security entities.", e);
        }
    }

    public Group findName(String name) {
        identityManager = partitionManager.createIdentityManager();
        Group group = BasicModel.getGroup(this.identityManager, name);
        return group;
    }

    @Override
    public List<Group> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        List<Group> result = new ArrayList<Group>();
        try {
            result = find(first, first, sortField, QuerySortOrder.ASC, filters);
            this.resultList = result;
            this.setRowCount(result.size());
            return result;
        } catch (UnsupportedCriterium | IdentityException ex) {
            java.util.logging.Logger.getLogger(LazyGroupDataModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    public int getNextFirstResult() {
        return firstResult + this.getPageSize();
    }

    public int getPreviousFirstResult() {
        return this.getPageSize() >= firstResult ? 0 : firstResult - this.getPageSize();
    }

    public int getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    public Group getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(Group selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    public Group[] getSelectedGroups() {
        return selectedGroups;
    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
                setSelectedGroup(((Group) event.getObject()));
                context.redirect(context.getRequestContextPath() + "/pages/admin/security/group/group.jsf?groupKey=" + ((Group) event.getObject()).getName());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(LazyGroupDataModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        this.setSelectedGroup(null);
    }

    @Override
    public Group getRowData(String rowKey) {
        return findName(rowKey);

    }

    @Override
    public Object getRowKey(Group entity) {
        return entity.getName();
    }
}
