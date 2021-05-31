/*
 * Copyright (C) 2021 kellypaulinc
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

import com.jlgranda.fede.ejb.RecordDetailTemplateService;
import com.jlgranda.fede.ejb.RecordTemplateService;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.jlgranda.fede.model.accounting.RecordDetailTemplate;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kellypaulinc
 */
public class RecordTemplateHome extends FedeController implements Serializable {
    
    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(GeneralJournalHome.class);
    
    @Inject
    private Subject subject;
    
    @EJB
    private RecordTemplateService recordTemplateService;
    
    @EJB
    private RecordDetailTemplateService recordDetailTemplateService;
    
    /**
     * El objeto Record para edición
     */
    private RecordTemplate recordTemplate;

    /**
     * RecordDetail para edición
     */
    private RecordDetailTemplate recordDetailTemplate;
    
    @PostConstruct
    private void init() {
        this.recordTemplate = recordTemplateService.createInstance();
        this.recordDetailTemplate = recordDetailTemplateService.createInstance();
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
