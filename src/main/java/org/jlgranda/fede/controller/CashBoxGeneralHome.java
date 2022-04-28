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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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
    private BigDecimal cashCurrent; //Efectivo actual, desde la cuenta principal
    private BigDecimal cashFinally; //Dinero que va quedando en los registros contables

    //Desglosar el efectivo de Caja
    private List<CashBoxDetail> cashBoxBills = new ArrayList<>();
    private List<CashBoxDetail> cashBoxMoneys = new ArrayList<>();

    //Manejo de registro contable
    private Account accountMain;
    private RecordDetail recordDetailSelected;

    //Manejo de componentes
    private boolean recordCompleto;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(getEnd()));

        setCashBoxGeneral(cashBoxGeneralService.createInstance());
        setCashBoxPartial(cashBoxPartialService.createInstance());

        setRecord(recordService.createInstance());
        setRecordDetail(recordDetailService.createInstance());
        initializeVars();
        asignedCashBoxPartialProperties();

        setOutcome("cash-initial");
    }

    public Long getCashBoxGeneralId() {
        setCashBoxGeneralId(cashBoxGeneralService.findIdByOrganizationAndCreatedOn(this.organizationData.getOrganization(), Dates.now(), Dates.now()));
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
        System.out.println("\n>>>getCashBoxPartial<<<\n");
        if (this.cashBoxPartialId != null) {
            this.cashBoxPartial = cashBoxPartialService.find(this.cashBoxPartialId);
            getBillsMoneysList();
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
    public void saveCashBoxGeneral() {

        if (this.cashBoxGeneral.isPersistent()) {
            this.cashBoxGeneral.setLastUpdate(Dates.now());
            this.cashBoxGeneral.setAuthor(this.subject); //Actualizar, para saber que sujeto lo cierra por última vez
        } else {
            this.cashBoxGeneral.setAuthor(this.subject);
            this.cashBoxGeneral.setOwner(this.subject);
            this.cashBoxGeneral.setOrganization(this.organizationData.getOrganization());
        }

        updateCashBoxPartialProperties();

        this.cashBoxGeneral.addCashBoxPartial(this.cashBoxPartial);//Añadir el cashBoxPartial al cashBoxGeneral

        this.cashBoxGeneral.setCashGeneral(this.cashBoxPartial.getCashPartial());
        this.cashBoxGeneral.setTotalBreakdown(this.cashBoxPartial.getTotalCashBreakdown());
        this.cashBoxGeneral.setMissCash(this.cashBoxPartial.getMissCash());
        this.cashBoxGeneral.setExcessCash(this.cashBoxPartial.getExcessCash());
        this.cashBoxGeneral.setCashFinally(this.cashBoxPartial.getCashFinally());
        this.cashBoxGeneral.setName(I18nUtil.getMessages("app.fede.accounting.close.cash.of", "" + this.organizationData.getOrganization().getInitials(), Dates.toDateString(Dates.now())));

        cashBoxGeneralService.save(this.cashBoxGeneral.getId(), this.cashBoxGeneral);
        this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.ajust.save.message"));
    }

    /**
     * METHODS UTIL.
     */
    public void closeCashBoxPartial() {
        updateCashBoxPartialStatusFinally();
        this.cashBoxPartial.setPriority(cashBoxPartialService.getPriority(this.cashBoxGeneral));
        this.cashBoxPartial.setStatusComplete(Boolean.TRUE); //Marcar como cerrado/completado el cashBoxPartial
        saveCashBoxGeneral();
        redirectToAccounting(this.cashBoxPartial.getId());
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

    private void initializeVars() {
        setCashCurrent(BigDecimal.ZERO);
        setCashFinally(BigDecimal.ZERO);

        setAccountMain(accountService.findUniqueByNamedQuery("Account.findByNameAndOrg", "CAJA DIA", this.organizationData.getOrganization()));
        if (getAccountMain() != null) {
            setCashCurrent(accountService.mayorizarTo(getAccountMain(), this.organizationData.getOrganization(), Dates.maximumDate(getEnd())));
        }
        setCashBoxMoneys(new ArrayList<>());
        setCashBoxBills(new ArrayList<>());
    }

    public void onRowEditCashBoxDetail(RowEditEvent<CashBoxDetail> event) {
        if (event.getObject() != null) {
            event.getObject().setAmount(event.getObject().getValuer().multiply(BigDecimal.valueOf(event.getObject().getQuantity())));
            updateCashBoxDetail(event.getObject());
        }
    }

    public void onRowCancelCashBoxDetail(RowEditEvent<CashBoxDetail> event) {
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

    public void updateCashBoxDetail(CashBoxDetail updateCashBoxDetail) {
        if (!this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
            for (int i = 0; i < this.cashBoxPartial.getCashBoxDetails().size(); i++) {
                if (updateCashBoxDetail.getDenominationType().equals(this.cashBoxPartial.getCashBoxDetails().get(i).getDenominationType())
                        && updateCashBoxDetail.getDenomination().equals(this.cashBoxPartial.getCashBoxDetails().get(i).getDenomination())) {
                    //Actualizar registro
                    this.cashBoxPartial.getCashBoxDetails().get(i).setQuantity(updateCashBoxDetail.getQuantity());
                    this.cashBoxPartial.getCashBoxDetails().get(i).setAmount(updateCashBoxDetail.getAmount());
                    recalculatedTotals();
                }
            }
        }
    }

    private void recalculatedTotals() {
        resetVariablesTotals();
        this.cashBoxPartial.setCashPartial(getCashCurrent());//Dinero en efectivo que marca según libros hasta ese momento.
        this.cashBoxPartial.setTotalCashBills(getSubTotals(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.BILL));
        this.cashBoxPartial.setTotalCashMoneys(getSubTotals(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.MONEY));
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
        Double subtotal = 0.00;
        subtotal = list.stream()
                .filter(d -> denominationType.equals(d.getDenominationType()))
                .mapToDouble(d -> d.getAmount().doubleValue())
                .sum();
        return new BigDecimal(subtotal);
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
        addDetailsByDenominations();
        getBillsMoneysList();
    }

    private void addDetailsByDenominations() {
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
                this.cashBoxPartial.addCashBoxDetail(this.cashBoxDetail);//Agregar el CashBoxDetail al CashBox instanciado
            });
            this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
        }
    }

    private void getBillsMoneysList() {
        setCashBoxBills(new ArrayList<>());
        setCashBoxMoneys(new ArrayList<>());
        setCashBoxBills(getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.BILL));
        setCashBoxMoneys(getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.MONEY));
    }

    public List<CashBoxDetail> getCashBoxDetailsByDenominationType(List<CashBoxDetail> list, CashBoxDetail.DenominationType denominationType) {
        List<CashBoxDetail> details = new ArrayList<>();
        if (denominationType != null && list != null && !list.isEmpty()) {
            details = list.stream().filter(d -> d.getDenominationType().equals(denominationType)).collect(Collectors.toList());
        }
        return details;
    }

    private void redirectToAccounting(Long casbBoxPartialId) {
        if (casbBoxPartialId != null) {
            setCashBoxPartialId(casbBoxPartialId);
            try {
                redirectTo("/pages/accounting/cash_accounting.jsf?cashBoxPartialId=" + (getCashBoxPartialId()));
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(CashBoxGeneralHome.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void onItemAccountSelect(SelectEvent<Account> event) {
        Account account = event.getObject();
        if (account.getName().toLowerCase().contains(Constantes.ACCOUNT_DAY)) {
            this.addErrorMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("app.fede.accounting.close.cash.account.main.noselected"));
            setRecordDetail(recordDetailService.createInstance());
        } else {
            Optional<RecordDetail> find = this.record.getRecordDetails().stream().filter(x -> x.getAccount().getId().equals(account.getId())).findFirst();
            if (find.isPresent()) {
                setRecordDetail(find.get());
                System.out.println("\n¡CUENTA YA EXISTENTE!\n");
            }
        }
    }

    public void recordDetailAdd(ActionEvent e) {

        if (validCashFinally(this.recordDetail.getAccount())) {
            this.recordDetail.setOwner(this.subject);
            this.record.addRecordDetail(this.recordDetail);

            //Restar el cashFinally que va quedando
            double total = this.record.getRecordDetails().stream().filter(x -> Objects.equals(x.isDeleted(), Boolean.FALSE))
                    .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
            setCashFinally(this.cashBoxPartial.getTotalCashBreakdown().subtract(new BigDecimal(total)));

            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully.detail"), String.valueOf(this.recordDetail.getAccount().getName()));

            //Encerar el registro
            this.recordDetail = recordDetailService.createInstance();
            this.recordDetailSelected = null;

        } else {
            this.addErrorMessage(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("app.fede.accouting.deposit.amount.invalid", "" + getCashFinally()));
        }
    }

    public void recordDetailDelete(RecordDetail detail) {

        detail.setDeleted(Boolean.TRUE);
        detail.setName(Strings.toUpperCase(Constantes.ESTADO_ELIMINADO + Constantes.SEPARADOR + detail.getName())); //Registrar el producto que estaba referenciado
        detail.setDescription(detail.toString()); //Registrar el producto que estaba referenciado
//                    aggregation.removeAggregationDetail(instance); //Quitar de la vista
        this.record.addRecordDetail(detail); //Reemplaza el detalle con el nuevo valor modificado para borrar
    }

    private Boolean validCashFinally(Account accountEdit) {
        if (this.cashBoxPartial.getTotalCashBreakdown().compareTo(calculateTotalByRecords(accountEdit)) == 1) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private BigDecimal calculateTotalByRecords(Account accountEdit) {
        double total = this.record.getRecordDetails().stream().filter(x -> (Objects.equals(x.isDeleted(), Boolean.FALSE) && !accountEdit.getId().equals(x.getAccount().getId())))
                .map(x -> x.getAmount()).collect(Collectors.summingDouble(BigDecimal::doubleValue));
        return new BigDecimal(total);
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
            //Localizar o generar el generalJournal
            setGeneralJournal(this.buildJournal(Dates.now()));

            this.record.getRecordDetails().forEach(rdd -> {
                Record newRecord = generateRecordInstance();
                RecordDetail newRecordDetail = recordDetailService.createInstance();
                newRecordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.HABER);
                newRecordDetail.setAccount(getAccountMain());
                newRecordDetail.setAmount(rdd.getAmount());
                newRecord.addRecordDetail(newRecordDetail);
                newRecordDetail = recordDetailService.createInstance();
                newRecordDetail.setRecordDetailType(RecordDetail.RecordTDetailType.DEBE);
                newRecordDetail.setAccount(rdd.getAccount());
                newRecordDetail.setAmount(rdd.getAmount());
                newRecord.addRecordDetail(newRecordDetail);
                newRecord.setBussinesEntityType(this.cashBoxPartial.getClass().getSimpleName());
                newRecord.setBussinesEntityId(this.cashBoxPartial.getId());
                newRecord.setBussinesEntityHashCode(this.cashBoxPartial.hashCode());
                newRecord.setName(String.format("%s: %s[id=%d]", "REGISTRO_CAJA_DIA_A_" + rdd.getAccount(), getClass().getSimpleName(), this.cashBoxPartial.getId()));
                newRecord.setDescription(String.format("%s \nEnvío de dinero desde: %s, a %s \nMonto: %s", this.subject.getFullName(), getAccountMain().getName(), rdd.getAccount().getName(), Strings.format(rdd.getAmount().doubleValue(), "$ #0.##")));
                recordService.save(newRecord.getId(), newRecord);
            });

            //Encerar objetos de pantalla
            this.recordDetail = recordDetailService.createInstance();
            this.record = recordService.createInstance();
            this.addSuccessMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("app.fede.accounting.record.correct.message"));
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

    public BigDecimal getCashCurrent() {
        return cashCurrent;
    }

    public void setCashCurrent(BigDecimal cashCurrent) {
        this.cashCurrent = cashCurrent;
    }

    public BigDecimal getCashFinally() {
        return cashFinally;
    }

    public void setCashFinally(BigDecimal cashFinally) {
        this.cashFinally = cashFinally;
    }

    public List<CashBoxDetail> getCashBoxBills() {
        System.out.println("\ngetCashBoxBills::" + this.cashBoxBills + "\n");
        return cashBoxBills;
    }

    public void setCashBoxBills(List<CashBoxDetail> cashBoxBills) {
        this.cashBoxBills = cashBoxBills;
//        this.cashBoxBills.addAll(cashBoxBills);
    }

    public List<CashBoxDetail> getCashBoxMoneys() {
        System.out.println("\ngetCashBoxMoneys::" + this.cashBoxMoneys + "\n");
        return cashBoxMoneys;
    }

    public void setCashBoxMoneys(List<CashBoxDetail> cashBoxMoneys) {
        this.cashBoxMoneys = cashBoxMoneys;
//        this.cashBoxMoneys.addAll(cashBoxMoneys);
    }

    public boolean isRecordCompleto() {
        return recordCompleto;
    }

    public void setRecordCompleto(boolean recordCompleto) {
        this.recordCompleto = recordCompleto;
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
