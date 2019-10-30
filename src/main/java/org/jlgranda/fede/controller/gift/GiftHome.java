/*
 * Copyright (C) 2019 jlgrandadev
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
package org.jlgranda.fede.controller.gift;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.controller.FedeController;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Ronny Ramirez
 */
@Named
@RequestScoped
public class GiftHome extends FedeController implements Serializable{

    private static final long serialVersionUID = 1L;
    
    String uuid;
    
    @Inject
    private Subject subject;
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    @PostConstruct
    private void init() {
        setOutcome("inicio");
    }
    
    /**
     * Executa el Servlet de generación de imagenes de cupón
     */
    public void print(){

        try{
            Long id = subject.getId();
            String code = subject.getCode();
            String email =subject.getEmail();
            String name = subject.getFirstname() + " " + subject.getSurname();
            String mobileNumber =subject.getMobileNumber();
            
            //Ejecutar servlet
            redirectTo("/giftServlet/?code=" + code + "&email=" + email +
                    "&name=" + name + "&mobileNumber=" + mobileNumber);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GiftHome.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
