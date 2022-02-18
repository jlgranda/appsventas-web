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
import com.jlgranda.fede.ejb.production.AggregationService;
import com.jlgranda.fede.ejb.sales.ProductCache;
import java.io.Serializable;
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
    private ProductCache productCache;

    /**
     * Objeto de edición.
     */
    private Long aggregationId;
    private Aggregation aggregation;
    private AggregationDetail detail;
    
    /**
     * Auxiliares.
     */
    private LazyAggregationDataModel lazyDataModel;

    @PostConstruct
    private void init() {
        setAggregation(aggregationService.createInstance());

    }

    public Aggregation getAggregation() {
        if (this.aggregationId != null && this.aggregation != null && !this.aggregation.isPersistent()) {
            this.aggregation = aggregationService.find(aggregationId);
        }
        return this.aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public Long getAggregationId() {
        return aggregationId;
    }

    public void setAggregationId(Long aggregationId) {
        this.aggregationId = aggregationId;
    }

    public AggregationDetail getAggregationDetail() {
        return this.detail;
    }

    public void setAggregationDetail(AggregationDetail detail) {
        this.detail = detail;
    }

    public LazyAggregationDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyAggregationDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public List<Product> filterProducts(String query) {
        return productCache.lookup(query, ProductType.PRODUCT, this.organizationData.getOrganization()); //sólo productos
    }

    public void aggregationAdd() {
//        if (this.aggregation.getProduct() != null) {
//            if (this.detail.getElement() != null && this.detail.getQuantity() != null && this.detail.getCost() != null) {
//                this.aggregation.getDetails().add(this.detail);
////                this.detail = aggregationService.createInstance();
//            }
//        } else {
//            addWarningMessage(I18nUtil.getMessages("action.warn"), "Se requiere seleccionar el producto principal!");
//        }
    }

    public void saveAggregation() {
        if (aggregation.isPersistent()) {
            aggregation.setLastUpdate(Dates.now());
        } else {
            aggregation.setAuthor(subject);
            aggregation.setOwner(subject);
//            aggregation.setOrganization(this.organizationData.getOrganization());
        }
        aggregationService.save(aggregation.getId(), aggregation);
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
