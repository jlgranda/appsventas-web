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
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.fede.ejb.sales.DetailService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import com.jlgranda.fede.ejb.sales.PaymentService;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.inventory.InventoryHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.model.sales.Detail;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.Payment;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.ui.model.LazyInvoiceDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

/**
 *
 * @author jlgranda
 */
@ManagedBean
@ViewScoped
public class InvoiceHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 115507468383355922L;

    Logger logger = LoggerFactory.getLogger(InvoiceHome.class);

    @Inject
    @LoggedIn
    private Subject subject;

    private Subject customer;
    
    @Inject
    private SettingHome settingHome;

    private Invoice invoice = null;

    private Invoice lastInvoice;

    private Invoice lastPreInvoice;

    private Long invoiceId;
    
    private Detail candidateDetail;

    private List<Detail> candidateDetails = new ArrayList<>();
    
    private Payment payment;
    
    @EJB
    private GroupService groupService;


    @EJB
    private InvoiceService invoiceService;

    @EJB
    private SubjectService subjectService;
    
    @EJB
    private DetailService detailService;
    
    @EJB
    private PaymentService paymentService;
    
    @EJB
    private ProductService productService;
    
    private LazyInvoiceDataModel lazyDataModel; 
    
    private DocumentType documentType;
    
    private boolean useDefaultCustomer;
    
    //Resumenes rápidos
    private List<Invoice> myLastlastPreInvoices = new ArrayList<>();
    
    private List<Invoice> myLastlastInvoices = new ArrayList<>();
    
    @Inject 
    private FacturaElectronicaHome facturaElectronicaHome;
    
    @PostConstruct
    private void init() {
        
        setInvoice(invoiceService.createInstance());
        setCandidateDetail(detailService.createInstance(1));
        setPayment(paymentService.createInstance());
        int amount = 0; //Rango de fechas para visualiar lista de entidades
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.MYLASTS_RANGE, "1"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            amount = 30;
        }

        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.addDays(getEnd(), -1 * amount));
        setDocumentType(DocumentType.PRE_INVOICE); //Listar prefacturas por defecto
        setOutcome("preinvoices");
        setUseDefaultCustomer(true); //Usar consumidor final por ahora
        List<BussinesEntity> defaultProducts = new ArrayList<>();
        defaultProducts.add(productService.find(101L));
        setSelectedBussinesEntities(defaultProducts);
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Invoice getInvoice() {

        if (invoiceId != null && !this.invoice.isPersistent()) {
            this.invoice = invoiceService.find(invoiceId);
            loadCandidateDetails(this.invoice.getDetails());
            setCustomer(this.invoice.getOwner());
        }
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Invoice getLastInvoice() {
        if (lastInvoice == null){
            List<Invoice> obs = invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", 1, DocumentType.INVOICE, true, getStart(), getEnd());
            lastInvoice = obs.isEmpty() ? new Invoice() : (Invoice) obs.get(0);
        }
        return lastInvoice;
    }

    public void setLastInvoice(Invoice lastInvoice) {
        this.lastInvoice = lastInvoice;
    }
    
    public Invoice getLastPreInvoice() {
        if (lastPreInvoice == null){
            List<Invoice> obs = invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", 1, DocumentType.PRE_INVOICE, true, getStart(), getEnd());
            lastPreInvoice = obs.isEmpty() ? new Invoice() : (Invoice) obs.get(0);
        }
        return lastPreInvoice;
    }

    public void setLastPreInvoice(Invoice lastPreInvoice) {
        this.lastPreInvoice = lastPreInvoice;
    }

    public Detail getCandidateDetail() {
        return candidateDetail;
    }

    public void setCandidateDetail(Detail candidateDetail) {
        this.candidateDetail = candidateDetail;
    }

    public List<Detail> getCandidateDetails() {
        return this.candidateDetails;
    }

    public void setCandidateDetails(List<Detail> candidateDetails) {
        this.candidateDetails = candidateDetails;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Subject getCustomer() {
        if (customer == null && isUseDefaultCustomer()){
            setCustomer(subjectService.findUniqueByNamedQuery("Subject.findUserByLogin", "consumidorfinal"));
        }
        return customer;
    }

    public void setCustomer(Subject customer) {
        this.customer = customer;
    }

    public boolean isUseDefaultCustomer() {
        return useDefaultCustomer;
    }

    public void setUseDefaultCustomer(boolean useDefaultCustomer) {
        this.useDefaultCustomer = useDefaultCustomer;
    }

    public List<Invoice> getMyLastlastPreInvoices() {
        if (myLastlastPreInvoices.isEmpty()){
            myLastlastPreInvoices = findInvoices(subject, DocumentType.PRE_INVOICE, 0, getStart(), getEnd());
        }
        return myLastlastPreInvoices;
    }

    public void setMyLastlastPreInvoices(List<Invoice> myLastlastPreInvoices) {
        this.myLastlastPreInvoices = myLastlastPreInvoices;
    }

    public List<Invoice> getMyLastlastInvoices() {
        if (myLastlastInvoices.isEmpty()){
            myLastlastInvoices = findInvoices(subject, DocumentType.INVOICE, 0, getStart(), getEnd());
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
     * Guarda la entidad marcandola como INVOICE CANCELED
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String cancel() {
        getInvoice().setDocumentType(DocumentType.PRE_INVOICE); //Se convierte en pre factura
        getInvoice().setEmissionType(EmissionType.CANCELED); //Se convierte en pre factura cancelada
        getInvoice().setStatus(EmissionType.CANCELED.toString());
        getInvoice().setActive(false);
        getInvoice().setSequencial(UUID.randomUUID().toString());//Generar el secuencia legal de factura
        save(true); //Guardar forzando
        return "success";
    }

    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String collect() {
        String outcome = "success";
        calculeChange();
        if (getPayment().getCash().compareTo(BigDecimal.ZERO) > 0 && getPayment().getChange().compareTo(BigDecimal.ZERO) >= 0) {
            getInvoice().setDocumentType(DocumentType.INVOICE); //Se convierte en factura
            getInvoice().setSequencial("TODO:generar-secuencial-sri");//Generar el secuencia legal de factura
            //Agregar el pago
            getPayment().setAmount(getInvoice().getTotal()); //Registrar el total a cobrarse
            getInvoice().addPayment(getPayment());
            save();
            setOutcome("preinvoices");
        } else {
            addErrorMessage(I18nUtil.getMessages("app.fede.sales.payment.incomplete"), I18nUtil.getFormat("app.fede.sales.payment.incomplete.detail", "" + this.getInvoice().getTotal()));
            setOutcome("");
        }
        return outcome;
    }

    public String save(){
        return save(false);
    }
    public String save(boolean force) {
        if (candidateDetails.isEmpty() && ! force) {
            addErrorMessage(I18nUtil.getMessages("app.fede.sales.invoice.incomplete"), I18nUtil.getMessages("app.fede.sales.invoice.incomplete.detail"));
            setOutcome("");
            return "";
        }
        try {
            getInvoice().setAuthor(subject);
            getInvoice().setOwner(getCustomer()); //Propietario de la factura, la persona que realiza la compra
            getInvoice().setOrganization(null);
            getCandidateDetails().stream().forEach((d) -> {
                if (d.isPersistent()) { //Actualizar la cantidad
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
            });

            invoiceService.save(getInvoice().getId(), getInvoice());
            this.addDefaultSuccessMessage();
            setOutcome("preinvoices");
            return "success";
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
        return "failed";
    }

    public void clear() {
        this.lastInvoice = null;
        this.lastPreInvoice = null;
    }

    public BigDecimal calculeTotal(List<Invoice> list) {
        BigDecimal subtotal = new BigDecimal(0);
        BigDecimal discount = new BigDecimal(0);
        Payment payment = null;
        for (Invoice i : list) {
            subtotal = subtotal.add(i.getTotal());
            payment = i.getPayments().isEmpty() ? null : i.getPayments().get(0);
            if (payment != null )
                discount = discount.add(payment.getDiscount());
        }

        return subtotal.subtract(discount, MathContext.UNLIMITED);
    }

    
    public void calculeChange() {

        
        //subtotal = total menos descuento
        BigDecimal subtotal = calculeCandidateDetailTotal().subtract(getPayment().getDiscount());
        
        //Preestablecer el dinero a recibir
        if (BigDecimal.ZERO.equals(getPayment().getCash()))
            getPayment().setCash(subtotal); //Asumir que se entregará exacto, si no se ha indicado nada
        
        //CAMBIO > lo que he recibido menos el subtotal
        getPayment().setChange(getPayment().getCash().subtract(subtotal));
        
    }

    public LazyInvoiceDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyInvoiceDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyInvoiceDataModel(invoiceService);
        }

        lazyDataModel.setAuthor(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());
        lazyDataModel.setDocumentType(getDocumentType());

        if (getKeyword() != null && getKeyword().startsWith("label:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
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
            if (event != null && event.getObject() != null) {
                redirectTo("/pages/fede/ride.jsf?key=" + ((BussinesEntity) event.getObject()).getId());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InvoiceHome.class.getName()).log(Level.SEVERE, null, ex);
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
        if (details != null) {
            this.candidateDetails.clear();
            this.invoice.getDetails().stream().forEach((d) -> {
                this.candidateDetails.add(d);
            });
        }
    }
    
    public boolean addCandidateDetail(){
        
        return touch(candidateDetail.getProduct());
    }
    
    public boolean touch(Product product){
        setCandidateDetail(detailService.createInstance(1));
        getCandidateDetail().setProduct(product);
        getCandidateDetail().setPrice(candidateDetail.getProduct().getPrice());//Establecer el precio actual
        getCandidateDetail().setInvoice(getInvoice());
        if (this.candidateDetails.contains(getCandidateDetail())) {
            int index = this.candidateDetails.indexOf(getCandidateDetail());
            float amount = this.candidateDetails.get(index).getAmount() + candidateDetail.getAmount();
            this.candidateDetails.get(index).setAmount(amount);
        } else {
            this.candidateDetails.add(getCandidateDetail());
        }

        calculeChange();
        //encerar para el siguiente producto
        setCandidateDetail(detailService.createInstance(1));
        
        return true;
    }
    
    public boolean macro(String command){
        for (String id : command.split(",")){
            touch(productService.find(Long.valueOf(id)));
        }
        return true;
    }
    
    public boolean addAndSaveCandidateDetail(){
        this.invoice.addDetail(candidateDetail); //Marcar detail como parte del objeto invoice
        boolean flag = addCandidateDetail();
        if (flag) save();
        return flag;
    }
    
    
    public List<Invoice> findInvoices(Subject author, DocumentType documentType, int limit, Date start, Date end){   
        if (author == null){ //retornar todas
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", limit, documentType, true, start, end);
        } else {
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentTypeAndAuthor", limit, documentType, author, true, start, end);
        }
    }

    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "invoice", "invoice"), Group.Type.LABEL));
        }

        return this.groups;
    }
    
    /////////////////////////////////////////////////////////////////////////
    // Chart data model
    /////////////////////////////////////////////////////////////////////////
    private LineChartModel balanceLineChartModel;

    public LineChartModel getBalanceLineChartModel() {
        if (balanceLineChartModel == null){
            setBalanceLineChartModel(createLineChartModel());
        }
        return balanceLineChartModel;
    }
    
    public void cleanChartModels(){
        setBalanceLineChartModel(null);
    }

    public void setBalanceLineChartModel(LineChartModel balanceLineChartModel) {
        this.balanceLineChartModel = balanceLineChartModel;
    }
    
    private LineChartModel createLineChartModel() {
        LineChartModel areaModel = new LineChartModel();
 
        LineChartSeries sales = new LineChartSeries();
        sales.setFill(false);
        sales.setLabel(I18nUtil.getMessages("app.fede.sales"));
        
        LineChartSeries purchases = new LineChartSeries();
        purchases.setFill(false);
        purchases.setLabel(I18nUtil.getMessages("common.purchases"));
        
        LineChartSeries profits = new LineChartSeries();
        profits.setFill(false);
        profits.setLabel(I18nUtil.getMessages("common.profits.gross"));
        
        Date _start = getStart();
        if (Dates.calculateNumberOfDaysBetween(getStart(), getEnd()) <= 1){
            int range = Integer.parseInt(settingHome.getValue("app.fede.chart.range", "7"));
            _start = Dates.addDays(getStart(), -1 * range);
        }
        Date _step = _start;
        String label = "";
        BigDecimal salesTotal;
        BigDecimal purchasesTotal;
        for (int i = 0; i <= Dates.calculateNumberOfDaysBetween(_start, getEnd()); i++){
            label = Strings.toString(_step, Calendar.DAY_OF_WEEK) + ", " + Dates.get(_step, Calendar.DAY_OF_MONTH);
            salesTotal = calculeTotal(findInvoices(subject, DocumentType.INVOICE, 0, Dates.minimumDate(_step), Dates.maximumDate(_step)));
            sales.set(label, salesTotal);
            
            facturaElectronicaHome.setStart(Dates.minimumDate(_step));
            facturaElectronicaHome.setEnd(Dates.maximumDate(_step));
            purchasesTotal = facturaElectronicaHome.calculeTotal(facturaElectronicaHome.getResultList());
            purchases.set(label, purchasesTotal);
            
            profits.set(label, salesTotal.subtract(purchasesTotal)); //Utilidad bruta
            
            _step = Dates.addDays(_step, 1); //Siguiente día
        }

        areaModel.addSeries(sales);
        areaModel.addSeries(profits);
        areaModel.addSeries(purchases);
        
         
        areaModel.setTitle(I18nUtil.getMessages("app.fede.chart.salesvspurchases"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "nw"));
        areaModel.setExtender("skinChart");
        //areaModel.setExtender("chartExtender");
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);
        
         
        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.chart.date.day.scale"));
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.chart.sales.scale"));
        yAxis.setMin(0);
        yAxis.setMax(settingHome.getValue("app.fede.chart.sales.scale.max", "200"));
        
        return areaModel;
    }
    
    
    public BigDecimal calculeCandidateDetailTotal() {
        BigDecimal total = new BigDecimal(BigInteger.ZERO);
        for (Detail d : getCandidateDetails()){
            total = total.add(d.getPrice().multiply(BigDecimal.valueOf(d.getAmount())));
        }
        return total;
    }
    
    @Inject
    private InventoryHome inventoryHome;
    public BarChartModel buildProductTopBarChartModel() {
        inventoryHome.setStart(Dates.minimumDate(getStart()));
        inventoryHome.setEnd(Dates.maximumDate(getEnd()));
        return inventoryHome.getTopBarChartModel();
    }
    public LineChartModel buildProductLineBarChartModel() {
        inventoryHome.setStart(Dates.minimumDate(getStart()));
        inventoryHome.setEnd(Dates.maximumDate(getEnd()));
        return inventoryHome.buildLineBarChartModel(getSelectedBussinesEntities());
    }
}
