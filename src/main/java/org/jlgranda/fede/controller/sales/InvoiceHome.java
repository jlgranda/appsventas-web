/*
 * Copyright (C) 2016 jlgranda
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
package org.jlgranda.fede.controller.sales;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.sales.DetailService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.model.sales.Detail;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.ui.model.LazyInvoiceDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.UnselectEvent;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class InvoiceHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(InvoiceHome.class);

    @Inject
    @LoggedIn
    private Subject subject;

    @Inject
    private SettingHome settingHome;

    private Invoice invoice = null;
    
    private Invoice lastInvoice;
    
    private Invoice lastPreInvoice;
    
    private Long invoiceId;

    private List<Detail> candidateDetails = new ArrayList<>();

    @EJB
    private InvoiceService invoiceService;

    @EJB
    private DetailService detailService;

    @EJB
    private ProductService productService;
    
    private LazyInvoiceDataModel lazyDataModel; 
    
    //Resumenes rápidos
    private List<Invoice> myLastlastPreInvoices = new ArrayList<>();
    private List<Invoice> myLastlastInvoices = new ArrayList<>();

    @PostConstruct
    private void init() {
        setInvoice(invoiceService.createInstance());
        int amount = 0;
        try {
            //amount = Integer.valueOf(settingService.findByName(SettingNames.DASHBOARD_RANGE).getValue());
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe){
            nfe.printStackTrace();
            amount = 30;
        }
        
        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Invoice getInvoice() {
            
        if (invoiceId != null && !this.invoice.isPersistent()){
            this.invoice = invoiceService.find(invoiceId);
            loadCandidateDetails(this.invoice.getDetails());
        }
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Invoice getLastInvoice() {
        if (lastInvoice == null){
            List<Invoice> obs = invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", 1, DocumentType.INVOICE);
            lastInvoice = obs.isEmpty() ? new Invoice() : (Invoice) obs.get(0);
        }
        return lastInvoice;
    }

    public void setLastInvoice(Invoice lastInvoice) {
        this.lastInvoice = lastInvoice;
    }
    
    public Invoice getLastPreInvoice() {
        if (lastPreInvoice == null){
            List<Invoice> obs = invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", 1, DocumentType.PRE_INVOICE);
            lastPreInvoice = obs.isEmpty() ? new Invoice() : (Invoice) obs.get(0);
        }
        return lastPreInvoice;
    }

    public void setLastPreInvoice(Invoice lastPreInvoice) {
        this.lastPreInvoice = lastPreInvoice;
    }

    public List<Detail> getCandidateDetails() {
        if (this.candidateDetails.isEmpty()) {
            loadCandidateDetails(null);
        }
        return this.candidateDetails;
    }

    public void setCandidateDetails(List<Detail> candidateDetails) {
        this.candidateDetails = candidateDetails;
    }

    public List<Invoice> getMyLastlastPreInvoices() {
        if (myLastlastPreInvoices.isEmpty()){
            myLastlastPreInvoices = findInvoices(subject, DocumentType.PRE_INVOICE, 0);
        }
        return myLastlastPreInvoices;
    }

    public void setMyLastlastPreInvoices(List<Invoice> myLastlastPreInvoices) {
        this.myLastlastPreInvoices = myLastlastPreInvoices;
    }

    public List<Invoice> getMyLastlastInvoices() {
        if (myLastlastInvoices.isEmpty()){
            myLastlastInvoices = findInvoices(subject, DocumentType.INVOICE, 0);
        }
        return myLastlastInvoices;
    }

    public void setMyLastlastInvoices(List<Invoice> myLastlastInvoices) {
        this.myLastlastInvoices = myLastlastInvoices;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean showInvoiceForm() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        super.openDialog(settingHome.getValue("app.fede.sales.invoice.popup", "/pages/fede/sales/popup_invoice"), width, height, true);
        return true;
    }
    
    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial valido
     * TODO debe generar también una factura electrónica
     * @return outcome de exito o fracaso de la acción
     */
    public String collect() {
        if (getInvoice().isPersistent()){
            getInvoice().setDocumentType(DocumentType.INVOICE); //Se convierte en factura
            getInvoice().setSequencial("TODO:generar-secuencial");//Generar el secuencia legal de factura
            save();
        } else {
            return "no-persistent";
        }
        return "success";
    }

    public String save() {
        try {
            getInvoice().setAuthor(subject);
            getInvoice().setOrganization(null);
            for (Detail d : getCandidateDetails()) {
                if (d.isPersistent()){ //Actualizar la cantidad
                    getInvoice().replaceDetail(d);
                } else {
                    if (d.getAmount() != 0) {
                        d.setProductId(d.getProduct().getId());
                        d.setPrice(d.getProduct().getPrice());
                        d.setUnit(d.getUnit());
                        d.setAmount(d.getAmount());

                        d.setProduct(null);
                        getInvoice().addDetail(d);

                    }
                }
            }

            invoiceService.save(getInvoice().getId(), getInvoice());
            this.addDefaultSuccessMessage();
            return "success";
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
        return "failed";
    }
    
    public void clear(){
        this.lastInvoice = null;
        this.lastPreInvoice = null;
    }
    
    
    public BigDecimal calculeTotal(List<Invoice> list){
        BigDecimal total = new BigDecimal(0);
        for (Invoice i : list){
            total = total.add(i.getTotal());
        }
        
        return total;
    }

    public LazyInvoiceDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyInvoiceDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }
 
    public void filter() {
        if (lazyDataModel == null ){
            lazyDataModel = new LazyInvoiceDataModel(invoiceService);
        }
        
        //lazyDataModel.setOwner(subject);
        lazyDataModel.setAuthor(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());
            
        if (getKeyword()!= null && getKeyword().startsWith("label:")){
            String parts[] = getKeyword().split(":");
            if (parts.length > 1){
                lazyDataModel.setTags(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else {
            lazyDataModel.setTags(getTags());
            lazyDataModel.setFilterValue(getKeyword());
        }
    }
    
     public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null){
                redirectTo("/pages/fede/ride.jsf?key=" + ((BussinesEntity) event.getObject()).getId());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FacturaElectronicaHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());

        FacesContext.getCurrentInstance().addMessage(null, msg);
        this.selectedBussinesEntities.remove((FacturaElectronica) event.getObject());
         logger.info(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
    }

    @Override
    public Group getDefaultGroup() {
        return null; //las facturas no se etiquetan aún
    }

    private void loadCandidateDetails(List<Detail> details) {
        this.candidateDetails.clear();
        Detail detail = null;
        if (details == null){
            for (Object obj : productService.findByNamedQuery("Product.findByOrganization")) {
                detail = detailService.createInstance();
                detail.setProduct((Product)obj);
                detail.setAmount(0);
                this.candidateDetails.add(detail);
            }
        } else {
            for (Detail d : this.invoice.getDetails()){
                this.candidateDetails.add(d);
            }
        }
    }
    
    
    public List<Invoice> findInvoices(Subject owner, DocumentType documentType, int limit){
        if (owner == null){ //retornar todas
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", limit, documentType);
        } else {
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentTypeAndOwner", limit, documentType, owner);
        }
    }
}
