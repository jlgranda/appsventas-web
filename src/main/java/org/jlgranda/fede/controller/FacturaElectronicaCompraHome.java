/*
 * Copyright (C) 2022 jlgranda
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
package org.jlgranda.fede.controller;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.FacturaElectronicaDetailService;
import com.jlgranda.fede.ejb.FacturaElectronicaService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.RecordTemplateService;
import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import com.jlgranda.fede.ejb.compras.ProveedorService;
import com.jlgranda.fede.ejb.mail.reader.FacturaElectronicaMailReader;
import com.jlgranda.fede.ejb.mail.reader.FacturaReader;
import com.jlgranda.fede.ejb.sales.KardexDetailService;
import com.jlgranda.fede.ejb.sales.KardexService;
import com.jlgranda.fede.ejb.sales.PaymentService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import com.jlgranda.fede.ejb.url.reader.FacturaElectronicaURLReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import org.apache.commons.io.IOUtils;
import static org.jlgranda.fede.controller.FedeController.KEY_SEPARATOR;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.model.Detailable;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jlgranda.fede.model.compras.Proveedor;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.model.document.FacturaElectronicaDetail;
import org.jlgranda.fede.model.document.FacturaType;
import org.jlgranda.fede.model.sales.KardexDetail;
import org.jlgranda.fede.model.sales.Payment;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.sri.jaxb.exception.FacturaXMLReadException;
import org.jlgranda.fede.sri.jaxb.factura.v110.Factura;
import org.jlgranda.fede.ui.model.LazyFacturaElectronicaDataModel;
import org.jlgranda.fede.util.FacturaUtil;
import org.jlgranda.rules.RuleRunner;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.SourceType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.Strings;
import org.kie.internal.builder.KnowledgeBuilderErrors;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Especilización de compras via factura electrónica
 *
 * @author jlgranda
 */
@ViewScoped
@Named
public class FacturaElectronicaCompraHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(FacturaElectronicaCompraHome.class);

    private static final long serialVersionUID = -8639341517802126909L;

    @Inject
    private Subject subject;

    @Inject
    private OrganizationData organizationData;

    @Inject
    private SettingHome settingHome;

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
    private ProveedorService proveedorService;

    @EJB
    private FacturaElectronicaDetailService facturaElectronicaDetailService;

    @EJB
    private RecordTemplateService recordTemplateService;

    @EJB
    private GeneralJournalService generalJournalService;

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
     * Instancia de entidad <tt>Factura</tt> para edición manual
     */
    private Factura factura;

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

    private Account accountPaymentSelected;

    private boolean payableTotal;

    //Variables de FacturaElectronicaDetail para edición
    private Product productSelected;
    private FacturaElectronicaDetail facturaElectronicaDetail;
    private List<Product> productsTypeProduct;
    private int days;
    private List<Product> products;

    @EJB
    private KardexService kardexService;

    @EJB
    private KardexDetailService kardexDetailService;

    private boolean activeTaxType;

    private boolean activePanelProduct;
    private boolean activeButtonProduct;

    private Product productNew;

    private Group groupSelected;

    @EJB
    private ProductCache productCache;

    private String productName;

    private BigDecimal amoutPending;

    private boolean recordCompleto;

    public FacturaElectronicaCompraHome() {
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
        getProductsByType();
        setActivePanelProduct(false);

//        //Establecer variable de sistema que habilita o no el registro contable
        getPayment().setAmount(BigDecimal.ZERO);
        getPayment().setDiscount(BigDecimal.ZERO);
        getPayment().setCash(BigDecimal.ZERO);
        getPayment().setChange(BigDecimal.ZERO);
        setAmoutPending(BigDecimal.ZERO);
        setRecordCompleto(Boolean.TRUE);
        setOutcome("compras");

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
            this.setSupplier(facturaElectronica.getAuthor()); //El proveedor
            montoPorPagar();
            calcularValoresFactura();
        }
        return facturaElectronica;
    }

    public void setFacturaElectronica(FacturaElectronica facturaElectronica) {
        this.facturaElectronica = facturaElectronica;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
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
     * <tt>FacturaElectronicaHome</tt>
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

    public BigDecimal getAmoutPending() {
        return amoutPending;
    }

    public void setAmoutPending(BigDecimal amoutPending) {
        this.amoutPending = amoutPending;
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
                    java.util.logging.Logger.getLogger(FacturaElectronicaHome.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), "Se agregaron " + result.size() + " facturas a fede desde el correo!");

        } catch (MessagingException | IOException ex) {
            addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("import.email.error"));
            java.util.logging.Logger.getLogger(FacturaElectronicaHome.class.getName()).log(Level.SEVERE, null, ex);
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
            java.util.logging.Logger.getLogger(FacturaElectronicaHome.class.getName()).log(Level.SEVERE, null, ex);
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

    /**
     * Guarda los datos
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
        this.facturaElectronica.setFacturaType(FacturaType.COMPRA);

        //Establecer un codigo por defecto
        if (Strings.isNullOrEmpty(this.facturaElectronica.getCode())) {
            this.facturaElectronica.setCode(UUID.randomUUID().toString());
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
//            facturaElectronicaService.save(facturaElectronica); //Se crea el registro
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
     * Filtro para llenar el Lazy Datamodel
     */
    public void filter() {
        //Todos los documentos, independientemente del cajero
        filter(null, Dates.minimumDate(getStart()), Dates.maximumDate(getEnd()), getKeyword(), getTags());
    }

    /**
     * Filta facturas del tipo COMPRA
     *
     * @param _subject
     * @param _start
     * @param _end
     * @param _keyword
     * @param _tags
     */
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

        lazyDataModel.setFacturaType(FacturaType.COMPRA);

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
                redirectTo("/pages/fede/compras/compra.jsf?facturaElectronicaId=" + fe.getId());
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
        if (this.amoutPending.compareTo(getPayment().getAmount()) == 0 || this.amoutPending.compareTo(getPayment().getAmount()) == 1) {
            getPayment().setCash(getPayment().getAmount().subtract(getPayment().getDiscount()));
            getPayment().setChange(getPayment().getCash().subtract(getPayment().getAmount()));
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.payment.cash.invalid", " " + this.amoutPending));
            getPayment().setCash(BigDecimal.ZERO);
            getPayment().setChange(BigDecimal.ZERO);
        }
    }

    public void calcularVueltoAbonado() {
        if (getPayment().getCash() != null && getPayment().getAmount() != null && getPayment().getDiscount() != null) {
            getPayment().setChange(getPayment().getCash().subtract(getPayment().getAmount().subtract(getPayment().getDiscount())));
        } else if (getPayment().getCash() != null && getPayment().getAmount() != null) {
            getPayment().setChange(getPayment().getCash().subtract(getPayment().getAmount()));
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
//            Collections.sort(facturaElectronica.getPayments(), Collections.reverseOrder((Payment pay, Payment other) -> pay.getLastUpdate().compareTo(other.getLastUpdate())));
//            facturaElectronica.getPayments().get(0).getAmount();
        }
        montoPorPagar();//Lo que resta por pagar, luego de registrar un pago
    }

    public void montoPorPagar() {
        BigDecimal total = BigDecimal.ZERO;
        this.amoutPending = BigDecimal.ZERO;
        if (this.facturaElectronica.getId() != null) {
            facturaElectronica.setPayments(paymentService.findByNamedQuery("Payment.findByFacturaElectronica", this.facturaElectronica));
        }
        for (Payment p : facturaElectronica.getPayments()) {
            total = total.add(p.getAmount());
        }
        this.amoutPending = facturaElectronica.getImporteTotal().subtract(total);
        if (this.amoutPending != null) {
            this.payableTotal = this.amoutPending.compareTo(BigDecimal.ZERO) == 0;
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
//            p.setAmount(facturaElectronica.getSubtotalIVA0().add(facturaElectronica.getSubtotalIVA12()));
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

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Inject
    private SubjectAdminHome subjectAdminHome; //para administrar proveedores en factura de compra

    public SubjectAdminHome getSubjectAdminHome() {
        return subjectAdminHome;
    }

    public void setSubjectAdminHome(SubjectAdminHome subjectAdminHome) {
        this.subjectAdminHome = subjectAdminHome;
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
        super.openDialog(SettingNames.POPUP_FORMULARIO_PROVEEDOR_COMPRA, width, height, left, top, true, params);
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

            //Crear como proveedor de la organización
            Proveedor proveedor = proveedorService.createInstance();
            proveedor.setOwner(getSupplier());
            proveedor.setOrganization(organizationData.getOrganization());

            proveedorService.save(proveedor); //persistir

            //Cerrar y regresar a pantalla de factura
            closeFormularioProfile(getSupplier());
        }

    }

    public void updateDefaultSupplier() {
//        this.facturaElectronica.setAuthor(getDefaultSupplier());
        this.setSupplier(getDefaultSupplier());
    }

    public void calcularTotalFactura() {
        this.facturaElectronica.setImporteTotal((this.facturaElectronica.getSubtotalIVA0().add(this.facturaElectronica.getSubtotalIVA12())).subtract(this.facturaElectronica.getTotalDescuento()));
    }

    public String registerRecordInJournal() {

        String outcome_ = ""; //Regresar a la lista.
        if (isAccountingEnabled()) {
            //Ejecutar las reglas de negocio para el registro del cierre de cada
            if (EmissionType.PURCHASE_CASH.equals(facturaElectronica.getEmissionType())) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrocomprasefectivo", "REGISTRO_COMPRAS_EFECTIVO"));
            } else if (EmissionType.PURCHASE_CREDIT.equals(facturaElectronica.getEmissionType())) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrocomprascredito", "REGISTRO_COMPRAS_CREDITO"));
            }

            List<Record> records = new ArrayList<>();
            getReglas().forEach(regla -> {
                Record r = aplicarReglaNegocio(regla, this.facturaElectronica);
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
                                //recordService.deleteLastRecords(generalJournal.getId(), this.facturaElectronica.getClass().getSimpleName(), this.facturaElectronica.getId(), this.facturaElectronica.hashCode());
                                if (this.facturaElectronica.getRecordId() != null) {
                                    recordService.deleteRecord(this.facturaElectronica.getRecordId());
                                    this.facturaElectronica.setRecordId(null);
                                }
                                this.facturaElectronica.setRecordId(rcr.getId());
                                facturaElectronicaService.save(this.facturaElectronica.getId(), this.facturaElectronica); //Se guardan todos los cambios
                                if (facturaElectronica.getRecordId() != null) {
                                    outcome_ = "compras"; //regresa al listado
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
            if ("EFECTIVO".equals(getPayment().getMethod())) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrocomprascreditopagoefectivo", "REGISTRO_COMPRAS_CREDITO_PAGO_EFECTIVO"));
            } else if ("TRANSFERENCIA".equals(getPayment().getMethod())) {
                setReglas(settingHome.getValue("app.fede.accounting.rule.registrocomprascreditopagotransferencia", "REGISTRO_COMPRAS_CREDITO_PAGO_TRANSFERENCIA"));
            }

            List<Record> records = new ArrayList<>();
            getReglas().forEach(regla -> {
                if (getPayment().getId() != null) {
                    Record r = aplicarReglaNegocioPayment(regla, getPayment());
                    if (r != null) {
                        records.add(r);
                    }
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
                                    outcome_ = "compras"; //regresa al listado
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

    public void getProductsByType() {
        this.productsTypeProduct = new ArrayList<>();
        this.productsTypeProduct = productService.findByOrganizationAndType(this.organizationData.getOrganization(), ProductType.PRODUCT);
    }

    public List<Product> completeProductType(String query) {
        String queryLowerCase = query.toLowerCase();
        return productsTypeProduct.stream().filter(t -> t.getName().toLowerCase().contains(queryLowerCase)).collect(Collectors.toList());
    }

    public List<Product> completeProduct(String query) {
        String queryLowerCase = query.toLowerCase();
        products = productService.findByOrganizationAndType(this.organizationData.getOrganization(), ProductType.PRODUCT);
        return products.stream().filter(t -> t.getName().toLowerCase().contains(queryLowerCase)).collect(Collectors.toList());
    }

    public boolean asignedPropiertiesProduct() {
        if (this.facturaElectronicaDetail.getProduct() != null) {
            this.facturaElectronicaDetail.setDescription(this.facturaElectronicaDetail.getProduct().getName());
            this.facturaElectronicaDetail.setUnitValue(this.facturaElectronicaDetail.getProduct().getPriceCost());
            if (this.facturaElectronicaDetail.getUnitValue() == null) {
                this.facturaElectronicaDetail.setUnitValue(BigDecimal.ZERO);
            }
            return validatedFacturaElectronicaDetail();
        } else {
            return true;
        }
    }

    public boolean validatedFacturaElectronicaDetail() {
        if (this.facturaElectronicaDetail.getProduct() != null) {
            if (this.facturaElectronicaDetail.getProduct().getId() != null) {
                return !(this.facturaElectronicaDetail.getUnitValue() != null && this.facturaElectronicaDetail.getAmount() != null);
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public void addFacturaElectronicaDetail() {
        if (this.activeTaxType == true) {
            this.facturaElectronicaDetail.setTaxValue(this.facturaElectronicaDetail.getUnitValue().multiply(BigDecimal.valueOf(0.12)));
        }
        this.facturaElectronicaDetail.setTotalValue(this.facturaElectronicaDetail.getUnitValue().multiply(this.facturaElectronicaDetail.getAmount()));
        this.facturaElectronica.addFacturaElectronicaDetail(this.facturaElectronicaDetail);
        calcularValoresFactura();
        this.facturaElectronicaDetail = facturaElectronicaDetailService.createInstance(); //Recargar para la nueva instancia
        setActiveTaxType(false);
    }

    public void calcularValoresFactura() {
        this.facturaElectronica.setSubtotalIVA0(BigDecimal.ZERO);
        this.facturaElectronica.setSubtotalIVA12(BigDecimal.ZERO);
        this.facturaElectronica.setTotalIVA0(BigDecimal.ZERO);
        this.facturaElectronica.setTotalIVA12(BigDecimal.ZERO);
        this.facturaElectronica.setTotalSinImpuestos(BigDecimal.ZERO);
        if (!this.facturaElectronica.getFacturaElectronicaDetails().isEmpty()) {
            for (FacturaElectronicaDetail f : this.facturaElectronica.getFacturaElectronicaDetails()) {
                if (f.getTaxValue().compareTo(BigDecimal.ZERO) == 1) {
                    this.facturaElectronica.setSubtotalIVA12(this.facturaElectronica.getSubtotalIVA12().add(f.getTotalValue()));
                    this.facturaElectronica.setTotalIVA12(this.facturaElectronica.getTotalIVA12().add(f.getTaxValue().multiply(f.getAmount())));
                } else {
                    this.facturaElectronica.setSubtotalIVA0(this.facturaElectronica.getSubtotalIVA0().add(f.getTotalValue()));
                    this.facturaElectronica.setTotalIVA0(this.facturaElectronica.getTotalIVA0().add(f.getTaxValue().multiply(f.getAmount())));
                }
            }
        }
        this.facturaElectronica.setTotalSinImpuestos(this.facturaElectronica.getSubtotalIVA0().add(this.facturaElectronica.getSubtotalIVA12()));
        this.facturaElectronica.setImporteTotal(this.facturaElectronica.getTotalSinImpuestos().add(this.facturaElectronica.getTotalIVA0()).add(this.facturaElectronica.getTotalIVA12()).subtract(this.facturaElectronica.getTotalDescuento()));
        montoPorPagar();
    }

    public void calculateDateExpired() {
        if (this.facturaElectronica.getFechaEmision() != null && this.days >= 0) {
            this.facturaElectronica.setFechaVencimiento(Dates.addDays(this.facturaElectronica.getFechaEmision(), this.days));
        }
    }

    public void calculateDaysExpired() {
        this.days = 0;
        Date x = this.facturaElectronica.getFechaVencimiento();
        if (this.facturaElectronica.getFechaVencimiento() != null) {
            this.days = ((int) Dates.calculateNumberOfDaysBetween(Dates.now(), (Date) this.facturaElectronica.getFechaVencimiento())) + 1;
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

    public void openPanelDetail() {
        this.activePanelProduct = false;
    }

    public void openPanelProduct() {
        if (this.facturaElectronicaDetail.getProduct() != null) {
            this.productNew = this.facturaElectronicaDetail.getProduct();
        } else {
            this.productNew = productService.createInstance();
        }
        this.productNew.setPriceCost(BigDecimal.ZERO);
        this.productNew.setPrice(BigDecimal.ZERO);
        this.activePanelProduct = true;
    }

    public boolean saveButtonProduct() {
        return !(this.productNew.getName() != null && this.productNew.getTaxType() != null
                && this.productNew.getPriceCost() != null && this.productNew.getPrice() != null);
    }

    public void saveProductNew() {
        this.productNew.setProductType(ProductType.PRODUCT);
        this.productNew.setName(this.productNew.getName());
        this.productNew.setDescription(this.productNew.getName());
        this.productNew.setCategory(this.groupSelected);
        this.productNew.setAuthor(this.subject);
        this.productNew.setOrganization(this.organizationData.getOrganization());
        this.productNew.setOwner(this.subject);
        productService.save(this.productNew.getId(), this.productNew); //Volver a guardar el producto para almacenar el ggroup
        this.facturaElectronicaDetail.setProduct(this.productNew);
        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.inventory.product.add.success", " " + this.productNew.getName()));
        this.activePanelProduct = false;

        //Cargar producto en el cache
        productCache.load();
        setProducts(productCache.filterByOrganization(this.organizationData.getOrganization()));
        asignedPropiertiesProduct();
        this.productNew = productService.createInstance();
    }

    public void messageAlert() {
        if (this.facturaElectronicaId == null) {
            this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.purchase.start.message"));
        }
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

    public void clear() {
        this.lazyDataModel = null; //Realizar una nueva busqueda
    }

    private List<Detailable> makeDetailableList(List<FacturaElectronicaDetail> details) {
        List<Detailable> datailables = new ArrayList<>();
        details.forEach(d -> {
            datailables.add((Detailable) d);
        });

        return datailables;
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
                    record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal facturado: %s \nDetalle: %s", (_instance.getCode() + "/" + _instance.getId()),
                            _instance.getAuthor().getFullName(), Strings.format(_instance.getImporteTotal().doubleValue(), "$ #0.##"), _instance.getSummary()));
                } else if (EmissionType.PURCHASE_CREDIT.equals(_instance.getEmissionType())) {
                    if (_instance.getPayments().isEmpty()) {
                        record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal facturado a crédito: %s \nÚltimo pago registrado: ninguno \nDetalle: %s", (_instance.getCode() + "/" + _instance.getId()),
                                _instance.getAuthor().getFullName(), Strings.format(_instance.getImporteTotal().doubleValue(), "$ #0.##"), _instance.getSummary()));
                    } else {
                        record.setDescription(String.format("Nº Documento: %s \nProveedor: %s \nTotal de la factura: %s \nÚltimo pago registrado: %s \nDetalle: %s", (_instance.getCode() + "/" + _instance.getId()),
                                _instance.getAuthor().getFullName(), Strings.format(_instance.getImporteTotal().doubleValue(), "$ #0.##"), Strings.format(_instance.getPayments().get(_instance.getPayments().size() - 1).getAmount().doubleValue(), "$ #0.##"), _instance.getSummary()));
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

//    private void registerDetailInKardex() {
//        Kardex kardex = null;
//        KardexDetail kardexDetail = null;
//        for (FacturaElectronicaDetail facturaElectronicaDetail1 : this.facturaElectronica.getFacturaElectronicaDetails()) {
//            kardex = kardexService.findUniqueByNamedQuery("Kardex.findByProductAndOrg", facturaElectronicaDetail1.getProduct(), this.organizationData.getOrganization());
//            if (kardex == null) {
//                kardex = kardexService.createInstance();
//                kardex.setOwner(this.subject);
//                kardex.setAuthor(this.subject);
//                kardex.setOrganization(this.organizationData.getOrganization());
//                kardex.setProduct(facturaElectronicaDetail1.getProduct());
//                kardex.setName(facturaElectronicaDetail1.getProduct().getName());
//                kardex.setUnitMinimum(1L);
//                kardex.setUnitMaximum(1L);
//            } else {
//                kardex.setAuthor(this.subject); //Saber quien lo modificó por última vez
//                kardex.setLastUpdate(Dates.now()); //Saber la hora que modificó por última vez
//                kardexDetail = kardexDetailService.findUniqueByNamedQuery("KardexDetail.findByKardexAndFacturaAndOperation", kardex, facturaElectronicaDetail1.getFacturaElectronica(), KardexDetail.OperationType.COMPRA);
//            }
//            if (kardexDetail == null) {
//                kardexDetail = kardexDetailService.createInstance();
//                kardexDetail.setOwner(this.subject);
//                kardexDetail.setAuthor(this.subject);
//                kardexDetail.setFacturaElectronica(facturaElectronicaDetail1.getFacturaElectronica());
//                if (facturaElectronicaDetail1.getFacturaElectronica().getDocumentType().equals(FacturaElectronica.DocumentType.PRODUCCION)) {
//                    kardexDetail.setOperationType(KardexDetail.OperationType.PRODUCCION);
//                } else {
//                    kardexDetail.setOperationType(KardexDetail.OperationType.COMPRA);
//                }
//            } else {
//                //Disminuir los valores acumulados de cantidad y total para al momento de modificar no se duplique el valor a aumentar por la venta
//                if (kardexDetail.getQuantity() != null && kardexDetail.getTotalValue() != null) {
//                    kardex.setQuantity(kardex.getQuantity() - kardexDetail.getQuantity());
//                    kardex.setFund(kardex.getFund().subtract(kardexDetail.getTotalValue()));
//                    kardexDetail.setCummulativeQuantity(kardex.getQuantity());
//                    kardexDetail.setCummulativeTotalValue(kardex.getFund());
//                }
//                kardexDetail.setAuthor(this.subject); //Saber quien lo modificó por última vez
//                kardexDetail.setLastUpdate(Dates.now()); //Saber la hora que modificó por última vez
//            }
//            kardexDetail.setCode(facturaElectronicaDetail1.getFacturaElectronica().getCode());
//            kardexDetail.setUnitValue(facturaElectronicaDetail1.getUnitValue());
//            kardexDetail.setQuantity(facturaElectronicaDetail1.getQuantity());
//            kardexDetail.setTotalValue(kardexDetail.getUnitValue().multiply(BigDecimal.valueOf(kardexDetail.getQuantity())));
//
//            if (kardex.getId() == null) {
//                kardexDetail.setCummulativeQuantity(kardexDetail.getQuantity());
//                kardexDetail.setCummulativeTotalValue(kardexDetail.getTotalValue());
//            } else {
//                if (kardex.getQuantity() != null && kardex.getFund() != null) {
//                    kardexDetail.setCummulativeQuantity(kardex.getQuantity() + kardexDetail.getQuantity());
//                    kardexDetail.setCummulativeTotalValue(kardex.getFund().add(kardexDetail.getTotalValue()));
//                }
//            }
//
//            kardex.addKardexDetail(kardexDetail);
//            kardex.addKardexDetail(kardexDetail);
//            if (kardex.getCode() == null) {
//                kardex.setCode(settingHome.getValue("app.inventory.kardex.code.prefix", "TK-P-") + facturaElectronicaDetail1.getProduct().getId());
//            }
//            kardex.setQuantity(kardexDetail.getCummulativeQuantity());
//            kardex.setFund(kardexDetail.getCummulativeTotalValue());
//            kardexService.save(kardex.getId(), kardex);
//        }
//    }
}
