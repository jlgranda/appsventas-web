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

import com.jlgranda.fede.ejb.SubjectService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.BussinesEntityType;
import org.jpapi.model.profile.Subject;
import org.jpapi.model.profile.Subject_;
import org.jpapi.util.Dates;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
public class LazySubjectDataModel extends LazyDataModel<Subject> implements Serializable {

    private static final int MAX_RESULTS = 5;
    private static final long serialVersionUID = -7690992762128327866L;
    Logger logger = LoggerFactory.getLogger(LazySubjectDataModel.class);
    private SubjectService service;
    private List<Subject> resultList;
    private int firstResult = 0;

    private BussinesEntityType type;

    private Subject author;

    private Subject owner;
    /**
     * Lista de etiquetas para filtrar facturas
     */
    private String tags;

    /**
     * Inicio del rango de fecha
     */
    private Date start;

    /**
     * Fin del rango de fecha
     */
    private Date end;

    private String typeName;

    private BussinesEntity[] selectedBussinesEntities;

    private BussinesEntity selectedBussinesEntity; //Filtro de cuenta schema

    private String filterValue;

    public LazySubjectDataModel(SubjectService bussinesEntityService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.service = bussinesEntityService;
    }

    @PostConstruct
    public void init() {
    }

    public List<Subject> getResultList() {

        if (resultList.isEmpty()/* && getSelectedBussinesEntity() != null*/) {
            resultList = service.find(this.getPageSize(), this.getFirstResult());
        }
        return resultList;
    }

    public int getFirstResult() {
        return firstResult;
    }

    public void setResultList(List<Subject> resultList) {
        this.resultList = resultList;
    }

    public int getNextFirstResult() {
        return firstResult + this.getPageSize();
    }

    public int getPreviousFirstResult() {
        return this.getPageSize() >= firstResult ? 0 : firstResult - this.getPageSize();
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
        this.resultList = null;;
    }

    public boolean isPreviousExists() {
        return firstResult > 0;
    }

    public boolean isNextExists() {
        return service.count() > this.getPageSize() + firstResult;
    }

    @Override
    public Subject getRowData(String rowKey) {
        return service.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(Subject entity) {
        return entity.getName();
    }

    @Override
    public List<Subject> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

        int _end = first + pageSize;
        String sortField = null;
        QuerySortOrder order = QuerySortOrder.DESC;
        if (!sortBy.isEmpty()){
            for (SortMeta sm : sortBy.values()){
                if ( sm.getOrder() == SortOrder.ASCENDING) {
                    order = QuerySortOrder.ASC;
                }
                sortField = sm.getField(); //TODO ver mejor manera de aprovechar el mapa de orden
            }
        }
        Map<String, Object> _filters = new HashMap<>();
        
        Map<String, Date> range = new HashMap<>();
        if (getStart() != null){
            range.put("start", getStart());
            if (getEnd() != null){
                range.put("end", getEnd());
            } else {
                range.put("end", Dates.now());
            }
        }
        if (!range.isEmpty()){
            _filters.put(Subject_.createdOn.getName(), range); //Filtro de fecha de creación
        }
        
        //_filters.put(BussinesEntity_.type.getName(), getType()); //Filtro por defecto
        if (null != getOwner())
            _filters.put(Subject_.owner.getName(), getOwner()); //Filtro por defecto
        
        if (getTags() != null && !getTags().isEmpty()){
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        
        if (!Strings.isNullOrEmpty(getFilterValue())){
            Map<String, String> columns = new HashMap<>();
            columns.put(Subject_.name.getName(), getFilterValue()); //Filtro general
            columns.put(Subject_.firstname.getName(), getFilterValue()); //Filtro general
            columns.put(Subject_.surname.getName(), getFilterValue()); //Filtro general
            columns.put(Subject_.email.getName(), getFilterValue()); //Filtro general
            columns.put(Subject_.mobileNumber.getName(), getFilterValue()); //Filtro general
            
            _filters.put("search", columns);
        }

        _filters.putAll(filters);

        if (sortField == null) {
            sortField = Subject_.createdOn.getName();
        }

        QueryData<Subject> qData = service.find(first, _end, sortField, order, _filters);
        this.setRowCount(qData.getTotalResultCount().intValue());
        return qData.getResult();
    }

    public BussinesEntityType getType() {
        return type;
    }

    public void setType(BussinesEntityType type) {
        this.type = type;
    }

    public Subject getAuthor() {
        return author;
    }

    public void setAuthor(Subject author) {
        this.author = author;
    }

    public Subject getOwner() {
        return owner;
    }

    public void setOwner(Subject owner) {
        this.owner = owner;
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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
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

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }
}
