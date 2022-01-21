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

import com.jlgranda.fede.ejb.reportes.ReporteService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.jlgranda.fede.model.reportes.Reporte;
import org.jlgranda.fede.model.reportes.Reporte_;
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
public class LazyReportDataModel extends LazyDataModel<Reporte> implements Serializable {

    private static final int MAX_RESULTS = 5;

    Logger logger = LoggerFactory.getLogger(LazyReportDataModel.class);

    private ReporteService reporteService;

    private Subject owner;
    private Organization organization;

    private List<Reporte> resultList;

    private int firstResult = 0;
    private String filterValue;
    private String tags;
    private Date start;
    private Date end;

    private BussinesEntity[] selectedBussinesEntities;
    private BussinesEntity selectedBussinesEntity; //Filtro de cuenta schema

    public LazyReportDataModel(ReporteService reporteService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.reporteService = reporteService;
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

    public List<Reporte> getResultList() {
        logger.info("load BussinesEntitys");
        if (resultList.isEmpty()) {
            resultList = reporteService.find(this.getPageSize(), this.getFirstResult());
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
        return reporteService.count() > this.getPageSize() + firstResult;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    @Override
    public Reporte getRowData(String rowKey) {
        return reporteService.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(Reporte entity) {
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

    @Override
    public List<Reporte> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

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
            _filters.put(Reporte_.createdOn.getName(), range); //Filtro de fecha inicial
        }
        if (getOwner() != null) {
            _filters.put(Reporte_.owner.getName(), getOwner());
        }
        if (getOrganization() != null) {
            _filters.put(Reporte_.organization.getName(), getOrganization()); //Filtro por  defecto organization
        }
        if (getTags() != null && !getTags().isEmpty()) {
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        if (getFilterValue() != null && !getFilterValue().isEmpty()) {
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        _filters.putAll(filters);

        if (sortField == null) {
            sortField = Reporte_.createdOn.getName();
        }

        QueryData<Reporte> qData = reporteService.find(first, _end, sortField, order, _filters);
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
            _filters.put(Reporte_.createdOn.getName(), range); //Filtro de fecha inicial
        }

        if (getOwner() != null) {
            _filters.put(Reporte_.owner.getName(), getOwner());
        }

        if (getOrganization() != null) {
            _filters.put(Reporte_.organization.getName(), getOrganization()); //Filtro por  defecto organization
        }

        if (getTags() != null && !getTags().isEmpty()) {
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }

        if (getFilterValue() != null && !getFilterValue().isEmpty()) {
            _filters.put("keyword", getFilterValue()); //Filtro general
        }

        _filters.putAll(filters);

        QueryData<Reporte> qData = reporteService.find(_filters);
        return qData.getTotalResultCount().intValue();
    }

}
