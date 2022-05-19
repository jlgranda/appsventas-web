/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlgranda.fede.ui.model;

import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import org.jlgranda.fede.model.document.DocumentType;
import org.jpapi.model.Organization;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Invoice_;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.BussinesEntityType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
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
public class LazyInvoiceDataModel extends LazyDataModel<Invoice> implements Serializable {
    
    private static final int MAX_RESULTS = 100;
    private static final long serialVersionUID = 2581443761081001033L;
    Logger  logger = LoggerFactory.getLogger(LazyInvoiceDataModel.class);

    private InvoiceService bussinesEntityService;
    
    private List<Invoice> resultList;
    
    private int firstResult = 0;
    
    private BussinesEntityType type;
    
    private String boardNumber;
    
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
    
    private DocumentType documentType;
    
    private Organization organization;

    public LazyInvoiceDataModel(InvoiceService bussinesEntityService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.bussinesEntityService = bussinesEntityService;
    }

    @PostConstruct
    public void init() {
    }

    public List<Invoice> getResultList() {

        if (resultList.isEmpty()) {
            //resultList = this.load(0, MAX_RESULTS, new HashMap<>(), buildFilters(true));
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
    
    public String getBoardNumber() {
        return boardNumber;
    }

    public void setBoardNumber(String boardNumber) {
        this.boardNumber = boardNumber;
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

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    public BussinesEntityType getType() {
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
    public Invoice getRowData(String rowKey) {
        return bussinesEntityService.find(Long.valueOf(rowKey));
    }

    @Override
    public String getRowKey(Invoice entity) {
        return "" + entity.getId();
    }

    @Override
    public List<Invoice> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filters) {

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
        Map<String, Object> _filters = buildFilters(true); //Filtros desde atributos de clase
        
        _filters.putAll(filters);
        
        if (sortField == null){
            sortField = Invoice_.emissionOn.getName();
        }

        QueryData<Invoice> qData = bussinesEntityService.find(first, _end, sortField, order, _filters);
        this.setRowCount(qData.getTotalResultCount().intValue());
        
        //Aplicar filtros a resultados
        if (!filters.isEmpty()){
            System.out.println(">>>>>>>>>>>>>>>>>>>>< aplicar filtros de UX " + filters);
            List<Invoice> invoices = qData.getResult().stream()
                    .skip(first)
                    .filter(o -> filter(FacesContext.getCurrentInstance(), filters.values(), o))
                    .limit(pageSize)
                    .collect(Collectors.toList());

            this.setRowCount(invoices.size());
            return invoices;
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
                Invoice invoice = (Invoice) o;
                Object columnValue = invoice.getSummary();
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
        if (!Strings.isNullOrEmpty(getBoardNumber())){
            _filters.put(Invoice_.boardNumber.getName(), getBoardNumber()); //Filtro de número de mesa
        }
        if (loadByAuthor){
            if (getAuthor() != null){
                _filters.put(Invoice_.author.getName(), getAuthor()); //Filtro por defecto
            }
        } else {
            if (getOwner() != null){
                _filters.put(Invoice_.owner.getName(), getOwner()); //Filtro por defecto
            } 
        }
        if (getOrganization()!= null){
            _filters.put(Invoice_.organization.getName(), getOrganization()); //Filtro por defecto de organizacion
        } 
        if (!range.isEmpty()){
            _filters.put(Invoice_.emissionOn.getName(), range); //Filtro de fecha de emissión
        }
        if (!Strings.isNullOrEmpty(getTags())){
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        if (!Strings.isNullOrEmpty(getFilterValue())){
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        if (getDocumentType() != null){
            _filters.put("documentType", getDocumentType()); //Filtro por tipos de documentos
        }
        
        return _filters;
    }

    @Override
    public int count(Map<String, FilterMeta> filters) {
        Map<String, Object> _filters = buildFilters(true); //Filtros desde atributos de clase
        
        _filters.putAll(filters);

        QueryData<Invoice> qData = bussinesEntityService.find(_filters);
        return qData.getTotalResultCount().intValue();
    }
}
