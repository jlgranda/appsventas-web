/*
 * Copyright (C) 2022 usuario
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

import com.jlgranda.fede.ejb.sales.KardexDetailService;
import com.jlgranda.fede.ejb.sales.KardexService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import com.jlgranda.fede.ejb.sales.ProductService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.sales.Kardex;
import org.jlgranda.fede.model.sales.KardexDetail;
import org.jlgranda.fede.model.sales.KardexType;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.ui.model.LazyKardexDataModel;
import org.jlgranda.fede.ui.util.UI;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.Strings;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class KardexHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(KardexHome.class);

    @Inject
    private Subject subject;
    @Inject
    private SettingHome settingHome;
    @Inject
    private OrganizationData organizationData;

    @EJB
    private KardexService kardexService;
    @EJB
    private KardexDetailService kardexDetailService;
    @EJB
    private ProductCache productCache;
    @EJB
    private ProductService productService;

    /**
     * EDIT OBJECT.
     */
    private Long kardexId;
    private Kardex kardex;
    private KardexDetail kardexDetail;

    /**
     * UX.
     */
    private LazyKardexDataModel lazyDataModel;
    private List<Kardex> kardexSelected;
    private String type;
    private List<KardexDetail.OperationType> operationTypesOutput;
    private List<Product> productsContains;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfMonth(getEnd()) - 1))));
        clear();
        initializeActions();

        setKardex(kardexService.createInstance());
        setKardexDetail(kardexDetailService.createInstance());
        initVariables();

        setOutcome("inventory-kardexs");
    }

    public Long getKardexId() {
        return kardexId;
    }

    public void setKardexId(Long kardexId) {
        this.kardexId = kardexId;
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

    public KardexDetail getKardexDetail() {
        return kardexDetail;
    }

    public void setKardexDetail(KardexDetail kardexDetail) {
        this.kardexDetail = kardexDetail;
    }

    /**
     * METHODS.
     *
     */
    public void save() {
        if (kardex.isPersistent()) {
            kardex.setLastUpdate(Dates.now());
        } else {
            kardex.setAuthor(this.subject);
            kardex.setOwner(this.subject);
            this.kardex.setCode(settingHome.getValue("app.inventory.kardex.code.prefix", "TK-P-").concat(this.organizationData.getOrganization().getId().toString()).concat(this.kardex.getProduct().getId() != null ? this.kardex.getProduct().getId().toString() : ""));
            this.kardex.setName(this.kardex.getProduct().getName() != null ? this.kardex.getProduct().getName().toUpperCase() : "");
            kardex.setKardexType(ProductType.PRODUCT.equals(this.kardex.getProduct().getProductType()) ? KardexType.COMERCIALIZACION
                    : ProductType.SERVICE.equals(this.kardex.getProduct().getProductType()) ? KardexType.SERVICE
                    : ProductType.RAW_MATERIAL.equals(this.kardex.getProduct().getProductType()) ? KardexType.PRODUCCION : KardexType.DEPRECATED);
            kardex.setOrganization(this.organizationData.getOrganization());
        }
        kardexService.save(kardex.getId(), kardex);
        if (this.kardexId == null) {//Cuando se crea una kardex nueva
            redirectToByType(kardex);
        }
        setKardex(kardexService.find(kardex.getId()));
    }

    /**
     * METHODS UTIL.
     *
     */
    private void initVariables() {
        setProductsContains(productService.findWhithoutKardex(this.organizationData.getOrganization()));
    }

    public void initOperationTypes(String type) {
        setType(type);
        setOperationTypesOutput(UI.getOperationTypeByType(getType()));
    }

    public List<Product> filterProductsRawMaterial(String query) {
        return productCache.lookup(query, ProductType.RAW_MATERIAL, this.organizationData.getOrganization()); //sólo servicios
    }

    public List<Product> filterProductsProduct(String query) {
        return productCache.lookup(query, ProductType.PRODUCT, this.organizationData.getOrganization()); //sólo servicios
    }

    public List<Product> filterProductsService(String query) {
        return productCache.lookup(query, ProductType.SERVICE, this.organizationData.getOrganization()); //sólo servicios
    }

    public List<Product> filterProductsRawMaterialWhitContains(String query) {
        return productCache.lookupContains(query, ProductType.RAW_MATERIAL, this.organizationData.getOrganization(), this.productsContains);
    }

    public List<Product> filterProductsProductWhitContains(String query) {
        return productCache.lookupContains(query, ProductType.PRODUCT, this.organizationData.getOrganization(), this.productsContains);
    }

    public List<Product> filterProductsServiceWhitContains(String query) {
        return productCache.lookupContains(query, ProductType.SERVICE, this.organizationData.getOrganization(), this.productsContains);
    }

    public void clear() {
        filter();
    }

    private void filter() {
        if (this.organizationData.getOrganization() != null) {
            if (lazyDataModel == null) {
                lazyDataModel = new LazyKardexDataModel(kardexService);
            }
            lazyDataModel.setOrganization(this.organizationData.getOrganization());
            lazyDataModel.setKardexType(KardexType.COMERCIALIZACION);
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
    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                Kardex k = (Kardex) event.getObject();
                redirectTo("/pages/inventory/kardex.jsf?kardexId=" + k.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void getKardexByProduct() {
        Kardex k = kardexService.findByProductAndOrganization(this.kardex.getProduct(), this.subject, this.organizationData.getOrganization());
        if (k != null) {
            redirectToByType(k);
        }
    }

    private void redirectToByType(Kardex k) {
        try {
            if ("output".equals(this.type)) {
                redirectTo("/pages/inventory/kardex.jsf?kardexId=" + k.getId());
            } else if ("output-raw".equals(this.type)) {
                redirectTo("/pages/production/kardex_produccion.jsf?kardexId=" + k.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), (k));
        }
    }

    public void getDetailValues(String type) {//Establecer valores previos para el KardexDetail
        if (this.kardex.getId() != null && this.kardex.getProduct() != null) {
            setType(type);
            this.kardexDetail.setUnitValue(this.kardex.getProduct().getPriceCost() != null ? this.kardex.getProduct().getPriceCost() : BigDecimal.ZERO);
        }
    }

    public void kardexDetailAdd() {
        this.kardexDetail.setTotalValue(this.kardexDetail.getUnitValue().multiply(this.kardexDetail.getQuantity()));
//        String fechaIngresoBodega = Dates.toString(this.kardexDetail.getEntryOn(), settingHome.getValue("fede.name.pattern", "dd/MM/yyyy hh:mm:s"));
        String fechaIngresoBodega = Dates.toString(this.kardexDetail.getEntryOn());
        List<BigDecimal> resultSetQuantity = new ArrayList<>();
        List<BigDecimal> resultSetTotalValue = new ArrayList<>();
        System.out.println("this.operationTypesOutput::"+this.operationTypesOutput);
//        if (this.kardexId != null) {
            String queryQuantity = Strings.render("select cast(sum(case when kdd_.operation_type in ('%s') then kdd_.quantity * -1 else kdd_.quantity end) as decimal(5,2)) as col_0_0_ from Kardex_detail kdd_ where kdd_.kardex_id=%s and kdd_.fecha_ingreso_bodega <= '%s' and kdd_.deleted=false",
                    Lists.toString(this.operationTypesOutput, "','"), this.kardexId, fechaIngresoBodega);
            resultSetQuantity = kardexDetailService.findBigDecimalResultSet(queryQuantity);
            String queryTotalValue = Strings.render("select cast(sum(case when kdd_.operation_type in ('%s') then kdd_.total_value * -1 else kdd_.total_value end) as decimal(5,2)) as col_0_0_ from Kardex_detail kdd_ where kdd_.kardex_id=%s and kdd_.fecha_ingreso_bodega <= '%s' and kdd_.deleted=false",
                    Lists.toString(this.operationTypesOutput, "','"), this.kardexId, fechaIngresoBodega);
            resultSetTotalValue = kardexDetailService.findBigDecimalResultSet(queryTotalValue);
//        }
        if (!resultSetQuantity.isEmpty() && !resultSetTotalValue.isEmpty()) {

            BigDecimal quantityToDate = resultSetQuantity.get(0);
            BigDecimal totalValueToDate = resultSetTotalValue.get(0);
            System.out.println("quantityToDate:: "+quantityToDate);
            System.out.println("totalValueToDate:: "+totalValueToDate);

            //Al total a la fecha le agregamos la cantidad/total del nuevo detalle
            BigDecimal totalQuantity = this.operationTypesOutput.contains(this.kardexDetail.getOperationType()) ? quantityToDate.subtract(this.kardexDetail.getQuantity()) : quantityToDate.add(this.kardexDetail.getQuantity());
            BigDecimal totalValue = this.operationTypesOutput.contains(this.kardexDetail.getOperationType()) ? totalValueToDate.subtract(this.kardexDetail.getTotalValue()) : totalValueToDate.add(this.kardexDetail.getTotalValue());
            //Añadimos los valores al objeto en edición
            this.kardexDetail.setCummulativeQuantity(totalQuantity);
            this.kardexDetail.setCummulativeTotalValue(totalValue);

            //Buscamos los detalles posteriores al nuevo detalle
            List<KardexDetail> detailsAfter = this.kardex.getKardexDetails().stream().filter(kd -> kd.getEntryOn().compareTo(this.kardexDetail.getEntryOn()) > 0).collect(Collectors.toList());
            if (!detailsAfter.isEmpty()) {
                for (KardexDetail kdd : detailsAfter) {
                    kdd.setCummulativeQuantity(BigDecimal.ZERO);
                    kdd.setCummulativeTotalValue(BigDecimal.ZERO);
                    totalQuantity = this.operationTypesOutput.contains(kdd.getOperationType()) ? totalQuantity.subtract(kdd.getQuantity()) : totalQuantity.add(kdd.getQuantity());
                    totalValue = this.operationTypesOutput.contains(kdd.getOperationType()) ? totalValue.subtract(kdd.getTotalValue()) : totalValue.add(kdd.getTotalValue());
                    //Actualizamos los valores de la kardex y su detalle
                    kdd.setCummulativeQuantity(totalQuantity);
                    kdd.setCummulativeTotalValue(totalValue);
                    this.kardex.replaceKardexDetail(kdd);
                    this.kardex.setQuantity(kdd.getCummulativeQuantity());
                    this.kardex.setFund(kdd.getCummulativeTotalValue());
                }
            } else {
                this.kardex.setQuantity(this.kardexDetail.getCummulativeQuantity());
                this.kardex.setFund(this.kardexDetail.getCummulativeTotalValue());
            }

            this.kardexDetail.setAuthor(subject);
            this.kardexDetail.setOwner(subject);
            this.kardex.addKardexDetail(this.kardexDetail);
            setKardexDetail(kardexDetailService.createInstance());
            save();
        }
    }

    //Acciones sobre seleccionados
    private void initializeActions() {
        this.actions = new ArrayList<>();

        SelectItem item;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        this.actions.add(item);

        item = new SelectItem("borrar", I18nUtil.getMessages("common.delete"));
        this.actions.add(item);
    }

    public boolean isActionExecutable() {
        if ("borrar".equalsIgnoreCase(this.selectedAction) && !getKardexSelected().isEmpty()) {
            return true;
        }
        return false;
    }

    public void execute() {
        if (isActionExecutable()) {
            if ("borrar".equalsIgnoreCase(this.selectedAction)) {
                for (Kardex instance : getKardexSelected()) {
                    instance.setDeleted(Boolean.TRUE);
                    instance.setDescription("Esta kardex es referencia del producto " + instance.getProduct().getId() + " " + instance.getProduct().getName() + "la cual fue borrada.");
                    instance.setProduct(null);
                    kardexService.save(instance.getId(), instance); //Actualizar la entidad
                }
                this.addDefaultSuccessMessage();
                setKardex(kardexService.createInstance());
                setKardexSelected(new ArrayList<>());
                setOutcome("");
            }
        }
    }

    public List<Kardex> getKardexSelected() {
        return kardexSelected;
    }

    public void setKardexSelected(List<Kardex> kardexSelected) {
        this.kardexSelected = kardexSelected;
    }

    public LazyKardexDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyKardexDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<KardexDetail.OperationType> getOperationTypesOutput() {
        return operationTypesOutput;
    }

    public void setOperationTypesOutput(List<KardexDetail.OperationType> operationTypesOutput) {
        this.operationTypesOutput = operationTypesOutput;
    }

    public List<Product> getProductsContains() {
        return productsContains;
    }

    public void setProductsContains(List<Product> productsContains) {
        this.productsContains = productsContains;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        Record record = null;
        //El registro casí listo para agregar al journal
        return record;
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
