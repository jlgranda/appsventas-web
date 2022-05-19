/*
 * Copyright (C) 2022 usuario
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
import com.jlgranda.fede.ejb.CashBoxDetailService;
import com.jlgranda.fede.ejb.CashBoxGeneralService;
import com.jlgranda.fede.ejb.CashBoxPartialService;
import com.jlgranda.fede.ejb.GeneralJournalService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.Constantes;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.CashBoxDetail;
import org.jlgranda.fede.model.accounting.CashBoxGeneral;
import org.jlgranda.fede.model.accounting.CashBoxPartial;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

/**
 * Ajustes para pantalla de cierre de caja
 *
 * @author usuario
 */
@Named
@ViewScoped
public class CashBoxGeneralHome extends FedeController implements Serializable {

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
    private CashBoxGeneralService cashBoxGeneralService;
    @EJB
    private CashBoxPartialService cashBoxPartialService;
    @EJB
    private CashBoxDetailService cashBoxDetailService;
    @EJB
    private GeneralJournalService generalJournalService;
    @EJB
    private RecordService recordService;
    @EJB
    private RecordDetailService recordDetailService;

    /**
     * EDIT OBJECT.
     */
    private Long cashBoxGeneralId;
    private CashBoxGeneral cashBoxGeneral;
    private Long cashBoxPartialId;
    private CashBoxPartial cashBoxPartial;
    private CashBoxDetail cashBoxDetail;
    private GeneralJournal generalJournal;
    private Record record;
    private RecordDetail recordDetail;

    /**
     * UX.
     */
    //Calcular resumen
    private CashBoxPartial cashBoxPartialPrevious;
    private BigDecimal cashCurrent; //Efectivo actual, desde la cuenta principal
    private BigDecimal cashToAccounting; //Dinero que va ha ser contabilizado

    //Desglosar el efectivo de Caja
    private List<CashBoxDetail> cashBoxBills = new ArrayList<>();
    private List<CashBoxDetail> cashBoxMoneys = new ArrayList<>();

    //Manejo de registro contable
    private Account accountMain;
    private RecordDetail recordDetailSelected;

    //Manejo de componentes
    private boolean recordCompleto;
    private int index = 0;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(getEnd()));

        setCashBoxGeneral(buildCashBoxGeneral(Dates.now()));
        setCashBoxPartial(cashBoxPartialService.createInstance());

        initializeVars();

        setRecord(recordService.createInstance());
        setRecordDetail(recordDetailService.createInstance());
        asignedCashBoxPartialProperties();

        setOutcome("cash-initial");
    }

    public Long getCashBoxGeneralId() {
        return cashBoxGeneralId;
    }

    public void setCashBoxGeneralId(Long cashBoxGeneralId) {
        this.cashBoxGeneralId = cashBoxGeneralId;
    }

    public CashBoxGeneral getCashBoxGeneral() {
        if (this.cashBoxGeneralId != null && !this.cashBoxGeneral.isPersistent()) {
            this.cashBoxGeneral = cashBoxGeneralService.find(this.cashBoxGeneralId);
        }
        return cashBoxGeneral;
    }

    public void setCashBoxGeneral(CashBoxGeneral cashBoxGeneral) {
        this.cashBoxGeneral = cashBoxGeneral;
    }

    public Long getCashBoxPartialId() {
        return cashBoxPartialId;
    }

    public void setCashBoxPartialId(Long cashBoxPartialId) {
        this.cashBoxPartialId = cashBoxPartialId;
    }

    public CashBoxPartial getCashBoxPartial() {
        if (this.cashBoxPartialId != null && !this.cashBoxPartial.isPersistent()) {
            this.cashBoxPartial = cashBoxPartialService.find(this.cashBoxPartialId);
            if (this.cashBoxPartial.getId() != null) {
                this.cashBoxBills = getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.BILL);
                this.cashBoxMoneys = getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.MONEY);
                //Cargar el dinero a registrar contablemente
                cargarCashFinally();
            }
        }
        return cashBoxPartial;
    }

    public void setCashBoxPartial(CashBoxPartial cashBoxPartial) {
        this.cashBoxPartial = cashBoxPartial;
    }

    public CashBoxDetail getCashBoxDetail() {
        return cashBoxDetail;
    }

    public void setCashBoxDetail(CashBoxDetail cashBoxDetail) {
        this.cashBoxDetail = cashBoxDetail;
    }

    public GeneralJournal getGeneralJournal() {
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

    public void editRecordDetail(RecordDetail detail) {
        this.recordDetail = detail; //En edición
    }

    /**
     * METHODS.
     */
    private CashBoxGeneral buildCashBoxGeneral(Date emissionDate) {
        String generalJournalPrefix = settingHome.getValue("app.fede.accounting.close.cash", "Cierre de Caja");
        String timestampPattern = settingHome.getValue("app.fede.accounting.close.cash.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
        return cashBoxGeneralService.find(emissionDate, this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);
    }

    public void saveCashBoxGeneral() {

        updateCashBoxPartialProperties();

        this.cashBoxGeneral.addCashBoxPartial(this.cashBoxPartial);//Añadir el cashBoxPartial al cashBoxGeneral

        this.cashBoxGeneral.setCashGeneral(this.cashBoxPartial.getCashPartial());
        this.cashBoxGeneral.setTotalBreakdown(this.cashBoxPartial.getTotalCashBreakdown());
        this.cashBoxGeneral.setMissCash(this.cashBoxPartial.getMissCash());
        this.cashBoxGeneral.setExcessCash(this.cashBoxPartial.getExcessCash());
        this.cashBoxGeneral.setCashFinally(this.cashBoxPartial.getCashFinally());

        cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral);
        if (this.cashBoxPartialId == null) {
            setCashBoxPartialId(cashBoxPartialService.getLastCashBoxPartialByCashBoxGeneral(this.cashBoxGeneral));
        }
        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.ajust.save.message"));
    }

    /**
     * METHODS UTIL.
     */
    private void initializeVars() {
        setCashBoxMoneys(new ArrayList<>());
        setCashBoxBills(new ArrayList<>());
        setCashCurrent(BigDecimal.ZERO);
        setCashToAccounting(BigDecimal.ZERO);
        setAccountMain(accountService.findUniqueByNamedQuery("Account.findByNameAndOrg", "CAJA DIA", this.organizationData.getOrganization()));
        if (getAccountMain() != null) {
            setCashCurrent(accountService.mayorizarTo(getAccountMain(), this.organizationData.getOrganization(), Dates.maximumDate(getEnd())));
        }
        setCashBoxPartialPrevious(cashBoxPartialService.findByOrganizationAndLastId(this.organizationData.getOrganization()));
    }

    public void closeCashBoxPartial() {
        updateCashBoxPartialStatusFinally();
        this.cashBoxPartial.setPriority(cashBoxPartialService.getPriority(this.cashBoxGeneral));
        this.cashBoxPartial.setStatusComplete(Boolean.FALSE);
        saveCashBoxGeneral();
        redirectToAccounting(this.cashBoxPartialId);
    }

    private void updateCashBoxPartialStatusFinally() {
        if (!this.cashBoxGeneral.getCashBoxPartials().isEmpty()) {
            for (int i = 0; i < this.cashBoxGeneral.getCashBoxPartials().size(); i++) {
                if (this.cashBoxGeneral.getCashBoxPartials().get(i).getPriority().equals(this.cashBoxGeneral.getCashBoxPartials().size() - 1)) {
                    //Actualizar registro
                    this.cashBoxGeneral.getCashBoxPartials().get(i).setStatusFinally(Boolean.TRUE);
                }
            }
        }
    }

    public void updateCashBoxPartialProperties() {
        if (this.cashBoxPartial.isPersistent()) {
            this.cashBoxPartial.setLastUpdate(Dates.now());
        } else {
            this.cashBoxPartial.setAuthor(this.subject);
            this.cashBoxPartial.setOwner(this.subject);
            this.cashBoxPartial.setName(I18nUtil.getMessages("app.fede.accounting.close.cash.of", "" + this.cashBoxPartial.getOwner().getFullName(), Dates.toDateString(Dates.now())));
        }
    }

    public void onRowEditCashBoxDetail(RowEditEvent<CashBoxDetail> event) {
        if (event.getObject() != null) {
            event.getObject().setAmount(event.getObject().getValuer().multiply(BigDecimal.valueOf(event.getObject().getQuantity())));
            recalculatedTotals();
        }
    }

    public void onRowCancelCashBoxDetail(RowEditEvent<CashBoxDetail> event) {
    }

    private void recalculatedTotals() {
        resetVariablesTotals();
        this.cashBoxPartial.setCashPartial(getCashCurrent());//Dinero en efectivo que marca según libros hasta ese momento.
        this.cashBoxPartial.setTotalCashBills(getSubTotals(this.cashBoxBills, CashBoxDetail.DenominationType.BILL));
        this.cashBoxPartial.setTotalCashMoneys(getSubTotals(this.cashBoxMoneys, CashBoxDetail.DenominationType.MONEY));
        this.cashBoxPartial.setTotalCashBreakdown(this.cashBoxPartial.getTotalCashBills().add(this.cashBoxPartial.getTotalCashMoneys())); //Dinero que ha sido desglosado, existente.
        this.cashBoxPartial.setCashFinally(this.cashBoxPartial.getTotalCashBreakdown());
        recalculateBalanceCashBoxPartial(this.cashBoxPartial.getCashPartial());
    }

    private void resetVariablesTotals() {
        this.cashBoxPartial.setTotalCashBills(BigDecimal.ZERO);
        this.cashBoxPartial.setTotalCashMoneys(BigDecimal.ZERO);
        this.cashBoxPartial.setCashPartial(BigDecimal.ZERO);
        this.cashBoxPartial.setMissCash(BigDecimal.ZERO);
        this.cashBoxPartial.setExcessCash(BigDecimal.ZERO);
    }

    private BigDecimal getSubTotals(List<CashBoxDetail> list, CashBoxDetail.DenominationType denominationType) {
        Double subtotal = list.stream()
                .filter(d -> denominationType.equals(d.getDenominationType()))
                .mapToDouble(d -> d.getAmount().doubleValue())
                .sum();
        return new BigDecimal(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private void recalculateBalanceCashBoxPartial(BigDecimal currentCash) {
        switch (this.cashBoxPartial.getTotalCashBreakdown().compareTo(currentCash)) {
            case 0:
                this.cashBoxPartial.setMissCash(BigDecimal.ZERO);
                this.cashBoxPartial.setExcessCash(BigDecimal.ZERO);
                break;
            case -1:
                this.cashBoxPartial.setMissCash(this.cashBoxPartial.getTotalCashBreakdown().subtract(currentCash));
                this.cashBoxPartial.setExcessCash(BigDecimal.ZERO);
                break;
            case 1:
                this.cashBoxPartial.setMissCash(BigDecimal.ZERO);
                this.cashBoxPartial.setExcessCash(this.cashBoxPartial.getTotalCashBreakdown().subtract(currentCash));
                break;
            default:
                break;
        }
        this.cashBoxPartial.setMissCash(this.cashBoxPartial.getMissCash().multiply(BigDecimal.valueOf(-1)));//Guardar valores positivos
    }

    public void asignedCashBoxPartialProperties() {
        if (this.cashBoxPartialId == null && this.cashBoxPartial.getId() == null && this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
            this.cashBoxPartial.setCashBoxDetails(getDetailsByDenominations(this.cashBoxPartial));
            this.cashBoxBills = getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.BILL);
            this.cashBoxMoneys = getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.MONEY);
        }
    }

    private List<CashBoxDetail> getDetailsByDenominations(CashBoxPartial partial) {
        List<CashBoxDetail> detailsByDenominations = new ArrayList<>();
        List<Setting> denominations = settingHome.getDenominations(CodeType.DENOMINATION);
        if (!denominations.isEmpty()) {
            denominations.stream().map(d -> {
                this.cashBoxDetail = cashBoxDetailService.createInstance(); //Preparar para un nuevo detalle del CashBox instanciado
                return d;
            }).map(d -> {
                if (d.getCategory().equals(CashBoxDetail.DenominationType.BILL.toString())) {
                    this.cashBoxDetail.setDenominationType(CashBoxDetail.DenominationType.BILL);
                } else if (d.getCategory().equals(CashBoxDetail.DenominationType.MONEY.toString())) {
                    this.cashBoxDetail.setDenominationType(CashBoxDetail.DenominationType.MONEY);
                }
                return d;
            }).map(d -> {
                this.cashBoxDetail.setDenomination(d.getLabel());
                return d;
            }).map(d -> {
                this.cashBoxDetail.setValuer(new BigDecimal(d.getValue()));
                return d;
            }).forEachOrdered(_item -> {
                this.cashBoxDetail.setCashBoxPartial(partial);
                detailsByDenominations.add(this.cashBoxDetail);//Agregar el CashBoxDetail al CashBox instanciado
            });
        }
        return detailsByDenominations;
    }

    public List<CashBoxDetail> getCashBoxDetailsByDenominationType(List<CashBoxDetail> list, CashBoxDetail.DenominationType denominationType) {
        List<CashBoxDetail> details = new ArrayList<>();
        if (denominationType != null && list != null && !list.isEmpty()) {
            details = list.stream()
                    .filter(d -> d.getDenominationType().equals(denominationType))
                    .collect(Collectors.toList());
        }
        return details;
    }

    private void redirectToAccounting(Long cashBoxPartialIdRedirect) {
        if (cashBoxPartialIdRedirect != null) {
            try {
                redirectTo("/pages/accounting/cash_accounting.jsf?cashBoxPartialId=" + (getCashBoxPartialId()));
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CashBoxGeneralHome.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void cargarCashFinally() {
        setCashToAccounting(this.index == 0 ? this.cashBoxPartial.getCashFinally() : (this.index == 1 ? this.cashBoxPartial.getExcessCash() : (this.index == 2 ? this.cashBoxPartial.getMissCash() : BigDecimal.ZERO)));
    }

    public void onItemAccountSelect(SelectEvent<Account> event) {
        Account account = event.getObject();
        if (account.getName().toLowerCase().contains(Constantes.ACCOUNT_DAY)) {
            this.addErrorMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accounting.close.cash.account.main.noselected"));
            setRecordDetail(recordDetailService.createInstance());
        } else {
            //Cargar valor por defecto
            this.recordDetail.setAmount(getAmountEstimatedWithout(this.recordDetail.getAccount(), this.cashToAccounting, this.record.getRecordDetails()));
        }
    }

    public void recordDetailAdd() {
        recordDetailAdd(this.recordDetail, this.cashToAccounting, this.record.getRecordDetails());
    }

    public void recordDetailAdd(RecordDetail rdEdit, BigDecimal totalMeta, List<RecordDetail> list) {
        if (validCashFinally(rdEdit, totalMeta, list)) {
            this.recordDetail.setOwner(this.subject);
            this.record.addRecordDetail(this.recordDetail);
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(rdEdit.getAccount().getName()));
            //Encerar el registro 
            this.recordDetail = recordDetailService.createInstance();
        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.deposit.amount.sum.invalid", "" + totalMeta));
        }
    }

    public void recordDetailDelete(RecordDetail detail) {
        Optional<RecordDetail> rd = this.record.getRecordDetails().stream()
                .filter(d -> d.getAccount().getId().equals(detail.getAccount().getId()))
                .findFirst();
        if (rd.isPresent()) {
            this.record.getRecordDetails().remove(this.record.getRecordDetails().indexOf(rd.get()));
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(rd.get().getAccount().getName()));
            //Encerar el registro
            this.recordDetail = recordDetailService.createInstance();
        }
    }

    private Boolean validCashFinally(RecordDetail rdEdit, BigDecimal totalMeta, List<RecordDetail> list) {
        if (rdEdit.getAmount().compareTo(getAmountEstimatedWithout(rdEdit.getAccount(), totalMeta, list)) == 1) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }

    public BigDecimal getTotalAmountByRecord(List<RecordDetail> list) {
        double total = list.stream()
                .filter(x -> Objects.equals(x.isDeleted(), Boolean.FALSE))
                .map(x -> x.getAmount())
                .collect(Collectors.summingDouble(BigDecimal::doubleValue));
        return new BigDecimal(total).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getAmountEstimated(BigDecimal totalMeta, List<RecordDetail> list) {
        return totalMeta.subtract(getTotalAmountByRecord(list));
    }

    private BigDecimal getAmountEstimatedWithout(Account accountEdit, BigDecimal totalMeta, List<RecordDetail> list) {
        List<RecordDetail> listEdit = list.stream()
                .filter(x -> (Objects.equals(x.isDeleted(), Boolean.FALSE) && !accountEdit.getId().equals(x.getAccount().getId())))
                .collect(Collectors.toList());
        return getAmountEstimated(totalMeta, listEdit);
    }

    public void recordSaveCash() {
        System.out.println("recordSaveCash");
        //Actualizar el cashFinal del cashBoxPartial
        this.cashBoxPartial.setCashFinally(getAmountEstimated(this.cashToAccounting, this.record.getRecordDetails()));
        this.cashBoxGeneral.addCashBoxPartial(this.cashBoxPartial);
        this.cashBoxGeneral.setCashFinally(this.cashBoxPartial.getCashFinally());
        cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral);
        recordSave();
    }
    
    public void recordSaveExcess() {
        System.out.println("recordSaveExcess");
        recordSave();
    }
    
    public void recordSaveMissing() {
        System.out.println("recordSaveMissing");
        recordSave();
    }

    public void recordSave() {
        //Construir los records de cada detail agregado
        boolean valido = true;
        if (this.record.getRecordDetails().isEmpty()) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.detail.incomplete"));
        }
        if (getAccountMain() == null) {
            valido = false;
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.close.cash.account.main.incorrect"));
        }
        if (valido) {
            saveBuildRecord(this.cashBoxPartial, this.subject, this.record, this.accountMain);

            //Encerar objetos de pantalla
            this.recordDetail = recordDetailService.createInstance();
            this.record = recordService.createInstance();
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.record.correct.message"));
            //Redireccionar al index correspondiente
            redirectToIndex();
        }
    }

    public void saveBuildRecord(CashBoxPartial partial, Subject sb, Record rEdit, Account accountBase) {
        //Localizar o generar el generalJournal
        setGeneralJournal(buildJournal(Dates.now()));

        rEdit.getRecordDetails().forEach(rdd -> {
            Record newRecord = generateRecordInstance();
            RecordDetail newRecordDetail = recordDetailService.createInstance();
            newRecordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.HABER);
            newRecordDetail.setAccount(accountBase);
            newRecordDetail.setAmount(rdd.getAmount());
            newRecord.addRecordDetail(newRecordDetail);
            newRecordDetail = recordDetailService.createInstance();
            newRecordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.DEBE);
            newRecordDetail.setAccount(rdd.getAccount());
            newRecordDetail.setAmount(rdd.getAmount());
            newRecord.addRecordDetail(newRecordDetail);
            newRecord.setBussinesEntityType(partial.getClass().getSimpleName());
            newRecord.setBussinesEntityId(partial.getId());
            newRecord.setBussinesEntityHashCode(partial.hashCode());
            newRecord.setName(String.format("%s: %s[id=%d]", "REGISTRO_CAJA_DIA_A_" + rdd.getAccount(), getClass().getSimpleName(), partial.getId()));
            newRecord.setDescription(String.format("%s \nEnvío de dinero desde: %s, a %s \nMonto: %s", sb.getFullName(), accountBase.getName(), rdd.getAccount().getName(), Strings.format(rdd.getAmount().doubleValue(), "$ #0.##")));
            recordService.save(newRecord.getId(), newRecord);
        });
    }

    private void redirectToIndex() {
        if (this.index < 1 && this.cashBoxPartial.getExcessCash().compareTo(BigDecimal.ZERO) == 1) {
            setIndex(1);
            cargarCashFinally();
        } else if (this.index < 2 && this.cashBoxPartial.getMissCash().compareTo(BigDecimal.ZERO) == 1) {
            setIndex(2);
            cargarCashFinally();
        } else {
            try {
                redirectTo("/pages/accounting/cash_initial.jsf");
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CashBoxGeneralHome.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Record generateRecordInstance() {
        Record newRecord = recordService.createInstance();
        record.setOwner(this.subject);
        if (this.generalJournal != null) {
            newRecord.setGeneralJournalId(this.generalJournal.getId());
        }
        return newRecord;
    }

    private GeneralJournal buildJournal(Date emissionDate) {
        String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", "Libro diario");
        String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
        return generalJournalService.find(emissionDate, this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);
    }

    public List<CashBoxPartial> getCashBoxPartialsCompleted(List<CashBoxPartial> list) {
        List<CashBoxPartial> partials = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(p -> {
                if (Objects.equals(Boolean.TRUE, p.getStatusComplete())) {
                    partials.add(p);
                }
            });
        }
        return partials;
    }

    public CashBoxPartial getCashBoxPartialPrevious() {
        return cashBoxPartialPrevious;
    }

    public void setCashBoxPartialPrevious(CashBoxPartial cashBoxPartialPrevious) {
        this.cashBoxPartialPrevious = cashBoxPartialPrevious;
    }

    public BigDecimal getCashCurrent() {
        return cashCurrent;
    }

    public void setCashCurrent(BigDecimal cashCurrent) {
        this.cashCurrent = cashCurrent;
    }

    public BigDecimal getCashToAccounting() {
        return cashToAccounting;
    }

    public void setCashToAccounting(BigDecimal cashToAccounting) {
        this.cashToAccounting = cashToAccounting;
    }

    public List<CashBoxDetail> getCashBoxBills() {
        return cashBoxBills;
    }

    public void setCashBoxBills(List<CashBoxDetail> cashBoxBills) {
        this.cashBoxBills = cashBoxBills;
    }

    public List<CashBoxDetail> getCashBoxMoneys() {
        return cashBoxMoneys;
    }

    public void setCashBoxMoneys(List<CashBoxDetail> cashBoxMoneys) {
        this.cashBoxMoneys = cashBoxMoneys;
    }

    public boolean isRecordCompleto() {
        return recordCompleto;
    }

    public void setRecordCompleto(boolean recordCompleto) {
        this.recordCompleto = recordCompleto;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Account getAccountMain() {
        return accountMain;
    }

    public void setAccountMain(Account accountMain) {
        this.accountMain = accountMain;
    }

    public RecordDetail getRecordDetailSelected() {
        return recordDetailSelected;
    }

    public void setRecordDetailSelected(RecordDetail recordDetailSelected) {
        this.recordDetailSelected = recordDetailSelected;
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

//public void recordSave() {
//        if (!this.cashFinally.equals(this.cashBoxPartial.getCashFinally())) {//Actualizar el cashFinal del cashBoxPartial
//            this.cashBoxPartial.setCashFinally(this.cashFinally);
//            this.cashBoxPartial.setStatusComplete(Boolean.TRUE);
//            this.cashBoxGeneral.addCashBoxPartial(this.cashBoxPartial);
//            this.cashBoxGeneral.setCashFinally(this.cashFinally);
//            cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral);
//        }
//        //Construir los records de cada detail agregado
//        boolean valido = true;
//        if (this.record.getRecordDetails().isEmpty()) {
//            valido = false;
//            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.record.detail.incomplete"));
//        }
//        if (getAccountMain() == null) {
//            valido = false;
//            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accounting.close.cash.account.main.incorrect"));
//        }
//        if (valido) {
//            //Localizar o generar el generalJournal
//            setGeneralJournal(this.buildJournal(Dates.now()));
//
//            this.record.getRecordDetails().forEach(rdd -> {
//                Record newRecord = generateRecordInstance();
//                RecordDetail newRecordDetail = recordDetailService.createInstance();
//                newRecordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.HABER);
//                newRecordDetail.setAccount(getAccountMain());
//                newRecordDetail.setAmount(rdd.getAmount());
//                newRecord.addRecordDetail(newRecordDetail);
//                newRecordDetail = recordDetailService.createInstance();
//                newRecordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.DEBE);
//                newRecordDetail.setAccount(rdd.getAccount());
//                newRecordDetail.setAmount(rdd.getAmount());
//                newRecord.addRecordDetail(newRecordDetail);
//                newRecord.setBussinesEntityType(this.cashBoxPartial.getClass().getSimpleName());
//                newRecord.setBussinesEntityId(this.cashBoxPartial.getId());
//                newRecord.setBussinesEntityHashCode(this.cashBoxPartial.hashCode());
//                newRecord.setName(String.format("%s: %s[id=%d]", "REGISTRO_CAJA_DIA_A_" + rdd.getAccount(), getClass().getSimpleName(), this.cashBoxPartial.getId()));
//                newRecord.setDescription(String.format("%s \nEnvío de dinero desde: %s, a %s \nMonto: %s", this.subject.getFullName(), getAccountMain().getName(), rdd.getAccount().getName(), Strings.format(rdd.getAmount().doubleValue(), "$ #0.##")));
//                recordService.save(newRecord.getId(), newRecord);
//            });
//            //Encerar objetos de pantalla
//            this.recordDetail = recordDetailService.createInstance();
//            this.record = recordService.createInstance();
//            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.record.correct.message"));
//        }
//    }
