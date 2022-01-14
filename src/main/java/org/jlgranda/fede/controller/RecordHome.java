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

import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author kellypaulinc
 */
@Named
@ViewScoped
public class RecordHome extends FedeController implements Serializable {

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;

    @EJB
    private RecordService recordService;
    @EJB
    private RecordDetailService recordDetailService;
    @EJB
    AccountCache accountCache;

    private List<Record> recordPorCreatedOn;
    private Record record;
    private RecordDetail recordDetail;

    @PostConstruct
    private void init() {
        recordPorCreatedOn = recordService.findUniqueByNamedQuery("Record.findByCreatedOnAndOrganization", this.organizationData.getOrganization(), Dates.minimumDate(Dates.now()), Dates.maximumDate(Dates.now()));
    }

    public List<Record> getRecordPorCreatedOn() {
        return recordPorCreatedOn;
    }

    public void setRecordPorCreatedOn(List<Record> recordPorCreatedOn) {
        this.recordPorCreatedOn = recordPorCreatedOn;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public RecordDetail getRecordDetail() {
        return recordDetail;
    }

    public void setRecordDetail(RecordDetail recordDetail) {
        this.recordDetail = recordDetail;
    }

    public List<Account> filterAccounts(String query) {
        return accountCache.filterByNameOrCode(query, this.organizationData.getOrganization());
    }
    
    public void recordAdd(){
        System.out.println(">>>>>>>>>>>>>>>>>>>>>><");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>><");
        System.out.println(">>>>>>>>>>>><<llegóaqwe");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>><");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>><");
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

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
