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

import com.jlgranda.fede.ejb.AccountService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
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
    AccountCache accountCache;
    @EJB
    private AccountService accountService;
    @EJB
    private GeneralJournalService generalJournalService;
    @EJB
    private RecordService recordService;
    @EJB
    private RecordDetailService recordDetailService;

    /**
     * EDIT OBJECT.
     */
    private Long generalJournalId;
    private GeneralJournal generalJournal;
    private Record record;

    /**
     * UX.
     */
    private RecordDetail recordDetail;
    private RecordDetail recordDetailSelected;
    private Date emissionDate;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfMonth(getEnd()) - 1))));

        setGeneralJournal(generalJournalService.createInstance());
        setRecord(recordService.createInstance());
        setEmissionDate(this.generalJournal.getEmissionDate());
        setRecordDetail(recordDetailService.createInstance());

        setOutcome("journals");
    }

    /**
     * METHODS.
     */
    public void save() {
        boolean valido = true;
        //Localizar o generar el generalJournal
        if (this.generalJournal == null || this.generalJournal.getId() == null) {
            this.generalJournal = this.buildJournal(getEmissionDate());
        }
        if (this.generalJournal == null || this.generalJournal.getId() == null) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), "Error interno relacionado con el libro diario.");
        }

        if (this.record.getRecordDetails().isEmpty()) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.detail.incomplete"));
        }
        if (org.jpapi.util.Strings.isNullOrEmpty(this.record.getDescription())) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.description.inlinehelp"));
        }

        if (getEmissionDate() == null) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.date.inlinehelp"));
        }

        if (valido) {
            Date lastEmissionDate = getEmissionDate();
            double debe = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.DEBE)
                    .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
            double haber = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.HABER)
                    .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));

            if (debe == haber) {

                this.record.setOwner(subject);
                this.record.setGeneralJournalId(this.generalJournal.getId());
                this.record.setEmissionDate(this.generalJournal.getEmissionDate());
                this.record.getEmissionDate().setHours(Dates.now().getHours());
                this.record.getEmissionDate().setMinutes(Dates.now().getMinutes());
                this.record.getEmissionDate().setSeconds(Dates.now().getSeconds());

                recordService.save(record.getId(), record);

                //Encerar objetos de pantalla
                setRecordDetail(recordDetailService.createInstance());
                setRecord(recordService.createInstance());
                setEmissionDate(lastEmissionDate);

                this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.record.correct.message"));
            } else {
                this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.balance.required"));
            }
        }
    }

    /**
     * METHODS UTIL.
     */
    public Long getGeneralJournalIdByEmisionDate() {
        if (this.generalJournalId != null) {
            return this.generalJournalId;
        } else if (getEmissionDate() != null) {
            return buildJournal(getEmissionDate()).getId();
        }
        return null;
    }

    private GeneralJournal buildJournal(Date emissionDate) {
        String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", "Libro diario");
        String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
        return generalJournalService.find(emissionDate, this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);
    }

    public void recordDetailAdd(ActionEvent e) {
        this.recordDetail.setOwner(this.subject);
        this.record.addRecordDetail(this.recordDetail);
        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(this.recordDetail.getAccount().getName()));

        //Encerar el registro
        this.recordDetail = recordDetailService.createInstance();

        this.recordDetail.setAmount(calculateValueSuggested()); //Por si el valor es el mismo
    }

    private BigDecimal calculateValueSuggested() {
        //Calcular valor sugerido, que sería el faltanta para cuadrar el registro
        BigDecimal suggestedAmount = BigDecimal.ZERO;
        double debe = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.DEBE)
                .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
        double haber = this.record.getRecordDetails().stream().filter(x -> x.getRecordDetailType() == RecordDetail.RecordTDetailType.HABER)
                .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));

        if (debe > haber) {
            suggestedAmount = BigDecimal.valueOf(debe - haber);
        } else {
            suggestedAmount = BigDecimal.valueOf(haber - debe);
        }
        return suggestedAmount;
    }

    public void onItemAccountSelect(SelectEvent<Account> event) {
        Account account = event.getObject();
        Optional<RecordDetail> find = this.record.getRecordDetails().stream().filter(x -> x.getAccount().equals(account)).findFirst();
        if (find.isPresent()) {
            System.out.println("\n¡CUENTA YA EXISTENTE!\n");
            this.setRecordDetail(find.get());
        }
    }

    public Long getGeneralJournalId() {
        return generalJournalId;
    }

    public void setGeneralJournalId(Long generalJournalId) {
        this.generalJournalId = generalJournalId;
    }

    public GeneralJournal getGeneralJournal() {
        if (this.generalJournalId != null) {
            this.generalJournal = generalJournalService.find(this.generalJournalId);
        }
        return generalJournal;
    }

    public void setGeneralJournal(GeneralJournal generalJournal) {
        this.generalJournal = generalJournal;
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

    public Date getEmissionDate() {
        if (this.getGeneralJournal().getId() != null) {
            setEmissionDate(this.generalJournal.getEmissionDate());
        }
        return emissionDate;
    }

    public void setEmissionDate(Date emissionDate) {
        this.emissionDate = emissionDate;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
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
