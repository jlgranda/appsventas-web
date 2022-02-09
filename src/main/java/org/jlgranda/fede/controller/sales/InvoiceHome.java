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
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.RecordTemplateService;
import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import com.jlgranda.fede.ejb.sales.DetailService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import com.jlgranda.fede.ejb.sales.KardexDetailService;
import com.jlgranda.fede.ejb.sales.KardexService;
import com.jlgranda.fede.ejb.sales.PaymentService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.talentohumano.EmployeeService;
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
import java.util.Locale;
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
import javax.faces.model.SelectItem;
import javax.inject.Inject;

import org.jlgranda.fede.controller.FacturaElectronicaHome;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.SubjectHome;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.controller.admin.TemplateHome;
import org.jlgranda.fede.controller.inventory.InventoryHome;
import org.jlgranda.fede.controller.sales.report.AdhocCustomizerReport;
import org.jlgranda.fede.model.Detailable;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.model.sales.Detail;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jlgranda.fede.model.sales.Invoice;
import org.jlgranda.fede.model.sales.KardexDetail;
import org.jlgranda.fede.model.sales.Payment;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.ui.model.LazyInvoiceDataModel;
import org.jlgranda.rules.RuleRunner;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.kie.internal.builder.KnowledgeBuilderErrors;
import org.primefaces.PrimeFaces;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

/**
 * Controlador de aplicación de ventas
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class InvoiceHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 115507468383355922L;

    Logger logger = LoggerFactory.getLogger(InvoiceHome.class);

    @EJB
    AccountCache accountCache;

    @Inject
    private Subject subject;

    private Subject customer;

    @Inject
    private SettingHome settingHome;

    @Inject
    private SubjectHome subjectHome;

    /**
     * Para almacemar la cantidad en el formulario de pedido rápido
     */
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

    @EJB
    private EmployeeService employeeService;

    @Inject
    private InventoryHome inventoryHome;

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
    private List<Invoice> filteredInvoices = new ArrayList<>(); // --
    private List<Invoice> myLastCourtesies = new ArrayList<>();

    @Inject
    private FacturaElectronicaHome facturaElectronicaHome;

    @Inject
    private TemplateHome templateHome;

    @Inject
    private SubjectAdminHome subjectAdminHome; //para administrar clientes en factura

    private boolean orderByCode;

    private String sortOrder = "DESCENDING";

    private Long interval; //Intervalo de tiempo

    private BigDecimal totalOverdues;

    @Inject
    private OrganizationData organizationData;

    @EJB
    private KardexService kardexService;

    @EJB
    private KardexDetailService kardexDetailService;

    @EJB
    private RecordService recordService;

    @EJB
    private GeneralJournalService generalJournalService;

    @EJB
    private AccountService accountService;

    @EJB
    private RecordTemplateService recordTemplateService;

    private List<FilterMeta> filterBy;

    protected List<Invoice> selectedInvoices;

    private boolean recordCompleto;

    private Account accountPaymentSelected;

    @PostConstruct
    private void init() {

        initializeDateInterval();

        setAmount(0L); //Sólo si se establece un valor
        setInvoice(invoiceService.createInstance());
        setCandidateDetail(detailService.createInstance(1));
        setPayment(paymentService.createInstance());

        setDocumentType(DocumentType.PRE_INVOICE); //Listar prefacturas por defecto
        setUseDefaultCustomer(true); //Usar consumidor final por defecto
        setUseDefaultEmail(false); //Usar consumidor final por ahora
        setNonnative(false); //Es extrangero
        setBusquedaEjecutada(!Strings.isNullOrEmpty(getKeyword()));
        updateDefaultEmail();

        getSubjectAdminHome().setOutcome(this.organizationData.getOrganization() != null ? this.organizationData.getOrganization().getVistaVenta() : "invoice");

        setOrderByCode(false);

        setBusquedaAvanzada(true);

        filterBy = new ArrayList<>();

//        filterBy.add(FilterMeta.builder()
//                .field("summary")
//                .filterValue()
//                .matchMode(MatchMode.CONTAINS)
//                .build());
//
//        filterBy.add(FilterMeta.builder()
//                .field("date")
//                .filterValue(Arrays.asList(LocalDate.now().minusDays(28), LocalDate.now().plusDays(28)))
//                .matchMode(MatchMode.RANGE)
//                .build());
        initializeActions();

        //Establecer variable de sistema que habilita o no el registro contable
        setAccountingEnabled(this.organizationData.getOrganization() != null ? this.organizationData.getOrganization().isAccountingEnabled() : false);
        setOutcome(this.organizationData.getOrganization() != null ? this.organizationData.getOrganization().getVistaVentas() : "preinvoices");

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

    public void updatePrintAlias() {
        String alias = settingHome.getValue("app.fede.sales.invoice.defaultprintalias", "Alimentación");
        if (Strings.isNullOrEmpty(invoice.getPrintAliasSummary())) {
            invoice.setPrintAliasSummary(alias);
        } else if (alias.equalsIgnoreCase(invoice.getPrintAliasSummary())) {
            invoice.setPrintAliasSummary("");
        }
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

    public BigDecimal getTotalOverdues() {
        return totalOverdues;
    }

    public void setTotalOverdues(BigDecimal totalOverdues) {
        this.totalOverdues = totalOverdues;
    }

    /**
     * Obtiene la lista de Pre facturas en estado de PRE_INVOICE para la fecha
     *
     * @return
     */
    public List<Invoice> getMyLastlastPreInvoices() {
        if (myLastlastPreInvoices.isEmpty()) {
            myLastlastPreInvoices = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndDocumentTypeAndEmission", this.organizationData.getOrganization(), getStart(), getEnd(), DocumentType.PRE_INVOICE);
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

        if (invoices == null) {
            return new ArrayList<>();
        }

        invoices.forEach((_invoice) -> {
            _invoice.getDetails().forEach((detail) -> {
                detail.setShowAmountInSummary(false);
            });
        });
        return invoices;

    }

    protected List<Invoice> getMyLastlastInvoices(boolean byAuthor) {
        if (myLastlastInvoices != null && myLastlastInvoices.isEmpty()) {
            if (byAuthor) {
                myLastlastInvoices = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndAuthorAndDocumentTypeAndEmission", this.organizationData.getOrganization(), this.subject, getStart(), getEnd(), DocumentType.INVOICE);
            } else {
                myLastlastInvoices = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndDocumentTypeAndEmission", this.organizationData.getOrganization(), getStart(), getEnd(), DocumentType.INVOICE);
            }
        }
        return myLastlastInvoices;
    }

    public List<Invoice> getMyPendingPreInvoices() {
        Date _end = Dates.addDays(getEnd(), -1); //Desde ayer
        Date _start = Dates.addDays(_end, -30); //Hasta 30 días atras
        if (myPendinglastPreInvoices != null && myPendinglastPreInvoices.isEmpty()) {
            myPendinglastPreInvoices = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndAuthorAndDocumentTypeAndEmission", this.organizationData.getOrganization(), this.subject, _start, _end, DocumentType.PRE_INVOICE);
        }
        return myPendinglastPreInvoices;
    }

    // --Obtener todas mis facturas INVOICES 
    public List<Invoice> getMyAllInvoices() {
        if (myAllInvoices.isEmpty()) {
            myAllInvoices = myLastlastPreInvoices = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndAuthorAndDocumentTypeAndEmission", this.organizationData.getOrganization(), this.subject, getStart(), getEnd(), DocumentType.INVOICE);
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
        if (myOverduelastPreInvoices.isEmpty()) {
            myOverduelastPreInvoices = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndAuthorAndDocumentTypeAndEmission", this.organizationData.getOrganization(), this.subject, getStart(), getEnd(), DocumentType.OVERDUE);
        }
        return myOverduelastPreInvoices;
    }

    public void setMyOverduelastPreInvoices(List<Invoice> myOverduelastPreInvoices) {
        this.myOverduelastPreInvoices = myOverduelastPreInvoices;
    }

    public List<Invoice> getMyLastCourtesies() {
        return getMyLastCourtesies(true);
    }

    public void setMyLastCourtesies(List<Invoice> myLastCourtesies) {
        this.myLastCourtesies = myLastCourtesies;
    }

    protected List<Invoice> getMyLastCourtesies(boolean byAuthor) {
        if (myLastCourtesies != null && myLastCourtesies.isEmpty()) {
            if (byAuthor) {
                myLastCourtesies = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndAuthorAndDocumentTypeAndEmission", this.organizationData.getOrganization(), this.subject, getStart(), getEnd(), DocumentType.COURTESY);
            } else {
                myLastCourtesies = invoiceService.findByNamedQuery("Invoice.findByOrganizationAndDocumentTypeAndEmission", this.organizationData.getOrganization(), getStart(), getEnd(), DocumentType.COURTESY);
            }
        }
        return myLastCourtesies;
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
     * Guarda la entidad marcandola como CLOSE y generando un secuencial valido
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String collect() {
        //Guardar cambios en la entidad invoice
        collect(StatusType.CLOSE.toString());
        return getOutcome(); //Redireccción de vistas
    }

    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String overdue() {
        if (!isUseDefaultCustomer()) {
            getInvoice().setDocumentTypeSource(DocumentType.OVERDUE);
            getPayment().getInvoice().setOwner(getCustomer());
            getPayment().setDatePaymentCancel(null);
            collect(DocumentType.OVERDUE, StatusType.CLOSE.toString());
            //            setOutcome("invoices");
            try {
                redirectTo("/pages/fede/sales/invoices_finder.jsf?documentType=OVERDUE&interval=7&overcome=overdues");
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(InvoiceHome.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            addWarningMessage("¿Quién será reesponsable del crédito?", "Seleccione una persona/entidad como responsable del crédito.");
            setOutcome("currentpage.xhtml?faces-redirect=true");
        }
        return getOutcome();
    }

    /**
     * Guarda la entidad marcandola como COURTESY y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @return outcome de exito o fracaso de la acción
     */
    public String courtesy() {
        if (!isUseDefaultCustomer()) {
            getInvoice().setDocumentTypeSource(DocumentType.COURTESY);
            getPayment().getInvoice().setOwner(getCustomer());
            getPayment().setDatePaymentCancel(null);
            collect(DocumentType.COURTESY, StatusType.CLOSE.toString());
            setOutcome("preinvoices");
        } else {
            addWarningMessage("Recuerde!", "Seleccione una persona/entidad a quien se concedió la cortesía.");
            setOutcome("currentpage.xhtml?faces-redirect=true");
        }
        return getOutcome();
    }

    /**
     * Guarda la entidad marcandola como INVOICE y generando un secuencial
     * valido TODO debe generar también una factura electrónica
     *
     * @param status
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
        if (DocumentType.PRE_INVOICE.equals(getInvoice().getDocumentTypeSource())) {
            getInvoice().setDocumentTypeSource(DocumentType.INVOICE);
        }
        if (DocumentType.INVOICE.equals(documentType) && !DocumentType.COURTESY.equals(getInvoice().getDocumentTypeSource())) {
            getPayment().setDatePaymentCancel(Dates.now());
        }
        calculeChange(); //Calcular el cambio sobre el objeto payment en edición

        if ((BigDecimal.ZERO.compareTo(getPayment().getCash()) == -1)
                && (BigDecimal.ZERO.compareTo(getPayment().getDiscount()) == -1 || BigDecimal.ZERO.compareTo(getPayment().getDiscount()) == 0)
                && (BigDecimal.ZERO.compareTo(getPayment().getChange()) == -1 || BigDecimal.ZERO.compareTo(getPayment().getChange()) == 0)) {
            //getInvoice().setSequencial(sequenceSRI);//Generar el secuencia legal de factura
            getInvoice().setDocumentType(documentType); //Se convierte en factura
            //Agregar el pago
            getInvoice().addPayment(getPayment());
            getPayment().setAmount(getInvoice().getTotal()); //Registrar el total a cobrarse
            getInvoice().setStatus(status);

            //Registrar asiento contable de la compra
            if (getInvoice().getId() != null) {
                registerRecordInJournal();
            } else {
                addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.sales.invoice.accounting.fail"));
            }

//            //Guardar cambios en la entidad invoice
//            save(true);
//            
            //Guardar movimientos en el Kardex
            registerInvoiceDetailsInKardex(this.invoice.getDetails());

            //Enviar saludo a cliente
            sendNotification();
            setOutcome("preinvoices");
        } else {
            addErrorMessage(I18nUtil.getMessages("app.fede.sales.payment.incomplete"), I18nUtil.getFormat("app.fede.sales.payment.detail.incomplete", "" + getInvoice().getTotal()));
            setOutcome("");
        }
        return getOutcome();
    }

    public String print() {
        //try {
        collect(StatusType.PRINTED.toString());
        //Forzar actualizar invoice para generación correcta del reporte
        setInvoice(invoiceService.createInstance());
        getInvoice(); //recargar la instancia actual
        //Imprimir reporte
        Map<String, String> settings = new HashMap<>();
        settings.put("app.fede.report.invoice.startLine", settingHome.getValue("app.fede.report.invoice.startLine", "40"));
        //settings.put("app.fede.report.invoice.fontName", settingHome.getValue("app.fede.report.invoice.fontName", "Epson1"));
        settings.put("app.fede.report.invoice.fontSize", settingHome.getValue("app.fede.report.invoice.fontSize", "9"));
        settings.put("app.fede.report.invoice.fontStyle", settingHome.getValue("app.fede.report.invoice.fontStyle", "plain"));

        AdhocCustomizerReport adhocCustomizerReport = new AdhocCustomizerReport(getInvoice(), settings);
        //InvoiceDesign invoiceDesign = new InvoiceDesign(getInvoice(), settings);
        //Invocar Servlet en nueva ventana del navegador
//            redirectTo("/fedeServlet/?entity=invoice&id=" + getInvoice().getSequencial() + "&type=odt");
        //redirectTo("/fedeServlet/?entity=invoice&id=" + getInvoice().getSequencial() + "&type=pdf");

        //} catch (IOException ex) {
        //    java.util.logging.Logger.getLogger(InvoiceHome.class.getName()).log(Level.SEVERE, null, ex);
        //}
        return this.getOutcome();
    }

    /**
     * Registra el pago de forma directa
     *
     * @param invoiceId
     * @return la regla de navegación
     */
    public String record(Long invoiceId) {
        this.setInvoiceId(invoiceId);
        //load invoice
        getInvoice();
        getInvoice().setDescription(settingHome.getValue("app.fede.status.pay_direct", StatusType.PAID_DIRECT.toString()));
        return this.collect(DocumentType.INVOICE, StatusType.CLOSE.toString());
    }

    /**
     * Registra como atendida. Cambia unicamente el status
     *
     * @param invoiceId
     * @return la regla de navegación
     */
    public String attend(Long invoiceId) {
        String outcome = this.organizationData.getOrganization().getVistaVentas();
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
     *
     * @param invoiceId
     * @throws java.io.IOException
     */
    public void reopen(Long invoiceId) throws IOException {
        //redirectTo("/pages/fede/sales/" + this.organizationData.getOrganization().getVistaVenta() + ".jsf?invoiceId=" + invoiceId);
        redirectTo(this.organizationData.getOrganization().getVistaVenta() + "?invoiceId=" + invoiceId);
    }

    /**
     * Guarda los cambios en el objeto Invoice
     *
     * @return
     */
    public String save() {
        calculeChange(); //Calcular el cambio sobre el objeto payment en edición
        if (getPayment().getDiscount().compareTo(BigDecimal.ZERO) == 1 && getInvoice().getDescription().equals("")) {
            addErrorMessage(I18nUtil.getMessages("app.fede.sales.payment.incomplete"), I18nUtil.getFormat("app.fede.sales.payment.description"));
            setOutcome("");
        } else {
            if ((BigDecimal.ZERO.compareTo(getPayment().getCash()) == -1)
                    && (BigDecimal.ZERO.compareTo(getPayment().getDiscount()) == -1 || BigDecimal.ZERO.compareTo(getPayment().getDiscount()) == 0)
                    && (BigDecimal.ZERO.compareTo(getPayment().getChange()) == -1 || BigDecimal.ZERO.compareTo(getPayment().getChange()) == 0)) {
                getInvoice().setDocumentType(DocumentType.PRE_INVOICE); //Mantener como preinvoice
                getInvoice().setDocumentTypeSource(DocumentType.PRE_INVOICE); //Mantener como preinvoice
                getPayment().setAmount(getInvoice().getTotal()); //Registrar el total a cobrarse
                getInvoice().addPayment(getPayment());
                setOutcome("preinvoices");
                setOutcome(save(false));
            } else {
                addErrorMessage(I18nUtil.getMessages("app.fede.sales.payment.incomplete"), I18nUtil.getFormat("app.fede.sales.payment.detail.incomplete", "" + getInvoice().getTotal()));
                setOutcome("");
            }
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
            getInvoice().setOrganization(this.organizationData.getOrganization());
            getCandidateDetails().stream().forEach((d) -> {
                if (d.isPersistent()) { //Actualizar la cantidad
                    getInvoice().replaceDetail(d);
                } else {
                    if (BigDecimal.ZERO.compareTo(d.getAmount()) != 0) {
                        d.setProductId(d.getProduct().getId());
                        d.setPrice(d.getProduct().getPrice());
                        d.setUnit(d.getUnit());
                        d.setAmount(this.getAmount().compareTo(0L) == 0 ? d.getAmount() : BigDecimal.valueOf(this.getAmount())); //Almacenar el valor del datalle, si no es vía el formulario rápido
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

        //Para realizar una nueva búsqueda
        this.lazyDataModel = null;
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
        iva = subTotal.multiply(BigDecimal.valueOf(invoice.IVA));
        return iva;
    }

    public void calculeChange() {
        BigDecimal subtotal = calculeCandidateDetailTotal();
        subtotal = subtotal.subtract(getPayment().getDiscount());
        subtotal = subtotal.add(subtotal.multiply(BigDecimal.valueOf(Invoice.IVA)));
        if (subtotal.compareTo(getPayment().getCash()) > 0) {
            getPayment().setCash(subtotal); //Asumir que se entregará exacto, si no se ha indicado nada
        }
        getPayment().setChange(getPayment().getCash().subtract(subtotal));
        //Se modificó el detalle, cambiar a estado abierto
        getInvoice().setStatus(StatusType.OPEN.toString());
        //Valores Impresos
    }

    public LazyInvoiceDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyInvoiceDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public void filter() {
        //Todos los documentos, independientemente del cajero
        filter(null, Dates.minimumDate(getStart()), Dates.maximumDate(getEnd()), this.documentType, getKeyword(), getTags());
    }

    public void filter(Subject _subject, Date _start, Date _end, DocumentType _documentType, String _keyword, String _tags) {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyInvoiceDataModel(invoiceService);
        }

        if (_start != null) {
            lazyDataModel.setStart(getStart());
        }
        if (_end != null) {
            lazyDataModel.setEnd(getEnd());
        }

        if (_subject != null) {
            lazyDataModel.setAuthor(_subject);
        }

        lazyDataModel.setOrganization(this.organizationData.getOrganization());
        lazyDataModel.setDocumentType(_documentType);

        if (!Strings.isNullOrEmpty(_keyword) && _keyword.startsWith("table:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setBoardNumber(parts[1]);
            }
            lazyDataModel.setFilterValue(null);//No buscar por keyword
        } else if (!Strings.isNullOrEmpty(_keyword) && _keyword.startsWith("label:")) {
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
                //redirectTo("/pages/fede/ride.jsf?key=" + ((BussinesEntity) event.getObject()).getId());
                this.reopen(((Invoice) event.getObject()).getId());
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InvoiceHome.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());

        FacesContext.getCurrentInstance().addMessage(null, msg);
        this.selectedBussinesEntities.remove((Invoice) event.getObject());
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
        if (touch(p)) {
            //Agregar a lista de últimos agregados desde autocomplete
            getRecents().add(p);
        }
        return true;
    }

    public boolean touch(Product product) {
        if (product == null) {
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("common.requiredMessage"));
            logger.error(I18nUtil.getMessages("common.selected.product.none"));
            return false;
        }
        setCandidateDetail(detailService.createInstance(1));
        getCandidateDetail().setProduct(product);
        getCandidateDetail().setPrice(candidateDetail.getProduct().getPrice());//Establecer el precio actual
        getCandidateDetail().setInvoice(getInvoice());
        if (this.candidateDetails.contains(getCandidateDetail())) {
            int index = this.candidateDetails.indexOf(getCandidateDetail());
            BigDecimal amount_ = this.candidateDetails.get(index).getAmount().add(candidateDetail.getAmount());
            this.candidateDetails.get(index).setAmount(amount_);
        } else {
            this.candidateDetails.add(getCandidateDetail());
        }
        this.getInvoice().setDetails(this.candidateDetails);
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

    public List<Invoice> getSelectedInvoices() {
        return selectedInvoices;
    }

    public void setSelectedInvoices(List<Invoice> selectedInvoices) {
        this.selectedInvoices = selectedInvoices;
    }

    public boolean isRecordCompleto() {
        return recordCompleto;
    }

    public void setRecordCompleto(boolean recordCompleto) {
        this.recordCompleto = recordCompleto;
    }

    public Account getAccountPaymentSelected() {
        return accountPaymentSelected;
    }

    public void setAccountPaymentSelected(Account accountPaymentSelected) {
        this.accountPaymentSelected = accountPaymentSelected;
    }

    /**
     * Busca objetos <tt>Subject</tt> para la clave de búsqueda en las columnas
     * usernae, firstname, surname
     *
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
     *
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

    public List<Invoice> getFilteredInvoices() {
        return filteredInvoices;
    }

    public void setFilteredInvoices(List<Invoice> filteredInvoices) {
        this.filteredInvoices = filteredInvoices;
    }

    public List<FilterMeta> getFilterBy() {
        return filterBy;
    }

    public void setFilterBy(List<FilterMeta> filterBy) {
        this.filterBy = filterBy;
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
        sales.setLabel(I18nUtil.getMessages("app.fede.sales.net"));

        LineChartSeries purchases = new LineChartSeries();
        purchases.setFill(fillSeries);
        purchases.setLabel(I18nUtil.getMessages("app.fede.inventory.purchases"));

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

        areaModel.setTitle(I18nUtil.getMessages("app.fede.smart.salesvspurchases"));
        areaModel.setLegendPosition(settingHome.getValue("app.fede.barchart.legendPosition", "ne"));
        areaModel.setExtender("skinChart");
        //areaModel.setExtender("chartExtender");
        areaModel.setAnimate(false);
        areaModel.setShowPointLabels(false);

        Axis xAxis = new CategoryAxis(I18nUtil.getMessages("common.day"));
        areaModel.getAxes().put(AxisType.X, xAxis);
        Axis yAxis = areaModel.getAxis(AxisType.Y);
        yAxis.setLabel(I18nUtil.getMessages("common.dollars"));
        yAxis.setMin(Integer.valueOf(settingHome.getValue("app.fede.barchart.scale.min", "-500")));
        yAxis.setMax(Integer.valueOf(settingHome.getValue("app.fede.barchart.scale.max", "500")));

        return areaModel;
    }

    public BigDecimal calculeCandidateDetailTotal() {
        BigDecimal total = new BigDecimal(BigInteger.ZERO);
        for (Detail d : getCandidateDetails()) {
            total = total.add(d.getPrice().multiply(d.getAmount()));
        }
        return total;
    }

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

    /**
     * Envia al medio persistente los detalles del invoice
     *
     * @param details
     */
    private void registerInvoiceDetailsInKardex(List<Detail> details) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><");
        System.out.println("details:::::"+details);
        kardexService.save(
                makeDetailableList(details),
                settingHome.getValue("app.inventory.kardex.code.prefix", "TK-P-"),
                subject, this.organizationData.getOrganization(),
                KardexDetail.OperationType.VENTA);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><");
    }

    @Override
    protected void initializeDateInterval() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            if (getInterval() != null && getInterval() > 0) {
                range = getInterval().intValue();
            } else {
                range = Integer.valueOf(settingHome.getValue("fede.preinvoices.range", "0"));
            }
            setEnd(Dates.maximumDate(Dates.now()));
            setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 1;
        }
    }

    public boolean globalFilterFunction(Object value, Object filter, Locale locale) {
        String filterText = (filter == null) ? null : filter.toString().trim().toLowerCase();
        if (Strings.isNullOrEmpty(filterText)) {
            return true;
        }

        //int filterInt = Strings.toInt(filterText);
        Invoice _invoice = (Invoice) value;
        return _invoice.getSummary().toLowerCase().contains(filterText);
    }

    /**
     * Enviar agradecimiento de compra
     */
    public void sendNotification() {
        if (this.invoice.isPersistent() && !this.isUseDefaultCustomer()) {
            //Agradecimiento compra
            String url = this.organizationData.getOrganization().getUrl();
            String url_title = this.organizationData.getOrganization().getName();

            Map<String, Object> values = new HashMap<>();
            values.put("firstname", this.invoice.getOwner().getFirstname());
            values.put("fullname", this.invoice.getOwner().getFullName());
            values.put("organization", this.organizationData.getOrganization().getInitials()); //Nombre comercial
            values.put("url", url);
            values.put("url_title", url_title);

            this.sendNotification(templateHome, settingHome, this.invoice.getOwner(), values, "app.mail.template.invoice.thanks", true);
        }
    }

    private List<Detailable> makeDetailableList(List<Detail> details) {
        List<Detailable> datailables = new ArrayList<>();
        details.forEach(d -> {
            datailables.add((Detailable) d);
        });

        return datailables;
    }

    //Acciones sobre seleccionados
    public void execute() {
        Invoice p = null;
        if (this.isActionExecutable()) {
            if ("collect".equalsIgnoreCase(this.selectedAction)) {
                for (Invoice inv : this.getSelectedInvoices()) {
                    this.invoice = inv;
                    List<Payment> payments = paymentService.findByNamedQuery("Payment.findByInvoice", this.invoice);
                    if (!payments.isEmpty()) {
                        setPayment(payments.get(0));
                    } else {
                        getPayment().setAmount(this.invoice.getTotal());
                        getPayment().setCash(this.invoice.getTotal());
                        getPayment().setChange(BigDecimal.ZERO);
                    }
                    getPayment().setDatePaymentCancel(Dates.now());
                    collect(DocumentType.INVOICE, StatusType.CLOSE.toString());
                }
                setOutcome("");
//                try {
//                    redirectTo("/pages/fede/sales/invoices_finder.jsf?documentType=OVERDUE&interval=7&overcome=overdues");
//                } catch (IOException ex) {
//                    java.util.logging.Logger.getLogger(InvoiceHome.class.getName()).log(Level.SEVERE, null, ex);
//                }
                this.totalOverdues = BigDecimal.ZERO;
                this.selectedInvoices.removeAll(this.getSelectedInvoices());
                this.selectedInvoices.clear();
            }
            /*else if ("moveto".equalsIgnoreCase(this.selectedAction) && this.getGroupSelected() != null){
                for (BussinesEntity be : this.getSelectedBussinesEntities()){
                    p = (Product) be;
                    p.setCategory(this.getGroupSelected());
                    this.productService.save(p.getId(), p); //Actualizar el tipo de producto
                }
                setOutcome("");
            } else if ("changeto".equalsIgnoreCase(this.selectedAction) && this.getProductType()!= null){
                for (BussinesEntity be : this.getSelectedBussinesEntities()){
                    p = (Product) be;
                    p.setProductType(this.getProductType());
                    this.productService.save(p.getId(), p); //Actualizar el tipo de producto
                    this.productCache.load(); //Actualizar el cache
                }
                setOutcome("");
            }*/
        }
    }

    public boolean isActionExecutable() {
        if ("collect".equalsIgnoreCase(this.selectedAction)) {
            return true;
        }/* else if ("moveto".equalsIgnoreCase(this.selectedAction) && this.getGroupSelected() != null){
            return true;
        } else if ("changeto".equalsIgnoreCase(this.selectedAction) && this.getProductType()!= null){
            return true;
        }*/
        return false;
    }

    private void initializeActions() {
        this.actions = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        actions.add(item);

        item = new SelectItem("collect", I18nUtil.getMessages("common.collect"));
        actions.add(item);

//        item = new SelectItem("moveto", "Mover a categoría");
//        actions.add(item);
//        
//        item = new SelectItem("changeto", "Cambiar tipo a");
//        actions.add(item);
    }

    public void calculateTotalOverdue() {
        this.totalOverdues = BigDecimal.ZERO;
        for (Invoice p : this.getSelectedInvoices()) {
            this.totalOverdues = this.totalOverdues.add(p.getTotal().subtract(p.getPaymentsDiscount()));
        }
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        Payment _instance = (Payment) fuenteDatos;

        RecordTemplate _recordTemplate = this.recordTemplateService.findUniqueByNamedQuery("RecordTemplate.findByCode", nombreRegla, this.organizationData.getOrganization());
        Record record = null;

        if (isAccountingEnabled() && _recordTemplate != null && !Strings.isNullOrEmpty(_recordTemplate.getRule())) {
            record = recordService.createInstance();
            RuleRunner ruleRunner1 = new RuleRunner();
            KnowledgeBuilderErrors kbers = ruleRunner1.run(_recordTemplate, _instance, record); //Armar el registro contable según la regla en recordTemplate
            if (kbers != null) { //Contiene errores de compilación
                logger.error(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("bussines.entity.rule.erroroncompile", "" + _recordTemplate.getCode(), _recordTemplate.getName()));
                logger.error(kbers.toString());
                record = null; //Invalidar el record
            } else {
                record.setBussinesEntityType(_instance.getClass().getSimpleName());
                record.setBussinesEntityId(_instance.getId());
                record.setBussinesEntityHashCode(_instance.hashCode());
                record.setName(String.format("%s: %s[id=%d]", _recordTemplate.getName(), getClass().getSimpleName(), _instance.getId()));
                if (employeeService.findByNamedQueryWithLimit("Employee.findByOwnerAndOrganization", 1, _instance.getOwner(), this.organizationData.getOrganization()).isEmpty()) {
                    record.setDescription(String.format("Cliente: %s \nDetalle: %s \nTotal del pedido: %s", _instance.getInvoice().getOwner().getFullName(), _instance.getInvoice().getSummary(), Strings.format(_instance.getInvoice().getTotal().doubleValue(), "$ #0.##")));
                } else {
                    record.setDescription(String.format("Empleado: %s \nDetalle: %s \nTotal del pedido: %s", _instance.getInvoice().getOwner().getFullName(), _instance.getInvoice().getSummary(), Strings.format(_instance.getInvoice().getTotal().doubleValue(), "$ #0.##")));
                }
            }

        }
        //El registro casí listo para agregar al journal
        return record;
    }

    public void registerRecordInJournal() {

        if (isAccountingEnabled()) {

            //Ejecutar las reglas de negocio para el registro de ventas
            if (DocumentType.INVOICE.equals(getInvoice().getDocumentType())) {
                if (DocumentType.OVERDUE.equals(getInvoice().getDocumentTypeSource())) {
                    if (getPayment().getMethod().equals("TRANSFERENCIA")) {
                        if (employeeService.findByNamedQueryWithLimit("Employee.findByOwnerAndOrganization", 1, getInvoice().getOwner(), this.organizationData.getOrganization()).isEmpty()) {
                            setReglas(settingHome.getValue("app.fede.accounting.rule.registroventascreditoclientescobrotransferencia", "REGISTRO_VENTAS_CREDITO_CLIENTES_COBRO_TRANSFERENCIA"));
                        } else {
                            setReglas(settingHome.getValue("app.fede.accounting.rule.registroventascreditoempleadoscobrotransferencia", "REGISTRO_VENTAS_CREDITO_EMPLEADOS_COBRO_TRANSFERENCIA"));
                        }
                    } else {
                        if (employeeService.findByNamedQueryWithLimit("Employee.findByOwnerAndOrganization", 1, getInvoice().getOwner(), this.organizationData.getOrganization()).isEmpty()) {
                            setReglas(settingHome.getValue("app.fede.accounting.rule.registroventascreditoclientescobroefectivo", "REGISTRO_VENTAS_CREDITO_CLIENTES_COBRO_EFECTIVO"));
                        } else {
                            setReglas(settingHome.getValue("app.fede.accounting.rule.registroventascreditoempleadoscobroefectivo", "REGISTRO_VENTAS_CREDITO_EMPLEADOS_COBRO_EFECTIVO"));
                        }
                    }
                } else {
                    if (DocumentType.COURTESY.equals(getInvoice().getDocumentTypeSource())) {
                        setReglas(settingHome.getValue("app.fede.accounting.rule.registrocortesiasauspicioscobroefectivo", "REGISTRO_CORTESIAS_AUSPICIOS_COBRO_EFECTIVO"));
                    } else {
                        if (getPayment().getMethod().equals("TRANSFERENCIA")) {
                            setReglas(settingHome.getValue("app.fede.accounting.rule.registroventastransferencia", "REGISTRO_VENTAS_TRANSFERENCIA"));
                        } else {
                            setReglas(settingHome.getValue("app.fede.accounting.rule.registroventasefectivo", "REGISTRO_VENTAS_EFECTIVO"));
                        }
                    }
                }
            } else {
                if (DocumentType.COURTESY.equals(getInvoice().getDocumentType())) {
                    setReglas(settingHome.getValue("app.fede.accounting.rule.registrocortesiasauspicios", "REGISTRO_CORTESIAS_AUSPICIOS"));
                } else if (DocumentType.OVERDUE.equals(getInvoice().getDocumentType())) {
                    if (employeeService.findByNamedQueryWithLimit("Employee.findByOwnerAndOrganization", 1, getInvoice().getOwner(), this.organizationData.getOrganization()).isEmpty()) {
                        setReglas(settingHome.getValue("app.fede.accounting.rule.registroventascreditoclientes", "REGISTRO_VENTAS_CREDITO_CLIENTES"));
                    } else {
                        setReglas(settingHome.getValue("app.fede.accounting.rule.registroventascreditoempleados", "REGISTRO_VENTAS_CREDITO_EMPLEADOS"));
                    }
                }
            }

            List<Record> records = new ArrayList<>();
            getReglas().forEach(regla -> {
                Record r = aplicarReglaNegocio(regla, getPayment());
                if (r != null) {
                    records.add(r);
                }
            });

            if (!records.isEmpty()) {
                //La regla compiló bien
                String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", I18nUtil.getMessages("app.fede.accounting.journal"));
                String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
                GeneralJournal generalJournal = generalJournalService.find(this.invoice.getEmissionOn(), this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);

                //El General Journal del día
                if (generalJournal != null) {
                    for (Record rcr : records) {
                        this.recordCompleto = Boolean.TRUE;

                        rcr.setCode(UUID.randomUUID().toString());

                        rcr.setOwner(this.subject);
                        rcr.setAuthor(this.subject);

                        rcr.setGeneralJournalId(generalJournal.getId());

                        //Corregir objetos cuenta en los detalles
                        rcr.getRecordDetails().forEach(rcrd -> {
                            rcrd.setLastUpdate(Dates.now());
                            if (rcrd.getAccountName().contains("$CEDULA")) {
                                Account cuentaPadreDetectada = accountCache.lookupByName(rcrd.getAccountName().substring(0, rcrd.getAccountName().length() - 8), this.organizationData.getOrganization());
                                if (cuentaPadreDetectada != null && cuentaPadreDetectada.getId() != null) {
                                    String nombreCuentaHija = cuentaPadreDetectada.getName().concat(" ").concat(getInvoice().getOwner().getFullName());
                                    Account cuentaHija = accountCache.lookupByName(nombreCuentaHija, this.organizationData.getOrganization());
                                    if (cuentaHija == null) {
                                        cuentaHija = accountService.createInstance();//crear la cuenta
                                        cuentaHija.setCode(this.accountCache.genereNextCode(cuentaPadreDetectada.getId()));
                                        cuentaHija.setCodeType(CodeType.SYSTEM);
                                        cuentaHija.setUuid(UUID.randomUUID().toString());
                                        cuentaHija.setName(nombreCuentaHija.toUpperCase());
                                        cuentaHija.setDescription(cuentaHija.getName());
                                        cuentaHija.setParentAccountId(cuentaPadreDetectada.getId());
                                        cuentaHija.setOrganization(this.organizationData.getOrganization());
                                        cuentaHija.setAuthor(this.subject);
                                        cuentaHija.setOwner(this.subject);
                                        cuentaHija.setOrden(Short.MIN_VALUE);
                                        cuentaHija.setPriority(0);
                                        accountService.save(cuentaHija.getId(), cuentaHija);
                                        this.accountCache.load(); //recargar todas las cuentas
                                    }
                                    rcrd.setAccount(cuentaHija);
                                    rcrd.setAccountName(rcrd.getAccount().getName());
                                }
                            } else if (rcrd.getAccountName().contains("$BANCO")) {
                                rcrd.setAccount(this.accountPaymentSelected);
                                rcrd.setAccountName(rcrd.getAccount().getName());
                            } else {
                                rcrd.setAccount(accountCache.lookupByName(rcrd.getAccountName(), this.organizationData.getOrganization()));
                            }
                            if (rcrd.getAccount() == null) {
                                this.recordCompleto = Boolean.FALSE;
                            }
                        });

                        //Persistencia
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                        System.out.println("this.recordCompleto:::" + this.recordCompleto);
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                        if (Boolean.TRUE.equals(this.recordCompleto)) {
                            rcr = recordService.save(rcr);
                            if (rcr.getId() != null) {
                                //Anular registros anteriores
                                //recordService.deleteLastRecords(generalJournal.getId(), getInvoice().getClass().getSimpleName(), getInvoice().getId(), getInvoice().hashCode());
                                if (getPayment().getRecordId() != null) {
                                    recordService.deleteRecord(getPayment().getRecordId());
                                    getPayment().setRecordId(null);
                                }
                                getPayment().setRecordId(rcr.getId());
                                paymentService.save(getPayment().getId(), getPayment()); //Se guardan todos los cambios
                                if (getPayment().getRecordId() != null) {
                                } else {
                                    addWarningMessage(I18nUtil.getMessages("action.warning"), "El registro contable, no se asoció a la Venta.");
                                }
                            }
                        } else {
                            PrimeFaces current = PrimeFaces.current();
                            current.executeScript("PF('myDialogVar').show();");
                        }
                    }
                }
            }
        }
    }
}
//    @Override
//    protected void initializeDateInterval() {
//        int range = 0; //Rango de fechas para visualiar lista de entidades
//        try {
//            if (null == getInterval()) {
//                range = 0; //El día de hoy
//            }
//
//            switch (getInterval().intValue()) {
//                case -1:
//                    range = Dates.get(Dates.now(), Calendar.DAY_OF_MONTH) - 1;
//                    break;
//                default:
//                    range = getInterval().intValue();
//                    break;
//            }
//        } catch (java.lang.NumberFormatException nfe) {
//            range = 7;
//        }
//        setEnd(Dates.maximumDate(Dates.now()));
//        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
//    }
//    private void registerDetailInKardex() {
//        for (Detail candidateDetail1 : getCandidateDetails()) {
//            Kardex kardex = null;
//            KardexDetail kardexDetail = null;
//            kardex = kardexService.findUniqueByNamedQuery("Kardex.findByProductAndOrg", candidateDetail1.getProduct(), this.organizationData.getOrganization());
//            if (kardex == null) {
//                kardex = kardexService.createInstance();
//                kardex.setOwner(this.subject);
//                kardex.setAuthor(this.subject);
//                kardex.setOrganization(this.organizationData.getOrganization());
//                kardex.setProduct(candidateDetail1.getProduct());
//                kardex.setName(candidateDetail1.getProduct().getName());
//                kardex.setUnitMinimum(1L);
//                kardex.setUnitMaximum(1L);
//            } else {
//                kardex.setAuthor(this.subject); //Saber quien lo modificó por última vez
//                kardexDetail = kardexDetailService.findUniqueByNamedQuery("KardexDetail.findByKardexAndInvoiceAndOperation", kardex, candidateDetail1.getInvoice(), KardexDetail.OperationType.VENTA);
//            }
//            if (kardexDetail == null) {
//                kardexDetail = kardexDetailService.createInstance();
//                kardexDetail.setOwner(this.subject);
//                kardexDetail.setAuthor(this.subject);
//                kardexDetail.setInvoice(candidateDetail1.getInvoice());
//                kardexDetail.setOperationType(KardexDetail.OperationType.VENTA);
//            } else {
//                //Aumentar los valores acumulados de cantidad y total para al momento de modificar no se duplique el valor a disminuir por la venta
//                if (kardexDetail.getQuantity() != null && kardexDetail.getTotalValue() != null) {
//                    kardex.setQuantity(kardex.getQuantity() + kardexDetail.getQuantity());
//                    kardex.setFund(kardex.getFund().add(kardexDetail.getTotalValue()));
//                    kardexDetail.setCummulativeQuantity(kardex.getQuantity());
//                    kardexDetail.setCummulativeTotalValue(kardex.getFund());
//                }
//                kardexDetail.setAuthor(this.subject); //Saber quien lo modificó por última vez
//                kardexDetail.setLastUpdate(Dates.now()); //Saber la hora que modificó por última vez
//            }
//            kardexDetail.setCode(candidateDetail1.getInvoice().getSequencial());
//            kardexDetail.setUnitValue(candidateDetail1.getPrice());
//            kardexDetail.setQuantity((long) candidateDetail1.getAmount());
//            kardexDetail.setTotalValue(kardexDetail.getUnitValue().multiply(BigDecimal.valueOf(kardexDetail.getQuantity())));
//            if (kardex.getId() == null) {
//                kardexDetail.setCummulativeQuantity(kardexDetail.getQuantity() * -1);
//                kardexDetail.setCummulativeTotalValue(kardexDetail.getTotalValue().multiply(BigDecimal.valueOf(-1)));
//            } else {
//                if (kardex.getQuantity() != null && kardex.getFund() != null) {
//                    kardexDetail.setCummulativeQuantity(kardex.getQuantity() - kardexDetail.getQuantity());
//                    kardexDetail.setCummulativeTotalValue(kardex.getFund().subtract(kardexDetail.getTotalValue()));
//                }
//            }
//            kardex.addKardexDetail(kardexDetail);
//            if (kardex.getCode() == null) {
//                kardex.setCode(settingHome.getValue("app.inventory.kardex.code.prefix", "TK-P-") + candidateDetail1.getProduct().getId());
//            }
//            kardex.setQuantity(kardexDetail.getCummulativeQuantity());
//            kardex.setFund(kardexDetail.getCummulativeTotalValue());
//            kardexService.save(kardex.getId(), kardex);
//        }
//    }
