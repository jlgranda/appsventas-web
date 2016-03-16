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
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import net.tecnopro.document.ejb.InstanciaProcesoService;
import net.tecnopro.document.ejb.TareaService;
import net.tecnopro.document.model.InstanciaProceso;
import net.tecnopro.document.model.Tarea;
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
@ManagedBean
@ViewScoped
public class InstanciaProcesoHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(InstanciaProcesoHome.class);

    @Inject
    @LoggedIn
    private Subject subject;

    private Long procesoId;

    private InstanciaProceso instanciaProceso;

    @Inject
    private SettingHome settingHome;

    @EJB
    private InstanciaProcesoService instanciaProcesoService;
    @EJB
    private TareaService tareaService;

    private Tarea ultimaTarea;
    private List<Tarea> tareas;

    @PostConstruct
    public void init() {
        int amount = 0;
        try {
            amount = Integer.valueOf(settingHome.getValue(SettingNames.DASHBOARD_RANGE, "360"));
        } catch (java.lang.NumberFormatException nfe) {
            amount = 30;
        }

        setEnd(Dates.now());
        setStart(Dates.addDays(getEnd(), -1 * amount));

        setOutcome("procesos");
        setInstanciaProceso(instanciaProcesoService.createInstance());
        //TODO Establecer temporalmente la organización por defecto
        //getOrganizationHome().setOrganization(organizationService.find(1L));
    }

    public Long getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(Long procesoId) {
        this.procesoId = procesoId;
    }

    public InstanciaProceso getInstanciaProceso() {
        if (this.procesoId != null && !this.instanciaProceso.isPersistent()) {
            this.instanciaProceso = instanciaProcesoService.find(procesoId);
        }
        return instanciaProceso;
    }

    public void setInstanciaProceso(InstanciaProceso instanciaProceso) {
        this.instanciaProceso = instanciaProceso;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Tarea getUltimaTarea() {
        if (procesoId != null && this.instanciaProceso.isPersistent()) {
            List<Tarea> tareasUltima = tareaService.findByNamedQuery("Tarea.findLastByInstanciaProceso", getInstanciaProceso());
            ultimaTarea = tareasUltima.isEmpty() ? new Tarea() : (Tarea) tareasUltima.get(0);
        }
        return ultimaTarea;
    }

    public void setUltimaTarea(Tarea ultimaTarea) {
        this.ultimaTarea = ultimaTarea;
    }

    public List<Tarea> getTareas() {
        if (procesoId != null && this.instanciaProceso.isPersistent()) {
            List<Tarea> tareasInstanciaProceso = tareaService.findByNamedQuery("Tarea.findLastsByInstanciaProceso", getInstanciaProceso());
            tareasInstanciaProceso.remove(this.ultimaTarea);
            tareas = tareasInstanciaProceso;
        }
        return tareas;
    }

    public void setTareas(List<Tarea> tareas) {
        this.tareas = tareas;
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
