/*
 * Copyright (C) 2022 usuario
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

import com.jlgranda.fede.ejb.production.AggregationService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.jlgranda.fede.model.production.Aggregation;
import org.jlgranda.fede.model.production.Aggregation_;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Organization;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author usuario
 */
public class LazyAggregationDataModel extends LazyDataModel<Aggregation> implements Serializable {

    private static final int MAX_RESULTS = 5;
    private static final long serialVersionUID = -3989947588787365293L;

    Logger logger = LoggerFactory.getLogger(LazyAggregationDataModel.class);

    private AggregationService aggregationService;

    private Subject owner;
    private Organization organization;

    private List<Aggregation> resultList;

    private int firstResult = 0;
    private String filterValue;
    private String tags;
    private Date start;
    private Date end;

    private BussinesEntity[] selectedBussinesEntities;
    private BussinesEntity selectedBussinesEntity; //Filtro de cuenta schema

    public LazyAggregationDataModel(AggregationService aggregationService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.aggregationService = aggregationService;
    }

    @PostConstruct
    public void init() {

    }

    public int getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(Integer firstResult) {
        logger.info("set first result + firstResult");
        this.firstResult = firstResult;
        this.resultList = null;
    }

    public List<Aggregation> getResultList() {
        logger.info("load BussinesEntitys");
        if (resultList.isEmpty()) {
            resultList = aggregationService.find(this.getPageSize(), this.getFirstResult());
        }
        return resultList;
    }

    public int getNextFirstResult() {
        return firstResult + this.getPageSize();
    }

    public int getPreviousFirstResult() {
        return this.getPageSize() >= firstResult ? 0 : firstResult - this.getPageSize();
    }

    public boolean isPreviousExists() {
        return firstResult > 0;
    }

    public boolean isNextExists() {
        return aggregationService.count() > this.getPageSize() + firstResult;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    @Override
    public Aggregation getRowData(String rowKey) {
        return aggregationService.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(Aggregation entity) {
        return "" + entity.getId();
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Subject getOwner() {
        return owner;
    }

    public void setOwner(Subject owner) {
        this.owner = owner;
    }

    public BussinesEntity[] getSelectedBussinesEntities() {
        return selectedBussinesEntities;
    }

    public void setSelectedBussinesEntities(BussinesEntity[] selectedBussinesEntities) {
        this.selectedBussinesEntities = selectedBussinesEntities;
    }

    public BussinesEntity getSelectedBussinesEntity() {
        return selectedBussinesEntity;
    }

    public void setSelectedBussinesEntity(BussinesEntity selectedBussinesEntity) {
        this.selectedBussinesEntity = selectedBussinesEntity;
    }

    @Override
    public List<Aggregation> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

        int _end = first + pageSize;
        String sortField = null;
        QuerySortOrder order = QuerySortOrder.DESC;
        if (!sortBy.isEmpty()) {
            for (SortMeta sm : sortBy.values()) {
                if (sm.getOrder() == SortOrder.ASCENDING) {
                    order = QuerySortOrder.ASC;
                }
                sortField = sm.getField(); //TODO ver mejor manera de aprovechar el mapa de orden
            }
        }
        Map<String, Object> _filters = new HashMap<>();
        Map<String, Date> range = new HashMap<>();
        if (getStart() != null) {
            range.put("start", getStart());
            if (getEnd() != null) {
                range.put("end", getEnd());
            } else {
                range.put("end", Dates.now());
            }
        }
        if (!range.isEmpty()) {
            _filters.put(Aggregation_.createdOn.getName(), range); //Filtro de fecha inicial
        }
        if (getOwner() != null) {
            _filters.put(Aggregation_.owner.getName(), getOwner());
        }
        if (getOrganization() != null) {
            _filters.put(Aggregation_.organization.getName(), getOrganization()); //Filtro por  defecto organization
        }
        if (getTags() != null && !getTags().isEmpty()) {
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        if (getFilterValue() != null && !getFilterValue().isEmpty()) {
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        _filters.putAll(filters);

        if (sortField == null) {
            sortField = Aggregation_.createdOn.getName();
        }

        QueryData<Aggregation> qData = aggregationService.find(first, _end, sortField, order, _filters);
        this.setRowCount(qData.getTotalResultCount().intValue());
        return qData.getResult();
    }

    @Override
    public int count(Map<String, FilterMeta> filters) {
        Map<String, Object> _filters = new HashMap<>();
        Map<String, Date> range = new HashMap<>();
        if (getStart() != null) {
            range.put("start", getStart());
            if (getEnd() != null) {
                range.put("end", getEnd());
            } else {
                range.put("end", Dates.now());
            }
        }
        if (!range.isEmpty()) {
            _filters.put(Aggregation_.createdOn.getName(), range); //Filtro de fecha inicial
        }

        if (getOwner() != null) {
            _filters.put(Aggregation_.owner.getName(), getOwner());
        }

        if (getOrganization() != null) {
            _filters.put(Aggregation_.organization.getName(), getOrganization()); //Filtro por  defecto organization
        }

        if (getTags() != null && !getTags().isEmpty()) {
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }

        if (getFilterValue() != null && !getFilterValue().isEmpty()) {
            _filters.put("keyword", getFilterValue()); //Filtro general
        }

        _filters.putAll(filters);

        QueryData<Aggregation> qData = aggregationService.find(_filters);
        return qData.getTotalResultCount().intValue();
    }

}
