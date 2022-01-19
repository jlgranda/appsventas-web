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

import com.google.common.base.Strings;
import com.jlgranda.fede.ejb.FacturaElectronicaService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.model.document.FacturaElectronica_;
import org.jlgranda.fede.model.document.FacturaType;
import org.jpapi.model.Organization;
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
import org.primefaces.model.filter.FilterConstraint;
import org.primefaces.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
public class LazyFacturaElectronicaDataModel extends LazyDataModel<FacturaElectronica> implements Serializable {

    private static final int MAX_RESULTS = 100;
    private static final long serialVersionUID = 201837221989669238L;

    Logger logger = LoggerFactory.getLogger(LazyFacturaElectronicaDataModel.class);

    private FacturaElectronicaService bussinesEntityService;

    private List<FacturaElectronica> resultList;
    private int firstResult = 0;

    private BussinesEntityType type;

    private Subject owner;

    private Subject author;

    private String code;

    private Organization organization;
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
    
    private FacturaType facturaType;

    public LazyFacturaElectronicaDataModel(FacturaElectronicaService bussinesEntityService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.bussinesEntityService = bussinesEntityService;
    }

    @PostConstruct
    public void init() {
    }

    public List<FacturaElectronica> getResultList() {
        if (resultList.isEmpty()/* && getSelectedBussinesEntity() != null*/) {
//            resultList = bussinesEntityService.find(this.getPageSize(), this.getFirstResult());
            resultList = this.load(0, MAX_RESULTS, new HashMap<>(), null);
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Subject getOwner() {
        return owner;
    }

    public void setOwner(Subject owner) {
        this.owner = owner;
    }
    
    public Subject getAuthor() {
        return author;
    }

    public void setAuthor(Subject author) {
        this.author = author;
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
    public FacturaElectronica getRowData(String rowKey) {
        return bussinesEntityService.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(FacturaElectronica entity) {
        return "" + entity.getId();
    }

    @Override
    public List<FacturaElectronica> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

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
        Map<String, Object> _filters = buildFilters(true); //Filtros desde atributos de clase

        _filters.putAll(filters);
        
        if (sortField == null) {
            sortField = FacturaElectronica_.fechaEmision.getName();
        }

        QueryData<FacturaElectronica> qData = bussinesEntityService.find(first, _end, sortField, order, _filters);
        this.setRowCount(qData.getTotalResultCount().intValue());

        //Aplicar filtros a resultados
        if (!filters.isEmpty()) {
            List<FacturaElectronica> facturas = qData.getResult().stream()
                    .skip(first)
                    .filter(o -> filter(FacesContext.getCurrentInstance(), filters.values(), o))
                    .limit(pageSize)
                    .collect(Collectors.toList());

            this.setRowCount(facturas.size());
            return facturas;
        }

        return qData.getResult();
    }

    private boolean filter(FacesContext context, Collection<FilterMeta> filterBy, Object o) {
        boolean matching = true;

        for (FilterMeta filter : filterBy) {
            FilterConstraint constraint = filter.getConstraint();
            Object filterValue = filter.getFilterValue();
//            try {
//                Object columnValue = String.valueOf(o.getClass().getField(filter.getField()).get(o));
            FacturaElectronica factura = (FacturaElectronica) o;
            Object columnValue = factura.getSummary();
            matching = constraint.isMatching(context, columnValue, filterValue, LocaleUtils.getCurrentLocale());
//            } catch (ReflectiveOperationException e) {
//                matching = false;
//            }

            if (!matching) {
                break;
            }
        }

        return matching;
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

    private Map<String, Object> buildFilters(boolean loadByAuthor) {
        Map<String, Object> _filters = new HashMap<>();
        Map<String, Date> range = new HashMap<>();
        range.put("start", getStart());
        range.put("end", getEnd());
        //_filters.put(BussinesEntity_.type.getName(), getType()); //Filtro por defecto
        
        if (!Strings.isNullOrEmpty(getCode())) {
            _filters.put(FacturaElectronica_.code.getName(), getCode()); //Filtro por número de factura
        }

        if (loadByAuthor){
            if (getAuthor() != null){
                _filters.put(FacturaElectronica_.author.getName(), getAuthor()); //Filtro por defecto
            }
        } else {
            if (getOwner() != null){
                _filters.put(FacturaElectronica_.owner.getName(), getOwner()); //Filtro por defecto
            } 
        }

        if (getOrganization() != null) {
            _filters.put(FacturaElectronica_.organization.getName(), getOrganization()); //Filtro por  defecto organization
        }

        if (!range.isEmpty()) {
            _filters.put(FacturaElectronica_.fechaEmision.getName(), range); //Filtro de fecha inicial
        }

        if (!Strings.isNullOrEmpty(getTags())) {
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }

        if (!Strings.isNullOrEmpty(getFilterValue())) {
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        
        if (getFacturaType() != null && !Strings.isNullOrEmpty(getFacturaType().toString())) {
            _filters.put(FacturaElectronica_.facturaType.getName(), getFacturaType()); //Tipo de factura
        }

        return _filters;
    }

    @Override
    public int count(Map<String, FilterMeta> filters) {
        
        Map<String, Object> _filters = buildFilters(true); //Filtros desde atributos de clase
        _filters.putAll(filters);
        QueryData<FacturaElectronica> qData = bussinesEntityService.find(_filters);
        return qData.getTotalResultCount().intValue();
    }

    public FacturaType getFacturaType() {
        return facturaType;
    }

    public void setFacturaType(FacturaType facturaType) {
        this.facturaType = facturaType;
    }

}
