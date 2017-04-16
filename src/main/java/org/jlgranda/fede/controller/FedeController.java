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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.jlgranda.fede.controller.admin.TemplateHome;
import org.jlgranda.fede.model.sales.Invoice;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
public abstract class FedeController {

    Logger logger = LoggerFactory.getLogger(FedeController.class);


    public static final String KEY_SEPARATOR = ",";

    String outcome = "home";
    
    private String tags;

    private String keyword;

    /**
     * Inicio del rango de fecha
     */
    private Date start;

    /**
     * Fin del rango de fecha
     */
    private Date end;
    
    
    protected Group defaultGroup = null;

    protected List<BussinesEntity> selectedBussinesEntities;

    protected Map<String, String> selectedTriStateGroups = new LinkedHashMap<>();
    
    protected List<Group> groups = new ArrayList<>();

    public List<BussinesEntity> getSelectedBussinesEntities() {
        return selectedBussinesEntities;
    }

    public void setSelectedBussinesEntities(List<BussinesEntity> selectedBussinesEntities) {
        this.selectedBussinesEntities = selectedBussinesEntities;
    }

    public Map<String, String> getSelectedTriStateGroups() {
        return selectedTriStateGroups;
    }

    public void setSelectedTriStateGroups(Map<String, String> selectedTriStateGroups) {
        this.selectedTriStateGroups = selectedTriStateGroups;
    }

    public String redirect(){
        return getOutcome();
    }
    /**
     * Gets the http servlet request.
     *
     * @return the http servlet request
     */
    public HttpServletRequest getHttpServletRequest() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        return request;
    }

    /**
     * Gets the http session.
     *
     * @return the http session
     */
    public HttpSession getHttpSession() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
        return session;
    }

    //Seccion Mensages de Informacion
    /**
     * Adds the info message.
     *
     * @param msg the msg
     * @param submensaje the submensaje
     */
    public void addInfoMessage(String msg, String submensaje) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, submensaje);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    //Seccion Mensajes de Exito
    /**
     * Adds the success message.
     *
     * @param msg the msg
     * @param submensaje the submensaje
     */
    public void addSuccessMessage(String msg, String submensaje) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, submensaje);
        FacesContext.getCurrentInstance().addMessage("successInfo", facesMsg);
    }

    //Seccion Mensajes de Advertencia
    /**
     * Adds the warning message.
     *
     * @param msg the msg
     * @param submensaje the submensaje
     */
    public void addWarningMessage(String msg, String submensaje) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, submensaje);
        FacesContext.getCurrentInstance().addMessage("successInfo", facesMsg);
    }

    //Seccion Mensajes Error
    /**
     * Adds the error message.
     *
     * @param ex the ex
     * @param defaultMsg the default msg
     */
    public void addErrorMessage(Exception ex, String defaultMsg) {
        String msg = ex.getLocalizedMessage();

        if ((msg != null) && (msg.length() > 0)) {
            addErrorMessage(msg, "");
        } else {
            addErrorMessage(defaultMsg, "");
        }
    }

    /**
     * Adds the error messages.
     *
     * @param messages the messages
     */
    public void addErrorMessages(List<String> messages) {

        for (String message : messages) {
            addErrorMessage(message, "");
        }
    }

    /**
     * Adds the error message.
     *
     * @param msg the msg
     * @param submensaje the submensaje
     */
    public void addErrorMessage(String msg, String submensaje) {

        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, submensaje);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    /**
     * Adds the error message.
     *
     * @param context the context
     * @param msg the msg
     * @param submensaje the submensaje
     */
    public void addErrorMessage(FacesContext context, String msg, String submensaje) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, submensaje);
        context.addMessage(null, facesMsg);
    }

    /**
     * Gets the request parameter.
     *
     * @param key the key
     * @return the request parameter
     */
    public String getRequestParameter(String key) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(key);
    }

    /**
     * Gets the object from request parameter.
     *
     * @param requestParameterName the request parameter name
     * @param converter the converter
     * @param component the component
     * @return the object from request parameter
     */
    public Object getObjectFromRequestParameter(String requestParameterName, Converter converter, UIComponent component) {
        String theId = this.getRequestParameter(requestParameterName);

        return converter.getAsObject(FacesContext.getCurrentInstance(), component, theId);
    }

    ////////////////////////////////////////////////////////////////////////
    // Popups general management
    ////////////////////////////////////////////////////////////////////////
    /**
     * Abre la ventana emergente indicada por popupName con el ancho y alto
     * especificado
     *
     * @param name nombre de la ventana emergente
     * @param width ancho de la ventana emergente
     * @param height alto de la ventana emergente
     * @param modal indica si la ventana emergente debe ser modal o no
     */
    protected void openDialog(String name, int width, int height, boolean modal) {
        openDialog(name, width, height, modal, null);
    }
    
    /**
     * Abre la ventana emergente indicada por popupName con el ancho y alto
     * especificado
     *
     * @param name nombre de la ventana emergente
     * @param width ancho de la ventana emergente
     * @param height alto de la ventana emergente
     * @param modal indica si la ventana emergente debe ser modal o no
     */
    protected void openDialog(String name, int width, int height, boolean modal, Map<String, List<String>> params) {
        Map<String, Object> options = new HashMap<>();
        options.put("modal", modal);
        options.put("draggable", false);
        options.put("resizable", true);
        options.put("contentWidth", width);
        options.put("contentHeight", height);
        options.put("closable", true);
        //options.put("includeViewParams", false);

//        Map<String, List<String>> params = new HashMap<String, List<String>>();
//        List<String> values = new ArrayList<String>();
//        values.add(bookName);
//        params.put("bookName", values);
        RequestContext.getCurrentInstance().openDialog(name, options, params);
        //logger.info("Popup '{}' abierto, con opciones {}. Context: {}", name, options, RequestContext.getCurrentInstance());
    }

    /**
     * Abre la ventana emergente indicada por popupName con el ancho y alto
     * especificado
     *
     * @param name nombre de la ventana emergente
     * @param width ancho de la ventana emergente
     * @param height alto de la ventana emergente
     * @param modal indica si la ventana emergente debe ser modal o no
     * @param draggable
     * @param resizable
     * @param closable
     * @param params
     */
    protected void openDialog(String name, String width, String height, String left, String top, boolean modal, boolean draggable, boolean resizable, boolean closable, Map<String, List<String>> params) {
        Map<String, Object> options = new HashMap<>();
        options.put("modal", modal);
        options.put("draggable", draggable);
        options.put("resizable", resizable);
        options.put("left", left);
        options.put("top", top);
        options.put("contentWidth", width);
        options.put("contentHeight", height);
        options.put("closable", closable);
        //options.put("includeViewParams", false);

        RequestContext.getCurrentInstance().openDialog(name, options, params);
        //logger.info("Popup '{}' abierto, con opciones {}. Context: {}", name, options, RequestContext.getCurrentInstance());
    }
    
    
    /**
     * Abre la ventana emergente indicada por popupName con el ancho y alto
     * especificado
     *
     * @param name nombre de la ventana emergente
     * @param width ancho de la ventana emergente
     * @param height alto de la ventana emergente
     * @param modal indica si la ventana emergente debe ser modal o no
     * @param params
     */
    protected void openDialog(String name, String width, String height, String left, String top, boolean modal, Map<String, List<String>> params) {
        openDialog(name, width, height, left, top, modal, true, true, true, null);
    }
    
    /**
     * Abre la ventana emergente indicada por popupName con el ancho y alto
     * especificado
     *
     * @param name nombre de la ventana emergente
     * @param width ancho de la ventana emergente
     * @param height alto de la ventana emergente
     * @param modal indica si la ventana emergente debe ser modal o no
     * @param params
     */
    protected void openDialog(String name, String width, String height, boolean modal, Map<String, List<String>> params) {
        openDialog(name, width, height, "0", "0", modal, true, true, true, null);
    }
    
    /**
     * Abre la ventana emergente indicada por popupName con el ancho y alto
     * especificado
     *
     * @param name nombre de la ventana emergente
     * @param width ancho de la ventana emergente
     * @param height alto de la ventana emergente
     * @param modal indica si la ventana emergente debe ser modal o no
     */
    protected void openDialog(String name, String width, String height, boolean modal) {
        openDialog(name, width, height, modal, null);
    }

    public void closeDialog(Object data) {
        RequestContext.getCurrentInstance().closeDialog(data);
        //logger.info("Popup '{}' cerrado, con data {}. Context: {}", "activo", data, RequestContext.getCurrentInstance());
    }

    public void redirectTo(String url) throws IOException {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + url);
    }

    public abstract void handleReturn(SelectEvent event);

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getTags() {
        
        if (this.tags == null || tags.isEmpty()) {
            setTags(getDefaultGroup() != null ? getDefaultGroup().getCode() :  null);
        }
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public void setDefaultGroup(Group defaultGroup) {
        this.defaultGroup = defaultGroup;
    }
    
    public abstract Group getDefaultGroup();

    public void addDefaultSuccessMessage() {
        addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("action.sucessfully.detail"));
    }
    
    public void addDefaultErrorMessage() {
        addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("action.fail.detail"));
    }
    
    public void addDefaultWarningMessage() {
        addWarningMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("action.warning.detail"));
    }
    
    public abstract List<Group> getGroups();

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
    
    //Invalidate the session and send a redirect to index.html
    public void logout() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
        session.invalidate();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.sendRedirect("index.html");
        facesContext.responseComplete();
    }
    
    
    //This method return the stack trace string from the Exception
    public String getStackTrace() {
        Throwable throwable = (Throwable)  FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get("javax.servlet.error.exception");
        StringBuilder builder = new StringBuilder();
        builder.append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            builder.append(element).append("\n");
        }
        return builder.toString();
    }
    
    public void sendNotification(TemplateHome templateHome, SettingHome settingHome, Subject subject, Map<String, Object> values, String templateName, boolean displayMessage) {
        if (templateHome != null) {

            if (templateHome.sendEmail(subject, settingHome.getValue(templateName, templateName), values)){
                if (displayMessage) addDefaultSuccessMessage();
            } else {
                if (displayMessage) addDefaultErrorMessage();
            }
        }
    }
}
