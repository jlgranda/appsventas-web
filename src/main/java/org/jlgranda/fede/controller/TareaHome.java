/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlgranda.fede.controller;

import com.jlgranda.fede.ejb.SubjectService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import net.tecnopro.document.ejb.TareaService;
import net.tecnopro.document.model.Tarea;
import org.jlgranda.fede.cdi.LoggedIn;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jorge
 */
@Named(value = "tareaHome")
@RequestScoped
public class TareaHome extends FedeController implements Serializable {

    Logger logger = LoggerFactory.getLogger(TareaHome.class);

    @Inject
    @LoggedIn
    private Subject subject;
    @Inject
    private SettingHome settingHome;
    @EJB
    private TareaService tareaService;
    @EJB
    private SubjectService subjectService;
    private List<Tarea> tareas = new ArrayList<>();
    private Tarea tarea;
    private String estado;

    @PostConstruct
    private void init() {
        setTarea(tareaService.createInstance());
    }

    public List<Tarea> getTareas() {
        int limit = Integer.parseInt(settingHome.getValue("fede.dashboard.timeline.length", "5"));
        if (tareas.isEmpty()) {
            tareas = tareaService.findByNamedQuery("Tarea.findLasts", limit);
        }
        return tareas;
    }

    public void save() {

        try {
            getTarea().setAuthor(subject);
            tareaService.save(getTarea().getId(), getTarea());
            this.addDefaultSuccessMessage();
        } catch (Exception e) {
            addErrorMessage(e, I18nUtil.getMessages("error.persistence"));
        }
    }

    public List<Subject> getSubjects() {
        return subjectService.all(subject);
    }

    public BigDecimal countRowsByTag(String tag) {
        BigDecimal total = new BigDecimal(0);
        if ("all".equalsIgnoreCase(tag)) {
            total = new BigDecimal(tareaService.count());
        } else if ("own".equalsIgnoreCase(tag)) {
            total = new BigDecimal(tareaService.count("Tarea.countBussinesEntityByOwner", subject));
        } else {
            total = new BigDecimal(tareaService.count("Tarea.countBussinesEntityByTagAndOwner", tag, subject));
        }
        return total;
    }

    public void editar(Tarea tarea) {
        setTarea(tarea);
    }

    public void setTareas(List<Tarea> tareas) {
        this.tareas = tareas;
    }


  

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Tarea getTarea() {
        return tarea;
    }

    public void setTarea(Tarea tarea) {
        this.tarea = tarea;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public Group getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
