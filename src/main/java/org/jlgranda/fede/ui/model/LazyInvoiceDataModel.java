/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlgranda.fede.ui.model;

import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Invoice_;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.BussinesEntityType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
public class LazyInvoiceDataModel extends LazyDataModel<Invoice> implements Serializable {
    
    private static final int MAX_RESULTS = 5;
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

    public LazyInvoiceDataModel(InvoiceService bussinesEntityService) {
        setPageSize(MAX_RESULTS);
        resultList = new ArrayList<>();
        this.bussinesEntityService = bussinesEntityService;
    }

    @PostConstruct
    public void init() {
    }

    public List<Invoice> getResultList() {
        logger.info("load BussinesEntitys");

        if (resultList.isEmpty()/* && getSelectedBussinesEntity() != null*/) {
            resultList = bussinesEntityService.find(this.getPageSize(), this.getFirstResult());
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
    public Invoice getRowData(String rowKey) {
        return bussinesEntityService.find(Long.valueOf(rowKey));
    }

    @Override
    public Object getRowKey(Invoice entity) {
        return entity.getId();
    }

    @Override
    public List<Invoice> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {

        int end = first + pageSize;

        QuerySortOrder order = QuerySortOrder.DESC;
        if (sortOrder == SortOrder.ASCENDING) {
            order = QuerySortOrder.ASC;
        }
        Map<String, Object> _filters = new HashMap<>();
        Map<String, Date> range = new HashMap<>();
        range.put("start", getStart());
        range.put("end", getEnd());
        //_filters.put(BussinesEntity_.type.getName(), getType()); //Filtro por defecto
        _filters.put(Invoice_.boardNumber.getName(), getBoardNumber()); //Filtro de número de mesa
        _filters.put(Invoice_.author.getName(), getAuthor()); //Filtro por defecto
        _filters.put(Invoice_.createdOn.getName(), range); //Filtro de fecha inicial
        if (getTags() != null && !getTags().isEmpty()){
            _filters.put("tag", getTags()); //Filtro de etiquetas
        }
        
        if (getFilterValue() != null && !getFilterValue().isEmpty()){
            _filters.put("keyword", getFilterValue()); //Filtro general
        }
        
        if (getDocumentType() != null){
            _filters.put("documentType", getDocumentType()); //Filtro por tipos de documentos
        }
        
        _filters.putAll(filters);
        
        if (sortField == null){
            sortField = Invoice_.createdOn.getName();
        }

        QueryData<Invoice> qData = bussinesEntityService.find(first, end, sortField, order, _filters);
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
