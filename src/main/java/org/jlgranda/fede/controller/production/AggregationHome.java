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

import com.jlgranda.fede.ejb.production.AggregationService;
import java.io.Serializable;
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

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class AggregationHome extends FedeController implements Serializable {

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;

    @EJB
    private AggregationService aggregationService;

    private List<ProductAggregations> productosAgregaciones;
    private ProductAggregations productoAgregaciones;

    @PostConstruct
    private void init() {
        productosAgregaciones = aggregationService.findByGroupProductAndOrganization(this.organizationData.getOrganization());
        System.out.println(">>>>>>>>>>>><productosAgregaciones: " + productosAgregaciones.get(0).costoTotal);
    }

    public List<ProductAggregations> getProductosAgregaciones() {
        return productosAgregaciones;
    }

    public void setProductosAgregaciones(List<ProductAggregations> productosAgregaciones) {
        this.productosAgregaciones = productosAgregaciones;
    }

    public ProductAggregations getProductoAgregaciones() {
        return productoAgregaciones;
    }

    public void setProductoAgregaciones(ProductAggregations productoAgregaciones) {
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
