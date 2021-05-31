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
package org.jlgranda.fede.ui.model;

import com.jlgranda.fede.ejb.talentohumano.JournalService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.model.talentohumano.Journal;
import org.jlgranda.fede.model.talentohumano.Journal_;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.BussinesEntityType;
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
 * @author jlgranda
 */
public class LazyJournalDataModel  extends LazyDataModel<Journal> implements Serializable {
    
    private static final int MAX_RESULTS = 100;
    private static final long serialVersionUID = 4204366822715059908L;
    
    Logger  logger = LoggerFactory.getLogger(LazyJournalDataModel.class);

    private final JournalService bussinesEntityService; 
    
    private List<Journal> resultList;
    private int firstResult = 0;
    
    private BussinesEntityType type;
    
    private Subject owner;
    
    private Subject author;
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
    
    /**
     */
    private ProductType productType;
    
    private String typeName;
    private BussinesEntity[] selectedBussinesEntities;
    private BussinesEntity selectedBussinesEntity; //Filtro de cuenta schema
    
    private String filterValue;

    public LazyJournalDataModel(JournalService bussinesEntityService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.bussinesEntityService = bussinesEntityService;
    }

    @PostConstruct
    public void init() {
    }

    public List<Journal> getResultList() {
        logger.info("load BussinesEntitys");
        if (resultList.isEmpty()) {
            resultList = load(0, MAX_RESULTS, new HashMap<>(), null);
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

    public Subject getOwner() {
        return owner;
    }

    public Subject getAuthor() {
        return author;
    }

    public void setAuthor(Subject author) {
        this.author = author;
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

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    public BussinesEntityType getType() {
        //if (type == null){
        //   setType(bussinesEntityService.findBussinesEntityTypeByName(getTypeName()));
        //}
        return type;
    }

    public void setType(BussinesEntityType type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
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
        return bussinesEntityService.count() > this.getPageSize() + firstResult;
    }

    @Override
    public Journal getRowData(String rowKey) {
        return bussinesEntityService.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(Journal entity) {
        return "" + entity.getId();
    }

    @Override
    //public List<Journal> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
    public List<Journal> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

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
            _filters.put(Journal_.beginTime.getName(), range); //Filtro de fecha registro
        }
        if (getOwner() != null){
            _filters.put(Journal_.owner.getName(), getOwner()); //Filtro por defecto
        }
        if (getAuthor()!= null){
            _filters.put(Journal_.author.getName(), getAuthor()); //Filtro por defecto
        }
        
        if (getTags() != null && !getTags().isEmpty()){
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        
        if (getFilterValue() != null && !getFilterValue().isEmpty()){
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        
        if (filters != null)
            _filters.putAll(filters);
        
        if (sortField == null){
            sortField = Journal_.beginTime.getName(); //ordenar por fecha de registro
        }

        QueryData<Journal> qData = bussinesEntityService.find(first, _end, sortField, order, _filters);
        this.setRowCount(qData.getTotalResultCount().intValue());
        return qData.getResult();
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

}
