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
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.RecordService;
import com.jlgranda.fede.ejb.RecordTemplateService;
import com.jlgranda.fede.ejb.accounting.AccountCache;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.jlgranda.fede.model.accounting.Account;
import org.jlgranda.fede.model.accounting.CashBoxDetail;
import org.jlgranda.fede.model.accounting.CashBoxGeneral;
import org.jlgranda.fede.model.accounting.CashBoxPartial;
import org.jlgranda.fede.model.accounting.GeneralJournal;
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.model.accounting.RecordTemplate;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jlgranda.rules.RuleRunner;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.jpapi.util.Strings;
import org.kie.internal.builder.KnowledgeBuilderErrors;
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

    private static final long serialVersionUID = -1007161141552849702L;
    Logger logger = LoggerFactory.getLogger(CashBoxHome.class);

    @Inject
    private Subject subject;
    @Inject
    private OrganizationData organizationData;
    @Inject
    private SettingHome settingHome;

    @EJB
    private GroupService groupService;
    @EJB
    private CashBoxGeneralService cashBoxGeneralService;
    @EJB
    private CashBoxPartialService cashBoxPartialService;
    @EJB
    private CashBoxDetailService cashBoxDetailService;
    @EJB
    private InvoiceService invoiceService;
    @EJB
    private RecordDetailService recordDetailService;
    @EJB
    private AccountService accountService;
    @EJB
    AccountCache accountCache;
    @EJB
    private GeneralJournalService generalJournalService;
    @EJB
    private RecordService recordService;
    @EJB
    private RecordTemplateService recordTemplateService;

    /**
     * EDIT OBJECT.
     */
    private Long cashBoxGeneralId;
    private CashBoxGeneral cashBoxGeneral;
    private CashBoxPartial cashBoxPartial;
    private CashBoxDetail cashBoxDetail;

    /**
     * UX.
     */
    //Calcular resumen
    private BigDecimal salesNet;//Dinero de ventas sólo en efectivo
    private BigDecimal purchasesNet;//Dinero de compras sólo en efectivo
    private BigDecimal transactionNet;//Dinero de otras transacciones sólo en efectivo
    private BigDecimal salesEffective;
    private BigDecimal salesCreditCollect;
//    private BigDecimal salesCredit;
//    private BigDecimal salesDedit;
    private BigDecimal purchasesCash;
    private BigDecimal purchasesCreditPaid;
//    private BigDecimal purchasesCredit;
    private BigDecimal transactionInput;
    private BigDecimal transactionOutput;
    private Account transactionAccount;
    private BigDecimal cashPrevious; //Saldo de ejercicion anterior
    private BigDecimal cashCurrent; //Efectivo actual

    //Desglosar el efectivo de Caja
    private List<CashBoxDetail> cashBoxBills;
    private List<CashBoxDetail> cashBoxMoneys;

    //Manejo de componentes
    private boolean disabledButtonBreakdown;
    private boolean activePanelBreakdown;
    private boolean disabledButtonCloseCashBoxGeneral;
    private boolean cashBoxInit; //Verifica si es el primer cashBoxGeneral de la base
    private Long cashBoxPartialInitId;
    private boolean activePanelInit;
    private boolean recordCompleto;

//    private boolean activePanelDeposit;
//    private boolean activeSelectDeposit;
//    private boolean activeButtonSelectDeposit;
    //Obtener lista de CashBoxClosed
//    private List<CashBoxPartial> cashBoxsClosed;
//    private List<CashBoxDetail> cashBoxsClosedBills;
//    private List<CashBoxDetail> cashBoxsClosedMoneys;
//    private CashBoxPartial cashBoxOpen;
//
//    //Calcular el dinero restante luego del depósito del Cierre de Caja
//    private BigDecimal saldoCashFund;
//    private boolean activePanelBreakdownFund;
//    private List<CashBoxDetail> cashBoxsInitialBills;
//    private List<CashBoxDetail> cashBoxsInitialMoneys;
//    private List<CashBoxGeneral> cashBoxGeneralInitial;
//    private CashBoxPartial cashBoxInitialFinish;
//    private int activeIndex;
//    private boolean activePanelVerification;
//
    @PostConstruct
    private void init() {
        setCashBoxInit(cashBoxGeneralService.isInitialByOrganization(this.organizationData.getOrganization()));

        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(getEnd()));

        setCashBoxGeneral(cashBoxGeneralService.createInstance());
        setCashBoxPartial(cashBoxPartialService.createInstance());

        getTodayCashBoxGeneralId();
        getCurrentCashBoxPartial();
        initializeVars();
        getCalculeSummaryCash(getStart(), getEnd());

        setOutcome("cash-close");
    }

    public void getTodayCashBoxGeneralId() {
        if (getCashBoxGeneralId() == null) {
            setCashBoxGeneralId(cashBoxGeneralService.findIdByOrganizationAndCreatedOn(this.organizationData.getOrganization(), Dates.now(), Dates.now()));
            getCashBoxGeneral();
        }
    }

    public void getCurrentCashBoxPartial() {
        if (!this.cashBoxGeneral.getCashBoxPartials().isEmpty()) {
            this.cashBoxGeneral.getCashBoxPartials().forEach(partial -> {
                if (Objects.equals(Boolean.FALSE, partial.getStatusComplete())) {
                    setCashBoxPartial(cashBoxPartialService.find(partial.getId()));
                    getBillsMoneysList();
                }
            });
        }
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

    public CashBoxPartial getCashBoxPartial() {
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
        this.cashBoxPartial.setStatusComplete(Boolean.TRUE); //Marcar como cerrado/completado el cashBoxPartial
        saveCashBoxGeneral();
    }

    public void closeCashBoxPartialInit() {
        if (Objects.equals(Boolean.TRUE, isCashBoxInit())) { //Enserar en 0 los datos de exceso o faltante
            this.cashBoxPartial.setStatusComplete(Boolean.TRUE); //Marcar como cerrado/completado el cashBoxPartial
            this.cashBoxPartial.setExcessCash(BigDecimal.ZERO);
            this.cashBoxPartial.setMissCash(BigDecimal.ZERO);
            saveCashBoxGeneral();
            setCashBoxPartialInitId(this.cashBoxGeneral.getCashBoxPartials().get(0).getId());
            //Solo para este caso, se realiza el depósito automático
            this.cashBoxPartial.setAccountDeposit(accountService.findUniqueByNamedQuery("Account.findByNameAndOrg", "CAJA", this.organizationData.getOrganization()));
            registerRecordInJournal();
            setCashBoxInit(Boolean.FALSE);
        }
    }

    public void closeCashBoxGeneral() {
        updateCashBoxPartialStatusFinally();//Marcar el último parcial como final
        this.cashBoxGeneral.setStatusComplete(Boolean.TRUE);
        cashBoxGeneralService.save(this.cashBoxGeneralId, this.cashBoxGeneral);
        setDisabledButtonCloseCashBoxGeneral(Boolean.TRUE);
        setDisabledButtonBreakdown(Boolean.TRUE);
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
        setSalesNet(BigDecimal.ZERO);
        setSalesEffective(BigDecimal.ZERO);
        setSalesCreditCollect(BigDecimal.ZERO);
//        setSalesCredit(BigDecimal.ZERO);
//        setSalesDedit(BigDecimal.ZERO);
        setPurchasesNet(BigDecimal.ZERO);
        setPurchasesCash(BigDecimal.ZERO);
        setPurchasesCreditPaid(BigDecimal.ZERO);
//        setPurchasesCredit(BigDecimal.ZERO);
        setTransactionNet(BigDecimal.ZERO);
        setTransactionInput(BigDecimal.ZERO);
        setTransactionOutput(BigDecimal.ZERO);
        setCashCurrent(BigDecimal.ZERO);

        setDisabledButtonBreakdown(validDisabledButtonBreakdown());
        setActivePanelBreakdown(validActivePanelBreakdown());
        setDisabledButtonCloseCashBoxGeneral(validDisabledButtonCloseCashBoxGeneral());
    }

    private boolean validDisabledButtonBreakdown() {
        if (this.cashBoxPartial.getId() != null
                && !this.cashBoxPartial.getStatusComplete()) {
            return true;
        } else if (this.cashBoxGeneral.getStatusComplete()) {
            return true;
        }
        return false;
    }

    private boolean validActivePanelBreakdown() {
        if (this.cashBoxPartial.getId() != null
                && !this.cashBoxPartial.getStatusComplete()) {
            return true;
        }
        return false;
    }

    private boolean validDisabledButtonCloseCashBoxGeneral() {
        if (this.cashBoxGeneral.getId() == null || this.cashBoxGeneral.getCashBoxPartials().isEmpty()
                || (this.cashBoxPartial.getId() != null
                && !this.cashBoxPartial.getStatusComplete())) {
            return true;
        } else if (this.cashBoxGeneral.getStatusComplete()) {
            return true;
        }
        return false;
    }

    public void startBreakdown() {
        asignedCashBoxPartialProperties(); //Cargar la data inicial para el cashBoxPartial
        setDisabledButtonBreakdown(Boolean.TRUE); //Deshabilitar el botón de inicio de desglose
        setDisabledButtonCloseCashBoxGeneral(Boolean.TRUE); //Deshabilitar el botón de finalización de caja del día
        setActivePanelBreakdown(Boolean.TRUE); //Mostrar el Panel de Detalle de CashBox instanciado
    }

    public void initRegister() {
        asignedCashBoxPartialProperties(); //Cargar la data inicial para el cashBoxPartial
        setActivePanelInit(Boolean.TRUE); //Mostrar el Panel de Inicio para el primer cashBoxPartial
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

    private void asignedCashBoxPartialProperties() {
        this.cashBoxPartial.setPriority(cashBoxPartialService.getPriority(this.cashBoxGeneral));

        List<Setting> denominations = settingHome.getDenominations(CodeType.DENOMINATION);
        if (!denominations.isEmpty() && this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
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
        }
        getBillsMoneysList();
        this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
    }

    private void getBillsMoneysList() {
        setCashBoxBills(getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.BILL));
        setCashBoxMoneys(getCashBoxDetailsByDenominationType(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.MONEY));
    }

    public List<CashBoxDetail> getCashBoxDetailsByDenominationType(List<CashBoxDetail> list, CashBoxDetail.DenominationType denominationType) {
        List<CashBoxDetail> details = new ArrayList<>();
        if (denominationType != null && list != null && !list.isEmpty()) {
            list.forEach(d -> {
                if (d.getDenominationType().equals(denominationType)) {
                    details.add(d);
                }
            });
        }
        return details;
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

    public void getCalculeSummaryCash(Date _start, Date _end) {

        setCashPrevious(cashBoxGeneralService.findTotalBreakdownFinalByOrganizationAndLastCreatedOn(this.organizationData.getOrganization(), Dates.now()));

        List<Object[]> objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesSourceMethodPaymentDateBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, "EFECTIVO");
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setSalesEffective((BigDecimal) object);
            }
        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesSourceMethodPaymentDateBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, "TARJETA DEBITO");
//        objects.stream().forEach((Object object) -> {
//            if (object != null) {
//                setSalesDedit((BigDecimal) object);
//            }
//        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesSourceMethodPaymentDateBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.INVOICE, StatusType.CLOSE.toString(), _start, _end, "TARJETA CREDITO");
//        objects.stream().forEach((Object object) -> {
//            if (object != null) {
//                setSalesCredit((BigDecimal) object);
//            }
//        });
        objects = invoiceService.findObjectsByNamedQueryWithLimit("Invoice.findTotalInvoiceSalesSourceMethodPaymentDateBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), DocumentType.OVERDUE, StatusType.CLOSE.toString(), _start, _end, "EFECTIVO");
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setSalesCreditCollect((BigDecimal) object);
            }
        });
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CASH);
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setPurchasesCash((BigDecimal) object);
            }
        });
//        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypeBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CREDIT);
//        objects.stream().forEach((Object object) -> {
//            if (object != null) {
//                setPurchasesCredit((BigDecimal) object);
//            }
//        });
        objects = invoiceService.findObjectsByNamedQueryWithLimit("FacturaElectronica.findTotalByEmissionTypePayBetweenOrg", Integer.MAX_VALUE, this.organizationData.getOrganization(), _start, _end, EmissionType.PURCHASE_CREDIT);
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setPurchasesCreditPaid((BigDecimal) object);
            }
        });
        setTransactionAccount(accountService.findUniqueByNamedQuery("Account.findByNameAndOrg", "CAJA DIA", this.organizationData.getOrganization()));
        objects = recordDetailService.findObjectsByNamedQueryWithLimit("RecordDetail.findTotalByAccountAndTypeAndNotClassInvoiceFacturaElectronica", Integer.MAX_VALUE, getTransactionAccount(), RecordDetail.RecordTDetailType.DEBE, _start, _end, this.organizationData.getOrganization());
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setTransactionInput((BigDecimal) object);
            }
        });
        objects = recordDetailService.findObjectsByNamedQueryWithLimit("RecordDetail.findTotalByAccountAndTypeAndNotClassInvoiceFacturaElectronica", Integer.MAX_VALUE, getTransactionAccount(), RecordDetail.RecordTDetailType.HABER, _start, _end, this.organizationData.getOrganization());
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setTransactionOutput((BigDecimal) object);
            }
        });
        setSalesNet(getSalesEffective().add(getSalesCreditCollect()));//Suma de ventas en caja
        setPurchasesNet(getPurchasesCash().add(getPurchasesCreditPaid()));//Suma de compras en caja
        setTransactionNet(getTransactionInput().subtract(getTransactionOutput()));//Suma de transacciones en caja
        setCashCurrent(getSalesNet().subtract(getPurchasesNet()).add(getTransactionNet())); //Total dinero en efectivo de Caja
        setCashCurrent(getCashCurrent().add(getCashPrevious())); //Aumentar el dinero previo de la caja;
    }

    private void registerRecordInJournal() {
        setReglas(new ArrayList<>());
        setAccountingEnabled(isCashBoxInit());
        if (isAccountingEnabled()) {
            setReglas(settingHome.getValue("app.fede.accounting.rule.registrocajadiainicial", "REGISTRO_CAJA_DIA_INICIAL"));

            List<Record> records = new ArrayList<>();
            getReglas().forEach(regla -> {
                Record r = aplicarReglaNegocio(regla, this.cashBoxPartial);
                if (r != null) {
                    records.add(r);
                }
            });

            if (!records.isEmpty()) {
                //La regla compiló bien
                String generalJournalPrefix = settingHome.getValue("app.fede.accounting.generaljournal.prefix", I18nUtil.getMessages("app.fede.accounting.journal"));
                String timestampPattern = settingHome.getValue("app.fede.accounting.generaljournal.timestamp.pattern", "E, dd MMM yyyy HH:mm:ss z");
                GeneralJournal generalJournal = generalJournalService.find(Dates.now(), this.organizationData.getOrganization(), this.subject, generalJournalPrefix, timestampPattern);

                //El General Journal del día
                if (generalJournal != null) {
                    for (Record rcr : records) {
                        setRecordCompleto(Boolean.TRUE);

                        rcr.setCode(UUID.randomUUID().toString());

                        rcr.setOwner(this.subject);
                        rcr.setAuthor(this.subject);

                        rcr.setGeneralJournalId(generalJournal.getId());

                        //Corregir objetos cuenta en los detalles
                        rcr.getRecordDetails().forEach(rcrd -> {
                            rcrd.setLastUpdate(Dates.now());
                            rcrd.setAccount(accountCache.lookupByName(rcrd.getAccountName(), this.organizationData.getOrganization()));
                            if (rcrd.getAccount() == null) {
                                setRecordCompleto(Boolean.FALSE);
                            }
                        });

                        //Persistencia
                        if (Objects.equals(Boolean.TRUE, isRecordCompleto())) {
                            rcr = recordService.save(rcr);
                        } else {
                            this.addSuccessMessage(I18nUtil.getMessages("action.warning"), I18nUtil.getMessages("No fue posible actualizar el Efectivo Actual en Caja."));
                        }
                    }
                }
            }
        }
    }

    public BigDecimal getSalesNet() {
        return salesNet;
    }

    public void setSalesNet(BigDecimal salesNet) {
        this.salesNet = salesNet;
    }

    public BigDecimal getPurchasesNet() {
        return purchasesNet;
    }

    public void setPurchasesNet(BigDecimal purchasesNet) {
        this.purchasesNet = purchasesNet;
    }

    public boolean isDisabledButtonBreakdown() {
        return disabledButtonBreakdown;
    }

    public void setDisabledButtonBreakdown(boolean disabledButtonBreakdown) {
        this.disabledButtonBreakdown = disabledButtonBreakdown;
    }

    public BigDecimal getSalesEffective() {
        return salesEffective;
    }

    public void setSalesEffective(BigDecimal salesEffective) {
        this.salesEffective = salesEffective;
    }

//    public BigDecimal getSalesDedit() {
//        return salesDedit;
//    }
//    
//    public void setSalesDedit(BigDecimal salesDedit) {
//        this.salesDedit = salesDedit;
//    }
//    
//    public BigDecimal getSalesCredit() {
//        return salesCredit;
//    }
//    
//    public void setSalesCredit(BigDecimal salesCredit) {
//        this.salesCredit = salesCredit;
//    }
    public BigDecimal getSalesCreditCollect() {
        return salesCreditCollect;
    }

    public void setSalesCreditCollect(BigDecimal salesCreditCollect) {
        this.salesCreditCollect = salesCreditCollect;
    }

    public BigDecimal getPurchasesCash() {
        return purchasesCash;
    }

    public void setPurchasesCash(BigDecimal purchasesCash) {
        this.purchasesCash = purchasesCash;
    }

//    public BigDecimal getPurchasesCredit() {
//        return purchasesCredit;
//    }
//    
//    public void setPurchasesCredit(BigDecimal purchasesCredit) {
//        this.purchasesCredit = purchasesCredit;
//    }
    public BigDecimal getPurchasesCreditPaid() {
        return purchasesCreditPaid;
    }

    public void setPurchasesCreditPaid(BigDecimal purchasesCreditPaid) {
        this.purchasesCreditPaid = purchasesCreditPaid;
    }

    public BigDecimal getTransactionInput() {
        return transactionInput;
    }

    public void setTransactionInput(BigDecimal transactionInput) {
        this.transactionInput = transactionInput;
    }

    public BigDecimal getTransactionOutput() {
        return transactionOutput;
    }

    public void setTransactionOutput(BigDecimal transactionOutput) {
        this.transactionOutput = transactionOutput;
    }

    public Account getTransactionAccount() {
        return transactionAccount;
    }

    public void setTransactionAccount(Account transactionAccount) {
        this.transactionAccount = transactionAccount;
    }

    public BigDecimal getTransactionNet() {
        return transactionNet;
    }

    public void setTransactionNet(BigDecimal transactionNet) {
        this.transactionNet = transactionNet;
    }

    public BigDecimal getCashPrevious() {
        return cashPrevious;
    }

    public void setCashPrevious(BigDecimal cashPrevious) {
        this.cashPrevious = cashPrevious;
    }

    public BigDecimal getCashCurrent() {
        return cashCurrent;
    }

    public void setCashCurrent(BigDecimal cashCurrent) {
        this.cashCurrent = cashCurrent;
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

    public boolean isActivePanelBreakdown() {
        return activePanelBreakdown;
    }

    public void setActivePanelBreakdown(boolean activePanelBreakdown) {
        this.activePanelBreakdown = activePanelBreakdown;
    }

    public boolean isDisabledButtonCloseCashBoxGeneral() {
        return disabledButtonCloseCashBoxGeneral;
    }

    public void setDisabledButtonCloseCashBoxGeneral(boolean disabledButtonCloseCashBoxGeneral) {
        this.disabledButtonCloseCashBoxGeneral = disabledButtonCloseCashBoxGeneral;
    }

    public boolean isCashBoxInit() {
        return cashBoxInit;
    }

    public void setCashBoxInit(boolean cashBoxInit) {
        this.cashBoxInit = cashBoxInit;
    }

    public Long getCashBoxPartialInitId() {
        return cashBoxPartialInitId;
    }

    public void setCashBoxPartialInitId(Long cashBoxPartialInitId) {
        this.cashBoxPartialInitId = cashBoxPartialInitId;
    }

    public boolean isActivePanelInit() {
        return activePanelInit;
    }

    public void setActivePanelInit(boolean activePanelInit) {
        this.activePanelInit = activePanelInit;
    }

    public boolean isRecordCompleto() {
        return recordCompleto;
    }

    public void setRecordCompleto(boolean recordCompleto) {
        this.recordCompleto = recordCompleto;
    }

    @Override
    public void handleReturn(SelectEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Record aplicarReglaNegocio(String nombreRegla, Object fuenteDatos) {
        CashBoxPartial _instance = (CashBoxPartial) fuenteDatos;

        RecordTemplate _recordTemplate = recordTemplateService.findUniqueByNamedQuery("RecordTemplate.findByCode", nombreRegla, this.organizationData.getOrganization());
        Record record = null;

        if (isAccountingEnabled() && _recordTemplate != null && !Strings.isNullOrEmpty(_recordTemplate.getRule())) {
            record = recordService.createInstance();
            RuleRunner ruleRunner1 = new RuleRunner();
            KnowledgeBuilderErrors kbers = ruleRunner1.run(_recordTemplate, _instance, record); //Armar el registro contable según la regla en recordTemplate
            if (kbers != null) { //Contiene errores de compilación
                logger.error(I18nUtil.getMessages("action.fail"), I18nUtil.getMessages("bussines.entity.rule.erroroncompile", "" + _recordTemplate.getCode(), _recordTemplate.getName()));
                logger.error(kbers.toString());
                record = null; //Invalidar el record
            } else {
                record.setBussinesEntityType(_instance.getClass().getSimpleName());
                record.setBussinesEntityId(getCashBoxPartialInitId());
                record.setBussinesEntityHashCode(_instance.hashCode());
                record.setName(String.format("%s: %s[id=%d]", _recordTemplate.getName(), getClass().getSimpleName(), _instance.getId()));
                record.setDescription(String.format("REGISTRO INICIAL PARA CIERRES DE CAJA\n %s| \nCuenta acreditada: CAJA DIA,\nCuenta debitada: %s, \nMonto: %s", this.subject.getId(), _instance.getAccountDeposit().getName(), Strings.format(_instance.getCashFinally().doubleValue(), "$ #0.##")));
            }
        }
        //El registro casí listo para agregar al journal
        return record;
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
