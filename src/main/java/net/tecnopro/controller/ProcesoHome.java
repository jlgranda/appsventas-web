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
package net.tecnopro.controller;

import com.jlgranda.fede.SettingNames;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Inject;
import net.tecnopro.document.ejb.ProcesoService;
import net.tecnopro.document.model.Proceso;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jlgranda.fede.controller.FedeController;
import org.jlgranda.fede.controller.SettingHome;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jlgranda
 */
public class ProcesoHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(ProcesoHome.class);

    @Inject
    @LoggedIn
    private Subject subject;
    
    private Long procesoId;
    
    private Proceso proceso;
    
    @Inject
    private SettingHome settingHome;
    
    @EJB
    private ProcesoService procesoService;
    
    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe){
            amount = 30;
        }
        
        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));
        
        setOutcome("procesos");
        
        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    public Long getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(Long procesoId) {
        this.procesoId = procesoId;
    }

    public Proceso getProceso() {
        if (this.procesoId != null && !this.proceso.isPersistent()) {
            this.proceso = procesoService.find(procesoId);
        }
        return proceso;
    }

    public void setProceso(Proceso proceso) {
        this.proceso = proceso;
    }
    
    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
