/*
 * Copyright (C) 2015 jlgranda
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jlgranda.fede.ui.model;

import com.jlgranda.fede.ejb.gifts.GiftService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.jlgranda.fede.model.gifts.GiftEntity;
import org.jlgranda.fede.model.gifts.GiftEntity_;
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
public class LazyGiftDataModel extends LazyDataModel<GiftEntity> implements Serializable {

    private static final int MAX_RESULTS = 5;
    private static final long serialVersionUID = 201837221989669238L;
    
    Logger  logger = LoggerFactory.getLogger(LazyGiftDataModel.class);

    private GiftService giftEntityService;
    
    private List<GiftEntity> resultList;
    private int firstResult = 0;
    
    private BussinesEntityType type;
    
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
    private GiftEntity[] selectedGiftEntities;
    private GiftEntity selectedGiftEntity; //Filtro de cuenta schema
    
    private String filterValue;

    public LazyGiftDataModel(GiftService giftEntityService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.giftEntityService = giftEntityService;
    }

    @PostConstruct
    public void init() {
    }

    public List<GiftEntity> getResultList() {
        if (resultList.isEmpty()/* && getSelectedBussinesEntity() != null*/) {
            resultList = giftEntityService.find(this.getPageSize(), this.getFirstResult());
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
        return giftEntityService.count() > this.getPageSize() + firstResult;
    }

    
    @Override
    public GiftEntity getRowData(String rowKey) {
        return giftEntityService.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(GiftEntity entity) {
        return entity.getName();
    }

    @Override
    public List<GiftEntity> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

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
            _filters.put(GiftEntity_.createdOn.getName(), range); //Filtro de fecha inicial
        }
        
        _filters.put(GiftEntity_.owner.getName(), getOwner()); //Filtro por defecto
        
        if (getTags() != null && !getTags().isEmpty()){
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        
        if (getFilterValue() != null && !getFilterValue().isEmpty()){
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        
        _filters.putAll(filters);
        
        if (sortField == null){
            sortField = GiftEntity_.createdOn.getName();
        }

        QueryData<GiftEntity> qData = giftEntityService.find(first, _end, sortField, order, _filters);
        this.setRowCount(qData.getTotalResultCount().intValue());
        return qData.getResult();
    }

    public GiftEntity[] getSelectedGiftEntities() {
        return selectedGiftEntities;
    }

    public void setSelectedGiftEntities(GiftEntity[] selectedGiftEntities) {
        this.selectedGiftEntities = selectedGiftEntities;
    }

    public GiftEntity getSelectedGiftEntity() {
        return selectedGiftEntity;
    }

    public void setSelectedGiftEntity(GiftEntity selectedGiftEntity) {
        this.selectedGiftEntity = selectedGiftEntity;
    }
}
