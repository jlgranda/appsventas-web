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
package org.jlgranda.fede.controller.mail;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import net.tecnopro.document.ejb.TemplateService;
import net.tecnopro.document.model.Template;
import net.tecnopro.helper.mail.MailingHelper;
import net.tecnopro.helper.mail.VelocityHelper;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author jlgranda
 */
@ManagedBean
@SessionScoped
public class TemplateHome extends FedeController {
    
    @EJB
    private TemplateService templateService;
    
    @Inject
    private SettingHome settingHome;
    
    ///////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// MAIL UTILS ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Eniva un mensaje de correo para el <tt>Subject</tt> destinatario, usando la
     * plantilla <tt>templateId</tt> y los valores <tt>values</tt> para rellenar la plantilla.
     * @param subject
     * @param templateId
     * @param values
     * @return true si el mensaje se envio sin problemas, false en caso contrario
     */
    public boolean sendEmail(Subject subject, String templateId, Map<String, Object> values) {
        boolean valorRetorno = true;
        String host = settingHome.getValue("mail.imap.host", "jlgranda.com");
        String port = settingHome.getValue("mail.imap.host", "25");
        String from = settingHome.getValue("mail.imap.host", "consiguemas@jlgranda.com");
        String username = settingHome.getValue("mail.imap.host", "consiguemas@jlgranda.com");
        String password = settingHome.getValue("mail.imap.host", "c0ns1gu3m4s");
        String smtpAuth = settingHome.getValue("mail.imap.host", "true");
        String tlsEnable = settingHome.getValue("mail.imap.host", "true");

        Template template = templateService.findByCode(templateId);
        String title;
        String message;
        try {
            title = VelocityHelper.getRendererMessage(template.getTitle(), values);
            message = VelocityHelper.getRendererMessage(template.getBody(), values);
            valorRetorno = MailingHelper.enviar(host, port, username, password, smtpAuth,
                tlsEnable, from, subject.getEmail(), title,
                message);
        } catch (Exception ex) {
            Logger.getLogger(TemplateHome.class.getName()).log(Level.SEVERE, null, ex);
        }
        return valorRetorno;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
