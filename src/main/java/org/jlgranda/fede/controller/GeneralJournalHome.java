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

import com.jlgranda.fede.SettingNames;
import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.ui.model.LazyGeneralJournalDataModel;
import org.jpapi.util.I18nUtil;
import org.jpapi.model.BussinesEntity;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.primefaces.event.CellEditEvent;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;
import org.primefaces.ecuador.domain.Car;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kellypaulinc
 */
@Named
@ViewScoped
public class GeneralJournalHome extends FedeController implements Serializable {

    private static final long serialVersionUID = -1007161141552849702L;

    Logger logger = LoggerFactory.getLogger(GeneralJournalHome.class);

    @Inject
    private Subject subject;

    @Inject
    private SettingHome settingHome;

    @Inject
    private OrganizationData organizationData;

    @EJB
    private GroupService groupService;

    @EJB
    private AccountService accountService;

    @EJB
    private GeneralJournalService journalService;

    @EJB
    private RecordService recordService;

    @EJB
    private RecordDetailService recordDetailService;

    private LazyGeneralJournalDataModel lazyDataModel;

    /**
     * El objeto Journal para edición
     */
    private GeneralJournal journal;

    /**
     * El objeto Record para edición
     */
    private Record record;

    /**
     * RecordDetail para edición
     */
    private RecordDetail recordDetail;

    private Long journalId;
    private Long recordId;

    private Account accountSelected;

    @PostConstruct
    private void init() {
        int range = 0;
        try {
            range = Integer.valueOf(settingHome.getValue(SettingNames.JOURNAL_TOP_RANGE, "7"));
        } catch (java.lang.NumberFormatException nfe) {
            nfe.printStackTrace();
            range = 7;
        }

        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * range)));
        setJournal(journalService.createInstance());//Instancia de Cuenta
        setOutcome("general-journals");
        filter();

        this.record = recordService.createInstance();
        this.recordDetail = recordDetailService.createInstance();

    }

    @Override
    public void handleReturn(SelectEvent event) {
        this.setJournalId((Long) event.getObject());
        this.setJournal(new GeneralJournal());
        this.getJournal();
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    public List<Group> getGroups() {
        if (this.groups.isEmpty()) {
            //Todos los grupos para el modulo actual
            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), Group.Type.LABEL));
        }
        return this.groups;
    }

    public Long getJournalId() {
        return journalId;
    }

    public void setJournalId(Long journalId) {
        this.journalId = journalId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public GeneralJournal getJournal() {
        if (this.journalId != null && !this.journal.isPersistent()) {
            this.journal = journalService.find(journalId);
        }
        return this.journal;
    }

    public void setJournal(GeneralJournal journal) {
        this.journal = journal;
    }

    public LazyGeneralJournalDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyGeneralJournalDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Record getRecord() {
        if (this.recordId != null && !this.record.isPersistent()) {
            this.record = recordService.find(recordId);
        }
        return this.record;
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

    public List<Account> getAccounts() {
        return accountService.findByOrganization(this.organizationData.getOrganization());
    }

    public Account getAccountSelected() {
        return accountSelected;
    }

    public void setAccountSelected(Account accountSelected) {
        this.accountSelected = accountSelected;
    }

    public void clear() {
        filter();
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyGeneralJournalDataModel(journalService);
        }
//        lazyDataModel.setOwner(this.subject);
        lazyDataModel.setOrganization(this.organizationData.getOrganization());
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
                GeneralJournal p = (GeneralJournal) event.getObject();
                redirectTo("/pages/fede/accounting/journal.jsf?journalId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void save() {
        if (journal.isPersistent()) {
            journal.setLastUpdate(Dates.now());
        } else {
            journal.setAuthor(this.subject);
            journal.setOwner(this.subject);
        }
        journalService.save(journal.getId(), journal);
    }

    public boolean mostrarFormularioRecord(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_RECORD, width, height, left, top, true, params);
        return true;
    }

    public boolean mostrarFormularioRecord() {

        if (this.journal != null) {
            super.setSessionParameter("journalId", this.journal.getId());
            super.setSessionParameter("recordId", null);
        }

        return mostrarFormularioRecord(null);
    }

    public boolean editarFormularioRecord(Long recordId) {

        if (this.journal != null) {
            super.setSessionParameter("journalId", this.journal.getId());
        }
        if (recordId != null) {
            super.setSessionParameter("recordId", recordId);
        }
        return mostrarFormularioRecord(null);
    }

    public void closeFormularioRecord(Object data) {
        removeSessionParameter("journalId");
        super.closeDialog(data);
    }

    public void loadSessionParameters() {

        if (existsSessionParameter("journalId")) {
            this.setJournalId((Long) getSessionParameter("journalId"));
            if (existsSessionParameter("recordId")) {
                this.setRecordId((Long) getSessionParameter("recordId"));
            }
            this.getJournal(); //Carga el objeto persistente
            this.getRecord(); //Carga el objeto persistente
        }
    }

    /**
     * Agrega un detalle al Record
     */
    public void addRecordDetail() {
        this.recordDetail.setOwner(subject);
            this.record.addRecordDetail(this.recordDetail);
        //Preparar para una nueva entrada
        this.recordDetail = recordDetailService.createInstance();
    }

    /**
     * Eliminar un detalle al Record
     */
    public void removeRecordDetail() {

    }

    /**
     * Agrega un record al Journal
     */
    public void saveRecord() {
        this.record.setOwner(subject);
        journal.addRecord(this.record); //Agregar el record al journal
        BigDecimal sumDebe = new BigDecimal(0);
        BigDecimal sumHaber = new BigDecimal(0);
        for (int i = 0; i < this.record.getRecordDetails().size(); i++) {
            if (this.record.getRecordDetails().get(i).getRecordDetailType() == RecordDetail.RecordTDetailType.DEBE) {
                sumDebe = sumDebe.add(this.record.getRecordDetails().get(i).getAmount());
            } else if (this.record.getRecordDetails().get(i).getRecordDetailType() == RecordDetail.RecordTDetailType.HABER) {
                sumHaber = sumHaber.add(this.record.getRecordDetails().get(i).getAmount());
            }
        }
        if (sumDebe.compareTo(sumHaber) == 0) {
            journalService.save(journal.getId(), journal);
            closeFormularioRecord(journal.getId());
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.journal.record.cuadre"));
        }
    }

    private GeneralJournal buildJournal() {
        GeneralJournal generalJournal = journalService.createInstance();
        generalJournal.setOrganization(this.organizationData.getOrganization());
        generalJournal.setOwner(subject);
        generalJournal.setCode(UUID.randomUUID().toString());
        generalJournal.setName(I18nUtil.getMessages("app.fede.accouting.journal") + " " + this.organizationData.getOrganization().getInitials() + "/" + Dates.toDateString(Dates.now()));
        return generalJournal;
    }

    public void newJournal() {
        if (this.journalId == null) {
            this.journal = journalService.save(buildJournal());
        }
    }

    public void validateNewJournal() throws IOException {
        GeneralJournal generalJournal = journalService.createInstance();
        generalJournal = journalService.findUniqueByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
        if (generalJournal == null) {
            redirectTo("/pages/fede/accounting/journal.jsf");
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.journal.isExist") + " " + Dates.toDateString(Dates.now()));
        }
//        List<GeneralJournal> generalJournal = new ArrayList<>();
//        generalJournal = journalService.findByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
//        if (generalJournal.isEmpty()) {
//            redirectTo("/pages/fede/accounting/journal.jsf");
//        }else{
//            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.journal.isExist") + " " + Dates.toDateString(Dates.now()));
//        }
    }

    public void validateNewReloadJournal() throws IOException {
        if (this.journalId == null) {
            GeneralJournal generalJournal = journalService.createInstance();
            generalJournal = journalService.findUniqueByNamedQuery("Journal.findByCreatedOnAndOrg", Dates.minimumDate(Dates.now()), Dates.now(), this.organizationData.getOrganization());
            if (generalJournal == null) {
                this.journal = journalService.save(buildJournal());
            } else {
                this.journal = generalJournal;
            }
        }
    }
    
    public boolean isRecordOfReferen() {
        return this.record.getFacturaElectronica() == null;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
