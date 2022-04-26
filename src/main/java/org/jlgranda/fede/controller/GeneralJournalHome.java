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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;
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
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
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

    /**
     * EDIT OBJECT.
     */
    private Long journalId;
    private GeneralJournal journal;
    private Long recordId;
    private Record record;
    private RecordDetail recordDetail;

    /**
     * UX.
     */
    private Date generalJournalDate; //Fecha tentativa para obtener el registro contable, si existiese
    private List<Record> recordsSelected;
    private RecordDetail recordDetailSelected;
    protected List<SelectItem> generalJournalActions = new ArrayList<>();
    private LazyGeneralJournalDataModel lazyDataModel;
    private GeneralJournal journalSelected;
    private List<GeneralJournal> journalsSelected;
    private BigDecimal subtotalDebe;
    private BigDecimal subtotalHaber;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(Dates.addDays(getEnd(), -1 * (Dates.getDayOfMonth(getEnd()) - 1))));

        filter();
        initializeActions();

        setJournal(journalService.createInstance());//Instancia de Cuenta
        setRecord(recordService.createInstance());
        setRecordDetail(recordDetailService.createInstance());

        setOutcome("journals");
    }

    public Long getJournalId() {
        return journalId;
    }

    public void setJournalId(Long journalId) {
        this.journalId = journalId;
    }

    public GeneralJournal getJournal() {
        System.out.println("this.journalId:: " + this.journalId);
        System.out.println("this.journal.isPersistent():: " + (!this.journal.isPersistent()));
        if (this.journalId != null && !this.journal.isPersistent()) {
            this.journal = journalService.find(journalId);
            this.journal.setRecords(recordService.findByNamedQuery("Record.findByJournalId", this.journalId));
            getCalculateBalance();
        }
        return this.journal;
    }

    public void setJournal(GeneralJournal journal) {
        this.journal = journal;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
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

    public LazyGeneralJournalDataModel getLazyDataModel() {
        return lazyDataModel;
    }

    public void setLazyDataModel(LazyGeneralJournalDataModel lazyDataModel) {
        this.lazyDataModel = lazyDataModel;
    }

    public Date getGeneralJournalDate() {
        return generalJournalDate;
    }

    public void setGeneralJournalDate(Date generalJournalDate) {
        this.generalJournalDate = generalJournalDate;
    }

    public GeneralJournal getJournalSelected() {
        if (this.generalJournalDate != null) {
            this.journalSelected = buildJournal(generalJournalDate);
        }
        return journalSelected;
    }

    public void setJournalSelected(GeneralJournal journalSelected) {
        this.journalSelected = journalSelected;
    }

    public List<GeneralJournal> getJournalsSelected() {
        return journalsSelected;
    }

    public void setJournalsSelected(List<GeneralJournal> journalsSelected) {
        this.journalsSelected = journalsSelected;
    }

    public List<Record> getRecordsSelected() {
        return recordsSelected;
    }

    public void setRecordsSelected(List<Record> recordsSelected) {
        this.recordsSelected = recordsSelected;
    }

    public RecordDetail getRecordDetailSelected() {
        return recordDetailSelected;
    }

    public void setRecordDetailSelected(RecordDetail recordDetailSelected) {
        this.recordDetailSelected = recordDetailSelected;
    }

    public BigDecimal getSubtotalDebe() {
        return subtotalDebe;
    }

    public void setSubtotalDebe(BigDecimal subtotalDebe) {
        this.subtotalDebe = subtotalDebe;
    }

    public BigDecimal getSubtotalHaber() {
        return subtotalHaber;
    }

    public void setSubtotalHaber(BigDecimal subtotalHaber) {
        this.subtotalHaber = subtotalHaber;
    }

    /**
     * METHODS.
     *
     */
    public void save() {
        if (journal.isPersistent()) {
            journal.setLastUpdate(Dates.now());
        } else {
            journal.setAuthor(this.subject);
            journal.setOwner(this.subject);
        }
        journalService.save(journal.getId(), journal);
    }

    /**
     * METHODS UTIL.
     *
     */
    public void clear() {
        filter();
    }

    public void filter() {
        if (lazyDataModel == null) {
            lazyDataModel = new LazyGeneralJournalDataModel(journalService);
        }
        lazyDataModel.setOrganization(this.organizationData.getOrganization());
        lazyDataModel.setStart(Dates.minimumDate(getStart()));
        lazyDataModel.setEnd(Dates.maximumDate(getEnd()));
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

    private void getCalculateBalance() {
        setSubtotalDebe(journalService.findBigDecimal("RecordDetail.findTotalByGeneralJournalAndType", this.journalId, RecordDetail.RecordTDetailType.DEBE, this.organizationData.getOrganization()));
        setSubtotalHaber(journalService.findBigDecimal("RecordDetail.findTotalByGeneralJournalAndType", this.journalId, RecordDetail.RecordTDetailType.HABER, this.organizationData.getOrganization()));
    }

    public void onRowSelect(SelectEvent event) {
        try {
            //Redireccionar a RIDE de objeto seleccionado
            if (event != null && event.getObject() != null) {
                GeneralJournal p = (GeneralJournal) event.getObject();
                redirectTo("/pages/accounting/journal.jsf?journalId=" + p.getId());
            }
        } catch (IOException ex) {
            logger.error("No fue posible seleccionar las {} con nombre {}" + I18nUtil.getMessages("BussinesEntity"), ((BussinesEntity) event.getObject()).getName());
        }
    }

    public void onRowSelectAsRecord(SelectEvent event) {
    }

    public void onRowSelectRecord(SelectEvent<RecordDetail> event) {
        this.recordDetail = event.getObject();
        addInfoMessage(I18nUtil.getMessages("action.sucessfully"), "Registro seleccionado: " + this.recordDetail.getAccount().getName());
    }

    public void onRowUnselectRecord(UnselectEvent<RecordDetail> event) {
        setRecordDetail(recordDetailService.createInstance()); //Encerar formulario de edición
    }

    private GeneralJournal buildJournal() {
        return this.buildJournal(Dates.now());
    }

    private GeneralJournal buildJournal(Date emissionDate) {
        String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", "Libro diario");
        String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
        return journalService.find(emissionDate, this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);
    }

    public void validateNewJournal() throws IOException {
        List<GeneralJournal> generalJournal = journalService.findByNamedQueryWithLimit("GeneralJournal.findByCreatedOnAndOrganization", 1, Dates.minimumDate(Dates.now()), Dates.maximumDate(Dates.now()), this.organizationData.getOrganization());
        if (generalJournal.isEmpty()) {
            redirectTo("/pages/accounting/journal.jsf");
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.journal.available.date", " " + Dates.toDateString(Dates.now())));
        }
    }

    public void validateNewReloadJournal() throws IOException {
        if (this.journalId == null) {
            setJournal(buildJournal());
        }
    }

    //Acciones sobre seleccionados
    public void execute() {
        if (isActionExecutable()) {
            if ("borrar".equalsIgnoreCase(this.selectedAction)) {
                for (Record instance : getRecordsSelected()) {
                    instance.setDeleted(Boolean.TRUE);
                    this.recordService.save(instance.getId(), instance); //Actualizar la entidad
                }
                setJournal(journalService.createInstance()); //Forzar carga de journal actual
                getJournal();
                setOutcome("");
            } else if ("moveto".equalsIgnoreCase(this.selectedAction) && getJournalSelected() != null) {
                for (Record instance : getRecordsSelected()) {
                    if (getJournalSelected() != null && getJournalSelected().getId() != null) {
                        instance.setGeneralJournalId(getJournalSelected().getId());
                        instance.setEmissionDate(getJournalSelected().getEmissionDate());
                        recordService.save(instance.getId(), instance); //Actualizar la entidad
                        getJournal();
                    } else {
                        this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.journal.available.date", " " + Dates.toDateString(Dates.now())));
                    }
                }
                //Recargar el journal actual
                setJournal(journalService.createInstance()); //Forzar carga de journal actual
                getJournal();
                setOutcome("");
            } else if ("generaljournal-borrar".equalsIgnoreCase(this.selectedAction)) {
                for (GeneralJournal instance : getJournalsSelected()) {
                    instance.setDeleted(Boolean.TRUE);
                    journalService.save(instance.getId(), instance); //Actualizar la entidad
                }

                //volver a filtrar la vista
                setLazyDataModel(null);
                clear();
                setOutcome("");
            }
        }
    }

    public boolean isActionExecutable() {
        if ("borrar".equalsIgnoreCase(this.selectedAction) && !getRecordsSelected().isEmpty()) {
            return true;
        } else if ("moveto".equalsIgnoreCase(this.selectedAction) && getJournalSelected() != null) {
            return true;
        } else if ("generaljournal-borrar".equalsIgnoreCase(this.selectedAction) && !getJournalsSelected().isEmpty()) {
            return true;
        }
        return false;
    }

    private void initializeActions() {
        this.actions = new ArrayList<>();
        SelectItem item = null;
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        actions.add(item);

        item = new SelectItem("borrar", I18nUtil.getMessages("common.delete"));
        actions.add(item);

        item = new SelectItem("moveto", I18nUtil.getMessages("common.moveto"));
        actions.add(item);

        this.generalJournalActions = new ArrayList<>();
        item = new SelectItem(null, I18nUtil.getMessages("common.choice"));
        this.generalJournalActions.add(item);

        item = new SelectItem("generaljournal-borrar", I18nUtil.getMessages("common.delete"));
        this.generalJournalActions.add(item);
    }

    public List<SelectItem> getGeneralJournalActions() {
        return generalJournalActions;
    }

    public void setGeneralJournalActions(List<SelectItem> generalJournalActions) {
        this.generalJournalActions = generalJournalActions;
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
            setJournalId((Long) getSessionParameter("journalId"));
            if (existsSessionParameter("recordId")) {
                setRecordId((Long) getSessionParameter("recordId"));
            }
            getJournal(); //Carga el objeto persistente
            getRecord(); //Carga el objeto persistente
        }
    }

    public void orderRecordDetails() {
        Collections.sort(this.record.getRecordDetails(), (RecordDetail recordDetail1, RecordDetail other) -> recordDetail1.getRecordDetailType().compareTo(other.getRecordDetailType()));//Ordenar por tipo de entrada/salida de transacción
    }

    public boolean isRecordOfReferen() {
        return this.record.getBussinesEntityId() == null;
    }

    public void recordDetailAdd() {
        if (this.recordDetail.getAccount() != null && (BigDecimal.ZERO.compareTo(this.recordDetail.getAmount()) == -1) && this.recordDetail.getRecordDetailType() != null) {
            this.recordDetail.setOwner(subject);
            this.record.addRecordDetail(this.recordDetail);
            //Preparar para una nueva entrada
            this.recordDetail = recordDetailService.createInstance();
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.detail.incomplete"));
        }
    }

    public void recordSave() {
        this.record.setOwner(subject);
        this.record.setGeneralJournalId(this.journalId);
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
            recordService.save(record.getId(), record);
            closeFormularioRecord(journal.getId());
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.balance.required"));
        }
    }

    @Override
    public void handleReturn(SelectEvent event) {
        setJournalId((Long) event.getObject());
        setJournal(new GeneralJournal());
        getJournal();
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        Record record = null;
        //El registro casí listo para agregar al journal
        return record;
    }

    @Override
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    @Override
    protected void initializeDateInterval() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Group> getGroups() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
//    public void handleReturn(SelectEvent event) {
//        this.setJournalId((Long) event.getObject());
//        this.setJournal(new GeneralJournal());
//        this.getJournal();
//    }
//    
//    @Override
//    public Group getDefaultGroup() {
//        return this.defaultGroup;
//    }
//    
//    @Override
//    public List<Group> getGroups() {
//        if (this.groups.isEmpty()) {
//            //Todos los grupos para el modulo actual
//            setGroups(groupService.findByOwnerAndModuleAndType(subject, settingHome.getValue(SettingNames.MODULE + "inventory", "inventory"), Group.Type.LABEL));
//        }
//        return this.groups;
//    }
}
