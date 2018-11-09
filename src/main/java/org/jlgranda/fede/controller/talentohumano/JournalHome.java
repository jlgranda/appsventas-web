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

import com.google.common.base.Strings;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.talentohumano.EmployeeService;
import com.jlgranda.fede.ejb.talentohumano.JournalService;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.admin.SubjectAdminHome;
import org.jlgranda.fede.model.sales.Product;
import org.jlgranda.fede.model.talentohumano.Employee;
import org.jlgranda.fede.model.talentohumano.Journal;
import org.jlgranda.fede.ui.model.LazyEmployeeDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.timeline.TimelineSelectEvent;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class JournalHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -3433906827306269659L;
    
    Logger logger = LoggerFactory.getLogger(JournalHome.class);
    
    private Employee employee;
    private Employee employeeSelected;
    
    private Long employeeSelectedId;
    
    private Journal journal;
    
    @Inject
    private SettingHome settingHome;
    
    protected List<Employee> selectedEmployees;
    
    private LazyEmployeeDataModel lazyDataModel;
    
    @EJB
    private EmployeeService employeeService;
    
    @EJB
    private JournalService journalService;
    
    @Inject
    private Subject subject;
    
    private String password;
    
    PasswordService passwordService = new DefaultPasswordService();
    
    //UIX
    private TimelineModel model;
 
    private boolean selectable = true;
    private boolean zoomable = true;
    private boolean moveable = true;
    private boolean stackEvents = true;
    private String eventStyle = "box";
    private boolean axisOnTop;
    private boolean showCurrentTime = true;
    private boolean showNavigation = false;

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
        
        setEmployee(null); //Forzar inicilización con subject
        setEmployeeSelected(employeeService.createInstance());
        setOutcome("registrar");
        
        //Establecer objeto para nuevas entradas
        setJournal(journalService.createInstance());
        getJournal().setBeginTime(Dates.now()); //Fecha y hora actual
        
        //Modelo de línea de tiempo. Enfocar en el día actual
        model = new TimelineModel();
        Journal inicioDia = new Journal();
        inicioDia.setName("");
        inicioDia.setBeginTime(Dates.minimumDate(Dates.now()));
        Journal finDia = new Journal();
        finDia.setName("");
        finDia.setBeginTime(Dates.maximumDate(Dates.now()));
        model.add(new TimelineEvent(inicioDia, inicioDia.getBeginTime()));
        model.add(new TimelineEvent(finDia, finDia.getBeginTime()));
    }


    public Employee getEmployee() {
         if (this.employee == null
                 && this.subject != null 
                 && this.subject.isPersistent()) {
            this.employee = employeeService.findUniqueByNamedQuery("Employee.findByOwner", this.subject);
            for (Journal j : this.employee.getJournals()){
                model.add(new TimelineEvent(j, j.getBeginTime()));
            }
        
        }
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Employee getEmployeeSelected() {
        if (this.employeeSelectedId != null && !this.employeeSelected.isPersistent()) {
            this.employeeSelected = employeeService.find(employeeSelectedId);
        }
        return employeeSelected;
    }

    public void setEmployeeSelected(Employee employeeSelected) {
        this.employeeSelected = employeeSelected;
    }

    public Long getEmployeeSelectedId() {
        return employeeSelectedId;
    }

    public void setEmployeeSelectedId(Long employeeSelectedId) {
        this.employeeSelectedId = employeeSelectedId;
    }

    public List<Employee> getSelectedEmployees() {
        
        return selectedEmployees;
    }

    public void setSelectedEmployees(List<Employee> selectedEmployees) {
        this.selectedEmployees = selectedEmployees;
    }

    public Journal getJournal() {
        return journal;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public TimelineModel getModel() {
        return model;
    }

    public void setModel(TimelineModel model) {
        this.model = model;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isZoomable() {
        return zoomable;
    }

    public void setZoomable(boolean zoomable) {
        this.zoomable = zoomable;
    }

    public boolean isMoveable() {
        return moveable;
    }

    public void setMoveable(boolean moveable) {
        this.moveable = moveable;
    }

    public boolean isStackEvents() {
        return stackEvents;
    }

    public void setStackEvents(boolean stackEvents) {
        this.stackEvents = stackEvents;
    }

    public String getEventStyle() {
        return eventStyle;
    }

    public void setEventStyle(String eventStyle) {
        this.eventStyle = eventStyle;
    }

    public boolean isAxisOnTop() {
        return axisOnTop;
    }

    public void setAxisOnTop(boolean axisOnTop) {
        this.axisOnTop = axisOnTop;
    }

    public boolean isShowCurrentTime() {
        return showCurrentTime;
    }

    public void setShowCurrentTime(boolean showCurrentTime) {
        this.showCurrentTime = showCurrentTime;
    }

    public boolean isShowNavigation() {
        return showNavigation;
    }

    public void setShowNavigation(boolean showNavigation) {
        this.showNavigation = showNavigation;
    }

    public LazyEmployeeDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyEmployeeDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }
    
    public void onSelect(TimelineSelectEvent e) {
        TimelineEvent timelineEvent = e.getTimelineEvent();
        Journal j = (Journal) timelineEvent.getData();
        addWarningMessage(j.getName(), j.getEndTime().toString());
 
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
        if (getJournal().isPersistent()){
            getJournal().setLastUpdate(Dates.now());
        } else {
            getJournal().setLastUpdate(Dates.now());
            getJournal().setOwner(this.subject);
            getJournal().setAuthor(this.subject);
        }
        journalService.save(journal.getId(), journal);
    }
    
    public void add() throws IOException{
        getJournal().setName(calculeEvent(getEmployeeSelected()));
        getJournal().setEndTime(getJournal().getBeginTime()); //tiempo igual al de inicio
        getJournal().setLastUpdate(Dates.now());
        getJournal().setOwner(getEmployeeSelected().getOwner()); 
        getJournal().setAuthor(this.subject); //el admin
        getJournal().setEmployeeId(getEmployeeSelected().getId());
        getJournal().setDescription("Ingresado por " + this.subject.getFullName());
        
        journalService.save(getJournal().getId(), getJournal());
        
        //setOutcome("/pages/fede/talentohumano/registrar_manual.jsf?employeeSelectedId=" + getEmployeeSelected().getId());
        redirectTo("/pages/fede/talentohumano/registrar_manual.jsf?employeeSelectedId=" +  + getEmployeeSelected().getId());
    }
    
    public void addRest() throws IOException{
        Journal salida13h = journalService.createInstance();
        Journal entrada14h = journalService.createInstance();
        
        Calendar c13h = Calendar.getInstance();
        c13h.setTime(getJournal().getBeginTime());
        c13h.set(Calendar.HOUR_OF_DAY, 13);
        c13h.set(Calendar.MINUTE, 0);
        c13h.set(Calendar.SECOND, 0);
        c13h.set(Calendar.MILLISECOND, 0);
        
        Calendar c14h = Calendar.getInstance();
        c14h.setTime(getJournal().getBeginTime());
        c14h.set(Calendar.HOUR_OF_DAY, 14);
        c14h.set(Calendar.MINUTE, 0);
        c14h.set(Calendar.SECOND, 0);
        c14h.set(Calendar.MILLISECOND, 0);
        
        //Salida 13h
        salida13h.setName("Salida");
        salida13h.setBeginTime(c13h.getTime()); //tiempo de entrada
        salida13h.setEndTime(c13h.getTime()); //tiempo de entrada
        salida13h.setLastUpdate(Dates.now());
        salida13h.setOwner(getEmployeeSelected().getOwner()); 
        salida13h.setAuthor(this.subject); //el admin
        salida13h.setEmployeeId(getEmployeeSelected().getId());
        salida13h.setDescription("Ingresado por " + this.subject.getFullName());
        
        //Entrada 14h
        entrada14h.setName("Entrada");
        entrada14h.setBeginTime(c14h.getTime()); //tiempo de salida
        entrada14h.setEndTime(c14h.getTime()); //tiempo de salida
        entrada14h.setLastUpdate(Dates.now());
        entrada14h.setOwner(getEmployeeSelected().getOwner()); 
        entrada14h.setAuthor(this.subject); //el admin
        entrada14h.setEmployeeId(getEmployeeSelected().getId());
        entrada14h.setDescription("Ingresado por " + this.subject.getFullName());
        
        journalService.save(salida13h.getId(), salida13h);
        journalService.save(entrada14h.getId(), entrada14h);
        
        redirectTo("/pages/fede/talentohumano/registrar_manual.jsf?employeeSelectedId=" +  + getEmployeeSelected().getId());
    }
    
    
    public void check() {
        if (!Strings.isNullOrEmpty(getPassword()) && passwordService.passwordsMatch(getPassword(), getEmployee().getOwner().getPassword())){
            getJournal().setName(calculeEvent(getEmployee()));
            getJournal().setBeginTime(Dates.now());
            getJournal().setEndTime(Dates.now());
            getJournal().setEmployeeId(getEmployee().getId());
            save();
        } else {
            addWarningMessage("La contraseña no es válida!", "Vuelva a intentar.");
        }
    }
    
    public void checkRest() throws IOException{
        Journal salida13h = journalService.createInstance();
        Journal entrada14h = journalService.createInstance();
        
        Calendar c13h = Calendar.getInstance();
        c13h.setTime(getJournal().getBeginTime());
        c13h.set(Calendar.HOUR_OF_DAY, 13);
        c13h.set(Calendar.MINUTE, 0);
        c13h.set(Calendar.SECOND, 0);
        c13h.set(Calendar.MILLISECOND, 0);
        
        Calendar c14h = Calendar.getInstance();
        c14h.setTime(getJournal().getBeginTime());
        c14h.set(Calendar.HOUR_OF_DAY, 14);
        c14h.set(Calendar.MINUTE, 0);
        c14h.set(Calendar.SECOND, 0);
        c14h.set(Calendar.MILLISECOND, 0);
        
        //Salida 13h
        salida13h.setName("Salida");
        salida13h.setBeginTime(c13h.getTime()); //tiempo de entrada
        salida13h.setEndTime(c13h.getTime()); //tiempo de entrada
        salida13h.setLastUpdate(Dates.now());
        salida13h.setOwner(getEmployeeSelected().getOwner()); 
        salida13h.setAuthor(this.subject); //el admin
        salida13h.setEmployeeId(getEmployee().getId());
        salida13h.setDescription("Registrado por " + this.subject.getFullName());
        
        //Entrada 14h
        entrada14h.setName("Entrada");
        entrada14h.setBeginTime(c14h.getTime()); //tiempo de salida
        entrada14h.setEndTime(c14h.getTime()); //tiempo de salida
        entrada14h.setLastUpdate(Dates.now());
        entrada14h.setOwner(getEmployeeSelected().getOwner()); 
        entrada14h.setAuthor(this.subject); //el admin
        entrada14h.setEmployeeId(getEmployee().getId());
        entrada14h.setDescription("Registrado por " + this.subject.getFullName());
        
        journalService.save(salida13h.getId(), salida13h);
        journalService.save(entrada14h.getId(), entrada14h);
    }
    
    private String calculeEvent(Employee e){
        String eventName = "";
        if (e.getJournals().isEmpty()){
            eventName = "Entrada";
        } else if (e.getJournals().size() % 2 != 0){
            eventName = "Salida";
        } else {
            eventName = "Entrada";
        }
        
        return eventName;
    }
    
    public void onRowSelect(SelectEvent event) {
        System.out.println(">> onRowSelect");
//        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                Employee e = (Employee) event.getObject();
                System.out.println(">> Employe: " + e);
            }
//        } catch (IOException ex) {
//            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
//        }
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
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
