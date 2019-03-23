/*
 * Copyright (C) 2018 jlgranda
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
package org.jlgranda.fede.controller.talentohumano;

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.talentohumano.JobRoleService;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.talentohumano.Employee;
import org.jlgranda.fede.model.talentohumano.JobRole;
import org.jlgranda.fede.ui.model.LazyJobRoleDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class JobRoleHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1297269824173991802L;

    
    Logger logger = LoggerFactory.getLogger(JobRoleHome.class);
    
    private Long jobRoleId;
    
    private JobRole jobRole;
    
    @Inject
    private SettingHome settingHome;
    
    protected List<Employee> selectedEmployees;
    
    private LazyJobRoleDataModel lazyDataModel;
    
    @EJB
    private JobRoleService jobRoleService;
    
    @Inject
    private Subject subject;

    @PostConstruct
    private void init() {
        
        setJobRole(jobRoleService.createInstance());
        
        setOutcome("jobroles");
    }

    public Long getJobRoleId() {
        return jobRoleId;
    }

    public void setJobRoleId(Long jobRoleId) {
        this.jobRoleId = jobRoleId;
    }

    public JobRole getJobRole() {
        if (this.jobRoleId != null && !this.jobRole.isPersistent()) {
            this.jobRole = jobRoleService.find(jobRoleId);
        }
        return jobRole;
    }

    public void setJobRole(JobRole jobRole) {
        this.jobRole = jobRole;
    }


    public List<Employee> getSelectedEmployees() {
        return selectedEmployees;
    }

    public void setSelectedEmployees(List<Employee> selectedEmployees) {
        this.selectedEmployees = selectedEmployees;
    }

    public LazyJobRoleDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyJobRoleDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    
    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyJobRoleDataModel(jobRoleService);
        }
        
        lazyDataModel.setOwner(subject);

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
    
    /**
     * Limpiar para refrescar vista
     */
    public void clear(){
        filter();
    }
    
    public void save(){
        if (jobRole.isPersistent()){
            jobRole.setLastUpdate(Dates.now());
        } else {
            jobRole.setAuthor(this.subject);
            jobRole.setOwner(this.subject);
        }
        jobRoleService.save(jobRole.getId(), jobRole);
    }
    
    /**
     * Mostrar el formulario para edición de clientes
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
        return mostrarFormularioProfile(null);
    }
    
    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                JobRole p = (JobRole) event.getObject();
                redirectTo("/pages/fede/talentohumano/role.jsf?jobRoleId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
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
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
