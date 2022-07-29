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
import com.jlgranda.fede.ejb.reportes.ReporteService;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import net.sf.jasperreports.engine.JRException;
import org.jlgranda.fede.Constantes;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.OrganizationData;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.controller.compras.ProveedorHome;
import org.jlgranda.fede.model.talentohumano.Employee;
import org.jlgranda.fede.ui.model.LazyEmployeeDataModel;
import org.jlgranda.reportes.ReportUtil;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jlgranda.fede.model.reportes.Reporte;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.BussinesEntity;

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
    
    @Inject
    private OrganizationData organizationData;
    
    private List<Reporte> reports;
    private Reporte selectedReport;
    
    @EJB
    private ReporteService reporteService;
    

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
        setReports(reporteService.findByModuloAndOrganization(Constantes.MODULE_PROVIDERS, organizationData.getOrganization()));
        initializeActions();
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
    
    public void clear(){
        this.filter();
    }

    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyEmployeeDataModel(employeeService);
        }
//        lazyDataModel.setOwner(null); //listar todos
        lazyDataModel.setOrganization(this.organizationData.getOrganization());;
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
    
    public void save(){
        if (employee.isPersistent()){
            employee.setLastUpdate(Dates.now());
            employeeService.save(employee.getId(), employee);
        } else {
            employee.setAuthor(this.subject);
            employee.setOrganization(this.organizationData.getOrganization());
            employeeService.save(employee);
        }
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

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Busca objetos <tt>Subject</tt> para la clave de búsqueda en las columnas
     * usernae, firstname, surname
     * @param keyword
     * @return una lista de objetos <tt>Subject</tt> que coinciden con la palabra clave dada.
     */
    public List<Employee> find(String keyword) {
        keyword = "%" + keyword.trim() + "%";
        Map<String, Object> filters = new HashMap<>();
        filters.put("code", keyword);
        filters.put("firstname", keyword);
        filters.put("surname", keyword);
        QueryData<Employee> queryData = employeeService.find("Employee.findByOwnerCodeAndName", -1, -1, "", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }
    
    public List<Reporte> getReports() {
        return reports;
    }

    public void setReports(List<Reporte> reports) {
        this.reports = reports;
    }

    public Reporte getSelectedReport() {
        return selectedReport;
    }

    public void setSelectedReport(Reporte selectedReport) {
        this.selectedReport = selectedReport;
    }
    
    //Acciones sobre seleccionados
    
    private void initializeActions() {
        this.actions = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        actions.add(item);

        item = new SelectItem("imprimir", I18nUtil.getMessages("common.print"));
        actions.add(item);
        
        item = new SelectItem("unactivate", "Desactivar");
        actions.add(item);
//        
//        item = new SelectItem("changeto", "Cambiar tipo a");
//        actions.add(item);
    }

    
    public boolean isActionExecutable() {
        if ("imprimir".equalsIgnoreCase(this.selectedAction)) {
            return true;
        } else if ("unactivate".equalsIgnoreCase(this.selectedAction) && this.getSelectedEmployees() != null){
            return true;
        } /*else if ("changeto".equalsIgnoreCase(this.selectedAction) && this.getProductType()!= null){
            return true;
        }*/
        return false;
    }
    
    public void execute() throws IOException {
        if (this.isActionExecutable() && !this.selectedEmployees.isEmpty()) {
            if ("imprimir".equalsIgnoreCase(this.selectedAction) && this.selectedReport != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("organizationId", this.organizationData.getOrganization().getId());
                this.selectedEmployees.forEach(prv -> {
                    params.put("proveedorId", prv.getId());
                    System.out.println("proveedorId: " + prv.getId());
                    byte[] encodedLogo = null;
                    try {
                        encodedLogo = this.organizationData.generarLogo();
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(ProveedorHome.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    params.put("logo", new String(encodedLogo));
                    try {
                        ReportUtil.getInstance().generarReporte(Constantes.DIRECTORIO_SALIDA_REPORTES, this.selectedReport.getRutaArchivoXml(), params);
                    } catch (JRException ex) {
                        java.util.logging.Logger.getLogger(ProveedorHome.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                setOutcome("");
            } else if ("unactivate".equalsIgnoreCase(this.selectedAction) && this.selectedEmployees != null && !this.selectedEmployees.isEmpty()) {
                this.selectedEmployees.forEach(emp -> {
                    emp.setActive(false);
                    this.employeeService.save(emp.getId(), emp);
                });
                
            }
        }
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    /**
     * Evento para redirigir en el caso de seleccionar un proveedor
     *
     * @param event
     */
    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                Employee p = (Employee) event.getObject();
                redirectTo("/pages/fede/pagos/employee.jsf?proveedorId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }
}
