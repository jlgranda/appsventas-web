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
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.sales.KardexType;
import org.jlgranda.fede.ui.model.LazyKardexDataModel;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author usuario
 */
@Named
@ViewScoped
public class KardexProduccionHome extends FedeController implements Serializable {

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
     * UX.
     */
    private LazyKardexDataModel lazyDataModel;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfMonth(getEnd()) - 1))));
        clear();

        setOutcome("inventory-kardexs");
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
            lazyDataModel.setKardexType(KardexType.PRODUCCION);
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

    public LazyKardexDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyKardexDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
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
