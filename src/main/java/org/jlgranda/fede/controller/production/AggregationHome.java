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
package org.jlgranda.fede.controller.production;

import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.production.AggregationDetailService;
import com.jlgranda.fede.ejb.production.AggregationService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.primefaces.event.SelectEvent;
import org.jlgranda.appsventas.data.ProductAggregations;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.production.Aggregation;
import org.jlgranda.fede.model.production.AggregationDetail;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.sales.ProductType;
import org.jlgranda.fede.ui.model.LazyAggregationDataModel;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class AggregationHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(AggregationHome.class);

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;
    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;
    @EJB
    private AggregationService aggregationService;
    @EJB
    private AggregationDetailService aggregationDetailService;
    @EJB
    private ProductCache productCache;

    /**
     * EDIT OBJECT.
     */
    private Long aggregationId;
    private Aggregation aggregation;
    private AggregationDetail aggregationDetail;

    /**
     * UX.
     */
    private LazyAggregationDataModel lazyDataModel;
    private BigDecimal priceUnit;
    private List<Aggregation> productosAgregaciones;
    private List<ProductAggregations> productoAgregaciones;

    @PostConstruct
    private void init() {
        setAggregation(aggregationService.createInstance());
        setAggregationDetail(aggregationDetailService.createInstance());
        setPriceUnit(BigDecimal.ZERO);
    }

    public Long getAggregationId() {
        return aggregationId;
    }

    public void setAggregationId(Long aggregationId) {
        this.aggregationId = aggregationId;
    }

    public Aggregation getAggregation() {
        if (this.aggregationId != null && this.aggregation != null && !this.aggregation.isPersistent()) {
            this.aggregation = aggregationService.find(this.aggregationId);
        }
        return this.aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public AggregationDetail getAggregationDetail() {
        return this.aggregationDetail;
    }

    public void setAggregationDetail(AggregationDetail aggregationDetail) {
        this.aggregationDetail = aggregationDetail;
    }

    public LazyAggregationDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyAggregationDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public BigDecimal getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(BigDecimal priceUnit) {
        this.priceUnit = priceUnit;
    }

    /**
     * METHODS.
     */
    public void aggregationDetailAdd() {
        if (this.aggregation.getProduct() != null) {
            if (this.aggregationDetail.getProduct() != null && this.aggregationDetail.getQuantity() != null && this.priceUnit != null) {
                if (BigDecimal.ZERO.compareTo(this.aggregationDetail.getQuantity()) == -1 && BigDecimal.ZERO.compareTo(this.aggregationDetail.getProduct().getPriceCost()) == -1) {
                    this.aggregationDetail.setCost(this.aggregationDetail.getQuantity().multiply(this.priceUnit));
                }
                this.aggregationDetailValid();
            } else {
                addWarningMessage(I18nUtil.getMessages("action.warn"), I18nUtil.getMessages("property.required.invalid.form"));
            }
        } else {
            addWarningMessage(I18nUtil.getMessages("action.warn"), I18nUtil.getMessages("app.fede.inventory.product.main.required"));
        }
    }

    public void saveAggregation() {
        if (this.aggregation.isPersistent()) {
            this.aggregation.setLastUpdate(Dates.now());
        } else {
            this.aggregation.setAuthor(subject);
            this.aggregation.setOwner(subject);
            this.aggregation.setOrganization(this.organizationData.getOrganization());
        }
        aggregationService.save(aggregation.getId(), aggregation);
        //Actualizar la agregación
        setAggregationId(this.aggregation.getId());
        this.getAggregation();
    }

    /**
     * METHODS UTIL.
     */
    public List<Product> filterProductsService(String query) {
        return productCache.lookup(query, ProductType.SERVICE, this.organizationData.getOrganization()); //sólo servicios
    }

    public List<Product> filterProductsRawMaterial(String query) {
        return productCache.lookup(query, ProductType.RAW_MATERIAL, this.organizationData.getOrganization()); //sólo servicios
    }

    private void aggregationDetailValid() {
        this.aggregationDetail.setAggregation(this.aggregation);
        System.out.println("AAAA::: " + this.aggregation.getOrganization());
        System.out.println("AAAA::: " + this.aggregation.getProduct());
        System.out.println("this.agg::: " + this.aggregationDetail.getProduct());
        System.out.println("this.agg::: " + this.aggregationDetail.getQuantity());
        System.out.println("this.agg::: " + this.aggregationDetail.getCost());
        if (this.aggregation.getAggregationDetails().contains(this.aggregationDetail)) {
            int index = this.aggregation.getAggregationDetails().indexOf(this.aggregationDetail);
            this.aggregation.getAggregationDetails().set(index, this.aggregationDetail);
        } else {
            this.aggregation.getAggregationDetails().add(this.aggregationDetail);
        }
        //Guardar la agregacion
        System.out.println("this.asdasd::: " + this.aggregation.getAggregationDetails());
        this.saveAggregation();
        this.aggregationDetail = aggregationDetailService.createInstance();
        this.priceUnit = BigDecimal.ZERO;
    }

    public void calculePriceUnit() {
        if (BigDecimal.ZERO.compareTo(this.aggregationDetail.getProduct().getPriceCost()) == -1) {
            this.priceUnit = this.aggregationDetail.getProduct().getPriceCost();
        }
    }

//BORRAR
    public List<Aggregation> getProductosAgregaciones() {
        return productosAgregaciones;
    }

    public void setProductosAgregaciones(List<Aggregation> productosAgregaciones) {
        this.productosAgregaciones = productosAgregaciones;
    }

    public List<ProductAggregations> getProductoAgregaciones() {
        return productoAgregaciones;
    }

    public void setProductoAgregaciones(List<ProductAggregations> productoAgregaciones) {
        this.productoAgregaciones = productoAgregaciones;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
