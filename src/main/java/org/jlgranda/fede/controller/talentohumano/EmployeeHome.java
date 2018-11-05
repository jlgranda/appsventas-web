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
import com.jlgranda.fede.ejb.talentohumano.EmployeeService;
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
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.model.talentohumano.Employee;
import org.jlgranda.fede.ui.model.LazyEmployeeDataModel;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class EmployeeHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -3433906827306269659L;
    
    Logger logger = LoggerFactory.getLogger(EmployeeHome.class);
    
    private Long employeeId;
    
    private Employee employee;
    
    @Inject
    private SettingHome settingHome;
    
    protected List<Employee> selectedEmployees;
    
    private LazyEmployeeDataModel lazyDataModel;
    
    @EJB
    private EmployeeService employeeService;
    
    @Inject
    private Subject subject;
    
    @Inject
    private SubjectAdminHome subjectAdminHome; //para administrar clientes en factura

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
        
        setEmployee(employeeService.createInstance());
        setOutcome("employees");
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Employee getEmployee() {
         if (this.employeeId != null && !this.employee.isPersistent()) {
            this.employee = employeeService.find(employeeId);
        }
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public List<Employee> getSelectedEmployees() {
        return selectedEmployees;
    }

    public void setSelectedEmployees(List<Employee> selectedEmployees) {
        this.selectedEmployees = selectedEmployees;
    }

    public LazyEmployeeDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyEmployeeDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public SubjectAdminHome getSubjectAdminHome() {
        return subjectAdminHome;
    }

    public void setSubjectAdminHome(SubjectAdminHome subjectAdminHome) {
        this.subjectAdminHome = subjectAdminHome;
    }

    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyEmployeeDataModel(employeeService);
        }
        
        lazyDataModel.setOwner(null); //listar todos
        lazyDataModel.setAuthor(subject);

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
    
    public void save(){
        if (employee.isPersistent()){
            employee.setLastUpdate(Dates.now());
        } else {
            employee.setAuthor(this.subject);
        }
        employeeService.save(employee.getId(), employee);
    }
    
    public void saveEmployee(){
        getSubjectAdminHome().save("EMPLOYEE"); //Guardar profile
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
        super.openDialog("subject", width, height, left, top, true, params);
        return true;
    }
    
    public boolean mostrarFormularioProfile() {
        return mostrarFormularioProfile(null);
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        getEmployee().setOwner((Subject) event.getObject());
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
