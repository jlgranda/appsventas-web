/*
 * Copyright (C) 2016 jlgranda
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

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.mailing.MessageService;
import com.jlgranda.fede.ejb.mailing.NotificationService;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import net.tecnopro.document.ejb.TemplateService;
import net.tecnopro.document.model.Template;
import net.tecnopro.helper.mail.VelocityHelper;
import net.tecnopro.mailing.Message;
import net.tecnopro.mailing.Notification;
import org.apache.commons.text.StringEscapeUtils;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.document.FacturaElectronica;
import org.jlgranda.fede.ui.model.LazyTemplateDataModel;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
@Named(value = "templateHome")
@ViewScoped
public class TemplateHome extends FedeController implements Serializable {

    private static final long serialVersionUID = 3052478735706172968L;

    Logger logger = LoggerFactory.getLogger(TemplateHome.class);

    @Inject
    private Subject subject;

    @EJB
    private GroupService groupService;
    
    @EJB
    private NotificationService notificationService;
    
    @EJB
    private MessageService messageService;

    @EJB
    private TemplateService templateService;

    @Inject
    private SettingHome settingHome;

    private LazyTemplateDataModel lazyDataModel;

    private Template template;
    
    private Long templateId;

    @PostConstruct
    public void init() {
        
         setStart(null); //Marcar para ignorar rango de fechas
        setOutcome("admin-template");

        setTemplate(templateService.createInstance()); //Siempre listo para recibir la petición de creación
        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Template getTemplate() {
        if (templateId != null && !this.template.isPersistent()) {
            this.template = templateService.find(templateId);
        }
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public LazyTemplateDataModel getLazyDataModel() {
        filter();
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyTemplateDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
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
            lazyDataModel = new LazyTemplateDataModel(templateService);
        }

        lazyDataModel.setOwner(subject);
        lazyDataModel.setStart(getStart());
        lazyDataModel.setEnd(getEnd());

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
    
    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                redirectTo("/pages/admin/template/template.jsf?templateId=" + ((BussinesEntity) event.getObject()).getId());
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        FacesMessage msg = new FacesMessage(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());

        FacesContext.getCurrentInstance().addMessage(null, msg);
        this.selectedBussinesEntities.remove((FacturaElectronica) event.getObject());
        logger.info(I18nUtil.getMessages("BussinesEntity") + " " + I18nUtil.getMessages("common.unselected"), ((BussinesEntity) event.getObject()).getName());
    }
    
    /**
     * Guardar template en edición
     */
    public void save() {
        if (!template.isPersistent()){
            template.setUuid(java.util.UUID.randomUUID().toString());
            template.setAuthor(subject);
            template.setOwner(subject);
            templateService.save(template);
        } else {
            templateService.save(template.getId(), template);
        }
        
        this.addDefaultSuccessMessage();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////////// MAILING subsytem /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Envia un mensaje de correo para el <tt>Subject</tt> destinatario, usando
     * la plantilla <tt>templateId</tt> y los valores <tt>values</tt> para
     * rellenar la plantilla.
     *
     * @param subject
     * @param templateId
     * @param values
     * @return true si el mensaje se envio sin problemas, false en caso
     * contrario
     */
    public boolean sendEmail(Subject subject, String templateId, Map<String, Object> values) {
        boolean valorRetorno = true;

        template = templateService.findByCode(templateId);

        if (template == null) {//Nada que hacer!
            valorRetorno = false;
            logger.warn(I18nUtil.getMessages("app.mail.template.404", templateId));
        } else {

            String _from = settingHome.getValue("mail.smtps.from", "AppsVentas Plataforma <notificacion@jlgranda.com>");
            String title;
            String body;
            String txt;
            try {
                title = VelocityHelper.getRendererMessage(template.getTitle(), values);
                body = VelocityHelper.getRendererMessage(template.getBody(), values);
                txt = VelocityHelper.getRendererMessage(template.getBody(), values);
                Notification notification = notificationService.createInstance();
                Message message = messageService.createInstance();
                message.setSubject(title);
                message.setHtml(body);
                message.setText(txt);
                messageService.save(message);
                
                notification.setFfrom(_from);
                notification.setTto(subject.getEmailAddress());
                notification.setMessageId(message.getId());
                
                notificationService.save(notification);
                
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return valorRetorno;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
