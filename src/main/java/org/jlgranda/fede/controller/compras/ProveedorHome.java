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
package org.jlgranda.fede.controller.compras;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.compras.ProveedorService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.compras.Proveedor;
import org.jlgranda.fede.ui.model.LazyProveedorDataModel;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class ProveedorHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -3433960827306269659L;

    Logger logger = LoggerFactory.getLogger(ProveedorHome.class);

    private Long proveedorId;

    private Proveedor proveedor;

    @Inject
    private SettingHome settingHome;

    protected List<Proveedor> selectedProveedores;

    private LazyProveedorDataModel lazyDataModel;

    @EJB
    private ProveedorService proveedorService;

    @Inject
    private Subject subject;

    @Inject
    private SubjectAdminHome subjectAdminHome; //para administrar clientes en factura

    @Inject
    private OrganizationData organizationData;

    @PostConstruct
    private void init() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.PRODUCT_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));

        setProveedor(proveedorService.createInstance());
        setOutcome("proveedores");
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public Proveedor getProveedor() {
        if (this.proveedorId != null && !this.proveedor.isPersistent()) {
            this.proveedor = proveedorService.find(proveedorId);
        }
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public List<Proveedor> getSelectedProveedores() {
        return selectedProveedores;
    }

    public void setSelectedEmployees(List<Proveedor> selectedProveedores) {
        this.selectedProveedores = selectedProveedores;
    }

    public LazyProveedorDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyProveedorDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public SubjectAdminHome getSubjectAdminHome() {
        return subjectAdminHome;
    }

    public void setSubjectAdminHome(SubjectAdminHome subjectAdminHome) {
        this.subjectAdminHome = subjectAdminHome;
    }

    public void clear() {
        this.filter();
    }

    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyProveedorDataModel(proveedorService);
        }
//        lazyDataModel.setOwner(null); //listar todos
        lazyDataModel.setOrganization(this.organizationData.getOrganization());
        //lazyDataModel.setAuthor(subject);

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

    public void save() {
        if (proveedor.isPersistent()) {
            if (proveedor.getOrganization() == null) {
                proveedor.setOrganization(organizationData.getOrganization());
            }
            proveedor.setLastUpdate(Dates.now());
        } else {
            proveedor.setOrganization(organizationData.getOrganization());
            proveedor.setAuthor(this.subject);
        }
        proveedorService.save(proveedor.getId(), proveedor);
        setOutcome("proveedores");
    }

    public void saveProveedor() {
        getSubjectAdminHome().save("PROVEEDOR"); //Guardar profile
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
        super.openDialog("subject", width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioProfile() {
        return mostrarFormularioProfile(null);
    }

    @Override
    public void handleReturn(SelectEvent event) {
        getProveedor().setOwner((Subject) event.getObject()); //Asocia el subject actual al proveedor
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Busca objetos <tt>Subject</tt> para la clave de búsqueda en las columnas
     * usernae, firstname, surname
     *
     * @param keyword
     * @return una lista de objetos <tt>Subject</tt> que coinciden con la
     * palabra clave dada.
     */
    public List<Subject> find(String keyword) {
        keyword = "%" + keyword.trim() + "%";
        Map<String, Object> filters = new HashMap<>();
        filters.put("code", keyword);
        filters.put("firstname", keyword);
        filters.put("surname", keyword);
        QueryData<Proveedor> queryData = proveedorService.find("Proveedor.findByOwnerCodeAndName", -1, -1, "", QuerySortOrder.ASC, filters);
//        QueryData<Proveedor> queryData = proveedorService.find("Proveedor.OwnerFindByOwnerCodeAndName", -1, -1, "", QuerySortOrder.ASC, filters);
        List<Subject> subjects = new ArrayList<>();
        if (queryData.getResult() != null && !queryData.getResult().isEmpty()) {
            for (Proveedor pr : queryData.getResult()) {
                System.out.println("pr::::::" + pr);
                subjects.add(pr.getOwner());
            }
        }
//        return queryData.getResult();
        return subjects;
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
