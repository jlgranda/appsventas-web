/*
 * Copyright (C) 2015 jlgranda
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jlgranda.fede.controller;

import com.jlgranda.fede.ejb.SubjectService;
import com.jlgranda.shiro.UsersRoles;
import com.jlgranda.shiro.UsersRolesPK;
import com.jlgranda.shiro.ejb.UsersRolesFacade;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.UserTransaction;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.jlgranda.fede.controller.admin.TemplateHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jpapi.model.CodeType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.QueryData;
import org.jpapi.util.QuerySortOrder;
import org.jpapi.util.Strings;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador de entidades Subject: signup, profile
 *
 * @author jlgranda
 */
@Named
@ViewScoped
public class SubjectHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(SubjectHome.class);

    @Inject
    Subject subject; //La instancia Subject de la sessión activa
    
    
    Subject signup = null; //El objeto para edición

    @EJB
    SubjectService subjectService;
    
    @EJB
    UsersRolesFacade usersRolesFacade;

    @Inject
    private SettingHome settingHome;

    @Inject
    GroupHome groupHome;

    @Resource
    private UserTransaction userTransaction; //https://issues.jboss.org/browse/PLINK-332
    
    @Inject
    private TemplateHome templateHome;
    
    PasswordService svc = new DefaultPasswordService();
    
    private boolean handledPhotoUpload;
    
    private byte[] photo;
    
    private String clave;
    private String confirmarClave;
    
    @PostConstruct
    public void init() {
        setHandledPhotoUpload(false);
    }

    public boolean isLoggedIn() {
        return this.signup != null && this.signup.getId() != null;
    }

    public void save(){
        if (isHandledPhotoUpload()){
            getSignup().setPhoto(getPhoto()); //Guardar la foto cargada
        }
        save(getSignup());
    }
    /**
     * Guardar la instancia <tt>Subject</tt> en el medio de almacenamiento persistente
     * @param _subject la instancia a guardar
     */
    public void save(Subject _subject) {
        subjectService.save(_subject.getId(), _subject); 
        addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully.detail"));
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Subject getSignup() {
        if (signup == null) {
            signup = subjectService.find(subject.getId()); //Obtener una copia del objeto en sessión
        }
        return signup;
    }

    public void setSignup(Subject signup) {
        this.signup = signup;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public boolean isHandledPhotoUpload() {
        return handledPhotoUpload;
    }

    public void setHandledPhotoUpload(boolean handledPhotoUpload) {
        this.handledPhotoUpload = handledPhotoUpload;
    }

    /**
     * Procesa la creación de una cuenta en fede para el usuario dado
     * @param _signup el objeto <tt>Subject</tt> a agregar
     * @param owner el propietario del objeto a agregar
     */
    public void processSignup(Subject _signup, Subject owner) {
        processSignup(_signup, owner, "USER", true);
    }
    
    /**
     * Procesa la creación de una cuenta en fede para el usuario dado
     * @param _signup el objeto <tt>Subject</tt> a agregar
     * @param owner el propietario del objeto a agregar
     */
    public void processSignupEmployee(Subject _signup, Subject owner) {
        
        processSignup(_signup, owner, "EMPLOYEE", true);
    }
    
    /**
     * Procesa la creación de una cuenta en fede para el usuario dado
     * @param _signup el objeto <tt>Subject</tt> a agregar
     * @param owner el propietario del objeto a agregar
     * @param roles
     */
    public void processSignup(Subject _signup, Subject owner, String roles, boolean setupRoles) {
        if (_signup != null) {
            //Crear la identidad para acceso al sistema
            try {

                //separar nombres
                if (Strings.isNullOrEmpty(_signup.getSurname()) 
                        && !Strings.isNullOrEmpty(_signup.getFirstname())){
                   
                    List<String> names = Strings.splitNamesAt(_signup.getFirstname());

                    if (names.size() > 1) {
                        _signup.setFirstname(names.get(0));
                        _signup.setSurname(names.get(1));
                    }
                } else if (!Strings.isNullOrEmpty(_signup.getDescription()) 
                        && Strings.isNullOrEmpty(_signup.getFirstname())
                        && Strings.isNullOrEmpty(_signup.getSurname())){
                    
                    List<String> names = Strings.splitNamesAt(_signup.getDescription());

                    switch (names.size()) {
                        case 1:
                            _signup.setFirstname(names.get(0));
                            _signup.setSurname("");
                            break;
                        case 2:
                            _signup.setFirstname(names.get(0));
                            _signup.setSurname(names.get(1));
                            break;
                        case 3:
                            _signup.setFirstname(names.get(0) + names.get(1));
                            _signup.setSurname(names.get(2));
                            break;
                        case 4: 
                            _signup.setFirstname(names.get(0) + names.get(1));
                            _signup.setSurname(names.get(2)  + names.get(3));
                            break;
                        default:
                            break;
                    }
                }
                //Más valores de autenticación
                _signup.setUsername(_signup.getEmail());
                _signup.setCodeType(CodeType.CEDULA);
                _signup.setUsernameConfirmed(true);

                //Set fede email
                _signup.setFedeEmail(_signup.getCode().concat("@").concat(settingHome.getValue("mail.imap.host", "localhost")));
                _signup.setFedeEmailPassword(_signup.getPassword());
//
                //Finalmente crear en fede
                _signup.setUuid(UUID.randomUUID().toString());
                _signup.setSubjectType(Subject.Type.NATURAL);
                
                //No cambiar owner y author si ya es persistente
                if (!_signup.isPersistent()){
                    _signup.setOwner(owner);
                    _signup.setAuthor(owner);
                }
                
                _signup.setActive(true);
                
                //crypt password
                _signup.setPassword(svc.encryptPassword(_signup.getPassword()));
                
                subjectService.save(_signup);
                if (setupRoles){
                    //Asignar roles
                    UsersRoles shiroUsersRoles = new UsersRoles();
                    UsersRolesPK usersRolesPK = new UsersRolesPK(_signup.getUsername(), roles);
                    shiroUsersRoles.setUsersRolesPK(usersRolesPK);
                    usersRolesFacade.create(shiroUsersRoles);
                }
                

            } catch (SecurityException | IllegalStateException e) {
                throw new RuntimeException("Could not create default security entities.", e);
            }
        }

    }
    
    /**
     * Procesa la creación de una cuenta en fede para el usuario dado
     * @param _signup el objeto <tt>Subject</tt> a agregar
     * @throws java.io.IOException
     */
    public void processChangePassword(Subject _signup) throws IOException {

//
        if (_signup != null) {
            //Crear la identidad para acceso al sistema
            try {
                //crypt password
                _signup.setPassword(svc.encryptPassword(_signup.getPassword()));
                
                subjectService.save(_signup.getId(), _signup);
                
                //Cerrar sessión
                redirectTo("/logout");

            } catch (SecurityException | IllegalStateException e) {
                throw new RuntimeException("Could not create default security entities.", e);
            }
        }

    }
    
    /**
     * Procesa la creación de una cuenta en fede, 
     * se asigna admin como propietario
     */
    public void processSignup() {
        Subject admin = subjectService.findUniqueByNamedQuery("Subject.findUserByLogin", "admin");
        processSignup(this.signup, admin);
        sendConfirmation(this.signup);
        
    }
    
    public void sendConfirmation(Subject _subject) {
        if (_subject.isPersistent() && !_subject.isConfirmed()) {
            //Notificar alta en appsventas
            String confirm_url = settingHome.getValue("app.login.confirm.url", "http://emporiolojano.com:8080/appsventas/confirm.jsf?uuid=");
            Map<String, Object> values = new HashMap<>();
            
            //TODO implementar una forma de definición de parametros desde coniguración
            values.put("fullname", _subject.getFullName());
            values.put("url", confirm_url + _subject.getUuid());

            if (templateHome.sendEmail(_subject, settingHome.getValue("app.mail.template.signin", "app.mail.template.signin"), values)){
                addDefaultSuccessMessage();
            } else {
                addDefaultErrorMessage();
            }
        }
    }

    /**
     * Busca objetos <tt>Subject</tt> para la clave de búsqueda en las columnas
     * usernae, firstname, surname
     * @param _keyword
     * @return una lista de objetos <tt>Subject</tt> que coinciden con la palabra clave dada.
     */
    public List<Subject> find(String _keyword) {
        _keyword = _keyword.trim();
        Map<String, Object> filters = new HashMap<>();
        Map<String, String> columns = new HashMap<>();
        columns.put("username", _keyword);
        columns.put("code", _keyword);
        columns.put("firstname", _keyword);
        columns.put("surname", _keyword);
        filters.put("dummy", columns);
        QueryData<Subject> queryData = subjectService.find(-1, -1, "surname, firstname", QuerySortOrder.ASC, filters);
        return queryData.getResult();
    }

    @Override
    public org.jpapi.model.Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<org.jpapi.model.Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void handlePhotoUpload(FileUploadEvent event) {
        setPhoto(event.getFile().getContent());
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getSessionMap().put("photoUser", getPhoto()); //Para cargar desde memoria
        addSuccessMessage(I18nUtil.getMessages("subject.upload.photo"), I18nUtil.getMessages("subject.upload.photo"));
        setHandledPhotoUpload(true);
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getConfirmarClave() {
        return confirmarClave;
    }

    public void setConfirmarClave(String confirmarClave) {
        this.confirmarClave = confirmarClave;
    }
    
    /**
     * El método debe actualizar en picketlink, de otra manera no tiene efecto
     * el cambio de clave.
     * @throws java.io.IOException
     */
    public void changePassword() throws IOException {
        if (getClave().equalsIgnoreCase(getConfirmarClave())) {
            this.signup.setPassword(getClave());
            this.signup.setLastUpdate(Dates.now());
            this.processChangePassword(this.signup);
            this.addSuccessMessage("La contraseña se actualizó correctamente.", "");
        } else {
            this.addWarningMessage("Las contraseñas no coinciden! Intente nuevamente.", "");
        }

    }
    
    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
