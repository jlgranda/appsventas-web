/*
 * Copyright (C) 2021 jlgranda
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
package org.jlgranda.fede.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.jpapi.model.Organization;
import org.jpapi.model.profile.Subject;
import org.primefaces.component.selectonemenu.SelectOneMenu;

/**
 *
 * @author jlgranda
 */
@Named @SessionScoped
public class OrganizationData implements Serializable {
    
    private Organization organization;
    
    @Inject
    private Subject subject;
    
    @Inject
    private OrganizationHome organizationHome;

    @PostConstruct
    private void init() {
        List<Organization> orgs = organizationHome.findOrganizations(subject);
        if (orgs.size() == 1){
            setOrganization(orgs.get(0));
        }
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
    
    public void organizationValueChange(javax.faces.event.AjaxBehaviorEvent event) throws IOException {
        
        SelectOneMenu x = (SelectOneMenu) event.getSource();
        Organization newOrganization = ((Organization) x.getValue());
        setOrganization(newOrganization);
        redirectTo();
    }
    
    private void redirectTo() throws IOException {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath());
    }
    
}
