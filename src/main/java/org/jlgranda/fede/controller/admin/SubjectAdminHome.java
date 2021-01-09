/*
 * Copyright (C) 2016 Jorge
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
package org.jlgranda.fede.controller.admin;

import com.google.common.base.Strings;
import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.SettingService;
import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.shiro.UsersRoles;
import com.jlgranda.shiro.UsersRolesPK;
import com.jlgranda.shiro.ejb.UsersRolesFacade;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.GroupHome;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.controller.SubjectHome;
import org.jlgranda.fede.ui.model.LazySubjectDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Lists;
import org.jpapi.util.StringValidations;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
@ViewScoped
@Named
public class SubjectAdminHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 2914718473249636007L;

    Logger logger = LoggerFactory.getLogger(SubjectAdminHome.class);

    private Long subjectId;

    @Inject
    private Subject subject;

    private Subject subjectEdit;

    @Inject
    private SettingHome settingHome;
    @Inject
    GroupHome groupHome;

    @EJB
    private GroupService groupService;
    @EJB
    SubjectService subjectService;
    @EJB
    SettingService settingService;
    private List<org.jpapi.model.Group> grupos;

    private LazySubjectDataModel lazyDataModel;

    @Inject
    private SubjectHome subjectHome;
    private String confirmarClave;
    private String clave;
    private boolean cambiarClave;
    
    @EJB
    UsersRolesFacade usersRolesFacade;
    
    PasswordService svc = new DefaultPasswordService();

    public SubjectAdminHome() {
        this.grupos = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }
        this.cambiarClave = false;
        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));
        setOutcome("subjects");
        setSubjectEdit(subjectService.createInstance()); //Siempre listo para recibir la petición de creación
    }

    @Override
    public List<org.jpapi.model.Group> getGroups() {
        if (groups.isEmpty()) {
            groups = groupService.findByOwnerAndModuleAndType(subject, "admin", org.jpapi.model.Group.Type.LABEL);
        }

        return groups;
    }

    @Override
    public void setGroups(List<org.jpapi.model.Group> groups) {
        this.groups = groups;
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazySubjectDataModel(subjectService);
        }
//        lazyDataModel.setOwner(subject); //Buscar todos los sujetos
        lazyDataModel.setOwner(null); //Buscar todos los sujetos
//        lazyDataModel.setStart(getStart());
        //lazyDataModel.setEnd(getEnd());

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

    public LazySubjectDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void mostrarFormularioCambiarClave() {
        this.cambiarClave = true;
    }

    public void handlePhotoUpload(FileUploadEvent event) {
        this.subjectEdit.setPhoto(event.getFile().getContents());
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getSessionMap().put("photoUser", this.subjectEdit.getPhoto());
        addSuccessMessage(I18nUtil.getMessages("subject.upload.photo"), I18nUtil.getMessages("subject.upload.photo"));
    }

    public void setLazyDataModel(LazySubjectDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {

                //TODO leer desde configuración el url destino
                redirectTo("/pages/fede/admin/subject/profile.jsf?subjectId=" + ((BussinesEntity) event.getObject()).getId());
//                redirectTo("/pages/fede/admin/subject/profile_summary.jsf?subjectId=" + ((BussinesEntity) event.getObject()).getId());
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    /**
     * Registrar personas en el sistema
     * @return 
     */
    @Deprecated
    public String validateAndSave() {
        //Realizar signup
        try {
            if (!getSubjectEdit().isPersistent() 
                    || (getSubjectEdit().isPersistent() && !getSubjectEdit().isConfirmed())) {
                
                if (!StringValidations.isPassword(clave)) {
                    addErrorMessage(I18nUtil.getMessages("passwordInvalidMsg"), I18nUtil.getMessages("passwordInvalidLengthMsg", "7"));
                    setOutcome("signin");
                    return getOutcome();
                }
//                if (!this.clave.equals(this.confirmarClave)) {
//                    addErrorMessage(I18nUtil.getMessages("passwordsDontMatch"), I18nUtil.getMessages("passwordsDontMatch"));
//                    return "";
//                }
                return save();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return getOutcome();
    }
    
    public void validate() {
        setValidated(true);
        int length = Integer.valueOf(settingHome.getValue("app.security.password.length", "5"));
        if (!StringValidations.isPassword(getClave(), length)) {
            addErrorMessage(I18nUtil.getMessages("passwordInvalidMsg"), I18nUtil.getMessages("passwordInvalidLengthMsg", ""+length));
            setValidated(false);
        }
    }

    public String saveValidado(){
        //La validación se hace en la vista
        return save(true);
    }
    
    public String save() {
        
        return save(isValidated());
    }
    
    public String save(boolean _validated) {
        
        if (_validated){
            return save("USER");
        } else {
            setOutcome(""); //quedarse en el mismo lugar
            addErrorMessage(I18nUtil.getMessages("validation.general"), I18nUtil.getMessages("validation.general.detail"));
            return ""; 
        }
    }

    public String save(String role) {
        //Realizar signup
        try {

            if (!getSubjectEdit().isPersistent()) {
                if (subject == null) {
                    subject = subjectService.find(9L); //ID del usuario ADMIN
                }
                boolean setupRoles = "admin".equalsIgnoreCase(subject.getUsername());
                if (Strings.isNullOrEmpty(getSubjectEdit().getPassword())){ //Preguntar si no se estableció desde fuera
                    getSubjectEdit().setPassword(getClave()); //Establece la clave desde la vista
                }
                subjectHome.processSignup(getSubjectEdit(), subject, role, setupRoles); //El propietario es el administrador actual y se asigna con un rol
                addDefaultSuccessMessage();

            } else if (getSubjectEdit().isPersistent() && !getSubjectEdit().isConfirmed()){
                processSigupForExistentSubject(getSubjectEdit());
                addDefaultSuccessMessage();
            } else {
                //Solo actualizar
                if (Strings.isNullOrEmpty(getSubjectEdit().getPassword())){ //Preguntar si no se estableció desde fuera
                    getSubjectEdit().setPassword("UNSET"); //Establece la clave desde la vista
                }
                subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
                addDefaultSuccessMessage();
            }
            
            setOutcome("home");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            addErrorMessage(I18nUtil.getMessages("error.persistence"), e.getMessage());
            setOutcome("failed");
        }

        return getOutcome();
    }
    
    public void update() {
        //Solo actualizar
        subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
        addDefaultSuccessMessage();
    }
    
    /**
     * Procesa la creación de una cuenta en fede para el usuario dado
     * @param _signup el objeto <tt>Subject</tt> a agregar
     */
    public void processSigupForExistentSubject(Subject _signup) {

//
        if (_signup != null) {
            //Crear la identidad para acceso al sistema
            try {
                //crypt password
                //Solo actualizar
                if (Strings.isNullOrEmpty(getClave())){ //Preguntar si no se estableció desde fuera
                    setClave("UNSET"); //Establece la clave desde la vista
                }
                _signup.setPassword(svc.encryptPassword(getClave()));
                _signup.setConfirmed(true);
                //actualizar directamente
                subjectService.save(_signup.getId(), _signup);
                if (true){
                    //Asignar roles
                    UsersRoles shiroUsersRoles = new UsersRoles();
                    UsersRolesPK usersRolesPK = new UsersRolesPK(_signup.getUsername(), "USER");
                    shiroUsersRoles.setUsersRolesPK(usersRolesPK);
                    if (null == usersRolesFacade.find(usersRolesPK)){
                        usersRolesFacade.create(shiroUsersRoles);
                    }
                    
                }
            } catch (SecurityException | IllegalStateException e) {
                throw new RuntimeException("Could not create default security entities.", e);
            }
        }

    }

    public void confirm(boolean force) {
        if (force) {
            getSubjectEdit().setConfirmed(false);
            subjectService.save(getSubjectEdit().getId(), getSubjectEdit());
        }

        if (!getSubjectEdit().isConfirmed()) {
            subjectHome.sendConfirmation(getSubjectEdit());
        } else {
            addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.subject.confirmed"));
        }
    }

    /**
     * El método debe actualizar en picketlink, de otra manera no tiene efecto
     * el cambio de clave.
     */
    public void changePassword() {
        if (getClave().equalsIgnoreCase(getConfirmarClave())) {
            getSubjectEdit().setPassword(getClave());
            getSubjectEdit().setLastUpdate(Dates.now());
            subjectHome.processChangePassword(getSubjectEdit());
            this.addSuccessMessage("La contraseña se actualizó correctamente.", "");
        } else {
            this.addWarningMessage("Las contraseñas no coinciden! Intente nuevamente.", "");
        }

    }
    
    /**
     * Probar si el código ingresado por el usario pertenece aun usuario inactivo o registrado por el operador
     * Si el usuario existe, cargar los datos y dejar listo para confirmar.
     */
    public void tryForExistentSubject(){
        String code = getSubjectEdit().getCode();
        Subject _subject = subjectService.findUniqueByNamedQuery("Subject.findByCode", code);
        if (null != _subject && !_subject.isConfirmed()){
            setSubjectId(_subject.getId()); //alista para cargar el objeto desde el home
            setSubjectEdit(subjectService.createInstance()); //Cargar en memoria para edición
            getSubjectEdit().setDescription(getSubjectEdit().getFullName());
            addWarningMessage("Hola " + _subject.getFirstname(), "¡Bienvenido de nuevo, actualice sus datos y consiga más en dolar directo!");
        } else if (null != _subject && _subject.isConfirmed()){
            setSubjectEdit(subjectService.createInstance());
            addWarningMessage("Hola " + _subject.getFirstname(), "¡Prueba iniciar una sesión y conseguir más en dolar directo!");
        } 
        
    }

    public Subject getSubjectEdit() {
        if (subjectId != null && !this.subjectEdit.isPersistent()) {
            this.subjectEdit = subjectService.find(subjectId);
            if (subjectEdit.getPhoto() != null) {
                FacesContext context = FacesContext.getCurrentInstance();
                context.getExternalContext().getSessionMap().put("photoUser", this.subjectEdit.getPhoto());
            }
        }
        return subjectEdit;
    }

    public void setSubjectEdit(Subject subjectEdit) {
        this.subjectEdit = subjectEdit;
    }

    public void applySelectedGroups() {
        String status = "";
        Group group = null;
        Set<String> addedGroups = new LinkedHashSet<>();
        for (BussinesEntity fe : getSelectedBussinesEntities()) {
            for (String key : selectedTriStateGroups.keySet()) {
                group = findGroup(key);
                status = selectedTriStateGroups.get(key);
                if ("0".equalsIgnoreCase(status)) {
                    if (fe.containsGroup(key)) {
                        fe.remove(group);
                    }
                } else if ("1".equalsIgnoreCase(status)) {
                    if (!fe.containsGroup(key)) {
                        fe.add(group);
                        addedGroups.add(group.getName());
                    }
                } else if ("2".equalsIgnoreCase(status)) {
                    if (!fe.containsGroup(key)) {
                        fe.add(group);
                        addedGroups.add(group.getName());
                    }
                }
            }
            subjectService.save(fe.getId(), (Subject) fe);
        }

        this.addSuccessMessage("Las facturas se agregaron a " + Lists.toString(addedGroups), "");
    }

    private Group findGroup(String key) {
        for (Group g : getGroups()) {
            if (key.equalsIgnoreCase(g.getCode())) {
                return g;
            }
        }
        return new Group("null", "null");
    }

    public void mostrarAsignarGruposUsuarios() {
        try {
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
            List<Subject> subjects = new ArrayList<>();
            getSelectedBussinesEntities().stream().map((entity) -> (Subject) entity).forEach((s) -> {
                subjects.add(s);
            });

            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(I18nUtil.getMessages("subject.selected"), getSelectedBussinesEntities());
            redirectTo("/pages/admin/subject/subjects_group.jsf");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public String getConfirmarClave() {
        return confirmarClave;
    }

    public void setConfirmarClave(String confirmarClave) {
        this.confirmarClave = confirmarClave;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public boolean isCambiarClave() {
        return cambiarClave;
    }

    public void setCambiarClave(boolean cambiarClave) {
        this.cambiarClave = cambiarClave;
    }

    public List<Group> getGrupos() {
        return grupos;
    }

    public void setGrupos(List<Group> grupos) {
        this.grupos = grupos;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
