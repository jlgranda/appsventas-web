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

import com.jlgranda.fede.ejb.CashBoxDetailService;
import com.jlgranda.fede.ejb.CashBoxGeneralService;
import com.jlgranda.fede.ejb.CashBoxPartialService;
import com.jlgranda.fede.ejb.GroupService;
import com.jlgranda.fede.ejb.RecordDetailService;
import com.jlgranda.fede.ejb.sales.InvoiceService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import org.jlgranda.fede.model.accounting.Record;
import org.jlgranda.fede.model.accounting.RecordDetail;
import org.jlgranda.fede.model.document.DocumentType;
import org.jlgranda.fede.model.document.EmissionType;
import org.jpapi.model.CodeType;
import org.jpapi.model.Group;
import org.jpapi.model.Setting;
import org.jpapi.model.StatusType;
import org.jpapi.model.profile.Subject;
import org.jpapi.util.Dates;
import org.jpapi.util.I18nUtil;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

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

    //Calcular el monto y panel de depósito
    private boolean disabledButtonBreakdown;
    private boolean activePanelDeposit;
    private boolean activeSelectDeposit;
    private boolean activeButtonSelectDeposit;
    private Account selectedAccount;

    //Calcular el resumen del cierre de caja
    private BigDecimal salesEffective;
    private BigDecimal salesCreditCollect;
//    private BigDecimal salesCredit;
//    private BigDecimal salesDedit;
    private BigDecimal purchasesCash;
    private BigDecimal purchasesCreditPaid;
//    private BigDecimal purchasesCredit;
    private BigDecimal transactionInput;
    private BigDecimal transactionOutput;

    //Obtener saldo inicial del cierre de caja anterior
    private BigDecimal cashPrevious; //Saldo de ejercicion anterior

    //Calcular el dinero obtenido del cierre de caja
    private BigDecimal cashCurrent; //Efectivo actual

    //Desglosar el efectivo de Caja
    private List<CashBoxDetail> cashBoxBills;
    private List<CashBoxDetail> cashBoxMoneys;
    private boolean activePanelBreakdown;

    //Obtener lista de CashBoxClosed
    private List<CashBoxPartial> cashBoxsClosed;
    private List<CashBoxDetail> cashBoxsClosedBills;
    private List<CashBoxDetail> cashBoxsClosedMoneys;
    private CashBoxPartial cashBoxOpen;
    private boolean disabledButtonCloseCashBoxGeneral;

    //Calcular el dinero restante luego del depósito del Cierre de Caja
    private BigDecimal saldoCashFund;
    private boolean activePanelBreakdownFund;
    private List<CashBoxDetail> cashBoxsInitialBills;
    private List<CashBoxDetail> cashBoxsInitialMoneys;
    private List<CashBoxGeneral> cashBoxGeneralInitial;
    private CashBoxPartial cashBoxInitialFinish;
    private int activeIndex;
    private boolean activePanelVerification;

    @PostConstruct
    private void init() {
        setEnd(Dates.maximumDate(Dates.now()));
        setStart(Dates.minimumDate(getEnd()));

        setCashBoxGeneral(cashBoxGeneralService.createInstance());
        setCashBoxPartial(cashBoxPartialService.createInstance());
        getTodayCashBoxGeneralId();

        initializeVars();
        getCalculeSummaryCash(getStart(), getEnd());

        setOutcome("cash-close");
    }

    public void getTodayCashBoxGeneralId() {
        if (this.cashBoxGeneralId == null) {
            setCashBoxGeneralId(cashBoxGeneralService.findIdByOrganizationAndCreatedOn(this.organizationData.getOrganization(), Dates.now(), Dates.now()));
            getCashBoxGeneral();
        }
    }

    public void getLastCashBoxGeneralTotal() {
        setCashPrevious(cashBoxGeneralService.findTotalBreakdownFinalByOrganizationAndLastCreatedOn(this.organizationData.getOrganization(), Dates.now()));
    }

    public Long getCashBoxGeneralId() {
        return cashBoxGeneralId;
    }

    public void setCashBoxGeneralId(Long cashBoxGeneralId) {
        this.cashBoxGeneralId = cashBoxGeneralId;
    }

    public CashBoxGeneral getCashBoxGeneral() {
        return cashBoxGeneral;
    }

    public void setCashBoxGeneral(CashBoxGeneral cashBoxGeneral) {
        this.cashBoxGeneral = cashBoxGeneral;
    }

    public CashBoxPartial getCashBoxPartial() {
        if (this.cashBoxGeneralId != null && !this.cashBoxGeneral.isPersistent()) {
            this.cashBoxGeneral = cashBoxGeneralService.find(this.cashBoxGeneralId);
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

    /**
     * METHODS.
     */
    public void saveCashBoxPartial() {
    }

    /**
     * METHODS UTIL.
     */
    public void closeCashBoxGeneral() {
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

        setDisabledButtonBreakdown(validActiveButtonBreakdown());
        setActivePanelBreakdown(Boolean.FALSE); //Iniciar con el panel de desglose oculto
    }

    private boolean validActiveButtonBreakdown() {
        if (this.cashBoxPartial.getId() != null
                && !CashBoxPartial.StatusPriority.FINAL.equals(this.cashBoxPartial.getStatus_priority())
                && !CashBoxPartial.Status.OPEN.equals(this.cashBoxPartial.getStatusCashBoxPartial())) {
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

    /**
     * Calcular el monto de dinero en cada evento.
     *
     * @param event
     */
    public void onRowEditCashBoxDetail(RowEditEvent<CashBoxDetail> event) {
        if (event.getObject() != null) {
            event.getObject().setAmount(event.getObject().getValuer().multiply(BigDecimal.valueOf(event.getObject().getQuantity())));
            updateCashBoxDetail(event.getObject());
        }
    }

    public void onRowCancelCashBoxDetail(RowEditEvent<CashBoxDetail> event) {
        System.out.println("onRowCancelCashBoxDetail::: " + event);
    }

    public void updateCashBoxDetail(CashBoxDetail updateCashBoxDetail) {
        if (!this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
            for (int i = 0; i < this.cashBoxPartial.getCashBoxDetails().size(); i++) {
                if (updateCashBoxDetail.getDenomination_type().equals(this.cashBoxPartial.getCashBoxDetails().get(i).getDenomination_type())
                        && updateCashBoxDetail.getDenomination().equals(this.cashBoxPartial.getCashBoxDetails().get(i).getDenomination())) {
                    //Actualizar registro
                    this.cashBoxPartial.getCashBoxDetails().get(i).setQuantity(updateCashBoxDetail.getQuantity());
                    this.cashBoxPartial.getCashBoxDetails().get(i).setAmount(updateCashBoxDetail.getAmount());
                    System.out.println("updateCashBoxDetail::: " + this.cashBoxPartial.getCashBoxDetails().get(i));
                    recalculatedTotals();
                }
            }
        }
    }

    private void recalculatedTotals() {
        resetVariablesTotals();
        this.cashBoxPartial.setCashPartial(getCashCurrent());//Dinero en efectivo que marca según libros hasta ese momento.
        this.cashBoxPartial.setTotalcashBills(getSubTotals(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.BILL));
        this.cashBoxPartial.setTotalcashMoneys(getSubTotals(this.cashBoxPartial.getCashBoxDetails(), CashBoxDetail.DenominationType.MONEY));
        this.cashBoxPartial.setTotalCashBreakdown(this.cashBoxPartial.getTotalcashBills().add(this.cashBoxPartial.getTotalcashMoneys())); //Dinero que ha sido desglosado, existente.
        recalculateBalanceCashBoxPartial(this.cashBoxPartial.getCashPartial());
    }

    private void resetVariablesTotals() {
        this.cashBoxPartial.setTotalcashBills(BigDecimal.ZERO);
        this.cashBoxPartial.setTotalcashMoneys(BigDecimal.ZERO);
        this.cashBoxPartial.setCashPartial(BigDecimal.ZERO);
        this.cashBoxPartial.setMissCashPartial(BigDecimal.ZERO);
        this.cashBoxPartial.setExcessCashPartial(BigDecimal.ZERO);
    }

    private BigDecimal getSubTotals(List<CashBoxDetail> list, CashBoxDetail.DenominationType denominationType) {
        Double subtotal = 0.00;
        subtotal = list.stream()
                .filter(d -> denominationType.equals(d.getDenomination_type()))
                .mapToDouble(d -> d.getAmount().doubleValue())
                .sum();
        return new BigDecimal(subtotal);
    }

    private void recalculateBalanceCashBoxPartial(BigDecimal currentCash) {
        switch (this.cashBoxPartial.getTotalCashBreakdown().compareTo(currentCash)) {
            case 0:
                this.cashBoxPartial.setMissCashPartial(BigDecimal.ZERO);
                this.cashBoxPartial.setExcessCashPartial(BigDecimal.ZERO);
                break;
            case -1:
                this.cashBoxPartial.setMissCashPartial(this.cashBoxPartial.getTotalCashBreakdown().subtract(currentCash));
                this.cashBoxPartial.setExcessCashPartial(BigDecimal.ZERO);
                break;
            case 1:
                this.cashBoxPartial.setMissCashPartial(BigDecimal.ZERO);
                this.cashBoxPartial.setExcessCashPartial(this.cashBoxPartial.getTotalCashBreakdown().subtract(currentCash));
                break;
            default:
                break;
        }
        this.cashBoxPartial.setMissCashPartial(this.cashBoxPartial.getMissCashPartial().multiply(BigDecimal.valueOf(-1)));//Guardar valores positivos
    }

    private void asignedCashBoxPartialProperties() {
        this.cashBoxPartial.setPriority(cashBoxPartialService.getPriority(this.cashBoxGeneral));
        this.cashBoxPartial.setPriority_order(cashBoxPartialService.getPriorityOrder(this.cashBoxPartial));

        List<Setting> denominations = settingHome.getDenominations(CodeType.DENOMINATION);
        if (!denominations.isEmpty() && this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
            denominations.stream().map(d -> {
                this.cashBoxDetail = cashBoxDetailService.createInstance(); //Preparar para un nuevo detalle del CashBox instanciado
                return d;
            }).map(d -> {
                if (d.getCategory().equals(CashBoxDetail.DenominationType.BILL.toString())) {
                    this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.BILL);
                } else if (d.getCategory().equals(CashBoxDetail.DenominationType.MONEY.toString())) {
                    this.cashBoxDetail.setDenomination_type(CashBoxDetail.DenominationType.MONEY);
                }
                return d;
            }).map(d -> {
                this.cashBoxDetail.setDenomination(d.getLabel());
                return d;
            }).map(d -> {
                this.cashBoxDetail.setQuantity(0L);
                return d;
            }).map(d -> {
                this.cashBoxDetail.setValuer(new BigDecimal(d.getValue()));
                return d;
            }).map(_item -> {
                this.cashBoxDetail.setAmount(this.cashBoxDetail.getValuer().multiply(BigDecimal.valueOf(this.cashBoxDetail.getQuantity())));
                return _item;
            }).forEachOrdered(_item -> {
                this.cashBoxPartial.addCashBoxDetail(this.cashBoxDetail);//Agregar el CashBoxDetail al CashBox instanciado
            });
        }
        getBillsMoneysList();
        this.addInfoMessage(I18nUtil.getMessages("action.sucessfully"), I18nUtil.getMessages("common.start") + " " + I18nUtil.getMessages("app.fede.accounting.ajust.breakdown"));
    }

    private void getBillsMoneysList() {
        setCashBoxBills(new ArrayList<>());
        setCashBoxMoneys(new ArrayList<>());
        if (!this.cashBoxPartial.getCashBoxDetails().isEmpty()) {
            this.cashBoxPartial.getCashBoxDetails().forEach(cashboxDetail -> {
                if (cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.BILL)) {
                    getCashBoxBills().add(cashboxDetail);
                } else if (cashboxDetail.getDenomination_type().equals(CashBoxDetail.DenominationType.MONEY)) {
                    getCashBoxMoneys().add(cashboxDetail);
                }
            });
        }
    }

    public void getCalculeSummaryCash(Date _start, Date _end) {
        getLastCashBoxGeneralTotal();

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
        objects = recordDetailService.findObjectsByNamedQueryWithLimit("RecordDetail.findTotalByAccountAndTypeAndNotClassInvoiceFacturaElectronica", Integer.MAX_VALUE, this.selectedAccount, RecordDetail.RecordTDetailType.DEBE, _start, _end, this.organizationData.getOrganization());
        objects.stream().forEach((Object object) -> {
            if (object != null) {
                setTransactionInput((BigDecimal) object);
            }
        });
        objects = recordDetailService.findObjectsByNamedQueryWithLimit("RecordDetail.findTotalByAccountAndTypeAndNotClassInvoiceFacturaElectronica", Integer.MAX_VALUE, this.selectedAccount, RecordDetail.RecordTDetailType.HABER, _start, _end, this.organizationData.getOrganization());
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

    public boolean isActivePanelDeposit() {
        return activePanelDeposit;
    }

    public void setActivePanelDeposit(boolean activePanelDeposit) {
        this.activePanelDeposit = activePanelDeposit;
    }

    public boolean isActiveSelectDeposit() {
        return activeSelectDeposit;
    }

    public void setActiveSelectDeposit(boolean activeSelectDeposit) {
        this.activeSelectDeposit = activeSelectDeposit;
    }

    public boolean isActiveButtonSelectDeposit() {
        return activeButtonSelectDeposit;
    }

    public void setActiveButtonSelectDeposit(boolean activeButtonSelectDeposit) {
        this.activeButtonSelectDeposit = activeButtonSelectDeposit;
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(Account selectedAccount) {
        this.selectedAccount = selectedAccount;
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

    public List<CashBoxPartial> getCashBoxsClosed() {
        return cashBoxsClosed;
    }

    public void setCashBoxsClosed(List<CashBoxPartial> cashBoxsClosed) {
        this.cashBoxsClosed = cashBoxsClosed;
    }

    public List<CashBoxDetail> getCashBoxsClosedBills() {
        return cashBoxsClosedBills;
    }

    public void setCashBoxsClosedBills(List<CashBoxDetail> cashBoxsClosedBills) {
        this.cashBoxsClosedBills = cashBoxsClosedBills;
    }

    public List<CashBoxDetail> getCashBoxsClosedMoneys() {
        return cashBoxsClosedMoneys;
    }

    public void setCashBoxsClosedMoneys(List<CashBoxDetail> cashBoxsClosedMoneys) {
        this.cashBoxsClosedMoneys = cashBoxsClosedMoneys;
    }

    public CashBoxPartial getCashBoxOpen() {
        return cashBoxOpen;
    }

    public void setCashBoxOpen(CashBoxPartial cashBoxOpen) {
        this.cashBoxOpen = cashBoxOpen;
    }

    public boolean isDisabledButtonCloseCashBoxGeneral() {
        return disabledButtonCloseCashBoxGeneral;
    }

    public void setDisabledButtonCloseCashBoxGeneral(boolean disabledButtonCloseCashBoxGeneral) {
        this.disabledButtonCloseCashBoxGeneral = disabledButtonCloseCashBoxGeneral;
    }

    public BigDecimal getSaldoCashFund() {
        return saldoCashFund;
    }

    public void setSaldoCashFund(BigDecimal saldoCashFund) {
        this.saldoCashFund = saldoCashFund;
    }

    public boolean isActivePanelBreakdownFund() {
        return activePanelBreakdownFund;
    }

    public void setActivePanelBreakdownFund(boolean activePanelBreakdownFund) {
        this.activePanelBreakdownFund = activePanelBreakdownFund;
    }

    public List<CashBoxDetail> getCashBoxsInitialBills() {
        return cashBoxsInitialBills;
    }

    public void setCashBoxsInitialBills(List<CashBoxDetail> cashBoxsInitialBills) {
        this.cashBoxsInitialBills = cashBoxsInitialBills;
    }

    public List<CashBoxDetail> getCashBoxsInitialMoneys() {
        return cashBoxsInitialMoneys;
    }

    public void setCashBoxsInitialMoneys(List<CashBoxDetail> cashBoxsInitialMoneys) {
        this.cashBoxsInitialMoneys = cashBoxsInitialMoneys;
    }

    public List<CashBoxGeneral> getCashBoxGeneralInitial() {
        return cashBoxGeneralInitial;
    }

    public void setCashBoxGeneralInitial(List<CashBoxGeneral> cashBoxGeneralInitial) {
        this.cashBoxGeneralInitial = cashBoxGeneralInitial;
    }

    public CashBoxPartial getCashBoxInitialFinish() {
        return cashBoxInitialFinish;
    }

    public void setCashBoxInitialFinish(CashBoxPartial cashBoxInitialFinish) {
        this.cashBoxInitialFinish = cashBoxInitialFinish;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public boolean isActivePanelVerification() {
        return activePanelVerification;
    }

    public void setActivePanelVerification(boolean activePanelVerification) {
        this.activePanelVerification = activePanelVerification;
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
