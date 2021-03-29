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
import com.jlgranda.fede.ejb.talentohumano.EmployeeService;
import com.jlgranda.fede.ejb.talentohumano.JournalService;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.model.DataModel;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.talentohumano.Employee;
import org.jlgranda.fede.model.talentohumano.Journal;
import org.jlgranda.fede.ui.model.LazyEmployeeDataModel;
import org.jlgranda.fede.ui.model.LazyJournalDataModel;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.CellEditEvent;
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

    private Long employeeId;

    private Long employeeSelectedId;

    private Journal journal;

    @Inject
    private SettingHome settingHome;

    protected List<Employee> selectedEmployees;

    private LazyEmployeeDataModel lazyEmployeeDataModel;

    private LazyJournalDataModel lazyDataModel;

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
    private boolean axisOnTop = true;
    private boolean showCurrentTime = true;
    private boolean showNavigation = true;

    private boolean showCurrentDay = false;
    private boolean checkEndTime = false; //El usuario marca si pica la salida
    
    /**
     * Selector de grupos de fechas
     */
    private Date monthSelected;
    
    private JournalSummary journalSummary;

    @PostConstruct
    private void init() {

        initializeDateInterval();
        setEmployee(null); //Forzar inicilización con subject
        setEmployeeSelected(employeeService.createInstance());
        setOutcome("registrar");

        //Establecer objeto para nuevas entradas
        setJournal(journalService.createInstance());
        getJournal().setBeginTime(Dates.now()); //Fecha y hora actual
        
        //Establecer el mes por defecto
        setMonthSelected(Dates.now()); //El mes actual
        
        setJournalSummary(new JournalSummary());

    }

    public Employee getEmployee() {
        if (getEmployeeId() != null) { //Cargar el empleado indicado por employeeId
            this.employee = employeeService.find(getEmployeeId());
        } else if (this.employee == null
                && this.subject != null
                && this.subject.isPersistent()
                && !"admin".equalsIgnoreCase(this.subject.getUsername())) {//Sino el empleado logeado en la sessión
            this.employee = employeeService.findUniqueByNamedQuery("Employee.findByOwner", this.subject);
        }
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Employee getEmployeeSelected() {
        if (this.employeeSelectedId != null 
                && null != this.employeeSelected
                && !this.employeeSelected.isPersistent()) {
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

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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

    public Date getMonthSelected() {
        return monthSelected;
    }

    public void setMonthSelected(Date monthSelected) {
        this.monthSelected = monthSelected;
    }

    public JournalSummary getJournalSummary() {
        return journalSummary;
    }

    public void setJournalSummary(JournalSummary journalSummary) {
        this.journalSummary = journalSummary;
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

    public LazyEmployeeDataModel getLazyEmployeeDataModel() {
        filter();
        return lazyEmployeeDataModel;
    }

    public void setShowCurrentDay(boolean showCurrentDay) {
        this.showCurrentDay = showCurrentDay;
        clear(); //forzar carga de lazyDataModel
        initializeDateInterval();
    }

    public boolean isShowCurrentDay() {
        return showCurrentDay;
    }

    public boolean isCheckEndTime() {
        return checkEndTime;
    }

    public void setCheckEndTime(boolean checkEndTime) {
        this.checkEndTime = checkEndTime;
    }

    public void setLazyEmployeeDataModel(LazyEmployeeDataModel lazyDataModel) {
        this.lazyEmployeeDataModel = lazyDataModel;
    }

    public void setLazyDataModel(LazyJournalDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public LazyJournalDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void onSelect(TimelineSelectEvent e) {
        TimelineEvent timelineEvent = e.getTimelineEvent();
        Journal j = (Journal) timelineEvent.getData();
        addWarningMessage(j.getName(), j.getEndTime().toString());

    }

    public void clear() {
        lazyDataModel = null;
    }

    /**
     * Filtro que llena el Lazy Datamodel
     */
    private void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyJournalDataModel(journalService);
        }

        if (getEmployee() != null)
            lazyDataModel.setOwner(getEmployee().getOwner()); //listar todos
        else if (getEmployeeSelected() != null)
            lazyDataModel.setOwner(getEmployeeSelected().getOwner()); //listar todos
        
        lazyDataModel.setAuthor(null);
        lazyDataModel.setStart(this.getStart());
        lazyDataModel.setEnd(this.getEnd());

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
        if (getJournal().isPersistent()) {
            getJournal().setLastUpdate(Dates.now());
        } else {
            getJournal().setLastUpdate(Dates.now());
            getJournal().setOwner(getEmployee().getOwner()); //El propietario de los registros
            getJournal().setAuthor(this.subject); //Quien registra el registro
        }
        journalService.save(journal.getId(), journal);
    }

    public void add() throws IOException {

        if (getEmployeeSelected() == null) {
            addDefaultErrorMessage();
        } else {
            Journal begin = buildJournal("REGISTRO", getJournal().getBeginTime(), this.subject, getEmployeeSelected(), getJournal().getDescription());
            journalService.save(begin.getId(), begin);

            if (isCheckEndTime()) {
                Journal end = buildJournal("REGISTRO", getJournal().getEndTime(), this.subject, getEmployeeSelected(), getJournal().getDescription());

                journalService.save(end.getId(), end);
            }

            redirectTo("/pages/fede/talentohumano/registrar_manual.jsf?employeeSelectedId=" + +getEmployeeSelected().getId());
        }
    }
    
    Journal journalAccess = null;
    Journal journalEgress = null;

    public Journal getJournalAccess() {
        return journalAccess;
    }

//    public void setJournalAccess(Journal journalAccess) {
//        
//        try {
//            this.journalAccess = addAccees(journalAccess);
//        } catch (IllegalAccessException | InvocationTargetException ex) {
//            java.util.logging.Logger.getLogger(JournalHome.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

    public Journal getJournalEgress() {
        return journalEgress;
    }

//    public void setJournalEgress(Journal journalEgress) {
//        try {
//            this.journalEgress = addEgress(journalEgress);
//        } catch (IllegalAccessException | InvocationTargetException ex) {
//            java.util.logging.Logger.getLogger(JournalHome.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
//    public Journal addAccees(Journal _journal) throws IllegalAccessException, InvocationTargetException{
//        return add(_journal, -240); //4 horas
//    }
//    
//    public Journal addEgress(Journal _journal) throws IllegalAccessException, InvocationTargetException{
//        return add(_journal, 240); //4 horas
//    }
    
//    public Journal add(Journal _journal,int minutes) throws IllegalAccessException, InvocationTargetException{
//        Journal journal_ = journalService.createInstance();
//        BeanUtils.copyProperties(journal_, _journal);
//        journal_.setId(null);
//        journal_.setBeginTime(Dates.addMinutes(journal_.getBeginTime(), minutes));
//        return journal_;
//    }
    
    public void put(){
        if (journalAccess != null){
            journalService.save(journalAccess);
            journalAccess = null;
        } else if (journalEgress != null){
            journalService.save(journalEgress);
            journalEgress = null;
        }
    }
    
    public void delete(Journal _journal) throws IOException {
        if (_journal.isPersistent()){
            journalService.remove(_journal.getId(), _journal);
        }
    }

    public Journal buildJournal(String name, Date dateTime, Subject _subject, Employee employee, String observacion) {
        Journal _journal = journalService.createInstance();

        _journal.setName(name);
        _journal.setBeginTime(dateTime); //tiempo igual al de inicio
        _journal.setEndTime(dateTime); //tiempo igual al de inicio
        _journal.setLastUpdate(Dates.now());
        _journal.setOwner(employee.getOwner());
        _journal.setAuthor(_subject); //el admin o usuario con privilegios
        _journal.setEmployeeId(employee.getId());
        _journal.setDescription(observacion + "\nIngresado por " + this.subject.getFullName());

        return _journal;
    }

    public void addRest() throws IOException {
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

        redirectTo("/pages/fede/talentohumano/registrar_manual.jsf?employeeSelectedId=" + +getEmployeeSelected().getId());
    }

    /**
     * Registrar registro de jornada.
     */
//    private void register() {
//        if (isCheckable()) {
//            getJournal().setName(calculeEvent(getEmployee()));
//            getJournal().setBeginTime(Dates.now());
//            getJournal().setEndTime(Dates.now());
//            getJournal().setEmployeeId(getEmployee().getId());
//            save();
//        } else {
//            addErrorMessage("Acaba de registrarse!", "Vuelva a intentar más tarde.");
//        }
//    }

//    public void check() throws IOException {
//        if (!Strings.isNullOrEmpty(getPassword()) && passwordService.passwordsMatch(getPassword(), getEmployee().getOwner().getPassword())) {
//            register();
//        } else {
//            addWarningMessage("La contraseña no es válida!", "Vuelva a intentar.");
//        }
//        redirectTo("/pages/fede/talentohumano/registrar.jsf?showCurrentDay=true");
//    }

//    public void quickCheck() throws IOException {
//
//        if (!Strings.isNullOrEmpty(getPassword())) {
//            //Cargar Empleado dado el código rápido
//            try {
//                Employee _employee = employeeService.find(Long.valueOf(getPassword()));
//                setEmployee(_employee);
//                setEmployeeId(_employee.getId());
//                register();
//            } catch (NumberFormatException nfe) {
//                addWarningMessage("El código es un número!", "Vuelva a intentar.");
//            }
//        } else {
//            addWarningMessage("Indique un código válido!", "Vuelva a intentar.");
//        }
//
//        redirectTo("/pages/fede/talentohumano/registrar_rapido.jsf?showCurrentDay=true&employeeId=" + getEmployeeId()); //volver a carga la vista para el usuario en registro
//    }

//    public boolean isCheckable() {
//
//        List<Journal> journals = journalService.findByNamedQueryWithLimit("Journal.findLastForOwner", 1, getEmployee().getOwner());
//
//        if (journals.isEmpty()) {
//            return true;
//        }
//
//        Journal lastJournal = journals.get(0);
//        int limit = Integer.parseInt(settingHome.getValue("app.fede.talentohumano.check.gap", "10"));
//        long diffTime = Dates.calculateNumberOfMinutesBetween(Dates.now(), lastJournal.getBeginTime());
//        return !(diffTime >= 0 && diffTime < limit); //
//    }

    public void checkRest() throws IOException {
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

    private String calculeEvent(Employee e) {
        String eventName = "REGISTRO";
//        if (e.getJournals().isEmpty()) {
//            eventName = "REGISTRO";
//        } else if (e.getJournals().size() % 2 != 0) {
//            eventName = "REGISTRO";
//        } else {
//            eventName = "REGISTRO";
//        }
        return eventName;
    }

    public void onRowSelect(SelectEvent event) {
        if (event != null && event.getObject() != null) {
            Employee e = (Employee) event.getObject();
        }
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

    @Override
    protected void initializeDateInterval() {
        int range = 0; //Rango de fechas para visualiar lista de entidades
        try {
            //range = Integer.valueOf(settingHome.getValue(SettingNames.JOURNAL_REPORT_DEFAULT_RANGE, "7"));
            if (isShowCurrentDay()) {
                range = 0; //El día de hoy
            } else {
                range = Dates.get(Dates.now(), Calendar.DAY_OF_MONTH) - 1;
            }
        } catch (java.lang.NumberFormatException nfe) {
            range = 7;
        }
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
    }

    public void updateCheckEndTime() {
        //dummy
    }

    /**
     * Aplica reglamento sobre la lista de registros
     * @return la lista de Journal modificadas segun el reglamento.
     */
//    public List<Journal> getJournalsRulesApplied() {
//
//        journalSummary = new JournalSummary();
//        
//        List<Journal> journals = getLazyDataModel().getResultList();
//        List<Journal> journalsReglamentarios = new ArrayList<>();
//
//        //Separar en grupos
//        Map<String, List<Journal>> journalMap 
//                = journals.stream().collect(Collectors.groupingBy(Journal::getDayHour));
//        
//        journalSummary.setTotalDays(Long.valueOf(journalMap.size())); //Total de dias agrupados
//        
//        //Contar días trabajados
//        java.util.Map<String, Long> daysCounting = journals.stream().collect(Collectors.groupingBy(Journal::getDayOfWeek, Collectors.counting()));
//        daysCounting.forEach((name, count) -> {
//           journalSummary.agregar(name, count);
//        });
//        
//        
//        //Recorrido para aplicar reglas y calculo
//        Date inicio = null;
//        Date fin = null;
//        int i = 1;
//        
//        for (String key : journalMap.keySet()) {
//            if (journalMap.get(key).size() % 2 == 0) {//Número par
//                //Corregir hora conforme a reglas de negocio
//                i = 1;
//                for (Journal j : journalMap.get(key)) {
//                    if (i % 2 != 0) {//entrada
//                        j.setName("ENTRADA");
//                        //Desde Diciembre en la mañana se ingresa a las 07h30
//                        if (Dates.get(j.getBeginTime(), Calendar.HOUR_OF_DAY) == 7){
//                            inicio = Dates.addMinutes(Dates.minimumDateHour(j.getBeginTime()), 30); //07H30
//                        } else {
//                            inicio = Dates.minimumDateHour(j.getBeginTime());
//                        }
//                        
//                        fin = Dates.addMinutes(inicio, 11);
//                        if (Dates.isInRange(inicio, fin, j.getBeginTime())) {
//                            j.setEndTime(j.getBeginTime()); //Entra dentro del rago de 27 minutos
//                        } else if (j.getBeginTime().before(inicio)) {
//                            j.setEndTime(inicio); //Entra a tiempo
//                        } else if (j.getBeginTime().after(fin)) {
//                            j.setEndTime(org.omnifaces.el.functions.Dates.addHours(inicio, 1)); //la siguiente hora
//                        }
//                    } else {//salida
//                        j.setName("SALIDA");
//                        inicio = Dates.minimumDateHour(j.getBeginTime());
//                        fin = Dates.addMinutes(inicio, 28);
//                        if (Dates.isInRange(inicio, fin, j.getBeginTime())) {
//                            j.setEndTime(inicio);
//                        } else if (j.getBeginTime().before(inicio)) {//salio antes
//                            j.setEndTime(j.getBeginTime());
//                        } else if (j.getBeginTime().after(fin)) {//salio más tarde
//                            j.setEndTime(j.getBeginTime()); //marcar el máximo
//                        }
//                    }
//                    i += 1;
//                }
//            } else {
//                //TODO implementar tratamiento de jornadas impares
//                //System.out.println(">>>>>>>>>>>>>> registros impares");
//            }
//            
//            journalsReglamentarios.addAll(journalMap.get(key)); //Agregar correcciones
//        }
//        
//        Collections.sort(journalsReglamentarios);
//        return journalsReglamentarios;
//    }
    
    public void onCellEdit(CellEditEvent event) {
        
        Date  oldValue = (Date ) event.getOldValue();
        Date  newValue = (Date ) event.getNewValue();
        
        try{
            if(newValue != null && !newValue.equals(oldValue)) {
                DataTable controladorTabla = (DataTable) event.getComponent();
                DataModel dm = (DataModel) controladorTabla.getValue();
                Journal _journal = (Journal) dm.getRowData();           
                _journal.setBeginTime(newValue);
                journalService.save(_journal.getId(), _journal); //Enviar a la base de datos
                addInfoMessage("¡Muy bien!", "Se actualizó el valor del registro con el valor + " + newValue.toString());
            }
            clear();
        }catch(NullPointerException ex){
            System.out.println(ex.getMessage());

        }
    }
    
    public void onMonthSelected(){
        Calendar inicio = Calendar.getInstance();
        inicio.setTime(monthSelected);
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        Calendar fin = Calendar.getInstance();
        fin.setTime(monthSelected);
        fin.set(Calendar.DAY_OF_MONTH, fin.getActualMaximum(Calendar.DAY_OF_MONTH));
        setStart(Dates.minimumDate(inicio.getTime()));
        setEnd(Dates.maximumDate(fin.getTime()));
        
        clear();
        System.out.println("start: " + getStart());
        System.out.println("end: " + getEnd());
    }
}
