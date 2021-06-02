/*
 * Copyright (C) 2021 kellypaulinc
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
package org.jlgranda.fede.controller.inventory;

import com.jlgranda.fede.ejb.FacturaElectronicaService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import com.jlgranda.fede.ejb.sales.KardexDetailService;
import com.jlgranda.fede.ejb.sales.KardexService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.sales.Kardex;
import org.jlgranda.fede.model.sales.KardexDetail;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.ui.model.LazyKardexDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kellypaulinc
 */
@Named
@ViewScoped
public class KardexInventoryHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(KardexInventoryHome.class);

    //VARIABLES AND OBJECTS
    @Inject
    private SettingHome settingHome;

    @Inject
    private Subject subject;

    @Inject
    private OrganizationData organizationData;

    @EJB
    private ProductService productService;

    @EJB
    private KardexService kardexService;

    @EJB
    private KardexDetailService kardexDetailService;

    @EJB
    private InvoiceService invoiceService;

    @EJB
    private FacturaElectronicaService facturaElectronicaService;

    private LazyKardexDataModel lazyDataModel; //Modelo de datos

    //VARIABLES AND OBJECTS TO EDIT
    private Product productSelected;
    private Kardex kardex;
    private Long kardexId;
    private KardexDetail kardexDetail;
    private boolean activeKardexEdition;
    private List<String> listInvoices;
    private List<String> listFacturas;
    private List<Product> productsWithoutKardex;

    @EJB
    private ProductCache productCache;

    @PostConstruct
    private void init() {
        setOutcome("inventory-kardexs");
        setKardex(kardexService.createInstance());
        setKardexDetail(kardexDetailService.createInstance());
        filter();
        setActiveKardexEdition(true);
        setListInvoices(invoiceService.findByNamedQuery("Invoice.findSequencialByDocumentTypeAndStatusAndOrg", this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString()));
        setListFacturas(facturaElectronicaService.findByNamedQuery("FacturaElectronica.findCodeByOrg", this.organizationData.getOrganization(), true));
        //generateProductsWithoutKardex();
    }

    //GETTER AND SETTER
    public Product getProductSelected() {
        return productSelected;
    }

    public void setProductSelected(Product productSelected) {
        this.productSelected = productSelected;
    }

    public LazyKardexDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyKardexDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Kardex getKardex() {
        if (this.kardexId != null && !this.kardex.isPersistent()) {
            this.kardex = kardexService.find(kardexId);
        }
        return kardex;
    }

    public void setKardex(Kardex kardex) {
        this.kardex = kardex;
    }

    public Long getKardexId() {
        return kardexId;
    }

    public void setKardexId(Long kardexId) {
        this.kardexId = kardexId;
    }

    public KardexDetail getKardexDetail() {
        return kardexDetail;
    }

    public void setKardexDetail(KardexDetail kardexDetail) {
        this.kardexDetail = kardexDetail;
    }

    public boolean isActiveKardexEdition() {
        return activeKardexEdition;
    }

    public void setActiveKardexEdition(boolean activeKardexEdition) {
        this.activeKardexEdition = activeKardexEdition;
    }

    public List<String> getListInvoices() {
        return listInvoices;
    }

    public void setListInvoices(List<Object[]> listInvoices) {
        this.listInvoices = new ArrayList<>();
        listInvoices.stream().forEach((Object object) -> {
            this.listInvoices.add((String) object);
        });
        for (KardexDetail kardexDetail1 : this.kardex.getKardexDetails()) {
            if (kardexDetail1.getOperation_type().equals(KardexDetail.OperationType.VENTA)) {
                this.listInvoices.add(kardexDetail1.getCode());
            }
        }
    }

    public List<String> getListFacturas() {
        return listFacturas;
    }

    public void setListFacturas(List<Object[]> listFacturas) {
        this.listFacturas = new ArrayList<>();
        listFacturas.stream().forEach((Object object) -> {
            this.listFacturas.add((String) object);
        });
        this.kardex.getKardexDetails().stream().filter(kardexDetail1 -> (kardexDetail1.getOperation_type().equals(KardexDetail.OperationType.COMPRA))).forEachOrdered(kardexDetail1 -> {
            this.listInvoices.add(kardexDetail1.getCode());
        });
    }

    public List<Product> getProductsWithoutKardex() {
        return productsWithoutKardex;
    }

    public void setProductsWithoutKardex(List<Product> productsWithoutKardex) {
        this.productsWithoutKardex = productsWithoutKardex;
    }

    //METHODS
    public List<Kardex> getKardexs() {
        return kardexService.findByOrganization(this.organizationData.getOrganization());
    }

    public List<Product> getProducts() {
        //return productService.findByOrganization(this.organizationData.getOrganization());
        //Obtener los productos desde el cache, filtrar por organization
        //productCache.load();
        return productCache.filterByOrganization(this.organizationData.getOrganization());
    }

    public void generateProductsWithoutKardex() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> generateProductsWithoutKardex " + Dates.now());
        boolean exist = false;
        this.productsWithoutKardex = this.productService.findWhithoutKardex(this.organizationData.getOrganization());
//        List<Kardex> kardexs = this.getKardexs();
//        
//            for (Kardex kd2 : kardexs) {
//                for (Product product : getProducts()) {
//                if (product.equals(kd2.getProduct())) {
//                    exist = true;
//                    break;
//                } else {
//                    exist = false;
//                    break;
//                }
//            }
//            if (exist == false) {
//                this.productsWithoutKardex.add(product);
//            }
//        }
        if (this.kardexId != null && this.kardex.getProduct() != null) {
            this.productsWithoutKardex.add(this.kardex.getProduct());
        }

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> //generateProductsWithoutKardex " + Dates.now());
    }

    public boolean hasProductsWithoutKardex() {
        return this.productService.count("Product.countWhithoutKardex", this.organizationData.getOrganization()) > 0;
    }

    public void clear() {
        filter();
    }

    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyKardexDataModel(kardexService);
        }
        lazyDataModel.setOrganization(this.organizationData.getOrganization());
        // lazyDataModel.setOwner(this.subject);
        // lazyDataModel.setStart(this.getStart());
        // lazyDataModel.setEnd(this.getEnd());
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
                Kardex k = (Kardex) event.getObject();
                redirectTo("/pages/fede/inventory/kardex.jsf?kardexId=" + k.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void messagesValidation() { //Emitir mensajes de validación por Cantidad de Producto
        if (this.kardexId != null && !this.kardex.getKardexDetails().isEmpty()) {
            getKardex(); //Cargar el Kardex del kardexId seleccionado en onRowSelect
            if (this.kardex.getQuantity() < this.kardex.getUnit_minimum()) {
                this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.kardex.minimum"));
            } else if (this.kardex.getQuantity() > this.kardex.getUnit_maximum()) {
                this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.kardex.maximum"));
            }
        }
    }

    public void asignedMaximum() {
        if (kardexId == null) {
            this.kardex.setUnit_maximum(1L);
        }
        if (this.kardex.getUnit_maximum().compareTo(this.kardex.getUnit_minimum()) == -1) {
            getKardex().setUnit_maximum(getKardex().getUnit_minimum());
        }
    }

    public void activePanelKardex() {
        if (this.kardexId != null) {
            activeKardexEdition = false; //Habilitar los inputs de propiedades
        }
    }

    public void editKardex() {
        if (this.kardex.getUnit_measure() != null && this.kardex.getUnit_minimum() != null && this.kardex.getUnit_maximum() != null) {
            if (this.kardex.getUnit_maximum().compareTo(this.kardex.getUnit_minimum()) == -1) {
                this.addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.inventory.kardex.maximum.minimum.valid"));
            } else {
                save();
                this.kardex = kardexService.find(this.kardex.getId());
                messagesValidation();//Emitir mensajes de validación por Cantidad de Producto
                this.activeKardexEdition = true; //Deshabilidar los inputs de propiedades
            }
        }
    }

    public void asignedKardexDetailProperties() {//Establecer valores previos para el KardexDetail
        if (this.kardex.getProduct() != null) {
            if (this.kardex.getProduct().getPriceCost() != null) {
                this.kardexDetail.setUnit_value(this.kardex.getProduct().getPriceCost());
            } else {
                this.kardexDetail.setUnit_value(BigDecimal.ZERO);
            }
        } else {
            this.kardexDetail.setUnit_value(BigDecimal.ZERO);
        }
    }

    public boolean validatedKardexDetail() {
        return !(this.kardexDetail.getOperation_type() != null && this.kardexDetail.getQuantity() != null && this.kardexDetail.getUnit_value() != null);
    }

    public void refreshFundKardex() {
        Kardex kardexUpdate = kardexService.find(this.kardexId);
        if (kardexUpdate != null) {
            if (!kardexUpdate.getKardexDetails().isEmpty()) {
                // Ordenar la lista por el atributo getCreatedOne(), para actualizar el saldo de la Kardex
                Collections.sort(kardexUpdate.getKardexDetails(), (KardexDetail kardexDetail1, KardexDetail other) -> kardexDetail1.getCreatedOn().compareTo(other.getCreatedOn()));

                for (int i = 0; i < kardexUpdate.getKardexDetails().size(); i++) { //Calcular Saldo de Kardex
                    if (kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.EXISTENCIA_INICIAL)
                            || kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.PRODUCCION)
                            || kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)
                            || kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.COMPRA)) { //Calcular el Saldo del Kardex
                        if (i == 0) {
                            kardexUpdate.getKardexDetails().get(i).setCummulative_quantity(kardexUpdate.getKardexDetails().get(i).getQuantity());
                            kardexUpdate.getKardexDetails().get(i).setCummulative_total_value(kardexUpdate.getKardexDetails().get(i).getTotal_value());
                        } else {
                            kardexUpdate.getKardexDetails().get(i).setCummulative_quantity(kardexUpdate.getKardexDetails().get(i - 1).getCummulative_quantity() + kardexUpdate.getKardexDetails().get(i).getQuantity());
                            kardexUpdate.getKardexDetails().get(i).setCummulative_total_value(kardexUpdate.getKardexDetails().get(i - 1).getCummulative_total_value().add(kardexUpdate.getKardexDetails().get(i).getTotal_value()));
                        }
                    } else if (kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)
                            || kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.VENTA)
                            || kardexUpdate.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.SALIDA_INVENTARIO)) {
                        if (i == 0) {
                            kardexUpdate.getKardexDetails().get(i).setCummulative_quantity(kardexUpdate.getKardexDetails().get(i).getQuantity() * -1);
                            kardexUpdate.getKardexDetails().get(i).setCummulative_total_value(kardexUpdate.getKardexDetails().get(i).getTotal_value().multiply(BigDecimal.valueOf(-1)));
                        } else {
                            kardexUpdate.getKardexDetails().get(i).setCummulative_quantity(kardexUpdate.getKardexDetails().get(i - 1).getCummulative_quantity() - kardexUpdate.getKardexDetails().get(i).getQuantity());
                            kardexUpdate.getKardexDetails().get(i).setCummulative_total_value(kardexUpdate.getKardexDetails().get(i - 1).getCummulative_total_value().subtract(kardexUpdate.getKardexDetails().get(i).getTotal_value()));
                        }
                    }
                    kardexUpdate.setQuantity(kardexUpdate.getKardexDetails().get(i).getCummulative_quantity()); //Actualizar Saldo del Kardex
                    kardexUpdate.setFund(kardexUpdate.getKardexDetails().get(i).getCummulative_total_value());
                }
            }
            kardexService.save(kardexUpdate.getId(), kardexUpdate);
        }
    }

    public void addKardexDetail() {
        boolean existTransaction = true;
        if ((this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.VENTA) || this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.SALIDA_INVENTARIO))
                && this.kardexDetail.getQuantity() > this.kardex.getQuantity()) {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.inventory.kardex.maximum.sales"));
        } else {
            Long residue = 0L;
            if (this.kardexId != null) {
                List<Object[]> objects = kardexDetailService.findByNamedQuery("KardexDetail.findTotalQuantityByKardexAndCode", this.kardex, this.kardexDetail.getCode());
                for (int i = 0; i < objects.size(); i++) { //Calcular el residuo de las transacciones de un comprobante según el código
                    if (objects.get(i)[0].equals(KardexDetail.OperationType.COMPRA) || objects.get(i)[0].equals(KardexDetail.OperationType.VENTA)) {
                        residue = residue + (Long) objects.get(i)[1];
                    } else if (objects.get(i)[0].equals(KardexDetail.OperationType.DEVOLUCION_COMPRA) || objects.get(i)[0].equals(KardexDetail.OperationType.DEVOLUCION_VENTA)) {
                        residue = residue - (Long) objects.get(i)[1];
                    }
                }
            }
            System.out.println("residue A: " + residue);
            List<KardexDetail> listNewKardexDetails = new ArrayList<>();
            for (KardexDetail kd : this.kardex.getKardexDetails()) {
                if (kd.getId() == (null) && kd.getCode().equals(this.kardexDetail.getCode())) {
                    listNewKardexDetails.add(kd);
                }
            }

            if (!this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.EXISTENCIA_INICIAL)
                    && !this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.PRODUCCION)
                    && !this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.SALIDA_INVENTARIO)) {
                if (!listNewKardexDetails.contains(this.kardexDetail)) {
                    if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.COMPRA)
                            || this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.VENTA)) {
                        if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.VENTA)) {
                            this.listInvoices.add(this.kardexDetail.getCode());
                        } else if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.COMPRA)) {
                            this.listFacturas.add(this.kardexDetail.getCode());
                        }
                    } else {
                        if ((this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA) && this.listFacturas.contains(this.kardexDetail.getCode()))
                                || (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA) && this.listInvoices.contains(this.kardexDetail.getCode()))) {
                            if (listNewKardexDetails.isEmpty()) {
                                residue = residue - this.kardexDetail.getQuantity();
                            } else {
                                if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)) {
                                    for (KardexDetail kd1 : listNewKardexDetails) {
                                        if (kd1.getOperation_type().equals(KardexDetail.OperationType.COMPRA)) {
                                            residue = residue + kd1.getQuantity();
                                        } else if (kd1.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)) {
                                            residue = residue - this.kardexDetail.getQuantity();
                                        }
                                    }
                                } else if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)) {
                                    for (KardexDetail kd1 : listNewKardexDetails) {
                                        if (kd1.getOperation_type().equals(KardexDetail.OperationType.VENTA)) {
                                            residue = residue + kd1.getQuantity();
                                        } else if (kd1.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)) {
                                            residue = residue - this.kardexDetail.getQuantity();
                                        }
                                    }
                                }
                                residue = residue - this.kardexDetail.getQuantity();
                            }
                        } else {
                            existTransaction = false;
                            if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)) {
                                this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.inventory.kardex.operationtype.dev.purchases") + this.kardexDetail.getOperation_type());
                            } else if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)) {
                                this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.inventory.kardex.operationtype.dev.sales") + this.kardexDetail.getOperation_type());
                            }
                        }
                    }
                } else {
                    System.out.println("residue: B" + residue);
                    if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)) {
                        for (KardexDetail kd1 : listNewKardexDetails) {
                            if (kd1.getOperation_type().equals(KardexDetail.OperationType.COMPRA)) {
                                residue = residue + kd1.getQuantity();
                            } else if (kd1.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)) {
                                residue = residue - this.kardexDetail.getQuantity();
                            }
                        }
                    } else if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)) {
                        for (KardexDetail kd1 : listNewKardexDetails) {
                            if (kd1.getOperation_type().equals(KardexDetail.OperationType.VENTA)) {
                                residue = residue + kd1.getQuantity();
                            } else if (kd1.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)) {
                                residue = residue - this.kardexDetail.getQuantity();
                            }
                        }
                    }
                    System.out.println("residue: C" + residue);
                }
                System.out.println("residue: D" + residue);
            }
            if (existTransaction == true) {
                if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA) && (residue < 0)) {
                    this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.inventory.kardex.operationtype.code.dev.sales"));
                } else if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA) && (residue < 0)) {
                    this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.inventory.kardex.operationtype.code.dev.purchases"));
                } else {
                    if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA) && this.kardexDetail.getQuantity() > this.kardex.getQuantity()) {
                        this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.inventory.kardex.maximum.dev.purchases"));
                    } else {
                        this.kardexDetail.setAuthor(this.subject);
                        this.kardexDetail.setOwner(this.subject);
                        this.kardexDetail.setTotal_value(this.kardexDetail.getUnit_value().multiply((BigDecimal.valueOf(this.kardexDetail.getQuantity())))); //Calcular valor total del KardexDetail
                        if (this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.EXISTENCIA_INICIAL)
                                || this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.PRODUCCION)
                                || this.kardexDetail.getOperation_type().equals(KardexDetail.OperationType.SALIDA_INVENTARIO)) {
                            this.kardexDetail.setCode(I18nUtil.getMessages("common.nonen"));
                        }
                        if (this.kardex.getKardexDetails().isEmpty()) {
                            this.kardexDetail.setCummulative_quantity(this.kardexDetail.getQuantity());
                            this.kardexDetail.setCummulative_total_value(this.kardexDetail.getTotal_value());
                        }
                        this.kardex.addKardexDetail(this.kardexDetail);
                        // Ordenar la lista por el atributo getCreatedOne(), para actualizar el saldo de la Kardex
                        Collections.sort(this.kardex.getKardexDetails(), (KardexDetail kardexDetail1, KardexDetail other) -> kardexDetail1.getCreatedOn().compareTo(other.getCreatedOn()));

                        for (int i = 1; i < this.kardex.getKardexDetails().size(); i++) { //Calcular Saldo de Kardex
                            if (this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.EXISTENCIA_INICIAL)
                                    || this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.PRODUCCION)
                                    || this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_VENTA)
                                    || this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.COMPRA)) { //Calcular el Saldo del Kardex
                                this.kardex.getKardexDetails().get(i).setCummulative_quantity(this.kardex.getKardexDetails().get(i - 1).getCummulative_quantity() + this.kardex.getKardexDetails().get(i).getQuantity());
                                this.kardex.getKardexDetails().get(i).setCummulative_total_value(this.kardex.getKardexDetails().get(i - 1).getCummulative_total_value().add(this.kardex.getKardexDetails().get(i).getTotal_value()));
                            } else if (this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.DEVOLUCION_COMPRA)
                                    || this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.VENTA)
                                    || this.kardex.getKardexDetails().get(i).getOperation_type().equals(KardexDetail.OperationType.SALIDA_INVENTARIO)) {
                                this.kardex.getKardexDetails().get(i).setCummulative_quantity(this.kardex.getKardexDetails().get(i - 1).getCummulative_quantity() - this.kardex.getKardexDetails().get(i).getQuantity());
                                this.kardex.getKardexDetails().get(i).setCummulative_total_value(this.kardex.getKardexDetails().get(i - 1).getCummulative_total_value().subtract(this.kardex.getKardexDetails().get(i).getTotal_value()));
                            }
                        }
                        this.kardex.setQuantity(this.kardexDetail.getCummulative_quantity()); //Actualizar Saldo del Kardex
                        this.kardex.setFund(this.kardexDetail.getCummulative_total_value());
                    }
                }
            }
        }
        this.kardexDetail = kardexDetailService.createInstance(); //Recargar para la nueva instancia
    }

    public List<String> completeCode(String query) {
        String queryLowerCase = query.toLowerCase();
        List<String> codeList = new ArrayList<>();
        switch (this.kardexDetail.getOperation_type()) {
            case DEVOLUCION_VENTA:
                for (String x : getListInvoices()) {
                    codeList.add(x);
                }
                break;
            case DEVOLUCION_COMPRA:
                for (String x : getListFacturas()) {
                    codeList.add(x);
                }
                break;
            default:
                codeList.add("null");
                break;
        }
        return codeList.stream().filter(t -> t.toLowerCase().startsWith(queryLowerCase)).collect(Collectors.toList());
    }

    public List<Product> completeProductKardex(String query) {

        if (productsWithoutKardex.isEmpty()) {
            generateProductsWithoutKardex(); //intentar cargar la lista
        }

        String queryLowerCase = query.toLowerCase();
        return productsWithoutKardex.stream().filter(t -> t.getName().toLowerCase().contains(queryLowerCase)).collect(Collectors.toList());
    }

    public void save() { //Guardar el Kardex, con todos sus KardexDetails
        if (this.kardex.getKardexDetails().isEmpty()) {
            this.kardex.setQuantity(0L);
            this.kardex.setFund(BigDecimal.ZERO);
        }
        if (this.kardex.isPersistent()) {
            this.kardex.setLastUpdate(Dates.now());
        } else {
            this.kardex.setAuthor(this.subject);
            this.kardex.setOwner(this.subject);
            this.kardex.setOrganization(this.organizationData.getOrganization());
        }
        kardexService.save(this.kardex.getId(), this.kardex);
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
