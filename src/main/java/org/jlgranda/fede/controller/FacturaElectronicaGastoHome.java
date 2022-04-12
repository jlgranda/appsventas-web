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
package org.jlgranda.fede.controller;

import javax.ejb.EJB;
import com.jlgranda.fede.ejb.FacturaElectronicaService;
import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.fede.ejb.mail.reader.FacturaElectronicaMailReader;
import com.jlgranda.fede.ejb.mail.reader.FacturaReader;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Named;
import javax.mail.MessagingException;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.sri.jaxb.exception.FacturaXMLReadException;
import org.jlgranda.fede.util.FacturaUtil;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.FileUploadEvent;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.FacturaElectronicaDetailService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.RecordTemplateService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import com.jlgranda.fede.ejb.sales.KardexDetailService;
import com.jlgranda.fede.ejb.sales.KardexService;
import com.jlgranda.fede.ejb.sales.PaymentService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import com.jlgranda.fede.ejb.url.reader.FacturaElectronicaURLReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.model.Detailable;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.model.document.FacturaElectronicaDetail;
import org.jlgranda.fede.model.document.FacturaType;
import org.jlgranda.fede.model.sales.KardexDetail;
import org.jlgranda.fede.model.sales.Payment;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.sri.jaxb.factura.v110.Factura;
import org.jlgranda.fede.ui.model.LazyFacturaElectronicaDataModel;
import org.jlgranda.rules.RuleRunner;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.SourceType;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.Strings;
import org.kie.internal.builder.KnowledgeBuilderErrors;
import org.primefaces.PrimeFaces;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.file.UploadedFile;

/**
 * Controlador de aplicaciones de factura electrónica
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class FacturaElectronicaGastoHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(FacturaElectronicaCompraHome.class);

    private static final long serialVersionUID = -8639341517802126909L;

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;
    @Inject
    private SettingHome settingHome;
    @Inject
    private SubjectAdminHome subjectAdminHome; //para administrar proveedores en factura de compra

    @EJB
    private GroupService groupService;
    @EJB
    private FacturaElectronicaService facturaElectronicaService;
    @EJB
    private FacturaElectronicaMailReader facturaElectronicaMailReader;
    @EJB
    private PaymentService paymentService;
    @EJB
    private SubjectService subjectService;
    @EJB
    AccountCache accountCache;
    @EJB
    private AccountService accountService;
    @EJB
    private GeneralJournalService journalService;
    @EJB
    private RecordService recordService;
    @EJB
    private RecordDetailService recordDetailService;
    @EJB
    private ProductService productService;
    @EJB
    private FacturaElectronicaDetailService facturaElectronicaDetailService;
    @EJB
    private RecordTemplateService recordTemplateService;
    @EJB
    private GeneralJournalService generalJournalService;
    @EJB
    private KardexService kardexService;
    @EJB
    private KardexDetailService kardexDetailService;
    @EJB
    private ProductCache productCache;

    private String keys;
    private String url;
    private List<String> urls = new ArrayList<>();
    private List<UploadedFile> uploadedFiles = Collections.synchronizedList(new ArrayList<>());
    private LazyFacturaElectronicaDataModel lazyDataModel;
    private FacturaElectronica ultimafacturaElectronica;

    /**
     * Instancia de entidad <tt>FacturaElectronica</tt> para edición manual
     */
    private FacturaElectronica facturaElectronica;

    /**
     * Id de la actura electrónica en edición
     */
    private Long facturaElectronicaId;

    /**
     * Instancia <tt>Subject</tt> para registro de proveedor
     */
    private Subject supplier;

    /**
     * Bandera de activación de uso de proveedor por defecto.
     */
    private boolean useDefaultSupplier;

    /**
     * Lista de facturas electrónicas a usar el dashboard y/o widgets
     */
    private List<FacturaElectronica> sampleResultList = Collections.synchronizedList(new ArrayList<>());

    /**
     * El objeto Payment para editar
     */
    private Payment payment;

    /**
     * El objeto Journal para edición
     */
    private GeneralJournal journal;

    /**
     * El objeto Record para edición
     */
    private Record record;

    /**
     * RecordDetail para edición
     */
    private RecordDetail recordDetail;

    private RecordDetail recordDetailSelected;

    private Account accountPaymentSelected;

    private boolean payableTotal;

    //Variables de FacturaElectronicaDetail para edición
    private Product productSelected;
    private FacturaElectronicaDetail facturaElectronicaDetail;
    private List<Product> productsTypeProduct;
    private int days;
    private List<Product> products;
    private boolean activeTaxType;
    private boolean activePanelProduct;
    private boolean activeButtonProduct;
    private Product productNew;
    private Group groupSelected;
    private String productName;
    private BigDecimal amountPending;
    private boolean recordCompleto;

    public FacturaElectronicaGastoHome() {
    }

    @PostConstruct
    private void init() {
        int amount = 0;
        amount = 30;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }
        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * amount)));

        setFacturaElectronica(facturaElectronicaService.createInstance());
        getFacturaElectronica().setCode("000 000 000");
        setUseDefaultSupplier(false); //TODO desde configuraciones

        setPayment(paymentService.createInstance("EFECTIVO", null, null, null));

        setFacturaElectronicaDetail(facturaElectronicaDetailService.createInstance());
        if (this.organizationData.getOrganization() != null) {
            setProducts(productCache.filterByOrganization(this.organizationData.getOrganization()));
        }
        setActivePanelProduct(false);

//        //Establecer variable de sistema que habilita o no el registro contable
        getPayment().setAmount(BigDecimal.ZERO);
        getPayment().setDiscount(BigDecimal.ZERO);
        getPayment().setCash(BigDecimal.ZERO);
        getPayment().setChange(BigDecimal.ZERO);
        setAmountPending(BigDecimal.ZERO);
        setRecordCompleto(Boolean.TRUE);
        setRecord(recordService.createInstance());
        setRecordDetail(recordDetailService.createInstance());
        setOutcome("gastos");

        //Establecer variable de sistema que habilita o no el registro contable
        setAccountingEnabled(this.organizationData.getOrganization() != null ? this.organizationData.getOrganization().isAccountingEnabled() : false);

    }

    public List<UploadedFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public LazyFacturaElectronicaDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyFacturaElectronicaDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public List<FacturaElectronica> getSampleResultList() {
        int limit = Integer.parseInt(settingHome.getValue("fede.dashboard.timeline.length", "5"));
        if (sampleResultList.isEmpty()) {
            sampleResultList = facturaElectronicaService.findByNamedQueryWithLimit("FacturaElectronica.findLastsByOwner", limit, subject);
        }
        return sampleResultList;
    }

    public List<FacturaElectronica> getResultList() {
        return facturaElectronicaService.findByNamedQueryWithLimit("FacturaElectronica.findByOwnerAndEmisionAndEmissionTypeAndOrg", Integer.MAX_VALUE, subject, getStart(), getEnd(), EmissionType.PURCHASE_CASH, true, this.organizationData.getOrganization());
    }

    public void setSampleResultList(List<FacturaElectronica> sampleResultList) {
        this.sampleResultList = sampleResultList;
    }

    public FacturaElectronica getUltimaFacturaElectronica() {
        if (ultimafacturaElectronica == null) {
            List<FacturaElectronica> obs = getSampleResultList();
            ultimafacturaElectronica = obs.isEmpty() ? new FacturaElectronica() : (FacturaElectronica) obs.get(0);
        }

        return ultimafacturaElectronica;
    }

    public void setUltimafacturaElectronica(FacturaElectronica ultimafacturaElectronica) {
        this.ultimafacturaElectronica = ultimafacturaElectronica;
    }

    public FacturaElectronica getFacturaElectronica() {
        if (this.facturaElectronicaId != null && !this.facturaElectronica.isPersistent()) {
            this.facturaElectronica = facturaElectronicaService.find(facturaElectronicaId);
            this.setSupplier(this.facturaElectronica.getAuthor()); //El proveedor
            if (this.facturaElectronica.getRecordId() != null) { //La información del record
                setRecord(recordService.find(this.facturaElectronica.getRecordId()));
                if (getRecord().isPersistent()) {
                    if (!getRecord().getRecordDetails().isEmpty()) {
                        getRecord().setRecordDetails(getRecord().getRecordDetails().stream()
                                .filter(val -> RecordDetail.RecordTDetailType.DEBE.equals(val.getRecordDetailType())).collect(Collectors.toList()));
                    }
                }
            }
            montoPorPagar();
        }
        return facturaElectronica;
    }

    public void setFacturaElectronica(FacturaElectronica facturaElectronica) {
        this.facturaElectronica = facturaElectronica;
    }

    public Long getFacturaElectronicaId() {
        return facturaElectronicaId;
    }

    public void setFacturaElectronicaId(Long facturaElectronicaId) {
        this.facturaElectronicaId = facturaElectronicaId;
    }

    public Subject getSupplier() {
        return supplier;
    }

    public void setSupplier(Subject supplier) {
        this.supplier = supplier;
    }

    public boolean isUseDefaultSupplier() {
        return useDefaultSupplier;
    }

    public void setUseDefaultSupplier(boolean useDefaultSupplier) {
        this.useDefaultSupplier = useDefaultSupplier;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public void addURL() {
        if (getUrl().isEmpty()) {
            return;
        }
        if (Strings.isUrl(getUrl()) && (getUrl().endsWith(".zip") || getUrl().endsWith(".xml"))) {
            this.urls.add(getUrl());
        } else {
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("add.url.invalid"));
        }
    }

    public void removeURL(String url) {
        this.urls.remove(url);
    }

    /**
     * Obtener todas las facturas disponibles en el sistema para el usuario
     * actual dados los ids de la instancia actual
     * <tt>FacturaElectronicaGastoHome</tt>
     * Se usa para mostrar los RIDE
     *
     * @return lista de facturas electrónicas
     */
    public List<FacturaElectronica> listarFacturasElectronicasPorIds() {
        if (getKeys() == null || getKeys().isEmpty()) {
            return new ArrayList<>();
        }

        return facturaElectronicaService.findByNamedQuery("BussinesEntity.findByIds", buildListIds());
    }

    private List<Long> buildListIds() {
        List<Long> ids = new ArrayList<>();

        if (Strings.isNullOrEmpty(getKeys())) {
            return ids;
        }

        for (String s : getKeys().split(KEY_SEPARATOR)) {
            ids.add(Long.valueOf(s.trim()));
        }

        return ids;
    }

    public boolean isPayableTotal() {
        return payableTotal;
    }

    public void setPayableTotal(boolean payableTotal) {
        this.payableTotal = payableTotal;
    }

    public Product getProductSelected() {
        return productSelected;
    }

    public void setProductSelected(Product productSelected) {
        this.productSelected = productSelected;
    }

    public FacturaElectronicaDetail getFacturaElectronicaDetail() {
        return facturaElectronicaDetail;
    }

    public void setFacturaElectronicaDetail(FacturaElectronicaDetail facturaElectronicaDetail) {
        this.facturaElectronicaDetail = facturaElectronicaDetail;
    }

    public List<Product> getProductsTypeProduct() {
        return productsTypeProduct;
    }

    public void setProductsTypeProduct(List<Product> productsTypeProduct) {
        this.productsTypeProduct = productsTypeProduct;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public boolean isActiveTaxType() {
        return activeTaxType;
    }

    public void setActiveTaxType(boolean activeTaxType) {
        this.activeTaxType = activeTaxType;
    }

    public boolean isActivePanelProduct() {
        return activePanelProduct;
    }

    public void setActivePanelProduct(boolean activePanelProduct) {
        this.activePanelProduct = activePanelProduct;
    }

    public boolean isActiveButtonProduct() {
        return activeButtonProduct;
    }

    public void setActiveButtonProduct(boolean activeButtonProduct) {
        this.activeButtonProduct = activeButtonProduct;
    }

    public Product getProductNew() {
        return productNew;
    }

    public void setProductNew(Product productNew) {
        this.productNew = productNew;
    }

    public Group getGroupSelected() {
        return groupSelected;
    }

    public void setGroupSelected(Group groupSelected) {
        this.groupSelected = groupSelected;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getAmountPending() {
        return amountPending;
    }

    public void setAmountPending(BigDecimal amountPending) {
        this.amountPending = amountPending;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public RecordDetail getRecordDetail() {
        return recordDetail;
    }

    public void setRecordDetail(RecordDetail recordDetail) {
        this.recordDetail = recordDetail;
    }

    public RecordDetail getRecordDetailSelected() {
        return recordDetailSelected;
    }

    public void setRecordDetailSelected(RecordDetail recordDetailSelected) {
        this.recordDetailSelected = recordDetailSelected;
    }

    public Account getAccountPaymentSelected() {
        return accountPaymentSelected;
    }

    public void setAccountPaymentSelected(Account accountPaymentSelected) {
        this.accountPaymentSelected = accountPaymentSelected;
    }

    public boolean isRecordCompleto() {
        return recordCompleto;
    }

    public void setRecordCompleto(boolean recordCompleto) {
        this.recordCompleto = recordCompleto;
    }

    private Group findGroup(String key) {
        for (Group g : getGroups()) {
            if (key.equalsIgnoreCase(g.getCode())) {
                return g;
            }
        }
        return new Group("null", "null");
    }

    /**
     * Retorna los grupos para este controlador
     *
     * @return
     */
    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "fede", "fede"), Group.Type.LABEL));
        }

        return this.groups;
    }

    private Subject getDefaultSupplier() {
        return subjectService.findUniqueByNamedQuery("Subject.findUserByLogin", "proveedorsinfactura");
    }

    public SubjectAdminHome getSubjectAdminHome() {
        return subjectAdminHome;
    }

    public void setSubjectAdminHome(SubjectAdminHome subjectAdminHome) {
        this.subjectAdminHome = subjectAdminHome;
    }

    /**
     * TODO !IMPLEMENTACION TEMPORAL
     *
     * @param tag
     * @return
     */
    public BigDecimal calcularImporteTotal(String tag) {
        BigDecimal total = new BigDecimal(0);
        for (Iterator<Object> it = facturaElectronicaService.findByNamedQueryWithLimit("findBussinesEntityByTagAndOwnerAndEmision", 0, tag, subject, getStart(), getEnd()).iterator(); it.hasNext();) {
            FacturaElectronica fe = (FacturaElectronica) it.next();
            total = total.add(fe.getImporteTotal());
        }
        return total;
    }

    public BigDecimal countRowsByTag(String tag) {
        BigDecimal total = BigDecimal.ZERO;
        if ("all".equalsIgnoreCase(tag)) {
            total = new BigDecimal(facturaElectronicaService.count());
        } else if ("own".equalsIgnoreCase(tag)) {
            total = new BigDecimal(facturaElectronicaService.count("FacturaElectronica.countBussinesEntityByOwner", subject));
        } else if ("org".equalsIgnoreCase(tag)) {
            total = new BigDecimal(facturaElectronicaService.count("FacturaElectronica.countBussinesEntityByOrg", this.organizationData.getOrganization()));
        } else {
            total = new BigDecimal(facturaElectronicaService.count("FacturaElectronica.countBussinesEntityByTagAndOwner", tag, subject));
        }
        return total;
    }

    public boolean mostrarFormularioCargaFacturaElectronica(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "550");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "480");
        super.openDialog(SettingNames.POPUP_SUBIR_FACTURA_ELECTRONICA, width, height, true, params);
        return true;
    }

    public boolean mostrarFormularioDescargaFacturaElectronica(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        super.openDialog(SettingNames.POPUP_DESCARGAR_FACTURA_ELECTRONICA, width, height, true, params);
        return true;
    }

    public boolean mostrarFormularioCargaFacturaElectronica() {
        return mostrarFormularioCargaFacturaElectronica(null);
    }

    public boolean mostrarFormularioDescargaFacturaElectronica() {
        return mostrarFormularioDescargaFacturaElectronica(null);
    }

    public boolean mostrarFormularioActualizacionFacturaElectronica(String origen) {
        boolean flag = false;
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add(keys);

        params.put("key", values);

        if ("file".equalsIgnoreCase(origen)) {
            flag = mostrarFormularioCargaFacturaElectronica(params);
        } else {
            flag = mostrarFormularioDescargaFacturaElectronica(params);
        }
        return flag;
    }

    public boolean mostrarFormularioNuevaEtiqueta() {
        String width = settingHome.getValue(SettingNames.POPUP_SMALL_WIDTH, "400");
        String height = settingHome.getValue(SettingNames.POPUP_SMALL_HEIGHT, "240");
        super.openDialog(SettingNames.POPUP_NUEVA_ETIQUETA, width, height, true);
        return true;
    }

    public List<FacturaElectronica> importarDesdeInbox() {
        List<FacturaElectronica> result = new ArrayList<>();

        if (subject == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.signin.login.user.null"));
            return result;
        }
        try {
            for (FacturaReader fr : facturaElectronicaMailReader.read(subject, "inbox")) {
                try {
                    result.add(procesarFactura(fr, SourceType.EMAIL));
                } catch (FacturaXMLReadException ex) {
                    addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("xml.read.error.detail"));
                    java.util.logging.Logger.getLogger(FacturaElectronicaGastoHome.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Se agregaron " + result.size() + " facturas a fede desde el correo!");

        } catch (MessagingException | IOException ex) {
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("import.email.error"));
            java.util.logging.Logger.getLogger(FacturaElectronicaGastoHome.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public void handleFileUpload(FileUploadEvent event) {
        procesarUploadFile(event.getFile());
    }

    public void procesarUploadFile(UploadedFile file) {

        if (file == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.fedecard.file.null"));
            return;
        }

        if (subject == null) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.signin.login.user.null"));
            return;
        }
        String xml = null;
        try {
            if (file.getFileName().endsWith(".xml")) {
                byte[] content = IOUtils.toByteArray(file.getInputStream());
                xml = new String(content);
                procesarFactura(FacturaUtil.read(xml), xml, file.getFileName(), SourceType.FILE);
                this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Su factura electrónica " + file.getFileName() + " ahora empieza a generar valor para ud!");
                IOUtils.closeQuietly(file.getInputStream());
            } else if (file.getFileName().endsWith(".zip")) {
                ZipInputStream zis = new ZipInputStream(file.getInputStream());
                try {
                    ZipEntry entry = null;
                    ByteArrayOutputStream fout = null;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().endsWith(".xml")) {
                            //logger.debug("Unzipping {}", entry.getFilename());
                            fout = new ByteArrayOutputStream();
                            for (int c = zis.read(); c != -1; c = zis.read()) {
                                fout.write(c);
                            }

                            xml = new String(fout.toByteArray(), Charset.defaultCharset());
                            procesarFactura(FacturaUtil.read(xml), xml, file.getFileName(), SourceType.FILE);
                            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Su factura electrónica " + entry.getName() + " ahora empieza a generar valor para ud!");
                            fout.close();
                        }
                        zis.closeEntry();
                    }
                    zis.close();

                } finally {
                    IOUtils.closeQuietly(file.getInputStream());
                    IOUtils.closeQuietly(zis);
                }
            }

        } catch (IOException | FacturaXMLReadException e) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), e.getMessage());
        }
    }

    /**
     * Cargar Facturas electrónicas a partir los urls indicados por el usuario
     *
     * @return lista de instancias FacturaElectronica
     */
    public List<FacturaElectronica> procesarURLs() {
        List<FacturaElectronica> result = new ArrayList<>();
        try {

            for (FacturaReader fr : FacturaElectronicaURLReader.getFacturasElectronicas(getUrls())) {
                result.add(procesarFactura(fr, SourceType.URL));
            }
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Se agregaron " + result.size() + " facturas a fede desde los URLs dados!");
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(FacturaElectronicaGastoHome.class.getName()).log(Level.SEVERE, null, ex);
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("xml.read.error.detail"));
        }
        closeDialog(null);
        return result;
    }

    public void addURLAndProcesarURLs() {
        addURL();
        procesarURLs();
        closeDialog(null);
    }

    private FacturaElectronica procesarFactura(Factura factura, String xml, String filename, SourceType sourceType) throws FacturaXMLReadException {
        FacturaElectronica instancia = null;

        if (buildListIds().isEmpty()) {
            instancia = procesarAgregarFactura(factura, xml, filename, sourceType);
        } else {
            instancia = procesarActualizarFactura(factura, xml, filename, sourceType);
        }
        return instancia;
    }

    private FacturaElectronica procesarAgregarFactura(Factura factura, String xml, String filename, SourceType sourceType) throws FacturaXMLReadException {
        FacturaElectronica instancia = null;

        if (factura == null) {
            addErrorMessage(I18nUtil.getMessages("action.fail"), "No fue posible leer el contenido XML");
            throw new FacturaXMLReadException("No fue posible leer el contenido XML!");
        }

        //CodeType codeType = CodeType.encode(factura.getInfoFactura().getTipoIdentificacionComprador());
        //logger.info("IdentificacionComprador {}, CodeType {}", factura.getInfoFactura().getIdentificacionComprador(), codeType);
        if (!(factura.getInfoFactura().getIdentificacionComprador().startsWith(subject.getCode())
                || subject.getCode().startsWith(factura.getInfoFactura().getIdentificacionComprador()))) {
            addErrorMessage(I18nUtil.getMessages("xml.read.forbidden"), I18nUtil.getMessages("xml.read.forbidden.detail"));
            throw new FacturaXMLReadException(I18nUtil.getMessages("xml.read.forbidden.detail"));
        }

        //actualizarDatosDesdeFactura(subject, factura);
        StringBuilder codigo = new StringBuilder(factura.getInfoTributaria().getEstab());
        codigo.append("-");
        codigo.append(factura.getInfoTributaria().getPtoEmi());
        codigo.append("-");
        codigo.append(factura.getInfoTributaria().getSecuencial());

        if ((instancia = facturaElectronicaService.findUniqueByNamedQuery("BussinesEntity.findByCode", codigo.toString())) == null) {

            instancia = facturaElectronicaService.createInstance();
            instancia.setCode(codigo.toString());
            instancia.setCodeType(CodeType.NUMERO_FACTURA);
            instancia.setFilename(filename);
            instancia.setContenido(xml);
            instancia.setFechaEmision(Dates.toDate(factura.getInfoFactura().getFechaEmision()));
            instancia.setTotalSinImpuestos(factura.getInfoFactura().getTotalSinImpuestos());
            instancia.setTotalDescuento(factura.getInfoFactura().getTotalDescuento());
            instancia.setImporteTotal(factura.getInfoFactura().getImporteTotal());
            instancia.setMoneda(factura.getInfoFactura().getMoneda());

            instancia.setClaveAcceso(factura.getInfoTributaria().getClaveAcceso());
            String tag = settingHome.getValue(SettingNames.TAG_FECHA_AUTORIZACION, "<fechaAutorizacion></fechaAutorizacion>");
            instancia.setFechaAutorizacion(Dates.toDate(FacturaUtil.read(xml, tag)));
            instancia.setNumeroAutorizacion(FacturaUtil.read(xml, tag));

            instancia.setSourceType(sourceType); //El tipo de importación realizado
            instancia.setEmissionType(EmissionType.PURCHASE_CASH);

            Subject author = null;
            if ((author = subjectService.findUniqueByNamedQuery("BussinesEntity.findByCodeAndCodeType", factura.getInfoTributaria().getRuc(), CodeType.RUC)) == null) {
                author = subjectService.createInstance();
                author.setCode(factura.getInfoTributaria().getRuc());
                author.setName(factura.getInfoTributaria().getRazonSocial());
                author.setFirstname(factura.getInfoTributaria().getRazonSocial());
                author.setInitials((factura.getInfoTributaria().getNombreComercial() != null && !factura.getInfoTributaria().getNombreComercial().isEmpty()) ? factura.getInfoTributaria().getNombreComercial() : factura.getInfoTributaria().getRazonSocial());
                //Todo guardar la dirección como html o xml para uso posterior
                author.setDescription(factura.getInfoTributaria().getDirMatriz());
                author.setSubjectType(Subject.Type.PRIVATE);
                author.setCodeType(CodeType.RUC);
                author.setRuc(factura.getInfoTributaria().getRuc());
                author.setNumeroContribuyenteEspecial(factura.getInfoFactura().getContribuyenteEspecial());
                author.setEmail(author.getCode() + "@dummy.com");
                author.setUsername(author.getEmail());
                author.setPassword((new org.apache.commons.codec.digest.Crypt().crypt("dummy")));
                author.setActive(Boolean.FALSE);

                author = subjectService.save(author);
                logger.info("Added new Subject as author. RUC {}, CodeType {}", factura.getInfoTributaria().getRuc(), CodeType.RUC);

            }

            instancia.setAuthor(author);
            instancia.setOwner(subject);

            instancia = facturaElectronicaService.save(instancia);

        } else {//Actualizar desde factura electrónica
            this.addWarningMessage(I18nUtil.getMessages("action.warning"), "El archivo " + filename + " contiene una factura que ya existe en fede. ID: " + codigo + ".");
        }

        return instancia;
    }

    private FacturaElectronica procesarActualizarFactura(Factura factura, String xml, String filename, SourceType sourceType) throws FacturaXMLReadException {
        FacturaElectronica instancia = null;

        if (factura == null) {
            addErrorMessage(I18nUtil.getMessages("action.fail"), "No fue posible leer el contenido XML");
            throw new FacturaXMLReadException("No fue posible leer el contenido XML!");
        }

        if (!(factura.getInfoFactura().getIdentificacionComprador().startsWith(subject.getCode())
                || subject.getCode().startsWith(factura.getInfoFactura().getIdentificacionComprador()))) {
            addErrorMessage(I18nUtil.getMessages("xml.read.forbidden"), I18nUtil.getMessages("xml.read.forbidden.detail"));
            throw new FacturaXMLReadException(I18nUtil.getMessages("xml.read.forbidden.detail"));
        }

        //actualizarDatosDesdeFactura(subject, factura);
        StringBuilder codigo = new StringBuilder(factura.getInfoTributaria().getEstab());
        codigo.append("-");
        codigo.append(factura.getInfoTributaria().getPtoEmi());
        codigo.append("-");
        codigo.append(factura.getInfoTributaria().getSecuencial());
        for (Long _facturaElectronicaId : buildListIds()) {
            if ((instancia = facturaElectronicaService.find(_facturaElectronicaId)) != null) { //Funciona en el caso que solo hay un id seleccionado

                instancia.setCode(codigo.toString());
                instancia.setCodeType(CodeType.NUMERO_FACTURA);
                instancia.setFilename(filename);
                instancia.setContenido(xml);
                instancia.setFechaEmision(Dates.toDate(factura.getInfoFactura().getFechaEmision()));
                instancia.setTotalSinImpuestos(factura.getInfoFactura().getTotalSinImpuestos());
                instancia.setTotalDescuento(factura.getInfoFactura().getTotalDescuento());
                instancia.setImporteTotal(factura.getInfoFactura().getImporteTotal());
                instancia.setMoneda(factura.getInfoFactura().getMoneda());

                instancia.setClaveAcceso(factura.getInfoTributaria().getClaveAcceso());
                String tag = settingHome.getValue(SettingNames.TAG_FECHA_AUTORIZACION, "<fechaAutorizacion></fechaAutorizacion>");
                instancia.setFechaAutorizacion(Dates.toDate(FacturaUtil.read(xml, tag)));
                instancia.setNumeroAutorizacion(FacturaUtil.read(xml, tag));

                instancia.setSourceType(sourceType); //El tipo de importación realizado
                instancia.setEmissionType(EmissionType.PURCHASE_CASH);

                Subject author = null;
                if ((author = subjectService.findUniqueByNamedQuery("BussinesEntity.findByCodeAndCodeType", factura.getInfoTributaria().getRuc(), CodeType.RUC)) == null) {
                    author = subjectService.createInstance();
                    author.setCode(factura.getInfoTributaria().getRuc());
                    author.setName(factura.getInfoTributaria().getRazonSocial());
                    author.setFirstname(factura.getInfoTributaria().getRazonSocial());
                    author.setInitials((factura.getInfoTributaria().getNombreComercial() != null && !factura.getInfoTributaria().getNombreComercial().isEmpty()) ? factura.getInfoTributaria().getNombreComercial() : factura.getInfoTributaria().getRazonSocial());
                    //Todo guardar la dirección como html o xml para uso posterior
                    author.setDescription(factura.getInfoTributaria().getDirMatriz());
                    author.setSubjectType(Subject.Type.PRIVATE);
                    author.setCodeType(CodeType.RUC);
                    author.setRuc(factura.getInfoTributaria().getRuc());
                    author.setNumeroContribuyenteEspecial(factura.getInfoFactura().getContribuyenteEspecial());
                    author.setEmail(author.getCode() + "@dummy.com");
                    author.setUsername(author.getEmail());
                    author.setPassword((new org.apache.commons.codec.digest.Crypt().crypt("dummy")));
                    author.setActive(Boolean.FALSE);

                    author = subjectService.save(author);
                    logger.info("Added new Subject as author. RUC {}, CodeType {}", factura.getInfoTributaria().getRuc(), CodeType.RUC);

                }

                instancia.setAuthor(author);
                instancia.setOwner(subject);

                instancia = facturaElectronicaService.save(_facturaElectronicaId, instancia);

            } else {//Actualizar desde factura electrónica
                return procesarAgregarFactura(factura, xml, filename, sourceType);
            }
        }
        return instancia;
    }

    private FacturaElectronica procesarFactura(FacturaReader fr, SourceType sourceType) throws FacturaXMLReadException {
        return procesarFactura(fr.getFactura(), fr.getXml(), fr.getFileName(), sourceType);
    }

    @Override
    public void handleReturn(SelectEvent event) {
        setSupplier((Subject) event.getObject());
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getSelectedKeys() {
        String _keys = "";
        if (getSelectedBussinesEntities() != null && !getSelectedBussinesEntities().isEmpty()) {
            _keys = Lists.toString(getSelectedBussinesEntities());
        }
        return _keys;
    }

    public Long getFacturaElectronicaIdFromSelectedKeys() {
        Long id = 0L;
        if (getSelectedBussinesEntities() != null && !getSelectedBussinesEntities().isEmpty()) {
            id = getSelectedBussinesEntities().get(0).getId(); //Tomar el primero de los seleccionados
        }
        return id;
    }

    public List<String> getGroupNames() {
        List<String> names = new ArrayList<>();

        for (Group g : getGroups()) {
            names.add(g.getName());
        }

        return names;
    }

    public List<Group> getGroupsByCodes() {
        if (getTags() == null) {
            return getGroups();
        }

        if (getTags().isEmpty()) {
            return getGroups();
        }

        return groupService.findByNamedQuery("BussinesEntity.findByCodesAndOwner", getTags(), subject);
    }

    /**
     * Realizar una nueva busqueda
     */
    public void clear() {
        this.lazyDataModel = null;
    }

    /**
     * Filtro para llenar el Lazy Datamodel
     */
    public void filter() {
        //Todos los documentos, independientemente del cajero
        filter(null, Dates.minimumDate(getStart()), Dates.maximumDate(getEnd()), getKeyword(), getTags());
    }

    public void filter(Subject _subject, Date _start, Date _end, String _keyword, String _tags) {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyFacturaElectronicaDataModel(facturaElectronicaService);
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

        lazyDataModel.setFacturaType(FacturaType.GASTO);

        lazyDataModel.setOrganization(this.organizationData.getOrganization());

        if (!Strings.isNullOrEmpty(_keyword) && _keyword.startsWith("table:")) {
            String parts[] = getKeyword().split(":");
            if (parts.length > 1) {
                lazyDataModel.setCode(parts[1]);
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

    public void applySelectedGroups() {
//        String status = "";
//        Group group = null;
//        Set<String> addedGroups = new LinkedHashSet<>();
//        Set<String> removedGroups = new LinkedHashSet<>();
//        for (BussinesEntity fe : getSelectedBussinesEntities()) {
//            for (String key : selectedTriStateGroups.keySet()) {
//                group = findGroup(key);
//                status = selectedTriStateGroups.get(key);
//                if ("0".equalsIgnoreCase(status)) {
//                    if (fe.containsGroup(key)) {
//                        fe.remove(group);
//                        removedGroups.add(group.getName());
//                    }
//                } else if ("1".equalsIgnoreCase(status)) {
//                    if (!fe.containsGroup(key)) {
//                        fe.add(group);
//                        addedGroups.add(group.getName());
//                    }
//                } else if ("2".equalsIgnoreCase(status)) {
//                    if (!fe.containsGroup(key)) {
//                        fe.add(group);
//                        addedGroups.add(group.getName());
//                    }
//                }
//            }
//
//            facturaElectronicaService.save(fe.getId(), (FacturaElectronica) fe);
//        }
//
//        if (!addedGroups.isEmpty()) {
//            this.addSuccessMessage("Agregar grupos", "Algunas facturas se agregaron a " + Lists.toString(addedGroups));
//        }
//        if (!removedGroups.isEmpty()) {
//            this.addSuccessMessage("Remover grupos", "Algunas facturas se removieron de " + Lists.toString(removedGroups));
//        }

    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                FacturaElectronica fe = (FacturaElectronica) event.getObject();
                redirectTo("/pages/fede/compras/gasto.jsf?facturaElectronicaId=" + fe.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());

        FacesContext.getCurrentInstance().addMessage(null, msg);
        this.selectedBussinesEntities.remove((FacturaElectronica) event.getObject());
        logger.info(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
    }

    /**
     * Calcular el valor total de un conjunto de facturas.
     */
    public BigDecimal calculeTotal(List<FacturaElectronica> list) {
        BigDecimal total = new BigDecimal(0);
        for (FacturaElectronica i : list) {
            total = total.add(i.getImporteTotal());
        }
        return total;
    }

    /**
     * Calcular valores del monto abonado del pago de la factura.
     */
    public void calcularMontoAbonado() {
        if (this.amountPending.compareTo(getPayment().getAmount()) == 0 || this.amountPending.compareTo(getPayment().getAmount()) == 1) {
            getPayment().setCash(getPayment().getAmount().subtract(getPayment().getDiscount()));
            getPayment().setChange(getPayment().getCash().subtract(getPayment().getAmount()));
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.payment.cash.invalid", " " + this.amountPending));
            getPayment().setCash(BigDecimal.ZERO);
            getPayment().setChange(BigDecimal.ZERO);
        }
    }

    /**
     * Calcular el vuelto del pago de la factura.
     */
    public void calcularVueltoAbonado() {
        if (getPayment().getCash() != null && getPayment().getAmount() != null && getPayment().getDiscount() != null) {
            getPayment().setChange(getPayment().getCash().subtract(getPayment().getAmount().subtract(getPayment().getDiscount())));
        } else if (getPayment().getCash() != null && getPayment().getAmount() != null) {
            getPayment().setChange(getPayment().getCash().subtract(getPayment().getAmount()));
        }
    }

    /**
     * Calcular valores del monto que aún hay que abonar al pago de la factura.
     */
    public void montoPorPagar() {
        BigDecimal total = BigDecimal.ZERO;
        this.amountPending = BigDecimal.ZERO;
        if (this.facturaElectronica.getId() != null) {
            facturaElectronica.setPayments(paymentService.findByNamedQuery("Payment.findByFacturaElectronica", this.facturaElectronica));
        }
        for (Payment p : facturaElectronica.getPayments()) {
            total = total.add(p.getAmount());
        }
        this.amountPending = facturaElectronica.getImporteTotal().subtract(total);
        if (this.amountPending != null) {
            this.payableTotal = this.amountPending.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    public BigDecimal montoPorPagar(FacturaElectronica facturaElectronica) {
        BigDecimal total = BigDecimal.ZERO;
        if (EmissionType.PURCHASE_CREDIT.equals(facturaElectronica.getEmissionType())) {
            for (Payment p : facturaElectronica.getPayments()) {
                total = total.add(p.getAmount());
            }
        } else {
            if (facturaElectronica.getPayments().isEmpty()) {
                total = facturaElectronica.getImporteTotal();
            } else {
                total = facturaElectronica.getPayments().get(facturaElectronica.getPayments().size() - 1).getAmount();
            }
        }
        return facturaElectronica.getImporteTotal().subtract(total);
    }

    /**
     * Calcular valor de la factura.
     */
    public void calcularTotalFactura() {
        double importe = this.record.getRecordDetails().stream().map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
        this.facturaElectronica.setTotalSinImpuestos(BigDecimal.valueOf(importe));
        this.facturaElectronica.setImporteTotal(this.facturaElectronica.getTotalSinImpuestos());
        montoPorPagar();
    }

    public void calculateDateExpired() {
        if (this.facturaElectronica.getFechaEmision() != null && this.days >= 0) {
            this.facturaElectronica.setFechaVencimiento(Dates.addDays(this.facturaElectronica.getFechaEmision(), this.days));
        }
    }

    public boolean calculateDayOfExpiration(FacturaElectronica facturaElectronica) {
        if (facturaElectronica.getPayments().isEmpty()) {
            facturaElectronica.setUltimaFechaPago(facturaElectronica.getFechaEmision());
        } else {
            if (facturaElectronica.getPayments().get(facturaElectronica.getPayments().size() - 1).getDatePaymentCancel() != null) {
                facturaElectronica.setUltimaFechaPago(facturaElectronica.getPayments().get(facturaElectronica.getPayments().size() - 1).getDatePaymentCancel());
            } else {
                facturaElectronica.setUltimaFechaPago(facturaElectronica.getPayments().get(facturaElectronica.getPayments().size() - 1).getCreatedOn());
            }
        }
        if (facturaElectronica.getFechaVencimiento() != null && BigDecimal.ZERO.compareTo(montoPorPagar(facturaElectronica)) == -1) {
            if (Dates.isInRange(getEnd(), Dates.minimumDate(Dates.addDays(getEnd(), 5)), facturaElectronica.getFechaVencimiento())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String messageDatePaymentExpired(Date fechaVencimiento) {
        if (fechaVencimiento != null) {
            return I18nUtil.getMessages("app.fede.payments.purchase.credit.alert", " " + Dates.calculateNumberOfDaysBetween(getEnd(), fechaVencimiento));
        } else {
            return "";
        }
    }

    public void messageAlert() {
        if (this.facturaElectronicaId == null) {
            this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.purchase.start.message"));
        }
    }

    /**
     * Guardar los datos de la factura.
     */
    @javax.transaction.Transactional
    public void save() {
        this.facturaElectronica.setCodeType(CodeType.NUMERO_FACTURA);
        this.facturaElectronica.setFilename(null);
        this.facturaElectronica.setContenido(null);
        this.facturaElectronica.setMoneda("DOLAR");

        this.facturaElectronica.setClaveAcceso(null);
        this.facturaElectronica.setFechaAutorizacion(null);
        this.facturaElectronica.setNumeroAutorizacion(null);

        this.facturaElectronica.setSourceType(SourceType.MANUAL); //El tipo de importación realizado

        facturaElectronica.setAuthor(getSupplier());
        this.facturaElectronica.setOwner(subject);
        this.facturaElectronica.setOrganization(this.organizationData.getOrganization());

        //Almacenar el tipo de factura
        this.facturaElectronica.setFacturaType(FacturaType.GASTO);

        //Establecer un codigo por defecto
        if (Strings.isNullOrEmpty(facturaElectronica.getCode())) {
            facturaElectronica.setCode(UUID.randomUUID().toString());
        }

        if (EmissionType.PURCHASE_CASH.equals(this.facturaElectronica.getEmissionType())) {//Registrar el pago
            directPayment();//Realizar un pago directo
        } else if (EmissionType.PURCHASE_CREDIT.equals(this.facturaElectronica.getEmissionType())) {
            for (Payment pay : this.facturaElectronica.getPayments()) {
                if (EmissionType.PURCHASE_CASH.equals(pay.getFacturaElectronica().getEmissionType())) {
                    pay.setDeleted(true);
                }
            }
        }

        if (!facturaElectronica.isPersistent()) {
            facturaElectronicaService.save(this.facturaElectronica.getId(), this.facturaElectronica);
        }

        registerDetalleFacturaElectronicaInKardex(this.facturaElectronica.getFacturaElectronicaDetails()); //Procesa y guarda la factura electrónica en el medio persistente

        //Registrar asiento contable de la compra
        if (this.facturaElectronica.getId() != null) {
            setOutcome(registerRecordInJournal());
        } else {
            addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.purchase.accounting.fail"));
        }

        if (Strings.isNullOrEmpty(getOutcome())) {
            addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.purchase.accounting.fail"));
        } else {
            addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.inventory.purchase.accounting.success"));
        }

    }

    /**
     * Agrega o actualiza el pago de la factura.
     */
    public void addPayment() {
        if (getPayment().getAmount() != null && getPayment().getAmount().compareTo(BigDecimal.ZERO) == 1) {
            Payment p = paymentService.createInstance();
            p.setAmount(getPayment().getAmount());
            p.setDiscount(getPayment().getDiscount());
            p.setCash(getPayment().getCash());
            p.setChange(getPayment().getChange());
            p.setMethod(getPayment().getMethod());
            p.setDatePaymentCancel(Dates.now());
            this.getFacturaElectronica().addPayment(p);
            facturaElectronicaService.save(facturaElectronica.getId(), facturaElectronica);///guardar el pago
            setFacturaElectronica(facturaElectronicaService.find(facturaElectronica.getId()));

            if (!facturaElectronica.getPayments().isEmpty()) {
                setPayment(facturaElectronica.getPayments().get(facturaElectronica.getPayments().size() - 1));
            }

            //Registrar asiento contable del pago de la compra
            if (getPayment().getId() != null && getPayment().getMethod() != null) {
                setOutcome(registerRecordInJournalPaymentCredit());
            } else {
                addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.purchase.accounting.fail"));
            }

            if (Strings.isNullOrEmpty(getOutcome())) {
                addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.payments.accounting.fail"));
            } else {
                addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.payments.accounting.success"));
            }
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.payment.cash.paid.inlinehelp"));
        }
        setPayment(paymentService.createInstance("EFECTIVO", null, null, null));
        getPayment().setAmount(BigDecimal.ZERO);
        getPayment().setDiscount(BigDecimal.ZERO);
        getPayment().setCash(BigDecimal.ZERO);
        getPayment().setChange(BigDecimal.ZERO);
        if (!facturaElectronica.getPayments().isEmpty()) {
        }
        montoPorPagar();//Lo que resta por pagar, luego de registrar un pago
    }

    /**
     * Agrega o actualiza el pago de la factura.
     */
    public void directPayment() {//Pago cuando la factura es en efectivo
        if (facturaElectronica.getImporteTotal().compareTo(BigDecimal.ZERO) == 1) {
            List<Payment> listPayment = new ArrayList<>();
            Payment p = new Payment();
            if (facturaElectronica.getId() != null) {
                listPayment = paymentService.findByNamedQuery("Payment.findByFacturaElectronica", facturaElectronica);
            }
            if (listPayment.isEmpty()) {
                p = paymentService.createInstance();
            } else {
                p = listPayment.get(0);
            }
//            p.setAmount(facturaElectronica.getTotalSinImpuestos());
            p.setAmount(facturaElectronica.getImporteTotal());
            p.setDiscount(facturaElectronica.getTotalDescuento());
            p.setCash(facturaElectronica.getImporteTotal());
            p.setChange(BigDecimal.ZERO);
            p.setDatePaymentCancel(Dates.now());
            facturaElectronica.addPayment(p);
            setPayment(paymentService.createInstance("EFECTIVO", null, null, null));
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.payment.cash.less.zero"));
        }
    }

    public void onItemAccountSelect(SelectEvent<Account> event) {
        Account account = event.getObject();
        Optional<RecordDetail> find = this.record.getRecordDetails().stream().filter(x -> x.getAccount().equals(account)).findFirst();
        if (find.isPresent()) {
            this.setRecordDetail(find.get());
        }
    }

    public void recordDetailAdd(String campo) {
        if (campo.equalsIgnoreCase("save")) {
            this.recordDetail.setAccountName(this.recordDetail.getAccount().getName());
            this.recordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.DEBE);
            this.recordDetail.setOwner(this.subject);
            this.record.addRecordDetail(this.recordDetail);
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(this.recordDetail.getAccount().getName()));
            //Encerar el registro
            this.recordDetailSelected = null;
            this.calcularTotalFactura();
        }
        setRecordDetail(recordDetailService.createInstance());
    }

    public String registerRecordInJournal() {
        String outcome_ = ""; //Regresar a la lista.
        if (isAccountingEnabled()) {
            //Ejecutar las reglas de negocio para el registro del cierre de cada
            this.recordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.HABER);
            this.recordDetail.setAmount(this.facturaElectronica.getImporteTotal());

            if (EmissionType.PURCHASE_CASH.equals(facturaElectronica.getEmissionType())) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrogastosefectivo", "REGISTRO_GASTOS_EFECTIVO"));
                this.recordDetail.setAccountName("Caja dia");
                this.record.addRecordDetail(recordDetail);
                this.record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal facturado: %s \nConcepto: %s", (this.facturaElectronica.getCode() + "/" + this.facturaElectronica.getId()),
                        this.facturaElectronica.getAuthor().getFullName(), Strings.format(this.facturaElectronica.getImporteTotal().doubleValue(), "$ #0.##"), this.facturaElectronica.getDescription()));
            } else if (EmissionType.PURCHASE_CREDIT.equals(facturaElectronica.getEmissionType())) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrogastoscredito", "REGISTRO_GASTOS_CREDITO"));
                this.recordDetail.setAccountName("PROVEEDORES $CEDULA");
                if (this.facturaElectronica.getPayments().isEmpty()) {
                    record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal facturado a crédito: %s \nÚltimo pago registrado: ninguno \nConcepto: %s", (this.facturaElectronica.getCode() + "/" + this.facturaElectronica.getId()),
                            this.facturaElectronica.getAuthor().getFullName(), Strings.format(this.facturaElectronica.getImporteTotal().doubleValue(), "$ #0.##"), this.facturaElectronica.getDescription()));
                } else {
                    record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal de la factura: %s \nÚltimo pago registrado: %s \nConcepto: %s", (this.facturaElectronica.getCode() + "/" + this.facturaElectronica.getId()),
                            this.facturaElectronica.getAuthor().getFullName(), Strings.format(this.facturaElectronica.getImporteTotal().doubleValue(), "$ #0.##"), Strings.format(this.facturaElectronica.getPayments().get(this.facturaElectronica.getPayments().size() - 1).getAmount().doubleValue(), "$ #0.##"), this.facturaElectronica.getDescription()));
                }
                this.record.addRecordDetail(recordDetail);
            }

            this.recordDetail = recordDetailService.createInstance();
            //Cargar data del record
            this.record.setBussinesEntityType(this.facturaElectronica.getClass().getSimpleName());
            this.record.setBussinesEntityId(this.facturaElectronica.getId());
            this.record.setBussinesEntityHashCode(this.facturaElectronica.hashCode());
            this.record.setName(String.format("%s: %s[id=%d]", "REGISTRO_GASTOS_EFECTIVO", getClass().getSimpleName(), this.facturaElectronica.getId()));

            List<Record> records = new ArrayList<>();
            getReglas().forEach(regla -> {
                if (this.record != null) {
                    records.add(this.record);
                }
            });
            if (!records.isEmpty()) {
                //La regla compiló bien
                String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", "Libro diario");
                String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
                GeneralJournal generalJournal = generalJournalService.find(this.facturaElectronica.getFechaEmision(), this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);

                //El General Journal del día
                if (generalJournal != null) {
                    for (Record rcr : records) {

                        rcr.setCode(UUID.randomUUID().toString());

                        rcr.setOwner(this.subject);
                        rcr.setAuthor(this.subject);

                        rcr.setGeneralJournalId(generalJournal.getId());

                        //Corregir objetos cuenta en los detalles
                        rcr.getRecordDetails().forEach(rcrd -> {
                            rcrd.setLastUpdate(Dates.now());
                            if (rcrd.getAccount() == null) {
                                if (rcrd.getAccountName().contains("$CEDULA")) {
                                    Account cuentaPadreDetectada = accountCache.lookupByName(rcrd.getAccountName().substring(0, rcrd.getAccountName().length() - 8), this.organizationData.getOrganization());
                                    if (cuentaPadreDetectada != null && cuentaPadreDetectada.getId() != null) {
                                        String nombreCuentaHija = cuentaPadreDetectada.getName().concat(" ").concat(this.facturaElectronica.getAuthor().getFullName());
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
                            }
                        });

                        //Persistencia
                        if (Boolean.TRUE.equals(this.recordCompleto)) {
                            rcr = recordService.save(rcr);
                            if (rcr.getId() != null) {
                                //Anular registros anteriores
                                //recordService.deleteLastRecords(generalJournal.getId(), this.facturaElectronica.getClass().getSimpleName(), this.facturaElectronica.getId(), this.facturaElectronica.hashCode());
                                if (this.facturaElectronica.getRecordId() != null) {
                                    recordService.deleteRecord(this.facturaElectronica.getRecordId());
                                    this.facturaElectronica.setRecordId(null);
                                }
                                this.facturaElectronica.setRecordId(rcr.getId());
                                facturaElectronicaService.save(this.facturaElectronica.getId(), this.facturaElectronica); //Se guardan todos los cambios
                                if (facturaElectronica.getRecordId() != null) {
                                    outcome_ = "gastos"; //regresa al listado
                                } else {
                                    addWarningMessage(I18nUtil.getMessages("action.warning"), "El registro contable, no se asoció a la Factura.");
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
        return outcome_;
    }

    public String registerRecordInJournalPaymentCredit() {

        String outcome_ = ""; //Regresar a la lista.

        if (isAccountingEnabled()) {
            //Ejecutar las reglas de negocio para el registro del cierre de cada
            if (getPayment().getMethod().equals("EFECTIVO")) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrogastoscreditopagoefectivo", "REGISTRO_GASTOS_CREDITO_PAGO_EFECTIVO"));
            } else if (getPayment().getMethod().equals("TRANSFERENCIA")) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrogastoscreditopagotransferencia", "REGISTRO_GASTOS_CREDITO_PAGO_TRANSFERENCIA"));
            }

            List<Record> records = new ArrayList<>();
            getReglas().forEach(regla -> {
                Record r = aplicarReglaNegocioPayment(regla, getPayment());
                if (r != null) {
                    records.add(r);
                }
            });

            if (!records.isEmpty()) {
                //La regla compiló bien
                String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", "Libro diario");
                String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
                GeneralJournal generalJournal = generalJournalService.find(this.facturaElectronica.getFechaEmision(), this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);

                //El General Journal del día
                if (generalJournal != null) {
                    for (Record rcr : records) {

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
                                    String nombreCuentaHija = cuentaPadreDetectada.getName().concat(" ").concat(this.facturaElectronica.getAuthor().getFullName());
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
                        if (Boolean.TRUE.equals(this.recordCompleto)) {
                            rcr = recordService.save(rcr);

                            if (rcr.getId() != null) {
                                //Anular registros anteriores
                                //recordService.deleteLastRecords(generalJournal.getId(), getPayment().getClass().getSimpleName(), getPayment().getId(), getPayment().hashCode());
                                if (getPayment().getRecordId() != null) {
                                    recordService.deleteRecord(getPayment().getRecordId());
                                    getPayment().setRecordId(null);
                                }
                                getPayment().setRecordId(rcr.getId());
                                paymentService.save(getPayment().getId(), getPayment()); //Se guardan todos los cambios
                                if (getPayment().getRecordId() != null) {
                                    outcome_ = "gastos"; //regresa al listado
                                } else {
                                    addWarningMessage(I18nUtil.getMessages("action.warning"), "El registro contable, no se asoció al Pago.");
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
        return outcome_;
    }

    /**
     * Registra el detalle de la factura electrnica en el sistema de Kardex
     *
     * @param facturaElectronicaDetail
     */
    private void registerDetalleFacturaElectronicaInKardex(List<FacturaElectronicaDetail> datails) {
        kardexService.save(
                makeDetailableList(datails),
                settingHome.getValue("app.inventory.kardex.code.prefix", "TK-P-"),
                settingHome.getValue("app.inventory.kardex.code.prefix.production", "TK-R-"),
                subject, this.organizationData.getOrganization(),
                KardexDetail.OperationType.COMPRA);
    }

    private List<Detailable> makeDetailableList(List<FacturaElectronicaDetail> details) {
        List<Detailable> datailables = new ArrayList<>();
        details.forEach(d -> {
            datailables.add((Detailable) d);
        });

        return datailables;
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
        super.openDialog(SettingNames.POPUP_FORMULARIO_PROVEEDOR_GASTO, width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioProfile() {

        setUseDefaultSupplier(false); //Implica que se agregará un nuevo usuario
        setSupplier(subjectService.createInstance()); //Nueva instancia
        if (getSupplier() != null
                && getSupplier().isPersistent()
                && !"9999999999999".equals(getSupplier().getCode())) {
            super.setSessionParameter("SUPPLIER", getSupplier());
        }
        return mostrarFormularioProfile(null);
    }

    public void closeFormularioProfile(Object data) {
        removeSessionParameter("KEYWORD");
        removeSessionParameter("SUPPLIER");
        super.closeDialog(data);
    }

    public void loadSessionParameters() {

        if (existsSessionParameter("SUPPLIER")) {
            this.subjectAdminHome.setSubjectEdit((Subject) getSessionParameter("SUPPLIER"));
        } else if (existsSessionParameter("KEYWORD")) {
            Subject _subject = subjectService.createInstance();
            _subject.setCode((String) getSessionParameter("KEYWORD"));
            this.subjectAdminHome.setSubjectEdit(_subject);
        }
    }

    public void saveSupplier() {
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
            setSupplier(getSubjectAdminHome().getSubjectEdit());
            closeFormularioProfile(getSupplier());
        }

    }

    public void updateDefaultSupplier() {
        this.setSupplier(getDefaultSupplier());
    }

    /**
     * @param nombreRegla
     * @param fuenteDatos
     * @return
     */
    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        FacturaElectronica _instance = (FacturaElectronica) fuenteDatos;

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
                if (EmissionType.PURCHASE_CASH.equals(_instance.getEmissionType())) {
                    record.setDescription(String.format("Proveedor: %s \nTotal facturado: %s \nDetalle: %s", _instance.getAuthor().getFullName(), Strings.format(_instance.getImporteTotal().doubleValue(), "$ #0.##"), _instance.getSummary()));
                } else if (EmissionType.PURCHASE_CREDIT.equals(_instance.getEmissionType())) {
                    if (_instance.getPayments().isEmpty()) {
                        record.setDescription(String.format("Proveedor: %s \nTotal facturado a crédito: %s \nÚltimo pago registrado: ninguno \nDetalle: %s", _instance.getAuthor().getFullName(), Strings.format(_instance.getImporteTotal().doubleValue(), "$ #0.##"), _instance.getSummary()));
                    } else {
                        record.setDescription(String.format("Proveedor: %s \nTotal de la factura: %s \nÚltimo pago registrado: %s \nDetalle: %s", _instance.getAuthor().getFullName(), Strings.format(_instance.getImporteTotal().doubleValue(), "$ #0.##"), Strings.format(_instance.getPayments().get(_instance.getPayments().size() - 1).getAmount().doubleValue(), "$ #0.##"), _instance.getSummary()));
                    }
                }
            }
        }
        //El registro casí listo para agregar al journal
        return record;
    }

    /**
     * @param nombreRegla
     * @param fuenteDatos
     * @return
     */
    public Record aplicarReglaNegocioPayment(String nombreRegla, Object fuenteDatos) {
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
                if (_instance.getFacturaElectronica().getPayments().isEmpty() || _instance.getFacturaElectronica().getPayments().size() == 1) {
                    record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal facturado a crédito: %s \nPenúltimo pago registrado: ninguno \nMonto abonado: %s \nFecha de pago del abono: %s \nMétodo de pago: %s",
                            (_instance.getFacturaElectronica().getCode() + "/" + _instance.getFacturaElectronica().getId()), _instance.getFacturaElectronica().getAuthor().getFullName(), Strings.format(_instance.getFacturaElectronica().getImporteTotal().doubleValue(), "$ #0.##"),
                            Strings.format(_instance.getAmount().doubleValue(), "$ #0.##"), _instance.getDatePaymentCancel(), _instance.getMethod()));
                } else {
                    record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal facturado a crédito: %s \nPenúltimo pago registrado: %s \nMonto abonado: %s \nFecha de pago del abono: %s \nMétodo de pago: %s",
                            (_instance.getFacturaElectronica().getCode() + "/" + _instance.getFacturaElectronica().getId()), _instance.getFacturaElectronica().getAuthor().getFullName(), Strings.format(_instance.getFacturaElectronica().getImporteTotal().doubleValue(), "$ #0.##"),
                            Strings.format(_instance.getFacturaElectronica().getPayments().get(_instance.getFacturaElectronica().getPayments().size() - 2).getAmount().doubleValue(), "$ #0.##"), Strings.format(_instance.getAmount().doubleValue(), "$ #0.##"), _instance.getDatePaymentCancel(), _instance.getMethod()));
                }
            }
        }
        //El registro casí listo para agregar al journal
        return record;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
