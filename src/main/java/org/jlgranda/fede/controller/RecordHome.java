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
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jpapi.model.Group;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

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
    @Inject
    private SettingHome settingHome;

    @EJB
    private GeneralJournalService generalJournalService;
    @EJB
    private RecordService recordService;
    @EJB
    private RecordDetailService recordDetailService;
    @EJB
    AccountCache accountCache;

    private GeneralJournal generalJournal;
    private Long generalJournalId;
    private Record record;
    private List<Record> recordPorCreatedOn;
    private List<Record> recordPorGeneralJournal;
    private RecordDetail recordDetail;
    private RecordDetail recordDetailSelected;

    @PostConstruct
    private void init() {
        record = recordService.createInstance();
        recordDetail = recordDetailService.createInstance();
        this.recordPorCreatedOn = recordService.findByNamedQuery("Record.findByCreatedOnAndOrganization", Dates.minimumDate(Dates.now()), Dates.maximumDate(Dates.now()), this.organizationData.getOrganization());
    }

    public GeneralJournal getGeneralJournal() {
        if (this.generalJournalId != null) {
            return generalJournalService.find(this.generalJournalId);
        }
        return generalJournal;
    }

    public void setGeneralJournal(GeneralJournal generalJournal) {
        this.generalJournal = generalJournal;
    }

    public Long getGeneralJournalId() {
        return generalJournalId;
    }

    public void setGeneralJournalId(Long generalJournalId) {
        this.generalJournalId = generalJournalId;
    }

    public List<Record> getRecordPorCreatedOn() {
        return recordPorCreatedOn;
    }

    public void setRecordPorCreatedOn(List<Record> recordPorCreatedOn) {
        this.recordPorCreatedOn = recordPorCreatedOn;
    }

    public List<Record> getRecordPorGeneralJournal() {
        return recordPorGeneralJournal;
    }

    public void setRecordPorGeneralJournal(List<Record> recordPorGeneralJournal) {
        this.recordPorGeneralJournal = recordPorGeneralJournal;
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

    public RecordDetail getRecordDetailSelected() {
        return recordDetailSelected;
    }

    public void setRecordDetailSelected(RecordDetail recordDetailSelected) {
        this.recordDetailSelected = recordDetailSelected;
    }

    public List<Account> filterAccounts(String query) {
        return accountCache.filterByNameOrCode(query, this.organizationData.getOrganization());
    }

    public List<Account> filterAccountsChildrens(String query) {
        return accountCache.filterByNameOrCodeChildrens(query, this.organizationData.getOrganization());
    }

    public void recordDetailAdd(ActionEvent e) {

        this.recordDetail.setOwner(this.subject);
        this.record.addRecordDetail(this.recordDetail);

        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(this.recordDetail.getAccount().getName()));

        //Encerar el registro
        this.recordDetail = recordDetailService.createInstance();

        //Calcular valor sugerido, que sería el faltanta para cuadrar el registro
        double debe;
        debe = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.DEBE)
                .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
        double haber;
        haber = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.HABER)
                .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));

        BigDecimal suggestedAmount = BigDecimal.ZERO;
        if (debe > haber) {
            suggestedAmount = BigDecimal.valueOf(debe - haber);
        } else {
            suggestedAmount = BigDecimal.valueOf(haber - debe);
        }
        this.recordDetail.setAmount(suggestedAmount); //Por si el valor es el mismo

        this.recordDetailSelected = null;
    }

    public void onItemAccountSelect(SelectEvent<Account> event) {
        Account account = event.getObject();
        Optional<RecordDetail> find = this.record.getRecordDetails().stream().filter(x -> x.getAccount().equals(account)).findFirst();
        if (find.isPresent()) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> cuenta ya existente!!! ");
            this.setRecordDetail(find.get());
        }
    }
    
    public void onRowSelect(SelectEvent<RecordDetail> event) {
        this.recordDetail = event.getObject();
        addInfoMessage(I18nUtil.getMessages("action.info"), "Registro seleccionado: " + this.recordDetail.getAccount().getName());

    }

    public void onRowUnselect(UnselectEvent<RecordDetail> event) {

        this.setRecordDetail(recordDetailService.createInstance()); //Encerar formulario de edición
    }

    public void recordSave() {
        boolean valido = true;
        if (this.record.getRecordDetails().isEmpty()) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.recordDetail.incomplete"));
        }
        if (org.jpapi.util.Strings.isNullOrEmpty(this.record.getDescription())) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.description.required"));
        }

        if (this.record.getEmissionDate() == null) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.emision.required"));
        }

        if (valido) {

            Date lastEmissionDate = this.record.getEmissionDate();
            double debe;
            debe = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.DEBE)
                    .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
            double haber;
            haber = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.HABER)
                    .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));

            if (debe == haber) {
                this.record.setOwner(subject);
                //Localizar o generar el generalJournal
                this.generalJournal = this.buildJournal(this.record.getEmissionDate());
                if (this.generalJournal != null && this.generalJournal.getId() != null) {
                    this.record.setGeneralJournalId(this.generalJournal.getId());
                    recordService.save(record.getId(), record);

                    //Encerar objetos de pantalla
                    this.recordDetail = recordDetailService.createInstance();
                    this.record = recordService.createInstance();
                    this.record.setEmissionDate(lastEmissionDate);

                    this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.record.manual.sucessfully"));
                    //this.generalJournal = generalJournalService.createInstance();
                }
            } else {
                this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.balance.required"));
            }

        }
    }

    private GeneralJournal buildJournal(Date emissionDate) {
        String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", "Libro diario");
        String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
        return generalJournalService.find(emissionDate, this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);
    }

    public void viewGeneralJournal() {
        this.generalJournal = this.buildJournal(this.record.getEmissionDate());
        if (this.generalJournal != null && this.generalJournal.getId() != null) {
            this.buildDialog();
        }
    }

    public boolean buildDialog() {
        super.setSessionParameter("generalJournalId", this.generalJournal.getId());
        return mostrarFormularioGeneralJournal(null);
    }

    public boolean mostrarFormularioGeneralJournal(Map<String, List<String>> params) {
        String width = settingHome.getValue(SettingNames.POPUP_WIDTH, "800");
        String height = settingHome.getValue(SettingNames.POPUP_HEIGHT, "600");
        String left = settingHome.getValue(SettingNames.POPUP_LEFT, "0");
        String top = settingHome.getValue(SettingNames.POPUP_TOP, "0");
        super.openDialog(SettingNames.POPUP_FORMULARIO_GENERALJOURNAL, width, height, left, top, true, params);
        return true;
    }

    public void closeFormularioGeneralJournal(Object data) {
        removeSessionParameter("generalJournalId");
        super.closeDialog(data);
    }

    public void loadSessionParameters() {
        if (existsSessionParameter("generalJournalId")) {
            this.setGeneralJournalId((Long) getSessionParameter("generalJournalId"));
            this.setGeneralJournal(this.getGeneralJournal()); //Carga el objeto persistente
            if (this.generalJournal != null && this.generalJournal.getId() != null) {
                this.recordPorGeneralJournal = recordService.findByNamedQuery("Record.findByJournalId", this.generalJournal.getId());
            }
        }
    }

    public void irALibroDiario(Long generalJournalId) throws IOException {
        //Redireccionar a RIDE de objeto seleccionado
        if (generalJournalId != null) {
            redirectTo("//pages/accounting/journal.jsf?journalId=" + generalJournalId);
        }
    }

    public void confirmRecordDelete(Record record) {
        this.recordDelete(record);
    }

    public void recordDelete(Record record) {
        recordService.deleteRecord(record);
        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf("Asiento # " + record.getId() + " eliminado."));
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
