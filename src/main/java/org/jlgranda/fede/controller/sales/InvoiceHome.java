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
import com.jlgranda.fede.ejb.sales.ProductCache;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.SubjectHome;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.controller.admin.TemplateHome;
import org.jlgranda.fede.controller.gift.GiftHome;
import org.jlgranda.fede.controller.inventory.InventoryHome;
import org.jlgranda.fede.controller.sales.report.AdhocCustomizerReport;
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
import org.jpapi.model.StatusType;
import org.jpapi.model.TaxType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.SortOrder;
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
@ViewScoped
@Named
public class InvoiceHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 115507468383355922L;

    Logger logger = LoggerFactory.getLogger(InvoiceHome.class);

    @Inject
    private Subject subject;

    private Subject customer;

    @Inject
    private SettingHome settingHome;

    @Inject
    private SubjectHome subjectHome;

    /** Para almacemar la cantidad en el formulario de pedido rápido */
    private Long amount;

    private Invoice invoice = null;

    private Invoice lastInvoice;

    private Invoice lastPreInvoice;

    private Long invoiceId;

    private Detail candidateDetail;

    private List<Detail> candidateDetails = new ArrayList<>();

    private Set<Product> recents = new HashSet<>();

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
    private ProductCache productCache;

    private LazyInvoiceDataModel lazyDataModel;

    private DocumentType documentType;

    private boolean useDefaultCustomer;

    private boolean useDefaultEmail;

    private boolean nonnative;

    private boolean busquedaAvanzada;

    private boolean busquedaEjecutada;

    //Resumenes rápidos
    private List<Invoice> myLastlastPreInvoices = new ArrayList<>();

    private List<Invoice> myPendinglastPreInvoices = new ArrayList<>();
    private List<Invoice> myOverduelastPreInvoices = new ArrayList<>();
    private List<Invoice> myLastlastInvoices = new ArrayList<>();
    private List<Invoice> myAllInvoices = new ArrayList<>(); // --

    @Inject
    private FacturaElectronicaHome facturaElectronicaHome;

    @Inject
    private TemplateHome templateHome;

    @Inject
    private SubjectAdminHome subjectAdminHome; //para administrar clientes en factura

    private boolean orderByCode;

    private String sortOrder="DESCENDING";

    private Long interval; //Intervalo de tiempo

    /**
     * Aplicación regalos
     */
    @Inject
    private GiftHome giftHome;

    @PostConstruct
    private void init() {

        setAmount(0L); //Sólo si se establece un valor
        setInvoice(invoiceService.createInstance());
        setCandidateDetail(detailService.createInstance(1));
        setPayment(paymentService.createInstance());

        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.now()));
        setDocumentType(DocumentType.PRE_INVOICE); //Listar prefacturas por defecto
        setOutcome("preinvoices");
        setUseDefaultCustomer(true); //Usar consumidor final por defecto
        setUseDefaultEmail(false); //Usar consumidor final por ahora
        setNonnative(false); //Es extrangero
        setBusquedaEjecutada(!Strings.isNullOrEmpty(getKeyword()));
        updateDefaultEmail();

        getSubjectAdminHome().setOutcome("invoice");

        setOrderByCode(false);

        setBusquedaAvanzada(true);

        setEnd(Dates.now()); //El día de hoy
        setStart(Dates.addDays(getEnd(), -7));

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
            calculeChange();//Prellenar formulario de pago
            setUseDefaultCustomer(this.invoice.getOwner() == null || Long.valueOf(511).equals(this.invoice.getOwner().getId()));
            //OJO
            //Establecer nuevo número de comanda
//            this.invoice.setSequencial(settingHome.getValue("app.fede.sales.invoice.comanda.sequence", ""));
            if (!this.invoice.getPayments().isEmpty()) {
                setPayment(this.invoice.getPayments().get(0)); //Cargar el pago guardado
            }
        } else {
            if (Strings.isNullOrEmpty(this.invoice.getSequencial())) //Establecer nuevo número de sequencia SRI
            {
                this.invoice.setSequencial(settingHome.getValue("app.fede.sales.invoice.sequence", "001-001-00000"));
            }
        }
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Invoice getLastInvoice() {
        if (lastInvoice == null) {
            List<Invoice> obs = invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", 1, DocumentType.INVOICE, true, getStart(), getEnd());
            lastInvoice = obs.isEmpty() ? new Invoice() : (Invoice) obs.get(0);
        }
        return lastInvoice;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setLastInvoice(Invoice lastInvoice) {
        this.lastInvoice = lastInvoice;
    }

    public Invoice getLastPreInvoice() {
        if (lastPreInvoice == null) {
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

    public Set<Product> getRecents() {
        return recents;
    }

    public void setRecents(Set<Product> recents) {
        this.recents = recents;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Subject getCustomer() {
        if (customer == null && isUseDefaultCustomer()) {
            setCustomer(subjectService.findUniqueByNamedQuery("Subject.findUserByLogin", "consumidorfinal"));
        }
        return customer;
    }

    public void updateDefaultCustomer() {
        this.customer = null;
    }

    public void updateDefaultEmail() {
        if (isUseDefaultEmail()) {
            this.subjectAdminHome.getSubjectEdit().setEmail(this.subjectAdminHome.getSubjectEdit().getCode().replaceAll("^\\s*", "") + "@emporiolojano.com");
        } else {
            this.subjectAdminHome.getSubjectEdit().setEmail("@");
        }
    }

    public void updateNonnative() {
        this.subjectAdminHome.getSubjectEdit().setNonnative(nonnative);
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

    public boolean isUseDefaultEmail() {
        return useDefaultEmail;
    }

    public void setUseDefaultEmail(boolean useDefaultEmail) {
        this.useDefaultEmail = useDefaultEmail;
    }

    public boolean isNonnative() {
        return nonnative;
    }

    public void setNonnative(boolean nonnative) {
        this.nonnative = nonnative;
    }


    public boolean isBusquedaAvanzada() {
        return busquedaAvanzada;
    }

    public void setBusquedaAvanzada(boolean busquedaAvanzada) {
        this.busquedaAvanzada = busquedaAvanzada;
    }

    public boolean isBusquedaEjecutada() {
        return busquedaEjecutada;
    }

    public void setBusquedaEjecutada(boolean busquedaEjecutada) {
        this.busquedaEjecutada = busquedaEjecutada;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
        initializeDateInterval();
    }

    /**
     * Obtiene la lista de Pre facturas en estado de PRE_INVOICE para la fecha
     *
     * @return
     */
    public List<Invoice> getMyLastlastPreInvoices() {
        if (myLastlastPreInvoices.isEmpty()) {
            filter(subject, getStart(), getEnd(), DocumentType.PRE_INVOICE, getKeyword(), getTags());
            myLastlastPreInvoices = getLazyDataModel().load(null, SortOrder.valueOf(getSortOrder()), true);
        }
        return myLastlastPreInvoices;
    }

    public List<Invoice> getMyLastlastInvoices() {
        return getMyLastlastInvoices(true);
    }

    /**
     * Lista de Invoices por propietario, se usa en vistas de tipo social, se
     * oculta la cantidad
     *
     * @return
     */
    public List<Invoice> getMyLastlastInvoicesByOwner() {
        List<Invoice> invoices = getMyLastlastInvoices(false);
        invoices.forEach((_invoice) -> {
            _invoice.getDetails().forEach((detail) -> {
                detail.setShowAmountInSummary(false);
            });
        });
        return invoices;
    }

    protected List<Invoice> getMyLastlastInvoices(boolean byAuthor) {
        if (myLastlastInvoices.isEmpty()) {
            filter(subject, Dates.minimumDate(getEnd()), Dates.maximumDate(getEnd()), DocumentType.INVOICE, getKeyword(), getTags());
            myLastlastInvoices = getLazyDataModel().load(null, SortOrder.valueOf(getSortOrder()), byAuthor);
        }
        return myLastlastInvoices;
    }

    
    public List<Invoice> getMyPendingPreInvoices() {
        Date _end = Dates.addDays(getEnd(), -1);
        Date _start = Dates.addDays(_end, -30);
        if (myPendinglastPreInvoices.isEmpty()) {
            filter(subject, Dates.minimumDate(_start), Dates.maximumDate(_end), DocumentType.PRE_INVOICE, getKeyword(), getTags());
            myPendinglastPreInvoices = getLazyDataModel().load(null, SortOrder.valueOf(getSortOrder()), true);
        }
        return myPendinglastPreInvoices;
    }

    // --Obtener todas mis facturas 
    public List<Invoice> getMyAllInvoices() {
        if (myAllInvoices.isEmpty()) {
            filter(subject, Dates.minimumDate(getStart()), Dates.maximumDate(getEnd()), DocumentType.INVOICE, getKeyword(), getTags());
            myAllInvoices = getLazyDataModel().load(null, SortOrder.valueOf(getSortOrder()), true);
        }
        return myAllInvoices;
    }

    public void setMyLastlastPreInvoices(List<Invoice> myLastlastPreInvoices) {
        this.myLastlastPreInvoices = myLastlastPreInvoices;
    }


    public void setMyLastlastInvoices(List<Invoice> myLastlastInvoices) {
        this.myLastlastInvoices = myLastlastInvoices;
    }

    public List<Invoice> getMyOverduelastPreInvoices() {
        Date _end = getEnd();
        Date _start = getStart();
        if (myOverduelastPreInvoices.isEmpty()) {
            filter(subject, Dates.minimumDate(_start), Dates.maximumDate(_end), DocumentType.OVERDUE, getKeyword(), getTags());
            myOverduelastPreInvoices = getLazyDataModel().load(null, SortOrder.valueOf(getSortOrder()), true);
        }
        return myOverduelastPreInvoices;
    }

    public void setMyOverduelastPreInvoices(List<Invoice> myOverduelastPreInvoices) {
        this.myOverduelastPreInvoices = myOverduelastPreInvoices;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        setCustomer((Subject) event.getObject());
    }

    public boolean showInvoiceForm() {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        super.openDialog(settingHome.getValue("app.fede.sales.invoice.popup", "/pages/fede/sales/popup_invoice"), width, height, true);
        return true;
    }

    public SubjectAdminHome getSubjectAdminHome() {
        return subjectAdminHome;
    }

    public void setSubjectAdminHome(SubjectAdminHome subjectAdminHome) {
        this.subjectAdminHome = subjectAdminHome;
    }

    
    /**
     * Guarda la entidad marcandola como INVOICE CANCELED
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String cancel() {
        getInvoice().setDocumentType(DocumentType.CANCELED_INVOICE); //Se convierte en pre factura
        getInvoice().setEmissionType(EmissionType.CANCELED); //Se convierte en pre factura cancelada
        getInvoice().setStatus(StatusType.CLOSE.toString());
        getInvoice().setActive(false);
        //getInvoice().setSequencial(UUID.randomUUID().toString());//Generar el secuencia legal de factura
        save(true); //Guardar forzando
        return getOutcome();
    }

    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String collect() {
        collect(StatusType.CLOSE.toString());
        return getOutcome();
    }
    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String overdue() {
        if (!isUseDefaultCustomer()) {
            collect(DocumentType.OVERDUE, StatusType.CLOSE.toString());
            setOutcome("overdues");
        } else {
            addWarningMessage("¿Quién será reesponsable del crédito?", "Seleccione una persona/entidad como responsable del crédito.");
            setOutcome("currentpage.xhtml?faces-redirect=true");
        }
        return getOutcome();
    }

    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String collect(String status) {
        return collect(DocumentType.INVOICE, status);
    }

    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @param documentType
     * @param status
     * @return outcome de exito o fracaso de la acción
     */
    public String collect(DocumentType documentType, String status) {
        calculeChange(); //Calcular el cambio sobre el objeto payment en edición
        if (getPayment().getCash().compareTo(BigDecimal.ZERO) > 0 && getPayment().getChange().compareTo(BigDecimal.ZERO) >= 0) {
            getInvoice().setDocumentType(documentType); //Se convierte en factura
            //getInvoice().setSequencial(sequenceSRI);//Generar el secuencia legal de factura
            //Agregar el pago
            getPayment().setAmount(getInvoice().getTotal()); //Registrar el total a cobrarse
            getInvoice().addPayment(getPayment());
            getInvoice().setStatus(status);
            save(true);
        } else {
            addErrorMessage(I18nUtil.getMessages("app.fede.sales.payment.incomplete"), I18nUtil.getFormat("app.fede.sales.payment.incomplete.detail", "" + this.getInvoice().getTotal()));
            setOutcome("");
        }
        return "failed";
    }

    public String print() {
        try {
            collect(StatusType.PRINTED.toString());
            //Forzar actualizar invoice para generación correcta del reporte
            setInvoice(invoiceService.createInstance());
            getInvoice(); //recargar la instancia actual
            //Imprimir reporte
            Map<String, String> settings = new HashMap<>();
            settings.put("app.fede.report.invoice.startLine", settingHome.getValue("app.fede.report.invoice.startLine", "48"));
            //settings.put("app.fede.report.invoice.fontName", settingHome.getValue("app.fede.report.invoice.fontName", "Epson1"));
            settings.put("app.fede.report.invoice.fontSize", settingHome.getValue("app.fede.report.invoice.fontSize", "8"));
            settings.put("app.fede.report.invoice.fontStyle", settingHome.getValue("app.fede.report.invoice.fontStyle", "plain"));

            AdhocCustomizerReport adhocCustomizerReport = new AdhocCustomizerReport(this.getInvoice(), settings);
            //InvoiceDesign invoiceDesign = new InvoiceDesign(this.getInvoice(), settings);
            //Invocar Servlet en nueva ventana del navegador
//            redirectTo("/fedeServlet/?entity=invoice&id=" + this.getInvoice().getSequencial() + "&type=odt");
            redirectTo("/fedeServlet/?entity=invoice&id=" + this.getInvoice().getSequencial() + "&type=pdf");

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InvoiceHome.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this.getOutcome();
    }

    /**
     * Registra el pago de forma directa
     * @param invoiceId
     * @return la regla de navegación
     */
    public String record(Long invoiceId) {
        this.setInvoiceId(invoiceId);
        //load invoice
        this.getInvoice();
        this.getInvoice().setDescription(settingHome.getValue("app.fede.status.pay_direct", StatusType.PAID_DIRECT.toString()));
        return this.collect(DocumentType.INVOICE, StatusType.CLOSE.toString());
    }

    /**
     * Registra como atendida. Cambia unicamente el status
     *
     * @param invoiceId
     * @return la regla de navegación
     */
    public String attend(Long invoiceId) {
        String outcome = "preinvoices";
        this.setInvoiceId(invoiceId);
        //Marcar invoice como atendido, sólo si no esta
        if (getInvoice().isPersistent() && !StatusType.ATTEND.toString().equals(getInvoice().getStatus())) {
            getInvoice().setStatus(StatusType.ATTEND.toString());
            getInvoice().setExpirationTime(Dates.now()); //Fecha de cierre de servicio
            save(true);
        }

        return outcome;
    }

    /**
     * Reabrir la factura como PRE-INVOICE
     * @param invoiceId
     * @throws java.io.IOException
     */
    public void reopen(Long invoiceId) throws IOException {
        this.setInvoiceId(invoiceId);
        //load invoice
        this.getInvoice();
        redirectTo("/pages/fede/sales/invoice.jsf?invoiceId=" + this.getInvoice().getId());
    }

    /**
     * Guarda los cambios en el objeto Invoice
     * @return
     */
    public String save() {
        calculeChange(); //Calcular el cambio sobre el objeto payment en edición
        if (getPayment().getCash().compareTo(BigDecimal.ZERO) > 0 && getPayment().getChange().compareTo(BigDecimal.ZERO) >= 0) {
            getInvoice().setDocumentType(DocumentType.PRE_INVOICE); //Mantener como preinvoice
            getPayment().setAmount(getInvoice().getTotal()); //Registrar el total a cobrarse
            getInvoice().addPayment(getPayment());
            setOutcome(save(false));
        } else {
            addErrorMessage(I18nUtil.getMessages("app.fede.sales.payment.incomplete"), I18nUtil.getFormat("app.fede.sales.payment.incomplete.detail", "" + this.getInvoice().getTotal()));
            setOutcome("");
        }
        return getOutcome();
    }

    public String save(boolean force) {
        if (candidateDetails.isEmpty() && !force) {
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
                        d.setAmount(this.getAmount().compareTo(Long.valueOf(0)) == 0 ? d.getAmount() : this.getAmount()); //Almacenar el valor del datalle, si no es vía el formulario rápido
                        d.setProduct(null);
                        getInvoice().addDetail(d);

                    }
                }
            });

            getInvoice().setLastUpdate(Dates.now()); //Forzar pues no se realiza ningun cambio en el objeto maestro
            invoiceService.save(getInvoice().getId(), getInvoice());
            this.addDefaultSuccessMessage();
            return getOutcome();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
        return "failed";
    }

    public void saveCustomer() {
        boolean success = false;
        if (!getSubjectAdminHome().getSubjectEdit().isPersistent()) {
            getSubjectAdminHome().getSubjectEdit().setPassword(UUID.randomUUID().toString());
            getSubjectAdminHome().getSubjectEdit().setConfirmed(false);

            getSubjectAdminHome().setValidated(true); //La validación se realiza en pantalla
            success = !"failed".equalsIgnoreCase(getSubjectAdminHome().save());
        } else {
            getSubjectAdminHome().update();
            success = true;
        }
        if (success) { //Guardar profile
            setCustomer(getSubjectAdminHome().getSubjectEdit());
            closeFormularioProfile(getCustomer());
        }

    }

    /**
     * Limpiar las listas de resultados, para que se actualice los criterios de
     * búsqueda
     */
    public void clear() {
        this.lastInvoice = null;
        this.lastPreInvoice = null;
        this.myLastlastInvoices.clear();
        this.myLastlastPreInvoices.clear();
        this.myOverduelastPreInvoices.clear();
        this.myAllInvoices.clear(); // --
    }

    public BigDecimal calculeTotal(List<Invoice> list) {
        BigDecimal subtotal = new BigDecimal(0);
        BigDecimal discount = new BigDecimal(0);
        Payment payment = null;
        for (Invoice i : list) {
            subtotal = subtotal.add(i.getTotal());
            payment = i.getPayments().isEmpty() ? null : i.getPayments().get(0);
            if (payment != null) {
                discount = discount.add(payment.getDiscount());
            }
        }
        return subtotal.subtract(discount, MathContext.UNLIMITED);
    }
    
    public BigDecimal calculeIva(BigDecimal subTotal) {
        BigDecimal iva = new BigDecimal(BigInteger.ZERO);
        iva = subTotal.multiply(BigDecimal.valueOf(0.12));        
        return iva;
    }

    public void calculeChange() {
        //subtotal = total menos descuento
        BigDecimal subtotal = calculeCandidateDetailTotal().subtract(getPayment().getDiscount());
        subtotal = subtotal.add(subtotal.multiply(BigDecimal.valueOf(0.12)));
//        subtotal = subtotal.subtract(getPayment().getDiscount());
        //Preestablecer el dinero a recibir
        if (subtotal.compareTo(getPayment().getCash()) > 0) {
            getPayment().setCash(subtotal); //Asumir que se entregará exacto, si no se ha indicado nada .add(calculeIva(calculeCandidateDetailTotal()))
        }        
        getPayment().setChange(getPayment().getCash().subtract(subtotal));
        
        //Se modificó el detalle, cambiar a estado abierto
        getInvoice().setStatus(StatusType.OPEN.toString());
        //Valores Impresos
        
    }

    public LazyInvoiceDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyInvoiceDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public void filter(Subject _subject, Date _start, Date _end, DocumentType _documentType, String _keyword, String _tags) {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyInvoiceDataModel(invoiceService);
        }

        if (_start != null || _end != null) {
            lazyDataModel.setStart(_start);
            lazyDataModel.setEnd(_end);
        }
        lazyDataModel.setAuthor(_subject);
        lazyDataModel.setOwner(_subject);
        lazyDataModel.setDocumentType(_documentType);


        if (_keyword != null && _keyword.startsWith("table:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setBoardNumber(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else if (_keyword != null && _keyword.startsWith("label:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setTags(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else {
            lazyDataModel.setTags(_tags);
            lazyDataModel.setFilterValue(_keyword); //Buscar por code, name, description
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

    public boolean addCandidateDetail() {

        Product p = candidateDetail.getProduct();
        touch(candidateDetail.getProduct());
        //Agregar a lista de últimos agregados desde autocomplete
        getRecents().add(p);
        return true;
    }

    public boolean touch(Product product) {
        setCandidateDetail(detailService.createInstance(1));
        getCandidateDetail().setProduct(product);
        getCandidateDetail().setPrice(candidateDetail.getProduct().getPrice());//Establecer el precio actual
        getCandidateDetail().setInvoice(getInvoice());
        if (this.candidateDetails.contains(getCandidateDetail())) {
            int index = this.candidateDetails.indexOf(getCandidateDetail());
            float amount_ = this.candidateDetails.get(index).getAmount() + candidateDetail.getAmount();
            this.candidateDetails.get(index).setAmount(amount_);
        } else {
            this.candidateDetails.add(getCandidateDetail());
        }

        calculeChange();
        //encerar para el siguiente producto
        setCandidateDetail(detailService.createInstance(1));

        return true;
    }

    public boolean touch(String command) {
        for (String id : command.split(",")) {
//            touch(productService.find(Long.valueOf(id)));
            touch(productCache.lookup(Long.valueOf(id)));
        }
        return true;
    }

    public String macro(String command) {
        for (String id : command.split(",")) {
//            touch(productService.find(Long.valueOf(id)));
            touch(productCache.lookup(Long.valueOf(id)));
        }
        return save(true);
    }

    public boolean addAndSaveCandidateDetail() {
        this.invoice.addDetail(candidateDetail); //Marcar detail como parte del objeto invoice
        boolean flag = addCandidateDetail();
        if (flag) {
            save();
        }
        return flag;
    }

    public List<Invoice> findInvoices(Subject author, DocumentType documentType, int limit, Date start, Date end) {
        if (author == null) { //retornar todas
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentType", limit, documentType, true, start, end);
        } else {
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentTypeAndAuthor", limit, documentType, author, true, start, end);
        }
    }

    public List<Invoice> findInvoices(Subject author, DocumentType documentType, int limit, Date start, Date end, boolean ordeByCode) {
        if (ordeByCode == false) { //retornar todas
            return findInvoices(author, documentType, limit, start, end);
        } else {
            return invoiceService.findByNamedQueryWithLimit("Invoice.findByDocumentTypeAndAuthorOrderByCode", limit, documentType, author, true, start, end);
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

    /**
     * Busca objetos <tt>Subject</tt> para la clave de búsqueda en las columnas
     * usernae, firstname, surname
     * @param _keyword
     * @return una lista de objetos <tt>Subject</tt> que coinciden con la
     * palabra clave dada.
     */
    public List<Subject> find(String _keyword) {
        setKeyword(_keyword);
        super.setSessionParameter("KEYWORD", getKeyword()); //Enviar parametro de sessión
        return subjectHome.find(_keyword);
    }

    /**
     * Mostrar el formulario para edición de clientes
     * @param params
     * @return
     */
    public boolean mostrarFormularioProfile(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_PROFILE, width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioProfile() {

        setUseDefaultCustomer(false); //Implica que se agregará un nuevo usuario
        if (getCustomer() != null
                && getCustomer().isPersistent()
                && !"9999999999999".equals(getCustomer().getCode())) {
            super.setSessionParameter("CUSTOMER", getCustomer());
        }
        return mostrarFormularioProfile(null);
    }

    public void closeFormularioProfile(Object data) {
        removeSessionParameter("KEYWORD");
        removeSessionParameter("CUSTOMER");
        super.closeDialog(data);
    }

    public void loadSessionParameters() {

        if (existsSessionParameter("CUSTOMER")) {
            this.subjectAdminHome.setSubjectEdit((Subject) getSessionParameter("CUSTOMER"));
        } else if (existsSessionParameter("KEYWORD")) {
            Subject _subject = subjectService.createInstance();
            _subject.setCode((String) getSessionParameter("KEYWORD"));
            this.subjectAdminHome.setSubjectEdit(_subject);
        }
    }

    public boolean isOrderByCode() {
        return orderByCode;
    }

    public void setOrderByCode(boolean orderByCode) {
        this.orderByCode = orderByCode;
    }

    /////////////////////////////////////////////////////////////////////////
    // Chart data model
    /////////////////////////////////////////////////////////////////////////
    private LineChartModel balanceLineChartModel;

    public LineChartModel getBalanceLineChartModel() {
        if (balanceLineChartModel == null) {
            setBalanceLineChartModel(createLineChartModel());
        }
        return balanceLineChartModel;
    }

    public void cleanChartModels() {
        setBalanceLineChartModel(null);
    }

    public void setBalanceLineChartModel(LineChartModel balanceLineChartModel) {
        this.balanceLineChartModel = balanceLineChartModel;
    }

    private LineChartModel createLineChartModel() {
        LineChartModel areaModel = new LineChartModel();

        boolean fillSeries = true;
//        
//        LineChartSeries fixedCosts = new LineChartSeries();
//        fixedCosts.setFill(!fillSeries);
//        fixedCosts.setLabel(I18nUtil.getMessages("app.fede.costs.fixed"));
//        fixedCosts.setShowMarker(false);
//        fixedCosts.setSmoothLine(false);

        LineChartSeries sales = new LineChartSeries();
        sales.setFill(fillSeries);
        sales.setLabel(I18nUtil.getMessages("app.fede.sales"));

        LineChartSeries purchases = new LineChartSeries();
        purchases.setFill(fillSeries);
        purchases.setLabel(I18nUtil.getMessages("common.purchases"));

        LineChartSeries profits = new LineChartSeries();
        profits.setFill(fillSeries);
        profits.setLabel(I18nUtil.getMessages("common.profit"));

        Date _start = getStart();
        if (Dates.calculateNumberOfDaysBetween(getStart(), getEnd()) <= 1) {
            int range = Integer.parseInt(settingHome.getValue("app.fede.chart.range", "7"));
            _start = Dates.addDays(getStart(), -1 * range);
        }
        Date _step = _start;
        String label = "";
        BigDecimal salesTotal;
        BigDecimal purchasesTotal;
//        BigDecimal fixedCost = new BigDecimal(settingHome.getValue("app.fede.costs.fixed", "50"));
        for (int i = 0; i <= Dates.calculateNumberOfDaysBetween(_start, getEnd()); i++) {
            label = Strings.toString(_step, Calendar.DAY_OF_WEEK) + ", " + Dates.get(_step, Calendar.DAY_OF_MONTH);
            salesTotal = calculeTotal(findInvoices(subject, DocumentType.INVOICE, 0, Dates.minimumDate(_step), Dates.maximumDate(_step)));
            sales.set(label, salesTotal);

            facturaElectronicaHome.setStart(Dates.minimumDate(_step));
            facturaElectronicaHome.setEnd(Dates.maximumDate(_step));
            purchasesTotal = facturaElectronicaHome.calculeTotal(facturaElectronicaHome.getResultList());

//            fixedCosts.set(label, fixedCost);
            purchases.set(label, purchasesTotal);
            profits.set(label, salesTotal.subtract(purchasesTotal)); //Utilidad bruta

            _step = Dates.addDays(_step, 1); //Siguiente día
        }

        areaModel.addSeries(sales);
        areaModel.addSeries(purchases);
        areaModel.addSeries(profits);
//        areaModel.addSeries(fixedCosts);

        areaModel.setTitle(I18nUtil.getMessages("app.fede.chart.salesvspurchases"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.chart.legendPosition", "ne"));
        areaModel.setExtender("skinChart");
        //areaModel.setExtender("chartExtender");
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("app.fede.chart.date.day.scale"));
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("app.fede.chart.sales.scale"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.chart.sales.scale.min", "-500")));
        yAxis.setMax(Integer.valueOf(settingHome.getValue("app.fede.chart.sales.scale.max", "500")));

        return areaModel;
    }

    
    public BigDecimal calculeCandidateDetailTotal() {
        BigDecimal total = new BigDecimal(BigInteger.ZERO);
        for (Detail d : getCandidateDetails()) {
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
        return inventoryHome.buildLineBarChartModel(getSelectedBussinesEntities(), "skinChart");
    }

    @Override
    protected void initializeDateInterval() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            if (null == getInterval()) {
                range = 0; //El día de hoy
            }

            switch (getInterval().intValue()) {
                case -1:
                    range = Dates.get(Dates.now(), Calendar.DAY_OF_MONTH) - 1;
                    break;
                default:
                    range = getInterval().intValue();
                    break;
            }
        } catch (java.lang.NumberFormatException nfe) {
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
    }

}
